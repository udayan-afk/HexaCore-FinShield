package com.finshield.app.model

import com.finshield.app.domain.calculator.InterestMethod

/**
 * Domain representation of a lender entry extracted from research datasets (e.g. lenders.csv).
 */
data class LenderRecord(
    val id: String,
    val lenderName: String,
    val lenderType: LenderClassification,
    val rawLenderType: String,
    val loanCategory: LoanCategory = LoanCategory.PERSONAL,
    val publishedInterestRate: String,
    val minAnnualRate: Double?,
    val maxAnnualRate: Double?,
    val isMonthlyRate: Boolean,
    val monthlyRatePercent: Double? = null,
    val isComparableStartingRate: Boolean,
    val rawMaxLoanAmount: String,
    val parsedMaxLoanAmount: Double?,
    val maxTenureMonths: Int,
    val publishedProcessingFee: String,
    val feePercentEstimate: Double? = null,
    val fixedFeeEstimate: Double? = null,
    val eligibilityNotes: String,
    val sourceUrl: String,
    val sourceAsOf: String,
    val datasetStatus: String,
    val datasetVersion: String = "2026.10-research",
) {
    val lenderInitial: String get() = lenderName.firstOrNull()?.uppercase() ?: "L"

    /**
     * Converts to a LoanOffer for integration with existing calculator / comparison models.
     */
    fun toLoanOffer(): LoanOffer {
        val rate = minAnnualRate ?: 0.0
        return LoanOffer(
            id = id,
            lenderName = lenderName,
            offerName = if (isMonthlyRate) "Digital Credit Line" else "Personal Loan",
            category = loanCategory,
            annualRatePercent = rate,
            interestMethod = InterestMethod.REDUCING_BALANCE,
            processingFeeFixed = fixedFeeEstimate ?: 0.0,
            processingFeePercent = feePercentEstimate ?: 0.0,
            hasGst = true,
            minAmount = 10_000.0,
            maxAmount = parsedMaxLoanAmount ?: 5_000_000.0,
            minTenureMonths = 3,
            maxTenureMonths = maxTenureMonths,
            eligibilityNotes = eligibilityNotes,
            officialUrl = sourceUrl,
            isVerifiedSource = false,
            lenderClassification = lenderType,
            publishedInterestRate = publishedInterestRate,
            isMonthlyRate = isMonthlyRate,
            monthlyRatePercent = monthlyRatePercent,
            isComparableStartingRate = isComparableStartingRate,
            rawMaxLoanAmount = rawMaxLoanAmount,
            sourceAsOf = sourceAsOf,
            datasetStatus = datasetStatus,
            datasetVersion = datasetVersion,
            publishedProcessingFee = publishedProcessingFee,
            minAnnualRate = minAnnualRate,
            maxAnnualRate = maxAnnualRate,
        )
    }
}
