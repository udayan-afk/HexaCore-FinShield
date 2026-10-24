package com.finshield.app.data

import com.finshield.app.data.auth.AuthRepository
import com.finshield.app.data.auth.DemoAuthRepository
import com.finshield.app.data.history.DemoHistoryRepository
import com.finshield.app.data.history.HistoryRepository

import com.finshield.app.data.comparison.DemoLoanOfferRepository
import com.finshield.app.data.comparison.LoanOfferRepository

/**
 * Minimal service locator. Frontend phase: demo implementations.
 * To integrate Firebase later, swap these properties for Firebase/Firestore implementations.
 */
object AppContainer {
    val auth: AuthRepository = DemoAuthRepository()
    val history: HistoryRepository = DemoHistoryRepository()
    val loanOffers: LoanOfferRepository = DemoLoanOfferRepository()
}

