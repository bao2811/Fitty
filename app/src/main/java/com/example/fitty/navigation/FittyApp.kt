package com.example.fitty.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController

@Composable
fun FittyApp() {
    val navController = rememberNavController()
    FittyNavHost(navController = navController)
}
