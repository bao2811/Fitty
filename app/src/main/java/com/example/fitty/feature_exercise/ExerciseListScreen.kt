package com.example.fitty.feature_exercise

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.example.fitty.domain.model.Exercise
import com.example.fitty.domain.model.ExerciseQuery
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
                        statusMessage = syncState.lastErrorMessage
                            ?: syncState.lastSuccessfulSyncAt?.let { "Metadata cached for offline use." }
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
            label = { Text("Search exercises") }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(null, "Beginner", "Intermediate", "Advanced").forEach { difficulty ->
                FilterChip(
                    selected = state.selectedDifficulty == difficulty,
                    onClick = { onDifficultySelected(difficulty) },
                    label = { Text(difficulty ?: "All") }
                )
            }
        }
        Button(onClick = onRefresh, enabled = !state.isSyncing) {
            Text(if (state.isSyncing) "Syncing..." else "Refresh Metadata")
        }
        state.statusMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(state.exercises, key = { it.id }) { exercise ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onExerciseSelected(exercise.id) }
                ) {
                    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AsyncImage(
                            model = exercise.localThumbnailPath.ifBlank { exercise.thumbnailUrl },
                            contentDescription = exercise.name
                        )
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(exercise.name, style = MaterialTheme.typography.titleMedium)
                            Text("${exercise.muscleGroup} - ${exercise.difficulty}", style = MaterialTheme.typography.bodySmall)
                            Text("${exercise.caloriesBurned} kcal - ${exercise.durationSeconds}s", style = MaterialTheme.typography.bodySmall)
                            Text(if (exercise.localVideoPath.isNotBlank()) "Offline video ready" else "Tap to stream", style = MaterialTheme.typography.labelSmall)
                        }
                        Text(
                            text = if (exercise.isFavorite) "Unfavorite" else "Favorite",
                            modifier = Modifier.clickable { onFavoriteToggle(exercise) }
                        )
                    }
                }
            }
        }
    }
}
