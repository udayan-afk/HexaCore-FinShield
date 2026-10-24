package com.finshield.app.data.comparison

import android.content.Context
import com.finshield.app.domain.calculator.InterestMethod
import com.finshield.app.model.LenderClassification
import com.finshield.app.model.LoanCategory
import com.finshield.app.model.LoanOffer
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.Locale

/**
 * Data model for records from loan_directory.csv.
 */
data class LoanDirectoryRecord(
    val bank: String,
    val loanType: String,
    val shortDescription: String,
    val securityType: String,
    val typicalTenure: String,
    val eligibilitySummary: String,
    val commonDocuments: String,
    val minAnnualRate: Double?,
    val maxAnnualRate: Double?,
    val processingFee: String,
    val prepaymentForeclosure: String,
    val availabilityStatus: String,
    val officialProductUrl: String,
    val ratesChargesUrl: String,
    val ctaLabel: String,
    val lastChecked: String,
    val appDisclaimer: String,
) {
    fun toLoanOffer(): LoanOffer {
        val category = LoanCategory.fromLabel(loanType)
        val classification = LenderClassification.fromBankName(bank)
        val id = LoanDirectoryParser.generateOfferId(bank, loanType)

        val publishedRate = when {
            minAnnualRate != null && maxAnnualRate != null -> "$minAnnualRate% - $maxAnnualRate% p.a."
            minAnnualRate != null -> "$minAnnualRate% onwards"
            else -> "Rate not listed — check lender"
        }

        return LoanOffer(
            id = id,
            lenderName = bank,
            offerName = "$bank $loanType",
            category = category,
            annualRatePercent = minAnnualRate ?: 0.0,
            interestMethod = InterestMethod.REDUCING_BALANCE,
            processingFeeFixed = 0.0,
            processingFeePercent = 0.0,
            hasGst = true,
            minAmount = 10_000.0,
            maxAmount = 100_000_000.0,
            minTenureMonths = 12,
            maxTenureMonths = 360,
            eligibilityNotes = eligibilitySummary,
            officialUrl = officialProductUrl.ifBlank { ratesChargesUrl }.takeIf { !it.isNullOrBlank() },
            isVerifiedSource = true,
            lenderClassification = classification,
            publishedInterestRate = publishedRate,
            isMonthlyRate = false,
            monthlyRatePercent = null,
            isComparableStartingRate = minAnnualRate != null,
            rawMaxLoanAmount = "",
            sourceAsOf = lastChecked,
            datasetStatus = availabilityStatus,
            datasetVersion = "2026.10-directory",
            publishedProcessingFee = processingFee,
            shortDescription = shortDescription,
            securityType = securityType,
            typicalTenure = typicalTenure,
            commonDocuments = commonDocuments,
            prepaymentForeclosure = prepaymentForeclosure,
            availabilityStatus = availabilityStatus,
            ratesChargesUrl = ratesChargesUrl.takeIf { it.isNotBlank() },
            ctaLabel = ctaLabel.ifBlank { "Visit bank website" },
            lastChecked = lastChecked,
            appDisclaimer = appDisclaimer,
            minAnnualRate = minAnnualRate,
            maxAnnualRate = maxAnnualRate,
        )
    }
}

/**
 * Robust parser for the 30-bank loan directory dataset (570 records across 19 categories).
 */
object LoanDirectoryParser {

    /**
     * Parses the directory CSV from an [InputStream].
     */
    fun parse(inputStream: InputStream): List<LoanDirectoryRecord> {
        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
        val rawRows = LenderCsvParser.parseCsvTokens(reader)
        if (rawRows.isEmpty()) return emptyList()

        val dataRows = rawRows.drop(1)
        return dataRows.mapNotNull { row ->
            if (row.all { it.isBlank() }) null
            else parseRow(row)
        }
    }

    /**
     * Loads CSV from Android assets or classpath.
     */
    fun loadFromAssetsOrClasspath(context: Context?, assetName: String = "loan_directory.csv"): List<LoanDirectoryRecord> {
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
     * Generates a stable, deterministic unique ID derived from bank, loan type and product identity.
     */
    fun generateOfferId(bank: String, loanType: String): String {
        val bankSlug = bank.lowercase(Locale.US)
            .replace("&", "and")
            .replace("[^a-z0-9]+".toRegex(), "-")
            .trim('-')
        val typeSlug = loanType.lowercase(Locale.US)
            .replace("&", "and")
            .replace("/", "-")
            .replace("[^a-z0-9]+".toRegex(), "-")
            .trim('-')
        return "$bankSlug-$typeSlug"
    }

    /**
     * Parses a single 17-column CSV row.
     * Keeps missing numeric values null; never substitutes zero.
     */
    fun parseRow(fields: List<String>): LoanDirectoryRecord? {
        if (fields.size < 2) return null

        val bank = fields.getOrNull(0)?.trim().orEmpty()
        if (bank.isBlank()) return null

        val loanType = fields.getOrNull(1)?.trim().orEmpty()
        if (loanType.isBlank()) return null

        val shortDescription = fields.getOrNull(2)?.trim().orEmpty()
        val securityType = fields.getOrNull(3)?.trim().orEmpty()
        val typicalTenure = fields.getOrNull(4)?.trim().orEmpty()
        val eligibilitySummary = fields.getOrNull(5)?.trim().orEmpty()
        val commonDocuments = fields.getOrNull(6)?.trim().orEmpty()

        val minRate = fields.getOrNull(7)?.trim()?.takeIf { it.isNotEmpty() }?.toDoubleOrNull()
        val maxRate = fields.getOrNull(8)?.trim()?.takeIf { it.isNotEmpty() }?.toDoubleOrNull()

        val processingFee = fields.getOrNull(9)?.trim().orEmpty()
        val prepaymentForeclosure = fields.getOrNull(10)?.trim().orEmpty()
        val availabilityStatus = fields.getOrNull(11)?.trim().orEmpty().ifBlank { "Verify on bank website" }
        val officialProductUrl = fields.getOrNull(12)?.trim().orEmpty()
        val ratesChargesUrl = fields.getOrNull(13)?.trim().orEmpty()
        val ctaLabel = fields.getOrNull(14)?.trim().orEmpty().ifBlank { "Visit bank website" }
        val lastChecked = fields.getOrNull(15)?.trim().orEmpty().ifBlank { "2026-10-24" }
        val appDisclaimer = fields.getOrNull(16)?.trim().orEmpty()

        return LoanDirectoryRecord(
            bank = bank,
            loanType = loanType,
            shortDescription = shortDescription,
            securityType = securityType,
            typicalTenure = typicalTenure,
            eligibilitySummary = eligibilitySummary,
            commonDocuments = commonDocuments,
            minAnnualRate = minRate,
            maxAnnualRate = maxRate,
            processingFee = processingFee,
            prepaymentForeclosure = prepaymentForeclosure,
            availabilityStatus = availabilityStatus,
            officialProductUrl = officialProductUrl,
            ratesChargesUrl = ratesChargesUrl,
            ctaLabel = ctaLabel,
            lastChecked = lastChecked,
            appDisclaimer = appDisclaimer,
        )
    }
}
