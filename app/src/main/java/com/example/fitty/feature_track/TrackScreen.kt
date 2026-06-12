package com.example.fitty.feature_track

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AccessibilityNew
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.example.fitty.R
import com.example.fitty.core.ui.ContentDebugSource
import com.example.fitty.core.ui.ContentSourceState
import com.example.fitty.core.ui.FittyLazyScreen
import com.example.fitty.data.content.LocalContentFallbacks
import com.example.fitty.domain.model.TrackBehaviorConfig
import com.example.fitty.domain.repository.ContentRepository
import com.example.fitty.ui.theme.FittyPink
import dagger.hilt.android.lifecycle.HiltViewModel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

internal enum class TrackTab {
    Meals,
    Body,
    Progress,
    Stats
}

internal data class TrackAnalysisRow(
    val label: String,
    val value: String
)

internal data class TrackAnalysisResult(
    val title: String,
    val summary: String,
    val rows: List<TrackAnalysisRow>
)

internal data class MealScanHistoryUi(
    val id: String,
    val imageUrl: String,
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fat: Int,
    val foodCount: Int,
    val dateKey: String,
    val timestamp: Long
)

internal data class BodyScanHistoryUi(
    val id: String,
    val imageUrl: String,
    val summary: String,
    val bodyFatPercent: Float?,
    val postureScore: Int?,
    val confidence: Float,
    val status: String,
    val timestamp: Long
)

internal data class TrackUiState(
    val selectedTab: TrackTab? = null,
    val tabs: List<TrackTab> = listOf(TrackTab.Meals, TrackTab.Body, TrackTab.Progress, TrackTab.Stats),
    val capturedImageUri: String? = null,
    val isLoading: Boolean = true,
    val isSubmittingImage: Boolean = false,
    val analysisResult: TrackAnalysisResult? = null,
    val captureError: String? = null,
    val mealConfirmed: Boolean = false,
    val bodyScanSaved: Boolean = false,
    // Progress
    val progressWeight: String = "--",
    val progressWeightPercent: Float = 0f,
    val progressWorkouts: String = "0 / 0",
    val progressWorkoutPercent: Float = 0f,
    val progressMeals: String = "0",
    val progressMealPercent: Float = 0f,
    val targetWeight: String = "--",
    val bmi: String = "--",
    val totalCaloriesBurned: String = "0",
    val avgDailyCalories: String = "0",
    val weeklyActiveDays: List<Boolean> = List(7) { false },
    val completionRate: Int = 0,
    // Stats
    val statWorkouts: String = "0",
    val statMeals: String = "0",
    val statStreak: String = "0",
    val statActiveMin: String = "0",
    val statBestStreak: String = "0",
    val statAvgCalories: String = "0",
    val statProteinAvg: String = "0",
    val statCarbsAvg: String = "0",
    val statFatAvg: String = "0",
    // Scan history
    val mealHistory: List<Pair<String, String>> = emptyList(),
    val scanHistory: List<MealScanHistoryUi> = emptyList(),
    val bodyScanHistory: List<BodyScanHistoryUi> = emptyList(),
    val contentSources: List<ContentDebugSource> = emptyList()
)

@Composable
fun TrackRoute(viewModel: TrackViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    TrackScreen(
        state = state,
        onTabSelected = viewModel::selectTab,
        onBackToPicker = viewModel::backToPicker,
        onImageCaptured = viewModel::setCapturedImage,
        onCaptureError = viewModel::setCaptureError,
        onSubmitImage = viewModel::submitCapturedImage,
        onConfirmMeal = viewModel::confirmMeal,
        onSaveBodyScan = viewModel::saveBodyScan
    )
}

@HiltViewModel
class TrackViewModel @Inject constructor(
    private val analyzeMealImageUseCase: com.example.fitty.domain.usecase.track.AnalyzeMealImageUseCase,
    private val confirmMealLogUseCase: com.example.fitty.domain.usecase.track.ConfirmMealLogUseCase,
    private val analyzeBodyScanUseCase: com.example.fitty.domain.usecase.track.AnalyzeBodyScanUseCase,
    private val saveBodyScanUseCase: com.example.fitty.domain.usecase.track.SaveBodyScanUseCase,
    private val getProgressStatsUseCase: com.example.fitty.domain.usecase.track.GetProgressStatsUseCase,
    private val getMealLogsUseCase: com.example.fitty.domain.usecase.track.GetMealLogsUseCase,
    private val getMealScanHistoryUseCase: com.example.fitty.domain.usecase.track.GetMealScanHistoryUseCase,
    private val getBodyScansUseCase: com.example.fitty.domain.usecase.track.GetBodyScansUseCase,
    private val updateStreakUseCase: com.example.fitty.domain.usecase.user.UpdateStreakUseCase,
    private val localContentFallbacks: LocalContentFallbacks,
    private val contentRepository: ContentRepository,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        TrackUiState(
            contentSources = listOf(
                ContentDebugSource("Track behavior", ContentSourceState.Fallback, "Using local fallback until remote load completes")
            )
        )
    )
    internal val uiState: StateFlow<TrackUiState> = _uiState
    private var behaviorConfig: TrackBehaviorConfig = localContentFallbacks.trackBehaviorConfig()

    private var lastMealResult: com.example.fitty.domain.model.MealAnalysisResult? = null
    private var lastBodyResult: com.example.fitty.domain.model.BodyScanAnalysisResult? = null

    init {
        loadBehaviorConfig()
        loadStats()
        loadScanHistory()
        loadBodyScanHistory()
    }

    private fun loadBehaviorConfig() {
        viewModelScope.launch {
            behaviorConfig = contentRepository.getTrackBehaviorConfig()
            val usedFallback = contentRepository.usedFallbackFor("track_behavior")
            _uiState.update {
                it.copy(
                    contentSources = listOf(
                        ContentDebugSource(
                            "Track behavior",
                            if (usedFallback) ContentSourceState.Fallback else ContentSourceState.Remote,
                            contentRepository.fallbackDetailFor("track_behavior")
                                ?: if (usedFallback) "Using local fallback" else "Loaded from Firebase"
                        )
                    )
                )
            }
            loadStats()
        }
    }

    private fun loadScanHistory() {
        viewModelScope.launch {
            runCatching { getMealScanHistoryUseCase() }.onSuccess { records ->
                _uiState.update { state ->
                    state.copy(scanHistory = records.map { r ->
                        MealScanHistoryUi(
                            id = r.id,
                            imageUrl = r.imageUrl.ifBlank { r.localImagePath },
                            calories = r.totalCalories,
                            protein = r.totalProtein,
                            carbs = r.totalCarbs,
                            fat = r.totalFat,
                            foodCount = r.foodItems.size,
                            dateKey = r.dateKey,
                            timestamp = r.timestamp
                        )
                    })
                }
            }
        }
    }

    private fun loadBodyScanHistory() {
        viewModelScope.launch {
            runCatching { getBodyScansUseCase() }.onSuccess { scans ->
                _uiState.update { state ->
                    state.copy(
                        bodyScanHistory = scans.map { scan ->
                            BodyScanHistoryUi(
                                id = scan.id,
                                imageUrl = scan.frontImageUrl.orEmpty(),
                                summary = scan.summary,
                                bodyFatPercent = scan.estimatedBodyFatPercent,
                                postureScore = scan.postureScore,
                                confidence = scan.confidence,
                                status = scan.status,
                                timestamp = scan.capturedAt
                            )
                        }
                    )
                }
            }
        }
    }

    private fun loadStats() {
        viewModelScope.launch {
            runCatching { getProgressStatsUseCase() }.onSuccess { stats ->
                val totalWorkoutsCompleted = stats.dailySummaries.sumOf { s -> s.progress.workoutsCompleted }
                val totalWorkoutTarget = stats.dailySummaries.sumOf { s -> s.targets.workouts }.coerceAtLeast(1)
                val totalCalBurned = stats.dailySummaries.sumOf { s -> s.progress.caloriesBurned }
                val totalActiveMinutes = stats.dailySummaries.sumOf { s -> s.progress.activeMinutes }
                val avgCal = if (stats.dailySummaries.isNotEmpty()) totalCalBurned / stats.dailySummaries.size else 0

                // Map weekly active days to actual days of the week (Mon=0..Sun=6)
                val today = java.time.LocalDate.now()
                val mondayOfThisWeek = today.with(java.time.DayOfWeek.MONDAY)
                val weekActive = MutableList(7) { false }
                stats.dailySummaries.forEach { s ->
                    runCatching {
                        val date = java.time.LocalDate.parse(s.dateKey)
                        val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(mondayOfThisWeek, date).toInt()
                        if (daysBetween in 0..6) {
                            weekActive[daysBetween] = s.progress.workoutsCompleted > 0 || s.progress.mealsLogged > 0
                        }
                    }
                }
                val activeDays = weekActive.count { it }
                val rate = (activeDays * 100) / 7

                // Macro averages from scan history
                val totalProtein = stats.dailySummaries.sumOf { s -> s.progress.proteinGrams }
                val totalCarbs = stats.dailySummaries.sumOf { s -> s.progress.carbsGrams }
                val totalFat = stats.dailySummaries.sumOf { s -> s.progress.fatGrams }
                val dayCount = stats.dailySummaries.size.coerceAtLeast(1)
                val baselineWeight = stats.bodyMeasurements.lastOrNull()?.weightKg ?: stats.latestWeight

                _uiState.update {
                    it.copy(
                        statWorkouts = stats.totalWorkouts.toString(),
                        statMeals = stats.totalMealsLogged.toString(),
                        statStreak = context.getString(R.string.track_days_value, stats.currentStreak),
                        statActiveMin = "$totalActiveMinutes",
                        statBestStreak = context.getString(R.string.track_days_value, stats.bestStreak),
                        statAvgCalories = context.getString(R.string.track_kcal_value, avgCal),
                        statProteinAvg = context.getString(R.string.track_grams_value, totalProtein / dayCount),
                        statCarbsAvg = context.getString(R.string.track_grams_value, totalCarbs / dayCount),
                        statFatAvg = context.getString(R.string.track_grams_value, totalFat / dayCount),
                        progressWeight = stats.latestWeight?.let { w -> context.getString(R.string.track_weight_value, w) } ?: "--",
                        progressWeightPercent = stats.latestWeight?.let { w ->
                            val target = stats.targetWeight
                            val start = baselineWeight
                            when {
                                target == null || start == null -> 0f
                                kotlin.math.abs(target - start) < 0.01f -> 1f
                                target < start -> ((start - w) / (start - target)).coerceIn(0f, 1f)
                                else -> ((w - start) / (target - start)).coerceIn(0f, 1f)
                            }
                        } ?: 0f,
                        targetWeight = stats.targetWeight?.let { w -> context.getString(R.string.track_weight_value, w) } ?: "--",
                        bmi = stats.bmi?.let { b -> "%.1f".format(b) } ?: "--",
                        progressWorkouts = "$totalWorkoutsCompleted/$totalWorkoutTarget",
                        progressWorkoutPercent = (totalWorkoutsCompleted.toFloat() / totalWorkoutTarget).coerceIn(0f, 1f),
                        progressMeals = stats.mealTargetPerDay?.takeIf { it > 0 }?.let { mealTarget ->
                            "${stats.totalMealsLogged}/$mealTarget"
                        } ?: context.getString(R.string.track_progress_total_meals, stats.totalMealsLogged),
                        progressMealPercent = if ((stats.mealTargetPerDay ?: 0) > 0) {
                            (stats.totalMealsLogged.toFloat() / stats.mealTargetPerDay!!).coerceIn(0f, 1f)
                        } else {
                            0f
                        },
                        totalCaloriesBurned = "$totalCalBurned",
                        avgDailyCalories = "$avgCal",
                        weeklyActiveDays = weekActive,
                        completionRate = rate,
                        isLoading = false
                    )
                }
            }.onFailure {
                _uiState.update { it.copy(isLoading = false) }
            }
            runCatching { getMealLogsUseCase() }.onSuccess { logs ->
                _uiState.update {
                    it.copy(
                        mealHistory = logs.map { m ->
                            m.mealType.toLocalizedTrackToken(context) to context.getString(R.string.track_kcal_value, m.totalCalories)
                        }
                    )
                }
            }
        }
    }

    internal fun selectTab(tab: TrackTab) {
        _uiState.update {
            it.copy(selectedTab = tab, capturedImageUri = null, isSubmittingImage = false,
                analysisResult = null, captureError = null, mealConfirmed = false, bodyScanSaved = false)
        }
    }

    internal fun backToPicker() {
        _uiState.update {
            it.copy(selectedTab = null, capturedImageUri = null, isSubmittingImage = false,
                analysisResult = null, captureError = null, mealConfirmed = false, bodyScanSaved = false)
        }
        loadStats()
    }

    internal fun setCapturedImage(uri: String) {
        lastMealResult = null
        lastBodyResult = null
        _uiState.update {
            it.copy(
                capturedImageUri = uri,
                analysisResult = null,
                captureError = null,
                mealConfirmed = false,
                bodyScanSaved = false
            )
        }
    }

    internal fun setCaptureError(message: String) {
        _uiState.update { it.copy(captureError = message, isSubmittingImage = false) }
    }

    internal fun submitCapturedImage() {
        val current = _uiState.value
        val selectedTab = current.selectedTab
        if (selectedTab == null) { setCaptureError(context.getString(R.string.track_error_choose_tab_first)); return }
        if (current.capturedImageUri.isNullOrBlank()) { setCaptureError(context.getString(R.string.track_error_take_photo_first)); return }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSubmittingImage = true,
                    captureError = null,
                    mealConfirmed = false,
                    bodyScanSaved = false
                )
            }
            when (selectedTab) {
                TrackTab.Meals -> {
                    analyzeMealImageUseCase(current.capturedImageUri)
                        .onSuccess { result ->
                            lastMealResult = result
                            _uiState.update { it.copy(isSubmittingImage = false, analysisResult = result.toUi()) }
                        }
                        .onFailure { e -> _uiState.update { it.copy(isSubmittingImage = false, captureError = e.message) } }
                }
                TrackTab.Body -> {
                    analyzeBodyScanUseCase(current.capturedImageUri)
                        .onSuccess { result ->
                            lastBodyResult = result
                            _uiState.update { it.copy(isSubmittingImage = false, analysisResult = result.toUi()) }
                        }
                        .onFailure { e -> _uiState.update { it.copy(isSubmittingImage = false, captureError = e.message) } }
                }
                else -> _uiState.update { it.copy(isSubmittingImage = false) }
            }
        }
    }

    private fun com.example.fitty.domain.model.MealAnalysisResult.toUi() = TrackAnalysisResult(
        title = context.getString(R.string.track_analysis_meal_title),
        summary = context.getString(R.string.track_analysis_meal_summary, mealLog.foodItems.size, (confidence * 100).toInt()),
        rows = listOf(
            TrackAnalysisRow(context.getString(R.string.track_row_calories), context.getString(R.string.track_kcal_value, mealLog.totalCalories)),
            TrackAnalysisRow(context.getString(R.string.track_row_protein), context.getString(R.string.track_grams_value, mealLog.totalProtein)),
            TrackAnalysisRow(context.getString(R.string.track_row_carbs), context.getString(R.string.track_grams_value, mealLog.totalCarbs)),
            TrackAnalysisRow(context.getString(R.string.track_row_fat), context.getString(R.string.track_grams_value, mealLog.totalFat))
        ) + mealLog.foodItems.map { TrackAnalysisRow(it.name, context.getString(R.string.track_kcal_value, it.calories)) }
    )

    private fun com.example.fitty.domain.model.BodyScanAnalysisResult.toUi() = TrackAnalysisResult(
        title = context.getString(R.string.track_analysis_body_title),
        summary = context.getString(
            R.string.track_analysis_body_summary,
            bodyScan.summary.ifBlank { context.getString(R.string.track_analysis_complete) },
            (confidence * 100).toInt()
        ),
        rows = listOfNotNull(
            bodyScan.estimatedBodyFatPercent?.let { TrackAnalysisRow(context.getString(R.string.track_row_body_fat), "%.1f%%".format(it)) },
            bodyScan.postureScore?.let { TrackAnalysisRow(context.getString(R.string.track_row_posture_score), context.getString(R.string.track_posture_score_value, it)) },
            TrackAnalysisRow(context.getString(R.string.track_row_status), bodyScan.status.toLocalizedTrackToken(context))
        )
    )

    internal fun confirmMeal() {
        val meal = lastMealResult?.mealLog ?: return
        val imageUri = _uiState.value.capturedImageUri
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingImage = true) }
            confirmMealLogUseCase(meal, imageUri)
                .onSuccess {
                    _uiState.update { it.copy(isSubmittingImage = false, mealConfirmed = true) }
                    runCatching { updateStreakUseCase(reason = "meal", incrementActivityCounters = false) }
                    loadStats()
                    loadScanHistory()
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isSubmittingImage = false, captureError = e.message) }
                }
        }
    }

    internal fun saveBodyScan() {
        val scan = lastBodyResult?.bodyScan ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingImage = true) }
            saveBodyScanUseCase(scan)
                .onSuccess {
                    _uiState.update { it.copy(isSubmittingImage = false, bodyScanSaved = true) }
                    runCatching { updateStreakUseCase("body_scan") }
                    loadStats()
                    loadBodyScanHistory()
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isSubmittingImage = false, captureError = e.message) }
                }
        }
    }
}

@Composable
private fun TrackScreen(
    state: TrackUiState,
    onTabSelected: (TrackTab) -> Unit,
    onBackToPicker: () -> Unit,
    onImageCaptured: (String) -> Unit,
    onCaptureError: (String) -> Unit,
    onSubmitImage: () -> Unit,
    onConfirmMeal: () -> Unit,
    onSaveBodyScan: () -> Unit
) {
    val context = LocalContext.current
    var pendingCaptureUri by rememberSaveable { mutableStateOf<String?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val imageUri = pendingCaptureUri
        pendingCaptureUri = null
        if (success && imageUri != null) {
            onImageCaptured(imageUri)
        } else {
            onCaptureError(context.getString(R.string.track_error_capture_cancelled))
        }
    }
    fun launchCameraCapture() {
        val imageUri = createTrackImageUri(context)
        pendingCaptureUri = imageUri.toString()
        cameraLauncher.launch(imageUri)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            launchCameraCapture()
        } else {
            onCaptureError(context.getString(R.string.track_error_permission_denied))
        }
    }
    val onOpenCamera = {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            launchCameraCapture()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    val openAppSettings = {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    FittyLazyScreen {
        item {
            Text(stringResource(R.string.track_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
        val selectedTab = state.selectedTab
        if (selectedTab == null) {
            item {
                TrackFeaturePicker(
                    tabs = state.tabs,
                    onTabSelected = onTabSelected
                )
            }
        } else {
            item {
                TrackDetailHeader(
                    tab = selectedTab,
                    onBackToPicker = onBackToPicker
                )
            }
            when (selectedTab) {
                TrackTab.Meals -> {
                    item { MealsTab(state = state, onOpenCamera = onOpenCamera, onSubmitImage = onSubmitImage, onConfirmMeal = onConfirmMeal) }
                }
                TrackTab.Body -> {
                    item { BodyTab(state = state, onOpenCamera = onOpenCamera, onSubmitImage = onSubmitImage, onSaveBodyScan = onSaveBodyScan) }
                    item { BodyHistorySection(state) }
                }
                TrackTab.Progress -> {
                    item { ProgressTab(state) }
                }
                TrackTab.Stats -> {
                    item { StatsTab(state) }
                }
            }
            if (selectedTab == TrackTab.Meals || selectedTab == TrackTab.Body) {
                item {
                    RecoveryHintCard(
                        state = state,
                        onOpenCamera = onOpenCamera,
                        onSubmitImage = onSubmitImage,
                        onOpenAppSettings = openAppSettings
                    )
                }
            }
        }
    }
}

private fun trackTabLabel(context: Context, tab: TrackTab): String {
    return when (tab) {
        TrackTab.Meals -> context.getString(R.string.track_tab_meals)
        TrackTab.Body -> context.getString(R.string.track_tab_body)
        TrackTab.Progress -> context.getString(R.string.track_tab_progress)
        TrackTab.Stats -> context.getString(R.string.track_tab_stats)
    }
}

private fun trackTabDescription(context: Context, tab: TrackTab): String {
    return when (tab) {
        TrackTab.Meals -> context.getString(R.string.track_tab_meals_desc)
        TrackTab.Body -> context.getString(R.string.track_tab_body_desc)
        TrackTab.Progress -> context.getString(R.string.track_tab_progress_desc)
        TrackTab.Stats -> context.getString(R.string.track_tab_stats_desc)
    }
}

private fun trackTabIcon(tab: TrackTab): ImageVector {
    return when (tab) {
        TrackTab.Meals -> Icons.Outlined.Restaurant
        TrackTab.Body -> Icons.Outlined.AccessibilityNew
        TrackTab.Progress -> Icons.Outlined.Timeline
        TrackTab.Stats -> Icons.Outlined.BarChart
    }
}

@Composable
private fun TrackFeaturePicker(
    tabs: List<TrackTab>,
    onTabSelected: (TrackTab) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            stringResource(R.string.track_choose_content),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        tabs.forEach { tab ->
            TrackFeatureCard(
                tab = tab,
                onClick = { onTabSelected(tab) }
            )
        }
    }
}

@Composable
private fun TrackFeatureCard(
    tab: TrackTab,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(FittyPink.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(trackTabIcon(tab), contentDescription = null, tint = FittyPink, modifier = Modifier.size(24.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(trackTabLabel(LocalContext.current, tab), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(trackTabDescription(LocalContext.current, tab), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun TrackDetailHeader(
    tab: TrackTab,
    onBackToPicker: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(onClick = onBackToPicker, shape = RoundedCornerShape(16.dp)) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(stringResource(R.string.track_choose_other), modifier = Modifier.padding(start = 8.dp), fontWeight = FontWeight.SemiBold)
        }
        Text(trackTabLabel(LocalContext.current, tab), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MealsTab(
    state: TrackUiState,
    onOpenCamera: () -> Unit,
    onSubmitImage: () -> Unit,
    onConfirmMeal: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // ── Camera Scan Card ──
        CameraAnalysisCard(
            icon = Icons.Outlined.CameraAlt,
            title = stringResource(R.string.track_meal_scan_title),
            body = stringResource(R.string.track_meal_scan_body),
            primaryAction = stringResource(R.string.track_take_photo),
            state = state,
            onOpenCamera = onOpenCamera,
            onSubmitImage = onSubmitImage
        )

        // ── Confirm button after analysis ──
        if (state.analysisResult != null && state.selectedTab == TrackTab.Meals) {
            if (state.mealConfirmed) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2E7D32).copy(alpha = 0.08f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.CheckCircle, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(18.dp))
                        Text(
                            stringResource(R.string.track_saved_badge_meal),
                            modifier = Modifier.padding(start = 8.dp),
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                Button(
                    onClick = onConfirmMeal,
                    enabled = !state.isSubmittingImage,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FittyPink),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    if (state.isSubmittingImage) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Icon(Icons.Outlined.CheckCircle, null, modifier = Modifier.size(18.dp))
                        Text(stringResource(R.string.track_confirm_save), modifier = Modifier.padding(start = 8.dp), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // ── Daily Summary Card ── (computed from today's scan history)
        val todayDate = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
        val todayScans = state.scanHistory.filter { it.dateKey == todayDate }
        val todayMealCount = todayScans.size
        val todayProtein = todayScans.sumOf { it.protein }
        val todayCarbs = todayScans.sumOf { it.carbs }
        val todayFat = todayScans.sumOf { it.fat }
        val todayCalories = todayScans.sumOf { it.calories }

        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(FittyPink.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Restaurant, contentDescription = null, tint = FittyPink, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text(stringResource(R.string.track_today_total), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            "${stringResource(R.string.track_today_meal_count, todayMealCount)} • ${stringResource(R.string.track_kcal_value, todayCalories)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    MacroStatPill("kcal", "$todayCalories", FittyPink)
                    MacroStatPill(stringResource(R.string.track_row_protein), "${todayProtein}g", Color(0xFF6C63FF))
                    MacroStatPill(stringResource(R.string.track_row_carbs), "${todayCarbs}g", Color(0xFFFF9F43))
                    MacroStatPill(stringResource(R.string.track_row_fat), "${todayFat}g", Color(0xFF2ED573))
                }
            }
        }

        // ── Scan History ──
        if (state.scanHistory.isNotEmpty()) {
            Text(stringResource(R.string.track_scan_history), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            state.scanHistory.forEach { scan ->
                ScanHistoryCard(scan)
            }
        }

        if (state.scanHistory.isEmpty()) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(FittyPink.copy(alpha = 0.06f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Restaurant, contentDescription = null, tint = FittyPink.copy(alpha = 0.4f), modifier = Modifier.size(24.dp))
                    }
                    Text(stringResource(R.string.track_empty_meals_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.track_empty_meals_body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun BodyTab(
    state: TrackUiState,
    onOpenCamera: () -> Unit,
    onSubmitImage: () -> Unit,
    onSaveBodyScan: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // ── Camera Scan Card ──
        CameraAnalysisCard(
            icon = Icons.Outlined.AccessibilityNew,
            title = stringResource(R.string.track_body_scan_title),
            body = stringResource(R.string.track_body_scan_body),
            primaryAction = stringResource(R.string.track_take_photo),
            state = state,
            onOpenCamera = onOpenCamera,
            onSubmitImage = onSubmitImage
        )

        // ── Save button after analysis ──
        if (state.analysisResult != null && state.selectedTab == TrackTab.Body) {
            if (state.bodyScanSaved) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2E7D32).copy(alpha = 0.08f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.CheckCircle, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(18.dp))
                        Text(
                            stringResource(R.string.track_saved_badge_body),
                            modifier = Modifier.padding(start = 8.dp),
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                Button(
                    onClick = onSaveBodyScan,
                    enabled = !state.isSubmittingImage,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FittyPink),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    if (state.isSubmittingImage) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Icon(Icons.Outlined.CheckCircle, null, modifier = Modifier.size(18.dp))
                        Text(stringResource(R.string.track_save_result), modifier = Modifier.padding(start = 8.dp), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // ── Body Metrics Card ──
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF6C63FF).copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.AccessibilityNew, contentDescription = null, tint = Color(0xFF6C63FF), modifier = Modifier.size(20.dp))
                    }
                    Text(stringResource(R.string.track_body_metrics), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.progressWeight, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.track_weight), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.bmi, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF6C63FF))
                        Text(stringResource(R.string.home_body_metric_bmi), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.targetWeight, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = FittyPink)
                        Text(stringResource(R.string.track_goal), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // ── Tips ──
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8F0)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.track_photo_tips), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("• ${stringResource(R.string.track_tip_lighting)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("• ${stringResource(R.string.track_tip_clothes)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("• ${stringResource(R.string.track_tip_angles)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ProgressTab(state: TrackUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // ── Completion Ring + Overview ──
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular progress
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(90.dp)) {
                    CircularProgressIndicator(
                        progress = { state.completionRate / 100f },
                        modifier = Modifier.size(90.dp),
                        strokeWidth = 8.dp,
                        color = FittyPink,
                        trackColor = FittyPink.copy(alpha = 0.1f)
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${state.completionRate}%", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = FittyPink)
                        Text(stringResource(R.string.track_this_week), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.track_week_progress), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    ProgressMiniRow(Icons.Outlined.FitnessCenter, stringResource(R.string.track_progress_workout), state.progressWorkouts)
                    ProgressMiniRow(Icons.Outlined.Restaurant, stringResource(R.string.track_progress_meal), state.progressMeals)
                    ProgressMiniRow(Icons.Outlined.LocalFireDepartment, stringResource(R.string.track_progress_calories_burned), state.totalCaloriesBurned)
                }
            }
        }

        // ── Weekly Activity ──
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.track_7day_activity), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    val days = listOf(
                        stringResource(R.string.track_day_mon),
                        stringResource(R.string.track_day_tue),
                        stringResource(R.string.track_day_wed),
                        stringResource(R.string.track_day_thu),
                        stringResource(R.string.track_day_fri),
                        stringResource(R.string.track_day_sat),
                        stringResource(R.string.track_day_sun)
                    )
                    state.weeklyActiveDays.forEachIndexed { i, active ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (active) FittyPink else FittyPink.copy(alpha = 0.08f)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (active) {
                                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                            Text(days.getOrElse(i) { "" }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // ── Weight Progress ──
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(FittyPink.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.MonitorWeight, contentDescription = null, tint = FittyPink, modifier = Modifier.size(20.dp))
                    }
                    Text(stringResource(R.string.track_weight), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(stringResource(R.string.track_current), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(state.progressWeight, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(stringResource(R.string.track_goal), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(state.targetWeight, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = FittyPink)
                    }
                }
                LinearProgressIndicator(
                    progress = { state.progressWeightPercent },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = FittyPink,
                    trackColor = FittyPink.copy(alpha = 0.1f)
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.track_bmi_value, state.bmi), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${(state.progressWeightPercent * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = FittyPink, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // ── Workout & Meal Progress Bars ──
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ProgressMiniCard(
                icon = Icons.Outlined.FitnessCenter,
                title = stringResource(R.string.track_progress_workout),
                value = state.progressWorkouts,
                progress = state.progressWorkoutPercent,
                modifier = Modifier.weight(1f)
            )
            ProgressMiniCard(
                icon = Icons.Outlined.Restaurant,
                title = stringResource(R.string.track_progress_meal),
                value = state.progressMeals,
                progress = state.progressMealPercent,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ProgressMiniRow(icon: ImageVector, label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = FittyPink, modifier = Modifier.size(14.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ProgressMiniCard(icon: ImageVector, title: String, value: String, progress: Float, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(FittyPink.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = FittyPink, modifier = Modifier.size(16.dp))
            }
            Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = FittyPink,
                trackColor = FittyPink.copy(alpha = 0.1f)
            )
            Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = FittyPink)
        }
    }
}

@Composable
private fun StatsTab(state: TrackUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // ── Overview tiles ──
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(state.statWorkouts, stringResource(R.string.track_stat_workouts), Icons.Outlined.FitnessCenter, Color(0xFF6C63FF), Modifier.weight(1f))
            StatTile(state.statMeals, stringResource(R.string.track_stat_meals), Icons.Outlined.Restaurant, Color(0xFFFF6B9D), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(state.statStreak, stringResource(R.string.track_stat_best_streak), Icons.Outlined.LocalFireDepartment, Color(0xFFFF9F43), Modifier.weight(1f))
            StatTile(state.statActiveMin, stringResource(R.string.track_stat_active_minutes), Icons.Outlined.BarChart, Color(0xFF2ED573), Modifier.weight(1f))
        }

        // ── Nutrition breakdown ──
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFFFF6B9D).copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Restaurant, contentDescription = null, tint = Color(0xFFFF6B9D), modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text(stringResource(R.string.track_avg_nutrition), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.track_per_day, state.statAvgCalories), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    MacroStatPill(stringResource(R.string.track_row_protein), state.statProteinAvg, Color(0xFF6C63FF))
                    MacroStatPill(stringResource(R.string.track_row_carbs), state.statCarbsAvg, Color(0xFFFF9F43))
                    MacroStatPill(stringResource(R.string.track_row_fat), state.statFatAvg, Color(0xFF2ED573))
                }
            }
        }

        // ── Avg daily calories ──
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.track_overview_activity), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(stringResource(R.string.track_burned_calories), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(state.totalCaloriesBurned, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color(0xFFFF6B9D))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(stringResource(R.string.track_avg_per_day), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(state.avgDailyCalories, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color(0xFF6C63FF))
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraAnalysisCard(
    icon: ImageVector,
    title: String,
    body: String,
    primaryAction: String,
    state: TrackUiState,
    onOpenCamera: () -> Unit,
    onSubmitImage: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(FittyPink.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = FittyPink, modifier = Modifier.size(22.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            state.capturedImageUri?.let { imageUri ->
                AsyncImage(
                    model = imageUri,
                    contentDescription = stringResource(R.string.track_captured_photo),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
            }

            state.captureError?.let { error ->
                Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onOpenCamera, shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f)) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(
                        text = if (state.capturedImageUri == null) primaryAction else stringResource(R.string.track_retake_photo),
                        modifier = Modifier.padding(start = 8.dp),
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Button(
                    onClick = onSubmitImage,
                    enabled = state.capturedImageUri != null && !state.isSubmittingImage,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (state.isSubmittingImage) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Text(stringResource(R.string.track_analyze), fontWeight = FontWeight.Bold)
                    }
                }
            }

            state.analysisResult?.let { result ->
                AnalysisResultCard(result)
            }
        }
    }
}

@Composable
private fun RecoveryHintCard(
    state: TrackUiState,
    onOpenCamera: () -> Unit,
    onSubmitImage: () -> Unit,
    onOpenAppSettings: () -> Unit
) {
    val error = state.captureError ?: return
    val permissionDenied = error.contains("permission", ignoreCase = true) ||
        error.contains("quyền", ignoreCase = true)
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (permissionDenied) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = if (permissionDenied) {
                    stringResource(R.string.track_recovery_permission_title)
                } else {
                    stringResource(R.string.track_recovery_retry_title)
                },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (permissionDenied) {
                    stringResource(R.string.track_recovery_permission_body)
                } else {
                    stringResource(R.string.track_recovery_retry_body)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = if (permissionDenied) onOpenAppSettings else onOpenCamera,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        if (permissionDenied) {
                            stringResource(R.string.track_open_app_settings)
                        } else {
                            stringResource(R.string.track_take_photo)
                        }
                    )
                }
                if (!permissionDenied && state.capturedImageUri != null) {
                    Button(
                        onClick = onSubmitImage,
                        enabled = !state.isSubmittingImage,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.track_retry_analysis))
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalysisResultCard(result: TrackAnalysisResult) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = FittyPink.copy(alpha = 0.07f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(result.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(result.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            result.rows.forEach { row ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(row.label, style = MaterialTheme.typography.bodySmall)
                    Text(row.value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(icon: ImageVector, title: String, value: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(FittyPink.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = FittyPink, modifier = Modifier.size(22.dp))
                }
                Column {
                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            content()
        }
    }
}

@Composable
private fun InfoRowCard(title: String, body: String, icon: ImageVector) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
            }
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun MacroProgress(label: String, progress: Float) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
        }
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)), color = FittyPink, trackColor = FittyPink.copy(alpha = 0.12f))
    }
}

@Composable
private fun StatTile(value: String, label: String, icon: ImageVector, tintColor: Color = FittyPink, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(tintColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tintColor, modifier = Modifier.size(20.dp))
            }
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MacroStatPill(label: String, value: String, color: Color) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.08f))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}


private fun createTrackImageUri(context: Context): Uri {
    val imagesDir = File(context.filesDir, "track_images").apply { mkdirs() }
    val imageFile = File.createTempFile(
        "track_${System.currentTimeMillis()}_",
        ".jpg",
        imagesDir
    )
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        imageFile
    )
}

@Composable
private fun ScanHistoryCard(scan: MealScanHistoryUi) {
    var imageLoadFailed by rememberSaveable(scan.imageUrl) { mutableStateOf(false) }
    val dateText = if (scan.dateKey.isNotBlank()) scan.dateKey else {
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date(scan.timestamp))
    }
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Scan image thumbnail
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(FittyPink.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                if (scan.imageUrl.isNotBlank() && !imageLoadFailed) {
                    AsyncImage(
                        model = scan.imageUrl,
                        contentDescription = stringResource(R.string.track_scan_photo),
                        contentScale = ContentScale.Crop,
                        onError = { imageLoadFailed = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                } else {
                    Icon(
                        Icons.Outlined.CameraAlt,
                        contentDescription = null,
                        tint = FittyPink.copy(alpha = 0.4f),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            // Info
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(R.string.track_kcal_value, scan.calories),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.track_macro_short_protein, scan.protein), style = MaterialTheme.typography.labelSmall, color = FittyPink)
                    Text(stringResource(R.string.track_macro_short_carbs, scan.carbs), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Text(stringResource(R.string.track_macro_short_fat, scan.fat), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                }
                Text(
                    dateText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // Food count badge
            if (scan.foodCount > 0) {
                Box(
                    modifier = Modifier
                        .background(FittyPink.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        stringResource(R.string.track_items_count, scan.foodCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = FittyPink,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun BodyHistorySection(state: TrackUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (state.bodyScanHistory.isNotEmpty()) {
            Text(stringResource(R.string.track_body_history), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            state.bodyScanHistory.forEach { scan ->
                BodyScanHistoryCard(scan)
            }
        } else {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFF6C63FF).copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.AccessibilityNew, contentDescription = null, tint = Color(0xFF6C63FF).copy(alpha = 0.5f), modifier = Modifier.size(24.dp))
                    }
                    Text(stringResource(R.string.track_empty_body_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.track_empty_body_body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun BodyScanHistoryCard(scan: BodyScanHistoryUi) {
    val dateText = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        .format(java.util.Date(scan.timestamp))
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF6C63FF).copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                if (scan.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = scan.imageUrl,
                        contentDescription = stringResource(R.string.track_scan_photo),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                } else {
                    Icon(
                        Icons.Outlined.CameraAlt,
                        contentDescription = null,
                        tint = Color(0xFF6C63FF).copy(alpha = 0.45f),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    scan.summary.ifBlank { stringResource(R.string.track_analysis_complete) },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    scan.bodyFatPercent?.let {
                        Text(
                            stringResource(R.string.track_body_fat_short, String.format(java.util.Locale.US, "%.1f", it)),
                            style = MaterialTheme.typography.labelSmall,
                            color = FittyPink
                        )
                    }
                    scan.postureScore?.let {
                        Text(
                            stringResource(R.string.track_posture_short, it),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF6C63FF)
                        )
                    }
                    Text(
                        stringResource(R.string.track_confidence_short, (scan.confidence * 100).toInt()),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                Text(
                    dateText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .background(FittyPink.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    scan.status.toLocalizedTrackToken(LocalContext.current),
                    style = MaterialTheme.typography.labelSmall,
                    color = FittyPink,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

private fun String.toLocalizedTrackToken(context: Context): String {
    return when (trim().lowercase(java.util.Locale.ROOT)) {
        "breakfast" -> context.getString(R.string.home_meal_breakfast)
        "lunch" -> context.getString(R.string.home_meal_lunch)
        "dinner" -> context.getString(R.string.home_meal_dinner)
        "snack" -> context.getString(R.string.home_meal_snack)
        "other" -> context.getString(R.string.home_meal_type_other)
        "good" -> context.getString(R.string.common_status_good)
        "excellent" -> context.getString(R.string.common_status_excellent)
        "normal" -> context.getString(R.string.common_status_normal)
        "needs_improvement", "needs improvement" -> context.getString(R.string.common_status_needs_improvement)
        "easy" -> context.getString(R.string.common_label_easy)
        "medium" -> context.getString(R.string.common_label_medium)
        "hard" -> context.getString(R.string.common_label_hard)
        "beginner" -> context.getString(R.string.plan_level_beginner)
        "intermediate" -> context.getString(R.string.common_label_intermediate)
        "advanced" -> context.getString(R.string.common_label_advanced)
        else -> split('_', ' ')
            .filter { it.isNotBlank() }
            .joinToString(" ") { part ->
                part.replaceFirstChar { char ->
                    if (char.isLowerCase()) char.titlecase(java.util.Locale.US) else char.toString()
                }
            }
    }
}



