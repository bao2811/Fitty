package com.example.fitty.feature_plan

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SportsScore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.example.fitty.R
import com.example.fitty.core.ui.AppLocaleManager
import com.example.fitty.core.ui.toDisplayBadges
import com.example.fitty.core.ui.toDisplaySummary
import com.example.fitty.core.ui.exerciseCategoryEmptyHintText
import com.example.fitty.data.content.ExercisePrescriptionResolver
import com.example.fitty.data.exercise.ExerciseGifDownloadManager
import com.example.fitty.domain.model.Exercise
import com.example.fitty.domain.model.ExercisePrescriptionContent
import com.example.fitty.domain.model.ExercisePrescriptionRecommendation
import com.example.fitty.domain.model.ExerciseQuery
import com.example.fitty.domain.model.FittyUser
import com.example.fitty.domain.model.DailySummary
import com.example.fitty.domain.model.DailySummaryTargets
import com.example.fitty.domain.repository.ContentRepository
import com.example.fitty.domain.repository.ExerciseCatalogRepository
import com.example.fitty.domain.repository.SessionRepository
import com.example.fitty.domain.repository.TrackingRepository
import com.example.fitty.domain.usecase.exercise.ObserveExerciseSyncStateUseCase
import com.example.fitty.domain.usecase.user.GetCurrentUserUseCase
import com.example.fitty.domain.usecase.user.UpdateStreakUseCase
import com.example.fitty.ui.theme.FittyPink
import com.example.fitty.ui.theme.FittyPinkLight
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.delay

// ── UI state ─────────────────────────────────────────────────────────

internal data class CategoryExerciseItemUi(
    val exercise: Exercise,
    val prescription: ExercisePrescriptionRecommendation? = null
)

internal data class CategoryExerciseListState(
    val categoryLabel: String = "",
    val exerciseItems: List<CategoryExerciseItemUi> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val syncStatusCode: String? = null,
    val selectedExerciseId: String? = null,
    val isGifLoading: Boolean = false,
    val completedExerciseIds: Set<String> = emptySet()
) {
    val filteredExerciseItems: List<CategoryExerciseItemUi>
        get() {
            val query = searchQuery.trim().lowercase(Locale.ROOT)
            if (query.isBlank()) return exerciseItems
            return exerciseItems.filter { item ->
                val exercise = item.exercise
                listOf(
                    exercise.name,
                    exercise.target,
                    exercise.bodyPart,
                    exercise.equipment,
                    exercise.description,
                    exercise.instructions
                ).any { value -> value.contains(query, ignoreCase = true) }
            }
        }
    val selectedItem: CategoryExerciseItemUi?
        get() = selectedExerciseId?.let { selectedId -> exerciseItems.firstOrNull { it.exercise.id == selectedId } }
}

// ── ViewModel ────────────────────────────────────────────────────────

@HiltViewModel
class CategoryExerciseListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val exerciseRepository: ExerciseCatalogRepository,
    private val observeExerciseSyncStateUseCase: ObserveExerciseSyncStateUseCase,
    private val gifDownloadManager: ExerciseGifDownloadManager,
    private val contentRepository: ContentRepository,
    private val sessionRepository: SessionRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val exercisePrescriptionResolver: ExercisePrescriptionResolver,
    private val trackingRepository: TrackingRepository,
    private val updateStreakUseCase: UpdateStreakUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val categoryLabel: String = Uri.decode(savedStateHandle["categoryLabel"] ?: "")
    private val bodyPartKeysRaw: String = Uri.decode(savedStateHandle["bodyPartKeys"] ?: "")

    private val _uiState = MutableStateFlow(
        CategoryExerciseListState(categoryLabel = categoryLabel)
    )
    internal val uiState: StateFlow<CategoryExerciseListState> = _uiState
    private var latestExercises: List<Exercise> = emptyList()
    private var currentLanguage: String = AppLocaleManager.resolveStoredLanguage(context)
    private var currentUser: FittyUser? = null
    private var prescriptionCatalog: List<ExercisePrescriptionContent> = emptyList()

    init {
        loadPrescriptionContext()
        observeSyncState()
        loadExercises()
    }

    private fun loadExercises() {
        val keys = bodyPartKeysRaw.split(",").filter { it.isNotBlank() }
        viewModelScope.launch {
            exerciseRepository.observeExercises(ExerciseQuery(limit = 500)).collect { allExercises ->
                val filtered = if (keys.isNotEmpty()) {
                    allExercises.filter { exercise ->
                        keys.any { key ->
                            exercise.bodyPart.equals(key, ignoreCase = true)
                        }
                    }
                } else {
                    emptyList()
                }
                latestExercises = filtered
                rebuildExerciseItems()
            }
        }
    }

    private fun loadPrescriptionContext() {
        viewModelScope.launch {
            currentLanguage = AppLocaleManager.resolveStoredLanguage(context)
            currentUser = runCatching { getCurrentUserUseCase() }.getOrNull()
            prescriptionCatalog = runCatching {
                contentRepository.getExercisePrescriptions(currentLanguage)
            }.getOrDefault(emptyList())
            rebuildExerciseItems()
        }
    }

    private fun rebuildExerciseItems() {
        _uiState.update { state ->
            state.copy(
                exerciseItems = latestExercises.map { exercise ->
                    CategoryExerciseItemUi(
                        exercise = exercise,
                        prescription = exercisePrescriptionResolver.resolve(
                            exercise = exercise,
                            user = currentUser,
                            language = currentLanguage,
                            catalog = prescriptionCatalog
                        )
                    )
                },
                isLoading = false
            )
        }
    }

    private fun observeSyncState() {
        viewModelScope.launch {
            observeExerciseSyncStateUseCase().collect { syncState ->
                _uiState.update { state ->
                    state.copy(syncStatusCode = syncState.statusCode)
                }
            }
        }
    }

    internal fun selectExercise(exercise: Exercise) {
        _uiState.update { it.copy(selectedExerciseId = exercise.id) }
        // Trigger GIF download if needed
        if (exercise.localGifPath.isBlank() && exercise.gifUrl.isNotBlank()) {
            viewModelScope.launch {
                _uiState.update { it.copy(isGifLoading = true) }
                gifDownloadManager.download(exercise)
                // After download, refresh exercise from repository
                val updated = exerciseRepository.getExercise(exercise.id) ?: exercise
                latestExercises = latestExercises.map { current ->
                    if (current.id == updated.id) updated else current
                }
                rebuildExerciseItems()
                _uiState.update { it.copy(isGifLoading = false) }
            }
        }
    }

    internal fun clearSelection() {
        _uiState.update { it.copy(selectedExerciseId = null, isGifLoading = false) }
    }

    internal fun markExerciseCompleted(exerciseId: String, elapsedSeconds: Int) {
        if (exerciseId.isBlank()) return
        if (_uiState.value.completedExerciseIds.contains(exerciseId)) return
        _uiState.update { state ->
            state.copy(completedExerciseIds = state.completedExerciseIds + exerciseId)
        }
        viewModelScope.launch {
            recordExerciseCompletion(exerciseId, elapsedSeconds)
        }
    }

    private suspend fun recordExerciseCompletion(exerciseId: String, elapsedSeconds: Int) {
        val uid = sessionRepository.getCurrentUserId() ?: return
        val item = _uiState.value.exerciseItems.firstOrNull { it.exercise.id == exerciseId } ?: return
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val user = currentUser ?: runCatching { getCurrentUserUseCase() }.getOrNull()
        val existing = trackingRepository.getDailySummary(uid, today)
        val baseSummary = existing ?: DailySummary(
            dateKey = today,
            targets = DailySummaryTargets(
                calories = user?.settings?.calorieTarget ?: 2100,
                waterMl = user?.settings?.waterGoalMl ?: 2500
            )
        )
        val activeMinutes = maxOf(1, (elapsedSeconds + 59) / 60)
        val caloriesBurned = item.exercise.caloriesBurned.takeIf { it > 0 }
            ?: maxOf(1, (activeMinutes * 5))
        val updated = baseSummary.copy(
            todayWorkoutTitle = item.exercise.name.ifBlank { baseSummary.todayWorkoutTitle },
            progress = baseSummary.progress.copy(
                workoutsCompleted = baseSummary.progress.workoutsCompleted + 1,
                caloriesBurned = baseSummary.progress.caloriesBurned + caloriesBurned,
                activeMinutes = baseSummary.progress.activeMinutes + activeMinutes
            )
        )
        trackingRepository.updateDailySummary(uid, today, updated)
        updateStreakUseCase(reason = "workout", incrementActivityCounters = true)
    }

    internal fun updateSearchQuery(value: String) {
        _uiState.update { it.copy(searchQuery = value) }
    }
}

// ── Route ────────────────────────────────────────────────────────────

@Composable
fun CategoryExerciseListRoute(
    onBack: () -> Unit,
    viewModel: CategoryExerciseListViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    CategoryExerciseListScreen(
        state = state,
        onBack = onBack,
        onExerciseClick = viewModel::selectExercise,
        onSearchQueryChange = viewModel::updateSearchQuery,
        onDismissDetail = viewModel::clearSelection,
        onExerciseCompleted = viewModel::markExerciseCompleted
    )
}

// ── Screen ───────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryExerciseListScreen(
    state: CategoryExerciseListState,
    onBack: () -> Unit,
    onExerciseClick: (Exercise) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onDismissDetail: () -> Unit,
    onExerciseCompleted: (String, Int) -> Unit
) {
    val context = LocalContext.current
    val selectedItem = state.selectedItem
    // Show GIF dialog when exercise is selected
    if (selectedItem != null) {
        CategoryQuickExerciseDialog(
            exercise = selectedItem.exercise,
            prescription = selectedItem.prescription,
            isGifLoading = state.isGifLoading,
            initialCompleted = state.completedExerciseIds.contains(selectedItem.exercise.id),
            onCompleted = onExerciseCompleted,
            onDismiss = onDismissDetail
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.categoryLabel,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    ) { innerPadding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = FittyPink)
            }
        } else if (state.filteredExerciseItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.FitnessCenter,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.category_no_exercises),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = exerciseCategoryEmptyHintText(context, state.syncStatusCode),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = onSearchQueryChange,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text(stringResource(R.string.exercise_search_label)) }
                        )
                        Text(
                            text = "${state.completedExerciseIds.size}/${state.filteredExerciseItems.size} đã tập",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                items(state.filteredExerciseItems, key = { it.exercise.id }) { item ->
                    ExerciseCard(
                        item = item,
                        isCompleted = state.completedExerciseIds.contains(item.exercise.id),
                        onClick = { onExerciseClick(item.exercise) }
                    )
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

// ── GIF Dialog ───────────────────────────────────────────────────────

@Composable
private fun CategoryQuickExerciseDialog(
    exercise: Exercise,
    prescription: ExercisePrescriptionRecommendation?,
    isGifLoading: Boolean,
    initialCompleted: Boolean,
    onCompleted: (String, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val imageLoader = rememberGifImageLoader()
    val context = LocalContext.current
    var isTimerRunning by rememberSaveable { mutableStateOf(false) }
    var isPaused by rememberSaveable { mutableStateOf(false) }
    var elapsedSeconds by rememberSaveable { mutableStateOf(0) }
    var isCompleted by rememberSaveable(exercise.id) { mutableStateOf(initialCompleted) }

    val isStrengthPrescription = (prescription?.sets ?: 0) > 0 || !prescription?.reps.isNullOrBlank()
    val requiredSeconds = prescription?.durationSeconds?.takeIf { !isStrengthPrescription }
        ?: exercise.defaultDurationSeconds
        ?: exercise.durationSeconds.takeIf { it > 0 }
        ?: if (isStrengthPrescription) 0 else 30
    val canComplete = if (isStrengthPrescription) {
        isTimerRunning
    } else {
        requiredSeconds > 0 && elapsedSeconds >= requiredSeconds
    }
    androidx.compose.runtime.LaunchedEffect(isTimerRunning, isPaused) {
        while (isTimerRunning && !isPaused) {
            delay(1000L)
            elapsedSeconds++
        }
    }

    Dialog(
        onDismissRequest = {
            isTimerRunning = false
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 12.dp),
            shape = RoundedCornerShape(30.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 16.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFFFFBFE))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 74.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CategoryQuickHero(
                        exercise = exercise,
                        isGifLoading = isGifLoading,
                        imageLoader = imageLoader,
                        elapsedSeconds = elapsedSeconds,
                        requiredSeconds = requiredSeconds,
                        isTimerRunning = isTimerRunning,
                        isPaused = isPaused,
                        onDismiss = {
                            isTimerRunning = false
                            onDismiss()
                        }
                    )
                    CategoryMetricsBox(
                        exercise = exercise,
                        prescription = prescription,
                        contextSummary = prescription?.toDisplaySummary(context),
                        requiredSeconds = requiredSeconds,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    CategoryWorkoutClockPanel(
                        elapsedSeconds = elapsedSeconds,
                        requiredSeconds = requiredSeconds,
                        isTimerRunning = isTimerRunning,
                        isPaused = isPaused,
                        isCompleted = isCompleted,
                        canComplete = canComplete,
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .weight(1f)
                    )
                prescription?.note.takeIf { false }?.let { note ->
                        CategoryQuickInfoCard(modifier = Modifier.padding(horizontal = 12.dp)) {
                            Text(stringResource(R.string.workout_suggestion_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                            Text(
                                text = note,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                    }
                }
            }
                CategoryQuickActions(
                    isCompleted = isCompleted,
                    isTimerRunning = isTimerRunning,
                    isPaused = isPaused,
                    requiredSeconds = requiredSeconds,
                    canComplete = canComplete,
                    onDismiss = {
                        isTimerRunning = false
                        onDismiss()
                    },
                    onStart = {
                        isTimerRunning = true
                        isPaused = false
                    },
                    onPauseToggle = { isPaused = !isPaused },
                    onComplete = {
                        isTimerRunning = false
                        isCompleted = true
                        onCompleted(exercise.id, elapsedSeconds)
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                )
        }
    }
}
}

@Composable
private fun CategoryMetricsBox(
    exercise: Exercise,
    prescription: ExercisePrescriptionRecommendation?,
    contextSummary: String?,
    requiredSeconds: Int,
    modifier: Modifier = Modifier
) {
    val level = when (exercise.difficulty.lowercase(Locale.US)) {
        "advanced", "hard", "expert" -> 2
        "beginner", "easy" -> 0
        else -> 1
    }
    val sets = prescription?.sets ?: 0
    val reps = prescription?.reps?.takeIf { it.isNotBlank() } ?: exercise.defaultRepsText
    val weightBasisShort = categoryWeightBasisShort(prescription)
    val suggestionText = listOfNotNull(
        contextSummary,
        weightBasisShort.takeIf { it.isNotBlank() }
    ).joinToString(" • ").ifBlank {
        reps.ifBlank { "${requiredSeconds.takeIf { it > 0 } ?: 30}s" }
    }
    val primary = exercise.primaryMuscleGroup.ifBlank { exercise.target.ifBlank { exercise.bodyPart } }
    val secondary = exercise.targetMuscles.filterNot { it.equals(primary, ignoreCase = true) }.distinct().take(2)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(stringResource(R.string.workout_intensity_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Brush.horizontalGradient(listOf(Color(0xFF43A047), Color(0xFFFFA000), FittyPink)))
                    ) {
                        Box(
                            modifier = Modifier
                                .align(when (level) {
                                    0 -> Alignment.CenterStart
                                    2 -> Alignment.CenterEnd
                                    else -> Alignment.Center
                                })
                                .size(10.dp)
                                .background(Color.White, CircleShape)
                                .padding(2.dp)
                                .background(Color(0xFFFF8A00), CircleShape)
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        CategoryIntensityLabel(stringResource(R.string.workout_intensity_light), Color(0xFF43A047), level == 0)
                        CategoryIntensityLabel(stringResource(R.string.workout_intensity_medium_short), Color(0xFFFF8A00), level == 1)
                        CategoryIntensityLabel(stringResource(R.string.workout_intensity_heavy), FittyPink, level == 2)
                    }
                }
                Column(
                    modifier = Modifier
                        .weight(0.82f)
                        .background(FittyPink.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 7.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.LocalFireDepartment, null, tint = FittyPink, modifier = Modifier.size(13.dp))
                        Text(stringResource(R.string.workout_suggestion_title), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = suggestionText,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            CategoryMetricDivider()

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
                CategoryQuickTargetMetric(Icons.Outlined.Refresh, reps.ifBlank { if (requiredSeconds > 0) requiredSeconds.toString() else "-" }, if (reps.isNotBlank()) stringResource(R.string.workout_unit_reps) else stringResource(R.string.workout_unit_seconds))
                CategoryQuickTargetMetric(Icons.Outlined.FitnessCenter, categoryWeightLabel(prescription) ?: "-", weightBasisShort)
                CategoryQuickTargetMetric(Icons.Outlined.SportsScore, if (sets > 0) sets.toString() else "1", stringResource(R.string.workout_unit_set))
            }

            CategoryMetricDivider()

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Brush.verticalGradient(listOf(FittyPink.copy(alpha = 0.14f), Color(0xFFF7F3F6)))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.FitnessCenter, null, tint = FittyPink, modifier = Modifier.size(20.dp))
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(stringResource(R.string.workout_target_muscle_groups), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold)
                    CategoryMuscleLine(primary.ifBlank { stringResource(R.string.workout_primary_muscle_fallback) }, exercise.target.ifBlank { exercise.bodyPart }, stringResource(R.string.workout_primary_badge), true)
                    secondary.forEach { muscle -> CategoryMuscleLine(muscle, exercise.bodyPart, stringResource(R.string.workout_secondary_badge), false) }
                }
            }
        }
    }
}

@Composable
private fun CategoryMetricDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(0xFFF1E3ED))
    )
}

@Composable
private fun CategoryQuickHero(
    exercise: Exercise,
    isGifLoading: Boolean,
    imageLoader: ImageLoader,
    elapsedSeconds: Int,
    requiredSeconds: Int,
    isTimerRunning: Boolean,
    isPaused: Boolean,
    onDismiss: () -> Unit
) {
    val imageModel: Any? = when {
        exercise.localGifPath.isNotBlank() -> File(exercise.localGifPath)
        exercise.gifUrl.isNotBlank() -> exercise.gifUrl
        exercise.localThumbnailPath.isNotBlank() -> File(exercise.localThumbnailPath)
        exercise.thumbnailUrl.isNotBlank() -> exercise.thumbnailUrl
        else -> null
    }
    val muscleLabel = exercise.primaryMuscleGroup.ifBlank { exercise.target.ifBlank { exercise.bodyPart } }
    val elapsedMin = elapsedSeconds / 60
    val elapsedSec = elapsedSeconds % 60
    val reqMin = requiredSeconds / 60
    val reqSec = requiredSeconds % 60
    val safeRequired = requiredSeconds.takeIf { it > 0 } ?: 1
    val timerProgress = (elapsedSeconds.toFloat() / safeRequired).coerceAtMost(1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(258.dp)
            .background(Color(0xFF24242A)),
        contentAlignment = Alignment.Center
    ) {
        when {
            isGifLoading -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(42.dp))
                    Text(
                        text = stringResource(R.string.category_loading_gif),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.82f)
                    )
                }
            }
            imageModel != null -> {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageModel)
                        .crossfade(true)
                        .build(),
                    imageLoader = imageLoader,
                    contentDescription = exercise.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    loading = {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = FittyPink, modifier = Modifier.size(36.dp))
                        }
                    }
                )
            }
            else -> {
                Icon(
                    imageVector = Icons.Outlined.FitnessCenter,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.56f),
                    modifier = Modifier.size(74.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.48f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.58f)
                        )
                    )
                )
        )

        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.36f))
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = stringResource(R.string.common_close),
                tint = Color.White
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = exercise.name.ifBlank { exercise.id },
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (muscleLabel.isNotBlank()) {
                ExerciseTag(muscleLabel)
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                if (isTimerRunning && !isPaused) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = "%02d:%02d".format(elapsedMin, elapsedSec),
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            LinearProgressIndicator(
                progress = { timerProgress },
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(999.dp)),
                color = FittyPink,
                trackColor = Color.White.copy(alpha = 0.34f)
            )
            Text(
                text = if (requiredSeconds > 0) "%02d:%02d".format(reqMin, reqSec) else "--:--",
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun CategoryQuickActions(
    isCompleted: Boolean,
    isTimerRunning: Boolean,
    isPaused: Boolean,
    requiredSeconds: Int,
    canComplete: Boolean,
    onDismiss: () -> Unit,
    onStart: () -> Unit,
    onPauseToggle: () -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        when {
            isCompleted -> {
                Button(onClick = onDismiss, shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2ED573)), modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Icon(Icons.Outlined.CheckCircle, null, modifier = Modifier.size(18.dp))
                    Text(" ${stringResource(R.string.workout_status_completed)}", fontWeight = FontWeight.Bold)
                }
            }
            !isTimerRunning -> {
                Button(onClick = onStart, shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = FittyPink), modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Icon(Icons.Outlined.PlayArrow, null, modifier = Modifier.size(20.dp))
                    Text(if (requiredSeconds > 0) " ${stringResource(R.string.category_start_exercise_with_duration, requiredSeconds)}" else " ${stringResource(R.string.category_start_exercise)}", fontWeight = FontWeight.ExtraBold)
                }
            }
            else -> {
                OutlinedButton(onClick = onPauseToggle, shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f).height(48.dp)) {
                    Icon(if (isPaused) Icons.Outlined.PlayArrow else Icons.Outlined.Pause, null, modifier = Modifier.size(18.dp))
                    Text(" ${if (isPaused) stringResource(R.string.workout_resume) else stringResource(R.string.workout_pause)}", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onComplete,
                    enabled = canComplete,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2ED573), disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Icon(Icons.Outlined.CheckCircle, null, modifier = Modifier.size(18.dp))
                    Text(
                        if (requiredSeconds > 0) {
                            " ${stringResource(R.string.category_complete_exercise)}"
                        } else {
                            " Kết thúc"
                        },
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryWorkoutClockPanel(
    elapsedSeconds: Int,
    requiredSeconds: Int,
    isTimerRunning: Boolean,
    isPaused: Boolean,
    isCompleted: Boolean,
    canComplete: Boolean,
    modifier: Modifier = Modifier
) {
    val elapsedMin = elapsedSeconds / 60
    val elapsedSec = elapsedSeconds % 60
    val progress = if (requiredSeconds > 0) {
        (elapsedSeconds.toFloat() / requiredSeconds).coerceAtMost(1f)
    } else {
        0f
    }
    val status = when {
        isCompleted -> stringResource(R.string.workout_status_completed)
        !isTimerRunning -> "Sẵn sàng"
        isPaused -> stringResource(R.string.workout_pause)
        canComplete -> stringResource(R.string.workout_status_ready_to_complete)
        else -> stringResource(R.string.workout_status_in_progress)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = FittyPink.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = status,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "%02d:%02d".format(elapsedMin, elapsedSec),
                style = MaterialTheme.typography.headlineMedium,
                color = if (isCompleted) Color(0xFF2ED573) else FittyPink,
                fontWeight = FontWeight.ExtraBold
            )
            if (requiredSeconds > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    color = if (canComplete) Color(0xFF2ED573) else FittyPink,
                    trackColor = Color.White.copy(alpha = 0.72f)
                )
            }
        }
    }
}

@Composable
private fun CategoryQuickInfoCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp), content = content)
    }
}

@Composable
private fun CategoryIntensityLabel(text: String, color: Color, selected: Boolean) {
    Text(text = text, style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = if (selected) 1f else 0.82f), fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold)
}

@Composable
private fun CategoryQuickTargetMetric(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Box(modifier = Modifier.size(28.dp).background(FittyPink.copy(alpha = 0.10f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = FittyPink, modifier = Modifier.size(14.dp))
        }
        Text(value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (label.isNotBlank()) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CategoryMuscleLine(title: String, subtitle: String, badge: String, primary: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(if (primary) 9.dp else 7.dp).background(if (primary) FittyPink else MaterialTheme.colorScheme.outline, CircleShape))
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = badge,
                style = MaterialTheme.typography.labelSmall,
                color = if (primary) FittyPink else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.background(if (primary) FittyPink.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(999.dp)).padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
        if (subtitle.isNotBlank()) {
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

private fun categoryWeightLabel(prescription: ExercisePrescriptionRecommendation?): String? {
    prescription?.targetWeightLabel?.takeIf { it.isNotBlank() }?.let { return it }
    val weight = prescription?.targetWeightKg ?: return null
    return if (weight % 1f == 0f) "${weight.toInt()} kg" else String.format(Locale.US, "%.1f kg", weight)
}

private fun categoryWeightBasisShort(prescription: ExercisePrescriptionRecommendation?): String {
    val debugSummary = prescription?.debugSummary.orEmpty()
    val userWeight = Regex("userWeight=([0-9]+)kg").find(debugSummary)?.groupValues?.getOrNull(1)
    return userWeight?.let { "theo $it kg" }.orEmpty()
}

// -- Exercise card ────────────────────────────────────────────────────

@Composable
private fun ExerciseCard(
    item: CategoryExerciseItemUi,
    isCompleted: Boolean,
    onClick: () -> Unit
) {
    val imageLoader = rememberGifImageLoader()
    val exercise = item.exercise

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            val imageModel: Any? = when {
                exercise.localThumbnailPath.isNotBlank() -> File(exercise.localThumbnailPath)
                exercise.thumbnailUrl.isNotBlank() -> exercise.thumbnailUrl
                exercise.localGifPath.isNotBlank() -> File(exercise.localGifPath)
                exercise.gifUrl.isNotBlank() -> exercise.gifUrl
                else -> null
            }

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
                        imageLoader = imageLoader,
                        contentDescription = exercise.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.FitnessCenter,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Exercise info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exercise.name.ifBlank { exercise.id },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (exercise.target.isNotBlank()) {
                        ExerciseTag(exercise.target)
                    }
                    if (exercise.equipment.isNotBlank()) {
                        ExerciseTag(exercise.equipment)
                    }
                }
                item.prescription?.let { prescription ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.exercise_prescription_label, prescription.toDisplaySummary(LocalContext.current)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        prescription.toDisplayBadges(LocalContext.current).take(3).forEach { badge ->
                            ExerciseTag(badge)
                        }
                    }
                }
            }

            Icon(
                imageVector = if (isCompleted) Icons.Outlined.CheckCircle else Icons.Outlined.PlayArrow,
                contentDescription = stringResource(R.string.category_view_exercise),
                tint = if (isCompleted) Color(0xFF2ED573) else FittyPink,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun ExerciseTag(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = FittyPink,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .background(
                color = FittyPink.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

@Composable
private fun rememberGifImageLoader(): ImageLoader {
    val context = LocalContext.current
    return remember(context) {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }
}
