package com.finshield.app.ui.navigation

object Routes {
    const val SPLASH = "splash"
    const val WELCOME = "welcome"
    const val ONBOARDING = "onboarding"
    const val LOGIN = "login"
    const val REGISTER = "register"

    const val DASHBOARD = "dashboard"
    const val ANALYZE = "analyze"
    const val COMPARE = "compare"
    const val PROFILE = "profile"
    const val CALCULATOR = "calculator"
    const val HISTORY = "history"
    const val REPORT = "report/{id}"
    const val COMPLETE_BREAKDOWN = "complete_breakdown/{id}"

    fun report(id: String) = "report/$id"
    fun completeBreakdown(id: String) = "complete_breakdown/$id"

    /** Destinations shown with the bottom navigation bar. */
    val bottomTabs = listOf(DASHBOARD, ANALYZE, COMPARE, PROFILE)

    /** Everything behind authenticated Firebase sign-in. */
    val protectedRoutes = setOf(DASHBOARD, ANALYZE, COMPARE, PROFILE, CALCULATOR, HISTORY, REPORT, COMPLETE_BREAKDOWN)
}
