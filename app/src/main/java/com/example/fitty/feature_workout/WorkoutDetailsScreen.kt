package com.example.fitty.feature_workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.fitty.R
import com.example.fitty.core.ui.toDisplayBadges
import com.example.fitty.core.ui.toDisplaySummary
import com.example.fitty.domain.model.Exercise
import com.example.fitty.domain.model.ScheduledWorkout
import com.example.fitty.domain.model.WorkoutExercise
import com.example.fitty.domain.repository.ExerciseCatalogRepository
import com.example.fitty.domain.repository.PlanRepository
import com.example.fitty.domain.repository.SessionRepository
import com.example.fitty.ui.theme.FittyGradientEnd
import com.example.fitty.ui.theme.FittyGradientStart
import com.example.fitty.ui.theme.FittyPink
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import javax.inject.Inject

data class WorkoutDetailsExerciseUi(
    val planned: WorkoutExercise,
    val exercise: Exercise? = null
)

data class WorkoutDetailsUiState(
    val isLoading: Boolean = true,
    val workout: ScheduledWorkout? = null,
    val exerciseItems: List<WorkoutDetailsExerciseUi> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class WorkoutDetailsViewModel @Inject constructor(
    private val planRepository: PlanRepository,
    private val exerciseRepository: ExerciseCatalogRepository,
    private val sessionRepository: SessionRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(WorkoutDetailsUiState())
    val uiState: StateFlow<WorkoutDetailsUiState> = _uiState

    fun load(planId: String, scheduledWorkoutId: String) {
        if (planId.isBlank() || scheduledWorkoutId.isBlank()) {
            _uiState.value = WorkoutDetailsUiState(
                isLoading = false,
                error = "MISSING_DETAILS"
            )
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val uid = sessionRepository.getCurrentUserId()
            val workout = if (uid != null) {
                runCatching {
                    planRepository.getScheduledWorkout(uid, planId, scheduledWorkoutId)
                }.getOrNull()
            } else {
                null
            }
            val exerciseItems = workout?.exercises.orEmpty().map { planned ->
                val resolved = planned.exerciseId.takeIf { it.isNotBlank() }?.let { exerciseId ->
                    runCatching { exerciseRepository.getExercise(exerciseId) }.getOrNull()
                }
                WorkoutDetailsExerciseUi(planned = planned, exercise = resolved)
            }
            _uiState.value = WorkoutDetailsUiState(
                isLoading = false,
                workout = workout,
                exerciseItems = exerciseItems,
                error = if (workout == null) "UNAVAILABLE" else null
            )
        }
    }
}

@Composable
fun WorkoutDetailsRoute(
    planId: String,
    scheduledWorkoutId: String,
    onBack: () -> Unit,
    onStartWorkout: () -> Unit,
    onOpenExercise: (String) -> Unit,
    viewModel: WorkoutDetailsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(planId, scheduledWorkoutId) {
        viewModel.load(planId = planId, scheduledWorkoutId = scheduledWorkoutId)
    }
    WorkoutDetailsScreen(
        state = state,
        onBack = onBack,
        onStartWorkout = onStartWorkout,
        onOpenExercise = onOpenExercise
    )
}

@Composable
fun WorkoutDetailsScreen(
    state: WorkoutDetailsUiState,
    onBack: () -> Unit,
    onStartWorkout: () -> Unit,
    onOpenExercise: (String) -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when {
            state.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = FittyPink)
                }
            }

            state.workout == null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(40.dp))
                    Icon(
                        Icons.Outlined.FitnessCenter,
                        contentDescription = null,
                        tint = FittyPink,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = when (state.error) {
                            "MISSING_DETAILS" -> stringResource(R.string.workout_details_missing)
                            else -> stringResource(R.string.workout_details_unavailable)
                        },
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Button(onClick = onBack) {
                        Text(stringResource(R.string.common_back))
                    }
                }
            }

            else -> {
                val workout = state.workout
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        WorkoutDetailsHeader(
                            workout = workout,
                            onBack = onBack
                        )
                    }
                    item {
                        WorkoutOverviewCard(workout = workout)
                    }
                    items(state.exerciseItems) { item ->
                        WorkoutDetailExerciseCard(
                            item = item,
                            onClick = {
                                item.planned.exerciseId.takeIf { exerciseId -> exerciseId.isNotBlank() }?.let(onOpenExercise)
                            }
                        )
                    }
                    item {
                        Button(
                            onClick = onStartWorkout,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = FittyPink)
                        ) {
                            Text(stringResource(R.string.workout_details_start))
                        }
                    }
                    item { Spacer(modifier = Modifier.height(20.dp)) }
                }
            }
        }
    }
}

@Composable
private fun WorkoutDetailsHeader(
    workout: ScheduledWorkout,
    onBack: () -> Unit
) {
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
                text = workout.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = listOfNotNull(
                    stringResource(R.string.workout_details_minutes, workout.durationMinutes),
                    workout.difficulty.takeIf { it.isNotBlank() }?.replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString()
                    },
                    workout.equipment.takeIf { it.isNotBlank() }
                ).joinToString(" • "),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
private fun WorkoutOverviewCard(workout: ScheduledWorkout) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.workout_details_plan_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(
                    R.string.workout_details_overview,
                    workout.exercises.size,
                    workout.estimatedCalories
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            workout.explanation.takeIf { it.isNotBlank() }?.let { explanation ->
                Text(
                    text = explanation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun WorkoutDetailExerciseCard(
    item: WorkoutDetailsExerciseUi,
    onClick: () -> Unit
) {
    val planned = item.planned
    val resolved = item.exercise
    val imageModel: Any? = when {
        resolved?.localThumbnailPath?.isNotBlank() == true -> File(resolved.localThumbnailPath)
        resolved?.thumbnailUrl?.isNotBlank() == true -> resolved.thumbnailUrl
        resolved?.localGifPath?.isNotBlank() == true -> File(resolved.localGifPath)
        resolved?.gifUrl?.isNotBlank() == true -> resolved.gifUrl
        else -> null
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFE8DEF8)),
                contentAlignment = Alignment.Center
            ) {
                if (imageModel != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageModel)
                            .crossfade(true)
                            .build(),
                        contentDescription = planned.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        Icons.Outlined.FitnessCenter,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = planned.name.ifBlank { planned.exerciseId },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                resolved?.let { exercise ->
                    val metaParts = listOfNotNull(
                        exercise.target.takeIf { it.isNotBlank() },
                        exercise.equipment.takeIf { it.isNotBlank() },
                        exercise.difficulty.takeIf { it.isNotBlank() }
                    )
                    if (metaParts.isNotEmpty()) {
                        Text(
                            text = metaParts.joinToString(" • "),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                val context = LocalContext.current
                Text(
                    text = planned.toDisplaySummary(context),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    planned.toDisplayBadges(context).forEach { badge ->
                        Text(
                            text = badge,
                            style = MaterialTheme.typography.labelSmall,
                            color = FittyPink,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(FittyPink.copy(alpha = 0.1f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
