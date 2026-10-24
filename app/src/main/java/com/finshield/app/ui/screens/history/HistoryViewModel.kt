package com.finshield.app.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finshield.app.data.AppContainer
import com.finshield.app.data.history.HistoryRepository
import com.finshield.app.data.history.HistoryState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(private val repo: HistoryRepository = AppContainer.history) : ViewModel() {
    private val reload = MutableStateFlow(0)

    val state: StateFlow<HistoryState> = reload
        .flatMapLatest {
            repo.observe().catch { emit(HistoryState.Error("Couldn't load your analyses.")) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryState.Loading)

    fun retry() { reload.value += 1 }
    fun clear() { viewModelScope.launch { repo.clear() } }
    fun restoreSamples() { viewModelScope.launch { repo.restoreSamples() } }
}
