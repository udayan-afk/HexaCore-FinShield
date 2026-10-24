package com.finshield.app.model

import com.finshield.app.domain.calculator.InterestMethod
import com.finshield.app.domain.calculator.LoanCalculator
import com.finshield.app.domain.calculator.Money

enum class LoanCategory(val label: String, val shortLabel: String) {
    PERSONAL("Personal Loan", "Personal"),
    HOME("Home Loan", "Home"),
    EDUCATION("Education Loan", "Education"),
    CAR("Car Loan", "Car"),
    OTHER("Other Loans", "Other");
}

data class LoanOffer(
    val id: String,
    val lenderName: String,
    val offerName: String,
    val category: LoanCategory,
    val annualRatePercent: Double,
    val interestMethod: InterestMethod,
    val processingFeeFixed: Double,
    val processingFeePercent: Double = 0.0,
    val hasGst: Boolean = true,
    val minAmount: Double = 25_000.0,
    val maxAmount: Double = 10_000_000.0,
    val minTenureMonths: Int = 12,
    val maxTenureMonths: Int = 84,
    val eligibilityNotes: String = "Salaried, 21–58",
    val officialUrl: String? = null,
    val isVerifiedSource: Boolean = false,
) {
    val lenderInitial: String get() = lenderName.firstOrNull()?.uppercase() ?: "L"
}

data class ComputedLoanOffer(
    val offer: LoanOffer,
    val principal: Double,
    val tenureMonths: Int,
    val installment: Double,
    val totalRepayment: Double,
    val totalInterest: Double,
    val feeDisplay: String,
    val feeWithGst: Double,
    val totalCost: Double,
    val indicativeApr: Double,
    val isLowestCost: Boolean = false,
    val isSelectedForComparison: Boolean = false,
)

object OfferCalculator {
    fun compute(
        offer: LoanOffer,
        principal: Double,
        tenureMonths: Int,
    ): ComputedLoanOffer {
        val baseFee = offer.processingFeeFixed + (principal * (offer.processingFeePercent / 100.0))
        val feeWithGst = if (offer.hasGst) baseFee * 1.18 else baseFee

        val installment: Double
        val totalRepayment: Double
        val totalInterest: Double

        when (offer.interestMethod) {
            InterestMethod.REDUCING_BALANCE -> {
                val monthlyRate = (offer.annualRatePercent / 100.0) / 12.0
                installment = LoanCalculator.emi(principal, monthlyRate, tenureMonths)
                totalRepayment = installment * tenureMonths
                totalInterest = totalRepayment - principal
            }
            InterestMethod.FLAT -> {
                val years = tenureMonths / 12.0
                totalInterest = principal * (offer.annualRatePercent / 100.0) * years
                totalRepayment = principal + totalInterest
                installment = totalRepayment / tenureMonths
            }
        }

        val totalCost = totalRepayment + feeWithGst

        // Indicative APR calculation: annual rate + annualised fee impact
        val years = (tenureMonths / 12.0).coerceAtLeast(1.0)
        val feePercentage = if (principal > 0) (feeWithGst / principal) * 100.0 else 0.0
        val n = tenureMonths.toDouble()
        val feeFactor = if (n > 1) (2.0 * n) / (n + 1.0) else 1.0
        val indicativeApr = if (offer.interestMethod == InterestMethod.REDUCING_BALANCE) {
            offer.annualRatePercent + (feePercentage / years) * feeFactor
        } else {
            (offer.annualRatePercent * 1.8) + (feePercentage / years)
        }

        val feeDisplay = when {
            offer.processingFeeFixed > 0 && offer.processingFeePercent > 0 ->
                "${Money.inr(offer.processingFeeFixed)} + ${offer.processingFeePercent}%${if (offer.hasGst) " + GST" else ""}"
            offer.processingFeeFixed > 0 ->
                "${Money.inr(offer.processingFeeFixed)}${if (offer.hasGst) " + GST" else ""}"
            offer.processingFeePercent > 0 ->
                "${offer.processingFeePercent}%${if (offer.hasGst) " + GST" else ""}"
            else -> "Nil / Zero"
        }

        return ComputedLoanOffer(
            offer = offer,
            principal = principal,
            tenureMonths = tenureMonths,
            installment = installment,
            totalRepayment = totalRepayment,
            totalInterest = totalInterest,
            feeDisplay = feeDisplay,
            feeWithGst = feeWithGst,
            totalCost = totalCost,
            indicativeApr = indicativeApr,
        )
    }
}
