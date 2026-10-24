package com.finshield.app.ui.screens.analyzer

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Locale

data class DocumentInfo(val uri: Uri, val name: String, val sizeBytes: Long?, val mimeType: String?)

data class AnalyzerUiState(
    val document: DocumentInfo? = null,
    val error: String? = null,
)

class AnalyzerViewModel : ViewModel() {
    private val _state = MutableStateFlow(AnalyzerUiState())
    val state: StateFlow<AnalyzerUiState> = _state.asStateFlow()

    /** Called with the picker result; null means the user cancelled (state is left untouched). */
    fun onDocumentPicked(info: DocumentInfo?) {
        if (info == null) return
        val looksPdf = info.mimeType == "application/pdf" || info.name.endsWith(".pdf", ignoreCase = true)
        _state.value = when {
            !looksPdf -> AnalyzerUiState(error = "That file isn't a PDF. Please choose a PDF loan agreement.")
            info.sizeBytes != null && info.sizeBytes > MAX_BYTES ->
                AnalyzerUiState(error = "That PDF is ${formatSize(info.sizeBytes)}. The maximum size is 20 MB.")
            info.sizeBytes == 0L -> AnalyzerUiState(error = "That file is empty.")
            else -> AnalyzerUiState(document = info)
        }
    }

    fun onPickerFailed() = _state.update { it.copy(error = "Couldn't open the file picker or read that file.") }
    fun clear() { _state.value = AnalyzerUiState() }
    fun dismissError() = _state.update { it.copy(error = null) }

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
