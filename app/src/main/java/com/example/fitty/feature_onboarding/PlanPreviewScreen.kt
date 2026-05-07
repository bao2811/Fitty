package com.example.fitty.feature_onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitty.R
import com.example.fitty.core.designsystem.component.FittyPrimaryButton
import com.example.fitty.core.designsystem.component.FittySecondaryButton
import com.example.fitty.core.ui.FittyLazyScreen
import com.example.fitty.domain.model.FittyUser
import com.example.fitty.domain.usecase.onboarding.CompleteOnboardingUseCase
import com.example.fitty.domain.usecase.user.GetCurrentUserUseCase
import com.example.fitty.ui.theme.FittyGradientEnd
import com.example.fitty.ui.theme.FittyGradientStart
import com.example.fitty.ui.theme.FittyPink
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

data class PlanPreviewDetailUi(
    val icon: ImageVector,
    val title: String,
    val body: String
)

data class PlanPreviewUiState(
    val title: String = "Your Fitty Starter Plan",
    val subtitle: String = "Personalized based on your answers",
    val details: List<PlanPreviewDetailUi> = defaultPlanPreviewDetails()
)

@HiltViewModel
class PlanPreviewViewModel @Inject constructor(
    private val completeOnboardingUseCase: CompleteOnboardingUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(PlanPreviewUiState())
    val uiState: StateFlow<PlanPreviewUiState> = _uiState

    init {
        refreshPreview()
    }

    fun startPlan(onComplete: () -> Unit) {
        viewModelScope.launch {
            completeOnboardingUseCase()
            onComplete()
        }
    }

    private fun refreshPreview() {
        viewModelScope.launch {
            val user = getCurrentUserUseCase() ?: return@launch
            _uiState.update { user.toPlanPreviewUiState() }
        }
    }
}

@Composable
fun PlanPreviewRoute(
    onBack: () -> Unit,
    onStartPlan: () -> Unit,
    onAdjustPreferences: () -> Unit,
    viewModel: PlanPreviewViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    BackHandler(onBack = onBack)
    PlanPreviewScreen(
        state = state,
        onBack = onBack,
        onStartPlan = { viewModel.startPlan(onStartPlan) },
        onAdjustPreferences = onAdjustPreferences
    )
}

@Composable
fun PlanPreviewScreen(
    state: PlanPreviewUiState,
    onBack: () -> Unit,
    onStartPlan: () -> Unit,
    onAdjustPreferences: () -> Unit
) {
    FittyLazyScreen {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onBack) { Text(stringResource(R.string.plan_preview_back), color = FittyPink, fontWeight = FontWeight.SemiBold) }
                TextButton(onClick = onAdjustPreferences) { Text(stringResource(R.string.plan_preview_adjust), color = FittyPink, fontWeight = FontWeight.SemiBold) }
            }
        }
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .background(Brush.horizontalGradient(listOf(FittyGradientStart, FittyGradientEnd)))
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(state.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(state.subtitle, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f))
                }
            }
        }
        state.details.forEach { detail ->
            item { PlanDetailCard(detail = detail) }
        }
        item { Spacer(modifier = Modifier.height(8.dp)) }
        item { FittyPrimaryButton(text = stringResource(R.string.plan_preview_start_plan), onClick = onStartPlan) }
        item { FittySecondaryButton(text = stringResource(R.string.plan_preview_adjust_preferences), onClick = onAdjustPreferences) }
        item { FittySecondaryButton(text = stringResource(R.string.plan_preview_back_to_onboarding), onClick = onBack) }
    }
}

@Composable
private fun PlanDetailCard(detail: PlanPreviewDetailUi) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(FittyPink.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(detail.icon, contentDescription = null, tint = FittyPink, modifier = Modifier.size(22.dp))
            }
            Column {
                Text(detail.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    detail.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

private fun FittyUser.toPlanPreviewUiState(): PlanPreviewUiState {
    val goalLabel = profile.primaryGoal.toDisplayLabel("Balanced Progress")
    val calorieTarget = when {
        profile.weightKg == null -> "Start with a practical daily target and adjust after the first week."
        profile.primaryGoal == "gain_muscle" -> "${profile.weightKg * 34} kcal target to support muscle gain."
        profile.primaryGoal == "lose_weight" -> "${profile.weightKg * 28} kcal target to create a steady deficit."
        else -> "${profile.weightKg * 30} kcal target to support consistent training."
    }
    val schedule = onboarding.workoutDays
        .ifEmpty { listOf("mon", "wed", "fri", "sat") }
        .mapIndexed { index, day ->
            "${day.toDisplayLabel(day)}: ${starterWorkoutName(index)}"
        }
        .joinToString("\n")
    val why = buildString {
        append("This first version matches your ")
        append(profile.fitnessLevel.toDisplayLabel("current level").lowercase(Locale.US))
        append(" level, ")
        append(onboarding.equipmentAccess.toDisplayLabel("available setup").lowercase(Locale.US))
        append(", and preferred ")
        append(onboarding.preferredTime.toDisplayLabel("training time").lowercase(Locale.US))
        append(" rhythm.")
    }

    return PlanPreviewUiState(
        title = "$goalLabel Starter Plan",
        subtitle = "Personalized from your onboarding profile and ready to activate.",
        details = listOf(
            PlanPreviewDetailUi(
                icon = Icons.Outlined.TrackChanges,
                title = "Goal",
                body = "Your first week is tuned for $goalLabel with manageable intensity and a clear routine."
            ),
            PlanPreviewDetailUi(
                icon = Icons.Outlined.Restaurant,
                title = "Calories target",
                body = calorieTarget
            ),
            PlanPreviewDetailUi(
                icon = Icons.Outlined.CalendarMonth,
                title = "Your first week",
                body = schedule
            ),
            PlanPreviewDetailUi(
                icon = Icons.Outlined.Lightbulb,
                title = "Why this plan?",
                body = why
            )
        )
    )
}

private fun defaultPlanPreviewDetails(): List<PlanPreviewDetailUi> = listOf(
    PlanPreviewDetailUi(Icons.Outlined.TrackChanges, "Goal", "A balanced first week built from your onboarding choices."),
    PlanPreviewDetailUi(Icons.Outlined.Restaurant, "Calories target", "Start with a practical daily target and adjust after the first week."),
    PlanPreviewDetailUi(Icons.Outlined.CalendarMonth, "Your first week", "Monday: Full body\nWednesday: Cardio + core\nFriday: Strength\nSaturday: Mobility"),
    PlanPreviewDetailUi(Icons.Outlined.Lightbulb, "Why this plan?", "The first version keeps intensity manageable, gives recovery space, and leaves room for meal tracking.")
)

private fun starterWorkoutName(index: Int): String = when (index % 4) {
    0 -> "Full Body Basics"
    1 -> "Cardio + Core"
    2 -> "Strength Foundations"
    else -> "Mobility Reset"
}

private fun String.toDisplayLabel(defaultValue: String): String {
    if (isBlank()) return defaultValue
    return split('_', ' ')
        .filter { it.isNotBlank() }
        .joinToString(" ") { part ->
            part.replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase(Locale.US) else char.toString()
            }
        }
}
