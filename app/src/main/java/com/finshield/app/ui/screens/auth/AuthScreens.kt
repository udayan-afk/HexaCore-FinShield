package com.finshield.app.ui.screens.auth

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finshield.app.ui.components.FinTextField
import com.finshield.app.ui.components.FinTopBar
import com.finshield.app.ui.components.InfoBanner
import com.finshield.app.ui.components.LinkText
import com.finshield.app.ui.components.PrimaryButton
import com.finshield.app.ui.components.ShieldLogo
import com.finshield.app.ui.theme.AppBackground
import com.finshield.app.ui.theme.CardWhite
import com.finshield.app.ui.theme.TextPrimary
import com.finshield.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch

/** Message shown until Firebase Authentication is wired in (Milestone B). Never fakes a sign-in. */
internal const val AUTH_NOT_CONNECTED = "Sign-in is not connected yet. Firebase Authentication is added in Milestone B."

@Composable
fun LoginScreen(onBack: () -> Unit, onCreateAccount: () -> Unit) {
    StatusBarIcons(darkIcons = true)
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var emailError by rememberSaveable { mutableStateOf<String?>(null) }
    var passwordError by rememberSaveable { mutableStateOf<String?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

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
            Spacer(Modifier.height(22.dp))
            InfoBanner("Setup in progress", "Email sign-in is connected to Firebase in the next milestone.")
            Spacer(Modifier.height(22.dp))
            FinTextField(
                label = "Email",
                value = email,
                onValueChange = { email = it; emailError = null },
                placeholder = "name@email.com",
                error = emailError,
                keyboardType = KeyboardType.Email,
            )
            Spacer(Modifier.height(18.dp))
            FinTextField(
                label = "Password",
                value = password,
                onValueChange = { password = it; passwordError = null },
                placeholder = "Enter your password",
                error = passwordError,
                isPassword = true,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                LinkText("Forgot Password?", onClick = {
                    emailError = AuthValidation.email(email)
                    scope.launch { snackbar.showSnackbar("Password reset is added together with Firebase Authentication.") }
                })
            }
            Spacer(Modifier.height(10.dp))
            PrimaryButton("Login", onClick = {
                emailError = AuthValidation.email(email)
                passwordError = AuthValidation.loginPassword(password)
                if (emailError == null && passwordError == null) {
                    scope.launch { snackbar.showSnackbar(AUTH_NOT_CONNECTED) }
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
}

@Composable
fun RegisterScreen(onBack: () -> Unit, onHaveAccount: () -> Unit) {
    StatusBarIcons(darkIcons = true)
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var nameError by rememberSaveable { mutableStateOf<String?>(null) }
    var emailError by rememberSaveable { mutableStateOf<String?>(null) }
    var passwordError by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmError by rememberSaveable { mutableStateOf<String?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

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
            Spacer(Modifier.height(22.dp))
            FinTextField("Full name", name, { name = it; nameError = null }, "Your name", error = nameError)
            Spacer(Modifier.height(16.dp))
            FinTextField("Email", email, { email = it; emailError = null }, "name@email.com", error = emailError, keyboardType = KeyboardType.Email)
            Spacer(Modifier.height(16.dp))
            FinTextField("Password", password, { password = it; passwordError = null }, "At least 8 characters, letters and numbers", error = passwordError, isPassword = true)
            Spacer(Modifier.height(16.dp))
            FinTextField("Confirm password", confirm, { confirm = it; confirmError = null }, "Re-enter your password", error = confirmError, isPassword = true)
            Spacer(Modifier.height(24.dp))
            PrimaryButton("Create Account", onClick = {
                nameError = AuthValidation.name(name)
                emailError = AuthValidation.email(email)
                passwordError = AuthValidation.newPassword(password)
                confirmError = AuthValidation.confirmPassword(password, confirm)
                if (listOf(nameError, emailError, passwordError, confirmError).all { it == null }) {
                    scope.launch { snackbar.showSnackbar(AUTH_NOT_CONNECTED) }
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
