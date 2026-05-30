package com.example.fitty.feature_plan

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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.example.fitty.domain.repository.ContentRepository
import com.example.fitty.domain.repository.ExerciseCatalogRepository
import com.example.fitty.domain.repository.SessionRepository
import com.example.fitty.domain.usecase.exercise.ObserveExerciseSyncStateUseCase
import com.example.fitty.domain.usecase.user.GetCurrentUserUseCase
import com.example.fitty.ui.theme.FittyPink
import com.example.fitty.ui.theme.FittyPinkLight
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import javax.inject.Inject

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
    val isGifLoading: Boolean = false
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
    private val exercisePrescriptionResolver: ExercisePrescriptionResolver
) : ViewModel() {

    private val categoryLabel: String = Uri.decode(savedStateHandle["categoryLabel"] ?: "")
    private val bodyPartKeysRaw: String = Uri.decode(savedStateHandle["bodyPartKeys"] ?: "")

    private val _uiState = MutableStateFlow(
        CategoryExerciseListState(categoryLabel = categoryLabel)
    )
    internal val uiState: StateFlow<CategoryExerciseListState> = _uiState
    private var latestExercises: List<Exercise> = emptyList()
    private var currentLanguage: String = Locale.getDefault().language
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
            currentLanguage = sessionRepository.getAppLanguage().orEmpty().ifBlank { Locale.getDefault().language }
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
        onDismissDetail = viewModel::clearSelection
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
    onDismissDetail: () -> Unit
) {
    val context = LocalContext.current
    val selectedItem = state.selectedItem
    // Show GIF dialog when exercise is selected
    if (selectedItem != null) {
        ExerciseGifDialog(
            exercise = selectedItem.exercise,
            prescription = selectedItem.prescription,
            isGifLoading = state.isGifLoading,
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
                            text = stringResource(R.string.category_exercise_count, state.filteredExerciseItems.size),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                items(state.filteredExerciseItems, key = { it.exercise.id }) { item ->
                    ExerciseCard(
                        item = item,
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
private fun ExerciseGifDialog(
    exercise: Exercise,
    prescription: ExercisePrescriptionRecommendation?,
    isGifLoading: Boolean,
    onDismiss: () -> Unit
) {
    val imageLoader = rememberGifImageLoader()
    val context = LocalContext.current

    // Timer state
    var isTimerRunning by rememberSaveable { mutableStateOf(false) }
    var isPaused by rememberSaveable { mutableStateOf(false) }
    var elapsedSeconds by rememberSaveable { mutableStateOf(0) }
    var isCompleted by rememberSaveable { mutableStateOf(false) }

    val isStrengthPrescription = (prescription?.sets ?: 0) > 0 || !prescription?.reps.isNullOrBlank()
    val requiredSeconds = prescription?.durationSeconds?.takeIf { !isStrengthPrescription }
        ?: exercise.defaultDurationSeconds
        ?: exercise.durationSeconds.takeIf { it > 0 }
        ?: if (isStrengthPrescription) 0 else 30
    val progress = if (requiredSeconds > 0) (elapsedSeconds.toFloat() / requiredSeconds).coerceAtMost(1f) else 0f
    val canComplete = isStrengthPrescription || (requiredSeconds > 0 && elapsedSeconds >= requiredSeconds)

    // Timer tick
    androidx.compose.runtime.LaunchedEffect(isTimerRunning, isPaused) {
        while (isTimerRunning && !isPaused) {
            kotlinx.coroutines.delay(1000L)
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
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header with title and close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = exercise.name.ifBlank { exercise.id },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            isTimerRunning = false
                            onDismiss()
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = stringResource(R.string.common_close),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                prescription?.let {
                    ExercisePrescriptionTargetCard(
                        title = stringResource(R.string.workout_prescription_title),
                        summary = it.toDisplaySummary(context),
                        badges = it.toDisplayBadges(context)
                    )
                }

                // GIF / Image preview
                val gifModel: Any? = when {
                    exercise.localGifPath.isNotBlank() -> File(exercise.localGifPath)
                    exercise.gifUrl.isNotBlank() -> exercise.gifUrl
                    exercise.localThumbnailPath.isNotBlank() -> File(exercise.localThumbnailPath)
                    exercise.thumbnailUrl.isNotBlank() -> exercise.thumbnailUrl
                    else -> null
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.2f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFFE8DEF8)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isGifLoading) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                color = FittyPink,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = stringResource(R.string.category_loading_gif),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else if (gifModel != null) {
                        SubcomposeAsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(gifModel)
                                .crossfade(true)
                                .build(),
                            imageLoader = imageLoader,
                            contentDescription = exercise.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize(),
                            loading = {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = FittyPink,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FitnessCenter,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = stringResource(R.string.common_no_image),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // Exercise info tags
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (exercise.bodyPart.isNotBlank()) {
                        ExerciseTag(exercise.bodyPart)
                    }
                    if (exercise.target.isNotBlank()) {
                        ExerciseTag(exercise.target)
                    }
                    if (exercise.equipment.isNotBlank()) {
                        ExerciseTag(exercise.equipment)
                    }
                }
                val instructionText = exercise.instructions.ifBlank { exercise.description }
                    .ifBlank { exercise.steps.firstOrNull().orEmpty() }
                if (instructionText.isNotBlank()) {
                    Text(
                        text = instructionText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                prescription?.note.takeIf { !it.isNullOrBlank() }?.let { note ->
                    Text(
                        text = note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // ── Timer Section ──
                if ((requiredSeconds > 0 && isTimerRunning) || isCompleted) {
                    val elapsedMin = elapsedSeconds / 60
                    val elapsedSec = elapsedSeconds % 60
                    val reqMin = requiredSeconds / 60
                    val reqSec = requiredSeconds % 60

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = if (canComplete) Color(0xFF2ED573).copy(alpha = 0.08f)
                                else FittyPink.copy(alpha = 0.06f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.workout_timer_format, elapsedMin, elapsedSec, reqMin, reqSec),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (canComplete) Color(0xFF2ED573) else FittyPink
                        )
                        androidx.compose.material3.LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (canComplete) Color(0xFF2ED573) else FittyPink,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Text(
                            text = if (isCompleted) stringResource(R.string.workout_status_completed)
                            else if (canComplete && !isStrengthPrescription) stringResource(R.string.workout_status_ready_to_complete)
                            else if (isStrengthPrescription) stringResource(R.string.category_strength_in_progress)
                            else stringResource(R.string.workout_status_in_progress),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // ── Action Buttons ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when {
                        isCompleted -> {
                            // Already completed — just show close
                            androidx.compose.material3.Button(
                                onClick = {
                                    isTimerRunning = false
                                    onDismiss()
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF2ED573)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Outlined.CheckCircle, null, modifier = Modifier.size(18.dp))
                                Text(" ${stringResource(R.string.workout_status_completed)}", fontWeight = FontWeight.Bold)
                            }
                        }
                        !isTimerRunning -> {
                            // Not started — show Start button
                            androidx.compose.material3.Button(
                                onClick = { isTimerRunning = true; isPaused = false },
                                shape = RoundedCornerShape(14.dp),
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = FittyPink
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Outlined.PlayArrow, null, modifier = Modifier.size(20.dp))
                                Text(
                                    text = if (requiredSeconds > 0) {
                                        " ${stringResource(R.string.category_start_exercise_with_duration, requiredSeconds)}"
                                    } else {
                                        " ${stringResource(R.string.category_start_exercise)}"
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        else -> {
                            // Running — show Pause + Done
                            if (requiredSeconds > 0) {
                                androidx.compose.material3.OutlinedButton(
                                    onClick = { isPaused = !isPaused },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        if (isPaused) Icons.Outlined.PlayArrow
                                        else Icons.Outlined.Pause,
                                        null, modifier = Modifier.size(18.dp)
                                    )
                                    Text(" ${if (isPaused) stringResource(R.string.workout_resume) else stringResource(R.string.workout_pause)}", fontWeight = FontWeight.SemiBold)
                                }
                            }
                            androidx.compose.material3.Button(
                                onClick = {
                                    isTimerRunning = false
                                    isCompleted = true
                                },
                                enabled = canComplete,
                                shape = RoundedCornerShape(14.dp),
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF2ED573),
                                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = if (requiredSeconds > 0) Modifier.weight(1f) else Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Outlined.CheckCircle, null, modifier = Modifier.size(18.dp))
                                Text(" ${stringResource(R.string.category_complete_exercise)}", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Exercise card ────────────────────────────────────────────────────

@Composable
private fun ExercisePrescriptionTargetCard(
    title: String,
    summary: String,
    badges: List<String>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(
                    listOf(FittyPink.copy(alpha = 0.12f), Color(0xFFFFF7FB))
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = summary,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            badges.forEach { badge ->
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall,
                    color = FittyPink,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.72f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun ExerciseCard(
    item: CategoryExerciseItemUi,
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

            // Play icon
            Icon(
                imageVector = Icons.Outlined.PlayArrow,
                contentDescription = stringResource(R.string.category_view_exercise),
                tint = FittyPink,
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
