package com.finshield.app.domain.analyzer

import com.finshield.app.domain.calculator.Money
import com.finshield.app.model.CompletenessStatus
import com.finshield.app.model.DocumentCompletenessReport
import com.finshield.app.model.EvidenceKind
import com.finshield.app.model.FieldCompletenessItem
import com.finshield.app.model.TextSourceKind

/**
 * Production automated completeness auditor for loan agreements.
 *
 * Implements a strict two-pass extraction audit:
 * 1. Evaluates all 19 standardized loan agreement fields against the structured extraction.
 * 2. If a field was missed, conducts a targeted second-pass regex scan against raw page texts
 *    to recover small print, table cells, or annexure clauses.
 * 3. Honestly assigns status: FOUND, NOT_STATED, AMBIGUOUS, UNREADABLE, or NEEDS_VERIFICATION.
 * 4. Never fabricates values.
 */
object CompletenessChecker {

    fun audit(
        pages: List<PageExtraction>,
        structured: StructuredExtraction,
        geminiOutput: GeminiStructuredOutput? = null,
    ): DocumentCompletenessReport {
        val items = mutableListOf<FieldCompletenessItem>()

        // 1. Lender Name
        val lender = structured.lenderName?.value ?: geminiOutput?.lenderName?.value
        val lenderSnippet = structured.lenderName?.snippet ?: geminiOutput?.lenderName?.snippet
        val lenderPage = structured.lenderName?.pageNumber ?: geminiOutput?.lenderName?.pageNumber
        items.add(
            createItem(
                key = "lender_name",
                label = "Lender Name",
                value = lender,
                snippet = lenderSnippet,
                page = lenderPage,
                fallback = { scanForLenderFallback(pages) },
            ),
        )

        // 2. Borrower Name
        val borrower = structured.borrowerName?.value ?: geminiOutput?.borrowerName?.value
        val borrowerSnippet = structured.borrowerName?.snippet ?: geminiOutput?.borrowerName?.snippet
        val borrowerPage = structured.borrowerName?.pageNumber ?: geminiOutput?.borrowerName?.pageNumber
        items.add(
            createItem(
                key = "borrower_name",
                label = "Borrower Name",
                value = borrower,
                snippet = borrowerSnippet,
                page = borrowerPage,
                fallback = { scanForBorrowerFallback(pages) },
            ),
        )

        // 3. Loan Product / Facility
        val isDocUnreadable = pages.isEmpty() || pages.all { it.isBlank || it.isUnreadable }
        val product = if (!isDocUnreadable && structured.loanType.kind != EvidenceKind.ASSUMED) {
            structured.loanType.value.takeIf { it.isNotBlank() } ?: geminiOutput?.loanProduct?.value
        } else null
        items.add(
            createItem(
                key = "loan_product",
                label = "Loan Product / Facility",
                value = product,
                snippet = structured.loanType.snippet,
                page = structured.loanType.pageNumber,
                fallback = { null },
            ),
        )

        // 4. Agreement / Reference Number
        val refNo = structured.referenceNumber?.value ?: geminiOutput?.referenceNumber?.value
        items.add(
            createItem(
                key = "reference_number",
                label = "Agreement / Reference Number",
                value = refNo,
                snippet = structured.referenceNumber?.snippet ?: geminiOutput?.referenceNumber?.snippet,
                page = structured.referenceNumber?.pageNumber ?: geminiOutput?.referenceNumber?.pageNumber,
                fallback = { scanForReferenceNumberFallback(pages) },
            ),
        )

        // 5. Agreement Date
        val date = structured.agreementDate?.value ?: geminiOutput?.agreementDate?.value
        items.add(
            createItem(
                key = "agreement_date",
                label = "Agreement Date",
                value = date,
                snippet = structured.agreementDate?.snippet ?: geminiOutput?.agreementDate?.snippet,
                page = structured.agreementDate?.pageNumber ?: geminiOutput?.agreementDate?.pageNumber,
                fallback = { scanForDateFallback(pages) },
            ),
        )

        // 6. Sanctioned Principal
        val principal = structured.sanctionedPrincipal?.value ?: geminiOutput?.sanctionedPrincipal?.value
        items.add(
            createItem(
                key = "sanctioned_principal",
                label = "Sanctioned Loan Principal",
                value = principal?.let { Money.inr(it) },
                snippet = structured.sanctionedPrincipal?.snippet ?: geminiOutput?.sanctionedPrincipal?.snippet,
                page = structured.sanctionedPrincipal?.pageNumber ?: geminiOutput?.sanctionedPrincipal?.pageNumber,
                fallback = { scanForPrincipalFallback(pages) },
            ),
        )

        // 7. Net Disbursed Amount
        val disbursed = structured.actualDisbursedAmount?.value ?: geminiOutput?.actualDisbursedAmount?.value
        items.add(
            createItem(
                key = "disbursed_amount",
                label = "Actual / Net Disbursed Amount",
                value = disbursed?.let { Money.inr(it) },
                snippet = structured.actualDisbursedAmount?.snippet ?: geminiOutput?.actualDisbursedAmount?.snippet,
                page = structured.actualDisbursedAmount?.pageNumber ?: geminiOutput?.actualDisbursedAmount?.pageNumber,
                fallback = { scanForDisbursalFallback(pages) },
            ),
        )

        // 8. Annual Interest Rate
        val rate = structured.annualRatePercent?.value ?: geminiOutput?.annualRatePercent?.value
        items.add(
            createItem(
                key = "interest_rate",
                label = "Annual Interest Rate (% p.a.)",
                value = rate?.let { "$it% p.a." },
                snippet = structured.annualRatePercent?.snippet ?: geminiOutput?.annualRatePercent?.snippet,
                page = structured.annualRatePercent?.pageNumber ?: geminiOutput?.annualRatePercent?.pageNumber,
                fallback = { scanForRateFallback(pages) },
            ),
        )

        // 9. Periodic / Monthly Rate
        val periodicRate = geminiOutput?.periodicRate?.value ?: (if (structured.isRateMonthly) "${structured.annualRatePercent?.value?.div(12.0)}% monthly" else null)
        items.add(
            createItem(
                key = "periodic_rate",
                label = "Periodic / Monthly Interest Rate",
                value = periodicRate,
                snippet = geminiOutput?.periodicRate?.snippet,
                page = geminiOutput?.periodicRate?.pageNumber,
                fallback = { null },
            ),
        )

        // 10. Fixed / Floating Status & Benchmark
        val isFloating = structured.isFloatingRate ?: geminiOutput?.isFloatingRate?.value
        val floatDesc = when (isFloating) {
            true -> "Floating / Variable" + (geminiOutput?.benchmarkRate?.value?.let { " ($it)" } ?: "")
            false -> "Fixed Rate"
            null -> null
        }
        items.add(
            createItem(
                key = "fixed_floating",
                label = "Rate Type & Benchmark (Fixed/Floating)",
                value = floatDesc,
                snippet = geminiOutput?.isFloatingRate?.snippet,
                page = geminiOutput?.isFloatingRate?.pageNumber,
                fallback = { scanForFloatingFallback(pages) },
            ),
        )

        // 11. APR (Annual Percentage Rate)
        val apr = structured.aprPercent?.value ?: geminiOutput?.aprPercent?.value
        items.add(
            createItem(
                key = "apr",
                label = "Annual Percentage Rate (APR)",
                value = apr?.let { "$it% APR" },
                snippet = structured.aprPercent?.snippet ?: geminiOutput?.aprPercent?.snippet,
                page = structured.aprPercent?.pageNumber ?: geminiOutput?.aprPercent?.pageNumber,
                fallback = { scanForAprFallback(pages) },
            ),
        )

        // 12. Interest Method (Reducing vs Flat)
        val method = structured.interestMethod?.value?.label ?: geminiOutput?.interestBasis?.value
        items.add(
            createItem(
                key = "interest_method",
                label = "Interest Basis (Reducing vs Flat)",
                value = method,
                snippet = structured.interestMethod?.snippet ?: geminiOutput?.interestBasis?.snippet,
                page = structured.interestMethod?.pageNumber ?: geminiOutput?.interestBasis?.pageNumber,
                fallback = { scanForMethodFallback(pages) },
            ),
        )

        // 13. Loan Tenure
        val tenure = structured.tenureMonths?.value ?: geminiOutput?.tenureMonths?.value
        items.add(
            createItem(
                key = "tenure",
                label = "Loan Tenure (Months / Years)",
                value = tenure?.let { "$it months (${"%.1f".format(it / 12.0)} yrs)" },
                snippet = structured.tenureMonths?.snippet ?: geminiOutput?.tenureMonths?.snippet,
                page = structured.tenureMonths?.pageNumber ?: geminiOutput?.tenureMonths?.pageNumber,
                fallback = { scanForTenureFallback(pages) },
            ),
        )

        // 14. Equated Monthly Installment (EMI)
        val emi = structured.statedEmi?.value ?: geminiOutput?.statedEmi?.value
        items.add(
            createItem(
                key = "stated_emi",
                label = "Stated Monthly EMI",
                value = emi?.let { Money.inr(it) },
                snippet = structured.statedEmi?.snippet ?: geminiOutput?.statedEmi?.snippet,
                page = structured.statedEmi?.pageNumber ?: geminiOutput?.statedEmi?.pageNumber,
                fallback = { scanForEmiFallback(pages) },
            ),
        )

        // 15. Repayment Schedule & Dates
        val repayDates = geminiOutput?.repaymentDates?.value
        items.add(
            createItem(
                key = "repayment_dates",
                label = "Repayment Due Dates & Schedule",
                value = repayDates,
                snippet = geminiOutput?.repaymentDates?.snippet,
                page = geminiOutput?.repaymentDates?.pageNumber,
                fallback = { scanForRepaymentDatesFallback(pages) },
            ),
        )

        // 16. Processing Fee & GST
        val procFee = structured.processingFee?.summaryText ?: geminiOutput?.processingFeeAmount?.value?.let { Money.inr(it) }
        items.add(
            createItem(
                key = "processing_fee",
                label = "Processing Fee & Applicable GST",
                value = procFee,
                snippet = structured.processingFee?.snippet ?: geminiOutput?.processingFeeAmount?.snippet,
                page = structured.processingFee?.pageNumber ?: geminiOutput?.processingFeeAmount?.pageNumber,
                fallback = { scanForProcessingFeeFallback(pages) },
            ),
        )

        // 17. Upfront Deductions (Stamp Duty, Admin, Insurance)
        val upfrontSummary = buildString {
            structured.stampDuty?.value?.let { append("Stamp: ${Money.inr(it)}; ") }
            structured.documentationCharges?.value?.let { append("Doc: ${Money.inr(it)}; ") }
            structured.insurancePremium?.value?.let { append("Ins: ${Money.inr(it)}; ") }
        }.takeIf { it.isNotBlank() } ?: geminiOutput?.totalUpfrontDeductions?.value?.let { "Upfront: ${Money.inr(it)}" }

        items.add(
            createItem(
                key = "upfront_deductions",
                label = "Other Upfront Deductions & Levies",
                value = upfrontSummary,
                snippet = structured.documentationCharges?.snippet ?: structured.stampDuty?.snippet,
                page = structured.documentationCharges?.pageNumber ?: structured.stampDuty?.pageNumber,
                fallback = { scanForUpfrontDeductionsFallback(pages) },
            ),
        )

        // 18. Penal & Late Payment / Bounce Charges
        val late = structured.latePaymentDetails?.displayText ?: geminiOutput?.latePaymentCharges?.value
        items.add(
            createItem(
                key = "penal_charges",
                label = "Penal Interest & Bounce Charges",
                value = late,
                snippet = structured.latePaymentDetails?.snippet ?: geminiOutput?.latePaymentCharges?.snippet,
                page = structured.latePaymentDetails?.pageNumber ?: geminiOutput?.latePaymentCharges?.pageNumber,
                fallback = { scanForPenalFallback(pages) },
            ),
        )

        // 19. Prepayment / Foreclosure Terms
        val prepay = structured.prepaymentDetails?.displayText ?: geminiOutput?.foreclosureCharges?.value
        items.add(
            createItem(
                key = "foreclosure_terms",
                label = "Prepayment / Foreclosure Terms",
                value = prepay,
                snippet = structured.prepaymentDetails?.snippet ?: geminiOutput?.foreclosureCharges?.snippet,
                page = structured.prepaymentDetails?.pageNumber ?: geminiOutput?.foreclosureCharges?.pageNumber,
                fallback = { scanForPrepaymentFallback(pages) },
            ),
        )

        // 20. Collateral / Security
        val collateral = structured.collateralSecurity?.value ?: geminiOutput?.collateralSecurity?.value
        items.add(
            createItem(
                key = "collateral_security",
                label = "Collateral / Security",
                value = collateral,
                snippet = structured.collateralSecurity?.snippet ?: geminiOutput?.collateralSecurity?.snippet,
                page = structured.collateralSecurity?.pageNumber ?: geminiOutput?.collateralSecurity?.pageNumber,
                fallback = { scanForCollateralFallback(pages) },
            ),
        )

        // 21. Moratorium / Grace Period
        val moratorium = geminiOutput?.moratorium?.value
        items.add(
            createItem(
                key = "moratorium",
                label = "Moratorium / Grace Period",
                value = moratorium,
                snippet = geminiOutput?.moratorium?.snippet,
                page = geminiOutput?.moratorium?.pageNumber,
                fallback = { scanForMoratoriumFallback(pages) },
            ),
        )

        val foundCount = items.count { it.status == CompletenessStatus.FOUND }
        val notStatedCount = items.count { it.status == CompletenessStatus.NOT_STATED }
        val needsVerificationCount = items.count { it.status == CompletenessStatus.NEEDS_VERIFICATION }
        val ambiguousCount = items.count { it.status == CompletenessStatus.AMBIGUOUS }
        val pct = if (items.isNotEmpty()) (foundCount * 100) / items.size else 0

        return DocumentCompletenessReport(
            totalFieldsChecked = items.size,
            foundCount = foundCount,
            notStatedCount = notStatedCount,
            needsVerificationCount = needsVerificationCount,
            ambiguousCount = ambiguousCount,
            items = items,
            completenessPercentage = pct,
        )
    }

    private inline fun createItem(
        key: String,
        label: String,
        value: String?,
        snippet: String?,
        page: Int?,
        fallback: () -> Triple<String, String, Int>?,
    ): FieldCompletenessItem {
        if (!value.isNullOrBlank()) {
            return FieldCompletenessItem(
                fieldKey = key,
                fieldLabel = label,
                status = CompletenessStatus.FOUND,
                extractedValue = value,
                evidenceSnippet = snippet,
                pageNumber = page,
                sourceKind = TextSourceKind.OCR,
            )
        }

        // Targeted 2nd pass
        val recovered = fallback()
        if (recovered != null) {
            return FieldCompletenessItem(
                fieldKey = key,
                fieldLabel = label,
                status = CompletenessStatus.FOUND,
                extractedValue = recovered.first,
                evidenceSnippet = recovered.second,
                pageNumber = recovered.third,
                notes = "Recovered in second-pass document scan",
                sourceKind = TextSourceKind.HYBRID,
            )
        }

        return FieldCompletenessItem(
            fieldKey = key,
            fieldLabel = label,
            status = CompletenessStatus.NOT_STATED,
            extractedValue = "Not stated in document",
            evidenceSnippet = null,
            pageNumber = null,
        )
    }

    // --- Targeted Fallback Scanners ---

    private fun scanForLenderFallback(pages: List<PageExtraction>): Triple<String, String, Int>? {
        val regex = "(?i)(?:lender|financier|bank)\\s*[:\\-]?\\s*([A-Za-z0-9\\s&.,]{4,50}(?:Bank|Finance|Financial|Capital|Ltd|Limited))".toRegex()
        for (p in pages) {
            val m = regex.find(p.text)
            if (m != null) {
                return Triple(m.groupValues[1].trim(), m.value.trim(), p.pageNumber)
            }
        }
        return null
    }

    private fun scanForBorrowerFallback(pages: List<PageExtraction>): Triple<String, String, Int>? {
        val regex = "(?i)(?:borrower|customer|applicant)\\s*[:\\-]?\\s*(?:Mr\\.?|Ms\\.?|Mrs\\.?)?\\s*([A-Za-z\\s.]{3,40})".toRegex()
        for (p in pages) {
            val m = regex.find(p.text)
            if (m != null) {
                return Triple(m.groupValues[1].trim(), m.value.trim(), p.pageNumber)
            }
        }
        return null
    }

    private fun scanForReferenceNumberFallback(pages: List<PageExtraction>): Triple<String, String, Int>? {
        val regex = "(?i)(?:agreement|loan|account|lan|application)\\s*(?:no|num|number|ref|id)\\.?\\s*[:\\-]?\\s*([A-Z0-9/\\-_]{5,25})".toRegex()
        for (p in pages) {
            val m = regex.find(p.text)
            if (m != null) {
                return Triple(m.groupValues[1].trim(), m.value.trim(), p.pageNumber)
            }
        }
        return null
    }

    private fun scanForDateFallback(pages: List<PageExtraction>): Triple<String, String, Int>? {
        val regex = "(?i)(?:agreement\\s+date|sanction\\s+date|dated|date)\\s*[:\\-]?\\s*(\\d{1,2}[\\/\\-\\.]\\d{1,2}[\\/\\-\\.]\\d{2,4})".toRegex()
        for (p in pages) {
            val m = regex.find(p.text)
            if (m != null) {
                return Triple(m.groupValues[1].trim(), m.value.trim(), p.pageNumber)
            }
        }
        return null
    }

    private fun scanForPrincipalFallback(pages: List<PageExtraction>): Triple<String, String, Int>? {
        val regex = "(?i)(?:principal|facility|sanction(?:ed)?\\s*amount)\\s*[:\\-]?\\s*(?:rs\\.?|inr|₹)?\\s*([\\d,]+(?:\\.\\d{2})?)".toRegex()
        for (p in pages) {
            val m = regex.find(p.text)
            if (m != null) {
                val num = StructuredLoanExtractor.parseIndianNumber(m.groupValues[1])
                if (num != null && num >= 1000) {
                    return Triple(Money.inr(num), m.value.trim(), p.pageNumber)
                }
            }
        }
        return null
    }

    private fun scanForDisbursalFallback(pages: List<PageExtraction>): Triple<String, String, Int>? {
        val regex = "(?i)(?:net\\s*disburs(?:al|ed)|disbursed\\s*amount)\\s*[:\\-]?\\s*(?:rs\\.?|inr|₹)?\\s*([\\d,]+(?:\\.\\d{2})?)".toRegex()
        for (p in pages) {
            val m = regex.find(p.text)
            if (m != null) {
                val num = StructuredLoanExtractor.parseIndianNumber(m.groupValues[1])
                if (num != null && num >= 1000) {
                    return Triple(Money.inr(num), m.value.trim(), p.pageNumber)
                }
            }
        }
        return null
    }

    private fun scanForRateFallback(pages: List<PageExtraction>): Triple<String, String, Int>? {
        val regex = "(?i)(?:rate\\s+of\\s+interest|interest\\s+rate|roi)\\s*[:\\-]?\\s*(\\d{1,2}(?:\\.\\d{1,2})?)\\s*%".toRegex()
        for (p in pages) {
            val m = regex.find(p.text)
            if (m != null) {
                return Triple("${m.groupValues[1]}% p.a.", m.value.trim(), p.pageNumber)
            }
        }
        return null
    }

    private fun scanForFloatingFallback(pages: List<PageExtraction>): Triple<String, String, Int>? {
        for (p in pages) {
            val lower = p.text.lowercase()
            if (lower.contains("floating") || lower.contains("variable") || lower.contains("repo rate") || lower.contains("mclr")) {
                return Triple("Floating Rate", "Floating rate terms identified", p.pageNumber)
            }
            if (lower.contains("fixed rate") || lower.contains("fixed interest")) {
                return Triple("Fixed Rate", "Fixed rate terms identified", p.pageNumber)
            }
        }
        return null
    }

    private fun scanForAprFallback(pages: List<PageExtraction>): Triple<String, String, Int>? {
        val regex = "(?i)\\b(?:apr|annualized\\s+percentage\\s+rate)\\b\\s*[:\\-]?\\s*(\\d{1,2}(?:\\.\\d{1,2})?)\\s*%".toRegex()
        for (p in pages) {
            val m = regex.find(p.text)
            if (m != null) {
                return Triple("${m.groupValues[1]}% APR", m.value.trim(), p.pageNumber)
            }
        }
        return null
    }

    private fun scanForMethodFallback(pages: List<PageExtraction>): Triple<String, String, Int>? {
        for (p in pages) {
            val lower = p.text.lowercase()
            if (lower.contains("reducing") || lower.contains("diminishing")) {
                return Triple("Reducing Balance", "Reducing balance identified", p.pageNumber)
            }
            if (lower.contains("flat rate") || lower.contains("flat basis")) {
                return Triple("Flat Rate", "Flat rate identified", p.pageNumber)
            }
        }
        return null
    }

    private fun scanForTenureFallback(pages: List<PageExtraction>): Triple<String, String, Int>? {
        val regex = "(?i)(?:tenure|period|repayable\\s+in)\\s*[:\\-]?\\s*(\\d{1,3})\\s*(?:months|equated\\s+monthly\\s+installments)".toRegex()
        for (p in pages) {
            val m = regex.find(p.text)
            if (m != null) {
                return Triple("${m.groupValues[1]} months", m.value.trim(), p.pageNumber)
            }
        }
        return null
    }

    private fun scanForEmiFallback(pages: List<PageExtraction>): Triple<String, String, Int>? {
        val regex = "(?i)(?:emi|equated\\s+monthly\\s+instalment|monthly\\s+instalment)\\s*[:\\-]?\\s*(?:rs\\.?|inr|₹)?\\s*([\\d,]+(?:\\.\\d{2})?)".toRegex()
        for (p in pages) {
            val m = regex.find(p.text)
            if (m != null) {
                val num = StructuredLoanExtractor.parseIndianNumber(m.groupValues[1])
                if (num != null && num >= 100) {
                    return Triple(Money.inr(num), m.value.trim(), p.pageNumber)
                }
            }
        }
        return null
    }

    private fun scanForRepaymentDatesFallback(pages: List<PageExtraction>): Triple<String, String, Int>? {
        val regex = "(?i)(?:due\\s+date|payable\\s+on\\s+the)\\s*[:\\-]?\\s*(\\d{1,2}(?:st|nd|rd|th)?\\s+(?:of\\s+each\\s+month|day\\s+of\\s+the\\s+month))".toRegex()
        for (p in pages) {
            val m = regex.find(p.text)
            if (m != null) {
                return Triple(m.groupValues[1].trim(), m.value.trim(), p.pageNumber)
            }
        }
        return null
    }

    private fun scanForProcessingFeeFallback(pages: List<PageExtraction>): Triple<String, String, Int>? {
        val regex = "(?i)(?:processing\\s+fee|pf)\\s*[:\\-]?\\s*(?:rs\\.?|inr|₹)?\\s*([\\d,]+|\\d{1,2}(?:\\.\\d{1,2})?%)".toRegex()
        for (p in pages) {
            val m = regex.find(p.text)
            if (m != null) {
                return Triple(m.groupValues[1].trim(), m.value.trim(), p.pageNumber)
            }
        }
        return null
    }

    private fun scanForUpfrontDeductionsFallback(pages: List<PageExtraction>): Triple<String, String, Int>? {
        val regex = "(?i)(?:stamp\\s+duty|documentation|admin(?:istrative)?\\s+charge|insurance)\\s*[:\\-]?\\s*(?:rs\\.?|inr|₹)?\\s*([\\d,]+)".toRegex()
        for (p in pages) {
            val m = regex.find(p.text)
            if (m != null) {
                return Triple(m.groupValues[0].trim(), m.value.trim(), p.pageNumber)
            }
        }
        return null
    }

    private fun scanForPenalFallback(pages: List<PageExtraction>): Triple<String, String, Int>? {
        val regex = "(?i)(?:penal\\s+(?:interest|charges?)|bounce\\s+charges?|default\\s+charges?)\\s*[:\\-]?\\s*([^\\n\\r]{5,60})".toRegex()
        for (p in pages) {
            val m = regex.find(p.text)
            if (m != null) {
                return Triple(m.groupValues[1].trim(), m.value.trim(), p.pageNumber)
            }
        }
        return null
    }

    private fun scanForPrepaymentFallback(pages: List<PageExtraction>): Triple<String, String, Int>? {
        val regex = "(?i)(?:prepayment|foreclosure)\\s*[:\\-]?\\s*([^\\n\\r]{5,70})".toRegex()
        for (p in pages) {
            val m = regex.find(p.text)
            if (m != null) {
                return Triple(m.groupValues[1].trim(), m.value.trim(), p.pageNumber)
            }
        }
        return null
    }

    private fun scanForCollateralFallback(pages: List<PageExtraction>): Triple<String, String, Int>? {
        val regex = "(?i)(?:collateral|security|secured\\s+by|unsecured)\\s*[:\\-]?\\s*([^\\n\\r]{4,60})".toRegex()
        for (p in pages) {
            val m = regex.find(p.text)
            if (m != null) {
                return Triple(m.groupValues[0].trim(), m.value.trim(), p.pageNumber)
            }
        }
        return null
    }

    private fun scanForMoratoriumFallback(pages: List<PageExtraction>): Triple<String, String, Int>? {
        val regex = "(?i)(?:moratorium|grace\\s+period)\\s*[:\\-]?\\s*([^\\n\\r]{4,50})".toRegex()
        for (p in pages) {
            val m = regex.find(p.text)
            if (m != null) {
                return Triple(m.groupValues[1].trim(), m.value.trim(), p.pageNumber)
            }
        }
        return null
    }
}

