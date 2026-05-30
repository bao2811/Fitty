package com.example.fitty.feature_exercise

import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import com.example.fitty.R
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
    onBack: () -> Unit,
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

    ExerciseVideoPlayerScreen(player = player, onBack = onBack)
}

@Composable
fun ExerciseVideoPlayerScreen(
    player: ExoPlayer,
    onBack: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize()) {
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = stringResource(R.string.common_back)
                    )
                }
                Text(
                    text = stringResource(R.string.exercise_stream_video),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}
