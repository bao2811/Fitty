package com.example.fitty.feature_plan

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
fun PlanScreen() {
    var selectedTab by remember { mutableStateOf("Today") }
    val tabs = listOf("Today", "This Week", "Library")

    FittyLazyScreen {
        item {
            Text(
                text = "My Plan",
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
            "Today" -> {
                item {
                    FittyInfoCard(
                        title = "Full body beginner",
                        body = "30 min - Easy - Bodyweight. Built for consistency and safe movement."
                    )
                }
                item {
                    FittyInfoCard(
                        title = "Why this workout?",
                        body = "It matches a beginner setup, keeps intensity low, and covers major muscle groups."
                    )
                }
                item {
                    FittyPrimaryButton(text = "Start Workout", onClick = { })
                }
            }
            "This Week" -> {
                item { FittyInfoCard("Monday", "Full body") }
                item { FittyInfoCard("Wednesday", "Cardio + core") }
                item { FittyInfoCard("Friday", "Strength") }
                item { FittyInfoCard("Saturday", "Mobility") }
            }
            else -> {
                item { FittyInfoCard("Strength", "Beginner strength workouts") }
                item { FittyInfoCard("Fat loss", "Low-impact cardio and circuits") }
                item { FittyInfoCard("Mobility", "Recovery and flexibility sessions") }
                item { FittyInfoCard("Cardio", "Endurance sessions for all levels") }
            }
        }
    }
}
