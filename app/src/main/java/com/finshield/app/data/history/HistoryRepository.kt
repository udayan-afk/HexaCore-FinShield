package com.finshield.app.data.history

import com.finshield.app.model.AnalysisSummary
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

sealed interface HistoryState {
    data object Loading : HistoryState
    data class Content(val items: List<AnalysisSummary>) : HistoryState
    data class Error(val message: String) : HistoryState
}

/** History source. A Firestore implementation replaces [DemoHistoryRepository] later. */
interface HistoryRepository {
    fun observe(): Flow<HistoryState>
    suspend fun clear()

    /** Demo-only; real implementations leave this as a no-op. */
    suspend fun restoreSamples() {}
    suspend fun addSummary(summary: AnalysisSummary) {}
}

/** Built-in sample entries. These are NOT user data and NOT from Firebase. */
class DemoHistoryRepository : HistoryRepository {
    private val items = MutableStateFlow(SAMPLES)

    override fun observe(): Flow<HistoryState> = flow {
        emit(HistoryState.Loading)
        delay(300)
        emitAll(items.map { HistoryState.Content(it) })
    }

    override suspend fun clear() { items.value = emptyList() }
    override suspend fun restoreSamples() { items.value = SAMPLES }
    override suspend fun addSummary(summary: AnalysisSummary) {
        items.value = listOf(summary) + items.value
    }

    companion object {
        val SAMPLES = listOf(
            AnalysisSummary("sample-personal", "Sample_Personal_Loan.pdf", "18 May", 12, 2, true),
            AnalysisSummary("sample-education", "Sample_Education_Loan.pdf", "02 May", 9, 1, true),
            AnalysisSummary("sample-vehicle", "Sample_Vehicle_Loan.pdf", "21 Apr", 14, 3, true),
        )
    }
}
