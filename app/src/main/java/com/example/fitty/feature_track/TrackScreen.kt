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

internal data class TrackUiState(
    val selectedTab: TrackTab? = null,
    val tabs: List<TrackTab> = listOf(TrackTab.Meals, TrackTab.Body, TrackTab.Progress, TrackTab.Stats),
    val capturedImageUri: String? = null,
    val isSubmittingImage: Boolean = false,
    val analysisResult: TrackAnalysisResult? = null,
    val captureError: String? = null,
    val mealConfirmed: Boolean = false,
    val bodyScanSaved: Boolean = false,
    val progressWeight: String = "--",
    val progressWeightPercent: Float = 0f,
    val progressWorkouts: String = "0 / 0",
    val progressWorkoutPercent: Float = 0f,
    val progressMeals: String = "0 meals logged",
    val progressMealPercent: Float = 0f,
    val statWorkouts: String = "0",
    val statMeals: String = "0",
    val statStreak: String = "0 days",
    val statActiveMin: String = "0",
    val mealHistory: List<Pair<String, String>> = emptyList()
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
    private val updateStreakUseCase: com.example.fitty.domain.usecase.user.UpdateStreakUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(TrackUiState())
    internal val uiState: StateFlow<TrackUiState> = _uiState

    private var lastMealResult: com.example.fitty.domain.model.MealAnalysisResult? = null
    private var lastBodyResult: com.example.fitty.domain.model.BodyScanAnalysisResult? = null

    init { loadStats() }

    private fun loadStats() {
        viewModelScope.launch {
            runCatching { getProgressStatsUseCase() }.onSuccess { stats ->
                _uiState.update {
                    it.copy(
                        statWorkouts = stats.totalWorkouts.toString(),
                        statMeals = stats.totalMealsLogged.toString(),
                        statStreak = "${stats.bestStreak} days",
                        statActiveMin = "${stats.totalWorkouts * 30}",
                        progressWeight = stats.latestWeight?.let { w -> "%.1f kg".format(w) } ?: "--",
                        progressWeightPercent = stats.latestWeight?.let { 0.42f } ?: 0f,
                        progressWorkouts = "${stats.dailySummaries.sumOf { s -> s.progress.workoutsCompleted }} total",
                        progressWorkoutPercent = if (stats.totalWorkouts > 0) 0.5f else 0f,
                        progressMeals = "${stats.totalMealsLogged} meals logged",
                        progressMealPercent = if (stats.totalMealsLogged > 0) 0.58f else 0f
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
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingImage = true) }
            confirmMealLogUseCase(meal)
                .onSuccess {
                    _uiState.update { it.copy(isSubmittingImage = false, mealConfirmed = true) }
                    runCatching { updateStreakUseCase("meal") }
                    loadStats()
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
        TrackTab.Meals -> "Track Meal"
        TrackTab.Body -> "Body"
        TrackTab.Progress -> "Progress"
        TrackTab.Stats -> "Stats"
    }
}

private fun trackTabDescription(tab: TrackTab): String {
    return when (tab) {
        TrackTab.Meals -> "Chụp ảnh bữa ăn, xem ảnh vừa chụp và gửi tới API phân tích dinh dưỡng."
        TrackTab.Body -> "Chụp ảnh body progress, xem ảnh và gửi tới API phân tích hình thể."
        TrackTab.Progress -> "Xem tiến độ cân nặng, tập luyện và lượng calo đã theo dõi."
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
            "Choose what you want to track",
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
            Text("Choose another", modifier = Modifier.padding(start = 8.dp), fontWeight = FontWeight.SemiBold)
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
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CameraAnalysisCard(
            icon = Icons.Outlined.CameraAlt,
            title = "Meal photo scan",
            body = "Take a meal photo, preview it here, then send it to the future nutrition API.",
            primaryAction = "Scan Meal",
            state = state,
            onOpenCamera = onOpenCamera,
            onSubmitImage = onSubmitImage
        )
        // Confirm & Log button after analysis
        if (state.analysisResult != null && state.selectedTab == TrackTab.Meals) {
            if (state.mealConfirmed) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2E7D32).copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.CheckCircle, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(20.dp))
                        Text(
                            "Meal logged successfully!",
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
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.isSubmittingImage) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Icon(Icons.Outlined.CheckCircle, null, modifier = Modifier.size(18.dp))
                        Text("Confirm & Log Meal", modifier = Modifier.padding(start = 8.dp), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        SummaryCard(Icons.Outlined.Restaurant, "Daily calories", "${state.progressMeals}") {
            MacroProgress("Protein", state.progressMealPercent)
            MacroProgress("Carbs", state.progressWorkoutPercent)
            MacroProgress("Fat", state.progressWeightPercent)
        }
        if (state.mealHistory.isNotEmpty()) {
            state.mealHistory.forEach { (label, cal) ->
                InfoRowCard(label, cal, Icons.Outlined.Restaurant)
            }
        } else {
            InfoRowCard("Meals", "No meals logged yet", Icons.Outlined.Restaurant)
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
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CameraAnalysisCard(
            icon = Icons.Outlined.AccessibilityNew,
            title = "Body photo scan",
            body = "Capture a body progress photo, preview it here, then send it to the future body analysis API.",
            primaryAction = "Start Body Scan",
            state = state,
            onOpenCamera = onOpenCamera,
            onSubmitImage = onSubmitImage
        )
        // Save Scan button after analysis
        if (state.analysisResult != null && state.selectedTab == TrackTab.Body) {
            if (state.bodyScanSaved) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2E7D32).copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.CheckCircle, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(20.dp))
                        Text(
                            "Body scan saved!",
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
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.isSubmittingImage) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Icon(Icons.Outlined.CheckCircle, null, modifier = Modifier.size(18.dp))
                        Text("Save Body Scan", modifier = Modifier.padding(start = 8.dp), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        SummaryCard(Icons.Outlined.AccessibilityNew, "Latest AI analysis", "Posture and body metrics will appear after your first scan") {
            Text("Capture front, side, and back photos in good lighting.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        InfoRowCard("Photo history", "No saved assessments yet", Icons.Outlined.Timeline)
    }
}

@Composable
private fun ProgressTab(state: TrackUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SummaryCard(Icons.Outlined.MonitorWeight, "Weight trend", state.progressWeight) {
            LinearProgressIndicator(progress = { state.progressWeightPercent }, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)), color = FittyPink, trackColor = FittyPink.copy(alpha = 0.12f))
            Text("${(state.progressWeightPercent * 100).toInt()}% toward target weight", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        SummaryCard(Icons.Outlined.BarChart, "Workouts", state.progressWorkouts) {
            LinearProgressIndicator(progress = { state.progressWorkoutPercent }, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)), color = FittyPink, trackColor = FittyPink.copy(alpha = 0.12f))
        }
        SummaryCard(Icons.Outlined.Restaurant, "Calories tracked", state.progressMeals) {
            LinearProgressIndicator(progress = { state.progressMealPercent }, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)), color = FittyPink, trackColor = FittyPink.copy(alpha = 0.12f))
        }
    }
}

@Composable
private fun StatsTab(state: TrackUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(state.statWorkouts, "Workouts", Icons.Outlined.FitnessCenter, Modifier.weight(1f))
            StatTile(state.statMeals, "Meals", Icons.Outlined.Restaurant, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(state.statStreak, "Best streak", Icons.Outlined.LocalFireDepartment, Modifier.weight(1f))
            StatTile(state.statActiveMin, "Active min", Icons.Outlined.BarChart, Modifier.weight(1f))
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
                        text = if (state.capturedImageUri == null) primaryAction else "Retake",
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
                        Text("Send to API", fontWeight = FontWeight.Bold)
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
private fun StatTile(value: String, label: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(FittyPink.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = FittyPink, modifier = Modifier.size(20.dp))
            }
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
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
