package com.example.fitty.feature_exercise

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Scale
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.example.fitty.R
import com.example.fitty.core.ui.AppLocaleManager
import com.example.fitty.core.ui.toDisplayBadges
import com.example.fitty.core.ui.toDisplaySummary
import com.example.fitty.data.content.ExercisePrescriptionResolver
import com.example.fitty.data.exercise.ExerciseVideoDownloadManager
import com.example.fitty.domain.model.Exercise
import com.example.fitty.domain.model.ExercisePrescriptionRecommendation
import com.example.fitty.domain.repository.ContentRepository
import com.example.fitty.domain.repository.SessionRepository
import com.example.fitty.domain.usecase.exercise.GetExerciseUseCase
import com.example.fitty.domain.usecase.exercise.RecordRecentlyViewedExerciseUseCase
import com.example.fitty.domain.usecase.user.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

private val ExerciseAccent = Color(0xFFE91E8F)
private val ExerciseAccentSoft = Color(0xFFFDE7F3)
private val ExerciseSurface = Color(0xFFFFFBFE)

data class ExerciseDetailUiState(
    val exercise: Exercise? = null,
    val prescription: ExercisePrescriptionRecommendation? = null,
    val statusMessage: String? = null
)

@HiltViewModel
class ExerciseDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getExerciseUseCase: GetExerciseUseCase,
    private val recordRecentlyViewedExerciseUseCase: RecordRecentlyViewedExerciseUseCase,
    private val videoDownloadManager: ExerciseVideoDownloadManager,
    private val contentRepository: ContentRepository,
    private val sessionRepository: SessionRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val exercisePrescriptionResolver: ExercisePrescriptionResolver,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val exerciseId: String = checkNotNull(savedStateHandle["exerciseId"])
    private val _uiState = MutableStateFlow(ExerciseDetailUiState())
    val uiState: StateFlow<ExerciseDetailUiState> = _uiState

    init {
        viewModelScope.launch {
            val exercise = getExerciseUseCase(exerciseId)
            if (exercise != null) {
                recordRecentlyViewedExerciseUseCase(exercise.id)
            }
            val language = AppLocaleManager.resolveStoredLanguage(context)
            val currentUser = runCatching { getCurrentUserUseCase() }.getOrNull()
            val catalog = runCatching { contentRepository.getExercisePrescriptions(language) }.getOrDefault(emptyList())
            _uiState.value = ExerciseDetailUiState(
                exercise = exercise,
                prescription = exercise?.let {
                    exercisePrescriptionResolver.resolve(
                        exercise = it,
                        user = currentUser,
                        language = language,
                        catalog = catalog
                    )
                }
            )
        }
    }

    fun downloadVideo() {
        val exercise = _uiState.value.exercise ?: return
        viewModelScope.launch {
            val result = videoDownloadManager.download(exercise)
            _uiState.value = _uiState.value.copy(
                exercise = getExerciseUseCase(exercise.id),
                statusMessage = result.fold(
                    onSuccess = { "VIDEO_DOWNLOAD_SUCCESS" },
                    onFailure = { it.message ?: "VIDEO_DOWNLOAD_FAILED" }
                )
            )
        }
    }

    fun deleteDownloadedVideo() {
        val exercise = _uiState.value.exercise ?: return
        viewModelScope.launch {
            videoDownloadManager.deleteDownloadedVideo(exercise)
            _uiState.value = _uiState.value.copy(
                exercise = getExerciseUseCase(exercise.id),
                statusMessage = "VIDEO_DELETE_SUCCESS"
            )
        }
    }
}

@Composable
fun ExerciseDetailRoute(
    onBack: () -> Unit,
    onPlayVideo: (String) -> Unit,
    viewModel: ExerciseDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    ExerciseDetailScreen(
        state = state,
        onBack = onBack,
        onPlayVideo = onPlayVideo,
        onDownloadVideo = viewModel::downloadVideo,
        onDeleteVideo = viewModel::deleteDownloadedVideo
    )
}

@Composable
fun ExerciseDetailScreen(
    state: ExerciseDetailUiState,
    onBack: () -> Unit,
    onPlayVideo: (String) -> Unit,
    onDownloadVideo: () -> Unit,
    onDeleteVideo: () -> Unit
) {
    val exercise = state.exercise ?: return
    val context = LocalContext.current
    val description = exercise.description.ifBlank { exercise.instructions }
        .ifBlank { exercise.steps.firstOrNull().orEmpty() }
    val badges = state.prescription?.toDisplayBadges(context).orEmpty()
    val suggestion = state.prescription?.note
        ?.takeIf { it.isNotBlank() }
        ?: stringResource(R.string.exercise_prescription_label, state.prescription?.toDisplaySummary(context).orEmpty())
    val targetSections = buildList {
        state.prescription?.reps?.takeIf { it.isNotBlank() }?.let { add("reps" to it) }
        state.prescription?.targetWeightLabel?.takeIf { it.isNotBlank() }?.let { add("weight" to it) }
        state.prescription?.targetWeightKg?.let { add("weight" to (if (it % 1f == 0f) "${it.toInt()} kg" else "$it kg")) }
        val sets = state.prescription?.sets ?: 0
        if (sets > 0) add("sets" to sets.toString())
        if (isEmpty() && exercise.defaultRepsText.isNotBlank()) add("reps" to exercise.defaultRepsText)
        if (isEmpty() && exercise.durationSeconds > 0) add("seconds" to exercise.durationSeconds.toString())
    }
    val imageModel = exercise.localGifPath.ifBlank {
        exercise.gifUrl.ifBlank {
            exercise.localThumbnailPath.ifBlank { exercise.thumbnailUrl }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = ExerciseSurface) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Box {
                    AsyncImage(
                        model = imageModel,
                        contentDescription = exercise.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.1f)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.1f)
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Black.copy(alpha = 0.08f),
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.48f)
                                    )
                                )
                            )
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.9f))
                        ) {
                            Icon(
                                Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = stringResource(R.string.common_back),
                                tint = Color.Black
                            )
                        }
                        FilterChip(
                            selected = false,
                            onClick = { onPlayVideo(exercise.id) },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.MenuBook,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            label = {
                                Text(
                                    if (exercise.localVideoPath.isNotBlank()) {
                                        stringResource(R.string.exercise_play_downloaded_video)
                                    } else {
                                        stringResource(R.string.exercise_stream_video)
                                    }
                                )
                            }
                        )
                    }
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 24.dp, top = 92.dp, end = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = exercise.name,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ExerciseHeroChip(exercise.muscleGroup.ifBlank { exercise.target })
                            exercise.difficulty.takeIf { it.isNotBlank() }?.let { ExerciseHeroChip(it) }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ExerciseInfoCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.workout_prescription_title),
                        body = state.prescription?.toDisplaySummary(context)
                            ?: stringResource(
                                R.string.exercise_meta_muscle_difficulty_duration,
                                exercise.muscleGroup.ifBlank { exercise.target },
                                exercise.difficulty.ifBlank { "Standard" },
                                exercise.durationSeconds
                            ),
                        icon = {
                            Icon(
                                Icons.Outlined.FavoriteBorder,
                                contentDescription = null,
                                tint = ExerciseAccent
                            )
                        }
                    )
                    ExerciseInfoCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.exercise_equipment_label, exercise.equipment.ifBlank { "-" }),
                        body = suggestion,
                        icon = {
                            Icon(
                                Icons.Outlined.FitnessCenter,
                                contentDescription = null,
                                tint = ExerciseAccent
                            )
                        }
                    )
                }
            }

            if (targetSections.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            targetSections.forEachIndexed { index, entry ->
                                ExerciseTargetMetric(
                                    modifier = Modifier.weight(1f),
                                    key = entry.first,
                                    value = entry.second
                                )
                                if (index != targetSections.lastIndex) {
                                    Spacer(
                                        modifier = Modifier
                                            .width(1.dp)
                                            .height(54.dp)
                                            .background(Color(0xFFF1E3ED))
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.plan_section_exercise_detail),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (badges.isNotEmpty()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                badges.forEach { badge ->
                                    Text(
                                        text = badge,
                                        color = ExerciseAccent,
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(ExerciseAccentSoft)
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ExerciseMiniStat(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.MyLocation,
                        label = exercise.primaryMuscleGroup.ifBlank { exercise.target.ifBlank { exercise.muscleGroup } },
                        value = exercise.targetMuscles.firstOrNull().orEmpty()
                    )
                    ExerciseMiniStat(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.LocalFireDepartment,
                        label = "${exercise.caloriesBurned}",
                        value = stringResource(R.string.common_kcal)
                    )
                    ExerciseMiniStat(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.Timer,
                        label = "${exercise.durationSeconds}",
                        value = "sec"
                    )
                }
            }

            if (exercise.steps.isNotEmpty()) {
                item {
                    ExerciseListSection(
                        title = stringResource(R.string.category_view_exercise),
                        items = exercise.steps
                    )
                }
            }
            if (exercise.tips.isNotEmpty()) {
                item {
                    ExerciseListSection(
                        title = stringResource(R.string.exercise_section_tips),
                        items = exercise.tips
                    )
                }
            }
            if (exercise.mistakes.isNotEmpty()) {
                item {
                    ExerciseListSection(
                        title = stringResource(R.string.exercise_section_mistakes),
                        items = exercise.mistakes
                    )
                }
            }

            item {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { onPlayVideo(exercise.id) },
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ExerciseAccent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (exercise.localVideoPath.isNotBlank()) {
                                stringResource(R.string.exercise_play_downloaded_video)
                            } else {
                                stringResource(R.string.exercise_stream_video)
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Button(
                        onClick = onDownloadVideo,
                        enabled = exercise.videoUrl.isNotBlank(),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF7EFF6), contentColor = ExerciseAccent),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.exercise_download_workout_offline), fontWeight = FontWeight.SemiBold)
                    }
                    if (exercise.localVideoPath.isNotBlank()) {
                        Button(
                            onClick = onDeleteVideo,
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.exercise_delete_downloaded_video))
                        }
                    }
                    state.statusMessage?.let { status ->
                        Text(
                            text = when (status) {
                                "VIDEO_DOWNLOAD_SUCCESS" -> stringResource(R.string.exercise_video_download_success)
                                "VIDEO_DOWNLOAD_FAILED" -> stringResource(R.string.exercise_video_download_failed)
                                "VIDEO_DELETE_SUCCESS" -> stringResource(R.string.exercise_video_delete_success)
                                else -> status
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

@Composable
private fun ExerciseHeroChip(text: String) {
    if (text.isBlank()) return
    Text(
        text = text,
        color = ExerciseAccent,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    )
}

@Composable
private fun ExerciseInfoCard(
    modifier: Modifier = Modifier,
    title: String,
    body: String,
    icon: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            icon()
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ExerciseTargetMetric(
    modifier: Modifier = Modifier,
    key: String,
    value: String
) {
    val icon = when (key) {
        "reps" -> Icons.Outlined.MyLocation
        "weight" -> Icons.Outlined.Scale
        "sets" -> Icons.Outlined.Timer
        else -> Icons.Outlined.FavoriteBorder
    }
    val suffix = when (key) {
        "reps" -> "reps"
        "weight" -> "kg"
        "sets" -> "set"
        else -> "sec"
    }
    Column(
        modifier = modifier.padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(ExerciseAccentSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = ExerciseAccent)
        }
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = suffix,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ExerciseMiniStat(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = ExerciseAccent)
            Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ExerciseListSection(
    title: String,
    items: List<String>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            items.forEach { line ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .padding(top = 7.dp)
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(ExerciseAccent)
                    )
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
