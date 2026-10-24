package com.finshield.app.domain.comparison

import com.finshield.app.domain.calculator.InterestMethod
import com.finshield.app.domain.calculator.LoanCalculator
import com.finshield.app.domain.calculator.LoanInput
import com.finshield.app.domain.calculator.LoanResult
import com.finshield.app.domain.calculator.RepaymentFrequency
import com.finshield.app.domain.calculator.Validation

/** Raw, editable text for one offer. */
data class OfferDraft(val id: Long, val name: String, val rateText: String, val feeText: String)

data class OfferEvaluation(
    val draft: OfferDraft,
    val result: LoanResult?,
    val knownFee: Double,
    /** Total repayment + the known fee entered by the user. Taxes, insurance and other charges are not included. */
    val totalCost: Double?,
    val rateError: String?,
    val feeError: String?,
)

data class ComparisonResult(
    val principalError: String?,
    val tenureError: String?,
    val offers: List<OfferEvaluation>,
    /** Offer with the lowest total cost, only when at least two offers are valid. */
    val lowestCostId: Long?,
)

/** Compares offers on the same principal and tenure using the reducing-balance monthly formula. */
object LoanComparison {
    const val MIN_OFFERS = 2
    const val MAX_OFFERS = 5
    const val MAX_FEE = 100_000_000.0

    fun evaluate(principalText: String, tenureMonthsText: String, offers: List<OfferDraft>): ComparisonResult {
        val shared = LoanCalculator.validate(
            principalText, "0", tenureMonthsText, false, InterestMethod.REDUCING_BALANCE, RepaymentFrequency.MONTHLY,
        )
        val principalError = (shared as? Validation.Invalid)?.errors?.principal
        val tenureError = (shared as? Validation.Invalid)?.errors?.tenure
        val base = (shared as? Validation.Valid)?.input

        val evaluations = offers.map { draft ->
            val rate = draft.rateText.trim().toDoubleOrNull()
            val rateError = when {
                rate == null || !rate.isFinite() -> "Enter the interest rate"
                rate < 0 -> "Rate cannot be negative"
                rate > LoanCalculator.MAX_RATE_PERCENT -> "Rate cannot exceed ${LoanCalculator.MAX_RATE_PERCENT.toInt()}%"
                else -> null
            }
            val feeRaw = draft.feeText.replace(",", "").trim()
            val fee = if (feeRaw.isEmpty()) 0.0 else feeRaw.toDoubleOrNull()
            val feeError = when {
                fee == null || !fee.isFinite() -> "Enter a valid fee amount"
                fee < 0 -> "Fee cannot be negative"
                fee > MAX_FEE -> "Fee is too large"
                else -> null
            }
            val result = if (base != null && rateError == null && rate != null) {
                LoanCalculator.calculate(LoanInput(base.principal, rate, base.tenureMonths))
            } else null
            val knownFee = if (feeError == null && fee != null) fee else 0.0
            OfferEvaluation(
                draft = draft,
                result = result,
                knownFee = knownFee,
                totalCost = if (result != null && feeError == null) result.totalRepayment + knownFee else null,
                rateError = rateError,
                feeError = feeError,
            )
        }
        val valid = evaluations.filter { it.totalCost != null }
        val best = if (valid.size >= MIN_OFFERS) valid.minByOrNull { it.totalCost!! }?.draft?.id else null
        return ComparisonResult(principalError, tenureError, evaluations, best)
    }
}
