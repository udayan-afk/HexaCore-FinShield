package com.finshield.app.model

import com.finshield.app.domain.analyzer.PageExtraction
import com.finshield.app.domain.calculator.InterestMethod
import com.finshield.app.domain.calculator.LoanCalculator
import com.finshield.app.domain.calculator.LoanInput
import com.finshield.app.domain.calculator.Money
import java.util.UUID

enum class EvidenceKind {
    EXPLICIT,
    HEURISTIC,
    CALCULATED,
    ASSUMED,
    NOT_FOUND,
}

enum class ChargeType(val label: String) {
    UPFRONT("Upfront Deduction"),
    RECURRING("Recurring / Amortized"),
    CONDITIONAL("Conditional / Penalty"),
    UNKNOWN("Unknown / To Confirm"),
}

enum class Severity(val label: String) {
    HIGH("High Attention"),
    MEDIUM("Review Required"),
    LOW("Minor / Notice"),
    INFO("Verified"),
}

enum class ObligationStatus(val label: String) {
    EXTRACTED("Extracted"),
    REVIEW_REQUIRED("Review Required"),
    NOT_FOUND("Not Found in Analyzed Text"),
}

/** One legacy extracted item, with the text it came from when known. */
data class Finding(
    val label: String,
    val value: String?,
    val evidence: String?,
    val kind: EvidenceKind,
    val pageNumber: Int? = null,
    val clauseReference: String? = null,
)

/** Detailed single financial field with extraction and calculation provenance. */
data class FinancialField<T>(
    val label: String,
    val value: T?,
    val displayText: String,
    val rawText: String? = null,
    val pageNumber: Int? = null,
    val clauseReference: String? = null,
    val evidence: String? = null,
    val kind: EvidenceKind = EvidenceKind.EXPLICIT,
)

/** Itemized financial charge model supporting exact formula, taxes, deductions and assumptions. */
data class FinancialCharge(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val chargeType: ChargeType,
    val percentage: Double? = null,
    val fixedAmount: Double? = null,
    val formulaDescription: String,
    val calculationBase: String? = null,
    val gstRatePercent: Double? = null,
    val isGstIncluded: Boolean = false,
    val calculatedAmount: Double? = null,
    val gstAmount: Double? = null,
    val totalWithGst: Double? = null,
    val isUpfrontDeduction: Boolean = false,
    val assumptions: String? = null,
    val evidence: String? = null,
    val pageNumber: Int? = null,
    val clauseReference: String? = null,
    val kind: EvidenceKind = EvidenceKind.EXPLICIT,
)

/** Identified risk or review item with plain-English explanation and financial impact. */
data class RiskFinding(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val explanation: String,
    val severity: Severity,
    val financialImpact: Double? = null,
    val financialImpactDescription: String? = null,
    val evidence: String? = null,
    val pageNumber: Int? = null,
    val clauseReference: String? = null,
)

/** Key borrower obligation category with verified status. */
data class KeyObligation(
    val category: String,
    val summary: String,
    val status: ObligationStatus,
    val details: String,
    val evidence: String? = null,
    val pageNumber: Int? = null,
    val clauseReference: String? = null,
)

/** Complete snapshot of the loan terms extracted vs calculated. */
data class LoanSnapshot(
    val loanType: String = "Personal Loan",
    val sanctionedPrincipal: Double?,
    val annualRatePercent: Double?,
    val interestMethod: InterestMethod?,
    val tenureMonths: Int?,
    val tenureYears: Double?,
    val statedEmi: Double?,
    val calculatedEmi: Double?,
    val emiDifference: Double?,
    val statedTotalRepayment: Double?,
    val calculatedTotalRepayment: Double?,
    val totalRepaymentDifference: Double?,
)

/** Complete, rigorous itemized financial breakdown. */
data class FinancialBreakdown(
    val sanctionedPrincipal: Double?,
    val processingFeeAmount: Double?,
    val processingFeeGst: Double?,
    val stampDuty: Double?,
    val documentationCharges: Double?,
    val insuranceCharges: Double?,
    val otherUpfrontDeductions: Double?,
    val totalUpfrontDeductions: Double?,
    val estimatedNetDisbursal: Double?,
    val totalInterest: Double?,
    val totalScheduledEmis: Double?,
    val totalRepaymentScheduled: Double?,
    val otherMandatoryCharges: Double?,
    val totalEstimatedBorrowerCost: Double?,
    val itemizedCharges: List<FinancialCharge> = emptyList(),
)

data class AnalysisReport(
    val id: String = UUID.randomUUID().toString(),
    val documentName: String,
    /** True when the content is built-in illustrative data and not extracted from a real document. */
    val isSample: Boolean,
    val analyzedAt: Long = System.currentTimeMillis(),
    val pagesCount: Int = 1,
    val isSessionOnly: Boolean = true,
    val loanSnapshot: LoanSnapshot,
    val financialBreakdown: FinancialBreakdown,
    val riskFindings: List<RiskFinding>,
    val feesAndCharges: List<FinancialCharge>,
    val keyObligations: List<KeyObligation>,
    val questionsForLender: List<String>,
    val rawPages: List<PageExtraction> = emptyList(),

    // Backwards-compatible properties
    val principal: Double? = loanSnapshot.sanctionedPrincipal,
    val annualRatePercent: Double? = loanSnapshot.annualRatePercent,
    val tenureMonths: Int? = loanSnapshot.tenureMonths,
    val statedEmi: Double? = loanSnapshot.statedEmi,
    val terms: List<Finding> = emptyList(),
    val feesAndPenalties: List<Finding> = emptyList(),
    val missingInformation: List<String> = emptyList(),
    val manualReview: List<String> = emptyList(),
)

/** Rich illustrative reports used to preview the complete report layout. */
object SampleReports {
    fun forId(id: String): AnalysisReport {
        val name = com.finshield.app.data.history.DemoHistoryRepository.SAMPLES.firstOrNull { it.id == id }?.documentName
            ?: "Sample_Personal_Loan.pdf"
        return build(name, id)
    }

    private fun build(name: String, id: String): AnalysisReport {
        val principal = 500_000.0
        val rate = 12.5
        val tenure = 36
        val statedEmi = 16_900.0
        val calc = LoanCalculator.calculate(LoanInput(principal, rate, tenure, InterestMethod.REDUCING_BALANCE))
        val emiDiff = statedEmi - calc.installment
        val statedTotal = statedEmi * tenure
        val totalDiff = statedTotal - calc.totalRepayment

        // Fees
        val procFeeAmount = 10_000.0 // 2%
        val procGst = 1_800.0 // 18% standard GST
        val stampDuty = 500.0
        val docCharge = 1_000.0
        val insurance = 3_500.0
        val totalUpfront = procFeeAmount + procGst + stampDuty + docCharge + insurance
        val netDisbursal = principal - totalUpfront
        val totalBorrowerCost = calc.totalRepayment + totalUpfront

        val charges = listOf(
            FinancialCharge(
                name = "Sanctioned Loan Principal",
                chargeType = ChargeType.UPFRONT,
                fixedAmount = principal,
                formulaDescription = "Base loan amount approved by the lender",
                calculationBase = "Sanctioned Amount",
                calculatedAmount = principal,
                totalWithGst = principal,
                pageNumber = 1,
                clauseReference = "Clause 1.1",
                evidence = "The Lender sanctions a Loan Facility of Rs. 5,00,000/- (Rupees Five Lakhs only).",
                kind = EvidenceKind.EXPLICIT,
            ),
            FinancialCharge(
                name = "Processing Fee",
                chargeType = ChargeType.UPFRONT,
                percentage = 2.0,
                formulaDescription = "2.0% of sanctioned principal + 18% GST",
                calculationBase = "Sanctioned Principal",
                gstRatePercent = 18.0,
                calculatedAmount = procFeeAmount,
                gstAmount = procGst,
                totalWithGst = procFeeAmount,
                isUpfrontDeduction = true,
                assumptions = "18% statutory GST applied as per Indian financial regulations.",
                pageNumber = 1,
                clauseReference = "Clause 4.1",
                evidence = "Processing fee of 2% of the sanctioned amount plus applicable taxes shall be deducted at disbursal.",
                kind = EvidenceKind.EXPLICIT,
            ),
            FinancialCharge(
                name = "GST on Processing Fee",
                chargeType = ChargeType.UPFRONT,
                fixedAmount = procGst,
                formulaDescription = "18% on ₹10,000 processing fee",
                calculationBase = "Processing Fee",
                calculatedAmount = procGst,
                totalWithGst = procGst,
                isUpfrontDeduction = true,
                pageNumber = 1,
                clauseReference = "Clause 4.1",
                evidence = "plus applicable taxes (GST @ 18%)",
                kind = EvidenceKind.EXPLICIT,
            ),
            FinancialCharge(
                name = "Stamp Duty",
                chargeType = ChargeType.UPFRONT,
                fixedAmount = stampDuty,
                formulaDescription = "Statutory state stamp duty on loan agreement",
                calculationBase = "State Schedule",
                calculatedAmount = stampDuty,
                totalWithGst = stampDuty,
                isUpfrontDeduction = true,
                pageNumber = 2,
                clauseReference = "Schedule B",
                evidence = "Stamp duty charges of Rs. 500/- payable prior to disbursement.",
                kind = EvidenceKind.EXPLICIT,
            ),
            FinancialCharge(
                name = "Documentation / Admin Charges",
                chargeType = ChargeType.UPFRONT,
                fixedAmount = docCharge,
                formulaDescription = "One-time agreement documentation & verification fee",
                calculatedAmount = docCharge,
                totalWithGst = docCharge,
                isUpfrontDeduction = true,
                pageNumber = 2,
                clauseReference = "Clause 4.3",
                evidence = "Administrative and documentation charge of Rs. 1,000/- shall be deducted from loan disbursement.",
                kind = EvidenceKind.EXPLICIT,
            ),
            FinancialCharge(
                name = "Credit Shield Insurance",
                chargeType = ChargeType.UPFRONT,
                fixedAmount = insurance,
                formulaDescription = "Group credit life protection premium (optional add-on)",
                calculatedAmount = insurance,
                totalWithGst = insurance,
                isUpfrontDeduction = true,
                assumptions = "Stated as bundled add-on deducted upfront.",
                pageNumber = 2,
                clauseReference = "Clause 5.2",
                evidence = "Borrower opts for Credit Shield Insurance coverage with one-time premium of Rs. 3,500/-.",
                kind = EvidenceKind.EXPLICIT,
            ),
            FinancialCharge(
                name = "Total Scheduled Interest",
                chargeType = ChargeType.RECURRING,
                formulaDescription = "Amortized interest at 12.5% p.a. reducing balance over 36 months",
                calculationBase = "Outstanding Principal Monthly",
                calculatedAmount = calc.totalInterest,
                totalWithGst = calc.totalInterest,
                pageNumber = 1,
                clauseReference = "Clause 2.1",
                evidence = "Interest at 12.5% per annum on a reducing balance basis.",
                kind = EvidenceKind.CALCULATED,
            ),
            FinancialCharge(
                name = "Foreclosure / Prepayment Penalty",
                chargeType = ChargeType.CONDITIONAL,
                percentage = 4.0,
                formulaDescription = "4.0% of outstanding principal at foreclosure + 18% GST",
                calculationBase = "Outstanding Principal at prepayment date",
                gstRatePercent = 18.0,
                assumptions = "Charge applies if loan closed before 36 months after 6-month lock-in. Illustrative cost at Month 12: ~₹16,373.",
                pageNumber = 2,
                clauseReference = "Clause 7.1",
                evidence = "Foreclosure permitted after 6 months subject to a fee of 4% of outstanding principal plus applicable taxes.",
                kind = EvidenceKind.EXPLICIT,
            ),
            FinancialCharge(
                name = "Late Payment Penal Interest",
                chargeType = ChargeType.CONDITIONAL,
                percentage = 2.0,
                formulaDescription = "2.0% per month (24% p.a.) on overdue installment amount",
                calculationBase = "Overdue EMI Amount",
                assumptions = "Calculated per month of delay until resolved.",
                pageNumber = 2,
                clauseReference = "Clause 8.1",
                evidence = "Default in payment of EMI attracts penal charge of 2% per month on the overdue amount.",
                kind = EvidenceKind.EXPLICIT,
            ),
            FinancialCharge(
                name = "ECS / NACH Bounce Charge",
                chargeType = ChargeType.CONDITIONAL,
                fixedAmount = 500.0,
                formulaDescription = "₹500 fixed per dishonored auto-debit presentation",
                pageNumber = 2,
                clauseReference = "Clause 8.2",
                evidence = "Cheque or NACH dishonour charge of Rs. 500/- per occurrence.",
                kind = EvidenceKind.EXPLICIT,
            ),
        )

        val riskFindings = listOf(
            RiskFinding(
                title = "Stated EMI Exceeds Standard Calculation",
                explanation = "The agreement states an EMI of ${Money.inr(statedEmi)}, which is ${Money.inr(emiDiff)} per month higher than the standard 12.5% reducing-balance amortisation (${Money.inr(calc.installment)}). Over 36 months, this amounts to an unexplained extra cost of ${Money.inr(totalDiff)}.",
                severity = Severity.HIGH,
                financialImpact = totalDiff,
                financialImpactDescription = "${Money.inr(totalDiff)} extra cost over 36 months",
                evidence = "\"EMI of Rs. 16,900/- payable on the 5th of each month for 36 months.\"",
                pageNumber = 1,
                clauseReference = "Clause 3.2",
            ),
            RiskFinding(
                title = "Significant Foreclosure Charge (4% + GST)",
                explanation = "Foreclosing or refinancing your loan attracts a 4% penalty on the remaining principal plus GST. For instance, foreclosing after 1 year on an outstanding balance of ~₹3,46,878 costs an estimated ₹16,373 in fees and taxes.",
                severity = Severity.MEDIUM,
                financialImpact = 16_373.0,
                financialImpactDescription = "Estimated ~₹16,373 if closed at month 12",
                evidence = "\"…foreclosure may be permitted subject to charges of 4% of outstanding principal plus applicable GST.\"",
                pageNumber = 2,
                clauseReference = "Clause 7.1",
            ),
            RiskFinding(
                title = "Lender Discretionary Charge Revision Clause",
                explanation = "Clause 11.4 allows the lender to revise interest spreads and administrative charges unilaterally 'as notified by the lender' without requiring explicit borrower re-consent.",
                severity = Severity.MEDIUM,
                evidence = "\"The Lender reserves the right to vary the interest rate spread or charges at its sole discretion from time to time as notified to the Borrower.\"",
                pageNumber = 2,
                clauseReference = "Clause 11.4",
            ),
            RiskFinding(
                title = "Upfront Deductions Exceed ₹16,800",
                explanation = "The estimated net amount credited to your account is ${Money.inr(netDisbursal)} after deduction of processing fee, 18% GST, stamp duty, admin fee and credit life insurance.",
                severity = Severity.LOW,
                financialImpact = totalUpfront,
                financialImpactDescription = "${Money.inr(totalUpfront)} deducted at disbursal",
                evidence = "\"All processing, legal, insurance and statutory charges shall be deducted directly from loan disbursement.\"",
                pageNumber = 1,
                clauseReference = "Clause 4.4",
            ),
        )

        val keyObligations = listOf(
            KeyObligation(
                category = "Prepayment & Foreclosure",
                summary = "Lock-in period of 6 months; 4% fee on outstanding principal thereafter",
                status = ObligationStatus.EXTRACTED,
                details = "Prepayment not permitted in first 6 months. Foreclosure fee of 4% + GST applies on outstanding balance.",
                evidence = "Clause 7.1: Foreclosure permitted after 6 months subject to charges of 4% of outstanding principal plus applicable GST.",
                pageNumber = 2,
                clauseReference = "Clause 7.1",
            ),
            KeyObligation(
                category = "Collateral & Security",
                summary = "Unsecured facility; demand promissory note required",
                status = ObligationStatus.EXTRACTED,
                details = "No physical mortgage or hypothecation; secured by personal guarantee and demand promissory note.",
                evidence = "Clause 6.1: Loan is clean/unsecured backed by Demand Promissory Note and continuous personal covenant.",
                pageNumber = 2,
                clauseReference = "Clause 6.1",
            ),
            KeyObligation(
                category = "Guarantor Obligations",
                summary = "No co-borrower or guarantor specified in scanned pages",
                status = ObligationStatus.NOT_FOUND,
                details = "No explicit guarantor requirement was found in the analyzed agreement text. ('Not found' does not guarantee absence in separate annexures).",
                evidence = null,
                pageNumber = null,
            ),
            KeyObligation(
                category = "Insurance Terms",
                summary = "Credit life protection insurance premium of ₹3,500 deducted upfront",
                status = ObligationStatus.REVIEW_REQUIRED,
                details = "Borrower is enrolled in Credit Shield Insurance. Verify with lender if this policy is mandatory or optional.",
                evidence = "Clause 5.2: Borrower opts for Credit Shield Insurance coverage with one-time premium of Rs. 3,500/-.",
                pageNumber = 2,
                clauseReference = "Clause 5.2",
            ),
            KeyObligation(
                category = "Late Payment Terms",
                summary = "Penal interest of 2% per month (24% p.a.) + ₹500 bounce charge",
                status = ObligationStatus.EXTRACTED,
                details = "Overdue installments accrue 2% per month penal interest. Each NACH/ECS return incurs ₹500 bounce penalty.",
                evidence = "Clause 8.1 & 8.2: Default in payment attracts penal charge of 2% per month and bounce fee of Rs. 500.",
                pageNumber = 2,
                clauseReference = "Clause 8.1",
            ),
            KeyObligation(
                category = "Interest Rate Variation",
                summary = "Subject to revision at the sole discretion of the lender",
                status = ObligationStatus.REVIEW_REQUIRED,
                details = "The agreement contains a discretionary revision clause allowing rate adjustments upon notice.",
                evidence = "Clause 11.4: Lender reserves the right to vary the interest rate spread or charges at its sole discretion.",
                pageNumber = 2,
                clauseReference = "Clause 11.4",
            ),
            KeyObligation(
                category = "Other Borrower Obligations",
                summary = "Prompt notification of address, employment, or bank account change within 14 days",
                status = ObligationStatus.EXTRACTED,
                details = "Borrower must notify lender in writing of any change in residence, employer or banking mandate within 14 days.",
                evidence = "Clause 12.3: Borrower covenants to inform the Lender of any address or employment change within 14 days.",
                pageNumber = 2,
                clauseReference = "Clause 12.3",
            ),
        )

        val questions = listOf(
            "Why is the stated EMI (${Money.inr(statedEmi)}) higher than the calculated 12.5% reducing amortisation EMI (${Money.inr(calc.installment)})?",
            "Is the Credit Shield Insurance premium of ${Money.inr(insurance)} mandatory or can it be opted out?",
            "Under what specific benchmark conditions can the lender exercise its clause 11.4 discretion to revise interest rates?",
            "Can you provide the complete month-by-month amortisation schedule showing the principal and interest split for all 36 months?",
            "Are there any waiver options for the 4% foreclosure charge if prepaying from own verifiable funds?",
        )

        val rawPages = listOf(
            PageExtraction(
                pageNumber = 1,
                text = """
                    LOAN AGREEMENT & SANCTION SCHEDULE
                    Lender: Apex Financial Services Ltd.
                    Borrower: Rahul Sharma
                    Loan Facility: Personal Loan Facility
                    1. PRINCIPAL LOAN AMOUNT: Rs. 5,00,000/- (Rupees Five Lakhs Only)
                    2. RATE OF INTEREST: 12.5% per annum on a reducing balance basis.
                    3. TENURE & REPAYMENT: Repayable in 36 monthly instalments.
                    Clause 3.2: Equated Monthly Installment (EMI) of Rs. 16,900/- payable on the 5th of each calendar month.
                    Clause 4.1: Processing fee of 2% of the sanctioned amount plus applicable taxes shall be deducted at disbursal.
                    Clause 4.4: All processing, legal, insurance and statutory charges shall be deducted directly from loan disbursement.
                """.trimIndent(),
                charCount = 680,
                isBlank = false,
            ),
            PageExtraction(
                pageNumber = 2,
                text = """
                    SCHEDULE B - CHARGES & COVENANTS
                    Clause 4.3: Administrative and documentation charge of Rs. 1,000/- shall be deducted from loan disbursement.
                    Clause 5.2: Borrower opts for Credit Shield Insurance coverage with one-time premium of Rs. 3,500/-.
                    Clause 6.1: Loan is clean/unsecured backed by Demand Promissory Note and continuous personal covenant.
                    Clause 7.1: Foreclosure permitted after 6 months subject to charges of 4% of outstanding principal plus applicable GST.
                    Clause 8.1: Default in payment of EMI attracts penal charge of 2% per month on the overdue amount.
                    Clause 8.2: Cheque or NACH dishonour charge of Rs. 500/- per occurrence.
                    Clause 11.4: The Lender reserves the right to vary the interest rate spread or charges at its sole discretion from time to time as notified to the Borrower.
                    Clause 12.3: Borrower covenants to inform the Lender of any address or employment change within 14 days.
                    Schedule B: Stamp duty charges of Rs. 500/- payable prior to disbursement.
                """.trimIndent(),
                charCount = 920,
                isBlank = false,
            ),
        )

        val snapshot = LoanSnapshot(
            loanType = "Personal Loan",
            sanctionedPrincipal = principal,
            annualRatePercent = rate,
            interestMethod = InterestMethod.REDUCING_BALANCE,
            tenureMonths = tenure,
            tenureYears = 3.0,
            statedEmi = statedEmi,
            calculatedEmi = calc.installment,
            emiDifference = emiDiff,
            statedTotalRepayment = statedTotal,
            calculatedTotalRepayment = calc.totalRepayment,
            totalRepaymentDifference = totalDiff,
        )

        val breakdown = FinancialBreakdown(
            sanctionedPrincipal = principal,
            processingFeeAmount = procFeeAmount,
            processingFeeGst = procGst,
            stampDuty = stampDuty,
            documentationCharges = docCharge,
            insuranceCharges = insurance,
            otherUpfrontDeductions = 0.0,
            totalUpfrontDeductions = totalUpfront,
            estimatedNetDisbursal = netDisbursal,
            totalInterest = calc.totalInterest,
            totalScheduledEmis = calc.totalRepayment,
            totalRepaymentScheduled = calc.totalRepayment,
            otherMandatoryCharges = 0.0,
            totalEstimatedBorrowerCost = totalBorrowerCost,
            itemizedCharges = charges,
        )

        val legacyTerms = listOf(
            Finding("Sanctioned amount", Money.inr(principal), "\"The Lender sanctions a Loan Facility of Rs. 5,00,000/- (Rupees Five Lakhs only).\"", EvidenceKind.EXPLICIT, 1, "Clause 1.1"),
            Finding("Interest rate", "12.5% per annum", "\"…interest at 12.5% per annum on a reducing balance basis.\"", EvidenceKind.EXPLICIT, 1, "Clause 2.1"),
            Finding("Tenure", "36 months (3 years)", "\"…repayable in 36 monthly instalments.\"", EvidenceKind.EXPLICIT, 1, "Clause 3.1"),
            Finding("Stated EMI", Money.inr(statedEmi), "\"EMI of Rs. 16,900/- payable on the 5th of each month.\"", EvidenceKind.EXPLICIT, 1, "Clause 3.2"),
        )

        val legacyFees = listOf(
            Finding("Processing fee", "2% of amount + taxes", "\"…a processing fee of 2% of the sanctioned amount plus applicable taxes…\"", EvidenceKind.EXPLICIT, 1, "Clause 4.1"),
            Finding("Late-payment charge", "2% per month on overdue amount", "\"…overdue amounts attract charges at 2% per month…\"", EvidenceKind.EXPLICIT, 2, "Clause 8.1"),
            Finding("Prepayment / foreclosure", "4% of outstanding principal + GST", "\"…foreclosure permitted after 6 months subject to charges of 4% of outstanding principal plus applicable GST.\"", EvidenceKind.EXPLICIT, 2, "Clause 7.1"),
        )

        return AnalysisReport(
            id = id,
            documentName = name,
            isSample = true,
            analyzedAt = System.currentTimeMillis(),
            pagesCount = 2,
            isSessionOnly = true,
            loanSnapshot = snapshot,
            financialBreakdown = breakdown,
            riskFindings = riskFindings,
            feesAndCharges = charges,
            keyObligations = keyObligations,
            questionsForLender = questions,
            rawPages = rawPages,
            terms = legacyTerms,
            feesAndPenalties = legacyFees,
            missingInformation = listOf(
                "Guarantor terms were not found in the analyzed agreement text.",
            ),
            manualReview = listOf(
                "Stated EMI (${Money.inr(statedEmi)}) differs from standard amortisation (${Money.inr(calc.installment)}) by ${Money.inr(emiDiff)}/mo.",
                "Discretionary variation clause (Clause 11.4): Lender may revise charges upon notification.",
                "Foreclosure charge of 4% plus GST applies after 6-month lock-in.",
            ),
        )
    }
}
