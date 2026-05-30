package com.example.fitty.feature_workout

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SportsScore
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.text.KeyboardOptions
import coil.ImageLoader
import coil.compose.SubcomposeAsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.example.fitty.R
import com.example.fitty.ui.theme.FittyPink
import java.io.File
import java.util.Locale

private val GreenDone = Color(0xFF2ED573)

private fun formatWeightKg(weightKg: Float?): String? {
    val weight = weightKg ?: return null
    return if (weight % 1f == 0f) {
        "${weight.toInt()} kg"
    } else {
        String.format(Locale.US, "%.1f kg", weight)
    }
}

private fun formatTargetWeight(item: WorkoutExerciseItem): String? {
    return item.targetWeightLabel ?: formatWeightKg(item.targetWeightKg)
}

@Composable
fun WorkoutSessionRoute(
    sessionId: String,
    planId: String = "",
    scheduledWorkoutId: String = "",
    onBack: () -> Unit,
    viewModel: WorkoutSessionViewModel = hiltViewModel()
) {
    LaunchedEffect(sessionId) {
        viewModel.initialize(sessionId, planId, scheduledWorkoutId)
    }
    val state by viewModel.uiState.collectAsState()
    WorkoutSessionScreen(
        state = state,
        onStart = viewModel::startWorkout,
        onStartExerciseTimer = viewModel::startExerciseTimer,
        onPauseExerciseTimer = viewModel::pauseExerciseTimer,
        onCompleteExercise = viewModel::completeExercise,
        onSelectExercise = viewModel::selectExercise,
        onUpdateSetReps = viewModel::updateSetReps,
        onUpdateSetWeight = viewModel::updateSetWeight,
        onFinish = { viewModel.finishWorkout(onBack) },
        onBack = onBack
    )
}

@Composable
private fun WorkoutSessionScreen(
    state: WorkoutSessionUiState,
    onStart: () -> Unit,
    onStartExerciseTimer: () -> Unit,
    onPauseExerciseTimer: () -> Unit,
    onCompleteExercise: () -> Unit,
    onSelectExercise: (Int) -> Unit,
    onUpdateSetReps: (Int, String) -> Unit,
    onUpdateSetWeight: (Int, String) -> Unit,
    onFinish: () -> Unit,
    onBack: () -> Unit
) {
    val totalMin = state.totalElapsedSeconds / 60
    val totalSec = state.totalElapsedSeconds % 60

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 16.dp, vertical = 12.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Header ──
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.common_back))
                }
                Text(
                    state.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.size(48.dp))
            }
        }
        if (state.error != null && state.exerciseItems.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = state.error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                    )
                }
            }
        }

        // ── Loading state ──
        if (state.isLoadingExercises) {
            item {
                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = FittyPink)
                        Spacer(Modifier.height(12.dp))
                        Text(stringResource(R.string.workout_loading_exercises), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        } else if (state.exerciseItems.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Refresh,
                            contentDescription = null,
                            tint = FittyPink,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = state.error ?: stringResource(R.string.workout_no_matching_exercises),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.workout_sync_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            // ── Active Exercise Card ──
            item {
                ActiveExerciseCard(
                    item = state.activeExercise,
                    isWorkoutRunning = state.isRunning,
                    onStartTimer = onStartExerciseTimer,
                    onPauseTimer = onPauseExerciseTimer,
                    onComplete = onCompleteExercise,
                    onUpdateSetReps = onUpdateSetReps,
                    onUpdateSetWeight = onUpdateSetWeight
                )
            }

            // ── Stats Bar ──
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MiniStat(
                        icon = Icons.Outlined.FitnessCenter,
                        value = "${state.completedCount}/${state.totalCount}",
                        label = stringResource(R.string.workout_stat_exercises),
                        modifier = Modifier.weight(1f)
                    )
                    MiniStat(
                        icon = Icons.Outlined.LocalFireDepartment,
                        value = "${state.estimatedCalories}",
                        label = stringResource(R.string.common_kcal),
                        modifier = Modifier.weight(1f)
                    )
                    MiniStat(
                        icon = Icons.Outlined.Timer,
                        value = "%02d:%02d".format(totalMin, totalSec),
                        label = stringResource(R.string.workout_stat_total),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ── Exercise List ──
            // ── Action Buttons ──
            item {
                Spacer(Modifier.height(4.dp))
                when {
                    state.isCompleted -> {
                        Button(
                            onClick = onBack,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GreenDone),
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        ) {
                            Icon(Icons.Outlined.CheckCircle, null, Modifier.size(20.dp))
                            Text(" ${stringResource(R.string.workout_complete_back)}", fontWeight = FontWeight.Bold)
                        }
                    }
                    !state.isRunning -> {
                        Button(
                            onClick = onStart,
                            enabled = !state.isSubmittingSession,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = FittyPink),
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        ) {
                            if (state.isSubmittingSession) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            } else {
                                Icon(Icons.Outlined.PlayArrow, null, Modifier.size(24.dp))
                                Text(" ${stringResource(R.string.workout_start_session)}", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    else -> {
                        Button(
                            onClick = onFinish,
                            enabled = !state.isSubmittingSession,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary
                            ),
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        ) {
                            if (state.isSubmittingSession) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            } else {
                                Icon(Icons.Outlined.SportsScore, null, Modifier.size(20.dp))
                                Text(" ${stringResource(R.string.workout_finish_session)}", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    stringResource(R.string.workout_exercise_list_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            itemsIndexed(state.exerciseItems, key = { i, item -> item.exercise.id + i }) { index, item ->
                ExerciseListItem(
                    item = item,
                    index = index,
                    isSelected = index == state.activeIndex,
                    onClick = { onSelectExercise(index) }
                )
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

// ── Active Exercise Card ─────────────────────────────────────────────

@Composable
private fun ActiveExerciseCard(
    item: WorkoutExerciseItem?,
    isWorkoutRunning: Boolean,
    onStartTimer: () -> Unit,
    onPauseTimer: () -> Unit,
    onComplete: () -> Unit,
    onUpdateSetReps: (Int, String) -> Unit,
    onUpdateSetWeight: (Int, String) -> Unit
) {
    if (item == null) return
    val imageLoader = rememberGifImageLoader()
    val ex = item.exercise
    val elapsedMin = item.elapsedSeconds / 60
    val elapsedSec = item.elapsedSeconds % 60
    val reqMin = item.requiredSeconds / 60
    val reqSec = item.requiredSeconds % 60
    val progress = (item.elapsedSeconds.toFloat() / item.requiredSeconds).coerceAtMost(1f)
    val canComplete = item.elapsedSeconds >= item.requiredSeconds

    Card(
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Exercise name
            Text(
                text = ex.name.ifBlank { ex.id },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            exerciseInstructionText(ex)?.takeIf { it.isNotBlank() }?.let { instructionText ->
                Text(
                    text = instructionText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            WorkoutPrescriptionSummary(item = item)

            // GIF preview
            val gifModel: Any? = when {
                ex.localGifPath.isNotBlank() -> File(ex.localGifPath)
                ex.gifUrl.isNotBlank() -> ex.gifUrl
                ex.localThumbnailPath.isNotBlank() -> File(ex.localThumbnailPath)
                ex.thumbnailUrl.isNotBlank() -> ex.thumbnailUrl
                else -> null
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.4f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFFE8DEF8)),
                contentAlignment = Alignment.Center
            ) {
                when {
                    item.isGifLoading -> {
                        CircularProgressIndicator(color = FittyPink, modifier = Modifier.size(42.dp))
                    }
                    item.isCompleted -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.CheckCircle, null,
                                tint = GreenDone, modifier = Modifier.size(56.dp)
                            )
                            Text(stringResource(R.string.workout_exercise_completed), fontWeight = FontWeight.Bold, color = GreenDone)
                        }
                    }
                    gifModel != null -> {
                        SubcomposeAsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(gifModel).crossfade(true).build(),
                            imageLoader = imageLoader,
                            contentDescription = ex.name,
                            contentScale = ContentScale.Fit,
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
                            Icons.Outlined.FitnessCenter, null,
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }

            // Timer display (visible when timer started)
            if (item.isTimerRunning || item.isCompleted) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = if (canComplete) GreenDone.copy(alpha = 0.08f)
                            else FittyPink.copy(alpha = 0.06f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.workout_timer_format, elapsedMin, elapsedSec, reqMin, reqSec),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (canComplete) GreenDone else FittyPink
                    )
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (canComplete) GreenDone else FittyPink,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Text(
                        text = when {
                            item.isCompleted -> stringResource(R.string.workout_status_completed)
                            canComplete -> stringResource(R.string.workout_status_ready_to_complete)
                            else -> stringResource(R.string.workout_status_in_progress)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (item.plannedSets > 0) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.workout_sets_and_weight_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    repeat(item.plannedSets) { setIndex ->
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.workout_set_label, setIndex + 1),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Column(horizontalAlignment = Alignment.End) {
                                    item.targetRepsLabel?.takeIf { it.isNotBlank() }?.let { reps ->
                                        Text(
                                            text = stringResource(R.string.workout_target_reps_label, reps),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                        formatTargetWeight(item)?.let { weightLabel ->
                                        Text(
                                            text = stringResource(R.string.workout_target_weight_label, weightLabel),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = item.repsBySetInput.getOrElse(setIndex) { "" },
                                    onValueChange = { onUpdateSetReps(setIndex, it) },
                                    label = { Text(stringResource(R.string.workout_reps_field)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    enabled = !item.isCompleted,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = item.weightKgBySetInput.getOrElse(setIndex) { "" },
                                    onValueChange = { onUpdateSetWeight(setIndex, it) },
                                    label = { Text(stringResource(R.string.workout_weight_field)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    enabled = !item.isCompleted,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // Action buttons for active exercise
            if (!item.isCompleted && isWorkoutRunning) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!item.isTimerRunning) {
                        Button(
                            onClick = onStartTimer,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = FittyPink),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Outlined.PlayArrow, null, Modifier.size(20.dp))
                            Text(" ${stringResource(R.string.workout_start_exercise_with_duration, item.requiredSeconds)}", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        OutlinedButton(
                            onClick = onPauseTimer,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                if (item.isPaused) Icons.Outlined.PlayArrow else Icons.Outlined.Pause,
                                null, modifier = Modifier.size(18.dp)
                            )
                            Text(" ${if (item.isPaused) stringResource(R.string.workout_resume) else stringResource(R.string.workout_pause)}", fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick = onComplete,
                            enabled = canComplete,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GreenDone,
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Outlined.CheckCircle, null, Modifier.size(18.dp))
                            Text(" ${stringResource(R.string.common_done)}", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ── Exercise List Item ───────────────────────────────────────────────

@Composable
private fun ExerciseListItem(
    item: WorkoutExerciseItem,
    index: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val imageLoader = rememberGifImageLoader()
    val ex = item.exercise
    val imageModel: Any? = when {
        ex.localThumbnailPath.isNotBlank() -> File(ex.localThumbnailPath)
        ex.thumbnailUrl.isNotBlank() -> ex.thumbnailUrl
        ex.localGifPath.isNotBlank() -> File(ex.localGifPath)
        ex.gifUrl.isNotBlank() -> ex.gifUrl
        else -> null
    }

    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 8.dp else 2.dp),
        modifier = Modifier.fillMaxWidth().clickable(enabled = !item.isCompleted, onClick = onClick)
    ) {
        val rowBrush = when {
            item.isCompleted -> Brush.linearGradient(
                listOf(GreenDone.copy(alpha = 0.16f), MaterialTheme.colorScheme.surface)
            )
            isSelected -> Brush.linearGradient(
                listOf(FittyPink.copy(alpha = 0.22f), FittyPink.copy(alpha = 0.07f), MaterialTheme.colorScheme.surface)
            )
            else -> Brush.linearGradient(
                listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surface)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(rowBrush)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Thumbnail
            Box(
                modifier = Modifier.size(64.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFFF7ECF5), Color(0xFFE8DEF8))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (imageModel != null) {
                    coil.compose.AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageModel).crossfade(true).build(),
                        imageLoader = imageLoader,
                        contentDescription = ex.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        Icons.Outlined.FitnessCenter, null,
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Info
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    text = ex.name.ifBlank { ex.id },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                WorkoutTargetChipRow(item = item)
                exerciseInstructionText(ex)?.takeIf { it.isNotBlank() }?.let { instructionText ->
                    Text(
                        text = instructionText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Status
            when {
                item.isCompleted -> {
                    Icon(
                        Icons.Outlined.CheckCircle, stringResource(R.string.common_done),
                        tint = GreenDone, modifier = Modifier.size(24.dp)
                    )
                }
                isSelected -> {
                    Box(
                        modifier = Modifier.size(40.dp)
                            .background(
                                Brush.linearGradient(listOf(FittyPink, Color(0xFFFF6CB4))),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.PlayArrow, stringResource(R.string.common_active),
                            tint = Color.White, modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkoutPrescriptionSummary(item: WorkoutExerciseItem) {
    val ex = item.exercise
    val summary = when {
        item.plannedSets > 0 -> {
            formatTargetWeight(item)?.let { weightLabel ->
                stringResource(
                    R.string.workout_sets_summary_with_weight,
                    item.plannedSets,
                    item.targetRepsLabel ?: stringResource(R.string.workout_reps_not_set),
                    weightLabel
                )
            } ?: stringResource(
                R.string.workout_sets_summary,
                item.plannedSets,
                item.targetRepsLabel ?: stringResource(R.string.workout_reps_not_set)
            )
        }
        else -> stringResource(R.string.exercise_prescription_duration, item.requiredSeconds)
    }
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
            text = stringResource(R.string.workout_prescription_title),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = summary,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        WorkoutTargetChipRow(item = item)
        if (item.plannedSets == 0 && ex.defaultRepsText.isNotBlank()) {
            Text(
                text = stringResource(R.string.workout_target_reps_label, ex.defaultRepsText),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WorkoutTargetChipRow(item: WorkoutExerciseItem) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (item.plannedSets > 0) {
            WorkoutTargetChip(stringResource(R.string.workout_details_sets_only, item.plannedSets))
        }
        item.targetRepsLabel?.takeIf { it.isNotBlank() }?.let { reps ->
            WorkoutTargetChip(reps)
        }
        if (item.plannedSets == 0) {
            WorkoutTargetChip(stringResource(R.string.exercise_prescription_duration, item.requiredSeconds))
        }
        formatTargetWeight(item)?.let { weight ->
            WorkoutTargetChip(weight)
        }
    }
}

@Composable
private fun WorkoutTargetChip(text: String) {
    Text(
        text = text,
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

private fun exerciseInstructionText(exercise: com.example.fitty.domain.model.Exercise): String {
    return exercise.instructions.ifBlank { exercise.description }
        .ifBlank { exercise.steps.firstOrNull().orEmpty() }
}

// ── Mini Stat Card ───────────────────────────────────────────────────

@Composable
private fun MiniStat(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, null, tint = FittyPink, modifier = Modifier.size(18.dp))
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ── GIF ImageLoader ──────────────────────────────────────────────────

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
