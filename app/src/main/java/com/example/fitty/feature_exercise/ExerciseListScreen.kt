package com.example.fitty.feature_exercise

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

private val ExerciseListAccent = Color(0xFFE91E8F)
private val ExerciseListAccentSoft = Color(0xFFFDE7F3)

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
    onBack: () -> Unit = {},
    onExerciseSelected: (String) -> Unit,
    viewModel: ExerciseListViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    ExerciseListScreen(
        state = state,
        onBack = onBack,
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
    onBack: () -> Unit,
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
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.common_back)
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.plan_section_exercise_library),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = stringResource(R.string.plan_exercise_library_intro),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.size(48.dp))
                }
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = onSearchChanged,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(Icons.Outlined.Search, contentDescription = null)
                    },
                    label = { Text(stringResource(R.string.exercise_search_label)) },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ExerciseListAccent,
                        focusedLeadingIconColor = ExerciseListAccent
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.FilterList,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                Button(
                    onClick = onRefresh,
                    enabled = !state.isSyncing,
                    shape = RoundedCornerShape(18.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = ExerciseListAccent
                    )
                ) {
                    Text(
                        if (state.isSyncing) {
                            stringResource(R.string.exercise_syncing)
                        } else {
                            stringResource(R.string.exercise_refresh_metadata)
                        },
                        fontWeight = FontWeight.Bold
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
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.exercises.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text(
                            text = state.syncState.toStatusText(context)
                                ?: stringResource(R.string.exercise_sync_empty_remote),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
            items(state.exercises, key = { it.id }) { exercise ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(26.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onExerciseSelected(exercise.id) }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(92.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFFFDE8F2), Color(0xFFF6EEF8))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = exercise.localGifPath.ifBlank {
                                    exercise.gifUrl.ifBlank {
                                        exercise.localThumbnailPath.ifBlank { exercise.thumbnailUrl }
                                    }
                                },
                                contentDescription = exercise.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    exercise.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { onFavoriteToggle(exercise) },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(ExerciseListAccentSoft)
                                ) {
                                    Icon(
                                        if (exercise.isFavorite) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                                        contentDescription = null,
                                        tint = ExerciseListAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ExerciseMetaChip(exercise.muscleGroup.ifBlank { exercise.target })
                                exercise.difficulty.takeIf { it.isNotBlank() }?.let { ExerciseMetaChip(it) }
                            }
                            Text(
                                stringResource(
                                    R.string.exercise_meta_calories_duration,
                                    exercise.caloriesBurned,
                                    exercise.durationSeconds
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(Color(0xFFF8F1F6))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Outlined.PlayArrow,
                                            contentDescription = null,
                                            tint = ExerciseListAccent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = if (exercise.localVideoPath.isNotBlank()) {
                                                stringResource(R.string.exercise_offline_video_ready)
                                            } else {
                                                stringResource(R.string.exercise_tap_to_stream)
                                            },
                                            style = MaterialTheme.typography.labelMedium,
                                            color = ExerciseListAccent
                                        )
                                    }
                                }
                                if (exercise.caloriesBurned > 0) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Outlined.Bolt,
                                            contentDescription = null,
                                            tint = Color(0xFFFF9800),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            "${exercise.caloriesBurned} kcal",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseMetaChip(text: String) {
    if (text.isBlank()) return
    Text(
        text = text,
        color = ExerciseListAccent,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(ExerciseListAccentSoft)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    )
}
