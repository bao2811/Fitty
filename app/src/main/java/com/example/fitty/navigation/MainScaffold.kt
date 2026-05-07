package com.example.fitty.navigation

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.fitty.feature_coach.CoachRoute
import com.example.fitty.feature_home.HomeRoute
import com.example.fitty.feature_plan.PlanRoute
import com.example.fitty.feature_profile.ProfileRoute
import com.example.fitty.feature_track.TrackRoute
import com.example.fitty.ui.theme.FittyPink

@Composable
fun MainScaffold(onLogout: () -> Unit) {
    val tabNavController = rememberNavController()
    val backStackEntry by tabNavController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
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
                            tabNavController.navigate(tab.route) {
                                popUpTo(tabNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { MainTabIcon(tab.route) },
                        label = {
                            Text(
                                tab.label,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                style = MaterialTheme.typography.labelSmall
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
    ) { innerPadding ->
        NavHost(
            navController = tabNavController,
            startDestination = MainRoutes.Home,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(MainRoutes.Home) {
                HomeRoute()
            }
            composable(MainRoutes.Plan) {
                PlanRoute()
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
