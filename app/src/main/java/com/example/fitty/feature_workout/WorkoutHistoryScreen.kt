package com.example.fitty.feature_workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitty.R
import com.example.fitty.domain.model.ExerciseLog
import com.example.fitty.domain.model.WorkoutSession
import com.example.fitty.domain.repository.SessionRepository
import com.example.fitty.domain.repository.WorkoutSessionRepository
import com.example.fitty.ui.theme.FittyGradientEnd
import com.example.fitty.ui.theme.FittyGradientStart
import com.example.fitty.ui.theme.FittyPink
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

data class WorkoutHistoryUiState(
    val isLoading: Boolean = true,
    val sessions: List<WorkoutSession> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class WorkoutHistoryViewModel @Inject constructor(
    private val workoutSessionRepository: WorkoutSessionRepository,
    private val sessionRepository: SessionRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(WorkoutHistoryUiState())
    val uiState: StateFlow<WorkoutHistoryUiState> = _uiState

    fun load() {
        viewModelScope.launch {
            val uid = sessionRepository.getCurrentUserId()
            if (uid == null) {
                _uiState.value = WorkoutHistoryUiState(
                    isLoading = false,
                    error = "NO_USER"
                )
                return@launch
            }

            _uiState.value = WorkoutHistoryUiState(isLoading = true)
            runCatching {
                workoutSessionRepository
                    .getRecentSessions(uid, limit = 3)
                    .filter { it.id.isNotBlank() }
                    .mapNotNull { session -> workoutSessionRepository.getSession(uid, session.id) }
                    .filter { it.exercises.isNotEmpty() || it.plannedExercises.isNotEmpty() }
            }.onSuccess { sessions ->
                _uiState.value = WorkoutHistoryUiState(
                    isLoading = false,
                    sessions = sessions
                )
            }.onFailure { error ->
                _uiState.value = WorkoutHistoryUiState(
                    isLoading = false,
                    error = error.message
                )
            }
        }
    }
}

@Composable
fun WorkoutHistoryRoute(
    onBack: () -> Unit,
    viewModel: WorkoutHistoryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.load()
    }
    WorkoutHistoryScreen(
        state = state,
        onBack = onBack
    )
}

@Composable
private fun WorkoutHistoryScreen(
    state: WorkoutHistoryUiState,
    onBack: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when {
            state.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = FittyPink)
                }
            }

            state.sessions.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(40.dp))
                    Icon(
                        Icons.Outlined.History,
                        contentDescription = null,
                        tint = FittyPink,
                        modifier = Modifier.size(46.dp)
                    )
                    Text(
                        text = state.error ?: stringResource(R.string.workout_history_empty),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        WorkoutHistoryHeader(onBack = onBack)
                    }
                    items(state.sessions) { session ->
                        WorkoutHistorySessionCard(session = session)
                    }
                    item { Spacer(modifier = Modifier.height(20.dp)) }
                }
            }
        }
    }
}

@Composable
private fun WorkoutHistoryHeader(onBack: () -> Unit) {
    Card(
        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(FittyGradientStart, FittyGradientEnd)))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.common_back),
                    tint = Color.White
                )
            }
            Text(
                text = stringResource(R.string.workout_history_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = stringResource(R.string.workout_history_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
private fun WorkoutHistorySessionCard(session: WorkoutSession) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = session.title.ifBlank { stringResource(R.string.workout_history_session_fallback) },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = formatSessionMeta(session),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            session.exercises.sortedBy { it.orderIndex }.forEachIndexed { index, exercise ->
                WorkoutHistoryExerciseRow(
                    index = index,
                    exercise = exercise
                )
            }
        }
    }
}

@Composable
private fun WorkoutHistoryExerciseRow(
    index: Int,
    exercise: ExerciseLog
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(FittyPink.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = (index + 1).toString(),
                    color = FittyPink,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = exercise.name.ifBlank { exercise.exerciseId },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatExerciseLogMeta(exercise),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (exercise.repsBySet.isNotEmpty() || exercise.weightKgBySet.isNotEmpty()) {
                    Text(
                        text = formatSetBreakdown(exercise),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

private fun formatSessionMeta(session: WorkoutSession): String {
    val startedAtText = session.startedAt.takeIf { it > 0L }?.let { millis ->
        Instant.ofEpochMilli(millis)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.US))
    }.orEmpty()

    return listOfNotNull(
        startedAtText.takeIf { it.isNotBlank() },
        session.durationMinutes.takeIf { it > 0 }?.let { "$it min" },
        session.caloriesBurned.takeIf { it > 0 }?.let { "$it kcal" }
    ).joinToString(" • ")
}

private fun formatExerciseLogMeta(exercise: ExerciseLog): String {
    return listOfNotNull(
        exercise.plannedSets.takeIf { it > 0 }?.let { "$it sets" },
        exercise.completedSets.takeIf { it > 0 }?.let { "$it completed" },
        exercise.durationSeconds?.takeIf { it > 0 }?.let { "$it sec" }
    ).joinToString(" • ")
}

private fun formatSetBreakdown(exercise: ExerciseLog): String {
    val totalSets = maxOf(
        exercise.completedSets,
        exercise.repsBySet.size,
        exercise.weightKgBySet.size
    ).coerceAtLeast(1)

    return (0 until totalSets).joinToString("  |  ") { setIndex ->
        val reps = exercise.repsBySet.getOrNull(setIndex)?.let { "$it reps" } ?: "-- reps"
        val weight = exercise.weightKgBySet.getOrNull(setIndex)?.let { "${trimWeight(it)} kg" } ?: "-- kg"
        "Set ${setIndex + 1}: $reps, $weight"
    }
}

private fun trimWeight(weight: Float): String {
    return if (weight % 1f == 0f) {
        weight.toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", weight)
    }
}
