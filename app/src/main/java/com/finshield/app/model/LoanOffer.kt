package com.finshield.app.model

import com.finshield.app.domain.calculator.InterestMethod
import com.finshield.app.domain.calculator.LoanCalculator
import com.finshield.app.domain.calculator.Money

enum class LoanCategory(
    val label: String,
    val shortLabel: String,
    val isPopular: Boolean = false,
) {
    PERSONAL("Personal Loan", "Personal", isPopular = true),
    HOME("Home Loan", "Home", isPopular = true),
    EDUCATION("Education Loan", "Education", isPopular = true),
    CAR("Car / Auto Loan", "Car / Auto", isPopular = true),
    TWO_WHEELER("Two-Wheeler Loan", "Two-Wheeler", isPopular = true),
    GOLD("Gold Loan", "Gold", isPopular = true),
    BUSINESS("Business / MSME Loan", "Business / MSME", isPopular = true),
    PROPERTY("Loan Against Property / Mortgage", "Against Property", isPopular = true),
    AGRICULTURE("Agriculture / Kisan Loan", "Agriculture / Kisan"),
    COMMERCIAL_VEHICLE("Commercial Vehicle Loan", "Commercial Vehicle"),
    CONSUMER_DURABLE("Consumer Durable / EMI Loan", "Consumer Durable"),
    MEDICAL("Medical / Healthcare Loan", "Medical / Healthcare"),
    SECURITIES("Loan Against Securities / Deposits", "Against Securities"),
    FIXED_DEPOSIT("Loan Against Fixed Deposit", "Against Fixed Deposit"),
    INSURANCE_POLICY("Loan Against Insurance Policy", "Against Insurance"),
    PROVIDENT_FUND("Loan Against Provident Fund / Retirement Benefit", "Against PF"),
    RENEWABLE_ENERGY("Loan for Renewable Energy / Solar", "Renewable / Solar"),
    MICROFINANCE("Microfinance / Group Loan", "Microfinance"),
    OVERDRAFT("Overdraft / Cash Credit", "Overdraft / CC"),
    OTHER("Other Loans", "Other");

    companion object {
        fun fromLabel(raw: String): LoanCategory {
            val clean = raw.trim()
            return entries.firstOrNull { it.label.equals(clean, ignoreCase = true) }
                ?: when {
                    clean.contains("personal", ignoreCase = true) -> PERSONAL
                    clean.contains("home", ignoreCase = true) -> HOME
                    clean.contains("education", ignoreCase = true) -> EDUCATION
                    clean.contains("two-wheeler", ignoreCase = true) || clean.contains("2-wheeler", ignoreCase = true) -> TWO_WHEELER
                    clean.contains("car", ignoreCase = true) || clean.contains("auto", ignoreCase = true) -> CAR
                    clean.contains("gold", ignoreCase = true) -> GOLD
                    clean.contains("msme", ignoreCase = true) || clean.contains("business", ignoreCase = true) -> BUSINESS
                    clean.contains("agriculture", ignoreCase = true) || clean.contains("kisan", ignoreCase = true) -> AGRICULTURE
                    clean.contains("commercial vehicle", ignoreCase = true) -> COMMERCIAL_VEHICLE
                    clean.contains("consumer durable", ignoreCase = true) -> CONSUMER_DURABLE
                    clean.contains("medical", ignoreCase = true) || clean.contains("healthcare", ignoreCase = true) -> MEDICAL
                    clean.contains("property", ignoreCase = true) || clean.contains("mortgage", ignoreCase = true) -> PROPERTY
                    clean.contains("securities", ignoreCase = true) -> SECURITIES
                    clean.contains("fixed deposit", ignoreCase = true) || clean.contains("fd", ignoreCase = true) -> FIXED_DEPOSIT
                    clean.contains("insurance", ignoreCase = true) -> INSURANCE_POLICY
                    clean.contains("provident", ignoreCase = true) || clean.contains("retirement", ignoreCase = true) -> PROVIDENT_FUND
                    clean.contains("solar", ignoreCase = true) || clean.contains("renewable", ignoreCase = true) -> RENEWABLE_ENERGY
                    clean.contains("microfinance", ignoreCase = true) || clean.contains("group", ignoreCase = true) -> MICROFINANCE
                    clean.contains("overdraft", ignoreCase = true) || clean.contains("cash credit", ignoreCase = true) -> OVERDRAFT
                    else -> OTHER
                }
        }
    }
}

data class LoanOffer(
    val id: String,
    val lenderName: String,
    val offerName: String,
    val category: LoanCategory,
    val annualRatePercent: Double,
    val interestMethod: InterestMethod = InterestMethod.REDUCING_BALANCE,
    val processingFeeFixed: Double = 0.0,
    val processingFeePercent: Double = 0.0,
    val hasGst: Boolean = true,
    val minAmount: Double = 25_000.0,
    val maxAmount: Double = 10_000_000.0,
    val minTenureMonths: Int = 12,
    val maxTenureMonths: Int = 84,
    val eligibilityNotes: String = "Salaried, 21–58",
    val officialUrl: String? = null,
    val isVerifiedSource: Boolean = false,

    // Real Lender Catalogue attributes (Feature 3)
    val lenderClassification: LenderClassification = LenderClassification.PRIVATE_BANK,
    val publishedInterestRate: String = if (annualRatePercent > 0) "$annualRatePercent% p.a." else "Rate not listed — check lender",
    val isMonthlyRate: Boolean = false,
    val monthlyRatePercent: Double? = null,
    val isComparableStartingRate: Boolean = true,
    val rawMaxLoanAmount: String = "",
    val sourceAsOf: String = "",
    val datasetStatus: String = "Indicative — verify with lender",
    val datasetVersion: String = "2026.10-research",
    val publishedProcessingFee: String = "",

    // 30-Bank Loan Directory extensions
    val shortDescription: String = "",
    val securityType: String = "",
    val typicalTenure: String = "",
    val commonDocuments: String = "",
    val prepaymentForeclosure: String = "",
    val availabilityStatus: String = "Verify on bank website",
    val ratesChargesUrl: String? = null,
    val ctaLabel: String = "Visit bank website",
    val lastChecked: String = "2026-10-24",
    val appDisclaimer: String = "",
    val minAnnualRate: Double? = null,
    val maxAnnualRate: Double? = null,
) {
    val lenderInitial: String get() = lenderName.firstOrNull()?.uppercase() ?: "L"
}

data class ComputedLoanOffer(
    val offer: LoanOffer,
    val principal: Double,
    val tenureMonths: Int,
    val installment: Double?,
    val totalRepayment: Double?,
    val totalInterest: Double?,
    val feeDisplay: String,
    val feeWithGst: Double?,
    val totalCost: Double?,
    val indicativeApr: Double?,
    val isLowestCost: Boolean = false,
    val isSelectedForComparison: Boolean = false,
    val calculationNote: String? = null,
)

object OfferCalculator {
    fun compute(
        offer: LoanOffer,
        principal: Double,
        tenureMonths: Int,
    ): ComputedLoanOffer {
        val feeDisplay = when {
            offer.publishedProcessingFee.isNotBlank() -> offer.publishedProcessingFee
            offer.processingFeeFixed > 0 && offer.processingFeePercent > 0 ->
                "${Money.inr(offer.processingFeeFixed)} + ${offer.processingFeePercent}%${if (offer.hasGst) " + GST" else ""}"
            offer.processingFeeFixed > 0 ->
                "${Money.inr(offer.processingFeeFixed)}${if (offer.hasGst) " + GST" else ""}"
            offer.processingFeePercent > 0 ->
                "${offer.processingFeePercent}%${if (offer.hasGst) " + GST" else ""}"
            else -> "Nil / Zero"
        }

        // 1. Overdraft / Cash Credit: Revolving credit facility; interest applies to utilized amount
        if (offer.category == LoanCategory.OVERDRAFT) {
            return ComputedLoanOffer(
                offer = offer,
                principal = principal,
                tenureMonths = tenureMonths,
                installment = null,
                totalRepayment = null,
                totalInterest = null,
                feeDisplay = feeDisplay,
                feeWithGst = null,
                totalCost = null,
                indicativeApr = null,
                isLowestCost = false,
                isSelectedForComparison = false,
                calculationNote = "Revolving overdraft facility: Interest applies only to drawn balance; monthly term EMI does not apply. Verify renewal fee and drawing limit.",
            )
        }

        // 2. Non-standard structures, missing rates or monthly rates
        val isMissingRate = offer.minAnnualRate == null && (offer.annualRatePercent <= 0.0 || !offer.isComparableStartingRate)
        if (offer.isMonthlyRate || !offer.isComparableStartingRate || isMissingRate) {
            val note = when {
                offer.isMonthlyRate ->
                    "Monthly rate (${offer.publishedInterestRate}). Not directly comparable to annual bank rates. Verify official APR with lender before comparing."
                isMissingRate ->
                    "Rate not listed in directory. Direct EMI calculation requires a confirmed interest rate from the lender."
                else ->
                    "Rate is offer-dependent or capped (${offer.publishedInterestRate}). Direct EMI calculation requires a confirmed starting rate from lender."
            }

            return ComputedLoanOffer(
                offer = offer,
                principal = principal,
                tenureMonths = tenureMonths,
                installment = null,
                totalRepayment = null,
                totalInterest = null,
                feeDisplay = feeDisplay,
                feeWithGst = null,
                totalCost = null,
                indicativeApr = null,
                isLowestCost = false,
                isSelectedForComparison = false,
                calculationNote = note,
            )
        }

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
