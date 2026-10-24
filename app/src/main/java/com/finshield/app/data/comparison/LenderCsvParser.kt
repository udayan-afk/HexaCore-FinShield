package com.finshield.app.data.comparison

import android.content.Context
import com.finshield.app.model.LenderClassification
import com.finshield.app.model.LenderRecord
import com.finshield.app.model.LoanCategory
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.Locale

/**
 * Robust RFC 4180 compliant CSV parser for FinShield's lender research catalogue.
 * Correctly parses quoted text fields containing commas, escaped quotes and special characters.
 */
object LenderCsvParser {

    const val DATASET_VERSION = "2026.10-research"
    const val DEFAULT_AS_OF = "14 Oct 2026"

    /**
     * Parses the CSV from an [InputStream].
     */
    fun parse(inputStream: InputStream): List<LenderRecord> {
        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
        val rawRows = parseCsvTokens(reader)
        if (rawRows.isEmpty()) return emptyList()

        // First row is the header; verify and skip
        val dataRows = rawRows.drop(1)
        return dataRows.mapNotNull { row ->
            if (row.all { it.isBlank() }) null
            else parseRowToRecord(row)
        }
    }

    /**
     * Loads CSV from Android assets or falls back to ClassLoader resource (for unit tests).
     */
    fun loadFromAssetsOrClasspath(context: Context?, assetName: String = "lenders.csv"): List<LenderRecord> {
        val stream = try {
            context?.assets?.open(assetName)
        } catch (_: Exception) {
            null
        } ?: javaClass.classLoader?.getResourceAsStream(assetName)
        ?: javaClass.classLoader?.getResourceAsStream("assets/$assetName")
        ?: javaClass.getResourceAsStream("/$assetName")

        return if (stream != null) {
            stream.use { parse(it) }
        } else {
            emptyList()
        }
    }

    /**
     * Parses a single row array of fields into a [LenderRecord].
     * Headers:
     * 0: Lender
     * 1: Lender Type
     * 2: Published Interest Rate
     * 3: Maximum Loan Amount
     * 4: Maximum Tenure (Months)
     * 5: Published Processing Fee
     * 6: Eligibility / Verification Notes
     * 7: Source URL
     * 8: Source As-Of
     * 9: Dataset Status
     */
    fun parseRowToRecord(fields: List<String>): LenderRecord? {
        if (fields.size < 6) return null

        val lenderName = fields.getOrNull(0)?.trim() ?: return null
        if (lenderName.isBlank()) return null

        val rawLenderType = fields.getOrNull(1)?.trim().orEmpty()
        val publishedRate = fields.getOrNull(2)?.trim().orEmpty()
        val rawMaxAmount = fields.getOrNull(3)?.trim().orEmpty()
        val rawMaxTenure = fields.getOrNull(4)?.trim().orEmpty()
        val rawFee = fields.getOrNull(5)?.trim().orEmpty()
        val notes = fields.getOrNull(6)?.trim().orEmpty()
        val sourceUrl = fields.getOrNull(7)?.trim().orEmpty()
        val sourceAsOf = fields.getOrNull(8)?.trim().orEmpty()
        val datasetStatus = fields.getOrNull(9)?.trim().takeIf { !it.isNullOrBlank() }
            ?: "Indicative — verify current offer before use"

        val classification = LenderClassification.fromCsv(rawLenderType)
        val id = generateLenderId(lenderName)

        val isMonthlyRate = publishedRate.contains("per month", ignoreCase = true) ||
            publishedRate.contains("/month", ignoreCase = true) ||
            publishedRate.contains("p.m.", ignoreCase = true)

        val monthlyRatePercent = if (isMonthlyRate) {
            Regex("""(\d+(?:\.\d+)?)\s*%""").find(publishedRate)?.groupValues?.getOrNull(1)?.toDoubleOrNull()
        } else null

        // Parse starting annual numeric rate only if NOT a monthly rate and explicit starting rate is present
        val (minAnnualRate, maxAnnualRate, isComparable) = parseAnnualRate(publishedRate, isMonthlyRate)

        val parsedMaxAmount = parseIndianCurrencyAmount(rawMaxAmount)
        val maxTenureMonths = rawMaxTenure.toIntOrNull() ?: 60

        // Parse fee estimates
        val (feePercent, feeFixed) = parseProcessingFee(rawFee)

        return LenderRecord(
            id = id,
            lenderName = lenderName,
            lenderType = classification,
            rawLenderType = rawLenderType,
            loanCategory = LoanCategory.PERSONAL,
            publishedInterestRate = publishedRate,
            minAnnualRate = minAnnualRate,
            maxAnnualRate = maxAnnualRate,
            isMonthlyRate = isMonthlyRate,
            monthlyRatePercent = monthlyRatePercent,
            isComparableStartingRate = isComparable,
            rawMaxLoanAmount = rawMaxAmount,
            parsedMaxLoanAmount = parsedMaxAmount,
            maxTenureMonths = maxTenureMonths,
            publishedProcessingFee = rawFee,
            feePercentEstimate = feePercent,
            fixedFeeEstimate = feeFixed,
            eligibilityNotes = notes,
            sourceUrl = sourceUrl,
            sourceAsOf = sourceAsOf,
            datasetStatus = datasetStatus,
            datasetVersion = DATASET_VERSION,
        )
    }

    /**
     * Generates a stable URL-safe identifier for each lender.
     */
    private fun generateLenderId(name: String): String {
        return name.lowercase(Locale.US)
            .replace("&", "and")
            .replace("[^a-z0-9]+".toRegex(), "-")
            .trim('-')
    }

    /**
     * Parses annual rates without guessing or converting monthly rates.
     */
    private fun parseAnnualRate(
        publishedRate: String,
        isMonthlyRate: Boolean,
    ): Triple<Double?, Double?, Boolean> {
        if (isMonthlyRate) {
            // Never convert monthly rate to annual rate without verified APR from lender
            return Triple(null, null, false)
        }

        // If rate is "Up to 36% p.a." - starting rate is unspecified/ambiguous
        if (publishedRate.startsWith("Up to", ignoreCase = true)) {
            val max = Regex("""(\d+(?:\.\d+)?)\s*%""").find(publishedRate)?.groupValues?.getOrNull(1)?.toDoubleOrNull()
            return Triple(null, max, false)
        }

        // Range pattern like "9.99%-16.50% p.a." or "11.10%-16.15%"
        val rangeMatch = Regex("""(\d+(?:\.\d+)?)\s*%\s*-\s*(\d+(?:\.\d+)?)\s*%""").find(publishedRate)
        if (rangeMatch != null) {
            val min = rangeMatch.groupValues[1].toDoubleOrNull()
            val max = rangeMatch.groupValues[2].toDoubleOrNull()
            return Triple(min, max, min != null)
        }

        // Onwards pattern like "9.99% onwards" or "18% onwards"
        val singleMatch = Regex("""(\d+(?:\.\d+)?)\s*%""").find(publishedRate)
        if (singleMatch != null) {
            val min = singleMatch.groupValues[1].toDoubleOrNull()
            return Triple(min, null, min != null)
        }

        return Triple(null, null, false)
    }

    /**
     * Parses Indian currency expressions into numerical values:
     * "₹40 lakh" -> 4,000,000
     * "₹1 crore" -> 10,000,000
     * "₹4.80 lakh" -> 480,000
     */
    fun parseIndianCurrencyAmount(raw: String): Double? {
        val clean = raw.replace("₹", "").replace(",", "").trim()
        val lower = clean.lowercase(Locale.US)

        return when {
            lower.contains("crore") || lower.contains("cr") -> {
                val num = Regex("""(\d+(?:\.\d+)?)""").find(lower)?.groupValues?.getOrNull(1)?.toDoubleOrNull()
                num?.times(10_000_000.0)
            }
            lower.contains("lakh") || lower.contains("lac") -> {
                val num = Regex("""(\d+(?:\.\d+)?)""").find(lower)?.groupValues?.getOrNull(1)?.toDoubleOrNull()
                num?.times(100_000.0)
            }
            else -> {
                Regex("""(\d+(?:\.\d+)?)""").find(lower)?.groupValues?.getOrNull(1)?.toDoubleOrNull()
            }
        }
    }

    /**
     * Extracts an indicative percentage or fixed fee from descriptions like:
     * "Up to 2% + applicable taxes" -> (2.0%, null)
     * "Up to ₹6,500 + GST" -> (null, 6500.0)
     * "Zero on listing; verify current offer" -> (0.0%, 0.0)
     */
    fun parseProcessingFee(raw: String): Pair<Double?, Double?> {
        val lower = raw.lowercase(Locale.US)
        if (lower.contains("zero") || lower.contains("nil")) {
            return Pair(0.0, 0.0)
        }

        // Check for fixed rupee fee first
        val rupeeMatch = Regex("""₹\s*(\d+(?:,\d+)*(?:\.\d+)?)""").find(raw)
        val fixedFee = rupeeMatch?.groupValues?.getOrNull(1)?.replace(",", "")?.toDoubleOrNull()

        // Check for percentage
        val percentMatch = Regex("""(\d+(?:\.\d+)?)\s*%""").find(raw)
        val feePercent = percentMatch?.groupValues?.getOrNull(1)?.toDoubleOrNull()

        return Pair(feePercent, fixedFee)
    }

    /**
     * RFC 4180 character-by-character parser handling quotes, internal commas, and line breaks.
     */
    fun parseCsvTokens(reader: BufferedReader): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val currentRow = mutableListOf<String>()
        val currentField = StringBuilder()
        var inQuotes = false

        var prevChar: Char? = null

        var r = reader.read()
        // Strip BOM if present
        if (r == 0xFEFF) {
            r = reader.read()
        }

        while (r != -1) {
            val c = r.toChar()

            when {
                c == '"' -> {
                    if (inQuotes) {
                        // Lookahead for escaped quote
                        reader.mark(1)
                        val next = reader.read()
                        if (next != -1 && next.toChar() == '"') {
                            currentField.append('"')
                        } else {
                            reader.reset()
                            inQuotes = false
                        }
                    } else {
                        inQuotes = true
                    }
                }
                c == ',' && !inQuotes -> {
                    currentRow.add(currentField.toString())
                    currentField.setLength(0)
                }
                (c == '\n' || c == '\r') && !inQuotes -> {
                    if (c == '\r') {
                        reader.mark(1)
                        val next = reader.read()
                        if (next != -1 && next.toChar() != '\n') {
                            reader.reset()
                        }
                    }
                    currentRow.add(currentField.toString())
                    currentField.setLength(0)
                    if (currentRow.any { it.isNotBlank() }) {
                        rows.add(currentRow.toList())
                    }
                    currentRow.clear()
                }
                else -> {
                    currentField.append(c)
                }
            }

            prevChar = c
            r = reader.read()
        }

        if (currentField.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(currentField.toString())
            if (currentRow.any { it.isNotBlank() }) {
                rows.add(currentRow.toList())
            }
        }

        return rows
    }
}
