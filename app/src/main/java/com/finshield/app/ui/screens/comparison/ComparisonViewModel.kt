package com.finshield.app.ui.screens.comparison

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finshield.app.data.AppContainer
import com.finshield.app.data.comparison.LoanOfferRepository
import com.finshield.app.model.ComputedLoanOffer
import com.finshield.app.model.LenderClassification
import com.finshield.app.model.LoanCategory
import com.finshield.app.model.LoanOffer
import com.finshield.app.model.OfferCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

enum class ComparisonSortOption(val label: String) {
    STARTING_RATE("Starting rate"),
    MONTHLY_EMI("Monthly EMI"),
    TOTAL_COST("Total cost"),
    LENDER_NAME("Lender name");
}

enum class LenderTypeFilter(val label: String) {
    ALL("All Lenders"),
    PUBLIC_BANK("Public Banks"),
    PRIVATE_BANK("Private Banks"),
    NBFC("NBFCs"),
    DIGITAL_LENDER("Digital Lenders");
}

data class ComparisonUiState(
    val searchQuery: String = "",
    val selectedCategory: LoanCategory = LoanCategory.PERSONAL,
    val lenderTypeFilter: LenderTypeFilter = LenderTypeFilter.ALL,
    val sortOption: ComparisonSortOption = ComparisonSortOption.STARTING_RATE,
    val principal: Double = 500_000.0,
    val tenureMonths: Int = 36,
    val selectedOfferIds: Set<String> = emptySet(),
    val allOffers: List<LoanOffer> = emptyList(),
    val displayedOffers: List<ComputedLoanOffer> = emptyList(),
    val comparisonTableOffers: List<ComputedLoanOffer> = emptyList(),
    val lowestCostOfferId: String? = null,
    val isEditingTerms: Boolean = false,
    val selectedOfferForDetails: ComputedLoanOffer? = null,
    val datasetVersion: String = "2026.10-directory",
    val datasetRefreshDate: String = "24 Oct 2026",
    val loadError: String? = null,
)

class ComparisonViewModel(
    private val repository: LoanOfferRepository = AppContainer.loanOffers,
    coroutineScope: kotlinx.coroutines.CoroutineScope? = null,
) : ViewModel() {

    private val scope = coroutineScope ?: viewModelScope
    private val _state = MutableStateFlow(ComparisonUiState())
    val state: StateFlow<ComparisonUiState> = _state.asStateFlow()

    init {
        repository.getAllOffers().onEach { offers ->
            val initialSelected = if (offers.size >= 3) {
                val publicOne = offers.firstOrNull { it.category == LoanCategory.PERSONAL && it.lenderClassification == LenderClassification.PUBLIC_BANK }?.id
                    ?: offers.firstOrNull { it.lenderClassification == LenderClassification.PUBLIC_BANK }?.id
                val privateOne = offers.firstOrNull { it.category == LoanCategory.PERSONAL && it.lenderClassification == LenderClassification.PRIVATE_BANK }?.id
                    ?: offers.firstOrNull { it.lenderClassification == LenderClassification.PRIVATE_BANK }?.id
                val nbfcOne = offers.firstOrNull { it.category == LoanCategory.PERSONAL && it.lenderClassification == LenderClassification.NBFC }?.id
                    ?: offers.firstOrNull { it.lenderClassification == LenderClassification.NBFC }?.id
                setOfNotNull(publicOne, privateOne, nbfcOne)
            } else {
                offers.take(3).map { it.id }.toSet()
            }

            _state.value = _state.value.copy(
                allOffers = offers,
                selectedOfferIds = if (_state.value.selectedOfferIds.isEmpty() || !_state.value.allOffers.any { it.id in _state.value.selectedOfferIds }) {
                    initialSelected
                } else _state.value.selectedOfferIds,
            )
            recompute()
        }.launchIn(scope)
    }

    fun onSearchQueryChanged(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
        recompute()
    }

    fun onCategorySelected(category: LoanCategory) {
        if (_state.value.selectedCategory == category) return
        _state.value = _state.value.copy(selectedCategory = category)
        recompute()
    }

    fun onLenderTypeFilterChanged(filter: LenderTypeFilter) {
        _state.value = _state.value.copy(lenderTypeFilter = filter)
        recompute()
    }

    fun cycleLenderTypeFilter() {
        val next = when (_state.value.lenderTypeFilter) {
            LenderTypeFilter.ALL -> LenderTypeFilter.PUBLIC_BANK
            LenderTypeFilter.PUBLIC_BANK -> LenderTypeFilter.PRIVATE_BANK
            LenderTypeFilter.PRIVATE_BANK -> LenderTypeFilter.NBFC
            LenderTypeFilter.NBFC -> LenderTypeFilter.DIGITAL_LENDER
            LenderTypeFilter.DIGITAL_LENDER -> LenderTypeFilter.ALL
        }
        onLenderTypeFilterChanged(next)
    }

    fun onSortOptionChanged(option: ComparisonSortOption) {
        _state.value = _state.value.copy(sortOption = option)
        recompute()
    }

    fun toggleOfferSelection(offerId: String) {
        // Resolve ID with backward compatibility for plain lender slugs
        val actualId = _state.value.allOffers.firstOrNull {
            it.id == offerId || it.id == "$offerId-personal-loan" || it.id.startsWith("$offerId-")
        }?.id ?: offerId

        val current = _state.value.selectedOfferIds
        val isAlreadySelected = current.contains(actualId) || current.contains(offerId)

        val updated = if (isAlreadySelected) {
            current - actualId - offerId
        } else {
            if (current.size >= 4) {
                // Keep max 4 selected offers for comparison table readability
                (current.drop(1).toSet()) + actualId
            } else {
                current + actualId
            }
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

    fun resetFilters() {
        _state.value = _state.value.copy(
            searchQuery = "",
            lenderTypeFilter = LenderTypeFilter.ALL,
            sortOption = ComparisonSortOption.STARTING_RATE,
        )
        recompute()
    }

    private fun recompute() {
        val s = _state.value
        val p = s.principal
        val t = s.tenureMonths

        var filtered = s.allOffers.filter { it.category == s.selectedCategory }

        // Filter by Lender Type
        when (s.lenderTypeFilter) {
            LenderTypeFilter.PUBLIC_BANK -> {
                filtered = filtered.filter { it.lenderClassification == LenderClassification.PUBLIC_BANK }
            }
            LenderTypeFilter.PRIVATE_BANK -> {
                filtered = filtered.filter { it.lenderClassification == LenderClassification.PRIVATE_BANK }
            }
            LenderTypeFilter.NBFC -> {
                filtered = filtered.filter { it.lenderClassification == LenderClassification.NBFC }
            }
            LenderTypeFilter.DIGITAL_LENDER -> {
                filtered = filtered.filter { it.lenderClassification == LenderClassification.DIGITAL_LENDER }
            }
            LenderTypeFilter.ALL -> Unit
        }

        // Search query filter: matches bank, product, loan type, description, security, notes, rate, fee
        if (s.searchQuery.isNotBlank()) {
            val q = s.searchQuery.trim().lowercase()
            filtered = filtered.filter {
                it.lenderName.lowercase().contains(q) ||
                    it.offerName.lowercase().contains(q) ||
                    it.category.label.lowercase().contains(q) ||
                    it.category.shortLabel.lowercase().contains(q) ||
                    it.shortDescription.lowercase().contains(q) ||
                    it.securityType.lowercase().contains(q) ||
                    it.eligibilityNotes.lowercase().contains(q) ||
                    it.publishedInterestRate.lowercase().contains(q) ||
                    it.publishedProcessingFee.lowercase().contains(q) ||
                    it.lenderClassification.label.lowercase().contains(q) ||
                    it.lenderClassification.shortLabel.lowercase().contains(q)
            }
        }

        // Compute offers
        val computedOffers = filtered.map { offer ->
            val isSelected = s.selectedOfferIds.contains(offer.id)
            OfferCalculator.compute(offer, p, t).copy(isSelectedForComparison = isSelected)
        }

        // Determine lowest cost among validly comparable offers
        val comparableOffers = computedOffers.filter { it.totalCost != null }
        val minCost = comparableOffers.minOfOrNull { it.totalCost!! }
        val lowestOfferId = if (minCost != null) {
            comparableOffers.firstOrNull { it.totalCost == minCost }?.offer?.id
        } else null

        val offersWithLowestCost = computedOffers.map {
            if (it.offer.id == lowestOfferId) it.copy(isLowestCost = true) else it
        }

        // Sort displayed offers: only comparable starting rates sorted numerically; non-comparables come after
        val sortedOffers = when (s.sortOption) {
            ComparisonSortOption.STARTING_RATE -> {
                offersWithLowestCost.sortedWith(
                    compareBy<ComputedLoanOffer> { !it.offer.isComparableStartingRate || it.offer.minAnnualRate == null }
                        .thenBy { it.offer.minAnnualRate ?: Double.MAX_VALUE }
                        .thenBy { it.offer.lenderName }
                )
            }
            ComparisonSortOption.MONTHLY_EMI -> {
                offersWithLowestCost.sortedWith(
                    compareBy<ComputedLoanOffer> { it.installment == null }
                        .thenBy { it.installment ?: Double.MAX_VALUE }
                        .thenBy { it.offer.lenderName }
                )
            }
            ComparisonSortOption.TOTAL_COST -> {
                offersWithLowestCost.sortedWith(
                    compareBy<ComputedLoanOffer> { it.totalCost == null }
                        .thenBy { it.totalCost ?: Double.MAX_VALUE }
                        .thenBy { it.offer.lenderName }
                )
            }
            ComparisonSortOption.LENDER_NAME -> {
                offersWithLowestCost.sortedBy { it.offer.lenderName }
            }
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
