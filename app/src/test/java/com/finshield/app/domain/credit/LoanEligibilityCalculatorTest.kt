package com.finshield.app.domain.credit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LoanEligibilityCalculatorTest {

    @Test
    fun calculateReducingBalanceEmi_standardLoan() {
        // Principal 5,00,000, 12% p.a., 36 months
        // Monthly rate r = 0.01
        // EMI = 500000 * 0.01 * (1.01)^36 / ((1.01)^36 - 1) ≈ 16607.19
        val emi = LoanEligibilityCalculator.calculateReducingBalanceEmi(
            principal = 500000.0,
            annualRatePercent = 12.0,
            tenureMonths = 36,
        )
        assertTrue("EMI should be around 16607, got $emi", emi in 16600.0..16615.0)
    }

    @Test
    fun calculateReducingBalanceEmi_zeroInterest() {
        val emi = LoanEligibilityCalculator.calculateReducingBalanceEmi(
            principal = 120000.0,
            annualRatePercent = 0.0,
            tenureMonths = 12,
        )
        assertEquals(10000.0, emi, 0.001)
    }

    @Test
    fun calculateReducingBalanceEmi_invalidInputs() {
        assertEquals(0.0, LoanEligibilityCalculator.calculateReducingBalanceEmi(0.0, 10.0, 12), 0.001)
        assertEquals(0.0, LoanEligibilityCalculator.calculateReducingBalanceEmi(50000.0, 10.0, 0), 0.001)
        assertEquals(0.0, LoanEligibilityCalculator.calculateReducingBalanceEmi(Double.NaN, 10.0, 12), 0.001)
    }

    @Test
    fun evaluate_missingIncome_returnsMoreInfoNeeded() {
        val input = LoanEligibilityInput(
            monthlyIncome = 0.0,
            existingMonthlyEmi = 5000.0,
            requestedLoanAmount = 200000.0,
            requestedTenureMonths = 24,
            expectedAnnualInterestRate = 11.0,
        )
        val result = LoanEligibilityCalculator.evaluate(input)
        assertEquals(EligibilityStatus.MORE_INFO_NEEDED, result.status)
    }

    @Test
    fun evaluate_affordableFoir_returnsAffordable() {
        // Monthly income 1,00,000, existing EMI 10,000, loan 3,00,000 for 36 months @ 10%
        // new EMI is approx 9,680 => Total EMI 19,680 => FOIR ~19.7% <= 45%
        val input = LoanEligibilityInput(
            monthlyIncome = 100000.0,
            existingMonthlyEmi = 10000.0,
            requestedLoanAmount = 300000.0,
            requestedTenureMonths = 36,
            expectedAnnualInterestRate = 10.0,
        )
        val result = LoanEligibilityCalculator.evaluate(input)
        assertEquals(EligibilityStatus.AFFORDABLE, result.status)
        assertTrue(result.remainingMonthlyCapacity > 70000.0)
    }

    @Test
    fun evaluate_highFoir_returnsHighBurden() {
        // Monthly income 50,000, existing EMI 20,000, loan 5,00,000 for 24 months @ 14%
        // new EMI is approx 24,000 => Total EMI 44,000 => FOIR ~88% > 60%
        val input = LoanEligibilityInput(
            monthlyIncome = 50000.0,
            existingMonthlyEmi = 20000.0,
            requestedLoanAmount = 500000.0,
            requestedTenureMonths = 24,
            expectedAnnualInterestRate = 14.0,
        )
        val result = LoanEligibilityCalculator.evaluate(input)
        assertEquals(EligibilityStatus.HIGH_BURDEN, result.status)
    }
}
