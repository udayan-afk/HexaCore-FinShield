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

data class ExtractedMatch<T>(
    val value: T,
    val snippet: String,
    val pageNumber: Int,
    val clause: String? = null,
    val kind: EvidenceKind = EvidenceKind.EXPLICIT,
)

object LoanAgreementAnalyzer {

    fun analyze(
        documentName: String,
        extraction: DocumentExtractionResult,
        reportId: String = UUID.randomUUID().toString(),
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

        // 1. Loan Type
        val loanTypeMatch = extractLoanType(pages, text)
        val loanType = loanTypeMatch.value

        // 2. Sanctioned Principal Amount
        val principalMatch = extractPrincipal(pages, text)
        val principal = principalMatch?.value
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
            terms.add(
                Finding(
                    label = "Sanctioned amount",
                    value = null,
                    evidence = null,
                    kind = EvidenceKind.NOT_FOUND,
                ),
            )
            missing.add("Sanctioned loan amount could not be detected with confidence.")
        }

        // 3. Interest Rate
        val rateMatch = extractInterestRate(pages, text)
        val annualRate = rateMatch?.value
        if (rateMatch != null) {
            terms.add(
                Finding(
                    label = "Interest rate",
                    value = "${rateMatch.value}% per annum",
                    evidence = rateMatch.snippet,
                    kind = rateMatch.kind,
                    pageNumber = rateMatch.pageNumber,
                    clauseReference = rateMatch.clause,
                ),
            )
        } else {
            terms.add(
                Finding(
                    label = "Interest rate",
                    value = null,
                    evidence = null,
                    kind = EvidenceKind.NOT_FOUND,
                ),
            )
            missing.add("Annual interest rate (ROI) was not explicitly identified.")
        }

        // 4. Interest Calculation Method
        val methodMatch = extractInterestMethod(pages, text)
        val interestMethod = methodMatch?.value

        // 5. Loan Tenure
        val tenureMatch = extractTenure(pages, text)
        val tenureMonths = tenureMatch?.value
        if (tenureMonths != null) {
            val yearsStr = if (tenureMonths >= 12 && tenureMonths % 12 == 0) " (${tenureMonths / 12} years)" else ""
            terms.add(
                Finding(
                    label = "Tenure",
                    value = "$tenureMonths months$yearsStr",
                    evidence = tenureMatch.snippet,
                    kind = tenureMatch.kind,
                    pageNumber = tenureMatch.pageNumber,
                    clauseReference = tenureMatch.clause,
                ),
            )
        } else {
            terms.add(
                Finding(
                    label = "Tenure",
                    value = null,
                    evidence = null,
                    kind = EvidenceKind.NOT_FOUND,
                ),
            )
            missing.add("Loan tenure or repayment period was not found.")
        }

        // 6. Stated EMI
        val statedEmiMatch = extractStatedEmi(pages, text)
        val statedEmi = statedEmiMatch?.value
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

        // 7. Stated Total Repayment
        val statedTotalMatch = extractStatedTotalRepayment(pages, text)
        val statedTotalRepayment = statedTotalMatch?.value ?: (if (statedEmi != null && tenureMonths != null) statedEmi * tenureMonths else null)

        // 8. Processing Fee & Taxes
        val procFeeMatch = extractProcessingFeeDetails(pages, text, principal)
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
                        assumptions = if (procFeeMatch.isGstAssumed) "Assumed standard 18% statutory GST (configurable)" else "Explicitly stated in agreement",
                        evidence = procFeeMatch.snippet,
                        pageNumber = procFeeMatch.pageNumber,
                        clauseReference = procFeeMatch.clause,
                        kind = if (procFeeMatch.isGstAssumed) EvidenceKind.ASSUMED else EvidenceKind.EXPLICIT,
                    ),
                )
            }
        } else {
            fees.add(
                Finding(
                    label = "Processing fee",
                    value = "Not disclosed in scanned pages",
                    evidence = null,
                    kind = EvidenceKind.NOT_FOUND,
                ),
            )
            missing.add("Processing fee details were not located in the document.")
        }

        // 9. Stamp Duty
        val stampDutyMatch = extractStampDuty(pages, text)
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

        // 10. Documentation / Admin Charges
        val docChargesMatch = extractDocumentationCharges(pages, text)
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

        // 11. Insurance / Add-ons
        val insuranceMatch = extractInsurance(pages, text)
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

        // 12. Prepayment / Foreclosure
        val prepaymentMatch = extractPrepaymentDetails(pages, text)
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
            fees.add(
                Finding(
                    label = "Prepayment / foreclosure",
                    value = "Terms not identified",
                    evidence = null,
                    kind = EvidenceKind.NOT_FOUND,
                ),
            )
            missing.add("Foreclosure and partial prepayment terms were not identified in the scanned pages.")
        }

        // 13. Late Payment / Overdue Charges
        val lateChargeMatch = extractLatePaymentDetails(pages, text)
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
                    assumptions = "Charge applies only on overdue amounts for the duration of delay.",
                    pageNumber = lateChargeMatch.pageNumber,
                    clauseReference = lateChargeMatch.clause,
                    evidence = lateChargeMatch.snippet,
                    kind = lateChargeMatch.kind,
                ),
            )
        } else {
            fees.add(
                Finding(
                    label = "Late-payment charge",
                    value = "Default penalty not stated",
                    evidence = null,
                    kind = EvidenceKind.NOT_FOUND,
                ),
            )
            missing.add("Overdue interest / late payment penalty rates were not specified.")
        }

        // 14. Calculations & Amortisation
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
                            explanation = "The agreement states an EMI of ${Money.inr(statedEmi)}, while standard ${calcMethod.label.lowercase()} amortisation yields ${Money.inr(calc.installment)}. Over $tenureMonths months, this represents a variance of ${Money.inr(abs(totalEmiImpact))}. Potential causes include fee capitalisation, broken-period interest, or flat-rate calculation.",
                            severity = if (abs(diff) >= 200.0) Severity.HIGH else Severity.MEDIUM,
                            financialImpact = abs(totalEmiImpact),
                            financialImpactDescription = "${Money.inr(abs(totalEmiImpact))} total variance over tenure",
                            evidence = statedEmiMatch?.snippet,
                            pageNumber = statedEmiMatch?.pageNumber,
                            clauseReference = statedEmiMatch?.clause,
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

        // High Foreclosure Fee Risk Check
        if (prepaymentMatch != null && prepaymentMatch.percentage != null && prepaymentMatch.percentage >= 3.0 && principal != null) {
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
                    explanation = "Foreclosing the loan carries a ${prepaymentMatch.percentage}% penalty on outstanding principal plus GST. For example, foreclosing at month 12 on an estimated remaining balance of ${Money.inr(estimatedBalanceAt12)} would cost approximately ${Money.inr(penaltyEst.totalCost)} in penalty and taxes.",
                    severity = Severity.MEDIUM,
                    financialImpact = penaltyEst.totalCost,
                    financialImpactDescription = "~${Money.inr(penaltyEst.totalCost)} penalty at month 12",
                    evidence = prepaymentMatch.snippet,
                    pageNumber = prepaymentMatch.pageNumber,
                    clauseReference = prepaymentMatch.clause,
                ),
            )
        }

        // Revision clauses
        val revisionClauses = findRevisionClauses(pages, text)
        if (revisionClauses.isNotEmpty()) {
            review.add("Lender interest/charge variation clause detected (${revisionClauses.joinToString { it.snippet }})")
            riskFindings.add(
                RiskFinding(
                    title = "Lender Discretionary Rate/Fee Variation Clause",
                    explanation = "The agreement contains terms permitting the lender to revise interest rate spreads or charges unilaterally upon notice without requiring borrower re-consent.",
                    severity = Severity.MEDIUM,
                    evidence = revisionClauses.first().snippet,
                    pageNumber = revisionClauses.first().pageNumber,
                    clauseReference = "Revision Clause",
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
                ),
            )
        }

        // OCR warnings
        extraction.warnings.forEach { warning -> review.add(warning) }
        if (extraction.fullText.length < 150) {
            review.add("The scanned document contained very little text. Ensure the physical agreement or PDF is fully readable and clear.")
        }

        // Key Obligations
        val keyObligations = buildKeyObligations(pages, text, prepaymentMatch, lateChargeMatch)

        // Questions to ask lender
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

        val snapshot = LoanSnapshot(
            loanType = loanType,
            sanctionedPrincipal = principal,
            annualRatePercent = annualRate,
            interestMethod = interestMethod,
            tenureMonths = tenureMonths,
            tenureYears = tenureMonths?.let { it.toDouble() / 12.0 },
            statedEmi = statedEmi,
            calculatedEmi = calculatedEmi,
            emiDifference = emiDifference,
            statedTotalRepayment = statedTotalRepayment,
            calculatedTotalRepayment = calculatedTotalRepayment,
            totalRepaymentDifference = totalDifference,
        )

        val breakdown = FinancialBreakdown(
            sanctionedPrincipal = principal,
            processingFeeAmount = procFeeAmount,
            processingFeeGst = procFeeGst,
            stampDuty = stampDutyAmount,
            documentationCharges = docChargesAmount,
            insuranceCharges = insuranceAmount,
            otherUpfrontDeductions = 0.0,
            totalUpfrontDeductions = totalUpfrontDeductions,
            estimatedNetDisbursal = estimatedNetDisbursal,
            totalInterest = calculatedTotalInterest,
            totalScheduledEmis = calculatedTotalRepayment,
            totalRepaymentScheduled = calculatedTotalRepayment,
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
            terms = terms,
            feesAndPenalties = fees,
            missingInformation = missing,
            manualReview = review,
        )
    }

    private fun extractLoanType(pages: List<PageExtraction>, fullText: String): ExtractedMatch<String> {
        val types = listOf(
            "Home Loan" to listOf("home loan", "housing loan", "mortgage loan"),
            "Vehicle Loan" to listOf("vehicle loan", "auto loan", "car loan", "two wheeler loan"),
            "Personal Loan" to listOf("personal loan", "consumer loan", "salary loan"),
            "Education Loan" to listOf("education loan", "student loan"),
            "Business Loan" to listOf("business loan", "commercial loan", "msme loan"),
            "Gold Loan" to listOf("gold loan"),
        )
        for (page in pages) {
            for ((type, keywords) in types) {
                for (kw in keywords) {
                    if (page.text.contains(kw, ignoreCase = true)) {
                        return ExtractedMatch(type, "Matched \"$kw\"", page.pageNumber)
                    }
                }
            }
        }
        return ExtractedMatch("Personal Loan Facility", "Standard Loan Agreement", 1)
    }

    private fun extractPrincipal(pages: List<PageExtraction>, fullText: String): ExtractedMatch<Double>? {
        val pattern = Regex(
            """(?:sanctioned\s+amount|loan\s+amount|principal\s+amount|loan\s+of|amount\s+of\s+loan|facility\s+amount|facility\s+of)\D{1,30}(?:rs\.?|inr|₹)?\s*([0-9]{1,3}(?:,[0-9]{2,3})*(?:\.[0-9]{2})?)""",
            RegexOption.IGNORE_CASE,
        )
        for (page in pages) {
            val match = pattern.find(page.text)
            if (match != null) {
                val numStr = match.groupValues[1].replace(",", "")
                val value = numStr.toDoubleOrNull()
                if (value != null && value >= 1000) {
                    return ExtractedMatch(value, match.value.trim(), page.pageNumber, "Clause 1.1")
                }
            }
        }

        // Word format e.g. 5 Lakhs, 50 Lakhs
        val wordPattern = Regex(
            """(?:sanctioned\s+amount|loan\s+amount|principal)\D{1,25}(?:rs\.?|inr|₹)?\s*([0-9]+(?:\.[0-9]+)?)\s*(lakhs?|lacs?|crores?|cr\b)""",
            RegexOption.IGNORE_CASE,
        )
        for (page in pages) {
            val match = wordPattern.find(page.text)
            if (match != null) {
                val num = match.groupValues[1].toDoubleOrNull()
                val unit = match.groupValues[2].lowercase()
                if (num != null) {
                    val multiplier = if (unit.startsWith("cr")) 10_000_000.0 else 100_000.0
                    val value = num * multiplier
                    return ExtractedMatch(value, match.value.trim(), page.pageNumber, "Clause 1.1")
                }
            }
        }
        return null
    }

    private fun extractInterestRate(pages: List<PageExtraction>, fullText: String): ExtractedMatch<Double>? {
        val pattern = Regex(
            """(?:interest\s+rate|rate\s+of\s+interest|roi|annual\s+rate|interest\s+at)\D{1,25}([0-9]{1,2}(?:\.[0-9]{1,2})?)\s*%""",
            RegexOption.IGNORE_CASE,
        )
        for (page in pages) {
            val match = pattern.find(page.text)
            if (match != null) {
                val rate = match.groupValues[1].toDoubleOrNull()
                if (rate != null && rate in 1.0..60.0) {
                    return ExtractedMatch(rate, match.value.trim(), page.pageNumber, "Clause 2.1")
                }
            }
        }
        val fallback = Regex("""([0-9]{1,2}(?:\.[0-9]{1,2})?)\s*%\s*(?:p\.?a\.?|per\s+annum)""", RegexOption.IGNORE_CASE)
        for (page in pages) {
            val fbMatch = fallback.find(page.text)
            if (fbMatch != null) {
                val rate = fbMatch.groupValues[1].toDoubleOrNull()
                if (rate != null && rate in 1.0..60.0) {
                    return ExtractedMatch(rate, fbMatch.value.trim(), page.pageNumber, "Clause 2.1")
                }
            }
        }
        return null
    }

    private fun extractInterestMethod(pages: List<PageExtraction>, fullText: String): ExtractedMatch<InterestMethod>? {
        for (page in pages) {
            if (page.text.contains("reducing balance", ignoreCase = true) ||
                page.text.contains("diminishing balance", ignoreCase = true) ||
                page.text.contains("reducing basis", ignoreCase = true)
            ) {
                return ExtractedMatch(InterestMethod.REDUCING_BALANCE, "Reducing balance basis stated in agreement", page.pageNumber)
            }
            if (page.text.contains("flat rate", ignoreCase = true) ||
                page.text.contains("flat basis", ignoreCase = true)
            ) {
                return ExtractedMatch(InterestMethod.FLAT, "Flat rate basis stated in agreement", page.pageNumber)
            }
        }
        return null
    }

    private fun extractTenure(pages: List<PageExtraction>, fullText: String): ExtractedMatch<Int>? {
        val monthPattern = Regex(
            """(?:tenure|term|period|duration|repayable\s+in)\D{1,25}([0-9]{1,3})\s*(?:months?|m\b|monthly\s+instalments?)""",
            RegexOption.IGNORE_CASE,
        )
        for (page in pages) {
            val mMatch = monthPattern.find(page.text)
            if (mMatch != null) {
                val months = mMatch.groupValues[1].toIntOrNull()
                if (months != null && months in 3..360) {
                    return ExtractedMatch(months, mMatch.value.trim(), page.pageNumber, "Clause 3.1")
                }
            }
        }

        val yearPattern = Regex(
            """(?:tenure|term|period|duration|repayable\s+over)\D{1,25}([0-9]{1,2}(?:\.[0-9]+)?)\s*(?:years?|yrs?\b)""",
            RegexOption.IGNORE_CASE,
        )
        for (page in pages) {
            val yMatch = yearPattern.find(page.text)
            if (yMatch != null) {
                val years = yMatch.groupValues[1].toDoubleOrNull()
                if (years != null && years in 0.5..30.0) {
                    val months = (years * 12).roundToInt()
                    return ExtractedMatch(months, "${yMatch.value.trim()} ($months months)", page.pageNumber, "Clause 3.1")
                }
            }
        }
        return null
    }

    private fun extractStatedEmi(pages: List<PageExtraction>, fullText: String): ExtractedMatch<Double>? {
        val pattern = Regex(
            """(?:emi|monthly\s+installment|equated\s+monthly\s+installment|monthly\s+instalment)\D{1,30}(?:rs\.?|inr|₹)?\s*([0-9]{1,3}(?:,[0-9]{2,3})*(?:\.[0-9]{2})?)""",
            RegexOption.IGNORE_CASE,
        )
        for (page in pages) {
            val match = pattern.find(page.text)
            if (match != null) {
                val numStr = match.groupValues[1].replace(",", "")
                val value = numStr.toDoubleOrNull()
                if (value != null && value >= 100) {
                    return ExtractedMatch(value, match.value.trim(), page.pageNumber, "Clause 3.2")
                }
            }
        }
        return null
    }

    private fun extractStatedTotalRepayment(pages: List<PageExtraction>, fullText: String): ExtractedMatch<Double>? {
        val pattern = Regex(
            """(?:total\s+repayment|total\s+amount\s+payable|sum\s+of\s+instalments|total\s+payable)\D{1,30}(?:rs\.?|inr|₹)?\s*([0-9]{1,3}(?:,[0-9]{2,3})*(?:\.[0-9]{2})?)""",
            RegexOption.IGNORE_CASE,
        )
        for (page in pages) {
            val match = pattern.find(page.text)
            if (match != null) {
                val numStr = match.groupValues[1].replace(",", "")
                val value = numStr.toDoubleOrNull()
                if (value != null && value >= 1000) {
                    return ExtractedMatch(value, match.value.trim(), page.pageNumber, "Clause 3.3")
                }
            }
        }
        return null
    }

    data class ProcessingFeeInfo(
        val feeAmount: Double,
        val gstAmount: Double?,
        val totalWithGst: Double,
        val percentage: Double?,
        val fixedAmount: Double?,
        val gstRatePercent: Double?,
        val isGstIncluded: Boolean,
        val isGstAssumed: Boolean,
        val isUpfront: Boolean,
        val formulaDescription: String,
        val summaryText: String,
        val assumptions: String?,
        val snippet: String,
        val pageNumber: Int,
        val clause: String?,
        val kind: EvidenceKind,
    )

    private fun extractProcessingFeeDetails(
        pages: List<PageExtraction>,
        fullText: String,
        principal: Double?,
    ): ProcessingFeeInfo? {
        val pattern = Regex(
            """(?:processing\s+fee|admin\s+fee|processing\s+charge)\D{1,35}((?:[0-9]{1,2}(?:\.[0-9]{1,2})?\s*%)|(?:(?:rs\.?|inr|₹)?\s*[0-9,]+(?:\.[0-9]{2})?))""",
            RegexOption.IGNORE_CASE,
        )

        for (page in pages) {
            val match = pattern.find(page.text)
            if (match != null) {
                val matchedText = match.groupValues[1].trim()
                val contextWindow = page.text.substring(
                    (match.range.first - 40).coerceAtLeast(0),
                    (match.range.last + 80).coerceAtMost(page.text.length),
                )

                val hasGstMention = contextWindow.contains("gst", ignoreCase = true) ||
                    contextWindow.contains("tax", ignoreCase = true)
                val isGstIncluded = contextWindow.contains("inclusive of gst", ignoreCase = true) ||
                    contextWindow.contains("including gst", ignoreCase = true) ||
                    contextWindow.contains("including taxes", ignoreCase = true)
                val isUpfront = contextWindow.contains("disburs", ignoreCase = true) ||
                    contextWindow.contains("deduct", ignoreCase = true) ||
                    contextWindow.contains("prior to", ignoreCase = true) ||
                    true // Default banking convention is upfront deduction

                val gstRate = if (hasGstMention) 18.0 else null
                val isGstAssumed = hasGstMention && !contextWindow.contains("18%")

                if (matchedText.endsWith("%")) {
                    val pct = matchedText.replace("%", "").trim().toDoubleOrNull()
                    if (pct != null && pct > 0) {
                        val baseAmount = principal ?: 500_000.0
                        val rawFee = baseAmount * (pct / 100.0)
                        val gst = if (gstRate != null && !isGstIncluded) rawFee * (gstRate / 100.0) else null
                        val total = rawFee + (gst ?: 0.0)
                        return ProcessingFeeInfo(
                            feeAmount = rawFee,
                            gstAmount = gst,
                            totalWithGst = total,
                            percentage = pct,
                            fixedAmount = null,
                            gstRatePercent = gstRate,
                            isGstIncluded = isGstIncluded,
                            isGstAssumed = isGstAssumed,
                            isUpfront = isUpfront,
                            formulaDescription = "$pct% of principal" + (if (gstRate != null) " + ${gstRate.toInt()}% GST" else ""),
                            summaryText = "$pct% of loan amount" + (if (hasGstMention) " + applicable taxes" else ""),
                            assumptions = if (isGstAssumed) "Assumed standard 18% statutory GST (configurable)" else null,
                            snippet = match.value.trim(),
                            pageNumber = page.pageNumber,
                            clause = "Clause 4.1",
                            kind = EvidenceKind.EXPLICIT,
                        )
                    }
                } else {
                    val num = matchedText.replace(Regex("""[^0-9.]"""), "").toDoubleOrNull()
                    if (num != null && num > 0) {
                        val gst = if (gstRate != null && !isGstIncluded) num * (gstRate / 100.0) else null
                        val total = num + (gst ?: 0.0)
                        return ProcessingFeeInfo(
                            feeAmount = num,
                            gstAmount = gst,
                            totalWithGst = total,
                            percentage = null,
                            fixedAmount = num,
                            gstRatePercent = gstRate,
                            isGstIncluded = isGstIncluded,
                            isGstAssumed = isGstAssumed,
                            isUpfront = isUpfront,
                            formulaDescription = "${Money.inr(num)} fixed" + (if (gstRate != null) " + ${gstRate.toInt()}% GST" else ""),
                            summaryText = Money.inr(num) + (if (hasGstMention) " + taxes" else ""),
                            assumptions = if (isGstAssumed) "Assumed standard 18% statutory GST (configurable)" else null,
                            snippet = match.value.trim(),
                            pageNumber = page.pageNumber,
                            clause = "Clause 4.1",
                            kind = EvidenceKind.EXPLICIT,
                        )
                    }
                }
            }
        }
        return null
    }

    private fun extractStampDuty(pages: List<PageExtraction>, fullText: String): ExtractedMatch<Double>? {
        val pattern = Regex(
            """(?:stamp\s+duty|stamp\s+paper)\D{1,25}(?:rs\.?|inr|₹)?\s*([0-9,]+(?:\.[0-9]{2})?)""",
            RegexOption.IGNORE_CASE,
        )
        for (page in pages) {
            val match = pattern.find(page.text)
            if (match != null) {
                val num = match.groupValues[1].replace(",", "").toDoubleOrNull()
                if (num != null && num in 50.0..50_000.0) {
                    return ExtractedMatch(num, match.value.trim(), page.pageNumber, "Schedule B")
                }
            }
        }
        return null
    }

    private fun extractDocumentationCharges(pages: List<PageExtraction>, fullText: String): ExtractedMatch<Double>? {
        val pattern = Regex(
            """(?:documentation|administrative|doc)\s+charges?\D{1,25}(?:rs\.?|inr|₹)?\s*([0-9,]+(?:\.[0-9]{2})?)""",
            RegexOption.IGNORE_CASE,
        )
        for (page in pages) {
            val match = pattern.find(page.text)
            if (match != null) {
                val num = match.groupValues[1].replace(",", "").toDoubleOrNull()
                if (num != null && num in 100.0..50_000.0) {
                    return ExtractedMatch(num, match.value.trim(), page.pageNumber, "Clause 4.3")
                }
            }
        }
        return null
    }

    private fun extractInsurance(pages: List<PageExtraction>, fullText: String): ExtractedMatch<Double>? {
        val pattern = Regex(
            """(?:credit\s+shield|credit\s+protection|loan\s+insurance|insurance\s+premium)\D{1,30}(?:rs\.?|inr|₹)?\s*([0-9,]+(?:\.[0-9]{2})?)""",
            RegexOption.IGNORE_CASE,
        )
        for (page in pages) {
            val match = pattern.find(page.text)
            if (match != null) {
                val num = match.groupValues[1].replace(",", "").toDoubleOrNull()
                if (num != null && num in 500.0..100_000.0) {
                    return ExtractedMatch(num, match.value.trim(), page.pageNumber, "Clause 5.2")
                }
            }
        }
        return null
    }

    data class PrepaymentDetails(
        val percentage: Double?,
        val hasGstMention: Boolean,
        val displayText: String,
        val formulaDescription: String,
        val assumptions: String?,
        val snippet: String,
        val pageNumber: Int,
        val clause: String?,
        val kind: EvidenceKind,
    )

    private fun extractPrepaymentDetails(pages: List<PageExtraction>, fullText: String): PrepaymentDetails? {
        val pattern = Regex(
            """(?:foreclosure|prepayment|pre-payment)\D{1,40}(chargeable|allowed|permitted|nil|zero|[0-9]{1,2}(?:\.[0-9]{1,2})?\s*%)""",
            RegexOption.IGNORE_CASE,
        )
        for (page in pages) {
            val match = pattern.find(page.text)
            if (match != null) {
                val captured = match.groupValues[1].trim()
                val context = page.text.substring(
                    (match.range.first - 20).coerceAtLeast(0),
                    (match.range.last + 60).coerceAtMost(page.text.length),
                )
                val pct = if (captured.endsWith("%")) captured.replace("%", "").trim().toDoubleOrNull() else null
                val hasGst = context.contains("gst", ignoreCase = true) || context.contains("tax", ignoreCase = true)
                val desc = if (pct != null) {
                    "$pct% of outstanding principal" + (if (hasGst) " + applicable GST" else "")
                } else {
                    "Foreclosure condition: $captured"
                }

                return PrepaymentDetails(
                    percentage = pct,
                    hasGstMention = hasGst,
                    displayText = desc,
                    formulaDescription = desc,
                    assumptions = "Charge applies on eligible outstanding principal at the date of foreclosure.",
                    snippet = match.value.trim(),
                    pageNumber = page.pageNumber,
                    clause = "Clause 7.1",
                    kind = if (pct != null) EvidenceKind.EXPLICIT else EvidenceKind.HEURISTIC,
                )
            }
        }
        return null
    }

    data class LatePaymentDetails(
        val percentage: Double?,
        val fixedFee: Double?,
        val displayText: String,
        val formulaDescription: String,
        val snippet: String,
        val pageNumber: Int,
        val clause: String?,
        val kind: EvidenceKind,
    )

    private fun extractLatePaymentDetails(pages: List<PageExtraction>, fullText: String): LatePaymentDetails? {
        val pattern = Regex(
            """(?:late\s+payment|penal\s+interest|overdue\s+interest|default\s+charge)\D{1,30}([0-9]{1,2}(?:\.[0-9]{1,2})?\s*%\s*(?:per\s+month|p\.?m\.?|per\s+annum|p\.?a\.?))""",
            RegexOption.IGNORE_CASE,
        )
        for (page in pages) {
            val match = pattern.find(page.text)
            if (match != null) {
                val textMatch = match.groupValues[1].trim()
                val pct = textMatch.substringBefore("%").trim().toDoubleOrNull()
                return LatePaymentDetails(
                    percentage = pct,
                    fixedFee = null,
                    displayText = textMatch,
                    formulaDescription = "$textMatch on overdue installment",
                    snippet = match.value.trim(),
                    pageNumber = page.pageNumber,
                    clause = "Clause 8.1",
                    kind = EvidenceKind.EXPLICIT,
                )
            }
        }
        return null
    }

    private fun findRevisionClauses(pages: List<PageExtraction>, fullText: String): List<ExtractedMatch<String>> {
        val keywords = listOf(
            "revise charges",
            "at the discretion of",
            "sole discretion",
            "as notified by the lender",
            "subject to change without notice",
        )
        val matches = mutableListOf<ExtractedMatch<String>>()
        for (page in pages) {
            for (kw in keywords) {
                val idx = page.text.indexOf(kw, ignoreCase = true)
                if (idx >= 0) {
                    val start = (idx - 30).coerceAtLeast(0)
                    val end = (idx + kw.length + 50).coerceAtMost(page.text.length)
                    matches.add(
                        ExtractedMatch(
                            value = kw,
                            snippet = page.text.substring(start, end).replace('\n', ' ').trim(),
                            pageNumber = page.pageNumber,
                            clause = "Revision Term",
                        ),
                    )
                }
            }
        }
        return matches
    }

    private fun buildKeyObligations(
        pages: List<PageExtraction>,
        text: String,
        prepayment: PrepaymentDetails?,
        lateCharge: LatePaymentDetails?,
    ): List<KeyObligation> {
        val obligations = mutableListOf<KeyObligation>()

        // 1. Prepayment & Foreclosure
        if (prepayment != null) {
            obligations.add(
                KeyObligation(
                    category = "Prepayment & Foreclosure",
                    summary = prepayment.displayText,
                    status = ObligationStatus.EXTRACTED,
                    details = "Foreclosure permitted subject to stated fees and terms.",
                    evidence = prepayment.snippet,
                    pageNumber = prepayment.pageNumber,
                    clauseReference = prepayment.clause,
                ),
            )
        } else {
            obligations.add(
                KeyObligation(
                    category = "Prepayment & Foreclosure",
                    summary = "Foreclosure terms not found in analyzed text",
                    status = ObligationStatus.NOT_FOUND,
                    details = "No explicit prepayment condition was identified. ('Not found' does not mean it does not exist in the agreement).",
                ),
            )
        }

        // 2. Collateral / Security
        val collateralMatch = pages.firstOrNull {
            it.text.contains("hypothecat", ignoreCase = true) ||
                it.text.contains("mortgage", ignoreCase = true) ||
                it.text.contains("unsecured", ignoreCase = true) ||
                it.text.contains("clean loan", ignoreCase = true) ||
                it.text.contains("promissory note", ignoreCase = true)
        }
        if (collateralMatch != null) {
            val isUnsecured = collateralMatch.text.contains("unsecured", ignoreCase = true) ||
                collateralMatch.text.contains("clean loan", ignoreCase = true)
            obligations.add(
                KeyObligation(
                    category = "Collateral & Security",
                    summary = if (isUnsecured) "Unsecured loan / Demand Promissory Note" else "Security / Charge on assets required",
                    status = ObligationStatus.EXTRACTED,
                    details = if (isUnsecured) "No asset collateral pledged; supported by borrower personal covenant." else "Asset hypothecation or mortgage stipulated.",
                    pageNumber = collateralMatch.pageNumber,
                    clauseReference = "Security Clause",
                ),
            )
        } else {
            obligations.add(
                KeyObligation(
                    category = "Collateral & Security",
                    summary = "Security details not found in analyzed text",
                    status = ObligationStatus.NOT_FOUND,
                    details = "Collateral or hypothecation terms were not explicitly detected.",
                ),
            )
        }

        // 3. Guarantor Obligations
        val guarantorMatch = pages.firstOrNull {
            it.text.contains("guarantor", ignoreCase = true) ||
                it.text.contains("co-borrower", ignoreCase = true) ||
                it.text.contains("co-applicant", ignoreCase = true)
        }
        if (guarantorMatch != null) {
            obligations.add(
                KeyObligation(
                    category = "Guarantor Obligations",
                    summary = "Co-borrower or guarantor obligation detected",
                    status = ObligationStatus.REVIEW_REQUIRED,
                    details = "Verify whether guarantor liability is joint, several, and continuous.",
                    pageNumber = guarantorMatch.pageNumber,
                ),
            )
        } else {
            obligations.add(
                KeyObligation(
                    category = "Guarantor Obligations",
                    summary = "No guarantor requirement identified in scanned pages",
                    status = ObligationStatus.NOT_FOUND,
                    details = "No explicit guarantor stipulation was located in the analyzed text.",
                ),
            )
        }

        // 4. Insurance
        val insuranceMatch = pages.firstOrNull {
            it.text.contains("insurance", ignoreCase = true) ||
                it.text.contains("credit shield", ignoreCase = true)
        }
        if (insuranceMatch != null) {
            obligations.add(
                KeyObligation(
                    category = "Insurance",
                    summary = "Credit life or property insurance mentioned",
                    status = ObligationStatus.REVIEW_REQUIRED,
                    details = "Check whether insurance coverage is strictly mandatory or an optional opt-out add-on.",
                    pageNumber = insuranceMatch.pageNumber,
                    clauseReference = "Insurance Term",
                ),
            )
        } else {
            obligations.add(
                KeyObligation(
                    category = "Insurance",
                    summary = "Insurance conditions not found",
                    status = ObligationStatus.NOT_FOUND,
                    details = "No mandatory insurance requirements detected in analyzed text.",
                ),
            )
        }

        // 5. Late Payment Terms
        if (lateCharge != null) {
            obligations.add(
                KeyObligation(
                    category = "Late Payment Terms",
                    summary = lateCharge.displayText,
                    status = ObligationStatus.EXTRACTED,
                    details = "Default in payment accrues penal interest at stated rates until cured.",
                    evidence = lateCharge.snippet,
                    pageNumber = lateCharge.pageNumber,
                    clauseReference = lateCharge.clause,
                ),
            )
        } else {
            obligations.add(
                KeyObligation(
                    category = "Late Payment Terms",
                    summary = "Late payment terms not found",
                    status = ObligationStatus.NOT_FOUND,
                    details = "Specific default charges were not identified in analyzed pages.",
                ),
            )
        }

        // 6. Interest Rate Changes / Variations
        val variationMatch = pages.firstOrNull {
            it.text.contains("sole discretion", ignoreCase = true) ||
                it.text.contains("at the discretion", ignoreCase = true) ||
                it.text.contains("revise", ignoreCase = true) ||
                it.text.contains("floating", ignoreCase = true) ||
                it.text.contains("mclr", ignoreCase = true) ||
                it.text.contains("eblr", ignoreCase = true)
        }
        if (variationMatch != null) {
            obligations.add(
                KeyObligation(
                    category = "Interest Rate Changes",
                    summary = "Rate variation or lender discretion clause detected",
                    status = ObligationStatus.REVIEW_REQUIRED,
                    details = "Agreement contains clauses allowing rate adjustments or benchmark resets.",
                    pageNumber = variationMatch.pageNumber,
                ),
            )
        } else {
            obligations.add(
                KeyObligation(
                    category = "Interest Rate Changes",
                    summary = "Fixed rate / no variation clause found",
                    status = ObligationStatus.NOT_FOUND,
                    details = "No explicit rate variation or benchmark reset clause identified in analyzed pages.",
                ),
            )
        }

        // 7. Other Obligations
        val noticeMatch = pages.firstOrNull {
            it.text.contains("notify", ignoreCase = true) ||
                it.text.contains("change of address", ignoreCase = true) ||
                it.text.contains("employment", ignoreCase = true)
        }
        if (noticeMatch != null) {
            obligations.add(
                KeyObligation(
                    category = "Other Borrower Obligations",
                    summary = "Obligation to notify change of address, employment, or bank account",
                    status = ObligationStatus.EXTRACTED,
                    details = "Borrower is required to report personal, residential, or employment changes to lender.",
                    pageNumber = noticeMatch.pageNumber,
                ),
            )
        } else {
            obligations.add(
                KeyObligation(
                    category = "Other Borrower Obligations",
                    summary = "Standard borrower covenants apply",
                    status = ObligationStatus.NOT_FOUND,
                    details = "No unusual non-financial covenants detected in analyzed text.",
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
        prepaymentMatch: PrepaymentDetails?,
        procFeeMatch: ProcessingFeeInfo?,
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
