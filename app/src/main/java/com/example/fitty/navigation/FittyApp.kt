package com.example.fitty.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.example.fitty.notifications.FittyInAppBannerHost

@Composable
fun FittyApp() {
    val navController = rememberNavController()
    val viewModel: FittyAppViewModel = hiltViewModel()
    val banner by viewModel.banner.collectAsState()
    Box {
        FittyNavHost(navController = navController)
        Box(contentAlignment = Alignment.TopCenter) {
            FittyInAppBannerHost(
                banner = banner,
                onDismiss = viewModel::dismissBanner
            )
        }
    }
}
