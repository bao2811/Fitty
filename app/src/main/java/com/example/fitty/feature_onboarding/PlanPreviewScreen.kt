package com.example.fitty.feature_onboarding

import android.app.Application
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fitty.core.designsystem.component.FittyInfoCard
import com.example.fitty.core.designsystem.component.FittyPrimaryButton
import com.example.fitty.core.designsystem.component.FittySecondaryButton
import com.example.fitty.core.ui.FittyLazyScreen
import com.example.fitty.data.preferences.AppPreferencesDataSource
import kotlinx.coroutines.launch

class PlanPreviewViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = AppPreferencesDataSource(application.applicationContext)

    fun startPlan(onComplete: () -> Unit) {
        viewModelScope.launch {
            preferences.setOnboardingCompleted(true)
            onComplete()
        }
    }
}

@Composable
fun PlanPreviewRoute(
    onStartPlan: () -> Unit,
    onAdjustPreferences: () -> Unit,
    viewModel: PlanPreviewViewModel = viewModel()
) {
    PlanPreviewScreen(
        onStartPlan = { viewModel.startPlan(onStartPlan) },
        onAdjustPreferences = onAdjustPreferences
    )
}

@Composable
fun PlanPreviewScreen(
    onStartPlan: () -> Unit,
    onAdjustPreferences: () -> Unit
) {
    FittyLazyScreen {
        item {
            Text(
                text = "Your Fitty starter plan",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
        item {
            FittyInfoCard(
                title = "Goal",
                body = "A balanced first week built from your onboarding choices."
            )
        }
        item {
            FittyInfoCard(
                title = "Calories target",
                body = "Start with a practical daily target and adjust after the first week."
            )
        }
        item {
            FittyInfoCard(
                title = "Your first week",
                body = "Monday: Full body\nWednesday: Cardio + core\nFriday: Strength\nSaturday: Mobility"
            )
        }
        item {
            FittyInfoCard(
                title = "Why this plan?",
                body = "The first version keeps intensity manageable, gives recovery space, and leaves room for meal tracking."
            )
        }
        item { Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp)) }
        item {
            FittyPrimaryButton(text = "Start My Plan", onClick = onStartPlan)
        }
        item {
            FittySecondaryButton(text = "Adjust preferences", onClick = onAdjustPreferences)
        }
    }
}
