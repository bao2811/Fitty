package com.example.fitty.feature_plan

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
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.example.fitty.data.exercise.ExerciseGifDownloadManager
import com.example.fitty.domain.model.Exercise
import com.example.fitty.domain.model.ExerciseQuery
import com.example.fitty.domain.repository.ExerciseCatalogRepository
import com.example.fitty.ui.theme.FittyPink
import com.example.fitty.ui.theme.FittyPinkLight
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

// ── UI state ─────────────────────────────────────────────────────────

internal data class CategoryExerciseListState(
    val categoryLabel: String = "",
    val exercises: List<Exercise> = emptyList(),
    val isLoading: Boolean = true,
    val selectedExercise: Exercise? = null,
    val isGifLoading: Boolean = false
)

// ── ViewModel ────────────────────────────────────────────────────────

@HiltViewModel
class CategoryExerciseListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val exerciseRepository: ExerciseCatalogRepository,
    private val gifDownloadManager: ExerciseGifDownloadManager
) : ViewModel() {

    private val categoryLabel: String = savedStateHandle["categoryLabel"] ?: ""
    private val bodyPartKeysRaw: String = savedStateHandle["bodyPartKeys"] ?: ""

    private val _uiState = MutableStateFlow(
        CategoryExerciseListState(categoryLabel = categoryLabel)
    )
    internal val uiState: StateFlow<CategoryExerciseListState> = _uiState

    init {
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
                _uiState.update {
                    // Also refresh selected exercise if it was updated (e.g. gif downloaded)
                    val updatedSelected = it.selectedExercise?.let { sel ->
                        filtered.firstOrNull { ex -> ex.id == sel.id }
                    }
                    it.copy(
                        exercises = filtered,
                        isLoading = false,
                        selectedExercise = updatedSelected ?: it.selectedExercise
                    )
                }
            }
        }
    }

    internal fun selectExercise(exercise: Exercise) {
        _uiState.update { it.copy(selectedExercise = exercise) }
        // Trigger GIF download if needed
        if (exercise.localGifPath.isBlank() && exercise.gifUrl.isNotBlank()) {
            viewModelScope.launch {
                _uiState.update { it.copy(isGifLoading = true) }
                gifDownloadManager.download(exercise)
                // After download, refresh exercise from repository
                val updated = exerciseRepository.getExercise(exercise.id)
                _uiState.update {
                    it.copy(
                        selectedExercise = updated ?: exercise,
                        isGifLoading = false
                    )
                }
            }
        }
    }

    internal fun clearSelection() {
        _uiState.update { it.copy(selectedExercise = null, isGifLoading = false) }
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
    onDismissDetail: () -> Unit
) {
    // Show GIF dialog when exercise is selected
    if (state.selectedExercise != null) {
        ExerciseGifDialog(
            exercise = state.selectedExercise,
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
                            contentDescription = "Quay lại"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
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
        } else if (state.exercises.isEmpty()) {
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
                        text = "Chưa có bài tập nào",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Hãy đồng bộ dữ liệu từ Firebase",
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
                    Text(
                        text = "${state.exercises.size} bài tập",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                items(state.exercises, key = { it.id }) { exercise ->
                    ExerciseCard(
                        exercise = exercise,
                        onClick = { onExerciseClick(exercise) }
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
    isGifLoading: Boolean,
    onDismiss: () -> Unit
) {
    val imageLoader = rememberGifImageLoader()

    // Timer state
    var isTimerRunning by rememberSaveable { mutableStateOf(false) }
    var isPaused by rememberSaveable { mutableStateOf(false) }
    var elapsedSeconds by rememberSaveable { mutableStateOf(0) }
    var isCompleted by rememberSaveable { mutableStateOf(false) }

    val requiredSeconds = exercise.defaultDurationSeconds
        ?: exercise.durationSeconds.takeIf { it > 0 }
        ?: 30
    val progress = (elapsedSeconds.toFloat() / requiredSeconds).coerceAtMost(1f)
    val canComplete = elapsedSeconds >= requiredSeconds

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
                            contentDescription = "Đóng",
                            modifier = Modifier.size(18.dp)
                        )
                    }
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
                                text = "Đang tải GIF...",
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
                                text = "Không có hình ảnh",
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

                // ── Timer Section ──
                if (isTimerRunning || isCompleted) {
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
                            text = "%02d:%02d / %02d:%02d".format(elapsedMin, elapsedSec, reqMin, reqSec),
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
                            text = if (isCompleted) "✅ Hoàn thành!"
                            else if (canComplete) "Đủ thời gian! Bấm Done"
                            else "Đang tập...",
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
                                Text(" Hoàn thành!", fontWeight = FontWeight.Bold)
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
                                    " Bắt đầu tập · ${requiredSeconds}s",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        else -> {
                            // Running — show Pause + Done
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
                                Text(
                                    if (isPaused) " Tiếp" else " Dừng",
                                    fontWeight = FontWeight.SemiBold
                                )
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
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Outlined.CheckCircle, null, modifier = Modifier.size(18.dp))
                                Text(" Done", fontWeight = FontWeight.Bold)
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
private fun ExerciseCard(
    exercise: Exercise,
    onClick: () -> Unit
) {
    val imageLoader = rememberGifImageLoader()

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
            }

            // Play icon
            Icon(
                imageVector = Icons.Outlined.PlayArrow,
                contentDescription = "Xem bài tập",
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
