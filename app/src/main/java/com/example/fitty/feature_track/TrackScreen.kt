package com.example.fitty.feature_track

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.fitty.core.ui.FittyLazyScreen

@Composable
fun TrackScreen() {
    var selectedTab by remember { mutableStateOf("Meals") }
    val tabs = listOf("Meals", "Body", "Progress", "Stats")

    FittyLazyScreen {
        item {
            Text(
                text = "Track",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                tabs.forEach { tab ->
                    FilterChip(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        label = { Text(tab) }
                    )
                }
            }
        }
        when (selectedTab) {
            "Meals" -> {
                item { MealsTab() }
            }
            "Body" -> {
                item { BodyTab() }
            }
            "Progress" -> {
                item { ProgressTab() }
            }
            else -> {
                item { StatsTab() }
            }
        }
    }
}

@Composable
private fun MealsTab() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(onClick = { }, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.CameraAlt, contentDescription = null)
            Text("Scan Meal", modifier = Modifier.padding(start = 8.dp))
        }
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
private fun BodyTab() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(onClick = { }, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.AccessibilityNew, contentDescription = null)
            Text("Start Body Scan", modifier = Modifier.padding(start = 8.dp))
        }
        SummaryCard(Icons.Outlined.AccessibilityNew, "Latest AI analysis", "Posture and body metrics will appear after your first scan") {
            Text("Capture front, side, and back photos in good lighting.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        InfoRowCard("Photo history", "No saved assessments yet", Icons.Outlined.Timeline)
    }
}

@Composable
private fun ProgressTab() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SummaryCard(Icons.Outlined.MonitorWeight, "Weight trend", "61.5 kg today") {
            LinearProgressIndicator(progress = { 0.42f }, modifier = Modifier.fillMaxWidth())
            Text("42% toward target weight", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        SummaryCard(Icons.Outlined.BarChart, "Weekly workouts", "2 / 4 completed") {
            LinearProgressIndicator(progress = { 0.5f }, modifier = Modifier.fillMaxWidth())
        }
        SummaryCard(Icons.Outlined.Restaurant, "Calories tracked", "3 meals logged today") {
            LinearProgressIndicator(progress = { 0.58f }, modifier = Modifier.fillMaxWidth())
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
private fun SummaryCard(
    icon: ImageVector,
    title: String,
    value: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            content()
        }
    }
}

@Composable
private fun InfoRowCard(title: String, body: String, icon: ImageVector) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
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
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun StatTile(value: String, label: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
