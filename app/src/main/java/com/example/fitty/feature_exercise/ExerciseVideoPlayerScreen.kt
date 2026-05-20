package com.example.fitty.feature_exercise

import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.fitty.domain.model.Exercise
import com.example.fitty.domain.usecase.exercise.GetExerciseUseCase
import com.example.fitty.domain.usecase.exercise.RecordRecentlyViewedExerciseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ExerciseVideoPlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getExerciseUseCase: GetExerciseUseCase,
    private val recordRecentlyViewedExerciseUseCase: RecordRecentlyViewedExerciseUseCase
) : ViewModel() {
    private val exerciseId: String = checkNotNull(savedStateHandle["exerciseId"])
    private val _exercise = MutableStateFlow<Exercise?>(null)
    val exercise: StateFlow<Exercise?> = _exercise

    init {
        viewModelScope.launch {
            _exercise.value = getExerciseUseCase(exerciseId)
            recordRecentlyViewedExerciseUseCase(exerciseId)
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun ExerciseVideoPlayerRoute(
    viewModel: ExerciseVideoPlayerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val exercise by viewModel.exercise.collectAsState()
    val currentExercise = exercise ?: return
    val player = remember(currentExercise.id) {
        ExoPlayer.Builder(context)
            .setLoadControl(
                DefaultLoadControl.Builder()
                    .setBufferDurationsMs(
                        3_000,
                        15_000,
                        1_500,
                        2_000
                    )
                    .build()
            )
            .build().apply {
                val mediaSource = currentExercise.localVideoPath.ifBlank { currentExercise.videoUrl }
                setMediaItem(MediaItem.fromUri(mediaSource))
                prepare()
                playWhenReady = true
            }
    }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    ExerciseVideoPlayerScreen(player = player)
}

@Composable
fun ExerciseVideoPlayerScreen(player: ExoPlayer) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            PlayerView(context).apply {
                layoutParams = android.view.ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                useController = true
                this.player = player
            }
        },
        update = { it.player = player }
    )
}
