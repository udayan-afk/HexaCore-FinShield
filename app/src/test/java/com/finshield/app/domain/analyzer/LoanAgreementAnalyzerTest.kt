package com.finshield.app.domain.analyzer

import com.finshield.app.domain.calculator.InterestMethod
import com.finshield.app.model.ChargeType
import com.finshield.app.model.EvidenceKind
import com.finshield.app.model.Severity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
        assertEquals(500_000.0, report.loanSnapshot.sanctionedPrincipal ?: 0.0, 0.01)

        // Rate & Method
        assertEquals(12.0, report.annualRatePercent ?: 0.0, 0.01)
        assertEquals(InterestMethod.REDUCING_BALANCE, report.loanSnapshot.interestMethod)

        // Tenure
        assertEquals(36, report.tenureMonths ?: 0)

        // Stated EMI
        assertEquals(16_607.0, report.statedEmi ?: 0.0, 0.01)

        // Terms and kinds
        val sanctionedFinding = report.terms.firstOrNull { it.label == "Sanctioned amount" }
        assertNotNull(sanctionedFinding)
        assertEquals(EvidenceKind.EXPLICIT, sanctionedFinding?.kind)

        // Fees
        assertTrue(report.feesAndPenalties.any { it.label == "Processing fee" && it.kind == EvidenceKind.EXPLICIT })
        assertTrue(report.feesAndPenalties.any { it.label == "Late-payment charge" && it.kind == EvidenceKind.EXPLICIT })

        // Manual review for variation clause
        assertTrue(report.manualReview.any { it.contains("discretion", ignoreCase = true) })
        assertTrue(report.riskFindings.any { it.title.contains("Discretionary", ignoreCase = true) })

        // Financial Breakdown checks
        val fb = report.financialBreakdown
        assertNotNull(fb.sanctionedPrincipal)
        assertEquals(7_500.0, fb.processingFeeAmount ?: 0.0, 0.01) // 1.5% of 5,00,000 = 7,500
        assertEquals(1_350.0, fb.processingFeeGst ?: 0.0, 0.01) // 18% of 7,500 = 1,350
        assertEquals(8_850.0, fb.totalUpfrontDeductions ?: 0.0, 0.01)
        assertEquals(491_150.0, fb.estimatedNetDisbursal ?: 0.0, 0.01) // 5,00,000 - 8,850 = 4,91,150
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

        assertNull(report.principal)
        assertNull(report.annualRatePercent)
        assertNull(report.tenureMonths)
        assertTrue(report.missingInformation.isNotEmpty())
    }

    @Test
    fun testDetectsEmiDiscrepancyAndCalculatesFinancialImpact() {
        // Standard 12% reducing EMI on 5,00,000 for 36 months is ~16,607.
        // Stated EMI is 17,500 (diff of 893/month, ~32,148 over tenure)
        val discrepancyText = """
            LOAN AGREEMENT
            Sanctioned Amount: Rs. 5,00,000/-
            Annual Interest Rate: 12.0% per annum on reducing balance basis
            Tenure: 36 months
            Monthly Installment (EMI): Rs. 17,500/-
        """.trimIndent()

        val extraction = DocumentExtractionResult(
            fullText = discrepancyText,
            pages = listOf(PageExtraction(1, discrepancyText, discrepancyText.length, isBlank = false)),
            totalPages = 1,
        )

        val report = LoanAgreementAnalyzer.analyze("Discrepancy_Agreement.pdf", extraction)

        assertEquals(17_500.0, report.statedEmi ?: 0.0, 0.01)
        val calculatedEmi = report.loanSnapshot.calculatedEmi ?: 0.0
        assertEquals(16_607.0, calculatedEmi, 1.0)

        // Check that discrepancy risk finding was created
        val emiRisk = report.riskFindings.firstOrNull { it.title.contains("EMI Discrepancy", ignoreCase = true) }
        assertNotNull(emiRisk)
        assertEquals(Severity.HIGH, emiRisk?.severity)
        assertTrue(emiRisk?.financialImpact != null && emiRisk.financialImpact!! > 30_000.0)
    }

    @Test
    fun testAmbiguousInterestMethodFlagged() {
        // Text specifies rate but omits whether it is reducing balance or flat rate
        val textWithoutMethod = """
            LOAN AGREEMENT
            Sanctioned Amount: Rs. 3,00,000/-
            Annual Interest Rate: 14.0%
            Tenure: 24 months
            Equated Monthly Installment: Rs. 14,400/-
        """.trimIndent()

        val extraction = DocumentExtractionResult(
            fullText = textWithoutMethod,
            pages = listOf(PageExtraction(1, textWithoutMethod, textWithoutMethod.length, isBlank = false)),
            totalPages = 1,
        )

        val report = LoanAgreementAnalyzer.analyze("Ambiguous_Loan.pdf", extraction)

        assertNull(report.loanSnapshot.interestMethod)
        assertTrue(report.riskFindings.any { it.title.contains("Interest Method Ambiguity", ignoreCase = true) })
    }

    @Test
    fun testItemizedChargesContainCategorizedTypes() {
        val completeText = """
            LOAN AGREEMENT
            Sanctioned Amount: Rs. 2,00,000/-
            Annual Interest Rate: 11.0% reducing balance
            Tenure: 24 months
            Equated Monthly Installment: Rs. 9,322/-
            Processing Fee: Rs. 3,000 + applicable GST
            Stamp duty: Rs. 400
            Documentation charges: Rs. 800
            Late payment charge: 2.5% per month on overdue amount
            Foreclosure: 4% of outstanding principal + GST
        """.trimIndent()

        val extraction = DocumentExtractionResult(
            fullText = completeText,
            pages = listOf(PageExtraction(1, completeText, completeText.length, isBlank = false)),
            totalPages = 1,
        )

        val report = LoanAgreementAnalyzer.analyze("Categorized_Charges.pdf", extraction)

        val charges = report.feesAndCharges
        assertTrue(charges.any { it.chargeType == ChargeType.UPFRONT })
        assertTrue(charges.any { it.chargeType == ChargeType.RECURRING })
        assertTrue(charges.any { it.chargeType == ChargeType.CONDITIONAL })

        // Check net disbursal
        val net = report.financialBreakdown.estimatedNetDisbursal
        assertNotNull(net)
        assertTrue(net!! < 200_000.0)
    }
}
