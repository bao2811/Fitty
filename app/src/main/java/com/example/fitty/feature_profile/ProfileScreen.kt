package com.example.fitty.feature_profile

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fitty.core.designsystem.component.FittyInfoCard
import com.example.fitty.core.designsystem.component.FittyPrimaryButton
import com.example.fitty.core.ui.FittyLazyScreen
import com.example.fitty.data.preferences.AppPreferencesDataSource
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = AppPreferencesDataSource(application.applicationContext)

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            preferences.clearSession()
            onComplete()
        }
    }
}

@Composable
fun ProfileRoute(
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    ProfileScreen(onLogout = { viewModel.logout(onLogout) })
}

@Composable
fun ProfileScreen(onLogout: () -> Unit) {
    FittyLazyScreen {
        item {
            Text(
                text = "Profile",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
        item { FittyInfoCard("Current Goal", "Personalized plan active.") }
        item { FittyInfoCard("Body Metrics", "Update age, height, weight, and target weight later.") }
        item { FittyInfoCard("Reminder Settings", "Workout, meal, water, and sleep reminders.") }
        item { FittyInfoCard("Nutrition Preferences", "Eating style and restrictions.") }
        item { FittyInfoCard("Achievements", "No achievements unlocked yet.") }
        item { FittyInfoCard("Linked Health Apps", "Health Connect will be requested only when you connect it.") }
        item { FittyInfoCard("Privacy & AI", "Manage body scan consent, chat history, and AI preferences.") }
        item { FittyInfoCard("Theme / Units", "System theme, kilograms, and centimeters.") }
        item { FittyPrimaryButton(text = "Log Out", onClick = onLogout) }
    }
}
