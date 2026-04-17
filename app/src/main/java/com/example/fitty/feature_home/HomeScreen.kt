package com.example.fitty.feature_home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.fitty.core.designsystem.component.FittyInfoCard
import com.example.fitty.core.designsystem.component.FittyPrimaryButton
import com.example.fitty.core.ui.FittyLazyScreen

@Composable
fun HomeScreen() {
    FittyLazyScreen {
        item {
            Column {
                Text(
                    text = "Good morning",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Today is ready",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        item {
            FittyInfoCard(
                title = "Today's target",
                body = "30 min strength + 2,100 kcal target. Keep the first session simple and consistent."
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricBlock("Workout", "0/1", Modifier.weight(1f))
                MetricBlock("Calories", "0", Modifier.weight(1f))
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricBlock("Water", "0/8", Modifier.weight(1f))
                MetricBlock("Steps", "0", Modifier.weight(1f))
            }
        }
        item {
            FittyInfoCard(
                title = "Today's workout",
                body = "Full body beginner. Duration: 30 min. Equipment: bodyweight."
            )
        }
        item {
            FittyInfoCard(
                title = "Meals today",
                body = "No meals logged yet. Start with breakfast or scan your next meal."
            )
        }
        item {
            FittyInfoCard(
                title = "AI insight",
                body = "Log one meal and complete one workout to unlock better daily recommendations."
            )
        }
        item {
            FittyPrimaryButton(text = "Start Today", onClick = { })
        }
    }
}

@Composable
private fun MetricBlock(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = label, style = MaterialTheme.typography.labelMedium)
        Text(text = value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    }
}
