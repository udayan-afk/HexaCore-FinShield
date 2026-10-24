package com.finshield.app.domain.analyzer

import com.finshield.app.model.EvidenceKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LoanAgreementAnalyzerTest {

    @Test
    fun testExtractsExplicitLoanTermsAccurately() {
        val sampleText = """
            LOAN SANCTION LETTER
            Borrower: John Doe
            Sanctioned Amount: Rs. 5,00,000/- (Rupees Five Lakhs Only)
            Annual Interest Rate: 12.0% p.a. on a reducing balance basis
            Loan Tenure: 36 months payable in regular equated installments
            Equated Monthly Installment (EMI): Rs. 16,607/-
            Processing Fee: 1.5% of loan amount + applicable GST
            Late payment charge: 2.0% per month on overdue installments
            Foreclosure: Permitted after 6 months subject to 3% prepayment penalty
            Note: The Lender reserves the right to revise charges at the discretion of the bank.
        """.trimIndent()

        val extraction = DocumentExtractionResult(
            fullText = sampleText,
            pages = listOf(PageExtraction(1, sampleText, sampleText.length, isBlank = false)),
            totalPages = 1,
        )

        val report = LoanAgreementAnalyzer.analyze("Test_Sanction_Letter.pdf", extraction)

        // Principal
        assertEquals(500_000.0, report.principal ?: 0.0, 0.01)

        // Rate
        assertEquals(12.0, report.annualRatePercent ?: 0.0, 0.01)

        // Tenure
        assertEquals(36, report.tenureMonths ?: 0)

        // Stated EMI
        assertEquals(16_607.0, report.statedEmi ?: 0.0, 0.01)

        // Check terms kinds
        val sanctionedFinding = report.terms.firstOrNull { it.label == "Sanctioned amount" }
        assertNotNull(sanctionedFinding)
        assertEquals(EvidenceKind.EXPLICIT, sanctionedFinding?.kind)

        // Fees
        assertTrue(report.feesAndPenalties.any { it.label == "Processing fee" && it.kind == EvidenceKind.EXPLICIT })
        assertTrue(report.feesAndPenalties.any { it.label == "Late-payment charge" && it.kind == EvidenceKind.EXPLICIT })

        // Manual review for variation clause
        assertTrue(report.manualReview.any { it.contains("discretion", ignoreCase = true) })
    }

    @Test
    fun testDetectsMissingInformationWhenOmitted() {
        val incompleteText = "Welcome to your loan agreement. Borrower: Jane Doe. Processing fee is Rs. 2,000."
        val extraction = DocumentExtractionResult(
            fullText = incompleteText,
            pages = listOf(PageExtraction(1, incompleteText, incompleteText.length, isBlank = false)),
            totalPages = 1,
        )

        val report = LoanAgreementAnalyzer.analyze("Incomplete_Doc.pdf", extraction)

        assertEquals(null, report.principal)
        assertEquals(null, report.annualRatePercent)
        assertEquals(null, report.tenureMonths)
        assertTrue(report.missingInformation.isNotEmpty())
    }
}
