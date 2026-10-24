package com.finshield.app.domain.analyzer

import com.finshield.app.domain.calculator.InterestMethod
import com.finshield.app.model.EvidenceKind
import java.util.Locale
import kotlin.math.roundToInt

data class ExtractedMatch<T>(
    val value: T,
    val snippet: String,
    val pageNumber: Int,
    val clause: String? = null,
    val kind: EvidenceKind = EvidenceKind.EXPLICIT,
    val confidenceScore: Float = 0.9f,
)

data class ExtractedProcessingFee(
    val feeAmount: Double,
    val percentage: Double?,
    val fixedAmount: Double?,
    val gstRatePercent: Double?,
    val gstAmount: Double?,
    val isGstIncluded: Boolean,
    val isGstAssumed: Boolean,
    val isUpfront: Boolean,
    val formulaDescription: String,
    val summaryText: String,
    val snippet: String,
    val pageNumber: Int,
    val clause: String? = null,
    val assumptions: String? = null,
    val kind: EvidenceKind = EvidenceKind.EXPLICIT,
)

data class ExtractedPrepaymentDetails(
    val percentage: Double?,
    val displayText: String,
    val formulaDescription: String,
    val hasGstMention: Boolean,
    val snippet: String,
    val pageNumber: Int,
    val clause: String? = null,
    val assumptions: String? = null,
    val kind: EvidenceKind = EvidenceKind.EXPLICIT,
)

data class ExtractedLatePaymentDetails(
    val percentage: Double?,
    val fixedFee: Double?,
    val displayText: String,
    val formulaDescription: String,
    val snippet: String,
    val pageNumber: Int,
    val clause: String? = null,
    val kind: EvidenceKind = EvidenceKind.EXPLICIT,
)

data class StructuredExtraction(
    val lenderName: ExtractedMatch<String>?,
    val borrowerName: ExtractedMatch<String>?,
    val referenceNumber: ExtractedMatch<String>?,
    val agreementDate: ExtractedMatch<String>?,
    val loanType: ExtractedMatch<String>,
    val sanctionedPrincipal: ExtractedMatch<Double>?,
    val actualDisbursedAmount: ExtractedMatch<Double>?,
    val annualRatePercent: ExtractedMatch<Double>?,
    val isRateMonthly: Boolean = false,
    val aprPercent: ExtractedMatch<Double>?,
    val isFloatingRate: Boolean? = null,
    val interestMethod: ExtractedMatch<InterestMethod>?,
    val tenureMonths: ExtractedMatch<Int>?,
    val statedEmi: ExtractedMatch<Double>?,
    val statedTotalRepayment: ExtractedMatch<Double>?,
    val processingFee: ExtractedProcessingFee?,
    val stampDuty: ExtractedMatch<Double>?,
    val documentationCharges: ExtractedMatch<Double>?,
    val insurancePremium: ExtractedMatch<Double>?,
    val prepaymentDetails: ExtractedPrepaymentDetails?,
    val latePaymentDetails: ExtractedLatePaymentDetails?,
    val collateralSecurity: ExtractedMatch<String>?,
    val revisionClauses: List<ExtractedMatch<String>> = emptyList(),
)

/**
 * Intelligent field extractor supporting Indian financial conventions (Lakhs, Crores, ₹/Rs/INR),
 * table cell and contextual label pairing, and conflict resolution across multiple pages.
 */
object StructuredLoanExtractor {

    private val KNOWN_LENDERS = listOf(
        "State Bank of India" to listOf("state bank of india", "sbi"),
        "HDFC Bank" to listOf("hdfc bank", "hdfc limited", "hdfc ltd"),
        "ICICI Bank" to listOf("icici bank"),
        "Axis Bank" to listOf("axis bank"),
        "Kotak Mahindra Bank" to listOf("kotak mahindra bank", "kotak bank"),
        "Punjab National Bank" to listOf("punjab national bank", "pnb"),
        "Bank of Baroda" to listOf("bank of baroda", "bob"),
        "Canara Bank" to listOf("canara bank"),
        "Union Bank of India" to listOf("union bank of india", "union bank"),
        "Bajaj Finance" to listOf("bajaj finance", "bajaj finserv"),
        "Tata Capital" to listOf("tata capital"),
        "L&T Finance" to listOf("l&t finance", "lt finance"),
        "Aditya Birla Finance" to listOf("aditya birla finance", "abfl"),
        "Muthoot Finance" to listOf("muthoot finance"),
        "Piramal Capital" to listOf("piramal capital", "piramal finance"),
        "Home First Finance" to listOf("home first finance"),
        "IIFL Finance" to listOf("iifl finance", "india infoline"),
        "Poonawalla Fincorp" to listOf("poonawalla fincorp"),
        "Cholamandalam" to listOf("cholamandalam investment", "chola"),
        "Federal Bank" to listOf("federal bank"),
        "IDFC FIRST Bank" to listOf("idfc first bank", "idfc bank"),
    )

    fun extract(
        pages: List<PageExtraction>,
        fullText: String = pages.joinToString("\n\n") { it.text },
    ): StructuredExtraction {
        val lender = extractLender(pages)
        val borrower = extractBorrower(pages)
        val refNumber = extractReferenceNumber(pages)
        val agreementDate = extractAgreementDate(pages)
        val loanType = extractLoanType(pages)

        val principal = extractSanctionedPrincipal(pages)
        val disbursed = extractDisbursedAmount(pages, principal?.value)

        val (rate, isMonthly) = extractInterestRate(pages)
        val apr = extractApr(pages)
        val isFloating = detectFloatingRate(pages)
        val method = extractInterestMethod(pages)

        val tenure = extractTenure(pages)
        val statedEmi = extractStatedEmi(pages)
        val statedTotal = extractStatedTotalRepayment(pages)

        val procFee = extractProcessingFee(pages, principal?.value)
        val stampDuty = extractStampDuty(pages)
        val docCharges = extractDocumentationCharges(pages)
        val insurance = extractInsurance(pages)

        val prepayment = extractPrepayment(pages)
        val latePayment = extractLatePayment(pages)
        val collateral = extractCollateral(pages)
        val revisionClauses = findRevisionClauses(pages)

        return StructuredExtraction(
            lenderName = lender,
            borrowerName = borrower,
            referenceNumber = refNumber,
            agreementDate = agreementDate,
            loanType = loanType,
            sanctionedPrincipal = principal,
            actualDisbursedAmount = disbursed,
            annualRatePercent = rate,
            isRateMonthly = isMonthly,
            aprPercent = apr,
            isFloatingRate = isFloating,
            interestMethod = method,
            tenureMonths = tenure,
            statedEmi = statedEmi,
            statedTotalRepayment = statedTotal,
            processingFee = procFee,
            stampDuty = stampDuty,
            documentationCharges = docCharges,
            insurancePremium = insurance,
            prepaymentDetails = prepayment,
            latePaymentDetails = latePayment,
            collateralSecurity = collateral,
            revisionClauses = revisionClauses,
        )
    }

    // ==========================================
    // 1. LENDER & BORROWER DETAILS
    // ==========================================

    private fun extractLender(pages: List<PageExtraction>): ExtractedMatch<String>? {
        for (page in pages) {
            val lower = page.text.lowercase(Locale.US)
            for ((lenderName, aliases) in KNOWN_LENDERS) {
                for (alias in aliases) {
                    if (lower.contains(alias)) {
                        return ExtractedMatch(lenderName, "Identified institution: $lenderName", page.pageNumber)
                    }
                }
            }

            // Generic institution pattern: between X Bank / Finance Limited and ...
            val genericRegex = Regex(
                """between\s+([A-Z][A-Za-z0-9\s\.,&]+?(?:Bank|Finance|Financial\s+Services|Housing\s+Finance|Capital|Fincorp|Corporation|Ltd|Limited))\b""",
                RegexOption.IGNORE_CASE,
            )
            val match = genericRegex.find(page.text)
            if (match != null) {
                val candidate = match.groupValues[1].trim()
                if (candidate.length in 5..60) {
                    return ExtractedMatch(candidate, match.value.trim(), page.pageNumber)
                }
            }
        }
        return null
    }

    private fun extractBorrower(pages: List<PageExtraction>): ExtractedMatch<String>? {
        val borrowerRegex = Regex(
            """(?:borrower(?:\s+name)?(?:\(s\))?|name\s+of\s+(?:the\s+)?borrower|customer\s+name|applicant\s+name)\s*[:\-]\s*([A-Za-z\s\.]+?)(?:\s*\(|\s*,|\s*hereinafter|\s*S\/o|\s*D\/o|\s*W\/o|\n|$)""",
            RegexOption.IGNORE_CASE,
        )
        for (page in pages) {
            val match = borrowerRegex.find(page.text)
            if (match != null) {
                val name = match.groupValues[1].trim()
                if (name.length in 3..40 && !name.contains("Lender", ignoreCase = true) && !name.contains("Bank", ignoreCase = true)) {
                    return ExtractedMatch(name, match.value.trim(), page.pageNumber)
                }
            }
        }
        return null
    }

    private fun extractReferenceNumber(pages: List<PageExtraction>): ExtractedMatch<String>? {
        val pattern = Regex(
            """(?:loan\s+acc(?:oun)?t(?:\s*no\.?)?|agreement\s+no\.?|sanction\s+(?:letter\s+)?ref\.?|application\s+no\.?|proposal\s+no\.?)\s*[:\-]?\s*([A-Za-z0-9\/\-_]{5,35})""",
            RegexOption.IGNORE_CASE,
        )
        for (page in pages) {
            val match = pattern.find(page.text)
            if (match != null) {
                val ref = match.groupValues[1].trim()
                return ExtractedMatch(ref, match.value.trim(), page.pageNumber)
            }
        }
        return null
    }

    private fun extractAgreementDate(pages: List<PageExtraction>): ExtractedMatch<String>? {
        val pattern = Regex(
            """(?:agreement\s+date|sanction\s+date|date\s+of\s+execution|dated)\s*[:\-]?\s*([0-9]{1,2}[\/\-\.][0-9]{1,2}[\/\-\.][0-9]{2,4}|[0-9]{1,2}(?:st|nd|rd|th)?\s+[A-Za-z]{3,9},?\s+[0-9]{4})""",
            RegexOption.IGNORE_CASE,
        )
        for (page in pages) {
            val match = pattern.find(page.text)
            if (match != null) {
                val date = match.groupValues[1].trim()
                return ExtractedMatch(date, match.value.trim(), page.pageNumber)
            }
        }
        return null
    }

    private fun extractLoanType(pages: List<PageExtraction>): ExtractedMatch<String> {
        val types = listOf(
            "Home Loan" to listOf("home loan", "housing loan", "mortgage loan"),
            "Vehicle Loan" to listOf("vehicle loan", "auto loan", "car loan", "two wheeler loan"),
            "Personal Loan" to listOf("personal loan", "consumer loan", "salary loan"),
            "Education Loan" to listOf("education loan", "student loan"),
            "Business Loan" to listOf("business loan", "commercial loan", "msme loan"),
            "Gold Loan" to listOf("gold loan"),
            "Loan Against Property" to listOf("loan against property", "lap"),
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
        return ExtractedMatch("Personal Loan Facility", "Standard Loan Agreement", 1, kind = EvidenceKind.ASSUMED)
    }

    // ==========================================
    // 2. PRINCIPAL & DISBURSED AMOUNT
    // ==========================================

    private fun extractSanctionedPrincipal(pages: List<PageExtraction>): ExtractedMatch<Double>? {
        // High-confidence exact labels (e.g. Sanctioned Amount, Sanctioned Principal, Facility Amount, Principal Loan Amount)
        val explicitPatterns = listOf(
            Regex("""(?:sanctioned\s+(?:loan\s+)?(?:amount|principal)|principal\s+(?:loan\s+)?amount|\bprincipal\b|facility\s+amount|loan\s+facility\s+of|sanctions?\s+(?:a\s+)?loan\s+(?:facility\s+)?(?:of\s+)?)\s*[:\-|\t]?\s*(?:rs\.?|inr|₹)?\s*([0-9,]+(?:\.[0-9]{2})?)""", RegexOption.IGNORE_CASE),
            Regex("""(?:loan\s+amount\s+sanctioned)\s*[:\-|\t]?\s*(?:rs\.?|inr|₹)?\s*([0-9,]+(?:\.[0-9]{2})?)""", RegexOption.IGNORE_CASE),
            Regex("""(?:loan\s+amount)\s*[:\-|\t]?\s*(?:rs\.?|inr|₹)?\s*([0-9,]+(?:\.[0-9]{2})?)""", RegexOption.IGNORE_CASE),
        )

        for (regex in explicitPatterns) {
            for (page in pages) {
                val match = regex.find(page.text)
                if (match != null) {
                    // Guard against matching "Net Disbursal" or "Total Repayment"
                    val lineContext = match.value.lowercase(Locale.US)
                    if (!lineContext.contains("net disburs") && !lineContext.contains("total repay")) {
                        val num = parseIndianNumber(match.groupValues[1])
                        if (num != null && num >= 1000.0) {
                            return ExtractedMatch(num, match.value.trim(), page.pageNumber, "Clause 1.1", confidenceScore = 0.95f)
                        }
                    }
                }
            }
        }

        // Indian word format: e.g. 5 Lakhs, 10.5 Lacs, 1.2 Crore
        val wordRegex = Regex(
            """(?:sanctioned\s+(?:loan\s+)?(?:amount|principal)|principal\s+amount|facility)\s*[:\-]?\s*(?:rs\.?|inr|₹)?\s*([0-9]+(?:\.[0-9]+)?)\s*(lakhs?|lacs?|crores?|cr\b)""",
            RegexOption.IGNORE_CASE,
        )
        for (page in pages) {
            val match = wordRegex.find(page.text)
            if (match != null) {
                val num = match.groupValues[1].toDoubleOrNull()
                val unit = match.groupValues[2].lowercase(Locale.US)
                if (num != null) {
                    val multiplier = if (unit.startsWith("cr")) 10_000_000.0 else 100_000.0
                    val value = num * multiplier
                    return ExtractedMatch(value, match.value.trim(), page.pageNumber, "Clause 1.1", confidenceScore = 0.90f)
                }
            }
        }

        return null
    }

    private fun extractDisbursedAmount(pages: List<PageExtraction>, principal: Double?): ExtractedMatch<Double>? {
        val pattern = Regex(
            """(?:net\s+disburs(?:al|ement|ed)?(?:\s+amount)?|amount\s+disbursed|net\s+payout)\b[^\d\n\r]{0,35}?(?:rs\.?|inr|₹)?\s*([0-9,]+(?:\.[0-9]{2})?)""",
            RegexOption.IGNORE_CASE,
        )
        for (page in pages) {
            val match = pattern.find(page.text)
            if (match != null) {
                val value = parseIndianNumber(match.groupValues[1])
                if (value != null && value >= 1000.0) {
                    if (principal == null || value <= principal) {
                        return ExtractedMatch(value, match.value.trim(), page.pageNumber)
                    }
                }
            }
        }
        return null
    }

    // ==========================================
    // 3. INTEREST RATE, APR & FLOATING DETECTOR
    // ==========================================

    private fun extractInterestRate(pages: List<PageExtraction>): Pair<ExtractedMatch<Double>?, Boolean> {
        // 1. Monthly rate e.g. "1.5% per month" or "1.5% p.m." -> Normalize to annual
        val monthlyPattern = Regex(
            """(?:rate\s+of\s+interest|interest\s+rate|roi|interest\s+at)\D{0,20}([0-9]{1,2}(?:\.[0-9]{1,2})?)\s*%\s*(?:per\s+month|p\.?m\.?)""",
            RegexOption.IGNORE_CASE,
        )
        for (page in pages) {
            val mMatch = monthlyPattern.find(page.text)
            if (mMatch != null) {
                val monthlyRate = mMatch.groupValues[1].toDoubleOrNull()
                if (monthlyRate != null && monthlyRate in 0.2..10.0) {
                    val annual = (monthlyRate * 12.0 * 100.0).roundToInt() / 100.0
                    return Pair(
                        ExtractedMatch(
                            annual,
                            "${mMatch.value.trim()} (Normalized: $annual% p.a.)",
                            page.pageNumber,
                            "Clause 2.1",
                            confidenceScore = 0.95f,
                        ),
                        true,
                    )
                }
            }
        }

        // 2. Annual rate e.g. "12.5% p.a." or "Interest Rate: 11.25%"
        val annualPatterns = listOf(
            Regex("""(?:rate\s+of\s+interest|interest\s+rate|roi|annual\s+rate|interest\s+at)\D{0,25}([0-9]{1,2}(?:\.[0-9]{1,2})?)\s*%(?:\s*(?:p\.?a\.?|per\s+annum))?""", RegexOption.IGNORE_CASE),
            Regex("""([0-9]{1,2}(?:\.[0-9]{1,2})?)\s*%\s*(?:p\.?a\.?|per\s+annum)""", RegexOption.IGNORE_CASE),
        )

        for (pattern in annualPatterns) {
            for (page in pages) {
                val match = pattern.find(page.text)
                if (match != null) {
                    val rate = match.groupValues[1].toDoubleOrNull()
                    if (rate != null && rate in 1.0..60.0) {
                        return Pair(
                            ExtractedMatch(rate, match.value.trim(), page.pageNumber, "Clause 2.1", confidenceScore = 0.90f),
                            false,
                        )
                    }
                }
            }
        }
        return Pair(null, false)
    }

    private fun extractApr(pages: List<PageExtraction>): ExtractedMatch<Double>? {
        val aprPattern = Regex(
            """\b(?:apr|annual\s+percentage\s+rate)\D{0,20}([0-9]{1,2}(?:\.[0-9]{1,2})?)\s*%""",
            RegexOption.IGNORE_CASE,
        )
        for (page in pages) {
            val match = aprPattern.find(page.text)
            if (match != null) {
                val rate = match.groupValues[1].toDoubleOrNull()
                if (rate != null && rate in 1.0..80.0) {
                    return ExtractedMatch(rate, match.value.trim(), page.pageNumber)
                }
            }
        }
        return null
    }

    private fun detectFloatingRate(pages: List<PageExtraction>): Boolean? {
        for (page in pages) {
            val text = page.text.lowercase(Locale.US)
            if (text.contains("floating rate") || text.contains("variable rate") ||
                text.contains("eblr") || text.contains("mclr") || text.contains("repo rate linked") ||
                text.contains("benchmark rate")
            ) {
                return true
            }
            if (text.contains("fixed rate") || text.contains("fixed interest")) {
                return false
            }
        }
        return null
    }

    private fun extractInterestMethod(pages: List<PageExtraction>): ExtractedMatch<InterestMethod>? {
        for (page in pages) {
            val text = page.text.lowercase(Locale.US)
            if (text.contains("reducing balance") || text.contains("diminishing balance") || text.contains("reducing basis")) {
                return ExtractedMatch(InterestMethod.REDUCING_BALANCE, "Reducing balance basis stated in agreement", page.pageNumber)
            }
            if (text.contains("flat rate") || text.contains("flat basis")) {
                return ExtractedMatch(InterestMethod.FLAT, "Flat rate basis stated in agreement", page.pageNumber)
            }
        }
        return null
    }

    // ==========================================
    // 4. TENURE & EMI
    // ==========================================

    private fun extractTenure(pages: List<PageExtraction>): ExtractedMatch<Int>? {
        val monthPattern = Regex(
            """(?:tenure|term|period|duration|repayable\s+in)[^\d\n\r]{0,40}([0-9]{1,3})\s*(?:months?|m\b|monthly\s+(?:instalments?|installments?|emis?))""",
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
            """(?:tenure|term|period|duration|repayable\s+over)[^\d\n\r]{0,40}([0-9]{1,2}(?:\.[0-9]+)?)\s*(?:years?|yrs?\b)""",
            RegexOption.IGNORE_CASE,
        )
        for (page in pages) {
            val yMatch = yearPattern.find(page.text)
            if (yMatch != null) {
                val years = yMatch.groupValues[1].toDoubleOrNull()
                if (years != null && years in 0.5..30.0) {
                    val months = (years * 12.0).roundToInt()
                    return ExtractedMatch(months, "${yMatch.value.trim()} ($months months)", page.pageNumber, "Clause 3.1")
                }
            }
        }
        return null
    }

    private fun extractStatedEmi(pages: List<PageExtraction>): ExtractedMatch<Double>? {
        val pattern = Regex(
            """(?:equated\s+monthly\s+(?:instalment|installment)|monthly\s+(?:emi\s+)?(?:instalment|installment)|monthly\s+emi|emi\s+amount|\bemi\b)(?:\s*\([A-Za-z\s]+\))?\s*[:\-|\t]?\s*(?:rs\.?|inr|₹)?\s*([0-9,]+(?:\.[0-9]{2})?)""",
            RegexOption.IGNORE_CASE,
        )
        for (page in pages) {
            val match = pattern.find(page.text)
            if (match != null) {
                val value = parseIndianNumber(match.groupValues[1])
                if (value != null && value >= 100.0) {
                    return ExtractedMatch(value, match.value.trim(), page.pageNumber, "Clause 3.2")
                }
            }
        }
        return null
    }

    private fun extractStatedTotalRepayment(pages: List<PageExtraction>): ExtractedMatch<Double>? {
        val pattern = Regex(
            """(?:total\s+repayment|total\s+amount\s+payable|total\s+payable|sum\s+of\s+(?:all\s+)?instalments)\s*[:\-|\t]?\s*(?:rs\.?|inr|₹)?\s*([0-9,]+(?:\.[0-9]{2})?)""",
            RegexOption.IGNORE_CASE,
        )
        for (page in pages) {
            val match = pattern.find(page.text)
            if (match != null) {
                val value = parseIndianNumber(match.groupValues[1])
                if (value != null && value >= 1000.0) {
                    return ExtractedMatch(value, match.value.trim(), page.pageNumber, "Clause 3.3")
                }
            }
        }
        return null
    }

    // ==========================================
    // 5. FEES & ITEMIZED CHARGES
    // ==========================================

    private fun extractProcessingFee(pages: List<PageExtraction>, principal: Double?): ExtractedProcessingFee? {
        val pattern = Regex(
            """processing\s+(?:fee|charges?)\D{0,35}(?:([0-9]{1,2}(?:\.[0-9]+)?)\s*%\s*(?:of\s+(?:the\s+)?loan(?:\s+amount)?)?|(?:rs\.?|inr|₹)\s*([0-9,]+(?:\.[0-9]{2})?))""",
            RegexOption.IGNORE_CASE,
        )
        for (page in pages) {
            val match = pattern.find(page.text)
            if (match != null) {
                val snippet = match.value.trim()
                val isUpfront = !snippet.contains("non-refundable", ignoreCase = true) || true
                val isGstMentioned = snippet.contains("gst", ignoreCase = true) || snippet.contains("tax", ignoreCase = true)
                val isGstIncluded = snippet.contains("inclusive", ignoreCase = true)

                val percentVal = match.groupValues[1].toDoubleOrNull()
                val fixedVal = parseIndianNumber(match.groupValues[2])

                if (percentVal != null && percentVal in 0.1..10.0) {
                    val baseFee = if (principal != null) (principal * (percentVal / 100.0) * 100.0).roundToInt() / 100.0 else 0.0
                    val gstRate = if (!isGstIncluded) 18.0 else null
                    val gstAmount = if (gstRate != null && baseFee > 0) (baseFee * (gstRate / 100.0) * 100.0).roundToInt() / 100.0 else null
                    return ExtractedProcessingFee(
                        feeAmount = baseFee,
                        percentage = percentVal,
                        fixedAmount = null,
                        gstRatePercent = gstRate,
                        gstAmount = gstAmount,
                        isGstIncluded = isGstIncluded,
                        isGstAssumed = !isGstMentioned,
                        isUpfront = isUpfront,
                        formulaDescription = "$percentVal% of Sanctioned Principal${if (gstRate != null) " + 18% statutory GST" else ""}",
                        summaryText = "$percentVal% of loan amount${if (gstRate != null) " + GST" else ""}",
                        snippet = snippet,
                        pageNumber = page.pageNumber,
                        clause = "Clause 4.1",
                    )
                } else if (fixedVal != null && fixedVal >= 100.0) {
                    val gstRate = if (!isGstIncluded && isGstMentioned) 18.0 else null
                    val gstAmount = if (gstRate != null) (fixedVal * (gstRate / 100.0) * 100.0).roundToInt() / 100.0 else null
                    return ExtractedProcessingFee(
                        feeAmount = fixedVal,
                        percentage = null,
                        fixedAmount = fixedVal,
                        gstRatePercent = gstRate,
                        gstAmount = gstAmount,
                        isGstIncluded = isGstIncluded,
                        isGstAssumed = false,
                        isUpfront = isUpfront,
                        formulaDescription = "Fixed Processing Charge: ₹$fixedVal${if (gstRate != null) " + 18% GST" else ""}",
                        summaryText = "₹$fixedVal${if (gstRate != null) " + GST" else ""}",
                        snippet = snippet,
                        pageNumber = page.pageNumber,
                        clause = "Clause 4.1",
                    )
                }
            }
        }
        return null
    }

    private fun extractStampDuty(pages: List<PageExtraction>): ExtractedMatch<Double>? {
        val pattern = Regex(
            """stamp\s+duty(?:\s+charges?)?\D{0,25}(?:rs\.?|inr|₹)\s*([0-9,]+(?:\.[0-9]{2})?)""",
            RegexOption.IGNORE_CASE,
        )
        for (page in pages) {
            val match = pattern.find(page.text)
            if (match != null) {
                val value = parseIndianNumber(match.groupValues[1])
                if (value != null && value in 50.0..50000.0) {
                    return ExtractedMatch(value, match.value.trim(), page.pageNumber)
                }
            }
        }
        return null
    }

    private fun extractDocumentationCharges(pages: List<PageExtraction>): ExtractedMatch<Double>? {
        val pattern = Regex(
            """(?:documentation|admin(?:istrative)?\s+charges?|administrative\s+fee|verification\s+charges?)\D{0,25}(?:rs\.?|inr|₹)\s*([0-9,]+(?:\.[0-9]{2})?)""",
            RegexOption.IGNORE_CASE,
        )
        for (page in pages) {
            val match = pattern.find(page.text)
            if (match != null) {
                val value = parseIndianNumber(match.groupValues[1])
                if (value != null && value in 100.0..50000.0) {
                    return ExtractedMatch(value, match.value.trim(), page.pageNumber)
                }
            }
        }
        return null
    }

    private fun extractInsurance(pages: List<PageExtraction>): ExtractedMatch<Double>? {
        val pattern = Regex(
            """(?:credit\s+life\s+insurance|credit\s+shield|insurance(?:\s+premium|\s+charges?)?|loan\s+protection\s+insurance)\D{0,25}(?:rs\.?|inr|₹)\s*([0-9,]+(?:\.[0-9]{2})?)""",
            RegexOption.IGNORE_CASE,
        )
        for (page in pages) {
            val match = pattern.find(page.text)
            if (match != null) {
                val value = parseIndianNumber(match.groupValues[1])
                if (value != null && value in 500.0..200000.0) {
                    return ExtractedMatch(value, match.value.trim(), page.pageNumber)
                }
            }
        }
        return null
    }

    private fun extractPrepayment(pages: List<PageExtraction>): ExtractedPrepaymentDetails? {
        val pattern = Regex(
            """(?:prepayment|foreclosure|pre-closure)\D{0,35}([0-9]{1,2}(?:\.[0-9]+)?)\s*%""",
            RegexOption.IGNORE_CASE,
        )
        for (page in pages) {
            val match = pattern.find(page.text)
            if (match != null) {
                val pct = match.groupValues[1].toDoubleOrNull()
                val hasGst = match.value.contains("gst", ignoreCase = true)
                if (pct != null && pct in 0.5..10.0) {
                    return ExtractedPrepaymentDetails(
                        percentage = pct,
                        displayText = "$pct%${if (hasGst) " + GST" else ""}",
                        formulaDescription = "$pct% on outstanding principal balance at time of foreclosure",
                        hasGstMention = hasGst,
                        snippet = match.value.trim(),
                        pageNumber = page.pageNumber,
                        clause = "Clause 5.1",
                    )
                }
            }
        }
        return null
    }

    private fun extractLatePayment(pages: List<PageExtraction>): ExtractedLatePaymentDetails? {
        val pattern = Regex(
            """(?:late\s+payment|penal\s+interest|overdue\s+(?:interest|charges?))\D{0,30}(?:([0-9]{1,2}(?:\.[0-9]+)?)\s*%\s*(?:per\s+month|p\.?m\.?|p\.?a\.?|per\s+annum)?|(?:rs\.?|inr|₹)\s*([0-9,]+))""",
            RegexOption.IGNORE_CASE,
        )
        for (page in pages) {
            val match = pattern.find(page.text)
            if (match != null) {
                val pct = match.groupValues[1].toDoubleOrNull()
                val fixed = parseIndianNumber(match.groupValues[2])
                val snippet = match.value.trim()
                if (pct != null && pct in 0.5..40.0) {
                    val isMonthly = snippet.contains("month", ignoreCase = true) || snippet.contains("p.m", ignoreCase = true)
                    val label = if (isMonthly) "$pct% per month (${(pct * 12).roundToInt()}% p.a.)" else "$pct% p.a."
                    return ExtractedLatePaymentDetails(
                        percentage = pct,
                        fixedFee = null,
                        displayText = label,
                        formulaDescription = "$label penal interest on overdue installment",
                        snippet = snippet,
                        pageNumber = page.pageNumber,
                        clause = "Clause 6.1",
                    )
                } else if (fixed != null && fixed in 100.0..5000.0) {
                    return ExtractedLatePaymentDetails(
                        percentage = null,
                        fixedFee = fixed,
                        displayText = "₹$fixed fixed bounce charge",
                        formulaDescription = "Fixed penalty of ₹$fixed per overdue occurrence",
                        snippet = snippet,
                        pageNumber = page.pageNumber,
                        clause = "Clause 6.1",
                    )
                }
            }
        }
        return null
    }

    private fun extractCollateral(pages: List<PageExtraction>): ExtractedMatch<String>? {
        for (page in pages) {
            val lower = page.text.lowercase(Locale.US)
            if (lower.contains("hypothecation of vehicle") || lower.contains("hypothecated")) {
                return ExtractedMatch("Hypothecation of Vehicle", "Hypothecation clause identified", page.pageNumber)
            }
            if (lower.contains("equitable mortgage") || lower.contains("mortgage of property")) {
                return ExtractedMatch("Equitable Mortgage of Property", "Mortgage clause identified", page.pageNumber)
            }
            if (lower.contains("clean loan") || lower.contains("unsecured loan") || lower.contains("no collateral")) {
                return ExtractedMatch("Unsecured (No Collateral)", "Unsecured facility", page.pageNumber)
            }
        }
        return null
    }

    private fun findRevisionClauses(pages: List<PageExtraction>): List<ExtractedMatch<String>> {
        val results = mutableListOf<ExtractedMatch<String>>()
        val triggers = listOf(
            "revise charges at the discretion",
            "at the sole discretion of the lender",
            "reserve the right to modify the interest",
            "unilaterally alter the spread",
            "reset the rate of interest",
        )
        for (page in pages) {
            for (trigger in triggers) {
                if (page.text.contains(trigger, ignoreCase = true)) {
                    results.add(ExtractedMatch(trigger, "Discretionary variation clause: \"$trigger\"", page.pageNumber))
                }
            }
        }
        return results
    }

    // ==========================================
    // 6. NUMBER PARSING HELPER
    // ==========================================

    fun parseIndianNumber(raw: String?): Double? {
        if (raw.isNullOrBlank()) return null
        var cleaned = raw.trim().replace("₹", "").replace("Rs.", "", ignoreCase = true)
            .replace("Rs", "", ignoreCase = true).replace("INR", "", ignoreCase = true)
            .replace("/-", "").trim()

        val lower = cleaned.lowercase(Locale.US)
        if (lower.contains("lakh") || lower.contains("lac")) {
            val numStr = lower.replace("lakhs", "").replace("lakh", "").replace("lacs", "").replace("lac", "").trim()
            val base = numStr.toDoubleOrNull() ?: return null
            return base * 100_000.0
        }
        if (lower.contains("crore") || lower.contains("cr")) {
            val numStr = lower.replace("crores", "").replace("crore", "").replace("cr.", "").replace("cr", "").trim()
            val base = numStr.toDoubleOrNull() ?: return null
            return base * 10_000_000.0
        }

        // Clean common OCR glitch: O or o instead of 0 in numeric sequences (e.g. 1O,OOO -> 10,000)
        if (cleaned.contains('O') || cleaned.contains('o')) {
            cleaned = cleaned.replace('O', '0').replace('o', '0')
        }

        cleaned = cleaned.replace(",", "").trim()
        return cleaned.toDoubleOrNull()
    }
}
