package com.example.fitty.feature_track

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessibilityNew
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CameraAlt
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import kotlinx.coroutines.delay
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
    val selectedTab: TrackTab = TrackTab.Meals,
    val tabs: List<TrackTab> = TrackTab.entries,
    val capturedImageUri: String? = null,
    val isSubmittingImage: Boolean = false,
    val analysisResult: TrackAnalysisResult? = null,
    val captureError: String? = null
)

@Composable
fun TrackRoute(viewModel: TrackViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    TrackScreen(
        state = state,
        onTabSelected = viewModel::selectTab,
        onImageCaptured = viewModel::setCapturedImage,
        onCaptureError = viewModel::setCaptureError,
        onSubmitImage = viewModel::submitCapturedImage
    )
}

@HiltViewModel
class TrackViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(TrackUiState())
    internal val uiState: StateFlow<TrackUiState> = _uiState

    internal fun selectTab(tab: TrackTab) {
        _uiState.update {
            it.copy(
                selectedTab = tab,
                capturedImageUri = null,
                isSubmittingImage = false,
                analysisResult = null,
                captureError = null
            )
        }
    }

    internal fun setCapturedImage(uri: String) {
        _uiState.update {
            it.copy(
                capturedImageUri = uri,
                analysisResult = null,
                captureError = null
            )
        }
    }

    internal fun setCaptureError(message: String) {
        _uiState.update {
            it.copy(captureError = message, isSubmittingImage = false)
        }
    }

    internal fun submitCapturedImage() {
        val current = _uiState.value
        if (current.capturedImageUri.isNullOrBlank()) {
            setCaptureError("Take a photo before sending it to the API.")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingImage = true, captureError = null) }
            delay(700)
            _uiState.update {
                it.copy(
                    isSubmittingImage = false,
                    analysisResult = fakeAnalysisFor(it.selectedTab)
                )
            }
        }
    }
}

@Composable
private fun TrackScreen(
    state: TrackUiState,
    onTabSelected: (TrackTab) -> Unit,
    onImageCaptured: (String) -> Unit,
    onCaptureError: (String) -> Unit,
    onSubmitImage: () -> Unit
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
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                state.tabs.forEach { tab ->
                    FilterChip(
                        selected = state.selectedTab == tab,
                        onClick = { onTabSelected(tab) },
                        label = {
                            Text(
                                trackTabLabel(tab),
                                fontWeight = if (state.selectedTab == tab) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = FittyPink.copy(alpha = 0.12f),
                            selectedLabelColor = FittyPink
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            }
        }
        when (state.selectedTab) {
            TrackTab.Meals -> { item { MealsTab(state = state, onOpenCamera = onOpenCamera, onSubmitImage = onSubmitImage) } }
            TrackTab.Body -> { item { BodyTab(state = state, onOpenCamera = onOpenCamera, onSubmitImage = onSubmitImage) } }
            TrackTab.Progress -> { item { ProgressTab() } }
            else -> { item { StatsTab() } }
        }
    }
}

private fun trackTabLabel(tab: TrackTab): String {
    return when (tab) {
        TrackTab.Meals -> "Meals"
        TrackTab.Body -> "Body"
        TrackTab.Progress -> "Progress"
        TrackTab.Stats -> "Stats"
    }
}

@Composable
private fun MealsTab(
    state: TrackUiState,
    onOpenCamera: () -> Unit,
    onSubmitImage: () -> Unit
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
        SummaryCard(Icons.Outlined.Restaurant, "Daily calories", "1,240 / 2,100 kcal") {
            MacroProgress("Protein", 0.46f)
            MacroProgress("Carbs", 0.58f)
            MacroProgress("Fat", 0.38f)
        }
        InfoRowCard("Breakfast", "Greek yogurt, banana • 420 kcal", Icons.Outlined.Restaurant)
        InfoRowCard("Lunch", "Chicken rice bowl • 610 kcal", Icons.Outlined.Restaurant)
        InfoRowCard("Snack", "Protein bar • 210 kcal", Icons.Outlined.Restaurant)
    }
}

@Composable
private fun BodyTab(
    state: TrackUiState,
    onOpenCamera: () -> Unit,
    onSubmitImage: () -> Unit
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
        SummaryCard(Icons.Outlined.AccessibilityNew, "Latest AI analysis", "Posture and body metrics will appear after your first scan") {
            Text("Capture front, side, and back photos in good lighting.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        InfoRowCard("Photo history", "No saved assessments yet", Icons.Outlined.Timeline)
    }
}

@Composable
private fun ProgressTab() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SummaryCard(Icons.Outlined.MonitorWeight, "Weight trend", "61.5 kg today") {
            LinearProgressIndicator(progress = { 0.42f }, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)), color = FittyPink, trackColor = FittyPink.copy(alpha = 0.12f))
            Text("42% toward target weight", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        SummaryCard(Icons.Outlined.BarChart, "Weekly workouts", "2 / 4 completed") {
            LinearProgressIndicator(progress = { 0.5f }, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)), color = FittyPink, trackColor = FittyPink.copy(alpha = 0.12f))
        }
        SummaryCard(Icons.Outlined.Restaurant, "Calories tracked", "3 meals logged today") {
            LinearProgressIndicator(progress = { 0.58f }, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)), color = FittyPink, trackColor = FittyPink.copy(alpha = 0.12f))
        }
    }
}

@Composable
private fun StatsTab() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("18", "Workouts", Icons.Outlined.FitnessCenter, Modifier.weight(1f))
            StatTile("42", "Meals", Icons.Outlined.Restaurant, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("12 days", "Best streak", Icons.Outlined.LocalFireDepartment, Modifier.weight(1f))
            StatTile("520", "Active min", Icons.Outlined.BarChart, Modifier.weight(1f))
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

private fun fakeAnalysisFor(tab: TrackTab): TrackAnalysisResult {
    return when (tab) {
        TrackTab.Meals -> TrackAnalysisResult(
            title = "Mock meal API response",
            summary = "This is placeholder data. Replace this mapper when the real meal analysis API is available.",
            rows = listOf(
                TrackAnalysisRow("Detected meal", "Chicken rice bowl"),
                TrackAnalysisRow("Calories", "~610 kcal"),
                TrackAnalysisRow("Protein", "42 g"),
                TrackAnalysisRow("Carbs", "68 g"),
                TrackAnalysisRow("Fat", "18 g"),
                TrackAnalysisRow("Confidence", "86%")
            )
        )

        TrackTab.Body -> TrackAnalysisResult(
            title = "Mock body scan API response",
            summary = "This is placeholder data. Replace this mapper when the real body analysis API is available.",
            rows = listOf(
                TrackAnalysisRow("Photo quality", "Good lighting"),
                TrackAnalysisRow("Posture", "Neutral"),
                TrackAnalysisRow("Shoulder balance", "Slight right tilt"),
                TrackAnalysisRow("Progress note", "Use same angle next scan"),
                TrackAnalysisRow("Confidence", "78%")
            )
        )

        TrackTab.Progress,
        TrackTab.Stats -> TrackAnalysisResult(
            title = "No scan result",
            summary = "Photo analysis is only available for Meals and Body tabs.",
            rows = emptyList()
        )
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
