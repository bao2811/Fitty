package com.example.fitty.feature_exercise

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.example.fitty.data.exercise.ExerciseVideoDownloadManager
import com.example.fitty.domain.model.Exercise
import com.example.fitty.domain.usecase.exercise.GetExerciseUseCase
import com.example.fitty.domain.usecase.exercise.RecordRecentlyViewedExerciseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ExerciseDetailUiState(
    val exercise: Exercise? = null,
    val statusMessage: String? = null
)

@HiltViewModel
class ExerciseDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getExerciseUseCase: GetExerciseUseCase,
    private val recordRecentlyViewedExerciseUseCase: RecordRecentlyViewedExerciseUseCase,
    private val videoDownloadManager: ExerciseVideoDownloadManager
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
            _uiState.value = ExerciseDetailUiState(exercise = exercise)
        }
    }

    fun downloadVideo() {
        val exercise = _uiState.value.exercise ?: return
        viewModelScope.launch {
            val result = videoDownloadManager.download(exercise)
            _uiState.value = _uiState.value.copy(
                exercise = getExerciseUseCase(exercise.id),
                statusMessage = result.fold(
                    onSuccess = { "Video downloaded for offline playback." },
                    onFailure = { it.message ?: "Video download failed." }
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
                statusMessage = "Offline video removed."
            )
        }
    }
}

@Composable
fun ExerciseDetailRoute(
    onPlayVideo: (String) -> Unit,
    viewModel: ExerciseDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    ExerciseDetailScreen(
        state = state,
        onPlayVideo = onPlayVideo,
        onDownloadVideo = viewModel::downloadVideo,
        onDeleteVideo = viewModel::deleteDownloadedVideo
    )
}

@Composable
fun ExerciseDetailScreen(
    state: ExerciseDetailUiState,
    onPlayVideo: (String) -> Unit,
    onDownloadVideo: () -> Unit,
    onDeleteVideo: () -> Unit
) {
    val exercise = state.exercise ?: return
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AsyncImage(
            model = exercise.localThumbnailPath.ifBlank { exercise.thumbnailUrl },
            contentDescription = exercise.name,
            modifier = Modifier.fillMaxWidth()
        )
        Text(exercise.name, style = MaterialTheme.typography.headlineSmall)
        Text(exercise.description.ifBlank { exercise.instructions })
        Text("${exercise.muscleGroup} • ${exercise.difficulty} • ${exercise.durationSeconds}s")
        Text("Equipment: ${exercise.equipment}")
        Button(onClick = { onPlayVideo(exercise.id) }) {
            Text(if (exercise.localVideoPath.isNotBlank()) "Play Downloaded Video" else "Stream Video")
        }
        Button(onClick = onDownloadVideo, enabled = exercise.videoUrl.isNotBlank()) {
            Text("Download Workout for Offline")
        }
        if (exercise.localVideoPath.isNotBlank()) {
            Button(onClick = onDeleteVideo) {
                Text("Delete Downloaded Video")
            }
        }
        state.statusMessage?.let { Text(it) }
    }
}
