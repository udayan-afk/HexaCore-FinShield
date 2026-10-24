package com.finshield.app.domain.analyzer

import com.finshield.app.domain.calculator.InterestMethod
import com.finshield.app.domain.calculator.LoanCalculator
import com.finshield.app.domain.calculator.LoanInput
import com.finshield.app.domain.calculator.Money
import com.finshield.app.model.AnalysisReport
import com.finshield.app.model.ChargeType
import com.finshield.app.model.EvidenceKind
import com.finshield.app.model.FinancialBreakdown
import com.finshield.app.model.FinancialCharge
import com.finshield.app.model.Finding
import com.finshield.app.model.KeyObligation
import com.finshield.app.model.LoanSnapshot
import com.finshield.app.model.ObligationStatus
import com.finshield.app.model.RiskFinding
import com.finshield.app.model.Severity
import java.util.UUID
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Intelligent loan agreement analyzer.
 * Extracts loan terms via [StructuredLoanExtractor], validates values, computes independent
 * reducing-balance amortisations, checks upfront deductions, and generates evidence-backed risk reviews.
 */
object LoanAgreementAnalyzer {

    fun analyze(
        documentName: String,
        extraction: DocumentExtractionResult,
        reportId: String = UUID.randomUUID().toString(),
        geminiOutput: GeminiStructuredOutput? = null,
    ): AnalysisReport {
        val pages = if (extraction.pages.isNotEmpty()) extraction.pages else listOf(
            PageExtraction(1, extraction.fullText, extraction.fullText.length, isBlank = extraction.fullText.isBlank()),
        )
        val text = extraction.fullText

        val terms = mutableListOf<Finding>()
        val fees = mutableListOf<Finding>()
        val missing = mutableListOf<String>()
        val review = mutableListOf<String>()
        val itemizedCharges = mutableListOf<FinancialCharge>()
        val riskFindings = mutableListOf<RiskFinding>()

        // 1. Structured extraction
        val extracted = StructuredLoanExtractor.extract(pages, text)

        val principalMatch = extracted.sanctionedPrincipal
        val principal = principalMatch?.value

        val rateMatch = extracted.annualRatePercent
        val annualRate = rateMatch?.value

        val tenureMatch = extracted.tenureMonths
        val tenureMonths = tenureMatch?.value

        val methodMatch = extracted.interestMethod
        val interestMethod = methodMatch?.value

        val statedEmiMatch = extracted.statedEmi
        val statedEmi = statedEmiMatch?.value

        val statedTotalMatch = extracted.statedTotalRepayment
        val statedTotalRepayment = statedTotalMatch?.value ?: (if (statedEmi != null && tenureMonths != null) statedEmi * tenureMonths else null)

        val procFeeMatch = extracted.processingFee
        val stampDutyMatch = extracted.stampDuty
        val docChargesMatch = extracted.documentationCharges
        val insuranceMatch = extracted.insurancePremium
        val prepaymentMatch = extracted.prepaymentDetails
        val lateChargeMatch = extracted.latePaymentDetails
        val revisionClauses = extracted.revisionClauses

        // 2. Terms assembly
        if (principalMatch != null) {
            terms.add(
                Finding(
                    label = "Sanctioned amount",
                    value = Money.inr(principalMatch.value),
                    evidence = principalMatch.snippet,
                    kind = principalMatch.kind,
                    pageNumber = principalMatch.pageNumber,
                    clauseReference = principalMatch.clause,
                ),
            )
            itemizedCharges.add(
                FinancialCharge(
                    name = "Sanctioned Loan Principal",
                    chargeType = ChargeType.UPFRONT,
                    fixedAmount = principalMatch.value,
                    formulaDescription = "Principal loan amount sanctioned by lender",
                    calculationBase = "Loan Principal",
                    calculatedAmount = principalMatch.value,
                    totalWithGst = principalMatch.value,
                    pageNumber = principalMatch.pageNumber,
                    clauseReference = principalMatch.clause,
                    evidence = principalMatch.snippet,
                    kind = principalMatch.kind,
                ),
            )
        } else {
            terms.add(Finding("Sanctioned amount", null, null, EvidenceKind.NOT_FOUND))
            missing.add("Sanctioned loan amount could not be detected with confidence.")
        }

        if (rateMatch != null) {
            val suffix = if (extracted.isRateMonthly) "% p.a. (converted from monthly rate)" else "% per annum"
            terms.add(
                Finding(
                    label = "Interest rate",
                    value = "${rateMatch.value}$suffix",
                    evidence = rateMatch.snippet,
                    kind = rateMatch.kind,
                    pageNumber = rateMatch.pageNumber,
                    clauseReference = rateMatch.clause,
                ),
            )
        } else {
            terms.add(Finding("Interest rate", null, null, EvidenceKind.NOT_FOUND))
            missing.add("Annual interest rate (ROI) was not explicitly identified in document.")
        }

        if (tenureMonths != null) {
            val yearsStr = if (tenureMonths >= 12 && tenureMonths % 12 == 0) " (${tenureMonths / 12} years)" else ""
            terms.add(
                Finding(
                    label = "Tenure",
                    value = "$tenureMonths months$yearsStr",
                    evidence = tenureMatch?.snippet,
                    kind = tenureMatch?.kind ?: EvidenceKind.EXPLICIT,
                    pageNumber = tenureMatch?.pageNumber,
                    clauseReference = tenureMatch?.clause,
                ),
            )
        } else {
            terms.add(Finding("Tenure", null, null, EvidenceKind.NOT_FOUND))
            missing.add("Loan tenure or repayment period was not found.")
        }

        if (statedEmiMatch != null) {
            terms.add(
                Finding(
                    label = "Stated EMI",
                    value = Money.inr(statedEmiMatch.value),
                    evidence = statedEmiMatch.snippet,
                    kind = statedEmiMatch.kind,
                    pageNumber = statedEmiMatch.pageNumber,
                    clauseReference = statedEmiMatch.clause,
                ),
            )
        }

        // 3. Processing Fee and Upfront Charges
        var procFeeAmount: Double? = null
        var procFeeGst: Double? = null
        if (procFeeMatch != null) {
            procFeeAmount = procFeeMatch.feeAmount
            procFeeGst = procFeeMatch.gstAmount

            fees.add(
                Finding(
                    label = "Processing fee",
                    value = procFeeMatch.summaryText,
                    evidence = procFeeMatch.snippet,
                    kind = procFeeMatch.kind,
                    pageNumber = procFeeMatch.pageNumber,
                    clauseReference = procFeeMatch.clause,
                ),
            )

            itemizedCharges.add(
                FinancialCharge(
                    name = "Processing Fee",
                    chargeType = if (procFeeMatch.isUpfront) ChargeType.UPFRONT else ChargeType.RECURRING,
                    percentage = procFeeMatch.percentage,
                    fixedAmount = procFeeMatch.fixedAmount,
                    formulaDescription = procFeeMatch.formulaDescription,
                    calculationBase = if (procFeeMatch.percentage != null) "Sanctioned Principal" else "Fixed Charge",
                    gstRatePercent = procFeeMatch.gstRatePercent,
                    isGstIncluded = procFeeMatch.isGstIncluded,
                    calculatedAmount = procFeeMatch.feeAmount,
                    gstAmount = procFeeMatch.gstAmount,
                    totalWithGst = procFeeMatch.feeAmount,
                    isUpfrontDeduction = procFeeMatch.isUpfront,
                    assumptions = procFeeMatch.assumptions,
                    evidence = procFeeMatch.snippet,
                    pageNumber = procFeeMatch.pageNumber,
                    clauseReference = procFeeMatch.clause,
                    kind = procFeeMatch.kind,
                ),
            )

            if (procFeeMatch.gstAmount != null && procFeeMatch.gstAmount > 0) {
                itemizedCharges.add(
                    FinancialCharge(
                        name = "GST on Processing Fee",
                        chargeType = if (procFeeMatch.isUpfront) ChargeType.UPFRONT else ChargeType.RECURRING,
                        fixedAmount = procFeeMatch.gstAmount,
                        formulaDescription = "${procFeeMatch.gstRatePercent}% GST on Processing Fee (${Money.inr(procFeeMatch.feeAmount)})",
                        calculationBase = "Processing Fee",
                        calculatedAmount = procFeeMatch.gstAmount,
                        totalWithGst = procFeeMatch.gstAmount,
                        isUpfrontDeduction = procFeeMatch.isUpfront,
                        assumptions = if (procFeeMatch.isGstAssumed) "Assumed standard 18% statutory GST" else "Explicitly stated in agreement",
                        evidence = procFeeMatch.snippet,
                        pageNumber = procFeeMatch.pageNumber,
                        clauseReference = procFeeMatch.clause,
                        kind = if (procFeeMatch.isGstAssumed) EvidenceKind.ASSUMED else EvidenceKind.EXPLICIT,
                    ),
                )
            }
        } else {
            fees.add(Finding("Processing fee", "Not disclosed in scanned pages", null, EvidenceKind.NOT_FOUND))
            missing.add("Processing fee details were not located in the document.")
        }

        var stampDutyAmount: Double? = null
        if (stampDutyMatch != null) {
            stampDutyAmount = stampDutyMatch.value
            itemizedCharges.add(
                FinancialCharge(
                    name = "Stamp Duty",
                    chargeType = ChargeType.UPFRONT,
                    fixedAmount = stampDutyMatch.value,
                    formulaDescription = "Statutory state stamp duty on loan agreement",
                    calculationBase = "State Stamp Duty Act",
                    calculatedAmount = stampDutyMatch.value,
                    totalWithGst = stampDutyMatch.value,
                    isUpfrontDeduction = true,
                    pageNumber = stampDutyMatch.pageNumber,
                    clauseReference = stampDutyMatch.clause,
                    evidence = stampDutyMatch.snippet,
                    kind = stampDutyMatch.kind,
                ),
            )
        }

        var docChargesAmount: Double? = null
        if (docChargesMatch != null) {
            docChargesAmount = docChargesMatch.value
            itemizedCharges.add(
                FinancialCharge(
                    name = "Documentation / Administrative Charges",
                    chargeType = ChargeType.UPFRONT,
                    fixedAmount = docChargesMatch.value,
                    formulaDescription = "One-time loan documentation & verification charges",
                    calculatedAmount = docChargesMatch.value,
                    totalWithGst = docChargesMatch.value,
                    isUpfrontDeduction = true,
                    pageNumber = docChargesMatch.pageNumber,
                    clauseReference = docChargesMatch.clause,
                    evidence = docChargesMatch.snippet,
                    kind = docChargesMatch.kind,
                ),
            )
        }

        var insuranceAmount: Double? = null
        if (insuranceMatch != null) {
            insuranceAmount = insuranceMatch.value
            itemizedCharges.add(
                FinancialCharge(
                    name = "Loan Protection Insurance / Add-on",
                    chargeType = ChargeType.UPFRONT,
                    fixedAmount = insuranceMatch.value,
                    formulaDescription = "Credit shield or loan protection insurance premium",
                    calculatedAmount = insuranceMatch.value,
                    totalWithGst = insuranceMatch.value,
                    isUpfrontDeduction = true,
                    pageNumber = insuranceMatch.pageNumber,
                    clauseReference = insuranceMatch.clause,
                    evidence = insuranceMatch.snippet,
                    kind = insuranceMatch.kind,
                ),
            )
        }

        if (prepaymentMatch != null) {
            fees.add(
                Finding(
                    label = "Prepayment / foreclosure",
                    value = prepaymentMatch.displayText,
                    evidence = prepaymentMatch.snippet,
                    kind = prepaymentMatch.kind,
                    pageNumber = prepaymentMatch.pageNumber,
                    clauseReference = prepaymentMatch.clause,
                ),
            )
            itemizedCharges.add(
                FinancialCharge(
                    name = "Foreclosure / Prepayment Charge",
                    chargeType = ChargeType.CONDITIONAL,
                    percentage = prepaymentMatch.percentage,
                    formulaDescription = prepaymentMatch.formulaDescription,
                    calculationBase = "Outstanding Principal at Prepayment Date",
                    gstRatePercent = if (prepaymentMatch.hasGstMention) 18.0 else null,
                    assumptions = prepaymentMatch.assumptions,
                    pageNumber = prepaymentMatch.pageNumber,
                    clauseReference = prepaymentMatch.clause,
                    evidence = prepaymentMatch.snippet,
                    kind = prepaymentMatch.kind,
                ),
            )
        } else {
            fees.add(Finding("Prepayment / foreclosure", "Terms not identified", null, EvidenceKind.NOT_FOUND))
            missing.add("Foreclosure and partial prepayment terms were not identified in the scanned pages.")
        }

        if (lateChargeMatch != null) {
            fees.add(
                Finding(
                    label = "Late-payment charge",
                    value = lateChargeMatch.displayText,
                    evidence = lateChargeMatch.snippet,
                    kind = lateChargeMatch.kind,
                    pageNumber = lateChargeMatch.pageNumber,
                    clauseReference = lateChargeMatch.clause,
                ),
            )
            itemizedCharges.add(
                FinancialCharge(
                    name = "Late Payment Penal Interest",
                    chargeType = ChargeType.CONDITIONAL,
                    percentage = lateChargeMatch.percentage,
                    fixedAmount = lateChargeMatch.fixedFee,
                    formulaDescription = lateChargeMatch.formulaDescription,
                    calculationBase = "Overdue Installment Amount",
                    assumptions = "Charge applies on overdue amounts for the duration of delay.",
                    pageNumber = lateChargeMatch.pageNumber,
                    clauseReference = lateChargeMatch.clause,
                    evidence = lateChargeMatch.snippet,
                    kind = lateChargeMatch.kind,
                ),
            )
        } else {
            fees.add(Finding("Late-payment charge", "Default penalty not stated", null, EvidenceKind.NOT_FOUND))
            missing.add("Overdue interest / late payment penalty rates were not specified.")
        }

        // 4. Calculations & Amortisation
        var calculatedEmi: Double? = null
        var calculatedTotalRepayment: Double? = null
        var calculatedTotalInterest: Double? = null
        var emiDifference: Double? = null
        var totalDifference: Double? = null

        if (principal != null && annualRate != null && tenureMonths != null) {
            val calcMethod = interestMethod ?: InterestMethod.REDUCING_BALANCE
            val calc = LoanCalculator.calculate(LoanInput(principal, annualRate, tenureMonths, calcMethod))
            calculatedEmi = calc.installment
            calculatedTotalRepayment = calc.totalRepayment
            calculatedTotalInterest = calc.totalInterest

            itemizedCharges.add(
                FinancialCharge(
                    name = "Total Scheduled Interest",
                    chargeType = ChargeType.RECURRING,
                    formulaDescription = "Interest calculated over $tenureMonths months at $annualRate% p.a. (${calcMethod.label})",
                    calculationBase = if (calcMethod == InterestMethod.REDUCING_BALANCE) "Diminishing Balance" else "Full Principal",
                    calculatedAmount = calc.totalInterest,
                    totalWithGst = calc.totalInterest,
                    kind = EvidenceKind.CALCULATED,
                ),
            )

            if (statedEmi != null) {
                val diff = statedEmi - calc.installment
                emiDifference = diff
                if (abs(diff) >= 50.0) {
                    val totalEmiImpact = diff * tenureMonths
                    val direction = if (diff > 0) "higher" else "lower"
                    review.add(
                        "Stated EMI (${Money.inr(statedEmi)}) differs from standard amortisation (${Money.inr(calc.installment)}) by ${Money.inr(abs(diff))}/month ($direction).",
                    )
                    riskFindings.add(
                        RiskFinding(
                            title = "EMI Discrepancy (${Money.inr(abs(diff))}/mo $direction)",
                            explanation = "The agreement states an EMI of ${Money.inr(statedEmi)}, while standard ${calcMethod.label.lowercase()} amortisation yields ${Money.inr(calc.installment)}. Over $tenureMonths months, this represents a cumulative variance of ${Money.inr(abs(totalEmiImpact))}. Potential causes include fee capitalisation, broken-period interest, or flat-rate calculation.",
                            severity = if (abs(diff) >= 200.0) Severity.HIGH else Severity.MEDIUM,
                            financialImpact = abs(totalEmiImpact),
                            financialImpactDescription = "${Money.inr(abs(totalEmiImpact))} total variance over tenure",
                            evidence = statedEmiMatch?.snippet,
                            pageNumber = statedEmiMatch?.pageNumber,
                            clauseReference = statedEmiMatch?.clause,
                            whyItMatters = "An unexplained higher EMI means you pay significantly more in total interest than the quoted annual percentage rate suggests.",
                            questionToAsk = "Why does the stated EMI of ${Money.inr(statedEmi)} exceed the standard reducing-balance amortisation of ${Money.inr(calc.installment)}? Are insurance premiums or broken-period interest added?",
                        ),
                    )
                }
            }

            if (statedTotalRepayment != null) {
                totalDifference = statedTotalRepayment - calc.totalRepayment
            }
        }

        // Upfront deductions & Net disbursal calculation
        val upfrontChargesList = itemizedCharges.filter { it.isUpfrontDeduction }
        val totalUpfrontDeductions = if (upfrontChargesList.isNotEmpty()) {
            upfrontChargesList.sumOf { it.totalWithGst ?: it.calculatedAmount ?: 0.0 }
        } else null

        val estimatedNetDisbursal = if (principal != null) {
            LoanCalculator.calculateNetDisbursal(principal, totalUpfrontDeductions ?: 0.0)
        } else null

        val totalBorrowerCost = if (calculatedTotalRepayment != null) {
            calculatedTotalRepayment + (totalUpfrontDeductions ?: 0.0)
        } else null

        // 5. Evidence-Backed Risk Findings (Phase 6)
        // High Upfront Deductions check
        if (totalUpfrontDeductions != null && principal != null && principal > 0) {
            val deductionPct = (totalUpfrontDeductions / principal) * 100.0
            if (deductionPct >= 3.0) {
                riskFindings.add(
                    RiskFinding(
                        title = "Significant Upfront Deductions (${deductionPct.roundToInt()}% of Principal)",
                        explanation = "A total of ${Money.inr(totalUpfrontDeductions)} (${deductionPct.roundToInt()}% of your sanctioned loan) will be deducted upfront from your disbursal. Your estimated take-home amount is only ${Money.inr(estimatedNetDisbursal ?: (principal - totalUpfrontDeductions))}.",
                        severity = if (deductionPct >= 5.0) Severity.HIGH else Severity.MEDIUM,
                        financialImpact = totalUpfrontDeductions,
                        financialImpactDescription = "${Money.inr(totalUpfrontDeductions)} deducted before payout",
                        evidence = procFeeMatch?.snippet ?: "Itemized upfront charges",
                        pageNumber = procFeeMatch?.pageNumber,
                        whyItMatters = "Upfront deductions reduce the actual cash you receive, while you still pay interest on the full sanctioned principal amount.",
                        questionToAsk = "Can the processing fee and insurance premiums be reduced or paid separately rather than being deducted directly from my disbursal?",
                    ),
                )
            }
        }

        // High Prepayment Penalty Risk Check
        if (prepaymentMatch?.percentage != null && prepaymentMatch.percentage >= 3.0 && principal != null) {
            val estimatedBalanceAt12 = LoanCalculator.outstandingBalanceAfter(
                principal = principal,
                annualRatePercent = annualRate ?: 12.0,
                tenureMonths = tenureMonths ?: 36,
                monthNumber = 12,
            )
            val penaltyEst = LoanCalculator.calculateForeclosure(estimatedBalanceAt12, prepaymentMatch.percentage, 18.0)
            riskFindings.add(
                RiskFinding(
                    title = "High Foreclosure Charge (${prepaymentMatch.percentage}% + GST)",
                    explanation = "Foreclosing or prepaying the loan carries a ${prepaymentMatch.percentage}% penalty on outstanding principal plus GST. For example, prepaying at month 12 on an estimated remaining balance of ${Money.inr(estimatedBalanceAt12)} costs ~${Money.inr(penaltyEst.totalCost)} in penalty fees.",
                    severity = Severity.MEDIUM,
                    financialImpact = penaltyEst.totalCost,
                    financialImpactDescription = "~${Money.inr(penaltyEst.totalCost)} penalty at month 12",
                    evidence = prepaymentMatch.snippet,
                    pageNumber = prepaymentMatch.pageNumber,
                    clauseReference = prepaymentMatch.clause,
                    whyItMatters = "Foreclosure penalties penalize you for repaying your debt early and hinder refinancing to cheaper lenders.",
                    questionToAsk = "Is the prepayment penalty waived if the loan is foreclosed using the borrower's own verified funds in accordance with RBI guidelines?",
                ),
            )
        }

        // Discretionary Revision Clauses
        if (revisionClauses.isNotEmpty()) {
            review.add("Lender interest/charge variation clause detected: ${revisionClauses.first().snippet}")
            riskFindings.add(
                RiskFinding(
                    title = "Lender Discretionary Rate/Fee Variation Clause",
                    explanation = "The agreement contains terms permitting the lender to revise interest rate spreads or charges unilaterally upon notice without requiring explicit borrower re-consent.",
                    severity = Severity.MEDIUM,
                    evidence = revisionClauses.first().snippet,
                    pageNumber = revisionClauses.first().pageNumber,
                    clauseReference = "Variation Clause",
                    whyItMatters = "Unilateral reset clauses allow the lender to raise your interest spread or fees even if benchmark market rates have not increased.",
                    questionToAsk = "What transparent external benchmark governs interest rate adjustments, and how much advance notice is provided before rate revisions take effect?",
                ),
            )
        }

        // Missing APR Check
        if (extracted.aprPercent == null && annualRate != null) {
            riskFindings.add(
                RiskFinding(
                    title = "Missing Annual Percentage Rate (APR) Disclosure",
                    explanation = "The document states an interest rate of $annualRate% but does not explicitly disclose the Annual Percentage Rate (APR) including all upfront and recurring fees as required by RBI Key Fact Statement (KFS) norms.",
                    severity = Severity.LOW,
                    evidence = rateMatch?.snippet,
                    pageNumber = rateMatch?.pageNumber,
                    whyItMatters = "Without APR, the true annualized cost of your loan after factoring in processing fees, documentation charges, and mandatory insurance remains concealed.",
                    questionToAsk = "Can the lender provide the official RBI-mandated Key Fact Statement (KFS) detailing the comprehensive Annual Percentage Rate (APR)?",
                ),
            )
        }

        // Method ambiguity check
        if (interestMethod == null && annualRate != null) {
            review.add("Interest calculation method (reducing balance vs flat rate) was not explicitly stated in the document.")
            riskFindings.add(
                RiskFinding(
                    title = "Interest Method Ambiguity",
                    explanation = "The document does not explicitly state whether the $annualRate% interest is charged on a reducing-balance or flat-rate basis. Flat-rate loans can result in nearly double the effective borrowing cost.",
                    severity = Severity.HIGH,
                    evidence = rateMatch?.snippet,
                    pageNumber = rateMatch?.pageNumber,
                    whyItMatters = "A flat rate of $annualRate% on a 3-year loan is equivalent to approximately ${((annualRate * 1.85) * 10).roundToInt() / 10.0}% reducing-balance rate.",
                    questionToAsk = "Please confirm in writing whether interest is computed strictly on a diminishing (reducing) monthly balance basis.",
                ),
            )
        }

        // OCR warnings
        extraction.warnings.forEach { warning -> review.add(warning) }
        if (extraction.fullText.length < 150) {
            review.add("The scanned document contained very little text. Ensure the physical agreement or PDF is fully readable and clear.")
        }

        // Contradictory documentation fee check (e.g. Clause 4.3 vs Schedule B)
        val docMatches = pages.flatMap { page ->
            val pattern = Regex(
                """(?:documentation|admin(?:istrative)?\s+charges?|verification\s+charges?)\D{0,35}(?:rs\.?|inr|₹)\s*([0-9,]+(?:\.[0-9]{2})?)""",
                RegexOption.IGNORE_CASE,
            )
            pattern.findAll(page.text).mapNotNull { m ->
                val v = StructuredLoanExtractor.parseIndianNumber(m.groupValues[1])
                if (v != null && v in 50.0..50000.0) Triple(v, m.value.trim(), page.pageNumber) else null
            }.toList()
        }
        val distinctDocAmounts = docMatches.map { it.first }.distinct()
        if (distinctDocAmounts.size > 1) {
            val first = docMatches.first()
            val second = docMatches.first { it.first != first.first }
            val diff = abs(first.first - second.first)
            riskFindings.add(
                RiskFinding(
                    title = "Contradictory Documentation Fee",
                    explanation = "The agreement contains conflicting documentation fee amounts: ${Money.inr(first.first)} (Page ${first.third}) vs ${Money.inr(second.first)} (Page ${second.third}). This inconsistency is flagged and not silently reconciled. Obtain written clarification from the lender.",
                    severity = Severity.HIGH,
                    financialImpact = diff,
                    financialImpactDescription = "${Money.inr(diff)} discrepancy between conflicting clauses",
                    evidence = "${first.second} vs ${second.second}",
                    pageNumber = first.third,
                    clauseReference = "Documentation Fee Conflict",
                    whyItMatters = "Conflicting fee clauses may lead to higher upfront deductions at disbursal.",
                    questionToAsk = "Which documentation fee amount is legally binding: ${Money.inr(first.first)} or ${Money.inr(second.first)}?",
                ),
            )
        }

        // Stated vs Calculated Net Disbursal check
        val statedDisbursed = extracted.actualDisbursedAmount?.value ?: geminiOutput?.actualDisbursedAmount?.value
        if (statedDisbursed != null && estimatedNetDisbursal != null) {
            val disbursalDiff = abs(statedDisbursed - estimatedNetDisbursal)
            if (disbursalDiff >= 200.0) {
                riskFindings.add(
                    RiskFinding(
                        title = "Stated Net Disbursal Mismatch (${Money.inr(disbursalDiff)})",
                        explanation = "The agreement states a net disbursal of ${Money.inr(statedDisbursed)}, but subtracting identified upfront deductions (${Money.inr(totalUpfrontDeductions ?: 0.0)}) from the sanctioned principal yields ${Money.inr(estimatedNetDisbursal)}. There may be undisclosed deductions or fee capitalisation.",
                        severity = Severity.HIGH,
                        financialImpact = disbursalDiff,
                        financialImpactDescription = "${Money.inr(disbursalDiff)} unexplained difference",
                        evidence = extracted.actualDisbursedAmount?.snippet ?: "Stated Disbursal vs Deductions",
                        pageNumber = extracted.actualDisbursedAmount?.pageNumber,
                        whyItMatters = "A lower net disbursal means you receive less money than expected while still paying interest on the full sanctioned principal.",
                        questionToAsk = "Can you provide an itemized disbursal statement accounting for the ${Money.inr(disbursalDiff)} variance between stated disbursal and deductions?",
                    ),
                )
            }
        }

        val keyObligations = buildKeyObligations(pages, prepaymentMatch, lateChargeMatch)
        val questionsForLender = generateQuestionsForLender(
            emiDifference = emiDifference,
            statedEmi = statedEmi,
            calculatedEmi = calculatedEmi,
            interestMethod = interestMethod,
            prepaymentMatch = prepaymentMatch,
            procFeeMatch = procFeeMatch,
            insuranceMatch = insuranceMatch,
            revisionClauses = revisionClauses,
        )

        val completeness = CompletenessChecker.audit(pages, extracted, geminiOutput)
        val sourceSummary = when {
            geminiOutput != null -> "Gemini AI + ${extraction.primarySource.label}"
            extraction.primarySource == com.finshield.app.model.TextSourceKind.NATIVE_PDF -> "Native PDF Text Extraction"
            extraction.primarySource == com.finshield.app.model.TextSourceKind.HYBRID -> "Hybrid (Native Text + OCR)"
            else -> "ML Kit OCR v2 (On-device)"
        }

        val snapshot = LoanSnapshot(
            loanType = extracted.loanType.value,
            lenderName = extracted.lenderName?.value ?: geminiOutput?.lenderName?.value,
            borrowerName = extracted.borrowerName?.value ?: geminiOutput?.borrowerName?.value,
            agreementDate = extracted.agreementDate?.value ?: geminiOutput?.agreementDate?.value,
            referenceNumber = extracted.referenceNumber?.value ?: geminiOutput?.referenceNumber?.value,
            aprPercent = extracted.aprPercent?.value ?: geminiOutput?.aprPercent?.value,
            isFloatingRate = extracted.isFloatingRate ?: geminiOutput?.isFloatingRate?.value,
            sanctionedPrincipal = principal ?: geminiOutput?.sanctionedPrincipal?.value,
            actualDisbursedAmount = extracted.actualDisbursedAmount?.value ?: geminiOutput?.actualDisbursedAmount?.value,
            annualRatePercent = annualRate ?: geminiOutput?.annualRatePercent?.value,
            interestMethod = interestMethod,
            tenureMonths = tenureMonths ?: geminiOutput?.tenureMonths?.value,
            tenureYears = (tenureMonths ?: geminiOutput?.tenureMonths?.value)?.let { it.toDouble() / 12.0 },
            statedEmi = statedEmi ?: geminiOutput?.statedEmi?.value,
            calculatedEmi = calculatedEmi,
            emiDifference = emiDifference,
            statedTotalRepayment = statedTotalRepayment ?: geminiOutput?.totalRepayment?.value,
            calculatedTotalRepayment = calculatedTotalRepayment,
            totalRepaymentDifference = totalDifference,
        )

        val breakdown = FinancialBreakdown(
            sanctionedPrincipal = principal ?: geminiOutput?.sanctionedPrincipal?.value,
            processingFeeAmount = procFeeAmount ?: geminiOutput?.processingFeeAmount?.value,
            processingFeeGst = procFeeGst ?: geminiOutput?.gstOnProcessingFee?.value,
            stampDuty = stampDutyAmount ?: geminiOutput?.stampDuty?.value,
            documentationCharges = docChargesAmount ?: geminiOutput?.documentationCharges?.value,
            insuranceCharges = insuranceAmount ?: geminiOutput?.insuranceCharges?.value,
            otherUpfrontDeductions = 0.0,
            totalUpfrontDeductions = totalUpfrontDeductions ?: geminiOutput?.totalUpfrontDeductions?.value,
            estimatedNetDisbursal = estimatedNetDisbursal,
            totalInterest = calculatedTotalInterest ?: geminiOutput?.totalInterest?.value,
            totalScheduledEmis = calculatedTotalRepayment ?: geminiOutput?.totalRepayment?.value,
            totalRepaymentScheduled = calculatedTotalRepayment ?: geminiOutput?.totalRepayment?.value,
            otherMandatoryCharges = 0.0,
            totalEstimatedBorrowerCost = totalBorrowerCost,
            itemizedCharges = itemizedCharges,
        )

        return AnalysisReport(
            id = reportId,
            documentName = documentName,
            isSample = false,
            analyzedAt = System.currentTimeMillis(),
            pagesCount = extraction.totalPages.coerceAtLeast(pages.size),
            isSessionOnly = true,
            loanSnapshot = snapshot,
            financialBreakdown = breakdown,
            riskFindings = riskFindings,
            feesAndCharges = itemizedCharges,
            keyObligations = keyObligations,
            questionsForLender = questionsForLender,
            rawPages = pages,
            isUserReviewed = false,
            completenessReport = completeness,
            unrecognizedTerms = geminiOutput?.otherDetectedTerms ?: emptyList(),
            extractionSourceSummary = sourceSummary,
            terms = terms,
            feesAndPenalties = fees,
            missingInformation = missing,
            manualReview = review,
        )
    }

    suspend fun analyzeWithAi(
        documentName: String,
        extraction: DocumentExtractionResult,
        reportId: String = UUID.randomUUID().toString(),
        geminiAssistant: GeminiLoanAssistant? = null,
    ): AnalysisReport {
        val geminiOutput = if (geminiAssistant != null && extraction.pages.isNotEmpty()) {
            when (val res = geminiAssistant.extractStructuredTerms(extraction.pages)) {
                is GeminiExtractionResult.Success -> res.output
                is GeminiExtractionResult.Failure -> null
            }
        } else null

        return analyze(documentName, extraction, reportId, geminiOutput)
    }

    /**
     * Recalculates an existing report with user-reviewed or corrected loan values.
     * Preserves original OCR evidence snippets while updating amortisation, net disbursal, and risks.
     */
    fun recalculateWithCustomTerms(
        originalReport: AnalysisReport,
        principal: Double? = originalReport.loanSnapshot.sanctionedPrincipal,
        annualRatePercent: Double? = originalReport.loanSnapshot.annualRatePercent,
        tenureMonths: Int? = originalReport.loanSnapshot.tenureMonths,
        statedEmi: Double? = originalReport.loanSnapshot.statedEmi,
        interestMethod: InterestMethod? = originalReport.loanSnapshot.interestMethod,
        processingFeeAmount: Double? = originalReport.financialBreakdown.processingFeeAmount,
        lenderName: String? = originalReport.loanSnapshot.lenderName,
        borrowerName: String? = originalReport.loanSnapshot.borrowerName,
    ): AnalysisReport {
        val calcMethod = interestMethod ?: InterestMethod.REDUCING_BALANCE
        var calculatedEmi: Double? = null
        var calculatedTotalRepayment: Double? = null
        var calculatedTotalInterest: Double? = null
        var emiDifference: Double? = null
        var totalDifference: Double? = null

        val updatedCharges = originalReport.feesAndCharges.toMutableList()

        if (principal != null && annualRatePercent != null && tenureMonths != null) {
            val calc = LoanCalculator.calculate(LoanInput(principal, annualRatePercent, tenureMonths, calcMethod))
            calculatedEmi = calc.installment
            calculatedTotalRepayment = calc.totalRepayment
            calculatedTotalInterest = calc.totalInterest

            if (statedEmi != null) {
                emiDifference = statedEmi - calc.installment
            }
            if (statedEmi != null) {
                totalDifference = (statedEmi * tenureMonths) - calc.totalRepayment
            }
        }

        // Upfront deductions
        val procFeeGst = if (processingFeeAmount != null && processingFeeAmount > 0) {
            (processingFeeAmount * 0.18 * 100.0).roundToInt() / 100.0
        } else null

        val otherDeductions = (originalReport.financialBreakdown.stampDuty ?: 0.0) +
            (originalReport.financialBreakdown.documentationCharges ?: 0.0) +
            (originalReport.financialBreakdown.insuranceCharges ?: 0.0)

        val totalUpfront = if (processingFeeAmount != null || otherDeductions > 0) {
            (processingFeeAmount ?: 0.0) + (procFeeGst ?: 0.0) + otherDeductions
        } else null

        val estimatedNetDisbursal = if (principal != null) {
            LoanCalculator.calculateNetDisbursal(principal, totalUpfront ?: 0.0)
        } else null

        val totalBorrowerCost = if (calculatedTotalRepayment != null) {
            calculatedTotalRepayment + (totalUpfront ?: 0.0)
        } else null

        val updatedSnapshot = originalReport.loanSnapshot.copy(
            lenderName = lenderName ?: originalReport.loanSnapshot.lenderName,
            borrowerName = borrowerName ?: originalReport.loanSnapshot.borrowerName,
            sanctionedPrincipal = principal,
            annualRatePercent = annualRatePercent,
            interestMethod = calcMethod,
            tenureMonths = tenureMonths,
            tenureYears = tenureMonths?.let { it.toDouble() / 12.0 },
            statedEmi = statedEmi,
            calculatedEmi = calculatedEmi,
            emiDifference = emiDifference,
            calculatedTotalRepayment = calculatedTotalRepayment,
            totalRepaymentDifference = totalDifference,
        )

        val updatedBreakdown = originalReport.financialBreakdown.copy(
            sanctionedPrincipal = principal,
            processingFeeAmount = processingFeeAmount,
            processingFeeGst = procFeeGst,
            totalUpfrontDeductions = totalUpfront,
            estimatedNetDisbursal = estimatedNetDisbursal,
            totalInterest = calculatedTotalInterest,
            totalScheduledEmis = calculatedTotalRepayment,
            totalRepaymentScheduled = calculatedTotalRepayment,
            totalEstimatedBorrowerCost = totalBorrowerCost,
            itemizedCharges = updatedCharges,
        )

        val updatedTerms = originalReport.terms.toMutableList()
        if (principal != null && principal != originalReport.loanSnapshot.sanctionedPrincipal) {
            val idx = updatedTerms.indexOfFirst { it.label == "Sanctioned amount" }
            val item = Finding("Sanctioned amount", Money.inr(principal), "User verified in OCR review", EvidenceKind.USER_CORRECTED)
            if (idx >= 0) updatedTerms[idx] = item else updatedTerms.add(item)
        }
        if (annualRatePercent != null && annualRatePercent != originalReport.loanSnapshot.annualRatePercent) {
            val idx = updatedTerms.indexOfFirst { it.label == "Annual interest rate" }
            val item = Finding("Annual interest rate", "$annualRatePercent%", "User verified in OCR review", EvidenceKind.USER_CORRECTED)
            if (idx >= 0) updatedTerms[idx] = item else updatedTerms.add(item)
        }
        if (tenureMonths != null && tenureMonths != originalReport.loanSnapshot.tenureMonths) {
            val idx = updatedTerms.indexOfFirst { it.label == "Tenure" }
            val item = Finding("Tenure", "$tenureMonths months", "User verified in OCR review", EvidenceKind.USER_CORRECTED)
            if (idx >= 0) updatedTerms[idx] = item else updatedTerms.add(item)
        }
        if (statedEmi != null && statedEmi != originalReport.loanSnapshot.statedEmi) {
            val idx = updatedTerms.indexOfFirst { it.label == "Stated EMI" }
            val item = Finding("Stated EMI", Money.inr(statedEmi), "User verified in OCR review", EvidenceKind.USER_CORRECTED)
            if (idx >= 0) updatedTerms[idx] = item else updatedTerms.add(item)
        }

        return originalReport.copy(
            loanSnapshot = updatedSnapshot,
            financialBreakdown = updatedBreakdown,
            feesAndCharges = updatedCharges,
            terms = updatedTerms,
            isUserReviewed = true,
        )
    }

    private fun buildKeyObligations(
        pages: List<PageExtraction>,
        prepaymentMatch: ExtractedPrepaymentDetails?,
        lateChargeMatch: ExtractedLatePaymentDetails?,
    ): List<KeyObligation> {
        val obligations = mutableListOf<KeyObligation>()

        // 1. Timely Repayment
        obligations.add(
            KeyObligation(
                category = "Monthly Repayment",
                summary = "Repayment via NACH / ECS / Auto-debit",
                status = ObligationStatus.EXTRACTED,
                details = "Equated monthly instalments must be paid on or before the designated monthly due date.",
                pageNumber = 1,
            ),
        )

        // 2. Prepayment
        if (prepaymentMatch != null) {
            obligations.add(
                KeyObligation(
                    category = "Prepayment & Foreclosure",
                    summary = prepaymentMatch.displayText,
                    status = ObligationStatus.EXTRACTED,
                    details = "Foreclosure permitted subject to ${prepaymentMatch.displayText} charges.",
                    evidence = prepaymentMatch.snippet,
                    pageNumber = prepaymentMatch.pageNumber,
                    clauseReference = prepaymentMatch.clause,
                ),
            )
        } else {
            obligations.add(
                KeyObligation(
                    category = "Prepayment & Foreclosure",
                    summary = "Foreclosure terms not found",
                    status = ObligationStatus.NOT_FOUND,
                    details = "Check with lender regarding lock-in period and partial prepayment rules.",
                ),
            )
        }

        // 3. Late Payment
        if (lateChargeMatch != null) {
            obligations.add(
                KeyObligation(
                    category = "Default & Penal Charges",
                    summary = lateChargeMatch.displayText,
                    status = ObligationStatus.EXTRACTED,
                    details = "Delayed instalments accrue penal interest at ${lateChargeMatch.displayText}.",
                    evidence = lateChargeMatch.snippet,
                    pageNumber = lateChargeMatch.pageNumber,
                    clauseReference = lateChargeMatch.clause,
                ),
            )
        }

        return obligations
    }

    private fun generateQuestionsForLender(
        emiDifference: Double?,
        statedEmi: Double?,
        calculatedEmi: Double?,
        interestMethod: InterestMethod?,
        prepaymentMatch: ExtractedPrepaymentDetails?,
        procFeeMatch: ExtractedProcessingFee?,
        insuranceMatch: ExtractedMatch<Double>?,
        revisionClauses: List<ExtractedMatch<String>>,
    ): List<String> {
        val questions = mutableListOf<String>()

        if (emiDifference != null && abs(emiDifference) >= 50.0 && statedEmi != null && calculatedEmi != null) {
            questions.add(
                "What explains the difference between the stated EMI (${Money.inr(statedEmi)}) and standard amortisation (${Money.inr(calculatedEmi)})? Does the stated EMI bundle insurance or advance interest?",
            )
        }

        if (interestMethod == null) {
            questions.add(
                "Is the interest calculated on a monthly reducing-balance basis or a flat-rate basis?",
            )
        }

        if (prepaymentMatch != null && prepaymentMatch.percentage != null) {
            questions.add(
                "Under what conditions does the ${prepaymentMatch.percentage}% foreclosure penalty apply? Is it waived if the loan is prepaid from the borrower's own verifiable funds as per RBI guidelines?",
            )
        }

        if (insuranceMatch != null) {
            questions.add(
                "Is the loan protection insurance premium of ${Money.inr(insuranceMatch.value)} compulsory for sanction, or is it an optional opt-out add-on?",
            )
        }

        if (revisionClauses.isNotEmpty()) {
            questions.add(
                "What specific external benchmark (e.g. RBI Repo Rate, EBLR, or MCLR) governs interest rate revisions, and how frequently can the rate be reset?",
            )
        }

        questions.add("Can the lender provide the complete amortization schedule showing month-by-month principal and interest allocation?")
        return questions
    }
}
