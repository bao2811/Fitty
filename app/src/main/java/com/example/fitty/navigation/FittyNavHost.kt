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
import com.example.fitty.feature_entry.WelcomeRoute
import com.example.fitty.feature_onboarding.OnboardingRoute
import com.example.fitty.feature_onboarding.PlanPreviewRoute
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
                onBack = { navController.popBackStack() },
                onCreateAccount = { navController.navigate(FittyRoutes.SignUp) },
                onSignedIn = { onboardingCompleted ->
                    navController.navigate(
                        if (onboardingCompleted) FittyRoutes.Main else FittyRoutes.Onboarding
                    ) {
                        popUpTo(FittyRoutes.Welcome) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onContinueAsGuest = {
                    navController.navigate(FittyRoutes.Onboarding) {
                        popUpTo(FittyRoutes.Welcome) { inclusive = true }
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
                        popUpTo(FittyRoutes.Welcome) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onContinueAsGuest = {
                    navController.navigate(FittyRoutes.Onboarding) {
                        popUpTo(FittyRoutes.Welcome) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(FittyRoutes.Onboarding) {
            OnboardingRoute(
                onExit = {
                    if (!navController.popBackStack()) {
                        navController.navigate(FittyRoutes.Welcome) {
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
                        popUpTo(FittyRoutes.Welcome) { inclusive = true }
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
                    navController.navigate(FittyRoutes.Welcome) {
                        popUpTo(FittyRoutes.Main) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(
            route = FittyRoutes.WorkoutSession,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
            WorkoutSessionRoute(
                sessionId = sessionId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
