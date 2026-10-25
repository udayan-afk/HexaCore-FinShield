package com.finshield.app.ui.screens.credit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finshield.app.data.AppContainer
import com.finshield.app.data.auth.Session
import com.finshield.app.data.credit.CibilScoreRequest
import com.finshield.app.data.credit.CreditHealthRepository
import com.finshield.app.data.credit.ProviderStatus
import com.finshield.app.domain.credit.AAConnectionStatus
import com.finshield.app.domain.credit.CreditScoreStatus
import com.finshield.app.domain.credit.EmploymentType
import com.finshield.app.domain.credit.LoanEligibilityCalculator
import com.finshield.app.domain.credit.LoanEligibilityInput
import com.finshield.app.domain.credit.LoanEligibilityResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class CreditHealthUiState(
    val creditHealthState: com.finshield.app.data.credit.CreditHealthState = com.finshield.app.data.credit.CreditHealthState(),
    // Calculator Form state
    val monthlyIncomeStr: String = "75000",
    val existingEmiStr: String = "12000",
    val loanAmountStr: String = "500000",
    val tenureMonthsStr: String = "36",
    val interestRateStr: String = "10.5",
    val tenureInYears: Boolean = false,
    val employmentType: EmploymentType = EmploymentType.SALARIED,
    val eligibilityResult: LoanEligibilityResult = LoanEligibilityCalculator.evaluate(LoanEligibilityInput()),
    // UI dialogs / modals
    val showConsentDialog: Boolean = false,
    val showCibilSetupInfo: Boolean = false,
    val showAaSetupInfo: Boolean = false,
    val snackbarMessage: String? = null,
)

class CreditHealthViewModel(
    private val repo: CreditHealthRepository = AppContainer.creditHealth,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreditHealthUiState())
    val uiState: StateFlow<CreditHealthUiState> = _uiState.asStateFlow()

    init {
        // Observe repository state
        viewModelScope.launch {
            repo.state.collectLatest { credState ->
                _uiState.value = _uiState.value.copy(
                    creditHealthState = credState,
                )
                recalculateEligibility()
            }
        }

        // Initialize user data from current Firebase session
        val currentSession = AppContainer.auth.session.value
        if (currentSession is Session.Authenticated) {
            viewModelScope.launch {
                repo.loadUserCreditHealth(currentSession.uid)
            }
        }
    }

    fun onIncomeChange(value: String) {
        val filtered = value.filter { it.isDigit() }
        _uiState.value = _uiState.value.copy(monthlyIncomeStr = filtered)
        recalculateEligibility()
    }

    fun onExistingEmiChange(value: String) {
        val filtered = value.filter { it.isDigit() }
        _uiState.value = _uiState.value.copy(existingEmiStr = filtered)
        recalculateEligibility()
    }

    fun onLoanAmountChange(value: String) {
        val filtered = value.filter { it.isDigit() }
        _uiState.value = _uiState.value.copy(loanAmountStr = filtered)
        recalculateEligibility()
    }

    fun onTenureChange(value: String) {
        val filtered = value.filter { it.isDigit() }
        _uiState.value = _uiState.value.copy(tenureMonthsStr = filtered)
        recalculateEligibility()
    }

    fun onRateChange(value: String) {
        val filtered = value.filter { it.isDigit() || it == '.' }
        _uiState.value = _uiState.value.copy(interestRateStr = filtered)
        recalculateEligibility()
    }

    fun onTenureUnitToggle(inYears: Boolean) {
        _uiState.value = _uiState.value.copy(tenureInYears = inYears)
        recalculateEligibility()
    }

    fun onEmploymentTypeSelect(type: EmploymentType) {
        _uiState.value = _uiState.value.copy(employmentType = type)
        recalculateEligibility()
    }

    private fun recalculateEligibility() {
        val income = _uiState.value.monthlyIncomeStr.toDoubleOrNull() ?: 0.0
        val existingEmi = _uiState.value.existingEmiStr.toDoubleOrNull() ?: 0.0
        val amount = _uiState.value.loanAmountStr.toDoubleOrNull() ?: 0.0
        val rawTenure = _uiState.value.tenureMonthsStr.toIntOrNull() ?: 36
        val tenureMonths = if (_uiState.value.tenureInYears) rawTenure * 12 else rawTenure
        val rate = _uiState.value.interestRateStr.toDoubleOrNull() ?: 10.5
        val verifiedScore = _uiState.value.creditHealthState.creditScore.score

        val input = LoanEligibilityInput(
            monthlyIncome = income,
            existingMonthlyEmi = existingEmi,
            employmentType = _uiState.value.employmentType,
            requestedLoanAmount = amount,
            requestedTenureMonths = tenureMonths,
            expectedAnnualInterestRate = rate,
            creditScore = verifiedScore,
        )

        val result = LoanEligibilityCalculator.evaluate(input)
        _uiState.value = _uiState.value.copy(eligibilityResult = result)
    }

    fun openConsentDialog() {
        val status = repo.getCibilProviderStatus()
        if (status is ProviderStatus.MissingConfiguration) {
            _uiState.value = _uiState.value.copy(showCibilSetupInfo = true)
        } else {
            _uiState.value = _uiState.value.copy(showConsentDialog = true)
        }
    }

    fun dismissConsentDialog() {
        _uiState.value = _uiState.value.copy(showConsentDialog = false)
    }

    fun dismissCibilSetupInfo() {
        _uiState.value = _uiState.value.copy(showCibilSetupInfo = false)
    }

    fun openAaSetupInfo() {
        _uiState.value = _uiState.value.copy(showAaSetupInfo = true)
    }

    fun dismissAaSetupInfo() {
        _uiState.value = _uiState.value.copy(showAaSetupInfo = false)
    }

    fun submitCibilInquiry(pan: String, dob: String, fullName: String, consentGranted: Boolean) {
        val currentSession = AppContainer.auth.session.value
        val uid = (currentSession as? Session.Authenticated)?.uid ?: ""

        dismissConsentDialog()

        viewModelScope.launch {
            val request = CibilScoreRequest(
                pan = pan.trim().uppercase(),
                fullName = fullName.trim(),
                dateOfBirth = dob.trim(),
                mobileNumber = "",
                consentGranted = consentGranted,
            )
            val result = repo.requestScore(uid, "", request)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(snackbarMessage = "Credit score updated successfully.")
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    snackbarMessage = err.localizedMessage ?: "Could not complete CIBIL score inquiry.",
                )
            }
        }
    }

    fun loadSandboxTestData() {
        val currentSession = AppContainer.auth.session.value
        val uid = (currentSession as? Session.Authenticated)?.uid ?: ""
        viewModelScope.launch {
            repo.setSandboxTestData(uid, simulatedScore = 772)
            _uiState.value = _uiState.value.copy(
                snackbarMessage = "Loaded verified sandbox test profile (772 - Excellent).",
            )
        }
    }

    fun disconnectAccountAggregator() {
        val currentSession = AppContainer.auth.session.value
        val uid = (currentSession as? Session.Authenticated)?.uid ?: ""
        viewModelScope.launch {
            repo.revokeAccountAggregator(uid, "")
            _uiState.value = _uiState.value.copy(
                snackbarMessage = "Account Aggregator consent revoked and accounts disconnected.",
            )
        }
    }

    fun dismissSnackbar() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }
}
