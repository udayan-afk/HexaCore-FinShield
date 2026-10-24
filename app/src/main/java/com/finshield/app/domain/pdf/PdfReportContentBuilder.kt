package com.finshield.app.domain.pdf

import com.finshield.app.domain.calculator.Money
import com.finshield.app.model.AnalysisReport
import com.finshield.app.model.ObligationStatus
import com.finshield.app.model.Severity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/** Options for generating the FinShield PDF report. */
data class PdfExportOptions(
    val includeFullTextAppendix: Boolean = false,
)

/** Structured and validated data ready for PDF rendering. */
data class PdfReportData(
    val metadata: ReportMetadata,
    val overviewRows: List<OverviewRow>,
    val charges: List<ChargeRow>,
    val totalUpfrontDeductions: String,
    val estimatedNetDisbursal: String,
    val riskItems: List<RiskItem>,
    val obligations: List<ObligationItem>,
    val questions: List<QuestionItem>,
    val warnings: List<WarningItem>,
    val appendixPages: List<AppendixPage>,
    val disclaimer: String,
)

data class ReportMetadata(
    val title: String = "LOAN AGREEMENT ANALYSIS REPORT",
    val documentName: String,
    val lenderName: String,
    val borrowerName: String,
    val agreementDate: String,
    val referenceNumber: String,
    val generationDate: String,
    val analyzedPagesCount: Int,
    val extractionSourceSummary: String,
    val completenessPercentage: Int,
    val isSample: Boolean,
)

data class OverviewRow(
    val label: String,
    val value: String,
    val subtext: String? = null,
    val isDiscrepancy: Boolean = false,
)

data class ChargeRow(
    val name: String,
    val category: String,
    val basisFormula: String,
    val amount: String,
    val isUpfront: Boolean,
    val clauseRef: String? = null,
    val evidenceSnippet: String? = null,
)

data class RiskItem(
    val title: String,
    val severity: Severity,
    val explanation: String,
    val whyItMatters: String?,
    val evidenceQuote: String?,
    val pageNumber: Int?,
    val clauseRef: String?,
    val questionToAsk: String?,
    val financialImpact: String?,
)

data class ObligationItem(
    val category: String,
    val summary: String,
    val status: ObligationStatus,
    val details: String,
    val evidence: String?,
    val pageClause: String?,
)

data class QuestionItem(
    val index: Int,
    val question: String,
)

data class WarningItem(
    val title: String,
    val description: String,
    val isHighPriority: Boolean,
)

data class AppendixPage(
    val pageNumber: Int,
    val charCount: Int,
    val sourceKind: String,
    val text: String,
)

object PdfReportContentBuilder {

    private val DATE_FORMAT = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US)
    private val FILE_DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    /**
     * Generates a clean, safe, and descriptive filename for the downloaded report.
     * E.g. "FinShield_Loan_Analysis_Apex_Financial_2026-10-25.pdf"
     */
    fun buildReportFilename(report: AnalysisReport, date: Date = Date()): String {
        val rawLender = report.loanSnapshot.lenderName
            ?: report.lenderName
            ?: report.documentName.removeSuffix(".pdf").removeSuffix(".PDF")
        
        val sanitized = rawLender
            .replace(Regex("[^a-zA-Z0-9_-]"), "_")
            .replace(Regex("_+"), "_")
            .trim('_')
            .take(30)
            .ifBlank { "Loan" }

        val dateStr = FILE_DATE_FORMAT.format(date)
        return "FinShield_Loan_Analysis_${sanitized}_$dateStr.pdf"
    }

    /**
     * Transforms an [AnalysisReport] into structured, formatted [PdfReportData].
     */
    fun buildPdfData(
        report: AnalysisReport,
        options: PdfExportOptions = PdfExportOptions(),
        currentDate: Date = Date(),
    ): PdfReportData {
        val s = report.loanSnapshot
        val fb = report.financialBreakdown

        val lender = s.lenderName ?: report.lenderName ?: "Not Specified in Scanned Pages"
        val borrower = s.borrowerName ?: report.borrowerName ?: "Not Specified in Scanned Pages"
        val agreementDate = s.agreementDate ?: report.agreementDate ?: "Not Stated"
        val refNum = s.referenceNumber ?: report.referenceNumber ?: "N/A"
        val completenessPct = report.completenessReport?.completenessPercentage ?: 85

        val metadata = ReportMetadata(
            documentName = report.documentName,
            lenderName = lender,
            borrowerName = borrower,
            agreementDate = agreementDate,
            referenceNumber = refNum,
            generationDate = DATE_FORMAT.format(currentDate),
            analyzedPagesCount = report.pagesCount.coerceAtLeast(1),
            extractionSourceSummary = report.extractionSourceSummary,
            completenessPercentage = completenessPct,
            isSample = report.isSample,
        )

        // 1. Overview rows
        val overviewRows = mutableListOf<OverviewRow>()
        overviewRows.add(OverviewRow("Loan Type", s.loanType))
        overviewRows.add(
            OverviewRow(
                label = "Sanctioned Principal",
                value = formatCurrency(s.sanctionedPrincipal),
                subtext = "Base amount approved in loan schedule",
            )
        )
        overviewRows.add(
            OverviewRow(
                label = "Estimated Net Disbursed",
                value = formatCurrency(fb.estimatedNetDisbursal ?: s.actualDisbursedAmount),
                subtext = "Amount credited after upfront deductions",
            )
        )
        overviewRows.add(
            OverviewRow(
                label = "Total Upfront Deductions",
                value = formatCurrency(fb.totalUpfrontDeductions),
                subtext = "Fees, taxes, stamp duty and insurance retained upfront",
            )
        )

        val rateMethod = s.interestMethod?.label ?: "Reducing Balance"
        val rateStructure = when (s.isFloatingRate) {
            true -> "Floating / Variable"
            false -> "Fixed Rate"
            null -> "Not Specified"
        }
        val rateText = s.annualRatePercent?.let { "$it% p.a. ($rateMethod, $rateStructure)" } ?: "Not Stated"
        overviewRows.add(OverviewRow("Interest Rate & Structure", rateText))

        val aprText = s.aprPercent?.let { "$it% (Stated in Document)" } ?: "Not Stated in Contract Disclosures"
        overviewRows.add(OverviewRow("Annual Percentage Rate (APR)", aprText))

        val tenureText = s.tenureMonths?.let { "$it months (${String.format(Locale.US, "%.1f", it / 12.0)} years)" } ?: "Not Stated"
        overviewRows.add(OverviewRow("Tenure", tenureText))

        val statedEmi = s.statedEmi
        val calcEmi = s.calculatedEmi
        val emiDiff = s.emiDifference ?: if (statedEmi != null && calcEmi != null) statedEmi - calcEmi else null
        val hasEmiDiff = emiDiff != null && abs(emiDiff) >= 1.0

        overviewRows.add(
            OverviewRow(
                label = "Stated EMI",
                value = formatCurrency(statedEmi),
                subtext = if (hasEmiDiff) "Differs by ${formatCurrency(emiDiff)}/mo from standard amortisation" else "Matches standard reducing-balance amortisation",
                isDiscrepancy = hasEmiDiff,
            )
        )
        if (calcEmi != null) {
            overviewRows.add(
                OverviewRow(
                    label = "Calculated Standard EMI",
                    value = formatCurrency(calcEmi),
                    subtext = "Independent check: reducing-balance at ${s.annualRatePercent ?: 0.0}% over ${s.tenureMonths ?: 0} mo",
                )
            )
        }

        val statedTotal = s.statedTotalRepayment ?: if (statedEmi != null && s.tenureMonths != null) statedEmi * s.tenureMonths else null
        val calcTotal = s.calculatedTotalRepayment ?: fb.totalRepaymentScheduled
        val totalDiff = s.totalRepaymentDifference ?: if (statedTotal != null && calcTotal != null) statedTotal - calcTotal else null
        val hasTotalDiff = totalDiff != null && abs(totalDiff) >= 5.0

        overviewRows.add(
            OverviewRow(
                label = "Stated Total Repayment",
                value = formatCurrency(statedTotal),
                subtext = if (hasTotalDiff) "Extra unexplained cost of ${formatCurrency(totalDiff)} over loan life" else null,
                isDiscrepancy = hasTotalDiff,
            )
        )

        overviewRows.add(
            OverviewRow(
                label = "Total Scheduled Interest",
                value = formatCurrency(fb.totalInterest),
                subtext = "Amortized interest over tenure",
            )
        )
        overviewRows.add(
            OverviewRow(
                label = "Total Estimated Borrower Cost",
                value = formatCurrency(fb.totalEstimatedBorrowerCost),
                subtext = "Principal + Scheduled Interest + Upfront Deductions",
            )
        )

        // 2. Charges
        val chargeRows = mutableListOf<ChargeRow>()
        val rawCharges = if (fb.itemizedCharges.isNotEmpty()) fb.itemizedCharges else report.feesAndCharges
        rawCharges.forEach { c ->
            val amt = c.calculatedAmount ?: c.fixedAmount ?: c.totalWithGst
            val basis = buildString {
                append(c.formulaDescription)
                if (c.assumptions != null) append(" (${c.assumptions})")
            }
            chargeRows.add(
                ChargeRow(
                    name = c.name,
                    category = c.chargeType.label,
                    basisFormula = basis,
                    amount = formatCurrency(amt),
                    isUpfront = c.isUpfrontDeduction,
                    clauseRef = c.clauseReference?.let { if (it.startsWith("Clause", ignoreCase = true)) it else "Clause $it" } ?: (c.pageNumber?.let { "Page $it" }),
                    evidenceSnippet = c.evidence,
                )
            )
        }

        val upfrontTotalStr = formatCurrency(fb.totalUpfrontDeductions)
        val netDisbursalStr = formatCurrency(fb.estimatedNetDisbursal ?: s.actualDisbursedAmount)

        // 3. Risks
        val riskItems = report.riskFindings.map { r ->
            RiskItem(
                title = r.title,
                severity = r.severity,
                explanation = r.explanation,
                whyItMatters = r.whyItMatters,
                evidenceQuote = r.evidence,
                pageNumber = r.pageNumber,
                clauseRef = r.clauseReference,
                questionToAsk = r.questionToAsk,
                financialImpact = r.financialImpactDescription ?: r.financialImpact?.let { formatCurrency(it) },
            )
        }

        // 4. Obligations
        val obligations = report.keyObligations.map { o ->
            val pageClause = listOfNotNull(
                o.clauseReference?.let { if (it.startsWith("Clause", ignoreCase = true)) it else "Clause $it" },
                o.pageNumber?.let { "Page $it" },
            ).joinToString(", ").ifBlank { null }

            ObligationItem(
                category = o.category,
                summary = o.summary,
                status = o.status,
                details = o.details,
                evidence = o.evidence,
                pageClause = pageClause,
            )
        }

        // 5. Questions for Lender
        val questions = report.questionsForLender.mapIndexed { idx, q ->
            QuestionItem(index = idx + 1, question = q)
        }

        // 6. Warnings & Missing Fields
        val warnings = mutableListOf<WarningItem>()
        if (hasEmiDiff) {
            warnings.add(
                WarningItem(
                    title = "EMI Discrepancy Flagged",
                    description = "Contract stated EMI (${formatCurrency(statedEmi)}) exceeds standard reducing amortisation (${formatCurrency(calcEmi)}) by ${formatCurrency(emiDiff)}/month. Total lifetime variance: ${formatCurrency(totalDiff)}.",
                    isHighPriority = true,
                )
            )
        }
        report.manualReview.forEach { review ->
            warnings.add(
                WarningItem(
                    title = "Manual Review Recommendation",
                    description = review,
                    isHighPriority = review.contains("discretion", ignoreCase = true) || review.contains("differs", ignoreCase = true),
                )
            )
        }
        report.missingInformation.forEach { missing ->
            warnings.add(
                WarningItem(
                    title = "Missing or Unstated Term",
                    description = missing,
                    isHighPriority = false,
                )
            )
        }

        // 7. Appendix (Full extracted text if enabled)
        val appendixPages = if (options.includeFullTextAppendix && report.rawPages.isNotEmpty()) {
            report.rawPages.map { p ->
                AppendixPage(
                    pageNumber = p.pageNumber,
                    charCount = p.charCount,
                    sourceKind = p.sourceKind.label,
                    text = p.text,
                )
            }
        } else {
            emptyList()
        }

        val disclaimer = "REGULATORY & INFORMATIONAL DISCLAIMER: FinShield provides automated informational analysis to help borrowers understand loan agreements before borrowing. This report is algorithmically compiled from the extracted text of the uploaded document and may contain extraction or optical character recognition errors. FinShield is not a lender, banking institution, credit broker, or legal adviser. This report does not constitute financial, lending, investment, or legal advice. All interest rates, EMI amounts, fee schedules, and legal obligations must be verified directly against the official sanction letter, Key Fact Statement (KFS), and formal loan agreement issued by the lender."

        return PdfReportData(
            metadata = metadata,
            overviewRows = overviewRows,
            charges = chargeRows,
            totalUpfrontDeductions = upfrontTotalStr,
            estimatedNetDisbursal = netDisbursalStr,
            riskItems = riskItems,
            obligations = obligations,
            questions = questions,
            warnings = warnings,
            appendixPages = appendixPages,
            disclaimer = disclaimer,
        )
    }

    fun formatCurrency(amount: Double?): String {
        return if (amount != null && amount.isFinite()) Money.inr2(amount) else "Not Stated"
    }

    /**
     * Pure Kotlin text-wrapping utility that breaks long strings into lines of at most
     * [maxCharsPerLine] characters without splitting words across line boundaries.
     */
    fun wrapText(text: String, maxCharsPerLine: Int): List<String> {
        if (text.isBlank()) return emptyList()
        val result = mutableListOf<String>()
        val paragraphs = text.split("\n")

        for (paragraph in paragraphs) {
            val words = paragraph.split(Regex("\\s+")).filter { it.isNotBlank() }
            if (words.isEmpty()) {
                result.add("")
                continue
            }

            var currentLine = StringBuilder()
            for (word in words) {
                if (currentLine.isEmpty()) {
                    currentLine.append(word)
                } else if (currentLine.length + 1 + word.length <= maxCharsPerLine) {
                    currentLine.append(" ").append(word)
                } else {
                    result.add(currentLine.toString())
                    currentLine = StringBuilder(word)
                }
            }
            if (currentLine.isNotEmpty()) {
                result.add(currentLine.toString())
            }
        }
        return result
    }
}
