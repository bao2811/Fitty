package com.example.fitty.feature_onboarding

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fitty.core.designsystem.component.FittyInfoCard
import com.example.fitty.core.designsystem.component.FittyPrimaryButton
import com.example.fitty.core.designsystem.component.FittySecondaryButton
import com.example.fitty.core.ui.FittyLazyScreen
import com.example.fitty.data.firebase.FittyFirebaseRepository
import com.example.fitty.data.preferences.AppPreferencesDataSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PlanPreviewViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = AppPreferencesDataSource(application.applicationContext)
    private val repository = FittyFirebaseRepository()

    fun startPlan(onComplete: () -> Unit) {
        viewModelScope.launch {
            preferences.currentUserId.first()?.let { uid ->
                repository.markOnboardingCompleted(uid)
            }
            preferences.setOnboardingCompleted(true)
            onComplete()
        }
    }
}

@Composable
fun PlanPreviewRoute(
    onBack: () -> Unit,
    onStartPlan: () -> Unit,
    onAdjustPreferences: () -> Unit,
    viewModel: PlanPreviewViewModel = viewModel()
) {
    BackHandler(onBack = onBack)

    PlanPreviewScreen(
        onBack = onBack,
        onStartPlan = { viewModel.startPlan(onStartPlan) },
        onAdjustPreferences = onAdjustPreferences
    )
}

@Composable
fun PlanPreviewScreen(
    onBack: () -> Unit,
    onStartPlan: () -> Unit,
    onAdjustPreferences: () -> Unit
) {
    FittyLazyScreen {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onBack) {
                    Text("Back")
                }
                TextButton(onClick = onAdjustPreferences) {
                    Text("Adjust")
                }
            }
        }
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
        item {
            FittySecondaryButton(text = "Back to onboarding", onClick = onBack)
        }
    }
}
