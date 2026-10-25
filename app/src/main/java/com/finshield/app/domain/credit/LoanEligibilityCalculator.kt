package com.finshield.app.domain.credit

import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * Independent Loan Eligibility Calculator using standard Indian banking FOIR
 * (Fixed Obligation to Income Ratio) and reducing-balance EMI calculation principles.
 */
object LoanEligibilityCalculator {

    /**
     * Standard reducing-balance EMI formula:
     * EMI = P * r * (1 + r)^n / ((1 + r)^n - 1)
     *
     * where:
     * - P = Principal loan amount
     * - r = Monthly interest rate (annual interest rate / 12 / 100)
     * - n = Number of monthly installments (tenure in months)
     */
    fun calculateReducingBalanceEmi(
        principal: Double,
        annualRatePercent: Double,
        tenureMonths: Int,
    ): Double {
        if (principal <= 0.0 || tenureMonths <= 0) return 0.0
        if (!principal.isFinite() || !annualRatePercent.isFinite()) return 0.0

        val r = (annualRatePercent / 12.0) / 100.0
        if (r <= 0.0) {
            // Zero interest loan: EMI is simply principal divided by tenure
            return principal / tenureMonths.toDouble()
        }

        val factor = (1.0 + r).pow(tenureMonths.toDouble())
        if (!factor.isFinite() || factor <= 1.0) {
            return principal / tenureMonths.toDouble()
        }

        val emi = (principal * r * factor) / (factor - 1.0)
        return if (emi.isFinite() && emi > 0.0) emi else 0.0
    }

    /**
     * Evaluates loan eligibility and repayment capacity based on:
     * - Monthly net income
     * - Existing EMI commitments
     * - New loan EMI requirement
     * - Calculated Fixed Obligation to Income Ratio (FOIR)
     * - Credit score profile (when provided)
     */
    fun evaluate(input: LoanEligibilityInput): LoanEligibilityResult {
        val income = if (input.monthlyIncome.isFinite() && input.monthlyIncome > 0) input.monthlyIncome else 0.0
        val existingEmi = if (input.existingMonthlyEmi.isFinite() && input.existingMonthlyEmi >= 0) input.existingMonthlyEmi else 0.0

        val newEmi = calculateReducingBalanceEmi(
            principal = input.requestedLoanAmount.coerceAtLeast(0.0),
            annualRatePercent = input.expectedAnnualInterestRate.coerceAtLeast(0.0),
            tenureMonths = input.requestedTenureMonths.coerceAtLeast(1),
        )

        val totalEmi = existingEmi + newEmi

        // Guard against missing or zero income
        if (income <= 0.0) {
            return LoanEligibilityResult(
                monthlyIncome = 0.0,
                existingMonthlyEmi = existingEmi,
                estimatedNewEmi = newEmi,
                totalMonthlyEmiObligation = totalEmi,
                foirPercent = 0.0,
                remainingMonthlyCapacity = 0.0,
                status = EligibilityStatus.MORE_INFO_NEEDED,
                summaryExplanation = "Enter your net monthly income to evaluate your debt-to-income (FOIR) capacity and borrowing affordability.",
                keyDecisionFactors = defaultDecisionFactors(input.creditScore),
            )
        }

        val foirPercent = (totalEmi / income) * 100.0
        val remainingCapacity = income - totalEmi

        val status: EligibilityStatus
        val explanation: String

        // Standard Indian banking FOIR benchmark thresholds
        when {
            foirPercent <= 45.0 -> {
                status = EligibilityStatus.AFFORDABLE
                val emiSharePct = foirPercent.roundToLong()
                explanation = "Your estimated total EMI burden ($emiSharePct% of net income) is within the standard 40%–50% bank benchmark, leaving ₹${formatRounded(remainingCapacity)} for living expenses and emergencies."
            }
            foirPercent <= 60.0 -> {
                status = EligibilityStatus.REVIEW_BUDGET
                val emiSharePct = foirPercent.roundToLong()
                explanation = "Your total monthly EMI obligations would consume $emiSharePct% of your income. Lenders typically prefer FOIR under 50% and may require a co-applicant, lower loan amount, or longer tenure."
            }
            else -> {
                status = EligibilityStatus.HIGH_BURDEN
                val emiSharePct = foirPercent.roundToLong()
                explanation = "Total EMI obligations ($emiSharePct% of income) exceed comfortable debt thresholds. Approvals may be difficult unless you clear existing debts or extend the tenure."
            }
        }

        return LoanEligibilityResult(
            monthlyIncome = income,
            existingMonthlyEmi = existingEmi,
            estimatedNewEmi = newEmi,
            totalMonthlyEmiObligation = totalEmi,
            foirPercent = foirPercent,
            remainingMonthlyCapacity = remainingCapacity,
            status = status,
            summaryExplanation = explanation,
            keyDecisionFactors = defaultDecisionFactors(input.creditScore),
        )
    }

    private fun formatRounded(amount: Double): String {
        val rounded = amount.roundToLong()
        return if (rounded < 0) "-₹${kotlin.math.abs(rounded)}" else "$rounded"
    }

    private fun defaultDecisionFactors(creditScore: Int?): List<String> {
        val scoreFactor = if (creditScore != null && creditScore >= 300) {
            "Credit Score: Verified bureau score of $creditScore gives lenders clear history on your repayment track record."
        } else {
            "Credit Score: Official CIBIL score (750+ preferred for best interest rates and fast approvals)."
        }

        return listOf(
            "FOIR (Fixed Obligation to Income Ratio): Most banks restrict total EMIs to 40%–50% of monthly net income.",
            scoreFactor,
            "Employment & Income Stability: Salaried with consistent service or self-employed with filed ITRs.",
            "Existing Loan Inquiries: Multiple recent credit card or loan inquiries can indicate credit hunger.",
            "Loan-to-Value (LTV) / Collateral: Depending on whether the loan is secured or unsecured.",
        )
    }
}
