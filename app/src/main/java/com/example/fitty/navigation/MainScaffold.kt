package com.example.fitty.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.fitty.feature_coach.CoachRoute
import com.example.fitty.feature_home.HomeRoute
import com.example.fitty.feature_plan.CategoryExerciseListRoute
import com.example.fitty.feature_plan.PlanRoute
import com.example.fitty.feature_profile.ProfileRoute
import com.example.fitty.feature_track.TrackRoute
import com.example.fitty.ui.theme.FittyPink
import kotlinx.coroutines.delay

private const val AUTO_HIDE_DELAY_MS = 10_000L
private const val SCROLL_THRESHOLD = 5f

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MainScaffold(
    onLogout: () -> Unit,
    onStartQuickWorkout: () -> Unit = {},
    onStartScheduledWorkout: (planId: String, scheduledWorkoutId: String) -> Unit = { _, _ -> },
    onOpenWorkoutDetails: (planId: String, scheduledWorkoutId: String) -> Unit = { _, _ -> }
) {
    val tabNavController = rememberNavController()
    val backStackEntry by tabNavController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute == null || MainTabs.any { it.route == currentRoute }

    // ── Bottom bar visibility state ──
    var isBarVisible by remember { mutableStateOf(true) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Reset interaction time & show bar on any touch
    fun onUserInteraction() {
        lastInteractionTime = System.currentTimeMillis()
        if (!isBarVisible) isBarVisible = true
    }

    // Auto-hide after 10 seconds of inactivity
    LaunchedEffect(lastInteractionTime) {
        delay(AUTO_HIDE_DELAY_MS)
        if (showBottomBar) {
            isBarVisible = false
        }
    }

    // Show bar when navigating between tabs
    LaunchedEffect(currentRoute) {
        isBarVisible = true
        lastInteractionTime = System.currentTimeMillis()
    }

    // Nested scroll connection: scroll down → hide, scroll up → show
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val dy = available.y
                if (dy < -SCROLL_THRESHOLD) {
                    // Scrolling down → hide
                    isBarVisible = false
                } else if (dy > SCROLL_THRESHOLD) {
                    // Scrolling up → show
                    isBarVisible = true
                    lastInteractionTime = System.currentTimeMillis()
                }
                return Offset.Zero // Don't consume any scroll
            }
        }
    }

    val navigateToTab: (String) -> Unit = { route ->
        tabNavController.navigate(route) {
            popUpTo(tabNavController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar && isBarVisible,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                    modifier = Modifier
                        .shadow(
                            elevation = 16.dp,
                            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                            ambientColor = Color.Black.copy(alpha = 0.08f),
                            spotColor = Color.Black.copy(alpha = 0.12f)
                        )
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                ) {
                    MainTabs.forEach { tab ->
                        val selected = currentRoute == tab.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                onUserInteraction()
                                navigateToTab(tab.route)
                            },
                            icon = { MainTabIcon(tab.route) },
                            label = {
                                Text(
                                    text = stringResource(tab.labelRes),
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = FittyPink,
                                selectedTextColor = FittyPink,
                                indicatorColor = FittyPink.copy(alpha = 0.12f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .nestedScroll(nestedScrollConnection)
                .pointerInteropFilter {
                    onUserInteraction()
                    false // Don't consume the event
                }
        ) {
            NavHost(
                navController = tabNavController,
                startDestination = MainRoutes.Home
            ) {
                composable(MainRoutes.Home) {
                    HomeRoute(
                        onNavigateToPlan = { planId, scheduledWorkoutId ->
                            if (planId.isNotBlank() && scheduledWorkoutId.isNotBlank()) {
                                onOpenWorkoutDetails(planId, scheduledWorkoutId)
                            } else {
                                navigateToTab(MainRoutes.Plan)
                            }
                        },
                        onNavigateToTrack = { navigateToTab(MainRoutes.Track) },
                        onNavigateToCoach = { navigateToTab(MainRoutes.Coach) },
                        onStartWorkout = { planId, scheduledWorkoutId ->
                            if (planId.isNotBlank() && scheduledWorkoutId.isNotBlank()) {
                                onStartScheduledWorkout(planId, scheduledWorkoutId)
                            } else {
                                onStartQuickWorkout()
                            }
                        }
                    )
                }
                composable(MainRoutes.Plan) {
                    PlanRoute(
                        onCategorySelected = { categoryId, categoryLabel, bodyPartKeys ->
                            tabNavController.navigate(
                                MainRoutes.categoryExerciseList(categoryId, categoryLabel, bodyPartKeys)
                            )
                        },
                        onStartQuickWorkout = onStartQuickWorkout
                    )
                }
                composable(
                    route = MainRoutes.CategoryExerciseList,
                    arguments = listOf(
                        navArgument("categoryId") { type = NavType.StringType },
                        navArgument("categoryLabel") { type = NavType.StringType },
                        navArgument("bodyPartKeys") { type = NavType.StringType }
                    )
                ) {
                    CategoryExerciseListRoute(
                        onBack = { tabNavController.popBackStack() }
                    )
                }
                composable(MainRoutes.Track) {
                    TrackRoute()
                }
                composable(MainRoutes.Coach) {
                    CoachRoute()
                }
                composable(MainRoutes.Profile) {
                    ProfileRoute(onLogout = onLogout)
                }
            }
        }
    }
}

@Composable
private fun MainTabIcon(route: String) {
    val icon = when (route) {
        MainRoutes.Home -> Icons.Outlined.Home
        MainRoutes.Plan -> Icons.Outlined.FitnessCenter
        MainRoutes.Track -> Icons.Outlined.Restaurant
        MainRoutes.Coach -> Icons.AutoMirrored.Outlined.Chat
        else -> Icons.Outlined.Person
    }
    Icon(imageVector = icon, contentDescription = null)
}
