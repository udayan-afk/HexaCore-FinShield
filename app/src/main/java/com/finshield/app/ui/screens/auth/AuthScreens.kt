package com.finshield.app.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finshield.app.ui.components.FinTextField
import com.finshield.app.ui.components.FinTopBar
import com.finshield.app.ui.components.InfoBanner
import com.finshield.app.ui.components.LinkText
import com.finshield.app.ui.components.PrimaryButton
import com.finshield.app.ui.components.SecondaryButton
import com.finshield.app.ui.components.ShieldLogo
import com.finshield.app.ui.theme.CardWhite
import com.finshield.app.ui.theme.EmeraldDark
import com.finshield.app.ui.theme.ErrorRed
import com.finshield.app.ui.theme.Navy
import com.finshield.app.ui.theme.TextPrimary
import com.finshield.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch

private const val LOGIN_BANNER_TITLE = "Firebase Authentication"
private const val LOGIN_BANNER_BODY =
    "Sign in with your registered email and password to access your documents and loan analysis tools."

private const val REGISTER_BANNER_TITLE = "Create FinShield Account"
private const val REGISTER_BANNER_BODY =
    "Your account will be securely authenticated with Firebase and your profile metadata stored in Cloud Firestore."

@Composable
fun LoginScreen(
    onBack: () -> Unit,
    onCreateAccount: () -> Unit,
    onAuthenticated: () -> Unit,
    vm: AuthViewModel = viewModel(),
) {
    StatusBarIcons(darkIcons = true)
    val ui by vm.state.collectAsState()
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var emailError by rememberSaveable { mutableStateOf<String?>(null) }
    var passwordError by rememberSaveable { mutableStateOf<String?>(null) }
    var showResetDialog by rememberSaveable { mutableStateOf(false) }
    var resetEmail by rememberSaveable { mutableStateOf("") }
    var resetEmailError by rememberSaveable { mutableStateOf<String?>(null) }

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(ui.error, ui.successMessage) {
        ui.error?.let {
            snackbar.showSnackbar(it)
            vm.clearError()
        }
        ui.successMessage?.let {
            snackbar.showSnackbar(it)
            vm.clearSuccessMessage()
        }
    }

    Scaffold(
        containerColor = CardWhite,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            FinTopBar(
                title = "FinShield",
                subtitle = "Understand Before You Borrow.",
                onBack = onBack,
                modifier = Modifier.statusBarsPadding(),
                leading = { ShieldLogo(size = 30.dp) },
            )
        },
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .navigationBarsPadding(),
        ) {
            Spacer(Modifier.height(24.dp))
            Text("Welcome back", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
            Spacer(Modifier.height(8.dp))
            Text(
                "Sign in to continue protecting your borrowing decisions.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
            Spacer(Modifier.height(20.dp))
            InfoBanner(LOGIN_BANNER_TITLE, LOGIN_BANNER_BODY)
            Spacer(Modifier.height(20.dp))
            FinTextField(
                label = "Email",
                value = email,
                onValueChange = { email = it; emailError = null },
                placeholder = "name@email.com",
                error = emailError,
                keyboardType = KeyboardType.Email,
                enabled = !ui.loading,
            )
            Spacer(Modifier.height(18.dp))
            FinTextField(
                label = "Password",
                value = password,
                onValueChange = { password = it; passwordError = null },
                placeholder = "Enter your password",
                error = passwordError,
                isPassword = true,
                enabled = !ui.loading,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                LinkText("Forgot Password?", onClick = {
                    resetEmail = email.trim()
                    resetEmailError = null
                    showResetDialog = true
                })
            }
            Spacer(Modifier.height(16.dp))
            PrimaryButton("Login", loading = ui.loading, onClick = {
                emailError = AuthValidation.email(email)
                passwordError = AuthValidation.loginPassword(password)
                if (emailError == null && passwordError == null) {
                    vm.signIn(email, password, onAuthenticated)
                }
            })
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Text("New to FinShield?", style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = TextSecondary)
                LinkText("Create Account", onClick = onCreateAccount)
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    // Password Reset Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { if (!ui.loading) showResetDialog = false },
            title = {
                Text(
                    "Reset Password",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Navy,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Enter your registered email address to receive a secure password reset link from Firebase.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                    FinTextField(
                        label = "Email",
                        value = resetEmail,
                        onValueChange = { resetEmail = it; resetEmailError = null },
                        placeholder = "name@email.com",
                        error = resetEmailError,
                        keyboardType = KeyboardType.Email,
                        enabled = !ui.loading,
                    )
                }
            },
            confirmButton = {
                PrimaryButton(
                    text = "Send Reset Link",
                    loading = ui.loading,
                    onClick = {
                        resetEmailError = AuthValidation.email(resetEmail)
                        if (resetEmailError == null) {
                            vm.sendPasswordReset(resetEmail) {
                                showResetDialog = false
                            }
                        }
                    },
                    modifier = Modifier.width(160.dp),
                )
            },
            dismissButton = {
                if (!ui.loading) {
                    TextButton(onClick = { showResetDialog = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            },
        )
    }
}

@Composable
fun RegisterScreen(
    onBack: () -> Unit,
    onHaveAccount: () -> Unit,
    onAuthenticated: () -> Unit,
    vm: AuthViewModel = viewModel(),
) {
    StatusBarIcons(darkIcons = true)
    val ui by vm.state.collectAsState()
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var consentAccepted by rememberSaveable { mutableStateOf(false) }

    var nameError by rememberSaveable { mutableStateOf<String?>(null) }
    var emailError by rememberSaveable { mutableStateOf<String?>(null) }
    var passwordError by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmError by rememberSaveable { mutableStateOf<String?>(null) }
    var consentError by rememberSaveable { mutableStateOf<String?>(null) }

    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(ui.error, ui.successMessage) {
        ui.error?.let {
            snackbar.showSnackbar(it)
            vm.clearError()
        }
        ui.successMessage?.let {
            snackbar.showSnackbar(it)
            vm.clearSuccessMessage()
        }
    }

    Scaffold(
        containerColor = CardWhite,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            FinTopBar(
                title = "FinShield",
                subtitle = "Understand Before You Borrow.",
                onBack = onBack,
                modifier = Modifier.statusBarsPadding(),
                leading = { ShieldLogo(size = 30.dp) },
            )
        },
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .navigationBarsPadding(),
        ) {
            Spacer(Modifier.height(24.dp))
            Text("Create your account", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
            Spacer(Modifier.height(8.dp))
            Text(
                "Save your analyses securely and compare loans with confidence.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
            Spacer(Modifier.height(20.dp))
            InfoBanner(REGISTER_BANNER_TITLE, REGISTER_BANNER_BODY)
            Spacer(Modifier.height(20.dp))
            FinTextField("Full name", name, { name = it; nameError = null }, "Your name", error = nameError, enabled = !ui.loading)
            Spacer(Modifier.height(16.dp))
            FinTextField("Email", email, { email = it; emailError = null }, "name@email.com", error = emailError, keyboardType = KeyboardType.Email, enabled = !ui.loading)
            Spacer(Modifier.height(16.dp))
            FinTextField("Password", password, { password = it; passwordError = null }, "At least 8 characters, letters and numbers", error = passwordError, isPassword = true, enabled = !ui.loading)
            Spacer(Modifier.height(16.dp))
            FinTextField("Confirm password", confirm, { confirm = it; confirmError = null }, "Re-enter your password", error = confirmError, isPassword = true, enabled = !ui.loading)
            Spacer(Modifier.height(16.dp))

            // Terms and Privacy Consent Checkbox
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = consentAccepted,
                    onCheckedChange = {
                        consentAccepted = it
                        consentError = null
                    },
                    colors = CheckboxDefaults.colors(checkedColor = EmeraldDark),
                    enabled = !ui.loading,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "I agree to FinShield's Terms of Service and Privacy Policy.",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (consentError != null) ErrorRed else TextSecondary,
                )
            }
            if (consentError != null) {
                Text(
                    text = consentError ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = ErrorRed,
                    modifier = Modifier.padding(start = 12.dp, top = 2.dp),
                )
            }

            Spacer(Modifier.height(20.dp))
            PrimaryButton("Create Account", loading = ui.loading, onClick = {
                nameError = AuthValidation.name(name)
                emailError = AuthValidation.email(email)
                passwordError = AuthValidation.newPassword(password)
                confirmError = AuthValidation.confirmPassword(password, confirm)
                if (!consentAccepted) {
                    consentError = "Please accept the terms to continue"
                }
                if (listOf(nameError, emailError, passwordError, confirmError, consentError).all { it == null }) {
                    vm.register(name, email, password, onAuthenticated)
                }
            })
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Text("Already have an account?", style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = TextSecondary)
                LinkText("Log in", onClick = onHaveAccount)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
