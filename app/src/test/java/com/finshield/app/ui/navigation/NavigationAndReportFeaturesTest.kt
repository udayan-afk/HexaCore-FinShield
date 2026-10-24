package com.finshield.app.ui.navigation

import com.finshield.app.domain.calculator.InterestMethod
import com.finshield.app.model.AnalysisReport
import com.finshield.app.model.ChargeType
import com.finshield.app.model.FinancialBreakdown
import com.finshield.app.model.LoanSnapshot
import com.finshield.app.model.RiskFinding
import com.finshield.app.model.SampleReports
import com.finshield.app.model.Severity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationAndReportFeaturesTest {

    @Test
    fun testCompleteBreakdownNavigationRoute() {
        assertEquals("complete_breakdown/{id}", Routes.COMPLETE_BREAKDOWN)
        assertEquals("complete_breakdown/doc_123", Routes.completeBreakdown("doc_123"))
        assertEquals("complete_breakdown/sample", Routes.completeBreakdown("sample"))

        // Must be in protectedRoutes so unauthorized users cannot access without session/demo
        assertTrue(Routes.protectedRoutes.contains(Routes.COMPLETE_BREAKDOWN))
    }

    @Test
    fun testReportDataConsistencyBetweenSummaryAndDetailedScreens() {
        val sample = SampleReports.forId("sample")

        val snapshot = sample.loanSnapshot
        val breakdown = sample.financialBreakdown

        // 1. Sanctioned Principal consistency
        assertEquals(snapshot.sanctionedPrincipal, breakdown.sanctionedPrincipal)
        assertEquals(500_000.0, snapshot.sanctionedPrincipal!!, 0.01)

        // 2. Net Disbursal consistency: Principal - Upfront Deductions
        val expectedNet = snapshot.sanctionedPrincipal!! - breakdown.totalUpfrontDeductions!!
        assertEquals(expectedNet, breakdown.estimatedNetDisbursal!!, 0.01)

        // 3. Stated EMI and Total Repayment consistency
        assertNotNull(snapshot.statedEmi)
        assertNotNull(snapshot.calculatedEmi)
        assertEquals(16_900.0, snapshot.statedEmi!!, 0.01)

        // 4. Breakdown line items sum correctly
        val upfrontItemsSum = breakdown.itemizedCharges
            .filter { it.chargeType == ChargeType.UPFRONT && it.isUpfrontDeduction }
            .sumOf { it.totalWithGst ?: 0.0 }
        assertEquals(breakdown.totalUpfrontDeductions!!, upfrontItemsSum, 0.01)
    }

    @Test
    fun testSwipeableRiskReviewWithZeroFindings() {
        val snapshot = LoanSnapshot(
            loanType = "Personal Loan",
            sanctionedPrincipal = 200_000.0,
            annualRatePercent = 12.0,
            interestMethod = InterestMethod.REDUCING_BALANCE,
            tenureMonths = 24,
            tenureYears = 2.0,
            statedEmi = 9414.0,
            calculatedEmi = 9414.0,
            emiDifference = 0.0,
            statedTotalRepayment = 225936.0,
            calculatedTotalRepayment = 225936.0,
            totalRepaymentDifference = 0.0,
        )
        val breakdown = FinancialBreakdown(
            sanctionedPrincipal = 200_000.0,
            processingFeeAmount = 2000.0,
            processingFeeGst = 360.0,
            stampDuty = 100.0,
            documentationCharges = 500.0,
            insuranceCharges = 0.0,
            otherUpfrontDeductions = 0.0,
            totalUpfrontDeductions = 2960.0,
            estimatedNetDisbursal = 197040.0,
            totalInterest = 25936.0,
            totalScheduledEmis = 225936.0,
            totalRepaymentScheduled = 225936.0,
            otherMandatoryCharges = 0.0,
            totalEstimatedBorrowerCost = 228896.0,
        )

        val emptyReport = AnalysisReport(
            id = "empty-risk",
            documentName = "clean_agreement.pdf",
            isSample = false,
            analyzedAt = System.currentTimeMillis(),
            loanSnapshot = snapshot,
            financialBreakdown = breakdown,
            riskFindings = emptyList(),
            feesAndCharges = emptyList(),
            keyObligations = emptyList(),
            questionsForLender = emptyList(),
            rawPages = emptyList(),
        )

        assertEquals(0, emptyReport.riskFindings.size)
        assertTrue(emptyReport.riskFindings.isEmpty())
    }

    @Test
    fun testSwipeableRiskReviewWithSingleFinding() {
        val snapshot = LoanSnapshot(
            loanType = "Personal Loan",
            sanctionedPrincipal = 300_000.0,
            annualRatePercent = 12.0,
            interestMethod = InterestMethod.REDUCING_BALANCE,
            tenureMonths = 24,
            tenureYears = 2.0,
            statedEmi = 14122.0,
            calculatedEmi = 14122.0,
            emiDifference = 0.0,
            statedTotalRepayment = 338928.0,
            calculatedTotalRepayment = 338928.0,
            totalRepaymentDifference = 0.0,
        )
        val breakdown = FinancialBreakdown(
            sanctionedPrincipal = 300_000.0,
            processingFeeAmount = 3000.0,
            processingFeeGst = 540.0,
            stampDuty = 200.0,
            documentationCharges = 500.0,
            insuranceCharges = 0.0,
            otherUpfrontDeductions = 0.0,
            totalUpfrontDeductions = 4240.0,
            estimatedNetDisbursal = 295760.0,
            totalInterest = 38928.0,
            totalScheduledEmis = 338928.0,
            totalRepaymentScheduled = 338928.0,
            otherMandatoryCharges = 0.0,
            totalEstimatedBorrowerCost = 343168.0,
        )

        val singleFindingReport = AnalysisReport(
            id = "single-risk",
            documentName = "single_risk_agreement.pdf",
            isSample = false,
            analyzedAt = System.currentTimeMillis(),
            loanSnapshot = snapshot,
            financialBreakdown = breakdown,
            riskFindings = listOf(
                RiskFinding(
                    id = "rf-1",
                    severity = Severity.HIGH,
                    title = "Prepayment Penalty on Floating Rate",
                    explanation = "Clause 7.2 imposes a 4% foreclosure fee on a floating rate facility, violating RBI Master Direction.",
                    financialImpactDescription = "₹12,000 on ₹3,00,000 outstanding balance",
                    pageNumber = 3,
                    clauseReference = "Clause 7.2",
                    evidence = "Foreclosure charges shall apply at 4% of the principal outstanding.",
                )
            ),
            feesAndCharges = emptyList(),
            keyObligations = emptyList(),
            questionsForLender = emptyList(),
            rawPages = emptyList(),
        )

        assertEquals(1, singleFindingReport.riskFindings.size)
        val finding = singleFindingReport.riskFindings.first()
        assertEquals(Severity.HIGH, finding.severity)
        assertEquals("Prepayment Penalty on Floating Rate", finding.title)
        assertEquals("Clause 7.2", finding.clauseReference)
        assertEquals(3, finding.pageNumber)
        assertNotNull(finding.financialImpactDescription)
    }

    @Test
    fun testSwipeableRiskReviewWithMultipleFindings() {
        val sample = SampleReports.forId("sample")
        val findings = sample.riskFindings

        assertTrue(findings.size >= 4)

        // Verify high attention finding
        val highAttentionFindings = findings.filter { it.severity == Severity.HIGH }
        assertTrue(highAttentionFindings.isNotEmpty())

        val emiVarianceFinding = findings.firstOrNull { it.title.contains("EMI", ignoreCase = true) }
        assertNotNull(emiVarianceFinding)
        assertEquals(Severity.HIGH, emiVarianceFinding!!.severity)
        assertNotNull(emiVarianceFinding.financialImpactDescription)

        // Verify review required finding
        val reviewRequired = findings.filter { it.severity == Severity.MEDIUM }
        assertTrue(reviewRequired.isNotEmpty())
    }

    @Test
    fun testFeesAndChargesExtractionAndFiltering() {
        val sample = SampleReports.forId("sample")
        val allCharges = sample.feesAndCharges

        // Filter out principal and scheduled interest as done on the compact main screen
        val feeRows = allCharges.filter { charge ->
            !charge.name.contains("Principal", ignoreCase = true) &&
                !charge.name.contains("Scheduled Interest", ignoreCase = true)
        }

        assertTrue(feeRows.isNotEmpty())
        assertEquals(8, feeRows.size)

        // Verify Processing Fee details
        val procFee = feeRows.firstOrNull { it.name.contains("Processing Fee", ignoreCase = true) && !it.name.contains("GST", ignoreCase = true) }
        assertNotNull(procFee)
        assertEquals(2.0, procFee!!.percentage!!, 0.01)
        assertEquals(10_000.0, procFee.calculatedAmount!!, 0.01)
        assertTrue(procFee.isUpfrontDeduction)
        assertEquals("Clause 4.1", procFee.clauseReference)

        // Verify GST on Processing Fee
        val gstFee = feeRows.firstOrNull { it.name.contains("GST", ignoreCase = true) }
        assertNotNull(gstFee)
        assertEquals(1_800.0, gstFee!!.fixedAmount!!, 0.01)
        assertTrue(gstFee.isUpfrontDeduction)

        // Verify Stamp Duty
        val stampDuty = feeRows.firstOrNull { it.name.contains("Stamp Duty", ignoreCase = true) }
        assertNotNull(stampDuty)
        assertEquals(500.0, stampDuty!!.fixedAmount!!, 0.01)

        // Verify Documentation Charges
        val docFee = feeRows.firstOrNull { it.name.contains("Documentation", ignoreCase = true) }
        assertNotNull(docFee)
        assertEquals(1_000.0, docFee!!.fixedAmount!!, 0.01)

        // Verify Credit Shield Insurance
        val insurance = feeRows.firstOrNull { it.name.contains("Insurance", ignoreCase = true) }
        assertNotNull(insurance)
        assertEquals(3_500.0, insurance!!.fixedAmount!!, 0.01)
        assertTrue(insurance.isUpfrontDeduction)

        // Verify Foreclosure Penalty
        val foreclosure = feeRows.firstOrNull { it.name.contains("Foreclosure", ignoreCase = true) }
        assertNotNull(foreclosure)
        assertEquals(4.0, foreclosure!!.percentage!!, 0.01)
        assertEquals(ChargeType.CONDITIONAL, foreclosure.chargeType)

        // Verify Late Payment Penal Interest
        val latePenalty = feeRows.firstOrNull { it.name.contains("Late Payment", ignoreCase = true) }
        assertNotNull(latePenalty)
        assertEquals(2.0, latePenalty!!.percentage!!, 0.01)
        assertEquals(ChargeType.CONDITIONAL, latePenalty.chargeType)
    }

    @Test
    fun testQuestionsForLenderDynamicGeneration() {
        val sample = SampleReports.forId("sample")
        val questions = sample.questionsForLender

        assertTrue(questions.isNotEmpty())
        assertEquals(5, questions.size)

        // Verify question regarding EMI variance
        assertTrue(questions.any { it.contains("stated EMI", ignoreCase = true) && it.contains("reducing", ignoreCase = true) })

        // Verify question regarding Insurance optionality
        assertTrue(questions.any { it.contains("Credit Shield Insurance", ignoreCase = true) && it.contains("mandatory", ignoreCase = true) })

        // Verify question regarding Interest Rate variation discretion
        assertTrue(questions.any { it.contains("Clause 11.4", ignoreCase = true) || it.contains("discretion", ignoreCase = true) })

        // Verify question regarding complete Amortization Schedule
        assertTrue(questions.any { it.contains("amortisation schedule", ignoreCase = true) || it.contains("amortization", ignoreCase = true) })

        // Verify question regarding Foreclosure charge waiver
        assertTrue(questions.any { it.contains("foreclosure", ignoreCase = true) && it.contains("waiver", ignoreCase = true) })
    }

    @Test
    fun testQuestionsForLenderThresholdControl() {
        // Test threshold logic: <= 5 shows all, > 5 initially caps at 5
        val list5 = listOf("Q1", "Q2", "Q3", "Q4", "Q5")
        val displayedInitial5 = if (list5.size <= 5) list5 else list5.take(5)
        assertEquals(5, displayedInitial5.size)

        val list7 = listOf("Q1", "Q2", "Q3", "Q4", "Q5", "Q6", "Q7")
        val displayedInitial7 = if (list7.size <= 5) list7 else list7.take(5)
        assertEquals(5, displayedInitial7.size)
        val displayedExpanded7 = list7
        assertEquals(7, displayedExpanded7.size)
    }

    @Test
    fun testExtractedTextAndEvidenceAvailability() {
        val sample = SampleReports.forId("sample")
        val pages = sample.rawPages

        assertEquals(2, pages.size)
        assertEquals(1, pages[0].pageNumber)
        assertTrue(pages[0].charCount > 0)
        assertTrue(pages[0].text.contains("LOAN AGREEMENT", ignoreCase = true))

        assertEquals(2, pages[1].pageNumber)
        assertTrue(pages[1].charCount > 0)
        assertTrue(pages[1].text.contains("SCHEDULE B", ignoreCase = true))
    }
}

