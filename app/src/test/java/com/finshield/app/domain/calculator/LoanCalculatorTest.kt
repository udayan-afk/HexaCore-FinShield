package com.finshield.app.domain.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LoanCalculatorTest {
    @Test
    fun reducingBalance_matchesKnownEmi() {
        val r = LoanCalculator.calculate(LoanInput(500_000.0, 12.5, 36))
        assertEquals(16_727.0, r.installment, 0.5)
        assertEquals(36, r.numberOfInstallments)
        assertEquals(r.installment * 36, r.totalRepayment, 1e-6)
        assertEquals(r.totalRepayment - 500_000.0, r.totalInterest, 1e-6)
        assertEquals("₹16,727", Money.inr(r.installment))
        assertEquals("₹6,02,165", Money.inr(r.totalRepayment))
    }

    @Test
    fun zeroInterest_isPrincipalDividedByPeriods() {
        val r = LoanCalculator.calculate(LoanInput(120_000.0, 0.0, 12))
        assertEquals(10_000.0, r.installment, 1e-9)
        assertEquals(0.0, r.totalInterest, 1e-9)
        assertEquals(120_000.0, r.totalRepayment, 1e-9)
    }

    @Test
    fun flatRate_interestIsSimple() {
        val r = LoanCalculator.calculate(LoanInput(100_000.0, 10.0, 24, InterestMethod.FLAT))
        assertEquals(20_000.0, r.totalInterest, 1e-9)
        assertEquals(120_000.0, r.totalRepayment, 1e-9)
        assertEquals(120_000.0 / 24, r.installment, 1e-9)
    }

    @Test
    fun quarterly_requiresMultipleOfThree() {
        val v = LoanCalculator.validate("100000", "10", "10", false, InterestMethod.REDUCING_BALANCE, RepaymentFrequency.QUARTERLY)
        assertTrue(v is Validation.Invalid)
        val ok = LoanCalculator.validate("100000", "10", "12", false, InterestMethod.REDUCING_BALANCE, RepaymentFrequency.QUARTERLY)
        assertTrue(ok is Validation.Valid)
    }

    @Test
    fun validation_rejectsBadInput() {
        fun bad(p: String, r: String, t: String) =
            LoanCalculator.validate(p, r, t, false, InterestMethod.REDUCING_BALANCE, RepaymentFrequency.MONTHLY) is Validation.Invalid
        assertTrue(bad("-5", "10", "12"))
        assertTrue(bad("0", "10", "12"))
        assertTrue(bad("1000", "-1", "12"))
        assertTrue(bad("1000", "10", "0"))
        assertTrue(bad("1000", "10", "abc"))
        assertTrue(bad("1000", "10", "12.5"))
        assertTrue(bad("1000", "NaN", "12"))
    }

    @Test
    fun yearsConvertToMonths() {
        val v = LoanCalculator.validate("500000", "12.5", "3", true, InterestMethod.REDUCING_BALANCE, RepaymentFrequency.MONTHLY)
        assertEquals(36, (v as Validation.Valid).input.tenureMonths)
    }

    @Test
    fun indianGrouping() {
        assertEquals("₹0", Money.inr(0.0))
        assertEquals("₹999", Money.inr(999.0))
        assertEquals("₹1,000", Money.inr(1000.0))
        assertEquals("₹1,00,000", Money.inr(100000.0))
        assertEquals("₹1,02,165", Money.inr(102165.4))
        assertEquals("₹12,34,56,789", Money.inr(123456789.0))
    }

    @Test
    fun indianGrouping_withDecimals() {
        assertEquals("₹5,00,000.00", Money.inr(500_000.0, 2))
        assertEquals("₹5,00,000.00", Money.inr2(500_000.0))
        assertEquals("₹16,727.48", Money.inr(16727.48, 2))
        assertEquals("-₹5,00,000.00", Money.inr(-500_000.0, 2))
        assertEquals("-₹1,000", Money.inr(-1000.0))
        assertEquals("₹0.00", Money.inr(0.0, 2))
    }

    @Test
    fun processingFeeCalculations_percentageAndFixed() {
        // 2% on 5,00,000 = 10,000 fee
        val pctResult = LoanCalculator.calculateProcessingFee(500_000.0, 2.0, null, gstRatePercent = 18.0)
        assertEquals(10_000.0, pctResult.feeAmount, 0.01)
        assertEquals(1_800.0, pctResult.gstAmount, 0.01)
        assertEquals(11_800.0, pctResult.totalWithGst, 0.01)

        // Fixed 5,000 fee + 18% GST = 5,900
        val fixedResult = LoanCalculator.calculateProcessingFee(500_000.0, null, 5_000.0, gstRatePercent = 18.0)
        assertEquals(5_000.0, fixedResult.feeAmount, 0.01)
        assertEquals(900.0, fixedResult.gstAmount, 0.01)
        assertEquals(5_900.0, fixedResult.totalWithGst, 0.01)
    }

    @Test
    fun gstCalculation_whenRateExplicitlySupplied() {
        val result12Gst = LoanCalculator.calculateProcessingFee(100_000.0, 1.0, null, gstRatePercent = 12.0)
        assertEquals(1_000.0, result12Gst.feeAmount, 0.01)
        assertEquals(120.0, result12Gst.gstAmount, 0.01)
        assertEquals(1_120.0, result12Gst.totalWithGst, 0.01)

        val result0Gst = LoanCalculator.calculateProcessingFee(100_000.0, 1.0, null, gstRatePercent = 0.0)
        assertEquals(1_000.0, result0Gst.feeAmount, 0.01)
        assertEquals(0.0, result0Gst.gstAmount, 0.01)
        assertEquals(1_000.0, result0Gst.totalWithGst, 0.01)
    }

    @Test
    fun preventingDuplicateChargesAndDoubleCountedTaxes() {
        // When fee already includes GST, gstAmount should be 0 and total should not add GST a second time
        val inclusiveResult = LoanCalculator.calculateProcessingFee(
            principal = 500_000.0,
            percentage = 2.0,
            fixedAmount = null,
            gstRatePercent = 18.0,
            isGstAlreadyIncluded = true,
        )
        assertEquals(10_000.0, inclusiveResult.feeAmount, 0.01)
        assertEquals(0.0, inclusiveResult.gstAmount, 0.01)
        assertEquals(10_000.0, inclusiveResult.totalWithGst, 0.01)
        assertTrue(inclusiveResult.isGstIncluded)
    }

    @Test
    fun netDisbursalAfterSupportedUpfrontDeductions() {
        val principal = 500_000.0
        val totalUpfrontDeductions = 11_800.0 // 10,000 fee + 1,800 GST
        val netDisbursed = LoanCalculator.calculateNetDisbursal(principal, totalUpfrontDeductions)
        assertEquals(488_200.0, netDisbursed, 0.01)

        // Zero deductions
        val fullDisbursal = LoanCalculator.calculateNetDisbursal(principal, 0.0)
        assertEquals(500_000.0, fullDisbursal, 0.01)
    }

    @Test
    fun totalInterestAndRepayment() {
        val input = LoanInput(principal = 300_000.0, annualRatePercent = 10.5, tenureMonths = 24)
        val result = LoanCalculator.calculate(input)
        assertEquals(24, result.numberOfInstallments)
        assertEquals(result.principal + result.totalInterest, result.totalRepayment, 1e-6)
        assertTrue(result.totalInterest > 0)
        assertTrue(result.installment > 0)
    }

    @Test
    fun percentageBasedForeclosureChargeWithKnownOutstandingBalance() {
        val balance = 346_878.0
        val feePercent = 4.0
        val gstRate = 18.0
        val foreclosure = LoanCalculator.calculateForeclosure(balance, feePercent, gstRate, isGstAssumed = false)

        assertEquals(balance, foreclosure.outstandingPrincipal, 0.01)
        assertEquals(13_875.12, foreclosure.feeAmount, 0.01)
        assertEquals(2_497.52, foreclosure.gstAmount, 0.01)
        assertEquals(16_372.64, foreclosure.totalCost, 0.01)
        assertFalse(foreclosure.isGstAssumed)
    }

    @Test
    fun amortisationOutstandingBalanceDecreasesMonotonically() {
        val principal = 500_000.0
        val rate = 12.0
        val tenure = 36

        val bal0 = LoanCalculator.outstandingBalanceAfter(principal, rate, tenure, 0)
        val bal12 = LoanCalculator.outstandingBalanceAfter(principal, rate, tenure, 12)
        val bal24 = LoanCalculator.outstandingBalanceAfter(principal, rate, tenure, 24)
        val bal36 = LoanCalculator.outstandingBalanceAfter(principal, rate, tenure, 36)

        assertEquals(500_000.0, bal0, 0.01)
        assertTrue(bal12 < bal0)
        assertTrue(bal24 < bal12)
        assertEquals(0.0, bal36, 0.01)
    }
}
