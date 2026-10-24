package com.finshield.app.ui.screens.comparison

import com.finshield.app.data.comparison.AssetLoanOfferRepository
import com.finshield.app.model.LenderClassification
import com.finshield.app.model.LoanCategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ComparisonViewModelTest {

    private fun createViewModel(): ComparisonViewModel {
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val repo = AssetLoanOfferRepository(null)
        return ComparisonViewModel(repo, scope)
    }

    @Test
    fun testInitialLoadingOfAllMergedOffers() {
        val vm = createViewModel()
        val state = vm.state.value

        // 570 directory records + 14 unique research records (NBFCs/digital) = 584 merged offers
        assertEquals(584, state.allOffers.size)

        // Default category is Personal Loan (30 directory banks + 14 research lenders = 44 offers)
        assertEquals(44, state.displayedOffers.size)
        assertTrue(state.selectedOfferIds.isNotEmpty())
    }

    @Test
    fun testHomeLoanCategoryFixesZeroResultsIssue() {
        val vm = createViewModel()

        // Switch to Home Loan
        vm.onCategorySelected(LoanCategory.HOME)
        val state = vm.state.value

        // Must NOT be 0! The 30-bank directory populates 30 Home Loan records
        assertEquals(30, state.displayedOffers.size)
        assertTrue(state.displayedOffers.all { it.offer.category == LoanCategory.HOME })

        // Check prominent banks
        val sbi = state.displayedOffers.firstOrNull { it.offer.lenderName == "State Bank of India" }
        assertNotNull("SBI Home Loan must be present", sbi)
        assertTrue(sbi!!.offer.securityType.contains("property", ignoreCase = true))
        assertEquals("Verify on bank website", sbi.offer.availabilityStatus)
        assertEquals("Rate not listed — check lender", sbi.offer.publishedInterestRate)
        assertNull(sbi.installment)

        val hdfc = state.displayedOffers.firstOrNull { it.offer.lenderName == "HDFC Bank" }
        assertNotNull("HDFC Home Loan must be present", hdfc)
    }

    @Test
    fun testEverySupportedCategoryFiltersCorrectly() {
        val vm = createViewModel()

        val expectedCategories = listOf(
            LoanCategory.PERSONAL,
            LoanCategory.HOME,
            LoanCategory.EDUCATION,
            LoanCategory.CAR,
            LoanCategory.TWO_WHEELER,
            LoanCategory.GOLD,
            LoanCategory.BUSINESS,
            LoanCategory.AGRICULTURE,
            LoanCategory.COMMERCIAL_VEHICLE,
            LoanCategory.CONSUMER_DURABLE,
            LoanCategory.MEDICAL,
            LoanCategory.PROPERTY,
            LoanCategory.SECURITIES,
            LoanCategory.FIXED_DEPOSIT,
            LoanCategory.INSURANCE_POLICY,
            LoanCategory.PROVIDENT_FUND,
            LoanCategory.RENEWABLE_ENERGY,
            LoanCategory.MICROFINANCE,
            LoanCategory.OVERDRAFT,
        )

        for (category in expectedCategories) {
            vm.onCategorySelected(category)
            val count = vm.state.value.displayedOffers.size
            assertTrue("Category ${category.label} must have at least 30 records, found $count", count >= 30)
            assertTrue(vm.state.value.displayedOffers.all { it.offer.category == category })
        }
    }

    @Test
    fun testSearchFilterAcrossBankProductAndSecurity() {
        val vm = createViewModel()

        // Search by bank name in Personal Loans
        vm.onSearchQueryChanged("Kotak")
        var state = vm.state.value
        assertEquals(1, state.displayedOffers.size)
        assertEquals("Kotak Mahindra Bank", state.displayedOffers.first().offer.lenderName)

        // Search by bank name in Home Loans
        vm.onCategorySelected(LoanCategory.HOME)
        vm.onSearchQueryChanged("State Bank")
        state = vm.state.value
        assertEquals(1, state.displayedOffers.size)
        assertEquals("State Bank of India", state.displayedOffers.first().offer.lenderName)

        // Search by security type
        vm.onSearchQueryChanged("Property")
        state = vm.state.value
        assertEquals(30, state.displayedOffers.size) // All Home Loans have Secured (Property)
    }

    @Test
    fun testFilterByLenderClassification() {
        val vm = createViewModel()

        // 1. In Personal Loans
        vm.onCategorySelected(LoanCategory.PERSONAL)

        vm.onLenderTypeFilterChanged(LenderTypeFilter.PUBLIC_BANK)
        val publicOffers = vm.state.value.displayedOffers
        assertEquals(13, publicOffers.size) // 12 PSBs from directory + J&K Bank (Public/Regional)
        assertTrue(vm.state.value.displayedOffers.all { it.offer.lenderClassification == LenderClassification.PUBLIC_BANK })

        vm.onLenderTypeFilterChanged(LenderTypeFilter.NBFC)
        assertEquals(5, vm.state.value.displayedOffers.size)
        assertTrue(vm.state.value.displayedOffers.all { it.offer.lenderClassification == LenderClassification.NBFC })

        vm.onLenderTypeFilterChanged(LenderTypeFilter.DIGITAL_LENDER)
        assertEquals(8, vm.state.value.displayedOffers.size)
        assertTrue(vm.state.value.displayedOffers.all { it.offer.lenderClassification == LenderClassification.DIGITAL_LENDER })

        // 2. In Home Loans: 12 Public Banks, 18 Private Banks
        vm.onCategorySelected(LoanCategory.HOME)
        vm.onLenderTypeFilterChanged(LenderTypeFilter.PUBLIC_BANK)
        assertEquals(12, vm.state.value.displayedOffers.size)

        vm.onLenderTypeFilterChanged(LenderTypeFilter.PRIVATE_BANK)
        assertEquals(18, vm.state.value.displayedOffers.size)
    }

    @Test
    fun testSortByStartingRatePlacesNonComparableAndMissingRatesLast() {
        val vm = createViewModel()
        vm.onCategorySelected(LoanCategory.PERSONAL)
        vm.onSortOptionChanged(ComparisonSortOption.STARTING_RATE)

        val displayed = vm.state.value.displayedOffers
        assertEquals(44, displayed.size)

        // First item must be the lowest starting rate: Bandhan Bank (8.86%)
        assertEquals("Bandhan Bank", displayed.first().offer.lenderName)
        assertEquals(8.86, displayed.first().offer.annualRatePercent, 0.01)

        // Second item must be Indian Bank (9.00%)
        assertEquals("Indian Bank", displayed[1].offer.lenderName)
        assertEquals(9.00, displayed[1].offer.annualRatePercent, 0.01)

        // Non-comparables and unlisted rates must be placed at the end, not at rate 0%
        val lastItems = displayed.takeLast(18)
        assertTrue(lastItems.any { it.offer.lenderName == "CASHe" })
        assertTrue(lastItems.any { it.offer.lenderName == "PaySense" })
        assertTrue(lastItems.all { !it.offer.isComparableStartingRate || it.offer.minAnnualRate == null })
    }

    @Test
    fun testNoDuplicateProductsForSameBankAndCategory() {
        val vm = createViewModel()
        val allOffers = vm.state.value.allOffers

        val duplicates = allOffers.groupBy { "${it.lenderName.lowercase()}-${it.category}" }
            .filter { it.value.size > 1 }

        assertTrue("No bank should have duplicate offers in the same category: ${duplicates.keys}", duplicates.isEmpty())
    }

    @Test
    fun testMultiOfferSelectionAndComparisonTable() {
        val vm = createViewModel()
        vm.onCategorySelected(LoanCategory.HOME)

        // Clear existing selections by toggling them
        val existing = vm.state.value.selectedOfferIds.toList()
        existing.forEach { vm.toggleOfferSelection(it) }
        assertEquals(0, vm.state.value.selectedOfferIds.size)
        assertEquals(0, vm.state.value.comparisonTableOffers.size)

        // Select 2 specific lenders
        vm.toggleOfferSelection("state-bank-of-india-home-loan")
        vm.toggleOfferSelection("hdfc-bank-home-loan")

        assertEquals(2, vm.state.value.selectedOfferIds.size)
        assertEquals(2, vm.state.value.comparisonTableOffers.size)
        assertTrue(vm.state.value.comparisonTableOffers.any { it.offer.id == "state-bank-of-india-home-loan" })
        assertTrue(vm.state.value.comparisonTableOffers.any { it.offer.id == "hdfc-bank-home-loan" })

        // Deselect one
        vm.toggleOfferSelection("hdfc-bank-home-loan")
        assertEquals(1, vm.state.value.comparisonTableOffers.size)
    }

    @Test
    fun testOverdraftStructureDoesNotCalculateStandardEmi() {
        val vm = createViewModel()
        vm.onCategorySelected(LoanCategory.OVERDRAFT)

        val displayed = vm.state.value.displayedOffers
        assertEquals(30, displayed.size)

        // All overdraft offers must not compute an artificial monthly EMI
        for (offer in displayed) {
            assertNull("Overdraft must not calculate standard EMI", offer.installment)
            assertNotNull(offer.calculationNote)
            assertTrue(offer.calculationNote!!.contains("overdraft", ignoreCase = true))
        }
    }
}
