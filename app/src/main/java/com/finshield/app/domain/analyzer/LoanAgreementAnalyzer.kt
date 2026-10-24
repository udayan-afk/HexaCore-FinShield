package com.finshield.app.domain.analyzer

import com.finshield.app.domain.calculator.LoanCalculator
import com.finshield.app.domain.calculator.LoanInput
import com.finshield.app.domain.calculator.Money
import com.finshield.app.model.AnalysisReport
import com.finshield.app.model.EvidenceKind
import com.finshield.app.model.Finding
import java.util.UUID
import kotlin.math.abs

object LoanAgreementAnalyzer {

    fun analyze(
        documentName: String,
        extraction: DocumentExtractionResult,
        reportId: String = UUID.randomUUID().toString(),
    ): AnalysisReport {
        val text = extraction.fullText
        val terms = mutableListOf<Finding>()
        val fees = mutableListOf<Finding>()
        val missing = mutableListOf<String>()
        val review = mutableListOf<String>()

        // 1. Sanctioned Principal Amount
        val principalMatch = extractPrincipal(text)
        val principal = principalMatch?.first
        if (principalMatch != null) {
            terms.add(
                Finding(
                    label = "Sanctioned amount",
                    value = Money.inr(principalMatch.first),
                    evidence = principalMatch.second,
                    kind = EvidenceKind.EXPLICIT,
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

        // 2. Interest Rate
        val rateMatch = extractInterestRate(text)
        val annualRate = rateMatch?.first
        if (rateMatch != null) {
            terms.add(
                Finding(
                    label = "Interest rate",
                    value = "${rateMatch.first}% per annum",
                    evidence = rateMatch.second,
                    kind = EvidenceKind.EXPLICIT,
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
            missing.add("Interest rate (annual ROI) was not explicitly identified.")
        }

        // 3. Tenure
        val tenureMatch = extractTenure(text)
        val tenureMonths = tenureMatch?.first
        if (tenureMatch != null) {
            terms.add(
                Finding(
                    label = "Tenure",
                    value = "${tenureMatch.first} months",
                    evidence = tenureMatch.second,
                    kind = EvidenceKind.EXPLICIT,
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

        // 4. Stated EMI
        val statedEmiMatch = extractStatedEmi(text)
        val statedEmi = statedEmiMatch?.first
        if (statedEmiMatch != null) {
            terms.add(
                Finding(
                    label = "Stated EMI",
                    value = Money.inr(statedEmiMatch.first),
                    evidence = statedEmiMatch.second,
                    kind = EvidenceKind.EXPLICIT,
                ),
            )
        }

        // 5. Processing Fee
        val feeMatch = extractProcessingFee(text)
        if (feeMatch != null) {
            fees.add(
                Finding(
                    label = "Processing fee",
                    value = feeMatch.first,
                    evidence = feeMatch.second,
                    kind = EvidenceKind.EXPLICIT,
                ),
            )
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

        // 6. Late Payment / Overdue Charges
        val lateChargeMatch = extractLatePaymentCharge(text)
        if (lateChargeMatch != null) {
            fees.add(
                Finding(
                    label = "Late-payment charge",
                    value = lateChargeMatch.first,
                    evidence = lateChargeMatch.second,
                    kind = EvidenceKind.EXPLICIT,
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

        // 7. Prepayment / Foreclosure
        val prepaymentMatch = extractPrepayment(text)
        if (prepaymentMatch != null) {
            fees.add(
                Finding(
                    label = "Prepayment / foreclosure",
                    value = prepaymentMatch.first,
                    evidence = prepaymentMatch.second,
                    kind = EvidenceKind.HEURISTIC,
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
            missing.add("Foreclosure and partial prepayment terms were not identified.")
        }

        // 8. Clauses Requiring Manual Review
        val revisionClauses = findRevisionClauses(text)
        if (revisionClauses.isNotEmpty()) {
            review.add("Lender interest/charge variation clause detected (${revisionClauses.joinToString(", ")})")
        }

        if (text.contains("arbitration", ignoreCase = true) || text.contains("jurisdiction", ignoreCase = true)) {
            review.add("Dispute resolution and jurisdiction clause detected. Verify dispute forum.")
        }

        // 9. Repayment Check & Discrepancies
        if (principal != null && annualRate != null && tenureMonths != null) {
            val calc = LoanCalculator.calculate(LoanInput(principal, annualRate, tenureMonths))
            if (statedEmi != null) {
                val diff = statedEmi - calc.installment
                if (abs(diff) >= 50.0) {
                    review.add(
                        "Stated EMI (${Money.inr(statedEmi)}) differs from standard amortisation (${Money.inr(calc.installment)}) by ${Money.inr(abs(diff))}. Clarify fee capitalisation or flat-rate calculation with the lender.",
                    )
                }
            }
        }

        // Add OCR warnings if any
        extraction.warnings.forEach { warning ->
            review.add(warning)
        }

        if (extraction.fullText.length < 150) {
            review.add("The scanned document contained very little text. Ensure the physical agreement or PDF is fully readable and clear.")
        }

        return AnalysisReport(
            documentName = documentName,
            isSample = false,
            principal = principal,
            annualRatePercent = annualRate,
            tenureMonths = tenureMonths,
            statedEmi = statedEmi,
            terms = terms,
            feesAndPenalties = fees,
            missingInformation = missing,
            manualReview = review,
        )
    }

    private fun extractPrincipal(text: String): Pair<Double, String>? {
        // Regex for loan amount / sanctioned amount
        val pattern = Regex(
            """(?:sanctioned\s+amount|loan\s+amount|principal\s+amount|loan\s+of|amount\s+of\s+loan)\D{1,25}(?:rs\.?|inr|₹)?\s*([0-9]{1,3}(?:,[0-9]{2,3})*(?:\.[0-9]{2})?)""",
            RegexOption.IGNORE_CASE,
        )
        val match = pattern.find(text)
        if (match != null) {
            val numStr = match.groupValues[1].replace(",", "")
            val value = numStr.toDoubleOrNull()
            if (value != null && value >= 1000) {
                val snippet = match.value.trim()
                return Pair(value, "Found: \"$snippet\"")
            }
        }
        return null
    }

    private fun extractInterestRate(text: String): Pair<Double, String>? {
        val pattern = Regex(
            """(?:interest\s+rate|rate\s+of\s+interest|roi|annual\s+rate)\D{1,20}([0-9]{1,2}(?:\.[0-9]{1,2})?)\s*%""",
            RegexOption.IGNORE_CASE,
        )
        val match = pattern.find(text)
        if (match != null) {
            val rate = match.groupValues[1].toDoubleOrNull()
            if (rate != null && rate in 1.0..60.0) {
                return Pair(rate, "Found: \"${match.value.trim()}\"")
            }
        }
        // Fallback for simple percentage near "interest"
        val fallback = Regex("""([0-9]{1,2}(?:\.[0-9]{1,2})?)\s*%\s*(?:p\.?a\.?|per\s+annum)""", RegexOption.IGNORE_CASE)
        val fbMatch = fallback.find(text)
        if (fbMatch != null) {
            val rate = fbMatch.groupValues[1].toDoubleOrNull()
            if (rate != null && rate in 1.0..60.0) {
                return Pair(rate, "Found: \"${fbMatch.value.trim()}\"")
            }
        }
        return null
    }

    private fun extractTenure(text: String): Pair<Int, String>? {
        // Months
        val monthPattern = Regex(
            """(?:tenure|term|period|duration)\D{1,20}([0-9]{1,3})\s*(?:months?|m\b)""",
            RegexOption.IGNORE_CASE,
        )
        val mMatch = monthPattern.find(text)
        if (mMatch != null) {
            val months = mMatch.groupValues[1].toIntOrNull()
            if (months != null && months in 3..360) {
                return Pair(months, "Found: \"${mMatch.value.trim()}\"")
            }
        }

        // Years
        val yearPattern = Regex(
            """(?:tenure|term|period|duration)\D{1,20}([0-9]{1,2})\s*(?:years?|yrs?\b)""",
            RegexOption.IGNORE_CASE,
        )
        val yMatch = yearPattern.find(text)
        if (yMatch != null) {
            val years = yMatch.groupValues[1].toIntOrNull()
            if (years != null && years in 1..30) {
                return Pair(years * 12, "Found: \"${yMatch.value.trim()}\" (${years * 12} months)")
            }
        }
        return null
    }

    private fun extractStatedEmi(text: String): Pair<Double, String>? {
        val pattern = Regex(
            """(?:emi|monthly\s+installment|equated\s+monthly\s+installment)\D{1,25}(?:rs\.?|inr|₹)?\s*([0-9]{1,3}(?:,[0-9]{2,3})*(?:\.[0-9]{2})?)""",
            RegexOption.IGNORE_CASE,
        )
        val match = pattern.find(text)
        if (match != null) {
            val numStr = match.groupValues[1].replace(",", "")
            val value = numStr.toDoubleOrNull()
            if (value != null && value >= 100) {
                return Pair(value, "Found: \"${match.value.trim()}\"")
            }
        }
        return null
    }

    private fun extractProcessingFee(text: String): Pair<String, String>? {
        val pattern = Regex(
            """(?:processing\s+fee|admin\s+fee)\D{1,30}((?:[0-9]{1,2}(?:\.[0-9]{1,2})?\s*%)|(?:(?:rs\.?|inr|₹)?\s*[0-9,]+(?:\.[0-9]{2})?))""",
            RegexOption.IGNORE_CASE,
        )
        val match = pattern.find(text)
        if (match != null) {
            return Pair(match.groupValues[1].trim(), "Found: \"${match.value.trim()}\"")
        }
        return null
    }

    private fun extractLatePaymentCharge(text: String): Pair<String, String>? {
        val pattern = Regex(
            """(?:late\s+payment|penal\s+interest|overdue\s+interest|default\s+charge)\D{1,30}([0-9]{1,2}(?:\.[0-9]{1,2})?\s*%\s*(?:per\s+month|p\.?m\.?|per\s+annum|p\.?a\.?))""",
            RegexOption.IGNORE_CASE,
        )
        val match = pattern.find(text)
        if (match != null) {
            return Pair(match.groupValues[1].trim(), "Found: \"${match.value.trim()}\"")
        }
        return null
    }

    private fun extractPrepayment(text: String): Pair<String, String>? {
        val pattern = Regex(
            """(?:foreclosure|prepayment|pre-payment)\D{1,40}(chargeable|allowed|permitted|nil|zero|[0-9]{1,2}\s*%)""",
            RegexOption.IGNORE_CASE,
        )
        val match = pattern.find(text)
        if (match != null) {
            return Pair("Foreclosure condition: ${match.groupValues[1].trim()}", "Found: \"${match.value.trim()}\"")
        }
        return null
    }

    private fun findRevisionClauses(text: String): List<String> {
        val keywords = listOf(
            "revise charges",
            "at the discretion of",
            "discretion",
            "as notified by the lender",
            "subject to change without notice",
            "sole discretion",
        )
        return keywords.filter { text.contains(it, ignoreCase = true) }
    }
}
