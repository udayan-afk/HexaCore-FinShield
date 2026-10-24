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

    /**
     * Calculates the exact outstanding principal balance remaining at the end of [monthNumber].
     */
    fun outstandingBalanceAfter(
        principal: Double,
        annualRatePercent: Double,
        tenureMonths: Int,
        monthNumber: Int,
        method: InterestMethod = InterestMethod.REDUCING_BALANCE,
    ): Double {
        require(monthNumber >= 0) { "Month must be non-negative" }
        if (monthNumber == 0) return principal
        if (monthNumber >= tenureMonths) return 0.0

        return when (method) {
            InterestMethod.REDUCING_BALANCE -> {
                val r = (annualRatePercent / 100.0) / 12.0
                if (r == 0.0) {
                    val emi = principal / tenureMonths
                    (principal - emi * monthNumber).coerceAtLeast(0.0)
                } else {
                    val emi = emi(principal, r, tenureMonths)
                    var balance = principal
                    for (m in 1..monthNumber) {
                        val interest = balance * r
                        val principalPaid = emi - interest
                        balance -= principalPaid
                        if (balance <= 0.0) return 0.0
                    }
                    balance.coerceAtLeast(0.0)
                }
            }
            InterestMethod.FLAT -> {
                (principal * (1.0 - monthNumber.toDouble() / tenureMonths)).coerceAtLeast(0.0)
            }
        }
    }

    /**
     * Computes net disbursal: Sanctioned Principal minus all upfront deductions explicitly deducted at disbursal.
     */
    fun calculateNetDisbursal(
        sanctionedPrincipal: Double,
        upfrontDeductions: Double,
    ): Double = (sanctionedPrincipal - upfrontDeductions).coerceAtLeast(0.0)

    /**
     * Computes processing fee and taxes.
     */
    fun calculateProcessingFee(
        principal: Double,
        percentage: Double?,
        fixedAmount: Double?,
        gstRatePercent: Double? = 18.0,
        isGstAlreadyIncluded: Boolean = false,
    ): ProcessingFeeResult {
        val rawFee = when {
            fixedAmount != null -> fixedAmount
            percentage != null -> principal * (percentage / 100.0)
            else -> 0.0
        }
        val rate = gstRatePercent ?: 0.0
        val gst = if (isGstAlreadyIncluded || rate == 0.0) 0.0 else rawFee * (rate / 100.0)
        val total = if (isGstAlreadyIncluded) rawFee else rawFee + gst
        val stated = when {
            percentage != null -> "$percentage%"
            fixedAmount != null -> Money.inr(fixedAmount)
            else -> "—"
        }
        return ProcessingFeeResult(
            feeAmount = rawFee,
            gstRatePercent = rate,
            gstAmount = gst,
            totalWithGst = total,
            rateStated = stated,
            isGstIncluded = isGstAlreadyIncluded,
        )
    }

    /**
     * Computes percentage-based foreclosure charge with GST for a known outstanding balance.
     */
    fun calculateForeclosure(
        outstandingPrincipal: Double,
        feePercent: Double,
        gstRatePercent: Double = 18.0,
        isGstAssumed: Boolean = true,
    ): ForeclosureCalculation {
        val fee = outstandingPrincipal * (feePercent / 100.0)
        val gst = fee * (gstRatePercent / 100.0)
        return ForeclosureCalculation(
            outstandingPrincipal = outstandingPrincipal,
            feePercent = feePercent,
            feeAmount = fee,
            gstRatePercent = gstRatePercent,
            gstAmount = gst,
            totalCost = fee + gst,
            isGstAssumed = isGstAssumed,
        )
    }

    /**
     * Illustrative late payment penalty calculation.
     */
    fun calculateIllustrativeLateFee(
        emi: Double,
        monthlyPenalRate: Double = 2.0,
        fixedBounceFee: Double = 500.0,
        monthsDelayed: Int = 1,
    ): LatePaymentEstimate {
        val interest = emi * (monthlyPenalRate / 100.0) * monthsDelayed
        return LatePaymentEstimate(
            overdueAmount = emi,
            monthsDelayed = monthsDelayed,
            penalRatePercentPerMonth = monthlyPenalRate,
            penalInterest = interest,
            fixedBounceFee = fixedBounceFee,
            totalLateFee = interest + fixedBounceFee,
        )
    }
}

data class ProcessingFeeResult(
    val feeAmount: Double,
    val gstRatePercent: Double,
    val gstAmount: Double,
    val totalWithGst: Double,
    val rateStated: String,
    val isGstIncluded: Boolean,
)

data class ForeclosureCalculation(
    val outstandingPrincipal: Double,
    val feePercent: Double,
    val feeAmount: Double,
    val gstRatePercent: Double,
    val gstAmount: Double,
    val totalCost: Double,
    val isGstAssumed: Boolean,
)

data class LatePaymentEstimate(
    val overdueAmount: Double,
    val monthsDelayed: Int,
    val penalRatePercentPerMonth: Double,
    val penalInterest: Double,
    val fixedBounceFee: Double,
    val totalLateFee: Double,
)

data class CalculatorPrefill(
    val principal: Double?,
    val rate: Double?,
    val tenureMonths: Int?,
    val method: InterestMethod? = null,
)

object CalculatorPrefillStore {
    private var pending: CalculatorPrefill? = null

    fun set(prefill: CalculatorPrefill) {
        pending = prefill
    }

    fun consume(): CalculatorPrefill? {
        val curr = pending
        pending = null
        return curr
    }
}

