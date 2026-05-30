package com.example.fitty

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import com.example.fitty.core.ui.AppLocaleManager
import com.example.fitty.data.exercise.ExerciseSyncScheduler
import com.example.fitty.data.preferences.AppPreferencesDataSource
import com.example.fitty.navigation.FittyApp
import com.example.fitty.notifications.FittyNotificationManager
import com.example.fitty.ui.theme.FittyTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var exerciseSyncScheduler: ExerciseSyncScheduler
    @Inject
    lateinit var preferences: AppPreferencesDataSource

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLocaleManager.applyLanguage(
            runBlocking { preferences.appLanguage() }
        )
        FittyNotificationManager.createChannels(this)
        requestNotificationPermissionIfNeeded()
        exerciseSyncScheduler.enqueueStartupSync()
        exerciseSyncScheduler.enqueuePeriodicSync()
        enableEdgeToEdge()
        setContent {
            FittyTheme {
                FittyApp()
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIFICATIONS)
        }
    }

    private companion object {
        const val REQUEST_NOTIFICATIONS = 1001
    }
}
