package com.example.fitty.feature_coach

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.fitty.core.designsystem.component.FittyInfoCard
import com.example.fitty.core.designsystem.component.FittyPrimaryButton
import com.example.fitty.core.ui.FittyLazyScreen

@Composable
fun CoachScreen() {
    val messages = remember { mutableStateListOf("Coach: Ask me about training, meals, or recovery.") }
    var input by remember { mutableStateOf("") }
    val prompts = listOf("Post-workout meal", "Adjust today", "I missed a session")

    FittyLazyScreen {
        item {
            Text(
                text = "Fitty Coach",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                prompts.forEach { prompt ->
                    FilterChip(
                        selected = false,
                        onClick = { input = prompt },
                        label = { Text(prompt) }
                    )
                }
            }
        }
        items(messages.size) { index ->
            FittyInfoCard(
                title = if (messages[index].startsWith("You:")) "You" else "Fitty Coach",
                body = messages[index].substringAfter(": ")
            )
        }
        item {
            FittyInfoCard(
                title = "Action card preview",
                body = "Future replies can include Apply to Plan, Save Meal Suggestion, and Grocery List actions."
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("Ask Fitty Coach") },
                    modifier = Modifier.fillMaxWidth()
                )
                FittyPrimaryButton(
                    text = "Send",
                    onClick = {
                        if (input.isNotBlank()) {
                            messages.add("You: $input")
                            messages.add("Coach: Start with one practical action today, then I will adjust your plan as you log more data.")
                            input = ""
                        }
                    }
                )
            }
        }
    }
}
