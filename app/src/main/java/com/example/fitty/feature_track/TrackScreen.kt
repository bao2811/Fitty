package com.example.fitty.feature_track

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.fitty.core.designsystem.component.FittyInfoCard
import com.example.fitty.core.designsystem.component.FittyPrimaryButton
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                item { FittyPrimaryButton(text = "Scan Meal", onClick = { }) }
                item { FittyInfoCard("Daily calories", "0 / 2,100 kcal logged today.") }
                item { FittyInfoCard("Meal timeline", "Breakfast, lunch, dinner, and snacks will appear here.") }
            }
            "Body" -> {
                item { FittyPrimaryButton(text = "Start Body Scan", onClick = { }) }
                item { FittyInfoCard("Photo guide", "Capture front, side, and back photos in good lighting.") }
                item { FittyInfoCard("History", "Saved assessments will appear here.") }
            }
            "Progress" -> {
                item { FittyInfoCard("Weight trend", "No data yet.") }
                item { FittyInfoCard("Weekly workouts", "Complete a session to start the chart.") }
                item { FittyInfoCard("Calories tracked", "Log meals to build a trend.") }
            }
            else -> {
                item { FittyInfoCard("Total workouts", "0") }
                item { FittyInfoCard("Best streak", "0 days") }
                item { FittyInfoCard("Meals logged", "0") }
                item { FittyInfoCard("Achievements", "Start your first workout to unlock one.") }
            }
        }
    }
}
