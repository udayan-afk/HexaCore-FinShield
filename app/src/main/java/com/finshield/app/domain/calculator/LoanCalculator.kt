package com.finshield.app.domain.calculator

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToLong

enum class InterestMethod(val label: String) { REDUCING_BALANCE("Reducing Balance"), FLAT("Flat Rate") }

enum class RepaymentFrequency(val label: String, val periodsPerYear: Int) {
    MONTHLY("Monthly", 12),
    QUARTERLY("Quarterly", 4),
    ANNUALLY("Annually", 1);

    val monthsPerPeriod: Int get() = 12 / periodsPerYear
    val installmentNoun: String get() = when (this) { MONTHLY -> "Monthly"; QUARTERLY -> "Quarterly"; ANNUALLY -> "Annual" }
}

data class LoanInput(
    val principal: Double,
    val annualRatePercent: Double,
    val tenureMonths: Int,
    val method: InterestMethod = InterestMethod.REDUCING_BALANCE,
    val frequency: RepaymentFrequency = RepaymentFrequency.MONTHLY,
)

data class LoanResult(
    val installment: Double,
    val numberOfInstallments: Int,
    val totalRepayment: Double,
    val totalInterest: Double,
    val principal: Double,
) {
    val principalShare: Double get() = if (totalRepayment > 0) principal / totalRepayment else 0.0
    val interestShare: Double get() = 1.0 - principalShare
}

data class FieldErrors(val principal: String? = null, val rate: String? = null, val tenure: String? = null) {
    val hasAny: Boolean get() = principal != null || rate != null || tenure != null
}

sealed interface Validation {
    data class Valid(val input: LoanInput) : Validation
    data class Invalid(val errors: FieldErrors) : Validation
}

/** Deterministic loan maths. No AI, no I/O. */
object LoanCalculator {
    const val MAX_PRINCIPAL = 1_000_000_000.0
    const val MAX_RATE_PERCENT = 100.0
    const val MAX_TENURE_MONTHS = 600

    /** Standard reducing-balance EMI: P·r·(1+r)^n / ((1+r)^n − 1), with r the periodic rate as a decimal. */
    fun emi(principal: Double, periodicRate: Double, periods: Int): Double {
        if (periodicRate == 0.0) return principal / periods
        val growth = (1 + periodicRate).pow(periods)
        return principal * periodicRate * growth / (growth - 1)
    }

    /** Computes the result for an already-validated input. Throws IllegalArgumentException on invalid values. */
    fun calculate(input: LoanInput): LoanResult {
        require(input.principal > 0 && input.principal.isFinite()) { "Principal must be positive" }
        require(input.annualRatePercent >= 0 && input.annualRatePercent.isFinite()) { "Rate must not be negative" }
        require(input.tenureMonths > 0) { "Tenure must be positive" }
        val perPeriod = input.frequency.monthsPerPeriod
        require(input.tenureMonths % perPeriod == 0) { "Tenure must be a multiple of $perPeriod months" }

        val n = input.tenureMonths / perPeriod
        val installment: Double
        val total: Double
        when (input.method) {
            InterestMethod.REDUCING_BALANCE -> {
                val r = input.annualRatePercent / 100.0 / input.frequency.periodsPerYear
                installment = emi(input.principal, r, n)
                total = installment * n
            }
            InterestMethod.FLAT -> {
                val interest = input.principal * (input.annualRatePercent / 100.0) * (input.tenureMonths / 12.0)
                total = input.principal + interest
                installment = total / n
            }
        }
        return LoanResult(installment, n, total, total - input.principal, input.principal)
    }

    /** Parses and validates raw text fields. Rejects negatives, zero, non-numbers and out-of-range values. */
    fun validate(
        principalText: String,
        rateText: String,
        tenureText: String,
        tenureInYears: Boolean,
        method: InterestMethod,
        frequency: RepaymentFrequency,
    ): Validation {
        val principal = principalText.replace(",", "").trim().toDoubleOrNull()
        val rate = rateText.trim().toDoubleOrNull()
        val tenureNumber = tenureText.trim().toDoubleOrNull()

        val principalError = when {
            principal == null || !principal.isFinite() -> "Enter the loan amount"
            principal <= 0 -> "Amount must be greater than zero"
            principal > MAX_PRINCIPAL -> "Amount is too large"
            else -> null
        }
        val rateError = when {
            rate == null || !rate.isFinite() -> "Enter the interest rate"
            rate < 0 -> "Rate cannot be negative"
            rate > MAX_RATE_PERCENT -> "Rate cannot exceed ${MAX_RATE_PERCENT.toInt()}%"
            else -> null
        }
        var months = 0
        val tenureError = when {
            tenureNumber == null || !tenureNumber.isFinite() -> "Enter the tenure"
            tenureNumber <= 0 -> "Tenure must be greater than zero"
            else -> {
                val m = if (tenureInYears) tenureNumber * 12 else tenureNumber
                val rounded = m.roundToLong()
                when {
                    abs(m - rounded) > 1e-9 -> "Tenure must be a whole number of months"
                    rounded > MAX_TENURE_MONTHS -> "Maximum tenure is ${MAX_TENURE_MONTHS / 12} years"
                    rounded % frequency.monthsPerPeriod != 0L ->
                        "Tenure must be a multiple of ${frequency.monthsPerPeriod} months for ${frequency.label.lowercase()} repayment"
                    else -> { months = rounded.toInt(); null }
                }
            }
        }
        val errors = FieldErrors(principalError, rateError, tenureError)
        return if (errors.hasAny) Validation.Invalid(errors)
        else Validation.Valid(LoanInput(principal!!, rate!!, months, method, frequency))
    }
}
