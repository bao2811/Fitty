package com.example.fitty.feature_exercise

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.example.fitty.R
import com.example.fitty.core.ui.toStatusText
import com.example.fitty.domain.model.Exercise
import com.example.fitty.domain.model.ExerciseQuery
import com.example.fitty.domain.model.ExerciseSyncState
import com.example.fitty.domain.usecase.exercise.ObserveExerciseSyncStateUseCase
import com.example.fitty.domain.usecase.exercise.ObserveExercisesUseCase
import com.example.fitty.domain.usecase.exercise.SyncExercisesUseCase
import com.example.fitty.domain.usecase.exercise.ToggleExerciseFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExerciseListUiState(
    val searchQuery: String = "",
    val exercises: List<Exercise> = emptyList(),
    val isSyncing: Boolean = false,
    val syncState: ExerciseSyncState = ExerciseSyncState(),
    val statusMessage: String? = null,
    val selectedDifficulty: String? = null
)

@HiltViewModel
class ExerciseListViewModel @Inject constructor(
    private val observeExercisesUseCase: ObserveExercisesUseCase,
    private val observeExerciseSyncStateUseCase: ObserveExerciseSyncStateUseCase,
    private val syncExercisesUseCase: SyncExercisesUseCase,
    private val toggleExerciseFavoriteUseCase: ToggleExerciseFavoriteUseCase
) : ViewModel() {
    private val searchQuery = MutableStateFlow("")
    private val selectedDifficulty = MutableStateFlow<String?>(null)
    private val _uiState = MutableStateFlow(ExerciseListUiState())
    val uiState: StateFlow<ExerciseListUiState> = _uiState

    init {
        viewModelScope.launch {
            combine(searchQuery, selectedDifficulty) { query, difficulty ->
                ExerciseQuery(searchQuery = query, difficulty = difficulty, limit = 100, offset = 0)
            }.collect { query ->
                observeExercisesUseCase(query).collect { exercises ->
                    _uiState.update { current ->
                        current.copy(
                            searchQuery = query.searchQuery,
                            selectedDifficulty = query.difficulty,
                            exercises = exercises
                        )
                    }
                }
            }
        }
        viewModelScope.launch {
            observeExerciseSyncStateUseCase().collect { syncState ->
                _uiState.update { current ->
                    current.copy(
                        isSyncing = syncState.isSyncing,
                        syncState = syncState,
                        statusMessage = syncState.lastErrorMessage
                            ?: syncState.lastSuccessfulSyncAt?.let { "SYNC_SUCCESS" }
                            ?: current.statusMessage
                    )
                }
            }
        }
    }

    fun setSearchQuery(value: String) {
        searchQuery.value = value
    }

    fun setDifficulty(value: String?) {
        selectedDifficulty.value = value
    }

    fun refresh() {
        viewModelScope.launch {
            syncExercisesUseCase(force = true)
        }
    }

    fun toggleFavorite(exercise: Exercise) {
        viewModelScope.launch {
            toggleExerciseFavoriteUseCase(exercise.id, !exercise.isFavorite)
        }
    }
}

@Composable
fun ExerciseListRoute(
    onExerciseSelected: (String) -> Unit,
    viewModel: ExerciseListViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    ExerciseListScreen(
        state = state,
        onSearchChanged = viewModel::setSearchQuery,
        onDifficultySelected = viewModel::setDifficulty,
        onRefresh = viewModel::refresh,
        onFavoriteToggle = viewModel::toggleFavorite,
        onExerciseSelected = onExerciseSelected
    )
}

@Composable
fun ExerciseListScreen(
    state: ExerciseListUiState,
    onSearchChanged: (String) -> Unit,
    onDifficultySelected: (String?) -> Unit,
    onRefresh: () -> Unit,
    onFavoriteToggle: (Exercise) -> Unit,
    onExerciseSelected: (String) -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = onSearchChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.exercise_search_label)) }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf<String?>(null, "Beginner", "Intermediate", "Advanced").forEach { difficulty ->
                FilterChip(
                    selected = state.selectedDifficulty == difficulty,
                    onClick = { onDifficultySelected(difficulty) },
                    label = {
                        Text(
                            when (difficulty) {
                                null -> stringResource(R.string.exercise_filter_all)
                                "Beginner" -> stringResource(R.string.plan_level_beginner)
                                "Intermediate" -> stringResource(R.string.common_label_intermediate)
                                else -> stringResource(R.string.common_label_advanced)
                            }
                        )
                    }
                )
            }
        }
        Button(onClick = onRefresh, enabled = !state.isSyncing) {
            Text(
                if (state.isSyncing) {
                    stringResource(R.string.exercise_syncing)
                } else {
                    stringResource(R.string.exercise_refresh_metadata)
                }
            )
        }
        state.statusMessage?.let { status ->
            Text(
                text = state.syncState.toStatusText(context = context)
                    ?: if (status == "SYNC_SUCCESS") {
                        stringResource(R.string.exercise_sync_cached)
                    } else {
                        status
                    },
                style = MaterialTheme.typography.bodySmall
            )
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(state.exercises, key = { it.id }) { exercise ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onExerciseSelected(exercise.id) }
                ) {
                    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier.size(72.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = exercise.localThumbnailPath.ifBlank { exercise.thumbnailUrl },
                                contentDescription = exercise.name
                            )
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(exercise.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                stringResource(
                                    R.string.exercise_meta_muscle_difficulty,
                                    exercise.muscleGroup,
                                    exercise.difficulty
                                ),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                stringResource(
                                    R.string.exercise_meta_calories_duration,
                                    exercise.caloriesBurned,
                                    exercise.durationSeconds
                                ),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                if (exercise.localVideoPath.isNotBlank()) {
                                    stringResource(R.string.exercise_offline_video_ready)
                                } else {
                                    stringResource(R.string.exercise_tap_to_stream)
                                },
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        Text(
                            text = if (exercise.isFavorite) {
                                stringResource(R.string.exercise_unfavorite)
                            } else {
                                stringResource(R.string.exercise_favorite)
                            },
                            modifier = Modifier.clickable { onFavoriteToggle(exercise) }
                        )
                    }
                }
            }
        }
    }
}
