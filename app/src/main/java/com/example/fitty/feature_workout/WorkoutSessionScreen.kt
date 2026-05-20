package com.example.fitty.feature_workout

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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitty.core.ui.FittyLazyScreen
import com.example.fitty.domain.usecase.workout.CompleteWorkoutSessionUseCase
import com.example.fitty.ui.theme.FittyPink
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WorkoutSessionUiState(
    val sessionId: String = "",
    val title: String = "Workout Session",
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val isCompleted: Boolean = false,
    val elapsedSeconds: Int = 0,
    val exerciseCount: Int = 0,
    val completedExercises: Int = 0,
    val estimatedCalories: Int = 0,
    val error: String? = null
)

@HiltViewModel
class WorkoutSessionViewModel @Inject constructor(
    private val completeWorkoutSessionUseCase: CompleteWorkoutSessionUseCase,
    private val updateStreakUseCase: com.example.fitty.domain.usecase.user.UpdateStreakUseCase,
    private val workoutSessionRepository: com.example.fitty.domain.repository.WorkoutSessionRepository,
    private val sessionRepository: com.example.fitty.domain.repository.SessionRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(WorkoutSessionUiState())
    val uiState: StateFlow<WorkoutSessionUiState> = _uiState

    private var timerRunning = false
    private var planId: String = ""
    private var scheduledWorkoutId: String = ""
    private var firestoreSessionId: String = ""

    fun initialize(sessionId: String, planId: String = "", scheduledWorkoutId: String = "") {
        this.planId = planId
        this.scheduledWorkoutId = scheduledWorkoutId
        this.firestoreSessionId = sessionId
        _uiState.update { it.copy(sessionId = sessionId) }
    }

    fun startWorkout() {
        _uiState.update { it.copy(isRunning = true, isPaused = false) }
        // Create Firestore session if needed
        viewModelScope.launch {
            val uid = sessionRepository.getCurrentUserId() ?: return@launch
            runCatching {
                val session = com.example.fitty.domain.model.WorkoutSession(
                    planId = planId,
                    scheduledWorkoutId = scheduledWorkoutId,
                    title = "Quick Workout",
                    source = if (planId.isNotBlank()) "plan" else "quick",
                    status = "in_progress",
                    startedAt = System.currentTimeMillis()
                )
                val result = workoutSessionRepository.startSession(uid, session)
                result.onSuccess { newId ->
                    firestoreSessionId = newId
                    _uiState.update { it.copy(sessionId = newId) }
                }
            }
        }
        startTimer()
    }

    fun pauseWorkout() {
        _uiState.update { it.copy(isPaused = true) }
        timerRunning = false
    }

    fun resumeWorkout() {
        _uiState.update { it.copy(isPaused = false) }
        startTimer()
    }

    fun completeExercise() {
        _uiState.update {
            it.copy(completedExercises = (it.completedExercises + 1).coerceAtMost(it.exerciseCount))
        }
    }

    fun finishWorkout(onComplete: () -> Unit) {
        timerRunning = false
        val state = _uiState.value
        _uiState.update { it.copy(isCompleted = true) }
        viewModelScope.launch {
            completeWorkoutSessionUseCase(
                sessionId = firestoreSessionId.ifBlank { state.sessionId },
                durationMinutes = state.elapsedSeconds / 60,
                caloriesBurned = state.estimatedCalories,
                completionRate = if (state.exerciseCount > 0) state.completedExercises.toFloat() / state.exerciseCount else 1f,
                perceivedEffort = null,
                exercises = emptyList(),
                planId = planId,
                scheduledWorkoutId = scheduledWorkoutId
            )
            runCatching { updateStreakUseCase("workout") }
            onComplete()
        }
    }

    private fun startTimer() {
        timerRunning = true
        viewModelScope.launch {
            while (timerRunning) {
                delay(1000)
                if (timerRunning) {
                    _uiState.update {
                        it.copy(
                            elapsedSeconds = it.elapsedSeconds + 1,
                            estimatedCalories = ((it.elapsedSeconds + 1) * 0.15).toInt()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WorkoutSessionRoute(
    sessionId: String,
    planId: String = "",
    scheduledWorkoutId: String = "",
    onBack: () -> Unit,
    viewModel: WorkoutSessionViewModel = hiltViewModel()
) {
    LaunchedEffect(sessionId) {
        viewModel.initialize(sessionId, planId, scheduledWorkoutId)
    }
    val state by viewModel.uiState.collectAsState()
    WorkoutSessionScreen(
        state = state,
        onStart = viewModel::startWorkout,
        onPause = viewModel::pauseWorkout,
        onResume = viewModel::resumeWorkout,
        onCompleteExercise = viewModel::completeExercise,
        onFinish = { viewModel.finishWorkout(onBack) },
        onBack = onBack
    )
}

@Composable
private fun WorkoutSessionScreen(
    state: WorkoutSessionUiState,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCompleteExercise: () -> Unit,
    onFinish: () -> Unit,
    onBack: () -> Unit
) {
    val minutes = state.elapsedSeconds / 60
    val seconds = state.elapsedSeconds % 60
    val timeDisplay = "%02d:%02d".format(minutes, seconds)

    FittyLazyScreen {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                }
                Text(
                    state.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.size(48.dp))
            }
        }

        // Timer card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(FittyPink.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Timer,
                            contentDescription = null,
                            tint = FittyPink,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Text(
                        timeDisplay,
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (state.isRunning && !state.isPaused) FittyPink else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        when {
                            state.isCompleted -> "Workout Complete! 🎉"
                            state.isPaused -> "Paused"
                            state.isRunning -> "In Progress..."
                            else -> "Ready to start"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Stats row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    value = "${state.estimatedCalories}",
                    label = "kcal burned",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    value = "${state.completedExercises}/${state.exerciseCount}",
                    label = "exercises",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Progress
        if (state.isRunning) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Progress", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        val progress = if (state.exerciseCount > 0)
                            state.completedExercises.toFloat() / state.exerciseCount
                        else 0f
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp)),
                            color = FittyPink,
                            trackColor = FittyPink.copy(alpha = 0.12f)
                        )
                        Text(
                            "${(progress * 100).toInt()}% complete",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Action buttons
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                when {
                    state.isCompleted -> {
                        Button(
                            onClick = onBack,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = FittyPink),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Outlined.CheckCircle, null, modifier = Modifier.size(20.dp))
                            Text("Back to Home", modifier = Modifier.padding(start = 8.dp), fontWeight = FontWeight.Bold)
                        }
                    }
                    !state.isRunning -> {
                        Button(
                            onClick = onStart,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = FittyPink),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            Icon(Icons.Outlined.PlayArrow, null, modifier = Modifier.size(24.dp))
                            Text("Start Workout", modifier = Modifier.padding(start = 8.dp), fontWeight = FontWeight.Bold)
                        }
                    }
                    else -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = if (state.isPaused) onResume else onPause,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    if (state.isPaused) Icons.Outlined.PlayArrow else Icons.Outlined.Pause,
                                    null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    if (state.isPaused) "Resume" else "Pause",
                                    modifier = Modifier.padding(start = 8.dp),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Button(
                                onClick = onCompleteExercise,
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = FittyPink),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Outlined.FitnessCenter, null, modifier = Modifier.size(20.dp))
                                Text("Done", modifier = Modifier.padding(start = 8.dp), fontWeight = FontWeight.Bold)
                            }
                        }
                        Button(
                            onClick = onFinish,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Outlined.CheckCircle, null, modifier = Modifier.size(20.dp))
                            Text("Finish Workout", modifier = Modifier.padding(start = 8.dp), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
