package com.finshield.app.domain.analyzer

import com.finshield.app.domain.calculator.InterestMethod
import com.finshield.app.model.EvidenceKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StructuredLoanExtractorTest {

    @Test
    fun testIndianCurrencyAndNumberNormalization() {
        assertEquals(500_000.0, StructuredLoanExtractor.parseIndianNumber("₹5,00,000/-") ?: 0.0, 0.01)
        assertEquals(1_250_000.0, StructuredLoanExtractor.parseIndianNumber("12.50 Lakhs") ?: 0.0, 0.01)
        assertEquals(15_000_000.0, StructuredLoanExtractor.parseIndianNumber("1.5 Crore") ?: 0.0, 0.01)
        assertEquals(75_000.50, StructuredLoanExtractor.parseIndianNumber("INR 75,000.50") ?: 0.0, 0.01)
        assertEquals(250_000.0, StructuredLoanExtractor.parseIndianNumber("Rs. 2.5 Lacs") ?: 0.0, 0.01)
        assertEquals(10_000.0, StructuredLoanExtractor.parseIndianNumber("1O,OOO") ?: 0.0, 0.01) // O/0 confusion handled
    }

    @Test
    fun testMonthlyToAnnualInterestRateNormalization() {
        val pages = listOf(
            PageExtraction(
                pageNumber = 1,
                text = """
                    SANCTION ADVICE
                    Sanctioned Loan Amount: INR 4,00,000/-
                    Rate of Interest: 1.25% per month (reducing balance)
                    Tenure: 24 Months
                    Equated Monthly Installment: Rs. 19,380/-
                """.trimIndent(),
                charCount = 200,
            )
        )

        val result = StructuredLoanExtractor.extract(pages)

        assertNotNull(result.annualRatePercent)
        // 1.25% per month * 12 = 15.0% per annum
        assertEquals(15.0, result.annualRatePercent?.value ?: 0.0, 0.01)
        assertEquals(InterestMethod.REDUCING_BALANCE, result.interestMethod?.value)
        assertEquals(400_000.0, result.sanctionedPrincipal?.value ?: 0.0, 0.01)
        assertEquals(24, result.tenureMonths?.value ?: 0)
    }

    @Test
    fun testFloatingRateAndAprExtraction() {
        val pages = listOf(
            PageExtraction(
                pageNumber = 1,
                text = """
                    MORTGAGE LOAN AGREEMENT
                    Borrower Name: Rajesh Sharma
                    Lender: State Bank of India
                    Agreement Date: 15/09/2024
                    Sanction Letter Ref: SBI/HL/2024/98712
                    Sanctioned Principal: Rs. 50,00,000/-
                    Nominal Interest Rate: 9.25% p.a. floating rate linked to Repo Benchmark
                    Annual Percentage Rate (APR): 9.68% p.a.
                    Tenure: 240 months
                    Monthly EMI: Rs. 45,793/-
                """.trimIndent(),
                charCount = 350,
            )
        )

        val result = StructuredLoanExtractor.extract(pages)

        assertEquals("State Bank of India", result.lenderName?.value)
        assertEquals("Rajesh Sharma", result.borrowerName?.value)
        assertEquals("SBI/HL/2024/98712", result.referenceNumber?.value)
        assertEquals(true, result.isFloatingRate)
        assertEquals(9.25, result.annualRatePercent?.value ?: 0.0, 0.01)
        assertEquals(9.68, result.aprPercent?.value ?: 0.0, 0.01)
    }

    @Test
    fun testDistinguishesSanctionedAmountFromNetDisbursalAndAppNumber() {
        val pages = listOf(
            PageExtraction(
                pageNumber = 1,
                text = """
                    LOAN DISBURSAL ADVICE
                    Application ID: 987654321
                    Mobile Number: 9812345678
                    Sanctioned Loan Amount: Rs. 10,00,000/-
                    Processing Fee (1% + GST): Rs. 11,800/-
                    Net Disbursed Amount credited to account: Rs. 9,88,200/-
                    Tenure: 48 months
                    Interest Rate: 10.5% p.a.
                """.trimIndent(),
                charCount = 300,
            )
        )

        val result = StructuredLoanExtractor.extract(pages)

        assertEquals(1_000_000.0, result.sanctionedPrincipal?.value ?: 0.0, 0.01)
        assertEquals(988_200.0, result.actualDisbursedAmount?.value ?: 0.0, 0.01)
    }

    @Test
    fun testMultiPageProvenanceTracking() {
        val pages = listOf(
            PageExtraction(
                pageNumber = 1,
                text = "SANCTION SUMMARY\nBorrower: Amit Patel\nSanctioned Principal: Rs. 6,00,000/-\nRate: 11.0% p.a. reducing balance",
                charCount = 120,
            ),
            PageExtraction(
                pageNumber = 2,
                text = "REPAYMENT SCHEDULE\nTenure: 36 Months\nMonthly EMI Amount: Rs. 19,645/-\nFirst EMI Date: 05/11/2024",
                charCount = 130,
            ),
            PageExtraction(
                pageNumber = 3,
                text = "TARIFF OF CHARGES\nProcessing fee: Rs. 6,000 + 18% GST\nForeclosure penalty: 3% of outstanding principal\nLate payment penalty: 2% per month",
                charCount = 150,
            )
        )

        val docResult = DocumentExtractionResult(
            fullText = pages.joinToString("\n\n") { it.text },
            pages = pages,
            totalPages = 3,
        )

        val report = LoanAgreementAnalyzer.analyze("MultiPageLoan.pdf", docResult)

        assertEquals(600_000.0, report.loanSnapshot.sanctionedPrincipal ?: 0.0, 0.01)
        assertEquals(36, report.loanSnapshot.tenureMonths ?: 0)
        assertEquals(19_645.0, report.loanSnapshot.statedEmi ?: 0.0, 0.01)

        // Check provenance pages
        val sanctionedTerm = report.terms.firstOrNull { it.label == "Sanctioned amount" }
        assertEquals(1, sanctionedTerm?.pageNumber)

        val tenureTerm = report.terms.firstOrNull { it.label == "Tenure" }
        assertEquals(2, tenureTerm?.pageNumber)

        val emiTerm = report.terms.firstOrNull { it.label == "Stated EMI" }
        assertEquals(2, emiTerm?.pageNumber)
    }

    @Test
    fun testRiskFindingsIncludeAllRequiredAttributes() {
        val pages = listOf(
            PageExtraction(
                pageNumber = 1,
                text = """
                    LOAN AGREEMENT
                    Principal: Rs. 3,00,000/-
                    Rate: 14% p.a.
                    Tenure: 24 months
                    Stated EMI: Rs. 16,500/-
                    Lender reserves the right to vary interest rate unilaterally at any time without prior consent.
                    Foreclosure charges of 5% apply on early closure.
                """.trimIndent(),
                charCount = 250,
            )
        )

        val docResult = DocumentExtractionResult(
            fullText = pages[0].text,
            pages = pages,
            totalPages = 1,
        )

        val report = LoanAgreementAnalyzer.analyze("RiskTest.pdf", docResult)

        assertTrue(report.riskFindings.isNotEmpty())
        for (finding in report.riskFindings) {
            assertFalse("Title must not be blank", finding.title.isBlank())
            assertFalse("Explanation must not be blank", finding.explanation.isBlank())
            assertNotNull("Severity must not be null", finding.severity)
            assertNotNull("Evidence must not be null", finding.evidence)
            assertNotNull("whyItMatters must be populated", finding.whyItMatters)
            assertNotNull("questionToAsk must be populated", finding.questionToAsk)
        }
    }

    @Test
    fun testUserCorrectionAndRecalculation() {
        val originalText = """
            LOAN AGREEMENT
            Sanctioned Amount: Rs. 5,00,000/-
            Annual Interest Rate: 12% p.a. reducing balance
            Tenure: 36 months
            Stated EMI: Rs. 16,607/-
            Processing Fee: Rs. 5,000
        """.trimIndent()

        val docResult = DocumentExtractionResult(
            fullText = originalText,
            pages = listOf(PageExtraction(1, originalText, originalText.length)),
            totalPages = 1,
        )

        val initialReport = LoanAgreementAnalyzer.analyze("TestLoan.pdf", docResult)
        assertEquals(500_000.0, initialReport.loanSnapshot.sanctionedPrincipal ?: 0.0, 0.01)

        // User corrects principal to 6,00,000 and tenure to 48 months
        val recalculatedReport = LoanAgreementAnalyzer.recalculateWithCustomTerms(
            originalReport = initialReport,
            principal = 600_000.0,
            annualRatePercent = 12.0,
            tenureMonths = 48,
            statedEmi = null,
            processingFeeAmount = 10_000.0,
        )

        // Verifications
        assertEquals(600_000.0, recalculatedReport.loanSnapshot.sanctionedPrincipal ?: 0.0, 0.01)
        assertEquals(48, recalculatedReport.loanSnapshot.tenureMonths ?: 0)
        // 600_000 - (10_000 + 1,800 GST) = 588_200
        assertEquals(588_200.0, recalculatedReport.financialBreakdown.estimatedNetDisbursal ?: 0.0, 0.01)

        // Calculated EMI for 6,00,000 at 12% for 48 months:
        // r = 0.01, n = 48 -> 600000 * 0.01 * (1.01^48) / ((1.01^48) - 1) = 15,800.27
        val newEmi = recalculatedReport.loanSnapshot.calculatedEmi ?: 0.0
        assertEquals(15_800.0, newEmi, 2.0)

        // Check that sanctioned amount term kind is USER_CORRECTED
        val principalFinding = recalculatedReport.terms.firstOrNull { it.label == "Sanctioned amount" }
        assertEquals(EvidenceKind.USER_CORRECTED, principalFinding?.kind)
    }
}
