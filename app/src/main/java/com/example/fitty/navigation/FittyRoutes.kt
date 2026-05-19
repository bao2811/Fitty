package com.example.fitty.navigation

object FittyRoutes {
    const val Splash = "splash"
    const val Welcome = "welcome"
    const val SignIn = "sign_in"
    const val SignUp = "sign_up"
    const val ForgotPassword = "forgot_password"
    const val Onboarding = "onboarding"
    const val PlanPreview = "plan_preview"
    const val Main = "main"
    const val WorkoutSession = "workout_session/{sessionId}"

    fun workoutSession(sessionId: String) = "workout_session/$sessionId"
}

object MainRoutes {
    const val Home = "home"
    const val Plan = "plan"
    const val Track = "track"
    const val Coach = "coach"
    const val Profile = "profile"
    const val CategoryExerciseList = "category_exercises/{categoryId}/{categoryLabel}/{bodyPartKeys}"

    fun categoryExerciseList(
        categoryId: String,
        categoryLabel: String,
        bodyPartKeys: List<String>
    ): String {
        val keys = bodyPartKeys.joinToString(",")
        return "category_exercises/$categoryId/$categoryLabel/$keys"
    }
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
