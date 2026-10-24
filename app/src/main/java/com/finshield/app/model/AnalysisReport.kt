package com.finshield.app.model

enum class EvidenceKind { EXPLICIT, HEURISTIC, NOT_FOUND }

/** One extracted item, with the text it came from when known. */
data class Finding(
    val label: String,
    val value: String?,
    val evidence: String?,
    val kind: EvidenceKind,
)

data class AnalysisReport(
    val documentName: String,
    /** True when the content is built-in illustrative data and not extracted from a real document. */
    val isSample: Boolean,
    val principal: Double?,
    val annualRatePercent: Double?,
    val tenureMonths: Int?,
    val statedEmi: Double?,
    val terms: List<Finding>,
    val feesAndPenalties: List<Finding>,
    val missingInformation: List<String>,
    val manualReview: List<String>,
)

/** Illustrative report used to preview the report layout. Not derived from any real agreement. */
object SampleReports {
    fun forId(id: String): AnalysisReport {
        val name = com.finshield.app.data.history.DemoHistoryRepository.SAMPLES.firstOrNull { it.id == id }?.documentName
            ?: "Sample_Loan_Agreement.pdf"
        return build(name)
    }

    private fun build(name: String) = AnalysisReport(
        documentName = name,
        isSample = true,
        principal = 500_000.0,
        annualRatePercent = 12.5,
        tenureMonths = 36,
        statedEmi = 16_900.0,
        terms = listOf(
            Finding("Sanctioned amount", "₹5,00,000", "\"The Lender sanctions a Loan of Rs. 5,00,000/- (Rupees Five Lakh only)…\"", EvidenceKind.EXPLICIT),
            Finding("Interest rate", "12.5% per annum", "\"…interest at 12.5% per annum on a reducing balance basis.\"", EvidenceKind.EXPLICIT),
            Finding("Tenure", "36 months", "\"…repayable in 36 monthly instalments.\"", EvidenceKind.EXPLICIT),
            Finding("Stated EMI", "₹16,900", "\"EMI of Rs. 16,900/- payable on the 5th of each month.\"", EvidenceKind.EXPLICIT),
        ),
        feesAndPenalties = listOf(
            Finding("Processing fee", "2% of amount + taxes", "\"…a processing fee of 2% of the sanctioned amount plus applicable taxes…\"", EvidenceKind.EXPLICIT),
            Finding("Late-payment charge", "2% per month on overdue amount", "\"…overdue amounts attract charges at 2% per month…\"", EvidenceKind.EXPLICIT),
            Finding("Prepayment / foreclosure", "Possible charge, rate unclear", "\"…foreclosure may be permitted subject to charges as notified by the Lender.\"", EvidenceKind.HEURISTIC),
        ),
        missingInformation = listOf(
            "Foreclosure charge percentage was not found.",
            "Insurance premium amount, if any, was not stated.",
        ),
        manualReview = listOf(
            "Clause allowing the lender to revise charges \"as notified\" (heuristic match).",
            "Stated EMI differs from the independent calculation. Confirm the basis with the lender.",
        ),
    )
}
