package com.example.fitty.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.navigation.compose.rememberNavController
import com.example.fitty.notifications.FittyInAppBannerHost

@Composable
fun FittyApp() {
    val navController = rememberNavController()
    Box {
        FittyNavHost(navController = navController)
        Box(contentAlignment = Alignment.TopCenter) {
            FittyInAppBannerHost()
        }
    }
}
