package com.finshield.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.finshield.app.ui.screens.auth.LoginScreen
import com.finshield.app.ui.screens.auth.RegisterScreen
import com.finshield.app.ui.screens.auth.SplashScreen
import com.finshield.app.ui.screens.auth.WelcomeScreen

@Composable
fun FinShieldNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) {
            SplashScreen(
                onFinished = {
                    // Milestone B: route to the dashboard when a Firebase session exists.
                    navController.navigate(Routes.WELCOME) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.WELCOME) {
            WelcomeScreen(
                onGetStarted = { navController.navigate(Routes.REGISTER) },
                onHaveAccount = { navController.navigate(Routes.LOGIN) },
            )
        }
        composable(Routes.LOGIN) {
            LoginScreen(
                onBack = { navController.popBackStack() },
                onCreateAccount = {
                    navController.navigate(Routes.REGISTER) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.REGISTER) {
            RegisterScreen(
                onBack = { navController.popBackStack() },
                onHaveAccount = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.REGISTER) { inclusive = true }
                    }
                },
            )
        }
    }
}
