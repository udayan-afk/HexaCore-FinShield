package com.finshield.app.ui.screens.analyzer

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finshield.app.data.AppContainer
import com.finshield.app.domain.analyzer.DocumentTextExtractor
import com.finshield.app.domain.analyzer.ExtractionProgress
import com.finshield.app.domain.analyzer.LoanAgreementAnalyzer
import com.finshield.app.model.AnalysisReportStore
import com.finshield.app.model.AnalysisSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

sealed interface SelectedDocument {
    val displayName: String
    val pageCountDescription: String

    data class Pdf(
        val uri: Uri,
        val name: String,
        val sizeBytes: Long?,
        val mimeType: String?,
    ) : SelectedDocument {
        override val displayName: String get() = name
        override val pageCountDescription: String
            get() = sizeBytes?.let { AnalyzerViewModel.formatSize(it) } ?: "PDF document"
    }

    data class Scanned(
        val pageUris: List<Uri>,
        val pdfUri: Uri?,
        val pageCount: Int,
        val scannedAt: Long = System.currentTimeMillis(),
    ) : SelectedDocument {
        override val displayName: String
            get() = "Camera_Scan_${SimpleDateFormat("ddMMM_HHmm", Locale.US).format(Date(scannedAt))}.pdf"
        override val pageCountDescription: String
            get() = "$pageCount page${if (pageCount != 1) "s" else ""} captured"
    }
}

data class AnalyzerUiState(
    val document: SelectedDocument? = null,
    val isAnalyzing: Boolean = false,
    val progressMessage: String = "",
    val error: String? = null,
    val generatedReportId: String? = null,
)

class AnalyzerViewModel : ViewModel() {
    private val _state = MutableStateFlow(AnalyzerUiState())
    val state: StateFlow<AnalyzerUiState> = _state.asStateFlow()

    /** Handles PDF file selected from system document picker */
    fun onPdfPicked(uri: Uri?, context: Context) {
        if (uri == null) return
        val info = readDocumentInfo(context, uri)
        if (info == null) {
            _state.update { it.copy(error = "Could not read the selected PDF file.") }
            return
        }

        val looksPdf = info.mimeType == "application/pdf" || info.name.endsWith(".pdf", ignoreCase = true)
        _state.value = when {
            !looksPdf -> _state.value.copy(error = "That file is not a PDF. Please choose a valid PDF loan agreement.")
            info.sizeBytes != null && info.sizeBytes > MAX_BYTES ->
                _state.value.copy(error = "That PDF is ${formatSize(info.sizeBytes)}. The maximum size is 20 MB.")
            info.sizeBytes == 0L -> _state.value.copy(error = "The selected file is empty.")
            else -> _state.value.copy(
                document = SelectedDocument.Pdf(
                    uri = info.uri,
                    name = info.name,
                    sizeBytes = info.sizeBytes,
                    mimeType = info.mimeType,
                ),
                error = null,
            )
        }
    }

    /** Handles multi-page camera scan completed via ML Kit Document Scanner */
    fun onPagesScanned(pageUris: List<Uri>, pdfUri: Uri?) {
        if (pageUris.isEmpty() && pdfUri == null) {
            _state.update { it.copy(error = "No pages were captured during scan.") }
            return
        }

        _state.update {
            it.copy(
                document = SelectedDocument.Scanned(
                    pageUris = pageUris,
                    pdfUri = pdfUri,
                    pageCount = if (pageUris.isNotEmpty()) pageUris.size else 1,
                ),
                error = null,
            )
        }
    }

    fun onScanCancelled() {
        // User voluntarily dismissed scanner; leave existing state untouched
    }

    fun onScanError(errorMessage: String) {
        _state.update { it.copy(error = "Scanner error: $errorMessage") }
    }

    /** Executes OCR and document analysis for either PDF or camera scan */
    fun startAnalysis(context: Context, onComplete: (String) -> Unit) {
        val doc = _state.value.document ?: return
        if (_state.value.isAnalyzing) return

        _state.update {
            it.copy(
                isAnalyzing = true,
                progressMessage = "Preparing document for OCR…",
                error = null,
            )
        }

        viewModelScope.launch {
            try {
                val extractor = DocumentTextExtractor(context)
                val extraction = when (doc) {
                    is SelectedDocument.Pdf -> {
                        extractor.extractFromPdf(doc.uri) { progress ->
                            when (progress) {
                                is ExtractionProgress.Status -> {
                                    _state.update { it.copy(progressMessage = progress.message) }
                                }
                            }
                        }
                    }
                    is SelectedDocument.Scanned -> {
                        extractor.extractFromImages(doc.pageUris) { progress ->
                            when (progress) {
                                is ExtractionProgress.Status -> {
                                    _state.update { it.copy(progressMessage = progress.message) }
                                }
                            }
                        }
                    }
                }

                if (extraction.fullText.isBlank()) {
                    _state.update {
                        it.copy(
                            isAnalyzing = false,
                            error = "Could not extract readable text from the document. Please ensure the document is clear and well-lit.",
                        )
                    }
                    return@launch
                }

                val consentStore = com.finshield.app.domain.analyzer.AiConsentStore(context)
                val geminiAssistant = if (consentStore.isAiEnabled && consentStore.hasUserConsented) {
                    _state.update { it.copy(progressMessage = "Extracting terms with Gemini AI & financial validation…") }
                    com.finshield.app.domain.analyzer.GeminiLoanAssistant()
                } else {
                    _state.update { it.copy(progressMessage = "Analyzing clauses, interest rates & EMI…") }
                    null
                }

                val reportId = UUID.randomUUID().toString()
                val report = LoanAgreementAnalyzer.analyzeWithAi(
                    documentName = doc.displayName,
                    extraction = extraction,
                    reportId = reportId,
                    geminiAssistant = geminiAssistant,
                )

                // Store report in in-memory session store
                AnalysisReportStore.saveReport(reportId, report)

                // Record in history
                val summary = AnalysisSummary(
                    id = reportId,
                    documentName = doc.displayName,
                    reviewedOn = SimpleDateFormat("dd MMM", Locale.US).format(Date()),
                    pages = extraction.totalPages,
                    itemsToReview = report.manualReview.size + report.missingInformation.size,
                    isSample = false,
                )
                AppContainer.history.addSummary(summary)

                _state.update {
                    it.copy(
                        isAnalyzing = false,
                        progressMessage = "Done!",
                        generatedReportId = reportId,
                    )
                }

                onComplete(reportId)
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isAnalyzing = false,
                        error = e.localizedMessage ?: "An error occurred while analyzing the document.",
                    )
                }
            }
        }
    }

    fun clear() {
        _state.value = AnalyzerUiState()
    }

    fun dismissError() {
        _state.update { it.copy(error = null) }
    }

    companion object {
        const val MAX_BYTES = 20L * 1024 * 1024

        fun formatSize(bytes: Long): String = when {
            bytes >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> String.format(Locale.US, "%d KB", bytes / 1024)
            else -> "$bytes B"
        }
    }
}

/** Reads display name/size/type for a picked document. Uses only the system picker grant, no storage permission. */
data class DocumentInfo(val uri: Uri, val name: String, val sizeBytes: Long?, val mimeType: String?)

fun readDocumentInfo(context: Context, uri: Uri): DocumentInfo? = try {
    var name = "document.pdf"
    var size: Long? = null
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
        if (c.moveToFirst()) {
            val ni = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val si = c.getColumnIndex(OpenableColumns.SIZE)
            if (ni >= 0 && !c.isNull(ni)) name = c.getString(ni)
            if (si >= 0 && !c.isNull(si)) size = c.getLong(si)
        }
    }
    DocumentInfo(uri, name, size, context.contentResolver.getType(uri))
} catch (e: Exception) {
    null
}
