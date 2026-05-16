package com.example.fitty.feature_plan

import android.content.Context
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessibilityNew
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.example.fitty.R
import com.example.fitty.core.designsystem.component.FittySectionBlock
import com.example.fitty.core.ui.FittyLazyScreen
import com.example.fitty.domain.model.ExerciseQuery
import com.example.fitty.domain.model.Exercise
import com.example.fitty.domain.repository.ExerciseCatalogRepository
import com.example.fitty.domain.usecase.exercise.ObserveExerciseSyncStateUseCase
import com.example.fitty.domain.usecase.exercise.SyncExercisesUseCase
import com.example.fitty.ui.theme.FittyGradientEnd
import com.example.fitty.ui.theme.FittyPink
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

internal data class SampleExercise(
    val id: String,
    val title: String,
    val summary: String,
    val muscle: String,
    val level: String,
    val equipment: String,
    val repsOrDuration: String,
    val thumbnailUrl: String,
    val localThumbnailPath: String = "",
    val videoUrl: String = "",
    val localVideoPath: String = "",
    val isDownloaded: Boolean = false,
    val description: String,
    val steps: List<String>,
    val mistakes: List<String>,
    val tips: List<String>,
    val easierVariation: String,
    val harderVariation: String,
    val targetMuscles: String
) {
    val mediaUrl: String
        get() = if (localThumbnailPath.isNotBlank()) localThumbnailPath else thumbnailUrl
}

internal data class SampleWorkout(
    val title: String,
    val summary: String,
    val duration: String,
    val level: String,
    val equipment: String,
    val exercises: List<SampleExercise>
)

internal enum class PlanTab(val labelRes: Int) {
    Today(R.string.plan_tab_today),
    Programs(R.string.plan_tab_programs),
    Library(R.string.plan_tab_library)
}

internal data class PlanUiState(
    val tabs: List<PlanTab> = PlanTab.entries,
    val selectedTab: PlanTab = PlanTab.Today,
    val exerciseLibrary: List<SampleExercise> = emptyList(),
    val categories: List<PracticeCategory> = emptyList(),
    val selectedCategory: String = CATEGORY_ALL,
    val starterWorkout: SampleWorkout? = null,
    val selectedExercise: SampleExercise? = null,
    val isSyncing: Boolean = false,
    val syncMessage: String? = null
)

internal data class PracticeCategory(
    val label: String,
    val count: Int
)

@HiltViewModel
class PlanViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val exerciseRepository: ExerciseCatalogRepository,
    private val syncExercisesUseCase: SyncExercisesUseCase,
    private val observeExerciseSyncStateUseCase: ObserveExerciseSyncStateUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        PlanUiState()
    )
    internal val uiState: StateFlow<PlanUiState> = _uiState

    init {
        observeExerciseLibrary()
        observeSyncState()
        syncMetadata()
    }

    internal fun selectTab(tab: PlanTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    internal fun selectCategory(category: String) {
        _uiState.update { current ->
            val filtered = current.exerciseLibrary.filterByCategory(category)
            current.copy(
                selectedCategory = category,
                starterWorkout = filtered.takeIf { it.isNotEmpty() }
                    ?.let { buildBeginnerStarterWorkout(context, it, category) },
                selectedExercise = filtered.firstOrNull()
            )
        }
    }

    internal fun selectExercise(exercise: SampleExercise) {
        _uiState.update { it.copy(selectedExercise = exercise) }
    }

    private fun observeExerciseLibrary() {
        viewModelScope.launch {
            exerciseRepository.observeExercises(ExerciseQuery(limit = 250)).collect { exercises ->
                val mappedLibrary = exercises.map { exercise -> exercise.toPracticeExercise(context) }
                val categories = buildCategories(mappedLibrary)
                val selectedCategory = _uiState.value.selectedCategory
                    .takeIf { selected -> categories.any { it.label == selected } }
                    ?: CATEGORY_ALL
                val filteredLibrary = mappedLibrary.filterByCategory(selectedCategory)
                val selectedId = _uiState.value.selectedExercise?.id
                val selectedExercise = filteredLibrary.firstOrNull { it.id == selectedId } ?: filteredLibrary.firstOrNull()

                _uiState.update {
                    it.copy(
                        exerciseLibrary = mappedLibrary,
                        categories = categories,
                        selectedCategory = selectedCategory,
                        starterWorkout = filteredLibrary.takeIf { library -> library.isNotEmpty() }
                            ?.let { buildBeginnerStarterWorkout(context, it, selectedCategory) },
                        selectedExercise = selectedExercise
                    )
                }

            }
        }
    }

    private fun syncMetadata() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, syncMessage = null) }
            val result = syncExercisesUseCase(force = false)
            _uiState.update { state ->
                state.copy(
                    isSyncing = false,
                    syncMessage = result.fold(
                        onSuccess = { report ->
                            "Synced ${report.fetched} exercises. Downloaded ${report.mediaDownloaded} thumbnails."
                        },
                        onFailure = { error ->
                            error.message ?: "Using cached exercise metadata."
                        }
                    )
                )
            }
        }
    }

    private fun observeSyncState() {
        viewModelScope.launch {
            observeExerciseSyncStateUseCase().collect { syncState ->
                _uiState.update {
                    it.copy(
                        isSyncing = syncState.isSyncing,
                        syncMessage = when {
                            syncState.lastErrorMessage != null -> syncState.lastErrorMessage
                            syncState.lastSuccessfulSyncAt != null -> "Metadata cached for offline use."
                            else -> it.syncMessage
                        }
                    )
                }
            }
        }
    }

    private fun buildCategories(exercises: List<SampleExercise>): List<PracticeCategory> {
        val grouped = exercises
            .groupingBy { it.muscle.ifBlank { CATEGORY_OTHER } }
            .eachCount()
            .toSortedMap(String.CASE_INSENSITIVE_ORDER)
        return buildList {
            add(PracticeCategory(label = CATEGORY_ALL, count = exercises.size))
            grouped.forEach { (label, count) ->
                add(PracticeCategory(label = label, count = count))
            }
        }
    }
}

private fun buildBeginnerStarterWorkout(
    context: Context,
    exercises: List<SampleExercise>,
    category: String
): SampleWorkout {
    val focusLabel = if (category == CATEGORY_ALL) {
        context.getString(R.string.plan_focus_full_body)
    } else {
        category
    }
    return SampleWorkout(
        title = if (category == CATEGORY_ALL) {
            context.getString(R.string.plan_workout_full_body_basics_title)
        } else {
            "$category Practice"
        },
        summary = context.getString(
            R.string.plan_workout_summary_format,
            focusLabel,
            context.getString(R.string.plan_level_beginner),
            context.getString(R.string.plan_calories_220)
        ),
        duration = context.getString(R.string.plan_duration_30_min),
        level = context.getString(R.string.plan_level_beginner),
        equipment = context.getString(R.string.plan_equipment_none),
        exercises = exercises.take(4)
    )
}

private fun Exercise.toPracticeExercise(context: Context): SampleExercise {
    val beginner = context.getString(R.string.plan_filter_beginner)
    val bodyPartLabel = bodyPart.ifBlank { context.getString(R.string.plan_tag_full_body) }
    val targetLabel = target.ifBlank { context.getString(R.string.plan_muscle_core) }
    val equipmentLabel = equipment.ifBlank { context.getString(R.string.plan_equipment_none) }

    return SampleExercise(
        id = id,
        title = name.ifBlank { id },
        summary = context.getString(
            R.string.plan_exercise_summary_format,
            bodyPartLabel,
            beginner,
            defaultRepsText.ifBlank { "12 reps" }
        ),
        muscle = bodyPartLabel,
        level = beginner,
        equipment = equipmentLabel,
        repsOrDuration = defaultRepsText.ifBlank { "12 reps" },
        thumbnailUrl = thumbnailUrl.ifBlank { gifUrl },
        localThumbnailPath = localThumbnailPath,
        videoUrl = videoUrl,
        localVideoPath = localVideoPath,
        isDownloaded = isDownloaded,
        description = "Targets $targetLabel with ${equipmentLabel.lowercase()}. Open this exercise once to cache the GIF locally for Practice.",
        steps = listOf(
            "Set up for $name with ${equipmentLabel.lowercase()}.",
            "Move with control and keep tension on $targetLabel.",
            "Reset your posture before starting the next rep."
        ),
        mistakes = listOf(
            "Rushing through the movement.",
            "Shortening the range before fatigue is managed.",
            "Losing control of the start and finish position."
        ),
        tips = listOf(
            "Start at a repeatable tempo.",
            "Exhale through the effort phase.",
            "Open this once on Wi-Fi to keep the GIF offline."
        ),
        easierVariation = "Reduce range or use support.",
        harderVariation = "Add more control, reps, or time under tension.",
        targetMuscles = targetMuscles.ifEmpty { listOf(targetLabel, bodyPartLabel) }.joinToString(", ")
    )
}

@Composable
fun PlanRoute(viewModel: PlanViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    PlanScreen(
        state = state,
        onTabSelected = viewModel::selectTab,
        onCategorySelected = viewModel::selectCategory,
        onExerciseSelected = viewModel::selectExercise
    )
}

@Composable
private fun PlanScreen(
    state: PlanUiState,
    onTabSelected: (PlanTab) -> Unit,
    onCategorySelected: (String) -> Unit,
    onExerciseSelected: (SampleExercise) -> Unit
) {
    val starterWorkout = state.starterWorkout
    val selectedExercise = state.selectedExercise
    val filteredExercises = state.exerciseLibrary.filterByCategory(state.selectedCategory)

    FittyLazyScreen {
        item {
            PracticeTopBar(
                title = stringResource(
                    if (state.selectedTab == PlanTab.Programs) R.string.plan_topbar_programs else R.string.plan_topbar_practice
                )
            )
        }
        item {
            PracticeTabs(
                tabs = state.tabs,
                selectedTab = state.selectedTab,
                onSelected = onTabSelected
            )
        }
        item {
            PracticeLibraryStatus(
                hasExercises = state.exerciseLibrary.isNotEmpty(),
                isSyncing = state.isSyncing,
                syncMessage = state.syncMessage
            )
        }
        if (state.categories.isNotEmpty()) {
            item {
                PracticeCategoryRow(
                    categories = state.categories,
                    selectedCategory = state.selectedCategory,
                    onCategorySelected = onCategorySelected
                )
            }
        }
        when (state.selectedTab) {
            PlanTab.Today -> {
                starterWorkout?.let { workout ->
                    item { TodayPracticeSection(workout) }
                    item {
                        WorkoutSessionDetailPreview(
                            workout = workout,
                            onExerciseSelected = { exercise ->
                                onExerciseSelected(exercise)
                                onTabSelected(PlanTab.Library)
                            }
                        )
                    }
                }
                item { CreatePlanSection() }
                item { CustomWeeklyPlannerPreview() }
            }

            PlanTab.Programs -> {
                item { ProgramsBannerCard() }
                item { ProgramFilterChips() }
                item { ProgramListSection() }
                item { ProgramDetailPreview() }
            }

            PlanTab.Library -> {
                if (selectedExercise != null) {
                    item {
                        ExerciseLibrarySection(
                            exercises = filteredExercises.take(PRACTICE_LIBRARY_PREVIEW_COUNT),
                            selectedExerciseId = selectedExercise.id,
                            onExerciseSelected = onExerciseSelected
                        )
                    }
                    item { ExerciseDetailPreview(selectedExercise) }
                }
                item { BuildWorkoutSection(previewExercises = filteredExercises.take(3)) }
                item { MyCustomPlansSection() }
            }
        }
        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

@Composable
private fun PracticeLibraryStatus(
    hasExercises: Boolean,
    isSyncing: Boolean,
    syncMessage: String?
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Text(
            text = when {
                isSyncing -> "Practice is syncing metadata and thumbnails."
                !syncMessage.isNullOrBlank() -> syncMessage
                hasExercises -> "Practice library is ready."
                else -> "Practice is waiting for exercise data."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun PracticeCategoryRow(
    categories: List<PracticeCategory>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState())
    ) {
        categories.forEach { category ->
            FilterChip(
                selected = category.label == selectedCategory,
                onClick = { onCategorySelected(category.label) },
                label = { Text("${category.label} (${category.count})") }
            )
        }
    }
}

@Composable
private fun PracticeTopBar(title: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Row {
            IconButton(onClick = { }, enabled = false) {
                Icon(Icons.Outlined.Search, contentDescription = null)
            }
            IconButton(onClick = { }, enabled = false) {
                Icon(Icons.Outlined.Tune, contentDescription = null)
            }
        }
    }
}

@Composable
private fun PracticeTabs(
    tabs: List<PlanTab>,
    selectedTab: PlanTab,
    onSelected: (PlanTab) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState())
    ) {
        tabs.forEach { tab ->
            FilterChip(
                selected = selectedTab == tab,
                onClick = { onSelected(tab) },
                label = { Text(stringResource(tab.labelRes)) }
            )
        }
    }
}

@Composable
private fun TodayPracticeSection(workout: SampleWorkout) {
    FittySectionBlock(title = stringResource(R.string.plan_section_todays_practice)) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    ExerciseMediaPreview(
                        exercise = workout.exercises.first(),
                        modifier = Modifier.size(92.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(workout.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    Icon(Icons.Outlined.BookmarkBorder, contentDescription = null, tint = FittyPink)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PracticeStat(Icons.Outlined.Timer, workout.duration, Modifier.weight(1f))
                    PracticeStat(Icons.Outlined.Speed, workout.level, Modifier.weight(1f))
                    PracticeStat(Icons.Outlined.Home, workout.equipment, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    workout.exercises.take(3).forEach { exercise ->
                        ExerciseMediaPreview(
                            exercise = exercise,
                            modifier = Modifier
                                .weight(1f)
                                .height(90.dp)
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { }, enabled = false, shape = RoundedCornerShape(20.dp), modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.plan_action_start_workout))
                    }
                    OutlinedButton(onClick = { }, enabled = false, shape = RoundedCornerShape(20.dp), modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.plan_action_edit_session))
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkoutSessionDetailPreview(
    workout: SampleWorkout,
    onExerciseSelected: (SampleExercise) -> Unit
) {
    FittySectionBlock(title = stringResource(R.string.plan_section_session)) {
        InfoCard(
            icon = Icons.Outlined.PlayArrow,
            title = stringResource(R.string.plan_section_session),
            body = stringResource(R.string.plan_session_summary, workout.exercises.size)
        )
        workout.exercises.forEachIndexed { index, exercise ->
            ExerciseSessionItem(
                number = (index + 1).toString(),
                exercise = exercise,
                onClick = { onExerciseSelected(exercise) }
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { }, enabled = false, shape = RoundedCornerShape(20.dp), modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.plan_action_start))
            }
            OutlinedButton(onClick = { }, enabled = false, shape = RoundedCornerShape(20.dp), modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.plan_action_replace))
            }
        }
    }
}

@Composable
private fun ExerciseSessionItem(
    number: String,
    exercise: SampleExercise,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NumberCircle(number)
            ExerciseMediaPreview(exercise = exercise, modifier = Modifier.size(62.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(exercise.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    stringResource(R.string.plan_exercise_item_meta, exercise.repsOrDuration, exercise.muscle, exercise.level),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.Outlined.Info, contentDescription = null, tint = FittyPink)
        }
    }
}

@Composable
private fun ProgramsBannerCard() {
    Surface(
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            FittyPink, FittyGradientEnd
                        )
                    )
                )
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                stringResource(R.string.plan_banner_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                stringResource(R.string.plan_banner_body),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f)
            )
            Button(onClick = { }, enabled = false, shape = RoundedCornerShape(20.dp)) {
                Text(stringResource(R.string.plan_banner_cta))
            }
        }
    }
}

@Composable
private fun ProgramFilterChips() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState())
    ) {
        listOf(
            stringResource(R.string.plan_filter_all),
            stringResource(R.string.plan_filter_fat_loss),
            stringResource(R.string.plan_filter_muscle_gain),
            stringResource(R.string.plan_filter_beginner),
            stringResource(R.string.plan_filter_home),
            stringResource(R.string.plan_filter_gym),
            stringResource(R.string.plan_filter_short_workouts),
            stringResource(R.string.plan_filter_mobility),
            stringResource(R.string.plan_filter_strength)
        )
            .forEachIndexed { index, label ->
                FilterChip(selected = index == 0, onClick = { }, enabled = false, label = { Text(label) })
            }
    }
}

@Composable
private fun ProgramListSection() {
    FittySectionBlock(title = stringResource(R.string.plan_section_ready_made_programs)) {
        ProgramCard(
            title = stringResource(R.string.plan_program_beginner_fat_loss_title),
            goal = stringResource(R.string.plan_filter_fat_loss),
            meta = stringResource(R.string.plan_program_beginner_fat_loss_meta),
            tags = listOf(stringResource(R.string.plan_filter_beginner), stringResource(R.string.plan_filter_home), stringResource(R.string.plan_tag_no_equipment))
        )
        ProgramCard(
            title = stringResource(R.string.plan_program_home_strength_title),
            goal = stringResource(R.string.plan_filter_strength),
            meta = stringResource(R.string.plan_program_home_strength_meta),
            tags = listOf(stringResource(R.string.plan_filter_beginner), stringResource(R.string.plan_tag_dumbbells), stringResource(R.string.plan_tag_full_body))
        )
        ProgramCard(
            title = stringResource(R.string.plan_program_mobility_reset_title),
            goal = stringResource(R.string.plan_filter_mobility),
            meta = stringResource(R.string.plan_program_mobility_reset_meta),
            tags = listOf(stringResource(R.string.plan_tag_recovery), stringResource(R.string.plan_filter_home), stringResource(R.string.plan_tag_stretching))
        )
    }
}

@Composable
private fun ProgramCard(
    title: String,
    goal: String,
    meta: String,
    tags: List<String>
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                ThumbnailBox(icon = Icons.Outlined.FitnessCenter, modifier = Modifier.size(70.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(goal, style = MaterialTheme.typography.labelLarge, color = FittyPink)
                    Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            ChipRow(tags)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { }, enabled = false, shape = RoundedCornerShape(20.dp), modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.plan_action_view_program))
                }
                Button(onClick = { }, enabled = false, shape = RoundedCornerShape(20.dp), modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.plan_action_start_plan))
                }
            }
        }
    }
}

@Composable
private fun ProgramDetailPreview() {
    FittySectionBlock(title = stringResource(R.string.plan_section_program_detail_preview)) {
        HeaderImageCard(
            title = stringResource(R.string.plan_program_detail_title),
            subtitle = stringResource(R.string.plan_program_detail_subtitle),
            icon = Icons.Outlined.FitnessCenter
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PracticeStat(Icons.Outlined.CalendarMonth, stringResource(R.string.plan_stat_4_weeks), Modifier.weight(1f))
            PracticeStat(Icons.Outlined.FitnessCenter, stringResource(R.string.plan_stat_4_days_week), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PracticeStat(Icons.Outlined.Timer, stringResource(R.string.plan_stat_20_30_min), Modifier.weight(1f))
            PracticeStat(Icons.Outlined.Home, stringResource(R.string.plan_tag_no_equipment), Modifier.weight(1f))
        }
        NotesCard(
            title = stringResource(R.string.plan_section_why_this_program),
            notes = listOf(
                stringResource(R.string.plan_program_detail_note_1),
                stringResource(R.string.plan_program_detail_note_2),
                stringResource(R.string.plan_program_detail_note_3)
            )
        )
    }
}

@Composable
private fun ExerciseLibrarySection(
    exercises: List<SampleExercise>,
    selectedExerciseId: String,
    onExerciseSelected: (SampleExercise) -> Unit
) {
    FittySectionBlock(title = stringResource(R.string.plan_section_exercise_library)) {
        OutlinedTextField(
            value = "",
            onValueChange = { },
            enabled = false,
            label = { Text(stringResource(R.string.plan_search_exercises)) },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            listOf(
                stringResource(R.string.plan_filter_muscle_group),
                stringResource(R.string.plan_filter_equipment),
                stringResource(R.string.plan_filter_difficulty),
                stringResource(R.string.plan_label_gif_demo),
                stringResource(R.string.plan_muscle_legs),
                stringResource(R.string.plan_muscle_core),
                stringResource(R.string.plan_filter_beginner),
                stringResource(R.string.plan_tag_no_equipment)
            )
                .forEachIndexed { index, label ->
                    FilterChip(selected = index == 6, onClick = { }, enabled = false, label = { Text(label) })
                }
        }
        Text(
            text = stringResource(R.string.plan_exercise_library_intro),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        exercises.forEach { exercise ->
            ExerciseLibraryItem(
                exercise = exercise,
                selected = selectedExerciseId == exercise.id,
                onClick = { onExerciseSelected(exercise) }
            )
        }
    }
}

@Composable
private fun ExerciseLibraryItem(
    exercise: SampleExercise,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            else MaterialTheme.colorScheme.surface
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExerciseMediaPreview(exercise = exercise, modifier = Modifier.size(72.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(exercise.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(exercise.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    stringResource(R.string.plan_gif_demo_with_equipment, exercise.equipment),
                    style = MaterialTheme.typography.labelMedium,
                    color = FittyPink
                )
            }
            Icon(Icons.Outlined.BookmarkBorder, contentDescription = null, tint = FittyPink)
        }
    }
}

@Composable
private fun ExerciseDetailPreview(exercise: SampleExercise) {
    FittySectionBlock(title = stringResource(R.string.plan_section_exercise_detail)) {
        ExerciseHeroCard(exercise)
        InfoCard(
            icon = Icons.Outlined.PlayArrow,
            title = stringResource(R.string.plan_section_why_beginners_use_this),
            body = exercise.description
        )
        NotesCard(title = stringResource(R.string.plan_section_how_to_do_it), notes = exercise.steps)
        NotesCard(title = stringResource(R.string.plan_section_common_mistakes), notes = exercise.mistakes, icon = Icons.Outlined.WarningAmber)
        NotesCard(title = stringResource(R.string.plan_section_trainer_tips), notes = exercise.tips)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            InfoCard(
                icon = Icons.Outlined.Home,
                title = stringResource(R.string.plan_section_easier),
                body = exercise.easierVariation,
                modifier = Modifier.weight(1f)
            )
            InfoCard(
                icon = Icons.Outlined.Speed,
                title = stringResource(R.string.plan_section_harder),
                body = exercise.harderVariation,
                modifier = Modifier.weight(1f)
            )
        }
        InfoCard(
            icon = Icons.Outlined.AccessibilityNew,
            title = stringResource(R.string.plan_section_target_muscles),
            body = exercise.targetMuscles
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { }, enabled = false, shape = RoundedCornerShape(20.dp), modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.plan_action_add))
            }
            OutlinedButton(onClick = { }, enabled = false, shape = RoundedCornerShape(20.dp), modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.plan_action_save))
            }
        }
    }
}

@Composable
private fun ExerciseHeroCard(exercise: SampleExercise) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ExerciseMediaPreview(
                exercise = exercise,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            )
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(exercise.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    stringResource(R.string.plan_exercise_hero_meta, exercise.level, exercise.muscle, exercise.equipment),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            ChipRow(listOf(stringResource(R.string.plan_label_gif_demo), exercise.repsOrDuration, exercise.muscle, exercise.level))
            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}

@Composable
private fun CreatePlanSection() {
    FittySectionBlock(title = stringResource(R.string.plan_section_create_plan)) {
        OutlinedTextField(
            value = stringResource(R.string.plan_default_plan_name),
            onValueChange = { },
            enabled = false,
            label = { Text(stringResource(R.string.plan_field_plan_name)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        ChipRow(listOf(stringResource(R.string.plan_filter_fat_loss), stringResource(R.string.plan_filter_muscle_gain), stringResource(R.string.plan_tag_general_fitness), stringResource(R.string.plan_filter_mobility), stringResource(R.string.plan_filter_strength)))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SelectorBox(stringResource(R.string.plan_field_weeks), "4", Modifier.weight(1f))
            SelectorBox(stringResource(R.string.plan_field_workouts_per_week), "4", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { }, enabled = false, shape = RoundedCornerShape(20.dp), modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.plan_action_generate))
            }
            OutlinedButton(onClick = { }, enabled = false, shape = RoundedCornerShape(20.dp), modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.plan_action_build_manually))
            }
        }
    }
}

@Composable
private fun CustomWeeklyPlannerPreview() {
    FittySectionBlock(title = stringResource(R.string.plan_section_custom_weekly_planner)) {
        listOf(
            stringResource(R.string.plan_day_monday) to stringResource(R.string.plan_planner_monday_detail),
            stringResource(R.string.plan_day_tuesday) to stringResource(R.string.plan_planner_tuesday_detail),
            stringResource(R.string.plan_day_wednesday) to stringResource(R.string.plan_planner_wednesday_detail),
            stringResource(R.string.plan_day_thursday) to stringResource(R.string.plan_planner_thursday_detail)
        ).forEach { (day, detail) ->
            PlannerDayCard(day, detail)
        }
        Button(onClick = { }, enabled = false, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.plan_action_save_weekly_plan))
        }
    }
}

@Composable
private fun PlannerDayCard(day: String, detail: String) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (detail == stringResource(R.string.plan_planner_tuesday_detail)) Icons.Outlined.Add else Icons.Outlined.CalendarMonth,
                contentDescription = null,
                tint = FittyPink
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(day, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun BuildWorkoutSection(previewExercises: List<SampleExercise>) {
    FittySectionBlock(title = stringResource(R.string.plan_section_build_workout)) {
        OutlinedTextField(
            value = stringResource(R.string.plan_default_workout_name),
            onValueChange = { },
            enabled = false,
            label = { Text(stringResource(R.string.plan_field_workout_name)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SelectorBox(stringResource(R.string.plan_field_focus_area), stringResource(R.string.plan_tag_full_body), Modifier.weight(1f))
            SelectorBox(stringResource(R.string.plan_filter_difficulty), stringResource(R.string.plan_filter_beginner), Modifier.weight(1f))
        }
        previewExercises.forEach { exercise ->
            BuilderExerciseItem(exercise.title, exercise.repsOrDuration)
        }
        OutlinedButton(onClick = { }, enabled = false, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.Add, contentDescription = null)
            Text(stringResource(R.string.plan_action_add_exercise), modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun BuilderExerciseItem(title: String, detail: String) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.FitnessCenter, contentDescription = null, tint = FittyPink)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun MyCustomPlansSection() {
    FittySectionBlock(title = stringResource(R.string.plan_section_my_custom_plans)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            listOf(stringResource(R.string.plan_filter_active), stringResource(R.string.plan_filter_drafts), stringResource(R.string.plan_filter_completed), stringResource(R.string.plan_filter_saved_templates)).forEachIndexed { index, label ->
                FilterChip(selected = index == 0, onClick = { }, enabled = false, label = { Text(label) })
            }
        }
        CustomPlanCard(stringResource(R.string.plan_custom_plan_home_strength_title), stringResource(R.string.plan_custom_plan_home_strength_body))
        CustomPlanCard(stringResource(R.string.plan_custom_plan_weekend_mobility_title), stringResource(R.string.plan_custom_plan_weekend_mobility_body))
    }
}

@Composable
private fun CustomPlanCard(title: String, body: String) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { }, enabled = false, shape = RoundedCornerShape(20.dp), modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.plan_action_start))
                }
                OutlinedButton(onClick = { }, enabled = false, shape = RoundedCornerShape(20.dp), modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.plan_action_edit))
                }
            }
        }
    }
}

@Composable
private fun ExerciseMediaPreview(
    exercise: SampleExercise,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imageLoader = rememberGifImageLoader()
    val imageModel: Any? = when {
        exercise.localThumbnailPath.isNotBlank() -> File(exercise.localThumbnailPath)
        exercise.thumbnailUrl.startsWith("http://", ignoreCase = true) ||
            exercise.thumbnailUrl.startsWith("https://", ignoreCase = true) -> exercise.thumbnailUrl
        else -> null
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.TopEnd
    ) {
        if (imageModel != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageModel)
                    .crossfade(true)
                    .build(),
                imageLoader = imageLoader,
                contentDescription = exercise.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        } else {
            Box(
                modifier = Modifier.matchParentSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Thumbnail unavailable",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        AssistChip(
            onClick = { },
            enabled = false,
            label = { Text(stringResource(R.string.plan_label_gif)) },
            leadingIcon = {
                Icon(
                    Icons.Outlined.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            },
            modifier = Modifier.padding(8.dp)
        )
    }
}

private fun List<SampleExercise>.filterByCategory(category: String): List<SampleExercise> {
    if (category == CATEGORY_ALL) return this
    return filter { exercise -> exercise.muscle.equals(category, ignoreCase = true) }
}

private const val CATEGORY_ALL = "All"
private const val CATEGORY_OTHER = "Other"
private const val PRACTICE_LIBRARY_PREVIEW_COUNT = 8

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

@Composable
private fun HeaderImageCard(title: String, subtitle: String, icon: ImageVector) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ThumbnailBox(icon = icon, modifier = Modifier.size(82.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun InfoCard(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = FittyPink)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun NotesCard(
    title: String,
    notes: List<String>,
    icon: ImageVector = Icons.Outlined.CheckCircle
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = FittyPink)
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            notes.forEach {
                Text(stringResource(R.string.plan_bullet_note, it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PracticeStat(icon: ImageVector, label: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = null, tint = FittyPink, modifier = Modifier.size(20.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun SelectorBox(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipRow(labels: List<String>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 14.dp)
    ) {
        labels.forEachIndexed { index, label ->
            AssistChip(
                onClick = { },
                enabled = false,
                label = { Text(label) },
                leadingIcon = if (index == 0) {
                    { Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp)) }
                } else {
                    null
                }
            )
        }
    }
}

@Composable
private fun NumberCircle(number: String) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .background(MaterialTheme.colorScheme.primary, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(number, color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ThumbnailBox(icon: ImageVector, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
    }
}



