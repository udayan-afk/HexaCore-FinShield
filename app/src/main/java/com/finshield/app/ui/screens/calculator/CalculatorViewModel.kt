package com.finshield.app.ui.screens.calculator

import androidx.lifecycle.ViewModel
import com.finshield.app.domain.calculator.FieldErrors
import com.finshield.app.domain.calculator.InterestMethod
import com.finshield.app.domain.calculator.LoanCalculator
import com.finshield.app.domain.calculator.LoanResult
import com.finshield.app.domain.calculator.RepaymentFrequency
import com.finshield.app.domain.calculator.Validation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class CalculatorUiState(
    val principal: String = "500000",
    val rate: String = "12.5",
    val tenure: String = "36",
    val tenureInYears: Boolean = false,
    val method: InterestMethod = InterestMethod.REDUCING_BALANCE,
    val frequency: RepaymentFrequency = RepaymentFrequency.MONTHLY,
    val errors: FieldErrors = FieldErrors(),
    val result: LoanResult? = null,
    /** The settings the visible result was computed with (so labels stay correct while editing). */
    val resultFrequency: RepaymentFrequency = RepaymentFrequency.MONTHLY,
    val resultMethod: InterestMethod = InterestMethod.REDUCING_BALANCE,
)

class CalculatorViewModel : ViewModel() {
    private val _state = MutableStateFlow(CalculatorUiState())
    val state: StateFlow<CalculatorUiState> = _state.asStateFlow()

    init {
        com.finshield.app.domain.calculator.CalculatorPrefillStore.consume()?.let { prefill ->
            _state.update { current ->
                val pStr = prefill.principal?.let { p ->
                    if (p % 1.0 == 0.0) p.toLong().toString() else p.toString()
                } ?: current.principal
                val rStr = prefill.rate?.let { r ->
                    if (r % 1.0 == 0.0) r.toLong().toString() else r.toString()
                } ?: current.rate
                val tStr = prefill.tenureMonths?.toString() ?: current.tenure
                current.copy(
                    principal = pStr,
                    rate = rStr,
                    tenure = tStr,
                    tenureInYears = false,
                    method = prefill.method ?: current.method,
                )
            }
            calculate()
        }
    }

    fun onPrincipal(v: String) = _state.update { it.copy(principal = v.filter { c -> c.isDigit() || c == '.' || c == ',' }, errors = it.errors.copy(principal = null)) }
    fun onRate(v: String) = _state.update { it.copy(rate = v.filter { c -> c.isDigit() || c == '.' }, errors = it.errors.copy(rate = null)) }
    fun onTenure(v: String) = _state.update { it.copy(tenure = v.filter { c -> c.isDigit() || c == '.' }, errors = it.errors.copy(tenure = null)) }
    fun onUnit(years: Boolean) = _state.update { it.copy(tenureInYears = years, errors = it.errors.copy(tenure = null)) }
    fun onMethod(m: InterestMethod) = _state.update { it.copy(method = m) }
    fun onFrequency(f: RepaymentFrequency) = _state.update { it.copy(frequency = f, errors = it.errors.copy(tenure = null)) }

    fun calculate() {
        val s = _state.value
        when (val v = LoanCalculator.validate(s.principal, s.rate, s.tenure, s.tenureInYears, s.method, s.frequency)) {
            is Validation.Invalid -> _state.update { it.copy(errors = v.errors, result = null) }
            is Validation.Valid -> _state.update {
                it.copy(
                    errors = FieldErrors(),
                    result = LoanCalculator.calculate(v.input),
                    resultFrequency = s.frequency,
                    resultMethod = s.method,
                )
            }
        }
    }
}
