package com.example.fitty.feature_onboarding

import android.content.Context
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitty.BuildConfig
import com.example.fitty.R
import com.example.fitty.core.designsystem.component.FittyChoiceCard
import com.example.fitty.core.designsystem.component.FittyPrimaryButton
import com.example.fitty.core.designsystem.component.FittySectionBlock
import com.example.fitty.core.designsystem.component.FittySecondaryButton
import com.example.fitty.core.ui.ContentDebugSource
import com.example.fitty.core.ui.ContentDiagnosticsCard
import com.example.fitty.core.ui.ContentSourceState
import com.example.fitty.core.ui.FittyLazyScreen
import com.example.fitty.data.content.LocalContentFallbacks
import com.example.fitty.domain.model.OnboardingChoiceContent
import com.example.fitty.domain.model.OnboardingContentConfig
import com.example.fitty.domain.repository.ContentRepository
import com.example.fitty.domain.repository.SessionRepository
import com.example.fitty.domain.model.FittyOnboardingAnswers
import com.example.fitty.domain.usecase.onboarding.SaveOnboardingAnswersUseCase
import com.example.fitty.ui.theme.FittyPink
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TotalSteps = 9
private const val LastStep = TotalSteps - 1

data class OnboardingUiState(
    val step: Int = 0, val goal: String = "", val age: String = "", val height: String = "",
    val weight: String = "", val targetWeight: String = "", val fitnessLevel: String = "",
    val workoutDays: Set<String> = emptySet(), val duration: String = "", val preferredTime: String = "",
    val equipment: String = "", val injuryNote: String = "", val nutrition: String = "",
    val restrictions: Set<String> = emptySet(), val reminders: Set<String> = emptySet(),
    val errorMessage: String? = null,
    val content: OnboardingContentConfig? = null,
    val contentSources: List<ContentDebugSource> = emptyList()
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val saveOnboardingAnswersUseCase: SaveOnboardingAnswersUseCase,
    private val localContentFallbacks: LocalContentFallbacks,
    private val contentRepository: ContentRepository,
    private val sessionRepository: SessionRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val fallbackContent = localContentFallbacks.onboarding(java.util.Locale.getDefault().language)
    private val _uiState = MutableStateFlow(
        OnboardingUiState(
            content = fallbackContent,
            contentSources = listOf(
                ContentDebugSource("Onboarding content", ContentSourceState.Fallback, "Using local fallback until remote load completes")
            )
        )
    )
    val uiState: StateFlow<OnboardingUiState> = _uiState

    init {
        loadContent()
    }

    private fun loadContent() {
        viewModelScope.launch {
            val language = sessionRepository.getAppLanguage().orEmpty().ifBlank { java.util.Locale.getDefault().language }
            val content = contentRepository.getOnboardingContent(language)
            val usedFallback = contentRepository.usedFallbackFor("onboarding_content")
            _uiState.update {
                it.copy(
                    content = content,
                    contentSources = listOf(
                        ContentDebugSource(
                            "Onboarding content",
                            if (usedFallback) ContentSourceState.Fallback else ContentSourceState.Remote,
                            contentRepository.fallbackDetailFor("onboarding_content")
                                ?: if (usedFallback) "Using local fallback" else "Loaded language=$language from Firebase"
                        )
                    )
                )
            }
        }
    }

    fun selectGoal(value: String) = update { copy(goal = value) }
    fun updateAge(value: String) = update { copy(age = value.filter(Char::isDigit)) }
    fun updateHeight(value: String) = update { copy(height = value.filter(Char::isDigit)) }
    fun updateWeight(value: String) = update { copy(weight = value.filter(Char::isDigit)) }
    fun updateTargetWeight(value: String) = update { copy(targetWeight = value.filter(Char::isDigit)) }
    fun selectFitnessLevel(value: String) = update { copy(fitnessLevel = value) }
    fun selectDuration(value: String) = update { copy(duration = value) }
    fun selectPreferredTime(value: String) = update { copy(preferredTime = value) }
    fun selectEquipment(value: String) = update { copy(equipment = value) }
    fun updateInjuryNote(value: String) = update { copy(injuryNote = value) }
    fun selectNutrition(value: String) = update { copy(nutrition = value) }
    fun toggleWorkoutDay(value: String) { update { copy(workoutDays = workoutDays.toggle(value)) } }
    fun toggleRestriction(value: String) { update { copy(restrictions = restrictions.toggle(value)) } }
    fun toggleReminder(value: String) { update { copy(reminders = reminders.toggle(value)) } }

    fun back() { _uiState.update { state -> state.copy(step = (state.step - 1).coerceAtLeast(0), errorMessage = null) } }

    fun next(onFinished: () -> Unit) {
        val error = validate(_uiState.value)
        if (error != null) { _uiState.update { it.copy(errorMessage = error) }; return }
        if (_uiState.value.step == LastStep) {
            viewModelScope.launch {
                val result = saveOnboardingAnswersUseCase(_uiState.value.toAnswers())
                result
                    .onSuccess { onFinished() }
                    .onFailure { errorState ->
                        _uiState.update {
                            it.copy(errorMessage = errorState.message ?: context.getString(R.string.onboarding_save_failed))
                        }
                    }
            }
        } else { _uiState.update { it.copy(step = it.step + 1, errorMessage = null) } }
    }

    private fun update(transform: OnboardingUiState.() -> OnboardingUiState) { _uiState.update { it.transform().copy(errorMessage = null) } }

    private fun validate(state: OnboardingUiState): String? = when (state.step) {
        0 -> if (state.goal.isBlank()) context.getString(R.string.onboarding_choose_goal) else null
        1 -> when {
            state.age.toIntOrNull() == null -> context.getString(R.string.onboarding_enter_age)
            state.age.toIntOrNull() !in 13..100 -> context.getString(R.string.onboarding_age_range)
            state.height.toIntOrNull() == null -> context.getString(R.string.onboarding_enter_height)
            state.height.toIntOrNull() !in 100..250 -> context.getString(R.string.onboarding_height_range)
            state.weight.toIntOrNull() == null -> context.getString(R.string.onboarding_enter_weight)
            state.weight.toIntOrNull() !in 30..300 -> context.getString(R.string.onboarding_weight_range)
            state.targetWeight.toIntOrNull() == null -> context.getString(R.string.onboarding_enter_target_weight)
            state.targetWeight.toIntOrNull() !in 30..300 -> context.getString(R.string.onboarding_target_weight_range)
            else -> null
        }
        2 -> if (state.fitnessLevel.isBlank()) context.getString(R.string.onboarding_choose_fitness_level) else null
        3 -> if (state.workoutDays.isEmpty()) context.getString(R.string.onboarding_choose_workout_day) else null
        4 -> if (state.preferredTime.isBlank()) context.getString(R.string.onboarding_choose_preferred_time) else null
        5 -> if (state.duration.isBlank()) context.getString(R.string.onboarding_choose_duration) else null
        6 -> if (state.equipment.isBlank()) context.getString(R.string.onboarding_choose_location) else null
        7 -> if (state.nutrition.isBlank()) context.getString(R.string.onboarding_choose_eating_style) else null
        else -> null
    }

    private fun Set<String>.toggle(value: String): Set<String> = if (contains(value)) this - value else this + value

    private fun OnboardingUiState.toAnswers(): FittyOnboardingAnswers = FittyOnboardingAnswers(
        goal = goal, age = age.toIntOrNull(), heightCm = height.toIntOrNull(), weightKg = weight.toIntOrNull(),
        targetWeightKg = targetWeight.toIntOrNull(), fitnessLevel = fitnessLevel, workoutDays = workoutDays,
        durationMinutes = duration.filter(Char::isDigit).toIntOrNull() ?: 0, preferredTime = preferredTime,
        equipment = equipment, injuryNote = injuryNote, nutrition = nutrition, restrictions = restrictions, reminders = reminders
    )
}

@Composable
fun OnboardingRoute(onExit: () -> Unit, onFinished: () -> Unit, viewModel: OnboardingViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val handleBack = { if (state.step > 0) viewModel.back() else onExit() }
    BackHandler(onBack = handleBack)
    OnboardingScreen(state = state, onGoalSelected = viewModel::selectGoal, onAgeChanged = viewModel::updateAge,
        onHeightChanged = viewModel::updateHeight, onWeightChanged = viewModel::updateWeight,
        onTargetWeightChanged = viewModel::updateTargetWeight, onFitnessLevelSelected = viewModel::selectFitnessLevel,
        onWorkoutDayToggled = viewModel::toggleWorkoutDay, onDurationSelected = viewModel::selectDuration,
        onPreferredTimeSelected = viewModel::selectPreferredTime, onEquipmentSelected = viewModel::selectEquipment,
        onInjuryNoteChanged = viewModel::updateInjuryNote, onNutritionSelected = viewModel::selectNutrition,
        onRestrictionToggled = viewModel::toggleRestriction, onReminderToggled = viewModel::toggleReminder,
        onBack = handleBack, onExit = onExit, onNext = { viewModel.next(onFinished) })
}

@Composable
fun OnboardingScreen(
    state: OnboardingUiState, onGoalSelected: (String) -> Unit, onAgeChanged: (String) -> Unit,
    onHeightChanged: (String) -> Unit, onWeightChanged: (String) -> Unit, onTargetWeightChanged: (String) -> Unit,
    onFitnessLevelSelected: (String) -> Unit, onWorkoutDayToggled: (String) -> Unit, onDurationSelected: (String) -> Unit,
    onPreferredTimeSelected: (String) -> Unit, onEquipmentSelected: (String) -> Unit, onInjuryNoteChanged: (String) -> Unit,
    onNutritionSelected: (String) -> Unit, onRestrictionToggled: (String) -> Unit, onReminderToggled: (String) -> Unit,
    onBack: () -> Unit, onExit: () -> Unit, onNext: () -> Unit
) {
    val content = state.content ?: return
    FittyLazyScreen {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, tint = FittyPink)
                    Text(stringResource(R.string.onboarding_back), color = FittyPink, fontWeight = FontWeight.SemiBold)
                }
                TextButton(onClick = onExit) { Text(stringResource(R.string.onboarding_exit), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.onboarding_step, state.step + 1, TotalSteps), style = MaterialTheme.typography.labelLarge, color = FittyPink, fontWeight = FontWeight.Bold)
                LinearProgressIndicator(
                    progress = { (state.step + 1) / TotalSteps.toFloat() },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = FittyPink, trackColor = FittyPink.copy(alpha = 0.12f)
                )
            }
        }
        item {
            Text(
                text = content.stepTitles.getOrElse(state.step) { content.stepTitles.lastOrNull().orEmpty() },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
        if (BuildConfig.DEBUG && state.contentSources.isNotEmpty()) {
            item {
                ContentDiagnosticsCard(sources = state.contentSources)
            }
        }
        item {
            when (state.step) {
                0 -> ChoiceList(content.goals, state.goal, onGoalSelected)
                1 -> BodyMetricsStep(state, onAgeChanged, onHeightChanged, onWeightChanged, onTargetWeightChanged)
                2 -> ChoiceList(content.fitnessLevels, state.fitnessLevel, onFitnessLevelSelected)
                3 -> WorkoutDaysStep(state, content.workoutDays, onWorkoutDayToggled)
                4 -> WorkoutTimeStep(state, content.preferredTimes, onPreferredTimeSelected)
                5 -> WorkoutDurationStep(state, content.durations, onDurationSelected)
                6 -> EquipmentStep(state, content.equipments, onEquipmentSelected, onInjuryNoteChanged)
                7 -> NutritionStep(state, content.nutritionStyles, content.restrictions, onNutritionSelected, onRestrictionToggled)
                8 -> ReminderStep(state, content.reminders, onReminderToggled)
            }
        }
        item { state.errorMessage?.let { Text(text = it, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium) } }
        item { OnboardingActions(step = state.step, onBack = onBack, onNext = onNext) }
    }
}

@Composable private fun BodyMetricsStep(state: OnboardingUiState, onAgeChanged: (String) -> Unit, onHeightChanged: (String) -> Unit, onWeightChanged: (String) -> Unit, onTargetWeightChanged: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { NumberField(stringResource(R.string.onboarding_age), state.age, onAgeChanged); NumberField(stringResource(R.string.onboarding_height_cm), state.height, onHeightChanged); NumberField(stringResource(R.string.onboarding_weight_kg), state.weight, onWeightChanged); NumberField(stringResource(R.string.onboarding_target_weight_kg), state.targetWeight, onTargetWeightChanged) }
}
@Composable private fun WorkoutDaysStep(state: OnboardingUiState, values: List<OnboardingChoiceContent>, onWorkoutDayToggled: (String) -> Unit) {
    MultiChoiceList(stringResource(R.string.onboarding_training_days), Icons.Outlined.CalendarMonth, values, state.workoutDays, onWorkoutDayToggled)
}
@Composable private fun WorkoutTimeStep(state: OnboardingUiState, values: List<OnboardingChoiceContent>, onPreferredTimeSelected: (String) -> Unit) {
    FittySectionBlock(title = stringResource(R.string.onboarding_preferred_workout_time), icon = Icons.Outlined.Schedule) {
        ChoiceList(values, state.preferredTime, onPreferredTimeSelected)
    }
}
@Composable private fun WorkoutDurationStep(state: OnboardingUiState, values: List<OnboardingChoiceContent>, onDurationSelected: (String) -> Unit) {
    FittySectionBlock(title = stringResource(R.string.onboarding_session_duration), icon = Icons.Outlined.Timer) {
        ChoiceList(values, state.duration, onDurationSelected)
    }
}
@Composable private fun OnboardingActions(step: Int, onBack: () -> Unit, onNext: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        FittySecondaryButton(text = stringResource(R.string.onboarding_back), onClick = onBack, modifier = Modifier.weight(1f))
        FittyPrimaryButton(text = if (step == LastStep) stringResource(R.string.onboarding_preview_plan) else stringResource(R.string.onboarding_continue), onClick = onNext, modifier = Modifier.weight(1f))
    }
}
@Composable private fun EquipmentStep(state: OnboardingUiState, values: List<OnboardingChoiceContent>, onEquipmentSelected: (String) -> Unit, onInjuryNoteChanged: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ChoiceList(values, state.equipment, onEquipmentSelected)
        OutlinedTextField(value = state.injuryNote, onValueChange = onInjuryNoteChanged, label = { Text(stringResource(R.string.onboarding_injury_optional)) },
            shape = RoundedCornerShape(16.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = FittyPink, cursorColor = FittyPink), modifier = Modifier.fillMaxWidth())
    }
}
@Composable private fun NutritionStep(state: OnboardingUiState, nutritionValues: List<OnboardingChoiceContent>, restrictionValues: List<OnboardingChoiceContent>, onNutritionSelected: (String) -> Unit, onRestrictionToggled: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ChoiceList(nutritionValues, state.nutrition, onNutritionSelected)
        MultiChoiceList(stringResource(R.string.onboarding_optional_restrictions), null, restrictionValues, state.restrictions, onRestrictionToggled)
    }
}
@Composable private fun ReminderStep(state: OnboardingUiState, values: List<OnboardingChoiceContent>, onReminderToggled: (String) -> Unit) {
    MultiChoiceList(stringResource(R.string.onboarding_set_reminders), null, values, state.reminders, onReminderToggled)
}
@Composable private fun ChoiceList(values: List<OnboardingChoiceContent>, selected: String, onSelected: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        values.forEach { value ->
            FittyChoiceCard(
                title = value.label,
                body = value.description,
                selected = selected == value.value,
                onClick = { onSelected(value.value) }
            )
        }
    }
}
@Composable private fun MultiChoiceList(title: String, icon: ImageVector? = null, values: List<OnboardingChoiceContent>, selected: Set<String>, onToggle: (String) -> Unit) {
    FittySectionBlock(title = title, icon = icon) {
        values.forEach { value ->
            FittyChoiceCard(
                title = value.label,
                body = if (selected.contains(value.value)) stringResource(R.string.onboarding_selected) else stringResource(R.string.onboarding_tap_to_select),
                selected = selected.contains(value.value),
                onClick = { onToggle(value.value) }
            )
        }
    }
}
@Composable private fun NumberField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onValueChange, label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = FittyPink, focusedLabelColor = FittyPink, cursorColor = FittyPink), modifier = Modifier.fillMaxWidth())
}
