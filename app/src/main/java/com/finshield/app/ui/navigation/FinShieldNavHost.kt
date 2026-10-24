package com.finshield.app.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.finshield.app.data.AppContainer
import com.finshield.app.data.auth.Session
import com.finshield.app.ui.screens.analyzer.AnalyzerScreen
import com.finshield.app.ui.screens.analyzer.ReportScreen
import com.finshield.app.ui.screens.auth.LoginScreen
import com.finshield.app.ui.screens.auth.OnboardingScreen
import com.finshield.app.ui.screens.auth.RegisterScreen
import com.finshield.app.ui.screens.auth.SplashScreen
import com.finshield.app.ui.screens.auth.WelcomeScreen
import com.finshield.app.ui.screens.calculator.CalculatorScreen
import com.finshield.app.ui.screens.comparison.ComparisonScreen
import com.finshield.app.ui.screens.dashboard.DashboardScreen
import com.finshield.app.ui.screens.history.HistoryScreen
import com.finshield.app.ui.screens.profile.ProfileScreen
import com.finshield.app.ui.theme.AppBackground

@Composable
fun FinShieldNavHost(navController: NavHostController = rememberNavController()) {
    val auth = AppContainer.auth
    val session by auth.session.collectAsState()
    val route = navController.currentBackStackEntryAsState().value?.destination?.route
    val showBottomBar = route in Routes.bottomTabs

    // Unauthorized-screen handling: signed-out users cannot access protected screens.
    LaunchedEffect(session, route) {
        if (session is Session.SignedOut && route in Routes.protectedRoutes) {
            navController.navigate(Routes.WELCOME) { popUpTo(navController.graph.id) { inclusive = true } }
        }
    }

    // Entering the app clears Splash/Welcome/Login/Register so Back never returns to auth screens.
    val enterApp: () -> Unit = {
        navController.navigate(Routes.DASHBOARD) { popUpTo(navController.graph.id) { inclusive = true } }
    }
    val openReport: (String) -> Unit = { navController.navigate(Routes.report(it)) }

    Scaffold(
        containerColor = AppBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                FinBottomBar(currentRoute = route, onSelect = { target ->
                    if (target == route) return@FinBottomBar
                    if (target == Routes.DASHBOARD) {
                        val popped = navController.popBackStack(Routes.DASHBOARD, inclusive = false)
                        if (!popped) {
                            navController.navigate(Routes.DASHBOARD) {
                                launchSingleTop = true
                            }
                        }
                    } else {
                        navController.navigate(target) {
                            popUpTo(Routes.DASHBOARD) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                })
            }
        },
    ) { inner ->
        NavHost(
            navController = navController,
            startDestination = Routes.SPLASH,
            modifier = Modifier.padding(bottom = inner.calculateBottomPadding()),
        ) {
            composable(Routes.SPLASH) {
                SplashScreen(
                    onFinished = {
                        val target = if (session is Session.SignedOut) Routes.WELCOME else Routes.DASHBOARD
                        navController.navigate(target) { popUpTo(Routes.SPLASH) { inclusive = true } }
                    },
                )
            }
            composable(Routes.WELCOME) {
                WelcomeScreen(
                    onGetStarted = { navController.navigate(Routes.ONBOARDING) },
                    onHaveAccount = { navController.navigate(Routes.LOGIN) },
                )
            }
            composable(Routes.ONBOARDING) {
                OnboardingScreen(
                    onBack = { navController.popBackStack() },
                    onSkip = {
                        navController.navigate(Routes.REGISTER) {
                            popUpTo(Routes.WELCOME)
                        }
                    },
                    onFinish = {
                        navController.navigate(Routes.REGISTER) {
                            popUpTo(Routes.WELCOME)
                        }
                    },
                )
            }
            composable(Routes.LOGIN) {
                LoginScreen(
                    onBack = { navController.popBackStack() },
                    onCreateAccount = {
                        navController.navigate(Routes.REGISTER) { popUpTo(Routes.LOGIN) { inclusive = true } }
                    },
                    onAuthenticated = enterApp,
                )
            }
            composable(Routes.REGISTER) {
                RegisterScreen(
                    onBack = { navController.popBackStack() },
                    onHaveAccount = {
                        navController.navigate(Routes.LOGIN) { popUpTo(Routes.REGISTER) { inclusive = true } }
                    },
                    onAuthenticated = enterApp,
                )
            }

            composable(Routes.DASHBOARD) {
                DashboardScreen(
                    session = session,
                    onAnalyze = { navController.navigate(Routes.ANALYZE) { launchSingleTop = true } },
                    onCalculator = { navController.navigate(Routes.CALCULATOR) },
                    onCompare = { navController.navigate(Routes.COMPARE) { launchSingleTop = true } },
                    onHistory = { navController.navigate(Routes.HISTORY) },
                    onOpenReport = openReport,
                    onProfile = { navController.navigate(Routes.PROFILE) { launchSingleTop = true } },
                )
            }
            composable(Routes.ANALYZE) {
                AnalyzerScreen(
                    onOpenSampleReport = { navController.navigate(Routes.ocrReview("sample")) },
                    onOpenReport = { reportId -> navController.navigate(Routes.ocrReview(reportId)) },
                    onBack = {
                        val popped = navController.popBackStack()
                        if (!popped) {
                            navController.navigate(Routes.DASHBOARD) { launchSingleTop = true }
                        }
                    }
                )
            }
            composable(Routes.COMPARE) {
                ComparisonScreen(
                    onBack = {
                        val popped = navController.popBackStack()
                        if (!popped) {
                            navController.navigate(Routes.DASHBOARD) { launchSingleTop = true }
                        }
                    }
                )
            }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    session = session,
                    onHistory = { navController.navigate(Routes.HISTORY) },
                    onSignOut = { auth.signOut() },
                )
            }
            composable(Routes.CALCULATOR) {
                CalculatorScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.HISTORY) {
                HistoryScreen(
                    onBack = { navController.popBackStack() },
                    onOpenReport = openReport,
                    onAnalyze = { navController.navigate(Routes.ANALYZE) { launchSingleTop = true } },
                )
            }
            composable(
                Routes.OCR_REVIEW,
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
            ) { entry ->
                val reportId = entry.arguments?.getString("id") ?: "sample"
                com.finshield.app.ui.screens.analyzer.OcrReviewScreen(
                    reportId = reportId,
                    onConfirmAndProceed = { id ->
                        navController.navigate(Routes.report(id)) {
                            popUpTo(Routes.ANALYZE)
                        }
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                Routes.REPORT,
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
            ) { entry ->
                ReportScreen(
                    reportId = entry.arguments?.getString("id") ?: "sample",
                    onBack = { navController.popBackStack() },
                    onOpenCalculator = { navController.navigate(Routes.CALCULATOR) },
                    onOpenCompleteBreakdown = { navController.navigate(Routes.completeBreakdown(it)) },
                    onOpenOcrReview = { navController.navigate(Routes.ocrReview(it)) },
                )
            }
            composable(
                Routes.COMPLETE_BREAKDOWN,
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
            ) { entry ->
                com.finshield.app.ui.screens.analyzer.CompleteBreakdownScreen(
                    reportId = entry.arguments?.getString("id") ?: "sample",
                    onBack = { navController.popBackStack() },
                    onOpenCalculator = { navController.navigate(Routes.CALCULATOR) },
                )
            }
        }
    }
}
