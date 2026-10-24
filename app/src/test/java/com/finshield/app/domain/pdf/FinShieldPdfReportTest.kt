package com.finshield.app.domain.pdf

import com.finshield.app.domain.analyzer.PageExtraction
import com.finshield.app.domain.calculator.InterestMethod
import com.finshield.app.model.AnalysisReport
import com.finshield.app.model.FinancialBreakdown
import com.finshield.app.model.FinancialCharge
import com.finshield.app.model.ChargeType
import com.finshield.app.model.LoanSnapshot
import com.finshield.app.model.RiskFinding
import com.finshield.app.model.SampleReports
import com.finshield.app.model.Severity
import com.finshield.app.model.TextSourceKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FinShieldPdfReportTest {

    private val fixedDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse("2026-10-25")!!

    @Test
    fun testFilenameGeneration_withLenderName() {
        val sample = SampleReports.forId("sample-1")
        val filename = PdfReportContentBuilder.buildReportFilename(sample, fixedDate)
        assertEquals("FinShield_Loan_Analysis_Sample_Personal_Loan_2026-10-25.pdf", filename)
    }

    @Test
    fun testFilenameGeneration_withSpecialCharacters() {
        val report = AnalysisReport(
            documentName = "My Loan @ HDFC Bank / Branch #12.pdf",
            isSample = false,
            loanSnapshot = LoanSnapshot(
                loanType = "Personal Loan",
                lenderName = "HDFC Bank Ltd. / Retail Lending",
                sanctionedPrincipal = 300000.0,
                annualRatePercent = 11.5,
                interestMethod = InterestMethod.REDUCING_BALANCE,
                tenureMonths = 24,
                tenureYears = 2.0,
                statedEmi = 14050.0,
                calculatedEmi = 14050.0,
                emiDifference = 0.0,
                statedTotalRepayment = 337200.0,
                calculatedTotalRepayment = 337200.0,
                totalRepaymentDifference = 0.0,
            ),
            financialBreakdown = FinancialBreakdown(
                sanctionedPrincipal = 300000.0,
                processingFeeAmount = 3000.0,
                processingFeeGst = 540.0,
                stampDuty = 200.0,
                documentationCharges = 500.0,
                insuranceCharges = 0.0,
                otherUpfrontDeductions = 0.0,
                totalUpfrontDeductions = 3740.0,
                estimatedNetDisbursal = 296260.0,
                totalInterest = 37200.0,
                totalScheduledEmis = 337200.0,
                totalRepaymentScheduled = 337200.0,
                otherMandatoryCharges = 0.0,
                totalEstimatedBorrowerCost = 340940.0,
            ),
            riskFindings = emptyList(),
            feesAndCharges = emptyList(),
            keyObligations = emptyList(),
            questionsForLender = emptyList(),
        )

        val filename = PdfReportContentBuilder.buildReportFilename(report, fixedDate)
        assertTrue(filename.startsWith("FinShield_Loan_Analysis_HDFC_Bank_Ltd_Retail_"))
        assertTrue(filename.endsWith("_2026-10-25.pdf"))
        assertFalse(filename.contains("/"))
        assertFalse(filename.contains("@"))
        assertFalse(filename.contains("#"))
    }

    @Test
    fun testTextWrapping_longTextAndParagraphs() {
        val longClause = "Foreclosure permitted after 6 months subject to charges of 4% of outstanding principal plus applicable GST."
        val lines = PdfReportContentBuilder.wrapText(longClause, maxCharsPerLine = 35)

        assertTrue(lines.size >= 3)
        for (line in lines) {
            assertTrue("Line '$line' exceeded max limit 35", line.length <= 35)
        }
        val reconstructed = lines.joinToString(" ")
        assertTrue(reconstructed.contains("Foreclosure permitted"))
        assertTrue(reconstructed.contains("applicable GST."))
    }

    @Test
    fun testEmptyOrPartiallyExtractedReport() {
        val minimalReport = AnalysisReport(
            documentName = "Scanned_Draft.pdf",
            isSample = false,
            loanSnapshot = LoanSnapshot(
                loanType = "Personal Loan",
                sanctionedPrincipal = null,
                annualRatePercent = null,
                interestMethod = null,
                tenureMonths = null,
                tenureYears = null,
                statedEmi = null,
                calculatedEmi = null,
                emiDifference = null,
                statedTotalRepayment = null,
                calculatedTotalRepayment = null,
                totalRepaymentDifference = null,
            ),
            financialBreakdown = FinancialBreakdown(
                sanctionedPrincipal = null,
                processingFeeAmount = null,
                processingFeeGst = null,
                stampDuty = null,
                documentationCharges = null,
                insuranceCharges = null,
                otherUpfrontDeductions = null,
                totalUpfrontDeductions = null,
                estimatedNetDisbursal = null,
                totalInterest = null,
                totalScheduledEmis = null,
                totalRepaymentScheduled = null,
                otherMandatoryCharges = null,
                totalEstimatedBorrowerCost = null,
            ),
            riskFindings = emptyList(),
            feesAndCharges = emptyList(),
            keyObligations = emptyList(),
            questionsForLender = emptyList(),
        )

        val data = PdfReportContentBuilder.buildPdfData(minimalReport, PdfExportOptions(includeFullTextAppendix = false))

        assertEquals("Scanned_Draft.pdf", data.metadata.documentName)
        assertEquals("Not Specified in Scanned Pages", data.metadata.lenderName)
        assertEquals("Not Stated", data.totalUpfrontDeductions)
        assertEquals("Not Stated", data.estimatedNetDisbursal)

        val principalRow = data.overviewRows.firstOrNull { it.label == "Sanctioned Principal" }
        assertNotNull(principalRow)
        assertEquals("Not Stated", principalRow?.value)
    }

    @Test
    fun testMultipleRiskFindingsPreservation() {
        val sample = SampleReports.forId("sample-1")
        val data = PdfReportContentBuilder.buildPdfData(sample)

        assertTrue("Expected multiple risk findings", data.riskItems.size >= 3)

        val highRisk = data.riskItems.firstOrNull { it.severity == Severity.HIGH }
        assertNotNull("Expected HIGH severity risk finding", highRisk)
        assertTrue(highRisk!!.title.contains("Stated EMI Exceeds"))
        assertNotNull(highRisk.financialImpact)
        assertNotNull(highRisk.evidenceQuote)

        val mediumRisk = data.riskItems.firstOrNull { it.severity == Severity.MEDIUM }
        assertNotNull("Expected MEDIUM severity risk finding", mediumRisk)
    }

    @Test
    fun testPreservationOfSourceQuotationsAndPageReferences() {
        val sample = SampleReports.forId("sample-1")
        val data = PdfReportContentBuilder.buildPdfData(sample)

        val riskWithQuote = data.riskItems.firstOrNull { it.clauseRef != null && it.pageNumber != null }
        assertNotNull(riskWithQuote)
        assertEquals(1, riskWithQuote?.pageNumber)
        assertEquals("Clause 3.2", riskWithQuote?.clauseRef)
        assertTrue(riskWithQuote!!.evidenceQuote!!.contains("16,900"))

        val upfrontCharge = data.charges.firstOrNull { it.name == "Processing Fee" }
        assertNotNull(upfrontCharge)
        assertTrue(upfrontCharge!!.isUpfront)
        assertTrue(upfrontCharge.basisFormula.contains("2.0%"))
        assertEquals("Clause 4.1", upfrontCharge.clauseRef)
        assertTrue(upfrontCharge.evidenceSnippet!!.contains("Processing fee of 2%"))
    }

    @Test
    fun testOptionalFullTextAppendixInclusion() {
        val sample = SampleReports.forId("sample-1")

        // 1. Without appendix
        val dataNoAppendix = PdfReportContentBuilder.buildPdfData(
            sample,
            PdfExportOptions(includeFullTextAppendix = false)
        )
        assertTrue("Appendix should be empty when disabled", dataNoAppendix.appendixPages.isEmpty())

        // 2. With appendix
        val dataWithAppendix = PdfReportContentBuilder.buildPdfData(
            sample,
            PdfExportOptions(includeFullTextAppendix = true)
        )
        assertEquals(2, dataWithAppendix.appendixPages.size)
        assertEquals(1, dataWithAppendix.appendixPages[0].pageNumber)
        assertEquals(2, dataWithAppendix.appendixPages[1].pageNumber)
        assertTrue(dataWithAppendix.appendixPages[0].text.contains("LOAN AGREEMENT & SANCTION SCHEDULE"))
        assertTrue(dataWithAppendix.appendixPages[1].text.contains("SCHEDULE B - CHARGES & COVENANTS"))
    }

    @Test
    fun testCurrencyAndPercentageFormatting() {
        assertEquals("₹5,00,000.00", PdfReportContentBuilder.formatCurrency(500000.0))
        assertEquals("₹16,900.00", PdfReportContentBuilder.formatCurrency(16900.0))
        assertEquals("₹0.00", PdfReportContentBuilder.formatCurrency(0.0))
        assertEquals("Not Stated", PdfReportContentBuilder.formatCurrency(null))
        assertEquals("Not Stated", PdfReportContentBuilder.formatCurrency(Double.NaN))
    }

    @Test
    fun testRealAnalysisDataVsDemoData() {
        val customReport = AnalysisReport(
            documentName = "Real_Borrower_Agreement.pdf",
            isSample = false,
            loanSnapshot = LoanSnapshot(
                loanType = "Home Loan",
                lenderName = "Axis Bank",
                borrowerName = "Priya Patel",
                agreementDate = "15 Oct 2026",
                referenceNumber = "AXIS-HL-2026-9912",
                sanctionedPrincipal = 4500000.0,
                actualDisbursedAmount = 4455000.0,
                annualRatePercent = 8.75,
                interestMethod = InterestMethod.REDUCING_BALANCE,
                tenureMonths = 240,
                tenureYears = 20.0,
                statedEmi = 39775.0,
                calculatedEmi = 39775.0,
                emiDifference = 0.0,
                statedTotalRepayment = 9546000.0,
                calculatedTotalRepayment = 9546000.0,
                totalRepaymentDifference = 0.0,
            ),
            financialBreakdown = FinancialBreakdown(
                sanctionedPrincipal = 4500000.0,
                processingFeeAmount = 40000.0,
                processingFeeGst = 5000.0,
                stampDuty = 0.0,
                documentationCharges = 0.0,
                insuranceCharges = 0.0,
                otherUpfrontDeductions = 0.0,
                totalUpfrontDeductions = 45000.0,
                estimatedNetDisbursal = 4455000.0,
                totalInterest = 5046000.0,
                totalScheduledEmis = 9546000.0,
                totalRepaymentScheduled = 9546000.0,
                otherMandatoryCharges = 0.0,
                totalEstimatedBorrowerCost = 9591000.0,
            ),
            riskFindings = listOf(
                RiskFinding(
                    title = "Property Title Search Required",
                    explanation = "Lender requires clear legal search of parent title deed.",
                    severity = Severity.MEDIUM,
                    evidence = "Schedule 1: Clear title report mandatory prior to disbursement.",
                    pageNumber = 3,
                )
            ),
            feesAndCharges = emptyList(),
            keyObligations = emptyList(),
            questionsForLender = listOf("Is the interest reset quarterly or semi-annually?"),
        )

        val data = PdfReportContentBuilder.buildPdfData(customReport)

        assertEquals("Real_Borrower_Agreement.pdf", data.metadata.documentName)
        assertEquals("Axis Bank", data.metadata.lenderName)
        assertEquals("Priya Patel", data.metadata.borrowerName)
        assertEquals("15 Oct 2026", data.metadata.agreementDate)
        assertEquals("AXIS-HL-2026-9912", data.metadata.referenceNumber)
        assertFalse(data.metadata.isSample)

        assertEquals("₹45,00,000.00", data.overviewRows.first { it.label == "Sanctioned Principal" }.value)
        assertEquals("₹44,55,000.00", data.estimatedNetDisbursal)
        assertEquals("₹45,000.00", data.totalUpfrontDeductions)
        assertEquals(1, data.riskItems.size)
        assertEquals("Property Title Search Required", data.riskItems[0].title)
        assertEquals("Is the interest reset quarterly or semi-annually?", data.questions[0].question)
    }

    @Test
    fun testEmiDiscrepancyDetection() {
        val sample = SampleReports.forId("sample-1")
        val data = PdfReportContentBuilder.buildPdfData(sample)

        val statedEmiRow = data.overviewRows.firstOrNull { it.label == "Stated EMI" }
        assertNotNull(statedEmiRow)
        assertTrue("Expected discrepancy flag on stated EMI", statedEmiRow!!.isDiscrepancy)
        assertTrue(statedEmiRow.subtext!!.contains("Differs by"))

        val warnings = data.warnings
        val emiWarning = warnings.firstOrNull { it.title.contains("EMI Discrepancy") }
        assertNotNull("Expected EMI discrepancy warning", emiWarning)
        assertTrue(emiWarning!!.isHighPriority)
    }
}
