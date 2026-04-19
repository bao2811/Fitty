package com.example.fitty.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.fitty.feature_coach.CoachScreen
import com.example.fitty.feature_home.HomeScreen
import com.example.fitty.feature_plan.PlanScreen
import com.example.fitty.feature_profile.ProfileRoute
import com.example.fitty.feature_track.TrackScreen

@Composable
fun MainScaffold(onLogout: () -> Unit) {
    val tabNavController = rememberNavController()
    val backStackEntry by tabNavController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                MainTabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            tabNavController.navigate(tab.route) {
                                popUpTo(tabNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { MainTabIcon(tab.route) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = tabNavController,
            startDestination = MainRoutes.Home,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(MainRoutes.Home) {
                HomeScreen()
            }
            composable(MainRoutes.Plan) {
                PlanScreen()
            }
            composable(MainRoutes.Track) {
                TrackScreen()
            }
            composable(MainRoutes.Coach) {
                CoachScreen()
            }
            composable(MainRoutes.Profile) {
                ProfileRoute(onLogout = onLogout)
            }
        }
    }
}

@Composable
private fun MainTabIcon(route: String) {
    val icon = when (route) {
        MainRoutes.Home -> Icons.Outlined.Home
        MainRoutes.Plan -> Icons.Outlined.CalendarMonth
        MainRoutes.Track -> Icons.Outlined.Restaurant
        MainRoutes.Coach -> Icons.Outlined.Chat
        else -> Icons.Outlined.Person
    }
    Icon(imageVector = icon, contentDescription = null)
}
