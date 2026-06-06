package com.example.fitty.navigation

import android.net.Uri
import androidx.annotation.StringRes
import com.example.fitty.R

object FittyRoutes {
    const val Splash = "splash"
    const val Welcome = "welcome"
    const val SignIn = "sign_in"
    const val SignUp = "sign_up"
    const val ForgotPassword = "forgot_password"
    const val Onboarding = "onboarding"
    const val PlanPreview = "plan_preview"
    const val Main = "main"
    const val WorkoutDetails = "workout_details/{planId}/{scheduledWorkoutId}"
    const val WorkoutSession = "workout_session/{sessionId}/{planId}/{scheduledWorkoutId}"
    const val ExerciseDetail = "exercise_detail/{exerciseId}"
    const val ExerciseVideo = "exercise_video/{exerciseId}"

    fun workoutSession(
        sessionId: String,
        planId: String = "",
        scheduledWorkoutId: String = ""
    ) = "workout_session/$sessionId/$planId/$scheduledWorkoutId"

    fun workoutDetails(
        planId: String,
        scheduledWorkoutId: String
    ) = "workout_details/$planId/$scheduledWorkoutId"

    fun exerciseDetail(exerciseId: String) = "exercise_detail/$exerciseId"

    fun exerciseVideo(exerciseId: String) = "exercise_video/$exerciseId"
}

object MainRoutes {
    const val Home = "home"
    const val Plan = "plan"
    const val Track = "track"
    const val Coach = "coach"
    const val Profile = "profile"
    const val ExerciseLibrary = "exercise_library"
    const val CategoryExerciseList = "category_exercises/{categoryId}/{categoryLabel}/{bodyPartKeys}"

    fun categoryExerciseList(
        categoryId: String,
        categoryLabel: String,
        bodyPartKeys: List<String>
    ): String {
        val encodedCategoryId = Uri.encode(categoryId)
        val encodedLabel = Uri.encode(categoryLabel)
        val encodedKeys = Uri.encode(bodyPartKeys.joinToString(","))
        return "category_exercises/$encodedCategoryId/$encodedLabel/$encodedKeys"
    }
}

data class MainTab(
    val route: String,
    @StringRes val labelRes: Int,
    val iconLabel: String
)

val MainTabs = listOf(
    MainTab(MainRoutes.Home, R.string.nav_tab_home, "H"),
    MainTab(MainRoutes.Plan, R.string.nav_tab_practice, "P"),
    MainTab(MainRoutes.Track, R.string.nav_tab_track, "T"),
    MainTab(MainRoutes.Coach, R.string.nav_tab_coach, "C"),
    MainTab(MainRoutes.Profile, R.string.nav_tab_profile, "U")
)
