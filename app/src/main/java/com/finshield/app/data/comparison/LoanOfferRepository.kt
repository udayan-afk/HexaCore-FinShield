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

    init {
        loadData()
    }

    private fun loadData() {
        val records = LenderCsvParser.loadFromAssetsOrClasspath(context, "lenders.csv")
        if (records.isNotEmpty()) {
            _lenderRecords.value = records
            _offers.value = records.map { it.toLoanOffer() }
        } else {
            // Fallback to sample offers if asset stream unavailable
            _offers.value = DemoLoanOfferRepository.SAMPLE_OFFERS
        }
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
