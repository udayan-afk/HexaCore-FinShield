package com.finshield.app.domain.analyzer

import com.finshield.app.model.CompletenessStatus
import com.finshield.app.model.Severity
import com.finshield.app.model.TextSourceKind
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ComprehensiveDocumentAiTest {

    @Test
    fun testSyntheticAgreementIntentionalDocumentationFeeInconsistencyDetected() {
        // Multi-page synthetic loan agreement where Clause 4.3 states documentation charge of Rs. 1,000/-
        // while Schedule B fee breakdown specifies documentation charges of Rs. 800/-.
        val page1 = PageExtraction(
            pageNumber = 1,
            text = """
                LOAN AGREEMENT & SANCTION SCHEDULE
                Lender: Apex Financial Services Ltd.
                Borrower: Rahul Sharma
                Loan Facility: Personal Loan Facility
                Sanctioned Loan Amount: Rs. 5,00,000/- (Rupees Five Lakhs Only)
                Rate of Interest: 12.5% per annum on reducing balance basis.
                Tenure: 36 monthly instalments
                Equated Monthly Installment (EMI): Rs. 16,900/-
                Clause 4.1: Processing fee of 2% of the sanctioned amount plus applicable taxes shall be deducted at disbursal.
            """.trimIndent(),
            charCount = 420,
            sourceKind = TextSourceKind.NATIVE_PDF,
        )

        val page2 = PageExtraction(
            pageNumber = 2,
            text = """
                SCHEDULE B - CHARGES & COVENANTS
                Clause 4.3: Administrative and documentation charge of Rs. 1,000/- shall be deducted from loan disbursement.
                Schedule Fee Breakdown: Documentation charges: Rs. 800/-
                Stamp duty: Rs. 500/-
                Credit Shield Insurance premium: Rs. 3,500/-
                Clause 7.1: Foreclosure permitted after 6 months subject to charges of 4% of outstanding principal plus applicable GST.
                Clause 8.1: Late payment charge of 2% per month on overdue installments.
            """.trimIndent(),
            charCount = 480,
            sourceKind = TextSourceKind.NATIVE_PDF,
        )

        val extraction = DocumentExtractionResult(
            fullText = "${page1.text}\n\n${page2.text}",
            pages = listOf(page1, page2),
            totalPages = 2,
            primarySource = TextSourceKind.NATIVE_PDF,
        )

        val report = LoanAgreementAnalyzer.analyze("Synthetic_FinShield_Agreement.pdf", extraction)

        // Verify that the intentional documentation fee contradiction was flagged and NOT silently reconciled
        val contradictoryRisk = report.riskFindings.firstOrNull {
            it.title.contains("Contradictory Documentation Fee", ignoreCase = true)
        }
        assertNotNull("Expected contradictory documentation fee finding to be detected", contradictoryRisk)
        assertEquals(Severity.HIGH, contradictoryRisk?.severity)
        assertTrue(contradictoryRisk?.explanation?.contains("1,000") == true)
        assertTrue(contradictoryRisk?.explanation?.contains("800") == true)
    }

    @Test
    fun testCompletenessAuditReportsHonestScoresAndIdentifiesAll19Fields() {
        val sampleText = """
            PERSONAL LOAN SANCTION LETTER
            Lender: HDFC Bank Limited
            Borrower: Priya Nair
            Loan Facility: Personal Loan
            Agreement Date: 12/10/2024
            Loan Account No: HDFC-PL-2024-998811
            Sanctioned Principal: Rs. 6,00,000/-
            Actual Disbursed Amount: Rs. 5,85,000/-
            Rate of Interest: 11.5% p.a. on a reducing balance basis
            Annual Percentage Rate (APR): 12.2% APR
            Loan Tenure: 48 months
            Monthly EMI: Rs. 15,650/- payable on the 5th of each month
            Processing Fee: 1.5% plus GST
            Stamp duty: Rs. 500/-
            Documentation charges: Rs. 1,000/-
            Insurance premium: Rs. 4,000/-
            Foreclosure: Permitted after 12 months with 3% foreclosure charges
            Penal interest: 24% p.a. (2% per month) on overdue amounts
            Collateral: Clean and unsecured loan facility
        """.trimIndent()

        val pages = listOf(PageExtraction(1, sampleText, sampleText.length, sourceKind = TextSourceKind.OCR))
        val structured = StructuredLoanExtractor.extract(pages)
        val completeness = CompletenessChecker.audit(pages, structured)

        assertTrue("Expected at least 19 fields checked", completeness.totalFieldsChecked >= 19)
        assertTrue("Expected high completeness score", completeness.completenessPercentage >= 80)

        // Check specific fields
        val lenderItem = completeness.items.first { it.fieldKey == "lender_name" }
        assertEquals(CompletenessStatus.FOUND, lenderItem.status)
        assertEquals("HDFC Bank", lenderItem.extractedValue)

        val principalItem = completeness.items.first { it.fieldKey == "sanctioned_principal" }
        assertEquals(CompletenessStatus.FOUND, principalItem.status)

        val aprItem = completeness.items.first { it.fieldKey == "apr" }
        assertEquals(CompletenessStatus.FOUND, aprItem.status)
        assertTrue(aprItem.extractedValue?.contains("12.2") == true)

        // Missing field (e.g. Moratorium is not in text)
        val moratoriumItem = completeness.items.first { it.fieldKey == "moratorium" }
        assertEquals(CompletenessStatus.NOT_STATED, moratoriumItem.status)
        assertEquals("Not stated in document", moratoriumItem.extractedValue)
    }

    @Test
    fun testConflictingNetDisbursalAndDeductionsFlagged() {
        // Principal 10,00,000 - deductions of 15,000 (PF 10k + GST 1800 + Stamp 500 + Doc 1000) = 9,86,700 expected.
        // Stated disbursal is 9,50,000 (diff of 36,700 unexplained)
        val text = """
            LOAN DISBURSEMENT SCHEDULE
            Sanctioned Amount: Rs. 10,00,000/-
            Net Disbursed Amount credited to account: Rs. 9,50,000/-
            Processing Fee: Rs. 10,000/- + applicable GST
            Stamp duty: Rs. 500/-
            Documentation charges: Rs. 1,000/-
            Interest rate: 10.0% reducing balance
            Tenure: 36 months
            EMI: Rs. 32,267/-
        """.trimIndent()

        val extraction = DocumentExtractionResult(
            fullText = text,
            pages = listOf(PageExtraction(1, text, text.length)),
            totalPages = 1,
        )

        val report = LoanAgreementAnalyzer.analyze("Disbursal_Mismatch.pdf", extraction)

        val disbursalRisk = report.riskFindings.firstOrNull {
            it.title.contains("Net Disbursal Mismatch", ignoreCase = true)
        }
        assertNotNull("Expected net disbursal mismatch finding", disbursalRisk)
        assertEquals(Severity.HIGH, disbursalRisk?.severity)
        assertTrue(disbursalRisk?.financialImpact != null && disbursalRisk.financialImpact!! > 30_000.0)
    }

    @Test
    fun testTableRowsAndSplitColumnsPreserved() {
        val tableText = """
            SCHEDULE OF CHARGES
            --------------------------------------------------------
            Charge Description              | Rate / Amount Payable
            --------------------------------------------------------
            Sanctioned Principal            | Rs. 8,00,000/-
            Annual Interest Rate            | 13.0% reducing balance
            Tenure in Months                | 60 months
            Monthly EMI Instalment          | Rs. 18,206/-
            Processing Fee                  | 2.0% of loan amount
            Stamp Duty Charges              | Rs. 600/-
            Administrative Fee              | Rs. 1,200/-
            Credit Life Insurance           | Rs. 5,500/-
            NACH Dishonour / Bounce         | Rs. 500/- per return
            Prepayment Penalty              | 4.0% of outstanding
            --------------------------------------------------------
        """.trimIndent()

        val pages = listOf(PageExtraction(1, tableText, tableText.length, sourceKind = TextSourceKind.NATIVE_PDF))
        val structured = StructuredLoanExtractor.extract(pages)

        assertEquals(800_000.0, structured.sanctionedPrincipal?.value ?: 0.0, 0.01)
        assertEquals(13.0, structured.annualRatePercent?.value ?: 0.0, 0.01)
        assertEquals(60, structured.tenureMonths?.value ?: 0)
        assertEquals(18_206.0, structured.statedEmi?.value ?: 0.0, 0.01)
        assertEquals(600.0, structured.stampDuty?.value ?: 0.0, 0.01)
        assertEquals(1_200.0, structured.documentationCharges?.value ?: 0.0, 0.01)
        assertEquals(5_500.0, structured.insurancePremium?.value ?: 0.0, 0.01)
    }

    @Test
    fun testValuesFoundOnLaterPagesAndAnnexures() {
        val page1 = PageExtraction(
            pageNumber = 1,
            text = """
                LOAN SANCTION LETTER
                Borrower: Sunil Verma
                Sanctioned Amount: Rs. 15,00,000/-
                Rate of Interest: 10.5% p.a.
            """.trimIndent(),
            charCount = 150,
        )

        val page2 = PageExtraction(
            pageNumber = 2,
            text = """
                REPAYMENT SCHEDULE
                Tenure: 84 months
                Equated Monthly Installment: Rs. 25,248/-
            """.trimIndent(),
            charCount = 100,
        )

        val page3 = PageExtraction(
            pageNumber = 3,
            text = """
                ANNEXURE IV: FEE TARIFF
                Processing fee: Rs. 15,000/-
                Prepayment charges: 3% on prepaid principal amount
                Late payment penalty: 2% per month
            """.trimIndent(),
            charCount = 120,
        )

        val pages = listOf(page1, page2, page3)
        val structured = StructuredLoanExtractor.extract(pages)

        assertEquals(1_500_000.0, structured.sanctionedPrincipal?.value ?: 0.0, 0.01)
        assertEquals(1, structured.sanctionedPrincipal?.pageNumber)

        assertEquals(84, structured.tenureMonths?.value ?: 0)
        assertEquals(2, structured.tenureMonths?.pageNumber)

        assertEquals(25_248.0, structured.statedEmi?.value ?: 0.0, 0.01)
        assertEquals(2, structured.statedEmi?.pageNumber)

        assertEquals(15_000.0, structured.processingFee?.feeAmount ?: 0.0, 0.01)
        assertEquals(3, structured.processingFee?.pageNumber)
    }

    @Test
    fun testUnreadablePageAndMissingInformationHonesty() {
        val blankPage = PageExtraction(
            pageNumber = 1,
            text = "SCAN 001",
            charCount = 8,
            isBlank = true,
            isUnreadable = true,
            warnings = listOf("Page 1 appeared blank or had negligible text."),
        )

        val extraction = DocumentExtractionResult(
            fullText = blankPage.text,
            pages = listOf(blankPage),
            totalPages = 1,
            warnings = blankPage.warnings,
        )

        val report = LoanAgreementAnalyzer.analyze("Unreadable_Scan.pdf", extraction)

        assertNull(report.principal)
        assertNull(report.annualRatePercent)
        assertTrue(report.missingInformation.isNotEmpty())
        assertTrue(report.manualReview.any { it.contains("Ensure the physical agreement or PDF is fully readable", ignoreCase = true) })

        val completeness = report.completenessReport
        assertNotNull(completeness)
        assertEquals(0, completeness?.completenessPercentage)
        assertEquals(completeness?.totalFieldsChecked, completeness?.notStatedCount)
    }

    @Test
    fun testGeminiAssistantFallbackWhenServiceThrowsException() = runBlocking {
        // Create an assistant subclass that simulates a Firebase Vertex AI error / offline state
        val simulatedAssistant = object : GeminiLoanAssistant() {
            override suspend fun extractStructuredTerms(pages: List<PageExtraction>, timeoutMs: Long): GeminiExtractionResult {
                return GeminiExtractionResult.Failure("Simulated Vertex AI 403 / unprovisioned project billing", isQuotaOrPermission = true)
            }
        }

        val text = "Sanctioned Amount: Rs. 3,50,000/-. Rate: 12% p.a. reducing. Tenure: 24 months. EMI: Rs. 16,478/-."
        val pages = listOf(PageExtraction(1, text, text.length))
        val extraction = DocumentExtractionResult(text, pages, 1)

        val report = LoanAgreementAnalyzer.analyzeWithAi(
            documentName = "Fallback_Test.pdf",
            extraction = extraction,
            geminiAssistant = simulatedAssistant,
        )

        // Must seamlessly succeed using deterministic extraction without crashing!
        assertNotNull(report)
        assertEquals(350_000.0, report.principal ?: 0.0, 0.01)
        assertEquals(12.0, report.annualRatePercent ?: 0.0, 0.01)
        assertEquals(24, report.tenureMonths ?: 0)
        assertEquals(16_478.0, report.statedEmi ?: 0.0, 0.01)
    }

    @Test
    fun testNoHallucinatedValuesForAbsentFields() {
        // Extremely minimal agreement with only principal stated
        val text = "LOAN MEMORANDUM. The bank sanctions a loan of Rs. 1,00,000/- to the borrower."
        val pages = listOf(PageExtraction(1, text, text.length))
        val extraction = DocumentExtractionResult(text, pages, 1)

        val report = LoanAgreementAnalyzer.analyze("Minimal.pdf", extraction)

        assertEquals(100_000.0, report.principal ?: 0.0, 0.01)
        assertNull(report.annualRatePercent)
        assertNull(report.tenureMonths)
        assertNull(report.statedEmi)
        assertNull(report.financialBreakdown.processingFeeAmount)
        assertNull(report.financialBreakdown.stampDuty)
        assertNull(report.financialBreakdown.documentationCharges)

        // Completeness check confirms absent fields are marked NOT_STATED
        val completeness = report.completenessReport
        assertNotNull(completeness)
        val rateItem = completeness?.items?.first { it.fieldKey == "interest_rate" }
        assertEquals(CompletenessStatus.NOT_STATED, rateItem?.status)
        assertEquals("Not stated in document", rateItem?.extractedValue)
    }
}
