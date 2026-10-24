package com.finshield.app.data

import android.content.Context
import com.finshield.app.data.auth.AuthRepository
import com.finshield.app.data.auth.FirebaseAuthRepository
import com.finshield.app.data.comparison.AssetLoanOfferRepository
import com.finshield.app.data.comparison.LoanOfferRepository
import com.finshield.app.data.history.DemoHistoryRepository
import com.finshield.app.data.history.HistoryRepository

/**
 * Service locator for FinShield repositories.
 */
object AppContainer {
    private var _auth: AuthRepository? = null
    var auth: AuthRepository
        get() = _auth ?: FirebaseAuthRepository().also { _auth = it }
        set(value) {
            _auth = value
        }

    val history: HistoryRepository = DemoHistoryRepository()

    private var _loanOffers: LoanOfferRepository? = null
    val loanOffers: LoanOfferRepository
        get() = _loanOffers ?: AssetLoanOfferRepository.getInstance().also { _loanOffers = it }

    fun init(context: Context) {
        _loanOffers = AssetLoanOfferRepository.getInstance(context.applicationContext)
    }
}
