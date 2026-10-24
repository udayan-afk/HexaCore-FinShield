package com.finshield.app.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finshield.app.data.AppContainer
import com.finshield.app.data.auth.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
)

/** Drives Login/Register through Firebase-backed [AuthRepository]. */
class AuthViewModel(private val repo: AuthRepository = AppContainer.auth) : ViewModel() {
    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun signIn(email: String, password: String, onSuccess: () -> Unit) {
        run(onSuccess) { repo.signIn(email.trim(), password) }
    }

    fun register(name: String, email: String, password: String, onSuccess: () -> Unit) {
        run(onSuccess) { repo.register(name.trim(), email.trim(), password) }
    }

    fun sendPasswordReset(email: String, onSent: () -> Unit) {
        val emailError = AuthValidation.email(email)
        if (emailError != null) {
            _state.value = AuthUiState(error = emailError)
            return
        }
        if (_state.value.loading) return
        _state.value = AuthUiState(loading = true)
        viewModelScope.launch {
            repo.sendPasswordReset(email.trim())
                .onSuccess {
                    _state.value = AuthUiState(successMessage = "Password reset link sent to $email. Please check your inbox.")
                    onSent()
                }
                .onFailure {
                    _state.value = AuthUiState(error = it.message ?: "Failed to send password reset email. Please try again.")
                }
        }
    }

    fun clearError() {
        if (_state.value.error != null) _state.value = _state.value.copy(error = null)
    }

    fun clearSuccessMessage() {
        if (_state.value.successMessage != null) _state.value = _state.value.copy(successMessage = null)
    }

    private fun run(onSuccess: () -> Unit, block: suspend () -> Result<*>) {
        if (_state.value.loading) return
        _state.value = AuthUiState(loading = true)
        viewModelScope.launch {
            block()
                .onSuccess {
                    _state.value = AuthUiState()
                    onSuccess()
                }
                .onFailure { _state.value = AuthUiState(error = it.message ?: "Something went wrong. Please try again.") }
        }
    }
}
