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
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Schedule
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
import com.example.fitty.core.ui.toDisplaySummary
import com.example.fitty.data.content.StarterPlanBuilder
import com.example.fitty.domain.repository.PlanRepository
import com.example.fitty.domain.usecase.onboarding.CompleteOnboardingUseCase
import com.example.fitty.domain.usecase.user.GetCurrentUserUseCase
import com.example.fitty.domain.repository.SessionRepository
import com.example.fitty.ui.theme.FittyGradientEnd
import com.example.fitty.ui.theme.FittyGradientStart
import com.example.fitty.ui.theme.FittyPink
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import android.content.Context
import javax.inject.Inject

data class PlanPreviewDetailUi(
    val icon: ImageVector,
    val title: String,
    val body: String
)

data class PlanPreviewExerciseUi(
    val name: String,
    val prescription: String
)

data class PlanPreviewUiState(
    val title: String = "",
    val subtitle: String = "",
    val details: List<PlanPreviewDetailUi> = emptyList(),
    val exercises: List<PlanPreviewExerciseUi> = emptyList(),
    val errorMessage: String? = null
)

@HiltViewModel
class PlanPreviewViewModel @Inject constructor(
    private val completeOnboardingUseCase: CompleteOnboardingUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val sessionRepository: SessionRepository,
    private val planRepository: PlanRepository,
    private val starterPlanBuilder: StarterPlanBuilder,
    @ApplicationContext private val appContext: Context
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        PlanPreviewUiState(
            title = "Fitty Plan Preview",
            subtitle = "Preview based on your onboarding answers.",
            details = emptyList()
        )
    )
    val uiState: StateFlow<PlanPreviewUiState> = _uiState

    init {
        refreshPreview()
    }

    fun startPlan(onComplete: () -> Unit) {
        viewModelScope.launch {
            completeOnboardingUseCase()
                .onSuccess { onComplete() }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            errorMessage = error.message ?: appContext.getString(R.string.onboarding_save_failed)
                        )
                    }
                }
        }
    }

    private fun refreshPreview() {
        viewModelScope.launch {
            val user = getCurrentUserUseCase() ?: return@launch
            val uid = sessionRepository.getCurrentUserId()
            val language = sessionRepository.getAppLanguage().orEmpty().ifBlank { java.util.Locale.getDefault().language }
            val persistedPlan = uid?.let { planRepository.getPlanInstance(it, STARTER_PLAN_ID) }
            val preview = if (persistedPlan != null &&
                (persistedPlan.previewTitle.isNotBlank() || persistedPlan.previewDetails.isNotEmpty())
            ) {
                com.example.fitty.domain.model.StarterPlanPreviewContent(
                    title = persistedPlan.previewTitle.ifBlank { persistedPlan.name },
                    subtitle = persistedPlan.previewSubtitle,
                    details = persistedPlan.previewDetails,
                    exercises = persistedPlan.previewExercises
                )
            } else {
                starterPlanBuilder.buildForUser(user, language).preview
            }
            _uiState.update {
                PlanPreviewUiState(
                    title = preview.title,
                    subtitle = preview.subtitle,
                    details = preview.details.map { detail ->
                        PlanPreviewDetailUi(
                            icon = detail.iconKey.toPreviewIcon(),
                            title = detail.title,
                            body = detail.body
                        )
                    },
                    exercises = preview.exercises.map { exercise ->
                        PlanPreviewExerciseUi(exercise.name, exercise.toDisplaySummary(appContext))
                    },
                    errorMessage = null
                )
            }
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
        if (state.exercises.isNotEmpty()) {
            item {
                PlanExercisePreviewCard(exercises = state.exercises)
            }
        }
        state.errorMessage?.let { error ->
            item {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        item { Spacer(modifier = Modifier.height(8.dp)) }
        item { FittyPrimaryButton(text = stringResource(R.string.plan_preview_start_plan), onClick = onStartPlan) }
        item { FittySecondaryButton(text = stringResource(R.string.plan_preview_adjust_preferences), onClick = onAdjustPreferences) }
        item { FittySecondaryButton(text = stringResource(R.string.plan_preview_back_to_onboarding), onClick = onBack) }
    }
}

@Composable
private fun PlanExercisePreviewCard(exercises: List<PlanPreviewExerciseUi>) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.FitnessCenter, contentDescription = null, tint = FittyPink)
                Text(
                    stringResource(R.string.plan_preview_exercises_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            exercises.forEach { exercise ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(exercise.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        exercise.prescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
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

private fun String.toPreviewIcon(): ImageVector = when (this) {
    "goal" -> Icons.Outlined.TrackChanges
    "calories" -> Icons.Outlined.Restaurant
    "workout_days" -> Icons.Outlined.CalendarMonth
    "duration" -> Icons.Outlined.Schedule
    else -> Icons.Outlined.Lightbulb
}

private const val STARTER_PLAN_ID = "starter_plan"
