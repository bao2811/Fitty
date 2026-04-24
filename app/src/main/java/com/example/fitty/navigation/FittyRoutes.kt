package com.example.fitty.navigation

object FittyRoutes {
    const val Splash = "splash"
    const val Welcome = "welcome"
    const val SignIn = "sign_in"
    const val SignUp = "sign_up"
    const val Onboarding = "onboarding"
    const val PlanPreview = "plan_preview"
    const val Main = "main"
}

object MainRoutes {
    const val Home = "home"
    const val Plan = "plan"
    const val Track = "track"
    const val Coach = "coach"
    const val Profile = "profile"
}

data class MainTab(
    val route: String,
    val label: String,
    val iconLabel: String
)

val MainTabs = listOf(
    MainTab(MainRoutes.Home, "Home", "H"),
    MainTab(MainRoutes.Plan, "Practice", "P"),
    MainTab(MainRoutes.Track, "Track", "T"),
    MainTab(MainRoutes.Coach, "Coach", "C"),
    MainTab(MainRoutes.Profile, "Profile", "U")
)
