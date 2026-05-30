package com.example.fitty.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.fitty.feature_auth.ForgotPasswordRoute
import com.example.fitty.feature_auth.SignInRoute
import com.example.fitty.feature_auth.SignUpRoute
import com.example.fitty.feature_entry.SplashRoute
import com.example.fitty.feature_exercise.ExerciseDetailRoute
import com.example.fitty.feature_exercise.ExerciseVideoPlayerRoute
import com.example.fitty.feature_entry.WelcomeRoute
import com.example.fitty.feature_onboarding.OnboardingRoute
import com.example.fitty.feature_onboarding.PlanPreviewRoute
import com.example.fitty.feature_workout.WorkoutDetailsRoute
import com.example.fitty.feature_workout.WorkoutSessionRoute

@Composable
fun FittyNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = FittyRoutes.Splash
    ) {
        composable(FittyRoutes.Splash) {
            SplashRoute(
                onOpenWelcome = {
                    navController.navigate(FittyRoutes.Welcome) {
                        popUpTo(FittyRoutes.Splash) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onOpenSignIn = {
                    navController.navigate(FittyRoutes.SignIn) {
                        popUpTo(FittyRoutes.Splash) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onOpenOnboarding = {
                    navController.navigate(FittyRoutes.Onboarding) {
                        popUpTo(FittyRoutes.Splash) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onOpenMain = {
                    navController.navigate(FittyRoutes.Main) {
                        popUpTo(FittyRoutes.Splash) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(FittyRoutes.Welcome) {
            WelcomeRoute(
                onCreateAccount = { navController.navigate(FittyRoutes.SignUp) },
                onSignIn = { navController.navigate(FittyRoutes.SignIn) },
                onContinueAsGuest = {
                    navController.navigate(FittyRoutes.Onboarding) {
                        popUpTo(FittyRoutes.Welcome) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(FittyRoutes.SignIn) {
            SignInRoute(
                onBack = { /* No back from root sign-in screen */ },
                onCreateAccount = { navController.navigate(FittyRoutes.SignUp) },
                onSignedIn = { onboardingCompleted ->
                    navController.navigate(
                        if (onboardingCompleted) FittyRoutes.Main else FittyRoutes.Onboarding
                    ) {
                        popUpTo(FittyRoutes.SignIn) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onContinueAsGuest = {
                    navController.navigate(FittyRoutes.Onboarding) {
                        popUpTo(FittyRoutes.SignIn) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onForgotPassword = {
                    navController.navigate(FittyRoutes.ForgotPassword)
                }
            )
        }
        composable(FittyRoutes.ForgotPassword) {
            ForgotPasswordRoute(
                onBack = { navController.popBackStack() }
            )
        }
        composable(FittyRoutes.SignUp) {
            SignUpRoute(
                onBack = { navController.popBackStack() },
                onSignedUp = { onboardingCompleted ->
                    navController.navigate(
                        if (onboardingCompleted) FittyRoutes.Main else FittyRoutes.Onboarding
                    ) {
                        popUpTo(FittyRoutes.SignIn) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onContinueAsGuest = {
                    navController.navigate(FittyRoutes.Onboarding) {
                        popUpTo(FittyRoutes.SignIn) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(FittyRoutes.Onboarding) {
            OnboardingRoute(
                onExit = {
                    if (!navController.popBackStack()) {
                        navController.navigate(FittyRoutes.SignIn) {
                            launchSingleTop = true
                        }
                    }
                },
                onFinished = {
                    navController.navigate(FittyRoutes.PlanPreview)
                }
            )
        }
        composable(FittyRoutes.PlanPreview) {
            PlanPreviewRoute(
                onBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate(FittyRoutes.Onboarding) {
                            launchSingleTop = true
                        }
                    }
                },
                onStartPlan = {
                    navController.navigate(FittyRoutes.Main) {
                        popUpTo(FittyRoutes.SignIn) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onAdjustPreferences = {
                    if (!navController.popBackStack()) {
                        navController.navigate(FittyRoutes.Onboarding) {
                            launchSingleTop = true
                        }
                    }
                }
            )
        }
        composable(FittyRoutes.Main) {
            MainScaffold(
                onLogout = {
                    navController.navigate(FittyRoutes.SignIn) {
                        popUpTo(FittyRoutes.Main) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onStartQuickWorkout = {
                    navController.navigate(FittyRoutes.workoutSession("quick"))
                },
                onStartScheduledWorkout = { planId, scheduledWorkoutId ->
                    navController.navigate(
                        FittyRoutes.workoutSession(
                            sessionId = "scheduled",
                            planId = planId,
                            scheduledWorkoutId = scheduledWorkoutId
                        )
                    )
                },
                onOpenWorkoutDetails = { planId, scheduledWorkoutId ->
                    navController.navigate(
                        FittyRoutes.workoutDetails(
                            planId = planId,
                            scheduledWorkoutId = scheduledWorkoutId
                        )
                    )
                }
            )
        }
        composable(
            route = FittyRoutes.WorkoutDetails,
            arguments = listOf(
                navArgument("planId") { type = NavType.StringType },
                navArgument("scheduledWorkoutId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val planId = backStackEntry.arguments?.getString("planId") ?: ""
            val scheduledWorkoutId = backStackEntry.arguments?.getString("scheduledWorkoutId") ?: ""
            WorkoutDetailsRoute(
                planId = planId,
                scheduledWorkoutId = scheduledWorkoutId,
                onBack = { navController.popBackStack() },
                onStartWorkout = {
                    navController.navigate(
                        FittyRoutes.workoutSession(
                            sessionId = "scheduled",
                            planId = planId,
                            scheduledWorkoutId = scheduledWorkoutId
                        )
                    )
                },
                onOpenExercise = { exerciseId ->
                    navController.navigate(FittyRoutes.exerciseDetail(exerciseId))
                }
            )
        }
        composable(
            route = FittyRoutes.ExerciseDetail,
            arguments = listOf(navArgument("exerciseId") { type = NavType.StringType })
        ) {
            ExerciseDetailRoute(
                onBack = { navController.popBackStack() },
                onPlayVideo = { exerciseId ->
                    navController.navigate(FittyRoutes.exerciseVideo(exerciseId))
                }
            )
        }
        composable(
            route = FittyRoutes.ExerciseVideo,
            arguments = listOf(navArgument("exerciseId") { type = NavType.StringType })
        ) {
            ExerciseVideoPlayerRoute(onBack = { navController.popBackStack() })
        }
        composable(
            route = FittyRoutes.WorkoutSession,
            arguments = listOf(
                navArgument("sessionId") { type = NavType.StringType },
                navArgument("planId") { type = NavType.StringType; defaultValue = "" },
                navArgument("scheduledWorkoutId") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
            val planId = backStackEntry.arguments?.getString("planId") ?: ""
            val scheduledWorkoutId = backStackEntry.arguments?.getString("scheduledWorkoutId") ?: ""
            WorkoutSessionRoute(
                sessionId = sessionId,
                planId = planId,
                scheduledWorkoutId = scheduledWorkoutId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
