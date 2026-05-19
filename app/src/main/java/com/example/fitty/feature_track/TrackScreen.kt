package com.example.fitty.feature_track

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.example.fitty.core.ui.FittyLazyScreen
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

internal data class TrackUiState(
    val selectedTab: TrackTab? = null,
    val tabs: List<TrackTab> = listOf(TrackTab.Meals, TrackTab.Body, TrackTab.Progress, TrackTab.Stats),
    val capturedImageUri: String? = null,
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
    val progressMeals: String = "0 meals logged",
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
    val statStreak: String = "0 days",
    val statActiveMin: String = "0",
    val statAvgCalories: String = "0 kcal",
    val statProteinAvg: String = "0g",
    val statCarbsAvg: String = "0g",
    val statFatAvg: String = "0g",
    // Scan history
    val mealHistory: List<Pair<String, String>> = emptyList(),
    val scanHistory: List<MealScanHistoryUi> = emptyList()
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
    private val updateStreakUseCase: com.example.fitty.domain.usecase.user.UpdateStreakUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(TrackUiState())
    internal val uiState: StateFlow<TrackUiState> = _uiState

    private var lastMealResult: com.example.fitty.domain.model.MealAnalysisResult? = null
    private var lastBodyResult: com.example.fitty.domain.model.BodyScanAnalysisResult? = null

    init {
        loadStats()
        loadScanHistory()
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

    private fun loadStats() {
        viewModelScope.launch {
            runCatching { getProgressStatsUseCase() }.onSuccess { stats ->
                val totalWorkoutsCompleted = stats.dailySummaries.sumOf { s -> s.progress.workoutsCompleted }
                val totalCalBurned = stats.dailySummaries.sumOf { s -> s.progress.caloriesBurned }
                val avgCal = if (stats.dailySummaries.isNotEmpty()) totalCalBurned / stats.dailySummaries.size else 0
                val weekActive = MutableList(7) { false }
                stats.dailySummaries.takeLast(7).forEachIndexed { i, s ->
                    weekActive[i] = s.progress.workoutsCompleted > 0 || s.progress.mealsLogged > 0
                }
                val activeDays = weekActive.count { it }
                val rate = if (7 > 0) (activeDays * 100) / 7 else 0

                // Macro averages from scan history
                val totalProtein = stats.dailySummaries.sumOf { s -> s.progress.proteinGrams }
                val totalCarbs = stats.dailySummaries.sumOf { s -> s.progress.carbsGrams }
                val totalFat = stats.dailySummaries.sumOf { s -> s.progress.fatGrams }
                val dayCount = stats.dailySummaries.size.coerceAtLeast(1)

                _uiState.update {
                    it.copy(
                        statWorkouts = stats.totalWorkouts.toString(),
                        statMeals = stats.totalMealsLogged.toString(),
                        statStreak = "${stats.bestStreak} days",
                        statActiveMin = "${stats.totalWorkouts * 30}",
                        statAvgCalories = "${avgCal} kcal",
                        statProteinAvg = "${totalProtein / dayCount}g",
                        statCarbsAvg = "${totalCarbs / dayCount}g",
                        statFatAvg = "${totalFat / dayCount}g",
                        progressWeight = stats.latestWeight?.let { w -> "%.1f kg".format(w) } ?: "--",
                        progressWeightPercent = stats.latestWeight?.let { w ->
                            val target = stats.targetWeight ?: w
                            if (target > 0) (w / target).toFloat().coerceIn(0f, 1f) else 0f
                        } ?: 0f,
                        targetWeight = stats.targetWeight?.let { w -> "%.1f kg".format(w) } ?: "--",
                        bmi = stats.bmi?.let { b -> "%.1f".format(b) } ?: "--",
                        progressWorkouts = "$totalWorkoutsCompleted total",
                        progressWorkoutPercent = if (stats.totalWorkouts > 0) (totalWorkoutsCompleted.toFloat() / (stats.totalWorkouts * 1.2f)).coerceIn(0f, 1f) else 0f,
                        progressMeals = "${stats.totalMealsLogged} meals",
                        progressMealPercent = if (stats.totalMealsLogged > 0) (stats.totalMealsLogged.toFloat() / (stats.totalMealsLogged + 5)).coerceIn(0f, 1f) else 0f,
                        totalCaloriesBurned = "$totalCalBurned",
                        avgDailyCalories = "$avgCal",
                        weeklyActiveDays = weekActive,
                        completionRate = rate
                    )
                }
            }
            runCatching { getMealLogsUseCase() }.onSuccess { logs ->
                _uiState.update {
                    it.copy(mealHistory = logs.map { m -> m.mealType.replaceFirstChar { c -> c.uppercase() } to "${m.totalCalories} kcal" })
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
        _uiState.update { it.copy(capturedImageUri = uri, analysisResult = null, captureError = null) }
    }

    internal fun setCaptureError(message: String) {
        _uiState.update { it.copy(captureError = message, isSubmittingImage = false) }
    }

    internal fun submitCapturedImage() {
        val current = _uiState.value
        val selectedTab = current.selectedTab
        if (selectedTab == null) { setCaptureError("Choose Track Meal or Body first."); return }
        if (current.capturedImageUri.isNullOrBlank()) { setCaptureError("Take a photo first."); return }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingImage = true, captureError = null) }
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
        title = "Meal Analysis",
        summary = "Detected ${mealLog.foodItems.size} food items (${(confidence * 100).toInt()}% confidence)",
        rows = listOf(
            TrackAnalysisRow("Calories", "${mealLog.totalCalories} kcal"),
            TrackAnalysisRow("Protein", "${mealLog.totalProtein} g"),
            TrackAnalysisRow("Carbs", "${mealLog.totalCarbs} g"),
            TrackAnalysisRow("Fat", "${mealLog.totalFat} g")
        ) + mealLog.foodItems.map { TrackAnalysisRow(it.name, "${it.calories} kcal") }
    )

    private fun com.example.fitty.domain.model.BodyScanAnalysisResult.toUi() = TrackAnalysisResult(
        title = "Body Scan Analysis",
        summary = bodyScan.summary.ifBlank { "Analysis complete" } + " (${(confidence * 100).toInt()}% confidence)",
        rows = listOfNotNull(
            bodyScan.estimatedBodyFatPercent?.let { TrackAnalysisRow("Body fat", "%.1f%%".format(it)) },
            bodyScan.postureScore?.let { TrackAnalysisRow("Posture score", "$it / 100") },
            TrackAnalysisRow("Status", bodyScan.status)
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
                    runCatching { updateStreakUseCase("meal") }
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
            onCaptureError("Camera capture was cancelled.")
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
            onCaptureError("Camera permission denied. Enable camera access to scan photos.")
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

    FittyLazyScreen {
        item {
            Text("Track", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
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
                TrackTab.Meals -> { item { MealsTab(state = state, onOpenCamera = onOpenCamera, onSubmitImage = onSubmitImage, onConfirmMeal = onConfirmMeal) } }
                TrackTab.Body -> { item { BodyTab(state = state, onOpenCamera = onOpenCamera, onSubmitImage = onSubmitImage, onSaveBodyScan = onSaveBodyScan) } }
                TrackTab.Progress -> { item { ProgressTab(state) } }
                TrackTab.Stats -> { item { StatsTab(state) } }
            }
        }
    }
}

private fun trackTabLabel(tab: TrackTab): String {
    return when (tab) {
        TrackTab.Meals -> "Bữa ăn"
        TrackTab.Body -> "Hình thể"
        TrackTab.Progress -> "Tiến độ"
        TrackTab.Stats -> "Thống kê"
    }
}

private fun trackTabDescription(tab: TrackTab): String {
    return when (tab) {
        TrackTab.Meals -> "Chụp ảnh bữa ăn để AI phân tích dinh dưỡng."
        TrackTab.Body -> "Chụp ảnh toàn thân để AI phân tích hình thể."
        TrackTab.Progress -> "Xem tiến độ cân nặng, tập luyện và dinh dưỡng."
        TrackTab.Stats -> "Xem thống kê tổng hợp."
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
            "Chọn nội dung theo dõi",
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
                Text(trackTabLabel(tab), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(trackTabDescription(tab), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            Text("Chọn mục khác", modifier = Modifier.padding(start = 8.dp), fontWeight = FontWeight.SemiBold)
        }
        Text(trackTabLabel(tab), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
            title = "Quét bữa ăn",
            body = "Chụp ảnh bữa ăn để phân tích dinh dưỡng bằng AI.",
            primaryAction = "Chụp ảnh",
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
                            "Đã lưu bữa ăn thành công!",
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
                        Text("Xác nhận & lưu", modifier = Modifier.padding(start = 8.dp), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // ── Daily Summary Card ──
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
                        Text("Tổng hôm nay", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(state.progressMeals, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    MacroStatPill("Protein", state.statProteinAvg, Color(0xFF6C63FF))
                    MacroStatPill("Carbs", state.statCarbsAvg, Color(0xFFFF9F43))
                    MacroStatPill("Fat", state.statFatAvg, Color(0xFF2ED573))
                }
            }
        }

        // ── Scan History ──
        if (state.scanHistory.isNotEmpty()) {
            Text("Lịch sử quét", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            state.scanHistory.forEach { scan ->
                ScanHistoryCard(scan)
            }
        }

        // ── Meal Log History ──
        if (state.mealHistory.isNotEmpty()) {
            Text("Nhật ký bữa ăn", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            state.mealHistory.forEach { (label, cal) ->
                InfoRowCard(label, cal, Icons.Outlined.Restaurant)
            }
        } else if (state.scanHistory.isEmpty()) {
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
                    Text("Chưa có bữa ăn nào", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text("Chụp ảnh bữa ăn để bắt đầu theo dõi dinh dưỡng.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            title = "Quét hình thể",
            body = "Chụp ảnh toàn thân để AI phân tích chỉ số cơ thể.",
            primaryAction = "Chụp ảnh",
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
                            "Đã lưu kết quả phân tích!",
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
                        Text("Lưu kết quả", modifier = Modifier.padding(start = 8.dp), fontWeight = FontWeight.Bold)
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
                    Text("Chỉ số cơ thể", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.progressWeight, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Cân nặng", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.bmi, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF6C63FF))
                        Text("BMI", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.targetWeight, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = FittyPink)
                        Text("Mục tiêu", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                Text("💡 Mẹo chụp ảnh", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("• Chụp ở nơi có ánh sáng tốt", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("• Mặc quần áo ôm sát", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("• Chụp cả 3 góc: trước, ngang, sau", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        Text("tuần này", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                    Text("Tiến độ tuần", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    ProgressMiniRow(Icons.Outlined.FitnessCenter, "Tập luyện", state.progressWorkouts)
                    ProgressMiniRow(Icons.Outlined.Restaurant, "Bữa ăn", state.progressMeals)
                    ProgressMiniRow(Icons.Outlined.LocalFireDepartment, "Calo đốt", state.totalCaloriesBurned)
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
                Text("Hoạt động 7 ngày", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    val days = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
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
                    Text("Cân nặng", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Hiện tại", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(state.progressWeight, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Mục tiêu", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    Text("BMI: ${state.bmi}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${(state.progressWeightPercent * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = FittyPink, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // ── Workout & Meal Progress Bars ──
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ProgressMiniCard(
                icon = Icons.Outlined.FitnessCenter,
                title = "Tập luyện",
                value = state.progressWorkouts,
                progress = state.progressWorkoutPercent,
                modifier = Modifier.weight(1f)
            )
            ProgressMiniCard(
                icon = Icons.Outlined.Restaurant,
                title = "Dinh dưỡng",
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
            StatTile(state.statWorkouts, "Buổi tập", Icons.Outlined.FitnessCenter, Color(0xFF6C63FF), Modifier.weight(1f))
            StatTile(state.statMeals, "Bữa ăn", Icons.Outlined.Restaurant, Color(0xFFFF6B9D), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(state.statStreak, "Chuỗi tốt nhất", Icons.Outlined.LocalFireDepartment, Color(0xFFFF9F43), Modifier.weight(1f))
            StatTile(state.statActiveMin, "Phút hoạt động", Icons.Outlined.BarChart, Color(0xFF2ED573), Modifier.weight(1f))
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
                        Text("Dinh dưỡng trung bình", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(state.statAvgCalories + " / ngày", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    MacroStatPill("Protein", state.statProteinAvg, Color(0xFF6C63FF))
                    MacroStatPill("Carbs", state.statCarbsAvg, Color(0xFFFF9F43))
                    MacroStatPill("Fat", state.statFatAvg, Color(0xFF2ED573))
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
                Text("Tổng quan hoạt động", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Calo đốt", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(state.totalCaloriesBurned, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color(0xFFFF6B9D))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Trung bình/ngày", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    contentDescription = "Captured Track photo",
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
                        text = if (state.capturedImageUri == null) primaryAction else "Chụp lại",
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
                        Text("Phân tích", fontWeight = FontWeight.Bold)
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
    val imagesDir = File(context.cacheDir, "track_images").apply { mkdirs() }
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
                if (scan.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = scan.imageUrl,
                        contentDescription = "Scan photo",
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
                        tint = FittyPink.copy(alpha = 0.4f),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            // Info
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "${scan.calories} kcal",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("P: ${scan.protein}g", style = MaterialTheme.typography.labelSmall, color = FittyPink)
                    Text("C: ${scan.carbs}g", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Text("F: ${scan.fat}g", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
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
                        "${scan.foodCount} items",
                        style = MaterialTheme.typography.labelSmall,
                        color = FittyPink,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
