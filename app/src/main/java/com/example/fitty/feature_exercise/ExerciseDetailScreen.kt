package com.example.fitty.feature_exercise

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.example.fitty.R
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

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
    private val exercisePrescriptionResolver: ExercisePrescriptionResolver
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
            val language = sessionRepository.getAppLanguage().orEmpty().ifBlank { Locale.getDefault().language }
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
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = stringResource(R.string.common_back)
                    )
                }
                Text(
                    text = stringResource(R.string.plan_section_exercise_detail),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            AsyncImage(
                model = exercise.localThumbnailPath.ifBlank { exercise.thumbnailUrl },
                contentDescription = exercise.name,
                modifier = Modifier.fillMaxWidth()
            )
            Text(exercise.name, style = MaterialTheme.typography.headlineSmall)
            Text(exercise.description.ifBlank { exercise.instructions })
            Text(
                stringResource(
                    R.string.exercise_meta_muscle_difficulty_duration,
                    exercise.muscleGroup,
                    exercise.difficulty,
                    exercise.durationSeconds
                )
            )
            state.prescription?.let { prescription ->
                Text(
                    text = stringResource(R.string.exercise_prescription_label, prescription.toDisplaySummary(context)),
                    style = MaterialTheme.typography.bodyMedium
                )
                prescription.note.takeIf { it.isNotBlank() }?.let { note ->
                    Text(
                        text = note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    prescription.toDisplayBadges(context).forEach { badge ->
                        Text(
                            text = badge,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            Text(stringResource(R.string.exercise_equipment_label, exercise.equipment))
            Button(onClick = { onPlayVideo(exercise.id) }) {
                Text(
                    if (exercise.localVideoPath.isNotBlank()) {
                        stringResource(R.string.exercise_play_downloaded_video)
                    } else {
                        stringResource(R.string.exercise_stream_video)
                    }
                )
            }
            Button(onClick = onDownloadVideo, enabled = exercise.videoUrl.isNotBlank()) {
                Text(stringResource(R.string.exercise_download_workout_offline))
            }
            if (exercise.localVideoPath.isNotBlank()) {
                Button(onClick = onDeleteVideo) {
                    Text(stringResource(R.string.exercise_delete_downloaded_video))
                }
            }
            state.statusMessage?.let { status ->
                Text(
                    when (status) {
                        "VIDEO_DOWNLOAD_SUCCESS" -> stringResource(R.string.exercise_video_download_success)
                        "VIDEO_DOWNLOAD_FAILED" -> stringResource(R.string.exercise_video_download_failed)
                        "VIDEO_DELETE_SUCCESS" -> stringResource(R.string.exercise_video_delete_success)
                        else -> status
                    }
                )
            }
        }
    }
}
