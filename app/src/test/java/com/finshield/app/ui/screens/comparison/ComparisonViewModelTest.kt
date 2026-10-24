package com.finshield.app.ui.screens.comparison

import com.finshield.app.data.comparison.AssetLoanOfferRepository
import com.finshield.app.model.LenderClassification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ComparisonViewModelTest {

    private fun createViewModel(): ComparisonViewModel {
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val repo = AssetLoanOfferRepository(null)
        return ComparisonViewModel(repo, scope)
    }

    @Test
    fun testInitialLoadingOfAll32Offers() {
        val vm = createViewModel()
        val state = vm.state.value

        assertEquals(31, state.allOffers.size)
        assertEquals(31, state.displayedOffers.size)
        assertTrue(state.selectedOfferIds.isNotEmpty())
    }

    @Test
    fun testSearchFilterByLenderName() {
        val vm = createViewModel()

        vm.onSearchQueryChanged("Kotak")
        val state = vm.state.value
        assertEquals(1, state.displayedOffers.size)
        assertEquals("Kotak Mahindra Bank", state.displayedOffers.first().offer.lenderName)

        vm.onSearchQueryChanged("State Bank")
        assertEquals(1, vm.state.value.displayedOffers.size)
        assertEquals("State Bank of India", vm.state.value.displayedOffers.first().offer.lenderName)

        vm.onSearchQueryChanged("Axis")
        assertEquals(1, vm.state.value.displayedOffers.size)
        assertEquals("Axis Bank", vm.state.value.displayedOffers.first().offer.lenderName)
    }

    @Test
    fun testFilterByLenderClassification() {
        val vm = createViewModel()

        // Filter: Public Banks
        vm.onLenderTypeFilterChanged(LenderTypeFilter.PUBLIC_BANK)
        assertEquals(9, vm.state.value.displayedOffers.size)
        assertTrue(vm.state.value.displayedOffers.all { it.offer.lenderClassification == LenderClassification.PUBLIC_BANK })

        // Filter: Private Banks
        vm.onLenderTypeFilterChanged(LenderTypeFilter.PRIVATE_BANK)
        assertEquals(9, vm.state.value.displayedOffers.size)
        assertTrue(vm.state.value.displayedOffers.all { it.offer.lenderClassification == LenderClassification.PRIVATE_BANK })

        // Filter: NBFCs
        vm.onLenderTypeFilterChanged(LenderTypeFilter.NBFC)
        assertEquals(5, vm.state.value.displayedOffers.size)
        assertTrue(vm.state.value.displayedOffers.all { it.offer.lenderClassification == LenderClassification.NBFC })

        // Filter: Digital Lenders
        vm.onLenderTypeFilterChanged(LenderTypeFilter.DIGITAL_LENDER)
        assertEquals(8, vm.state.value.displayedOffers.size)
        assertTrue(vm.state.value.displayedOffers.all { it.offer.lenderClassification == LenderClassification.DIGITAL_LENDER })
    }

    @Test
    fun testSortByStartingRatePlacesNonComparableMonthlyRatesLast() {
        val vm = createViewModel()

        vm.onSortOptionChanged(ComparisonSortOption.STARTING_RATE)
        val displayed = vm.state.value.displayedOffers
        assertEquals(31, displayed.size)

        // First item must be the lowest starting rate: Bandhan Bank (8.86%)
        assertEquals("Bandhan Bank", displayed.first().offer.lenderName)
        assertEquals(8.86, displayed.first().offer.annualRatePercent, 0.01)

        // Second item must be Indian Bank (9.00%)
        assertEquals("Indian Bank", displayed[1].offer.lenderName)
        assertEquals(9.00, displayed[1].offer.annualRatePercent, 0.01)

        // Non-comparable lenders (CASHe, Home Credit, MoneyTap, PaySense, mPokket) must be placed at the end
        val lastItems = displayed.takeLast(5)
        assertTrue(lastItems.any { it.offer.lenderName == "CASHe" })
        assertTrue(lastItems.any { it.offer.lenderName == "PaySense" })
        assertTrue(lastItems.all { !it.offer.isComparableStartingRate })
    }

    @Test
    fun testMultiOfferSelectionAndComparisonTable() {
        val vm = createViewModel()

        // Clear existing selections by toggling them
        val existing = vm.state.value.selectedOfferIds.toList()
        existing.forEach { vm.toggleOfferSelection(it) }
        assertEquals(0, vm.state.value.selectedOfferIds.size)
        assertEquals(0, vm.state.value.comparisonTableOffers.size)

        // Select 2 specific lenders
        vm.toggleOfferSelection("state-bank-of-india")
        vm.toggleOfferSelection("axis-bank")

        assertEquals(2, vm.state.value.selectedOfferIds.size)
        assertEquals(2, vm.state.value.comparisonTableOffers.size)
        assertTrue(vm.state.value.comparisonTableOffers.any { it.offer.id == "state-bank-of-india" })
        assertTrue(vm.state.value.comparisonTableOffers.any { it.offer.id == "axis-bank" })

        // Add a 3rd lender
        vm.toggleOfferSelection("bajaj-finance")
        assertEquals(3, vm.state.value.comparisonTableOffers.size)

        // Deselect one
        vm.toggleOfferSelection("axis-bank")
        assertEquals(2, vm.state.value.comparisonTableOffers.size)
        assertFalse(vm.state.value.comparisonTableOffers.any { it.offer.id == "axis-bank" })
    }
}
