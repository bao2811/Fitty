package com.example.fitty.feature_workout

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.FitnessCenter
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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

private fun formatTargetWeightBasisShort(item: WorkoutExerciseItem): String {
    return item.targetWeightBasisLabel
        ?.replace("Theo cân nặng ", "theo ")
        ?.replace("Based on ", "based on ")
        ?.replace(" body weight", "")
        .orEmpty()
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
        onSkipRest = viewModel::skipRest,
        onOpenReplaceExercise = viewModel::openReplaceExercise,
        onDismissReplaceExercise = viewModel::dismissReplaceExercise,
        onReplaceExercise = viewModel::replaceExercise,
        onOpenEditExercise = viewModel::openEditExercise,
        onDismissEditExercise = viewModel::dismissEditExercise,
        onSaveExerciseDetails = viewModel::saveExerciseDetails,
        onSelectExercise = viewModel::selectExercise,
        onFinish = viewModel::finishWorkout,
        onDismissAchievementPopup = viewModel::dismissAchievementPopup,
        onCloseCompletionSummary = {
            viewModel.dismissCompletionSummary()
            onBack()
        },
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
    onSkipRest: () -> Unit,
    onOpenReplaceExercise: (Int) -> Unit,
    onDismissReplaceExercise: () -> Unit,
    onReplaceExercise: (String) -> Unit,
    onOpenEditExercise: (Int) -> Unit,
    onDismissEditExercise: () -> Unit,
    onSaveExerciseDetails: (Int, String, String, String, String) -> Unit,
    onSelectExercise: (Int) -> Unit,
    onFinish: () -> Unit,
    onDismissAchievementPopup: () -> Unit,
    onCloseCompletionSummary: () -> Unit,
    onBack: () -> Unit
) {
    val totalMin = state.totalElapsedSeconds / 60
    val totalSec = state.totalElapsedSeconds % 60
    val listState = rememberLazyListState()
    var headerVisible by remember { mutableStateOf(true) }

    LaunchedEffect(listState) {
        var lastIndex = listState.firstVisibleItemIndex
        var lastOffset = listState.firstVisibleItemScrollOffset
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                val scrollingDown = index > lastIndex || (index == lastIndex && offset > lastOffset)
                val scrollingUp = index < lastIndex || (index == lastIndex && offset < lastOffset)
                if (scrollingDown) headerVisible = false
                if (scrollingUp) headerVisible = true
                lastIndex = index
                lastOffset = offset
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFFBFE))
    ) {
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    headerVisible = true
                }
            },
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp,
            top = 85.dp,
            end = 16.dp,
            bottom = if (state.exerciseItems.isNotEmpty() && !state.isLoadingExercises) 112.dp else 12.dp
        ),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // ── Header ──
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
                    isResting = state.isResting,
                    restElapsedSeconds = state.restElapsedSeconds,
                    restDurationSeconds = state.restDurationSeconds,
                    onStartTimer = onStartExerciseTimer,
                    onPauseTimer = onPauseExerciseTimer,
                    onComplete = onCompleteExercise,
                    onSkipRest = onSkipRest
                )
            }

            // ── Stats Bar ──
            

            // ── Exercise List ──
            // ── Action Buttons ──
            

            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        stringResource(R.string.workout_exercise_list_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
            itemsIndexed(state.exerciseItems, key = { i, item -> item.exercise.id + i }) { index, item ->
                ExerciseListItem(
                    item = item,
                    index = index,
                    isSelected = index == state.activeIndex,
                    canReplace = !state.isRunning && !state.isResting && !state.isSubmittingSession,
                    canEdit = !state.isRunning && !state.isResting && !state.isSubmittingSession,
                    onReplace = { onOpenReplaceExercise(index) },
                    onEdit = { onOpenEditExercise(index) },
                    onClick = { if (!state.isResting) onSelectExercise(index) }
                )
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
        }
        AnimatedVisibility(
            visible = headerVisible,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(start = 16.dp, top = 24.dp, end = 16.dp)
        ) {
            QuickWorkoutFloatingHeader(
                title = state.title,
                subtitle = if (state.isRunning) {
                    if (state.isResting) {
                        stringResource(
                            R.string.workout_rest_header_format,
                            state.restElapsedSeconds,
                            state.restDurationSeconds
                        )
                    } else {
                        "%02d:%02d".format(totalMin, totalSec)
                    }
                } else {
                    stringResource(R.string.plan_quick_workout_body)
                },
                onBack = onBack
            )
        }
        AnimatedVisibility(
            visible = state.exerciseItems.isNotEmpty() && !state.isLoadingExercises,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
        ) {
            WorkoutPrimaryAction(
                state = state,
                onBack = onBack,
                onStart = onStart,
                onFinish = onFinish
            )
        }
        if (state.replacementTargetIndex != null) {
            ReplaceExerciseDialog(
                muscleGroupLabel = state.replacementTargetLabel,
                options = state.replacementOptions,
                onDismiss = onDismissReplaceExercise,
                onSelect = onReplaceExercise
            )
        }
        state.editTargetIndex?.let { targetIndex ->
            state.exerciseItems.getOrNull(targetIndex)?.let { targetItem ->
                EditExerciseDialog(
                    item = targetItem,
                    onDismiss = onDismissEditExercise,
                    onSave = { sets, reps, weight, duration ->
                        onSaveExerciseDetails(targetIndex, sets, reps, weight, duration)
                    }
                )
            }
        }
        state.achievementPopup?.let { unlock ->
            Dialog(onDismissRequest = onDismissAchievementPopup) {
                AchievementUnlockPopup(
                    achievement = unlock,
                    onDismiss = onDismissAchievementPopup
                )
            }
        } ?: state.completionSummary?.let { summary ->
            PostWorkoutSummaryDialog(
                summary = summary,
                onDone = onCloseCompletionSummary
            )
        }
    }
}

// ── Active Exercise Card ─────────────────────────────────────────────
@Composable
private fun QuickWorkoutFloatingHeader(
    title: String,
    subtitle: String,
    onBack: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.96f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFF1F8))
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.common_back),
                    tint = Color.Black
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFF1F8)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.FitnessCenter,
                    contentDescription = null,
                    tint = FittyPink,
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}

@Composable
private fun WorkoutPrimaryAction(
    state: WorkoutSessionUiState,
    onBack: () -> Unit,
    onStart: () -> Unit,
    onFinish: () -> Unit
) {
    val totalMin = state.totalElapsedSeconds / 60
    val totalSec = state.totalElapsedSeconds % 60

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.97f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "${state.completedCount}/${state.totalCount} • ${state.estimatedCalories} kcal • %02d:%02d".format(totalMin, totalSec),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            when {
                state.isCompleted -> {
                    Button(
                        onClick = onBack,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenDone),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(Icons.Outlined.CheckCircle, null, Modifier.size(18.dp))
                        Text(" ${stringResource(R.string.workout_complete_back)}", fontWeight = FontWeight.Bold)
                    }
                }
                !state.isRunning -> {
                    Button(
                        onClick = onStart,
                        enabled = !state.isSubmittingSession,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FittyPink),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        if (state.isSubmittingSession) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Icon(Icons.Outlined.PlayArrow, null, Modifier.size(20.dp))
                            Text(" ${stringResource(R.string.workout_start_session)}", fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
                else -> {
                    Button(
                        onClick = onFinish,
                        enabled = !state.isSubmittingSession,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        if (state.isSubmittingSession) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Icon(Icons.Outlined.SportsScore, null, Modifier.size(18.dp))
                            Text(" ${stringResource(R.string.workout_finish_session)}", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AchievementUnlockPopup(
    achievement: WorkoutAchievementUnlockUi,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.EmojiEvents,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(23.dp)
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.workout_achievement_unlocked_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.75f),
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = achievement.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = achievement.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = stringResource(R.string.common_close),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.tertiary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.75f))
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun PostWorkoutSummaryDialog(
    summary: WorkoutCompletionSummaryUi,
    onDone: () -> Unit
) {
    Dialog(onDismissRequest = onDone) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(GreenDone.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = GreenDone, modifier = Modifier.size(26.dp))
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = stringResource(R.string.workout_summary_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = summary.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.workout_summary_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    SummaryMetricTile(
                        label = stringResource(R.string.workout_summary_duration),
                        value = stringResource(R.string.workout_summary_duration_value, summary.durationMinutes),
                        icon = Icons.Outlined.PlayArrow,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryMetricTile(
                        label = stringResource(R.string.workout_summary_calories),
                        value = stringResource(R.string.workout_summary_calories_value, summary.caloriesBurned),
                        icon = Icons.Outlined.LocalFireDepartment,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    SummaryMetricTile(
                        label = stringResource(R.string.workout_summary_completion),
                        value = stringResource(R.string.workout_summary_percent_value, summary.completionPercent),
                        icon = Icons.Outlined.SportsScore,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryMetricTile(
                        label = stringResource(R.string.workout_summary_exercises),
                        value = stringResource(
                            R.string.workout_summary_exercises_value,
                            summary.completedExercises,
                            summary.totalExercises
                        ),
                        icon = Icons.Outlined.FitnessCenter,
                        modifier = Modifier.weight(1f)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(R.string.workout_summary_completion),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.workout_summary_percent_value, summary.completionPercent),
                            style = MaterialTheme.typography.labelMedium,
                            color = FittyPink,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    LinearProgressIndicator(
                        progress = { summary.completionPercent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = FittyPink,
                        trackColor = FittyPink.copy(alpha = 0.12f)
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

                SummaryAchievementRow(summary.achievementUnlock)

                Button(
                    onClick = onDone,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FittyPink),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(stringResource(R.string.common_done), fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun SummaryMetricTile(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFFFF5F9))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = FittyPink, modifier = Modifier.size(20.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, maxLines = 1)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

@Composable
private fun SummaryAchievementRow(achievement: WorkoutAchievementUnlockUi?) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (achievement != null) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (achievement != null) Icons.Outlined.EmojiEvents else Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = if (achievement != null) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = achievement?.title ?: stringResource(R.string.workout_achievement_no_new_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = achievement?.description ?: stringResource(R.string.workout_achievement_no_new_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ActiveExerciseCard(
    item: WorkoutExerciseItem?,
    isWorkoutRunning: Boolean,
    isResting: Boolean,
    restElapsedSeconds: Int,
    restDurationSeconds: Int,
    onStartTimer: () -> Unit,
    onPauseTimer: () -> Unit,
    onComplete: () -> Unit,
    onSkipRest: () -> Unit
) {
    if (item == null) return
    val imageLoader = rememberGifImageLoader()
    val ex = item.exercise
    val canComplete = item.elapsedSeconds >= item.requiredSeconds

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        QuickWorkoutHero(item = item, imageLoader = imageLoader)
        QuickMetricsBox(item = item)
        if (isResting) {
            RestTimerCard(
                nextExerciseName = ex.name.ifBlank { ex.id },
                elapsedSeconds = restElapsedSeconds,
                durationSeconds = restDurationSeconds,
                onSkipRest = onSkipRest
            )
        } else {
            QuickTimerCard(item = item, canComplete = canComplete)
        }
        if (!isResting && !item.isCompleted && isWorkoutRunning) {
            QuickWorkoutControls(
                item = item,
                canComplete = canComplete,
                onStartTimer = onStartTimer,
                onPauseTimer = onPauseTimer,
                onComplete = onComplete
            )
        }
    }
}

@Composable
private fun RestTimerCard(
    nextExerciseName: String,
    elapsedSeconds: Int,
    durationSeconds: Int,
    onSkipRest: () -> Unit
) {
    val progress = (elapsedSeconds.toFloat() / durationSeconds).coerceIn(0f, 1f)

    QuickInfoCard {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.workout_rest_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = FittyPink
            )
            Text(
                text = stringResource(R.string.workout_rest_next_up, nextExerciseName),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = stringResource(R.string.workout_rest_format, elapsedSeconds, durationSeconds),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold
        )
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(999.dp)),
            color = FittyPink,
            trackColor = FittyPink.copy(alpha = 0.14f)
        )
        Button(
            onClick = onSkipRest,
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFFF1F8),
                contentColor = FittyPink
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(
                text = stringResource(R.string.workout_rest_skip),
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun QuickMetricsBox(item: WorkoutExerciseItem) {
    val ex = item.exercise
    val primary = ex.primaryMuscleGroup.ifBlank { ex.target.ifBlank { ex.bodyPart } }
    val secondary = ex.targetMuscles
        .filterNot { it.equals(primary, ignoreCase = true) }
        .distinct()
        .take(2)
    val level = when (ex.difficulty.lowercase(Locale.US)) {
        "advanced", "hard", "expert" -> 2
        "beginner", "easy" -> 0
        else -> 1
    }
    val suggestionText = listOfNotNull(
        item.targetRepsLabel ?: "${item.requiredSeconds}s",
        formatTargetWeight(item)
    ).joinToString(" • ")

    val weightBasisShort = formatTargetWeightBasisShort(item)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(stringResource(R.string.workout_intensity_title), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold)
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
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        IntensityLabel(stringResource(R.string.workout_intensity_light), Color(0xFF43A047), level == 0)
                        IntensityLabel(stringResource(R.string.workout_intensity_medium_short), Color(0xFFFF8A00), level == 1)
                        IntensityLabel(stringResource(R.string.workout_intensity_heavy), FittyPink, level == 2)
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
                        Text(stringResource(R.string.workout_suggestion_title), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = stringResource(R.string.workout_suggestion_with_target, item.targetRepsLabel ?: "${item.requiredSeconds}s"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            QuickMetricDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                QuickTargetMetric(Icons.Outlined.Refresh, item.targetRepsLabel ?: stringResource(R.string.exercise_prescription_duration, item.requiredSeconds), if (item.plannedSets > 0) stringResource(R.string.workout_unit_reps) else stringResource(R.string.workout_unit_seconds))
                QuickTargetMetric(Icons.Outlined.FitnessCenter, formatTargetWeight(item) ?: "-", weightBasisShort)
                QuickTargetMetric(Icons.Outlined.SportsScore, if (item.plannedSets > 0) item.plannedSets.toString() else "1", stringResource(R.string.workout_unit_set))
            }

            QuickMetricDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                    Text(stringResource(R.string.workout_target_muscle_groups), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold)
                    MuscleLine(primary.ifBlank { stringResource(R.string.workout_primary_muscle_fallback) }, ex.target.ifBlank { ex.bodyPart }, stringResource(R.string.workout_primary_badge), primary = true)
                    secondary.forEach { muscle -> MuscleLine(muscle, ex.bodyPart, stringResource(R.string.workout_secondary_badge), primary = false) }
                }
            }
        }
    }
}

@Composable
private fun QuickMetricDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(0xFFF1E3ED))
    )
}

@Composable
private fun QuickWorkoutHero(
    item: WorkoutExerciseItem,
    imageLoader: ImageLoader
) {
    val ex = item.exercise
    val context = LocalContext.current
    val gifModel: Any? = when {
        ex.localGifPath.isNotBlank() -> File(ex.localGifPath)
        ex.gifUrl.isNotBlank() -> ex.gifUrl
        ex.localThumbnailPath.isNotBlank() -> File(ex.localThumbnailPath)
        ex.thumbnailUrl.isNotBlank() -> ex.thumbnailUrl
        else -> null
    }
    val muscleLabel = ex.primaryMuscleGroup.ifBlank { ex.target.ifBlank { ex.bodyPart } }
    val elapsedMin = item.elapsedSeconds / 60
    val elapsedSec = item.elapsedSeconds % 60
    val reqMin = item.requiredSeconds / 60
    val reqSec = item.requiredSeconds % 60
    val progress = (item.elapsedSeconds.toFloat() / item.requiredSeconds).coerceAtMost(1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(252.dp)
            .clip(RoundedCornerShape(0.dp))
            .background(Color(0xFF24242A)),
        contentAlignment = Alignment.Center
    ) {
        when {
            item.isGifLoading -> CircularProgressIndicator(color = Color.White, modifier = Modifier.size(42.dp))
            gifModel != null -> {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(gifModel)
                        .crossfade(true)
                        .build(),
                    imageLoader = imageLoader,
                    contentDescription = ex.name,
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
                    Icons.Outlined.FitnessCenter,
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

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = ex.name.ifBlank { ex.id },
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (muscleLabel.isNotBlank()) {
                WorkoutTargetChip(muscleLabel)
            }
        }

        exerciseInstructionText(ex).takeIf { it.isNotBlank() }?.let {
            Text(
                text = "Hướng dẫn",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(14.dp)
                    .background(Color.Black.copy(alpha = 0.34f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                if (item.isTimerRunning && !item.isPaused) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = "%02d:%02d".format(elapsedMin, elapsedSec),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .weight(1f)
                    .height(5.dp)
                    .clip(RoundedCornerShape(999.dp)),
                color = FittyPink,
                trackColor = Color.White.copy(alpha = 0.34f)
            )
            Text(
                text = "%02d:%02d".format(reqMin, reqSec),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun QuickTimerCard(
    item: WorkoutExerciseItem,
    canComplete: Boolean
) {
    if (!item.isTimerRunning && !item.isCompleted && item.elapsedSeconds == 0) return
    val elapsedMin = item.elapsedSeconds / 60
    val elapsedSec = item.elapsedSeconds % 60
    val reqMin = item.requiredSeconds / 60
    val reqSec = item.requiredSeconds % 60
    val progress = (item.elapsedSeconds.toFloat() / item.requiredSeconds).coerceAtMost(1f)

    QuickInfoCard {
        Text(
            text = stringResource(R.string.workout_timer_format, elapsedMin, elapsedSec, reqMin, reqSec),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = if (canComplete) GreenDone else FittyPink
        )
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(999.dp)),
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

@Composable
private fun QuickWorkoutControls(
    item: WorkoutExerciseItem,
    canComplete: Boolean,
    onStartTimer: () -> Unit,
    onPauseTimer: () -> Unit,
    onComplete: () -> Unit
) {
    if (!item.isTimerRunning) {
        Button(
            onClick = onStartTimer,
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FittyPink),
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
        ) {
            Icon(Icons.Outlined.PlayArrow, null, Modifier.size(22.dp))
            Text(
                text = " ${stringResource(R.string.workout_start_exercise_with_duration, item.requiredSeconds)}",
                fontWeight = FontWeight.ExtraBold
            )
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onPauseTimer,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
            ) {
                Icon(
                    if (item.isPaused) Icons.Outlined.PlayArrow else Icons.Outlined.Pause,
                    null,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = " ${if (item.isPaused) stringResource(R.string.workout_resume) else stringResource(R.string.workout_pause)}",
                    fontWeight = FontWeight.Bold
                )
            }
            Button(
                onClick = onComplete,
                enabled = canComplete,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GreenDone,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
            ) {
                Icon(Icons.Outlined.CheckCircle, null, Modifier.size(18.dp))
                Text(" ${stringResource(R.string.common_done)}", fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun QuickInfoCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
private fun IntensityLabel(
    text: String,
    color: Color,
    selected: Boolean
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = color.copy(alpha = if (selected) 1f else 0.82f),
        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold
    )
}

@Composable
private fun QuickTargetMetric(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(FittyPink.copy(alpha = 0.10f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = FittyPink, modifier = Modifier.size(14.dp))
        }
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (label.isNotBlank()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun MuscleLine(
    title: String,
    subtitle: String,
    badge: String,
    primary: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(if (primary) 9.dp else 7.dp)
                    .background(if (primary) FittyPink else MaterialTheme.colorScheme.outline, CircleShape)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = badge,
                style = MaterialTheme.typography.labelSmall,
                color = if (primary) FittyPink else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(
                        if (primary) FittyPink.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(999.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
        if (subtitle.isNotBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ── Exercise List Item ───────────────────────────────────────────────

@Composable
private fun ExerciseListItem(
    item: WorkoutExerciseItem,
    index: Int,
    isSelected: Boolean,
    canReplace: Boolean,
    canEdit: Boolean,
    onReplace: () -> Unit,
    onEdit: () -> Unit,
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
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 5.dp else 2.dp),
        modifier = Modifier.fillMaxWidth().clickable(enabled = !item.isCompleted, onClick = onClick)
    ) {
        val rowBrush = when {
            item.isCompleted -> Brush.linearGradient(
                listOf(GreenDone.copy(alpha = 0.12f), Color(0xFFFFFFFF))
            )
            isSelected -> Brush.linearGradient(
                listOf(Color(0xFFFFD9EC), Color(0xFFFFF0F8))
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
                canReplace -> {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        if (canEdit) {
                            IconButton(
                                onClick = onEdit,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.86f))
                            ) {
                                Icon(
                                    Icons.Outlined.Edit,
                                    contentDescription = stringResource(R.string.workout_edit_action),
                                    tint = FittyPink,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                        IconButton(
                            onClick = onReplace,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.86f))
                        ) {
                            Icon(
                                Icons.Outlined.Refresh,
                                contentDescription = stringResource(R.string.workout_replace_action),
                                tint = FittyPink,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
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
private fun EditExerciseDialog(
    item: WorkoutExerciseItem,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var sets by rememberSaveable(item.exercise.id, item.plannedSets) { mutableStateOf(item.plannedSets.toString()) }
    var reps by rememberSaveable(item.exercise.id, item.targetRepsLabel) { mutableStateOf(item.targetRepsLabel.orEmpty()) }
    var weight by rememberSaveable(item.exercise.id, item.targetWeightKg) { mutableStateOf(item.targetWeightKg?.toString().orEmpty()) }
    var duration by rememberSaveable(item.exercise.id, item.requiredSeconds) { mutableStateOf(item.requiredSeconds.toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.workout_edit_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = item.exercise.name.ifBlank { item.exercise.id },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedTextField(
                    value = sets,
                    onValueChange = { sets = it.filter(Char::isDigit) },
                    label = { Text(stringResource(R.string.workout_edit_sets_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = reps,
                    onValueChange = { reps = it },
                    label = { Text(stringResource(R.string.workout_edit_reps_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = weight,
                    onValueChange = { input ->
                        weight = input.filterIndexed { index, char ->
                            char.isDigit() || (char == '.' && input.take(index).none { previousChar -> previousChar == '.' })
                        }
                    },
                    label = { Text(stringResource(R.string.workout_edit_weight_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = duration,
                    onValueChange = { duration = it.filter(Char::isDigit) },
                    label = { Text(stringResource(R.string.workout_edit_duration_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                    ) {
                        Text(text = stringResource(R.string.common_cancel), fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { onSave(sets, reps, weight, duration) },
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FittyPink),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                    ) {
                        Text(text = stringResource(R.string.common_save), fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReplaceExerciseDialog(
    muscleGroupLabel: String,
    options: List<com.example.fitty.domain.model.Exercise>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.workout_replace_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = stringResource(R.string.workout_replace_subtitle, muscleGroupLabel),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (options.isEmpty()) {
                    Text(
                        text = stringResource(R.string.workout_replace_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.height(360.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(options, key = { _, item -> item.id }) { index, option ->
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                ReplaceExerciseOption(
                                    exercise = option,
                                    onSelect = { onSelect(option.id) }
                                )
                                if (index != options.lastIndex) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(
                        text = stringResource(R.string.common_cancel),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ReplaceExerciseOption(
    exercise: com.example.fitty.domain.model.Exercise,
    onSelect: () -> Unit
) {
    val imageLoader = rememberGifImageLoader()
    val imageModel: Any? = when {
        exercise.localThumbnailPath.isNotBlank() -> File(exercise.localThumbnailPath)
        exercise.thumbnailUrl.isNotBlank() -> exercise.thumbnailUrl
        exercise.localGifPath.isNotBlank() -> File(exercise.localGifPath)
        exercise.gifUrl.isNotBlank() -> exercise.gifUrl
        else -> null
    }
    val primary = exercise.primaryMuscleGroup.ifBlank { exercise.target.ifBlank { exercise.bodyPart } }
    val secondary = exercise.targetMuscles
        .filterNot { it.equals(primary, ignoreCase = true) }
        .distinct()
        .take(2)

    OutlinedButton(
        onClick = onSelect,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFFFFEFF7)),
                contentAlignment = Alignment.Center
            ) {
                if (imageModel != null) {
                    coil.compose.AsyncImage(
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
                        Icons.Outlined.FitnessCenter,
                        contentDescription = null,
                        tint = FittyPink.copy(alpha = 0.55f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = exercise.name.ifBlank { exercise.id },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    WorkoutTargetChip(primary.ifBlank { exercise.bodyPart })
                    secondary.forEach { muscle -> WorkoutTargetChip(muscle) }
                }
                exerciseInstructionText(exercise).takeIf { it.isNotBlank() }?.let { summary ->
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
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
