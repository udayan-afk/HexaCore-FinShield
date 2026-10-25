package com.finshield.app.domain.credit

import androidx.compose.ui.graphics.Color
import com.finshield.app.ui.theme.Amber
import com.finshield.app.ui.theme.AmberDark
import com.finshield.app.ui.theme.AmberTint
import com.finshield.app.ui.theme.Emerald
import com.finshield.app.ui.theme.EmeraldDark
import com.finshield.app.ui.theme.EmeraldTint
import com.finshield.app.ui.theme.InfoBlue
import com.finshield.app.ui.theme.InfoBlueTint
import com.finshield.app.ui.theme.Rose
import com.finshield.app.ui.theme.RoseTint

/**
 * Score bracket classification according to standard TransUnion CIBIL specifications (range 300 to 900).
 */
enum class CreditScoreBand(
    val label: String,
    val minScore: Int,
    val maxScore: Int,
    val summary: String,
    val badgeBackground: Color,
    val badgeContent: Color,
) {
    POOR(
        label = "Poor",
        minScore = 300,
        maxScore = 549,
        summary = "High risk for lenders. Loan approvals may be rejected or carry high interest rates.",
        badgeBackground = RoseTint,
        badgeContent = Rose,
    ),
    AVERAGE(
        label = "Average",
        minScore = 550,
        maxScore = 649,
        summary = "Moderate risk. Eligible for some loans, but favorable terms usually require 700+.",
        badgeBackground = AmberTint,
        badgeContent = AmberDark,
    ),
    GOOD(
        label = "Good",
        minScore = 650,
        maxScore = 749,
        summary = "Low risk. Lenders view you favorably with good chances of competitive interest rates.",
        badgeBackground = InfoBlueTint,
        badgeContent = InfoBlue,
    ),
    EXCELLENT(
        label = "Excellent",
        minScore = 750,
        maxScore = 900,
        summary = "Prime borrower. Best approval odds, fastest processing, and the lowest interest rates.",
        badgeBackground = EmeraldTint,
        badgeContent = EmeraldDark,
    );

    companion object {
        fun fromScore(score: Int): CreditScoreBand = when {
            score >= 750 -> EXCELLENT
            score >= 650 -> GOOD
            score >= 550 -> AVERAGE
            else -> POOR
        }
    }
}

/**
 * Verification status of the credit score.
 */
enum class CreditScoreStatus {
    NOT_FETCHED,
    FETCHING,
    VERIFIED,
    NEEDS_CONSENT,
    INTEGRATION_REQUIRED,
    ERROR,
}

/**
 * Data model for a user's credit score.
 * Clearly separates verified official bureau data from estimated or user-provided data.
 */
data class CreditScoreData(
    val score: Int? = null,
    val minRange: Int = 300,
    val maxRange: Int = 900,
    val band: CreditScoreBand? = score?.let { CreditScoreBand.fromScore(it) },
    val retrievalDate: String? = null,
    val provider: String = "TransUnion CIBIL",
    val isOfficialBureauScore: Boolean = false,
    val status: CreditScoreStatus = CreditScoreStatus.NOT_FETCHED,
    val errorMessage: String? = null,
)

/**
 * Connection status for RBI-compliant Account Aggregator financial accounts.
 */
enum class AAConnectionStatus {
    NOT_CONNECTED,
    CONNECTING,
    CONNECTED,
    REVOKED,
    INTEGRATION_REQUIRED,
}

/**
 * Account Aggregator consent & connection record.
 * FinShield never receives or stores bank passwords, UPI PINs, ATM PINs, or OTPs.
 */
data class AccountAggregatorData(
    val status: AAConnectionStatus = AAConnectionStatus.NOT_CONNECTED,
    val connectedBankName: String? = null,
    val accountNumberMasked: String? = null,
    val lastSyncedTime: String? = null,
    val consentExpiry: String? = null,
    val purpose: String = "Cash flow assessment for loan eligibility verification",
    val dataScope: List<String> = listOf("Account Profile", "Transaction Summary (Last 6 Months)"),
)

/**
 * Employment classification for loan eligibility estimation.
 */
enum class EmploymentType(val label: String) {
    SALARIED("Salaried Employee"),
    SELF_EMPLOYED_BUSINESS("Self-Employed (Business)"),
    SELF_EMPLOYED_PROFESSIONAL("Self-Employed (Professional)"),
}

/**
 * Indicative eligibility outcome based on debt-to-income (FOIR) limits.
 */
enum class EligibilityStatus(
    val label: String,
    val badgeBackground: Color,
    val badgeContent: Color,
) {
    AFFORDABLE("Looks affordable", EmeraldTint, EmeraldDark),
    REVIEW_BUDGET("Review your budget", AmberTint, AmberDark),
    HIGH_BURDEN("High debt burden", RoseTint, Rose),
    MORE_INFO_NEEDED("More information needed", InfoBlueTint, InfoBlue),
}

/**
 * User inputs for the loan eligibility calculation.
 */
data class LoanEligibilityInput(
    val monthlyIncome: Double = 75000.0,
    val existingMonthlyEmi: Double = 12000.0,
    val employmentType: EmploymentType = EmploymentType.SALARIED,
    val requestedLoanAmount: Double = 500000.0,
    val requestedTenureMonths: Int = 36,
    val expectedAnnualInterestRate: Double = 10.5,
    val creditScore: Int? = null,
)

/**
 * Comprehensive calculation results for the loan eligibility assessment.
 */
data class LoanEligibilityResult(
    val monthlyIncome: Double,
    val existingMonthlyEmi: Double,
    val estimatedNewEmi: Double,
    val totalMonthlyEmiObligation: Double,
    val foirPercent: Double,
    val remainingMonthlyCapacity: Double,
    val status: EligibilityStatus,
    val summaryExplanation: String,
    val keyDecisionFactors: List<String>,
)
