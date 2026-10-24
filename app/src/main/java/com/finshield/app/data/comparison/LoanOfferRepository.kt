package com.finshield.app.data.comparison

import android.content.Context
import com.finshield.app.domain.calculator.InterestMethod
import com.finshield.app.model.LenderClassification
import com.finshield.app.model.LenderRecord
import com.finshield.app.model.LoanCategory
import com.finshield.app.model.LoanOffer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import java.util.Locale

interface LoanOfferRepository {
    fun getAllOffers(): Flow<List<LoanOffer>>
    fun getAllLenderRecords(): Flow<List<LenderRecord>> = flowOf(emptyList())
}

/**
 * Production-ready repository loading the research-backed 32-lender catalogue from `lenders.csv`.
 * Falls back gracefully in test environments using ClassLoader resources.
 */
class AssetLoanOfferRepository(
    private val context: Context? = null,
) : LoanOfferRepository {

    private val _lenderRecords = MutableStateFlow<List<LenderRecord>>(emptyList())
    private val _offers = MutableStateFlow<List<LoanOffer>>(emptyList())
    private val _loadError = MutableStateFlow<String?>(null)

    val loadError: Flow<String?> = _loadError.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        try {
            val researchRecords = LenderCsvParser.loadFromAssetsOrClasspath(context, "lenders.csv")
            val directoryRecords = LoanDirectoryParser.loadFromAssetsOrClasspath(context, "loan_directory.csv")

            if (researchRecords.isNotEmpty()) {
                _lenderRecords.value = researchRecords
            }

            val merged = mergeDatasets(researchRecords, directoryRecords)
            if (merged.isNotEmpty()) {
                _offers.value = merged
            } else if (researchRecords.isNotEmpty()) {
                _offers.value = researchRecords.map { it.toLoanOffer() }
            } else {
                // Fallback to sample offers if asset stream unavailable
                _offers.value = DemoLoanOfferRepository.SAMPLE_OFFERS
            }
        } catch (e: Exception) {
            _loadError.value = e.message
            _offers.value = DemoLoanOfferRepository.SAMPLE_OFFERS
        }
    }

    private fun mergeDatasets(
        researchRecords: List<LenderRecord>,
        directoryRecords: List<LoanDirectoryRecord>,
    ): List<LoanOffer> {
        if (directoryRecords.isEmpty() && researchRecords.isEmpty()) return emptyList()
        if (directoryRecords.isEmpty()) return researchRecords.map { it.toLoanOffer() }

        val matchedResearch = mutableSetOf<String>()
        val result = mutableListOf<LoanOffer>()

        // 1. Process all directory records
        for (dir in directoryRecords) {
            if (dir.loanType.equals("Personal Loan", ignoreCase = true)) {
                val match = researchRecords.firstOrNull { res ->
                    normalizeBankName(res.lenderName) == normalizeBankName(dir.bank)
                }
                if (match != null) {
                    matchedResearch.add(match.id)
                    result.add(
                        LoanOffer(
                            id = LoanDirectoryParser.generateOfferId(dir.bank, "Personal Loan"),
                            lenderName = dir.bank,
                            offerName = "Personal Loan",
                            category = LoanCategory.PERSONAL,
                            annualRatePercent = match.minAnnualRate ?: 0.0,
                            interestMethod = InterestMethod.REDUCING_BALANCE,
                            processingFeeFixed = match.fixedFeeEstimate ?: 0.0,
                            processingFeePercent = match.feePercentEstimate ?: 0.0,
                            hasGst = true,
                            minAmount = 10_000.0,
                            maxAmount = match.parsedMaxLoanAmount ?: 5_000_000.0,
                            minTenureMonths = 12,
                            maxTenureMonths = match.maxTenureMonths,
                            eligibilityNotes = match.eligibilityNotes.ifBlank { dir.eligibilitySummary },
                            officialUrl = dir.officialProductUrl.ifBlank { match.sourceUrl },
                            isVerifiedSource = true,
                            lenderClassification = match.lenderType,
                            publishedInterestRate = match.publishedInterestRate,
                            isMonthlyRate = match.isMonthlyRate,
                            monthlyRatePercent = match.monthlyRatePercent,
                            isComparableStartingRate = match.isComparableStartingRate,
                            rawMaxLoanAmount = match.rawMaxLoanAmount,
                            sourceAsOf = match.sourceAsOf.ifBlank { dir.lastChecked },
                            datasetStatus = match.datasetStatus,
                            datasetVersion = "2026.10-merged",
                            publishedProcessingFee = match.publishedProcessingFee.ifBlank { dir.processingFee },
                            shortDescription = dir.shortDescription,
                            securityType = dir.securityType,
                            typicalTenure = dir.typicalTenure,
                            commonDocuments = dir.commonDocuments,
                            prepaymentForeclosure = dir.prepaymentForeclosure,
                            availabilityStatus = dir.availabilityStatus,
                            ratesChargesUrl = dir.ratesChargesUrl.takeIf { it.isNotBlank() },
                            ctaLabel = dir.ctaLabel.ifBlank { "Visit bank website" },
                            lastChecked = dir.lastChecked,
                            appDisclaimer = dir.appDisclaimer,
                            minAnnualRate = match.minAnnualRate,
                            maxAnnualRate = match.maxAnnualRate,
                        )
                    )
                } else {
                    result.add(dir.toLoanOffer())
                }
            } else {
                result.add(dir.toLoanOffer())
            }
        }

        // 2. Add remaining research records (e.g. NBFCs and digital lenders) that are not in the directory
        for (res in researchRecords) {
            if (!matchedResearch.contains(res.id)) {
                val offer = res.toLoanOffer()
                result.add(
                    offer.copy(
                        id = LoanDirectoryParser.generateOfferId(res.lenderName, "Personal Loan"),
                        category = LoanCategory.PERSONAL,
                    )
                )
            }
        }

        return result
    }

    private fun normalizeBankName(name: String): String {
        return name.lowercase(Locale.US)
            .replace("&", "and")
            .replace("bank", "")
            .replace("[^a-z0-9]".toRegex(), "")
            .trim()
    }

    override fun getAllOffers(): Flow<List<LoanOffer>> = _offers.asStateFlow()

    override fun getAllLenderRecords(): Flow<List<LenderRecord>> = _lenderRecords.asStateFlow()

    companion object {
        @Volatile
        private var INSTANCE: AssetLoanOfferRepository? = null

        fun getInstance(context: Context? = null): AssetLoanOfferRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AssetLoanOfferRepository(context).also { INSTANCE = it }
            }
        }
    }
}

class DemoLoanOfferRepository : LoanOfferRepository {
    override fun getAllOffers(): Flow<List<LoanOffer>> = flowOf(SAMPLE_OFFERS)

    companion object {
        val SAMPLE_OFFERS = listOf(
            LoanOffer(
                id = "aster-personal",
                lenderName = "Aster Finance",
                offerName = "Sample personal loan",
                category = LoanCategory.PERSONAL,
                annualRatePercent = 11.75,
                interestMethod = InterestMethod.REDUCING_BALANCE,
                processingFeeFixed = 7500.0,
                hasGst = true,
                eligibilityNotes = "Salaried, 21–58",
                isVerifiedSource = false,
                officialUrl = null,
            ),
            LoanOffer(
                id = "metro-personal",
                lenderName = "Metro Bank",
                offerName = "Sample personal loan",
                category = LoanCategory.PERSONAL,
                annualRatePercent = 12.25,
                interestMethod = InterestMethod.REDUCING_BALANCE,
                processingFeeFixed = 5000.0,
                hasGst = true,
                eligibilityNotes = "Salaried, min ₹25k/mo",
                isVerifiedSource = false,
                officialUrl = null,
            ),
            LoanOffer(
                id = "swift-personal",
                lenderName = "SwiftCredit",
                offerName = "Fixed term personal credit",
                category = LoanCategory.PERSONAL,
                annualRatePercent = 10.50,
                interestMethod = InterestMethod.FLAT,
                processingFeeFixed = 2500.0,
                hasGst = true,
                eligibilityNotes = "Instant approval, 21–60",
                isVerifiedSource = false,
                officialUrl = null,
            ),
            LoanOffer(
                id = "finedge-personal",
                lenderName = "FinEdge Capital",
                offerName = "Flexi Personal Line",
                category = LoanCategory.PERSONAL,
                annualRatePercent = 13.50,
                interestMethod = InterestMethod.REDUCING_BALANCE,
                processingFeeFixed = 3500.0,
                hasGst = true,
                eligibilityNotes = "Credit score 720+",
                isVerifiedSource = false,
                officialUrl = null,
            ),
            LoanOffer(
                id = "axis-bank",
                lenderName = "Axis Bank",
                offerName = "Personal Loan",
                category = LoanCategory.PERSONAL,
                annualRatePercent = 9.99,
                interestMethod = InterestMethod.REDUCING_BALANCE,
                processingFeeFixed = 0.0,
                processingFeePercent = 2.0,
                hasGst = true,
                lenderClassification = LenderClassification.PRIVATE_BANK,
                publishedInterestRate = "9.99% onwards",
                eligibilityNotes = "Profile-dependent; verify official age, income, employment and credit rules.",
                isVerifiedSource = false,
                officialUrl = "https://www.paisabazaar.com/personal-loan/interest-rates/",
            ),
            LoanOffer(
                id = "bandhan-bank",
                lenderName = "Bandhan Bank",
                offerName = "Personal Loan",
                category = LoanCategory.PERSONAL,
                annualRatePercent = 8.86,
                interestMethod = InterestMethod.REDUCING_BALANCE,
                processingFeeFixed = 0.0,
                processingFeePercent = 3.0,
                hasGst = true,
                lenderClassification = LenderClassification.PRIVATE_BANK,
                publishedInterestRate = "8.86% onwards",
                eligibilityNotes = "Verify current official applicant and income criteria.",
                isVerifiedSource = false,
                officialUrl = "https://www.paisabazaar.com/personal-loan/interest-rates/",
            ),
            LoanOffer(
                id = "state-bank-of-india",
                lenderName = "State Bank of India",
                offerName = "Personal Loan",
                category = LoanCategory.PERSONAL,
                annualRatePercent = 10.00,
                interestMethod = InterestMethod.REDUCING_BALANCE,
                processingFeeFixed = 0.0,
                processingFeePercent = 1.5,
                hasGst = true,
                lenderClassification = LenderClassification.PUBLIC_BANK,
                publishedInterestRate = "10.00% onwards",
                eligibilityNotes = "Some schemes target salaried/salary-account customers; verify rules.",
                isVerifiedSource = false,
                officialUrl = "https://www.paisabazaar.com/personal-loan/interest-rates/",
            ),
        )
    }
}
