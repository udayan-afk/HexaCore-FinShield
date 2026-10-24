package com.finshield.app.domain.analyzer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result -> cont.resume(result) }
    addOnFailureListener { exception -> cont.resumeWithException(exception) }
    addOnCanceledListener { cont.cancel() }
}

data class PageExtraction(
    val pageNumber: Int,
    val text: String,
    val charCount: Int,
    val isBlank: Boolean,
)

data class DocumentExtractionResult(
    val fullText: String,
    val pages: List<PageExtraction>,
    val totalPages: Int,
    val warnings: List<String> = emptyList(),
)

sealed interface ExtractionProgress {
    data class Status(val message: String, val current: Int, val total: Int) : ExtractionProgress
}

class DocumentTextExtractor(private val context: Context) {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * Extracts text from multiple scanned image URIs captured by the document scanner.
     */
    suspend fun extractFromImages(
        imageUris: List<Uri>,
        onProgress: (ExtractionProgress) -> Unit = {},
    ): DocumentExtractionResult = withContext(Dispatchers.IO) {
        val pages = mutableListOf<PageExtraction>()
        val warnings = mutableListOf<String>()
        val total = imageUris.size

        imageUris.forEachIndexed { index, uri ->
            val pageNum = index + 1
            onProgress(ExtractionProgress.Status("Scanning page $pageNum of $total with OCR…", pageNum, total))

            try {
                val inputImage = InputImage.fromFilePath(context, uri)
                val visionText = recognizer.process(inputImage).awaitResult()
                val text = visionText.text.trim()
                val isBlank = text.length < 15

                if (isBlank) {
                    warnings.add("Page $pageNum had very little readable text. Please check image clarity.")
                }

                pages.add(
                    PageExtraction(
                        pageNumber = pageNum,
                        text = text,
                        charCount = text.length,
                        isBlank = isBlank,
                    ),
                )
            } catch (e: Exception) {
                warnings.add("Could not process page $pageNum: ${e.localizedMessage ?: "Unknown error"}")
                pages.add(PageExtraction(pageNum, "", 0, isBlank = true))
            }
        }

        val combinedText = pages.joinToString("\n\n--- Page Break ---\n\n") { it.text }
        DocumentExtractionResult(
            fullText = combinedText,
            pages = pages,
            totalPages = total,
            warnings = warnings,
        )
    }

    /**
     * Extracts text from a PDF document by rendering its pages with PdfRenderer and running ML Kit OCR.
     * Safely handles password protection, empty files, and renders up to [maxPages].
     */
    suspend fun extractFromPdf(
        pdfUri: Uri,
        maxPages: Int = 10,
        onProgress: (ExtractionProgress) -> Unit = {},
    ): DocumentExtractionResult = withContext(Dispatchers.IO) {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        val pages = mutableListOf<PageExtraction>()
        val warnings = mutableListOf<String>()

        try {
            pfd = context.contentResolver.openFileDescriptor(pdfUri, "r")
                ?: throw IllegalArgumentException("Cannot open PDF file descriptor.")

            try {
                renderer = PdfRenderer(pfd)
            } catch (e: SecurityException) {
                throw IllegalStateException("This PDF is password-protected or encrypted. Please provide an unlocked document.", e)
            }

            val pageCount = renderer.pageCount
            val pagesToProcess = minOf(pageCount, maxPages)

            if (pageCount > maxPages) {
                warnings.add("Document has $pageCount pages. First $maxPages pages were analyzed.")
            }

            for (i in 0 until pagesToProcess) {
                val pageNum = i + 1
                onProgress(ExtractionProgress.Status("Reading PDF page $pageNum of $pagesToProcess…", pageNum, pagesToProcess))

                val page = renderer.openPage(i)
                // Render at a clear resolution (e.g. 150-200 DPI scale)
                val width = page.width * 2
                val height = page.height * 2
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

                // White background to ensure OCR readability
                bitmap.eraseColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                try {
                    val inputImage = InputImage.fromBitmap(bitmap, 0)
                    val visionText = recognizer.process(inputImage).awaitResult()
                    val text = visionText.text.trim()
                    val isBlank = text.length < 15

                    if (isBlank) {
                        warnings.add("Page $pageNum appeared empty or has unreadable text.")
                    }

                    pages.add(
                        PageExtraction(
                            pageNumber = pageNum,
                            text = text,
                            charCount = text.length,
                            isBlank = isBlank,
                        ),
                    )
                } finally {
                    bitmap.recycle()
                }
            }

            val combinedText = pages.joinToString("\n\n--- Page Break ---\n\n") { it.text }
            DocumentExtractionResult(
                fullText = combinedText,
                pages = pages,
                totalPages = pageCount,
                warnings = warnings,
            )
        } finally {
            try {
                renderer?.close()
            } catch (_: Exception) {}
            try {
                pfd?.close()
            } catch (_: Exception) {}
        }
    }
}
