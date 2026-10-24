package com.finshield.app.domain.calculator

import org.junit.Assert.assertEquals
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
    }

    @Test
    fun flatRate_interestIsSimple() {
        val r = LoanCalculator.calculate(LoanInput(100_000.0, 10.0, 24, InterestMethod.FLAT))
        assertEquals(20_000.0, r.totalInterest, 1e-9)
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
}
