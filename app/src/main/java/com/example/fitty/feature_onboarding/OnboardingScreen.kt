package com.example.fitty.feature_onboarding

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fitty.core.designsystem.component.FittyChoiceCard
import com.example.fitty.core.designsystem.component.FittyPrimaryButton
import com.example.fitty.core.designsystem.component.FittySecondaryButton
import com.example.fitty.core.ui.FittyLazyScreen
import com.example.fitty.data.firebase.FittyFirebaseRepository
import com.example.fitty.data.firebase.FittyOnboardingAnswers
import com.example.fitty.data.preferences.AppPreferencesDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TotalSteps = 9
private const val LastStep = TotalSteps - 1

data class OnboardingUiState(
    val step: Int = 0,
    val goal: String = "",
    val age: String = "",
    val height: String = "",
    val weight: String = "",
    val targetWeight: String = "",
    val fitnessLevel: String = "",
    val workoutDays: Set<String> = emptySet(),
    val duration: String = "",
    val preferredTime: String = "",
    val equipment: String = "",
    val injuryNote: String = "",
    val nutrition: String = "",
    val restrictions: Set<String> = emptySet(),
    val reminders: Set<String> = emptySet(),
    val errorMessage: String? = null
)

class OnboardingViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = AppPreferencesDataSource(application.applicationContext)
    private val repository = FittyFirebaseRepository()
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

    fun toggleWorkoutDay(value: String) {
        update {
            copy(workoutDays = workoutDays.toggle(value))
        }
    }

    fun toggleRestriction(value: String) {
        update {
            copy(restrictions = restrictions.toggle(value))
        }
    }

    fun toggleReminder(value: String) {
        update {
            copy(reminders = reminders.toggle(value))
        }
    }

    fun back() {
        _uiState.update { state ->
            state.copy(step = (state.step - 1).coerceAtLeast(0), errorMessage = null)
        }
    }

    fun next(onFinished: () -> Unit) {
        val error = validate(_uiState.value)
        if (error != null) {
            _uiState.update { it.copy(errorMessage = error) }
            return
        }

        if (_uiState.value.step == LastStep) {
            viewModelScope.launch {
                val userId = preferences.currentUserId.first()
                if (userId == null) {
                    _uiState.update { it.copy(errorMessage = "Start a session before saving onboarding") }
                    return@launch
                }
                repository.saveOnboardingAnswers(userId, _uiState.value.toAnswers())
                onFinished()
            }
        } else {
            _uiState.update { it.copy(step = it.step + 1, errorMessage = null) }
        }
    }

    private fun update(transform: OnboardingUiState.() -> OnboardingUiState) {
        _uiState.update { it.transform().copy(errorMessage = null) }
    }

    private fun validate(state: OnboardingUiState): String? = when (state.step) {
        0 -> if (state.goal.isBlank()) "Choose one primary goal" else null
        1 -> when {
            state.age.toIntOrNull() == null -> "Enter your age"
            state.age.toIntOrNull() !in 13..100 -> "Age must be between 13 and 100"
            state.height.toIntOrNull() == null -> "Enter your height"
            state.height.toIntOrNull() !in 100..250 -> "Height must be between 100 and 250 cm"
            state.weight.toIntOrNull() == null -> "Enter your weight"
            state.weight.toIntOrNull() !in 30..300 -> "Weight must be between 30 and 300 kg"
            state.targetWeight.toIntOrNull() == null -> "Enter your target weight"
            state.targetWeight.toIntOrNull() !in 30..300 -> "Target weight must be between 30 and 300 kg"
            else -> null
        }
        2 -> if (state.fitnessLevel.isBlank()) "Choose your fitness level" else null
        3 -> if (state.workoutDays.isEmpty()) "Choose at least one workout day" else null
        4 -> if (state.preferredTime.isBlank()) "Choose preferred time" else null
        5 -> if (state.duration.isBlank()) "Choose workout duration" else null
        6 -> if (state.equipment.isBlank()) "Choose where you train" else null
        7 -> if (state.nutrition.isBlank()) "Choose your eating style" else null
        else -> null
    }

    private fun Set<String>.toggle(value: String): Set<String> =
        if (contains(value)) this - value else this + value

    private fun OnboardingUiState.toAnswers(): FittyOnboardingAnswers =
        FittyOnboardingAnswers(
            goal = goal,
            age = age.toIntOrNull(),
            heightCm = height.toIntOrNull(),
            weightKg = weight.toIntOrNull(),
            targetWeightKg = targetWeight.toIntOrNull(),
            fitnessLevel = fitnessLevel,
            workoutDays = workoutDays,
            durationMinutes = duration.filter(Char::isDigit).toIntOrNull() ?: 0,
            preferredTime = preferredTime,
            equipment = equipment,
            injuryNote = injuryNote,
            nutrition = nutrition,
            restrictions = restrictions,
            reminders = reminders
        )
}

@Composable
fun OnboardingRoute(
    onExit: () -> Unit,
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val handleBack = {
        if (state.step > 0) {
            viewModel.back()
        } else {
            onExit()
        }
    }

    BackHandler(onBack = handleBack)

    OnboardingScreen(
        state = state,
        onGoalSelected = viewModel::selectGoal,
        onAgeChanged = viewModel::updateAge,
        onHeightChanged = viewModel::updateHeight,
        onWeightChanged = viewModel::updateWeight,
        onTargetWeightChanged = viewModel::updateTargetWeight,
        onFitnessLevelSelected = viewModel::selectFitnessLevel,
        onWorkoutDayToggled = viewModel::toggleWorkoutDay,
        onDurationSelected = viewModel::selectDuration,
        onPreferredTimeSelected = viewModel::selectPreferredTime,
        onEquipmentSelected = viewModel::selectEquipment,
        onInjuryNoteChanged = viewModel::updateInjuryNote,
        onNutritionSelected = viewModel::selectNutrition,
        onRestrictionToggled = viewModel::toggleRestriction,
        onReminderToggled = viewModel::toggleReminder,
        onBack = handleBack,
        onExit = onExit,
        onNext = { viewModel.next(onFinished) }
    )
}

@Composable
fun OnboardingScreen(
    state: OnboardingUiState,
    onGoalSelected: (String) -> Unit,
    onAgeChanged: (String) -> Unit,
    onHeightChanged: (String) -> Unit,
    onWeightChanged: (String) -> Unit,
    onTargetWeightChanged: (String) -> Unit,
    onFitnessLevelSelected: (String) -> Unit,
    onWorkoutDayToggled: (String) -> Unit,
    onDurationSelected: (String) -> Unit,
    onPreferredTimeSelected: (String) -> Unit,
    onEquipmentSelected: (String) -> Unit,
    onInjuryNoteChanged: (String) -> Unit,
    onNutritionSelected: (String) -> Unit,
    onRestrictionToggled: (String) -> Unit,
    onReminderToggled: (String) -> Unit,
    onBack: () -> Unit,
    onExit: () -> Unit,
    onNext: () -> Unit
) {
    FittyLazyScreen {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                TextButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = null
                    )
                    Text("Back")
                }
                TextButton(onClick = onExit) {
                    Text("Exit")
                }
            }
        }
        item {
            Column {
                Text("Step ${state.step + 1} of $TotalSteps", style = MaterialTheme.typography.labelLarge)
                LinearProgressIndicator(
                    progress = { (state.step + 1) / TotalSteps.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }
        }
        item {
            Text(
                text = stepTitle(state.step),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
        item {
            when (state.step) {
                0 -> ChoiceList(
                    values = listOf("Lose Weight", "Gain Muscle", "Maintain Fitness", "Improve Endurance", "Improve Flexibility", "Build Healthy Habits"),
                    selected = state.goal,
                    onSelected = onGoalSelected
                )
                1 -> BodyMetricsStep(state, onAgeChanged, onHeightChanged, onWeightChanged, onTargetWeightChanged)
                2 -> ChoiceList(
                    values = listOf("Beginner", "Intermediate", "Advanced"),
                    selected = state.fitnessLevel,
                    onSelected = onFitnessLevelSelected
                )
                3 -> WorkoutDaysStep(state, onWorkoutDayToggled)
                4 -> WorkoutTimeStep(state, onPreferredTimeSelected)
                5 -> WorkoutDurationStep(state, onDurationSelected)
                6 -> EquipmentStep(state, onEquipmentSelected, onInjuryNoteChanged)
                7 -> NutritionStep(state, onNutritionSelected, onRestrictionToggled)
                8 -> ReminderStep(state, onReminderToggled)
            }
        }
        item {
            state.errorMessage?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error)
            }
        }
        item {
            OnboardingActions(
                step = state.step,
                onBack = onBack,
                onNext = onNext
            )
        }
    }
}

@Composable
private fun BodyMetricsStep(
    state: OnboardingUiState,
    onAgeChanged: (String) -> Unit,
    onHeightChanged: (String) -> Unit,
    onWeightChanged: (String) -> Unit,
    onTargetWeightChanged: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        NumberField("Age", state.age, onAgeChanged)
        NumberField("Height cm", state.height, onHeightChanged)
        NumberField("Weight kg", state.weight, onWeightChanged)
        NumberField("Target weight kg", state.targetWeight, onTargetWeightChanged)
    }
}

@Composable
private fun WorkoutDaysStep(
    state: OnboardingUiState,
    onWorkoutDayToggled: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        MultiChoiceList(
            title = "Training days",
            icon = Icons.Outlined.CalendarMonth,
            values = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"),
            selected = state.workoutDays,
            onToggle = onWorkoutDayToggled
        )
    }
}

@Composable
private fun WorkoutTimeStep(
    state: OnboardingUiState,
    onPreferredTimeSelected: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(Icons.Outlined.Schedule, "Preferred workout time")
        ChoiceList(
            values = listOf("Morning", "Afternoon", "Evening"),
            selected = state.preferredTime,
            onSelected = onPreferredTimeSelected
        )
    }
}

@Composable
private fun WorkoutDurationStep(
    state: OnboardingUiState,
    onDurationSelected: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(Icons.Outlined.Timer, "Session duration")
        ChoiceList(
            values = listOf("20 min", "30 min", "45 min", "60 min"),
            selected = state.duration,
            onSelected = onDurationSelected
        )
    }
}

@Composable
private fun OnboardingActions(
    step: Int,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FittySecondaryButton(
            text = "Back",
            onClick = onBack,
            modifier = Modifier.weight(1f)
        )
        FittyPrimaryButton(
            text = if (step == LastStep) "Preview Plan" else "Continue",
            onClick = onNext,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun EquipmentStep(
    state: OnboardingUiState,
    onEquipmentSelected: (String) -> Unit,
    onInjuryNoteChanged: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ChoiceList(
            values = listOf("Home, no equipment", "Home, basic equipment", "Gym", "Mix of home and gym"),
            selected = state.equipment,
            onSelected = onEquipmentSelected
        )
        OutlinedTextField(
            value = state.injuryNote,
            onValueChange = onInjuryNoteChanged,
            label = { Text("Injury or mobility note optional") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun NutritionStep(
    state: OnboardingUiState,
    onNutritionSelected: (String) -> Unit,
    onRestrictionToggled: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ChoiceList(
            values = listOf("Standard", "High Protein", "Vegetarian", "Vegan", "Low Carb", "Flexible"),
            selected = state.nutrition,
            onSelected = onNutritionSelected
        )
        MultiChoiceList(
            title = "Optional restrictions",
            values = listOf("Lactose-free", "Nut allergy", "Avoid seafood"),
            selected = state.restrictions,
            onToggle = onRestrictionToggled
        )
    }
}

@Composable
private fun ReminderStep(
    state: OnboardingUiState,
    onReminderToggled: (String) -> Unit
) {
    MultiChoiceList(
        title = "Set your reminders",
        icon = null,
        values = listOf("Workout reminder", "Meal reminder", "Water reminder", "Sleep reminder"),
        selected = state.reminders,
        onToggle = onReminderToggled
    )
}

@Composable
private fun ChoiceList(
    values: List<String>,
    selected: String,
    onSelected: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        values.forEach { value ->
            FittyChoiceCard(
                title = value,
                body = choiceDescription(value),
                selected = selected == value,
                onClick = { onSelected(value) }
            )
        }
    }
}

@Composable
private fun MultiChoiceList(
    title: String,
    icon: ImageVector? = null,
    values: List<String>,
    selected: Set<String>,
    onToggle: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (icon == null) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        } else {
            SectionHeader(icon, title)
        }
        values.forEach { value ->
            FittyChoiceCard(
                title = value,
                body = if (selected.contains(value)) "Selected" else "Tap to select",
                selected = selected.contains(value),
                onClick = { onToggle(value) }
            )
        }
    }
}

@Composable
private fun NumberField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun SectionHeader(icon: ImageVector, title: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary
        )
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

private fun stepTitle(step: Int): String = when (step) {
    0 -> "What is your main goal?"
    1 -> "Tell us about your body"
    2 -> "What is your current fitness level?"
    3 -> "Which days can you train?"
    4 -> "Which time of day suits you best?"
    5 -> "How long should each session be?"
    6 -> "Where do you usually train?"
    7 -> "What best matches your eating style?"
    else -> "Set your reminders"
}

private fun choiceDescription(value: String): String = when (value) {
    "Lose Weight" -> "Create a realistic calorie and workout plan."
    "Gain Muscle" -> "Prioritize strength sessions and protein habits."
    "Maintain Fitness" -> "Keep a balanced weekly rhythm."
    "Improve Endurance" -> "Build cardio capacity step by step."
    "Improve Flexibility" -> "Add mobility work and recovery routines."
    "Build Healthy Habits" -> "Focus on consistency, hydration, and sleep."
    "Beginner" -> "0-2 workouts per week."
    "Intermediate" -> "3-4 workouts per week."
    "Advanced" -> "5+ structured sessions per week."
    else -> "Personalize your Fitty plan."
}
