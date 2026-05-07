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
import com.example.fitty.R
import com.example.fitty.core.designsystem.component.FittyChoiceCard
import com.example.fitty.core.designsystem.component.FittyPrimaryButton
import com.example.fitty.core.designsystem.component.FittySectionBlock
import com.example.fitty.core.designsystem.component.FittySecondaryButton
import com.example.fitty.core.ui.FittyLazyScreen
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
    val errorMessage: String? = null
)

private data class OnboardingChoiceOption(
    val title: String,
    val description: String
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val saveOnboardingAnswersUseCase: SaveOnboardingAnswersUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState

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
        item { Text(text = stepTitle(state.step), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        item {
            when (state.step) {
                0 -> ChoiceList(goalOptions(), state.goal, onGoalSelected)
                1 -> BodyMetricsStep(state, onAgeChanged, onHeightChanged, onWeightChanged, onTargetWeightChanged)
                2 -> ChoiceList(fitnessOptions(), state.fitnessLevel, onFitnessLevelSelected)
                3 -> WorkoutDaysStep(state, onWorkoutDayToggled)
                4 -> WorkoutTimeStep(state, onPreferredTimeSelected)
                5 -> WorkoutDurationStep(state, onDurationSelected)
                6 -> EquipmentStep(state, onEquipmentSelected, onInjuryNoteChanged)
                7 -> NutritionStep(state, onNutritionSelected, onRestrictionToggled)
                8 -> ReminderStep(state, onReminderToggled)
            }
        }
        item { state.errorMessage?.let { Text(text = it, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium) } }
        item { OnboardingActions(step = state.step, onBack = onBack, onNext = onNext) }
    }
}

@Composable private fun BodyMetricsStep(state: OnboardingUiState, onAgeChanged: (String) -> Unit, onHeightChanged: (String) -> Unit, onWeightChanged: (String) -> Unit, onTargetWeightChanged: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { NumberField(stringResource(R.string.onboarding_age), state.age, onAgeChanged); NumberField(stringResource(R.string.onboarding_height_cm), state.height, onHeightChanged); NumberField(stringResource(R.string.onboarding_weight_kg), state.weight, onWeightChanged); NumberField(stringResource(R.string.onboarding_target_weight_kg), state.targetWeight, onTargetWeightChanged) }
}
@Composable private fun WorkoutDaysStep(state: OnboardingUiState, onWorkoutDayToggled: (String) -> Unit) {
    MultiChoiceList(stringResource(R.string.onboarding_training_days), Icons.Outlined.CalendarMonth, workoutDayOptions(), state.workoutDays, onWorkoutDayToggled)
}
@Composable private fun WorkoutTimeStep(state: OnboardingUiState, onPreferredTimeSelected: (String) -> Unit) {
    FittySectionBlock(title = stringResource(R.string.onboarding_preferred_workout_time), icon = Icons.Outlined.Schedule) {
        ChoiceList(timeOptions(), state.preferredTime, onPreferredTimeSelected)
    }
}
@Composable private fun WorkoutDurationStep(state: OnboardingUiState, onDurationSelected: (String) -> Unit) {
    FittySectionBlock(title = stringResource(R.string.onboarding_session_duration), icon = Icons.Outlined.Timer) {
        ChoiceList(durationOptions(), state.duration, onDurationSelected)
    }
}
@Composable private fun OnboardingActions(step: Int, onBack: () -> Unit, onNext: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        FittySecondaryButton(text = stringResource(R.string.onboarding_back), onClick = onBack, modifier = Modifier.weight(1f))
        FittyPrimaryButton(text = if (step == LastStep) stringResource(R.string.onboarding_preview_plan) else stringResource(R.string.onboarding_continue), onClick = onNext, modifier = Modifier.weight(1f))
    }
}
@Composable private fun EquipmentStep(state: OnboardingUiState, onEquipmentSelected: (String) -> Unit, onInjuryNoteChanged: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ChoiceList(equipmentOptions(), state.equipment, onEquipmentSelected)
        OutlinedTextField(value = state.injuryNote, onValueChange = onInjuryNoteChanged, label = { Text(stringResource(R.string.onboarding_injury_optional)) },
            shape = RoundedCornerShape(16.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = FittyPink, cursorColor = FittyPink), modifier = Modifier.fillMaxWidth())
    }
}
@Composable private fun NutritionStep(state: OnboardingUiState, onNutritionSelected: (String) -> Unit, onRestrictionToggled: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ChoiceList(nutritionOptions(), state.nutrition, onNutritionSelected)
        MultiChoiceList(stringResource(R.string.onboarding_optional_restrictions), null, restrictionOptions(), state.restrictions, onRestrictionToggled)
    }
}
@Composable private fun ReminderStep(state: OnboardingUiState, onReminderToggled: (String) -> Unit) {
    MultiChoiceList(stringResource(R.string.onboarding_set_reminders), null, reminderOptions(), state.reminders, onReminderToggled)
}
@Composable private fun ChoiceList(values: List<OnboardingChoiceOption>, selected: String, onSelected: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { values.forEach { value -> FittyChoiceCard(title = value.title, body = value.description, selected = selected == value.title, onClick = { onSelected(value.title) }) } }
}
@Composable private fun MultiChoiceList(title: String, icon: ImageVector? = null, values: List<String>, selected: Set<String>, onToggle: (String) -> Unit) {
    FittySectionBlock(title = title, icon = icon) {
        values.forEach { value -> FittyChoiceCard(title = value, body = if (selected.contains(value)) stringResource(R.string.onboarding_selected) else stringResource(R.string.onboarding_tap_to_select), selected = selected.contains(value), onClick = { onToggle(value) }) }
    }
}
@Composable private fun NumberField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onValueChange, label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = FittyPink, focusedLabelColor = FittyPink, cursorColor = FittyPink), modifier = Modifier.fillMaxWidth())
}
@Composable
private fun stepTitle(step: Int): String = when (step) {
    0 -> stringResource(R.string.onboarding_step_title_goal)
    1 -> stringResource(R.string.onboarding_step_title_body)
    2 -> stringResource(R.string.onboarding_step_title_fitness)
    3 -> stringResource(R.string.onboarding_step_title_days)
    4 -> stringResource(R.string.onboarding_step_title_time)
    5 -> stringResource(R.string.onboarding_step_title_duration)
    6 -> stringResource(R.string.onboarding_step_title_location)
    7 -> stringResource(R.string.onboarding_step_title_nutrition)
    else -> stringResource(R.string.onboarding_step_title_reminders)
}

@Composable
private fun goalOptions(): List<OnboardingChoiceOption> = listOf(
    OnboardingChoiceOption(stringResource(R.string.onboarding_goal_lose_weight), stringResource(R.string.onboarding_goal_lose_weight_desc)),
    OnboardingChoiceOption(stringResource(R.string.onboarding_goal_gain_muscle), stringResource(R.string.onboarding_goal_gain_muscle_desc)),
    OnboardingChoiceOption(stringResource(R.string.onboarding_goal_maintain_fitness), stringResource(R.string.onboarding_goal_maintain_fitness_desc)),
    OnboardingChoiceOption(stringResource(R.string.onboarding_goal_improve_endurance), stringResource(R.string.onboarding_goal_improve_endurance_desc)),
    OnboardingChoiceOption(stringResource(R.string.onboarding_goal_improve_flexibility), stringResource(R.string.onboarding_goal_improve_flexibility_desc)),
    OnboardingChoiceOption(stringResource(R.string.onboarding_goal_build_habits), stringResource(R.string.onboarding_goal_build_habits_desc))
)

@Composable
private fun fitnessOptions(): List<OnboardingChoiceOption> = listOf(
    OnboardingChoiceOption(stringResource(R.string.onboarding_fitness_beginner), stringResource(R.string.onboarding_fitness_beginner_desc)),
    OnboardingChoiceOption(stringResource(R.string.onboarding_fitness_intermediate), stringResource(R.string.onboarding_fitness_intermediate_desc)),
    OnboardingChoiceOption(stringResource(R.string.onboarding_fitness_advanced), stringResource(R.string.onboarding_fitness_advanced_desc))
)

@Composable
private fun timeOptions(): List<OnboardingChoiceOption> = listOf(
    OnboardingChoiceOption(stringResource(R.string.onboarding_time_morning), stringResource(R.string.onboarding_choice_generic_desc)),
    OnboardingChoiceOption(stringResource(R.string.onboarding_time_afternoon), stringResource(R.string.onboarding_choice_generic_desc)),
    OnboardingChoiceOption(stringResource(R.string.onboarding_time_evening), stringResource(R.string.onboarding_choice_generic_desc))
)

@Composable
private fun durationOptions(): List<OnboardingChoiceOption> = listOf(
    OnboardingChoiceOption(stringResource(R.string.onboarding_duration_20), stringResource(R.string.onboarding_choice_generic_desc)),
    OnboardingChoiceOption(stringResource(R.string.onboarding_duration_30), stringResource(R.string.onboarding_choice_generic_desc)),
    OnboardingChoiceOption(stringResource(R.string.onboarding_duration_45), stringResource(R.string.onboarding_choice_generic_desc)),
    OnboardingChoiceOption(stringResource(R.string.onboarding_duration_60), stringResource(R.string.onboarding_choice_generic_desc))
)

@Composable
private fun equipmentOptions(): List<OnboardingChoiceOption> = listOf(
    OnboardingChoiceOption(stringResource(R.string.onboarding_equipment_home_none), stringResource(R.string.onboarding_choice_generic_desc)),
    OnboardingChoiceOption(stringResource(R.string.onboarding_equipment_home_basic), stringResource(R.string.onboarding_choice_generic_desc)),
    OnboardingChoiceOption(stringResource(R.string.onboarding_equipment_gym), stringResource(R.string.onboarding_choice_generic_desc)),
    OnboardingChoiceOption(stringResource(R.string.onboarding_equipment_mix), stringResource(R.string.onboarding_choice_generic_desc))
)

@Composable
private fun nutritionOptions(): List<OnboardingChoiceOption> = listOf(
    OnboardingChoiceOption(stringResource(R.string.onboarding_nutrition_standard), stringResource(R.string.onboarding_choice_generic_desc)),
    OnboardingChoiceOption(stringResource(R.string.onboarding_nutrition_high_protein), stringResource(R.string.onboarding_choice_generic_desc)),
    OnboardingChoiceOption(stringResource(R.string.onboarding_nutrition_vegetarian), stringResource(R.string.onboarding_choice_generic_desc)),
    OnboardingChoiceOption(stringResource(R.string.onboarding_nutrition_vegan), stringResource(R.string.onboarding_choice_generic_desc)),
    OnboardingChoiceOption(stringResource(R.string.onboarding_nutrition_low_carb), stringResource(R.string.onboarding_choice_generic_desc)),
    OnboardingChoiceOption(stringResource(R.string.onboarding_nutrition_flexible), stringResource(R.string.onboarding_choice_generic_desc))
)

@Composable
private fun workoutDayOptions(): List<String> = listOf(
    stringResource(R.string.onboarding_day_mon),
    stringResource(R.string.onboarding_day_tue),
    stringResource(R.string.onboarding_day_wed),
    stringResource(R.string.onboarding_day_thu),
    stringResource(R.string.onboarding_day_fri),
    stringResource(R.string.onboarding_day_sat),
    stringResource(R.string.onboarding_day_sun)
)

@Composable
private fun restrictionOptions(): List<String> = listOf(
    stringResource(R.string.onboarding_restriction_lactose_free),
    stringResource(R.string.onboarding_restriction_nut_allergy),
    stringResource(R.string.onboarding_restriction_avoid_seafood)
)

@Composable
private fun reminderOptions(): List<String> = listOf(
    stringResource(R.string.onboarding_reminder_workout),
    stringResource(R.string.onboarding_reminder_meal),
    stringResource(R.string.onboarding_reminder_water),
    stringResource(R.string.onboarding_reminder_sleep)
)
