package com.finshield.app.ui.screens.comparison

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finshield.app.data.AppContainer
import com.finshield.app.data.comparison.LoanOfferRepository
import com.finshield.app.domain.calculator.InterestMethod
import com.finshield.app.model.ComputedLoanOffer
import com.finshield.app.model.LoanCategory
import com.finshield.app.model.LoanOffer
import com.finshield.app.model.OfferCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

enum class ComparisonSortOption(val label: String) {
    TOTAL_COST("Total cost"),
    INTEREST_RATE("Interest rate"),
    MONTHLY_EMI("Monthly EMI");
}

enum class MethodFilter(val label: String) {
    ALL("All methods"),
    REDUCING("Reducing only"),
    FLAT("Flat rate only");
}

data class ComparisonUiState(
    val searchQuery: String = "",
    val selectedCategory: LoanCategory = LoanCategory.PERSONAL,
    val methodFilter: MethodFilter = MethodFilter.ALL,
    val sortOption: ComparisonSortOption = ComparisonSortOption.TOTAL_COST,
    val principal: Double = 500_000.0,
    val tenureMonths: Int = 36,
    val selectedOfferIds: Set<String> = setOf("aster-personal", "metro-personal", "swift-personal"),
    val allOffers: List<LoanOffer> = emptyList(),
    val displayedOffers: List<ComputedLoanOffer> = emptyList(),
    val comparisonTableOffers: List<ComputedLoanOffer> = emptyList(),
    val lowestCostOfferId: String? = null,
    val isEditingTerms: Boolean = false,
    val selectedOfferForDetails: ComputedLoanOffer? = null,
    val selectedOfferForOfficialDetails: LoanOffer? = null,
)

class ComparisonViewModel(
    private val repository: LoanOfferRepository = AppContainer.loanOffers,
) : ViewModel() {

    private val _state = MutableStateFlow(ComparisonUiState())
    val state: StateFlow<ComparisonUiState> = _state.asStateFlow()

    init {
        repository.getAllOffers().onEach { offers ->
            _state.value = _state.value.copy(allOffers = offers)
            recompute()
        }.launchIn(viewModelScope)
    }

    fun onSearchQueryChanged(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
        recompute()
    }

    fun onCategorySelected(category: LoanCategory) {
        if (_state.value.selectedCategory == category) return
        // Pick initial selected offers in this category for the comparison table
        val categoryOffers = _state.value.allOffers.filter { it.category == category }
        val newSelected = categoryOffers.take(3).map { it.id }.toSet()
        _state.value = _state.value.copy(
            selectedCategory = category,
            selectedOfferIds = newSelected,
        )
        recompute()
    }

    fun onMethodFilterChanged(filter: MethodFilter) {
        _state.value = _state.value.copy(methodFilter = filter)
        recompute()
    }

    fun cycleMethodFilter() {
        val next = when (_state.value.methodFilter) {
            MethodFilter.ALL -> MethodFilter.REDUCING
            MethodFilter.REDUCING -> MethodFilter.FLAT
            MethodFilter.FLAT -> MethodFilter.ALL
        }
        onMethodFilterChanged(next)
    }

    fun onSortOptionChanged(option: ComparisonSortOption) {
        _state.value = _state.value.copy(sortOption = option)
        recompute()
    }

    fun toggleOfferSelection(offerId: String) {
        val current = _state.value.selectedOfferIds
        val updated = if (current.contains(offerId)) {
            current - offerId
        } else {
            current + offerId
        }
        _state.value = _state.value.copy(selectedOfferIds = updated)
        recompute()
    }

    fun setEditingTerms(isEditing: Boolean) {
        _state.value = _state.value.copy(isEditingTerms = isEditing)
    }

    fun updateTerms(principal: Double, tenureMonths: Int) {
        if (principal > 0 && tenureMonths > 0) {
            _state.value = _state.value.copy(
                principal = principal,
                tenureMonths = tenureMonths,
                isEditingTerms = false,
            )
            recompute()
        }
    }

    fun openOfferDetails(computedOffer: ComputedLoanOffer?) {
        _state.value = _state.value.copy(selectedOfferForDetails = computedOffer)
    }

    fun openOfficialDetails(offer: LoanOffer?) {
        _state.value = _state.value.copy(selectedOfferForOfficialDetails = offer)
    }

    fun resetFilters() {
        val categoryOffers = _state.value.allOffers.filter { it.category == LoanCategory.PERSONAL }
        _state.value = _state.value.copy(
            searchQuery = "",
            selectedCategory = LoanCategory.PERSONAL,
            methodFilter = MethodFilter.ALL,
            sortOption = ComparisonSortOption.TOTAL_COST,
            selectedOfferIds = categoryOffers.take(3).map { it.id }.toSet(),
        )
        recompute()
    }

    private fun recompute() {
        val s = _state.value
        val p = s.principal
        val t = s.tenureMonths

        // Filter by category
        var filtered = s.allOffers.filter { it.category == s.selectedCategory }

        // Filter by method
        when (s.methodFilter) {
            MethodFilter.REDUCING -> {
                filtered = filtered.filter { it.interestMethod == InterestMethod.REDUCING_BALANCE }
            }
            MethodFilter.FLAT -> {
                filtered = filtered.filter { it.interestMethod == InterestMethod.FLAT }
            }
            MethodFilter.ALL -> Unit
        }

        // Search query filter
        if (s.searchQuery.isNotBlank()) {
            val q = s.searchQuery.trim().lowercase()
            filtered = filtered.filter {
                it.lenderName.lowercase().contains(q) ||
                    it.offerName.lowercase().contains(q) ||
                    it.eligibilityNotes.lowercase().contains(q) ||
                    it.category.label.lowercase().contains(q)
            }
        }

        // Compute offers
        val computedOffers = filtered.map { offer ->
            val isSelected = s.selectedOfferIds.contains(offer.id)
            OfferCalculator.compute(offer, p, t).copy(isSelectedForComparison = isSelected)
        }

        // Determine lowest cost offer
        val minCost = computedOffers.minOfOrNull { it.totalCost }
        val lowestOfferId = if (computedOffers.isNotEmpty() && minCost != null) {
            computedOffers.firstOrNull { it.totalCost == minCost }?.offer?.id
        } else null

        // Apply lowest cost flag
        val offersWithLowestCost = computedOffers.map {
            if (it.offer.id == lowestOfferId) it.copy(isLowestCost = true) else it
        }

        // Sort displayed offers
        val sortedOffers = when (s.sortOption) {
            ComparisonSortOption.TOTAL_COST -> offersWithLowestCost.sortedBy { it.totalCost }
            ComparisonSortOption.INTEREST_RATE -> offersWithLowestCost.sortedBy { it.offer.annualRatePercent }
            ComparisonSortOption.MONTHLY_EMI -> offersWithLowestCost.sortedBy { it.installment }
        }

        // Comparison table offers: all selected offers computed for current terms
        val tableOffers = s.allOffers
            .filter { s.selectedOfferIds.contains(it.id) }
            .map { offer ->
                val computed = OfferCalculator.compute(offer, p, t)
                if (offer.id == lowestOfferId) computed.copy(isLowestCost = true, isSelectedForComparison = true)
                else computed.copy(isSelectedForComparison = true)
            }

        _state.value = s.copy(
            displayedOffers = sortedOffers,
            comparisonTableOffers = tableOffers,
            lowestCostOfferId = lowestOfferId,
        )
    }
}
