package com.finshield.app.ui.screens.comparison

import androidx.lifecycle.ViewModel
import com.finshield.app.domain.comparison.ComparisonResult
import com.finshield.app.domain.comparison.LoanComparison
import com.finshield.app.domain.comparison.OfferDraft
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ComparisonUiState(
    val principal: String,
    val tenureMonths: String,
    val offers: List<OfferDraft>,
    val result: ComparisonResult,
    val nextId: Long,
)

class ComparisonViewModel : ViewModel() {
    // Illustrative sample offers only: not real lenders, rates or live data.
    private val initialOffers = listOf(
        OfferDraft(1, "Sample Offer A", "10.5", "5000"),
        OfferDraft(2, "Sample Offer B", "11.25", "1500"),
    )
    private val _state = MutableStateFlow(build("500000", "36", initialOffers, 3))
    val state: StateFlow<ComparisonUiState> = _state.asStateFlow()

    private fun build(principal: String, tenure: String, offers: List<OfferDraft>, nextId: Long) =
        ComparisonUiState(principal, tenure, offers, LoanComparison.evaluate(principal, tenure, offers), nextId)

    private fun update(transform: (ComparisonUiState) -> ComparisonUiState) {
        val s = transform(_state.value)
        _state.value = build(s.principal, s.tenureMonths, s.offers, s.nextId)
    }

    fun onPrincipal(v: String) = update { it.copy(principal = v.filter { c -> c.isDigit() || c == '.' || c == ',' }) }
    fun onTenure(v: String) = update { it.copy(tenureMonths = v.filter { c -> c.isDigit() || c == '.' }) }

    fun onName(id: Long, v: String) = editOffer(id) { it.copy(name = v.take(40)) }
    fun onRate(id: Long, v: String) = editOffer(id) { it.copy(rateText = v.filter { c -> c.isDigit() || c == '.' }) }
    fun onFee(id: Long, v: String) = editOffer(id) { it.copy(feeText = v.filter { c -> c.isDigit() || c == '.' || c == ',' }) }

    private fun editOffer(id: Long, f: (OfferDraft) -> OfferDraft) =
        update { s -> s.copy(offers = s.offers.map { if (it.id == id) f(it) else it }) }

    fun addOffer() = update { s ->
        if (s.offers.size >= LoanComparison.MAX_OFFERS) s
        else s.copy(
            offers = s.offers + OfferDraft(s.nextId, "Offer ${s.offers.size + 1}", "", ""),
            nextId = s.nextId + 1,
        )
    }

    fun removeOffer(id: Long) = update { s ->
        if (s.offers.size <= LoanComparison.MIN_OFFERS) s else s.copy(offers = s.offers.filterNot { it.id == id })
    }
}
