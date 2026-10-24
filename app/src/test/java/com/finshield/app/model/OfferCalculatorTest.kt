package com.finshield.app.model

import com.finshield.app.data.comparison.DemoLoanOfferRepository
import com.finshield.app.domain.calculator.InterestMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class OfferCalculatorTest {

    @Test
    fun testAsterFinanceReducingBalanceCalculations() {
        val aster = DemoLoanOfferRepository.SAMPLE_OFFERS.first { it.id == "aster-personal" }
        val computed = OfferCalculator.compute(aster, 500_000.0, 36)

        // Mathematical reducing-balance EMI ~ ₹16,547.52
        assertEquals(16547.52, computed.installment, 0.1)

        // Total repayment = EMI * 36 = ~₹595,710.57
        assertEquals(595710.57, computed.totalRepayment, 0.1)

        // Total fee with 18% GST = 7,500 * 1.18 = 8,850.00
        assertEquals(8850.0, computed.feeWithGst, 0.01)

        // Total cost = 595,710.57 + 8,850.00 = ~₹604,560.57
        assertEquals(604560.57, computed.totalCost, 0.1)

        // Indicative APR
        assertTrue(computed.indicativeApr > 12.0)
    }

    @Test
    fun testMetroBankReducingBalanceCalculations() {
        val metro = DemoLoanOfferRepository.SAMPLE_OFFERS.first { it.id == "metro-personal" }
        val computed = OfferCalculator.compute(metro, 500_000.0, 36)

        // Fee with GST = 5,000 * 1.18 = 5,900.00
        assertEquals(5900.0, computed.feeWithGst, 0.01)

        // Total cost ~ ₹605,909.14
        assertEquals(605909.14, computed.totalCost, 0.1)
    }

    @Test
    fun testSwiftCreditFlatRateCalculations() {
        val swift = DemoLoanOfferRepository.SAMPLE_OFFERS.first { it.id == "swift-personal" }
        val computed = OfferCalculator.compute(swift, 500_000.0, 36)

        // Flat interest: 500,000 * 10.5% * 3 years = 157,500.00
        assertEquals(157500.0, computed.totalInterest, 0.01)

        // Total repayment = 500,000 + 157,500 = 657,500.00
        assertEquals(657500.0, computed.totalRepayment, 0.01)

        // Fee with GST = 2,500 * 1.18 = 2,950.00
        assertEquals(2950.0, computed.feeWithGst, 0.01)

        // Total cost = 657,500 + 2,950 = 660,450.00
        assertEquals(660450.0, computed.totalCost, 0.01)
    }

    @Test
    fun testLowestCostSelection() {
        val p = 500_000.0
        val t = 36
        val offers = DemoLoanOfferRepository.SAMPLE_OFFERS.filter { it.category == LoanCategory.PERSONAL }
        val computed = offers.map { OfferCalculator.compute(it, p, t) }
        val minCost = computed.minOf { it.totalCost }

        // Aster Finance has the lowest total cost among personal loan samples
        val best = computed.first { it.totalCost == minCost }
        assertEquals("aster-personal", best.offer.id)
        assertTrue(best.totalCost < 605000.0)
    }
}
