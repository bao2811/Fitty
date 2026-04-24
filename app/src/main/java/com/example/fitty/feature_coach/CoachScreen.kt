package com.example.fitty.feature_coach

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.fitty.core.ui.FittyLazyScreen

@Composable
fun CoachScreen() {
    val messages = remember {
        mutableStateListOf(
            CoachMessage("Fitty Coach", "Tell me what feels hard today and I will adjust your training, meals, or recovery.")
        )
    }
    var input by remember { mutableStateOf("") }
    val prompts = listOf("Post-workout meal", "Adjust today", "I missed a session", "Dinner idea")

    FittyLazyScreen {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Psychology, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Column {
                    Text("Fitty Coach", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Training, meals, and recovery support", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                prompts.forEach { prompt ->
                    AssistChip(
                        onClick = { input = prompt },
                        label = { Text(prompt) }
                    )
                }
            }
        }
        items(messages.size) { index ->
            ChatBubble(messages[index])
        }
        item {
            RichSuggestionCard(
                icon = Icons.Outlined.FitnessCenter,
                title = "Workout suggestion",
                body = "If your legs feel tired, switch today's strength block to a 20-minute mobility reset.",
                action = "Apply to Plan"
            )
        }
        item {
            RichSuggestionCard(
                icon = Icons.Outlined.Restaurant,
                title = "Meal suggestion",
                body = "Add chicken, tofu, eggs, or Greek yogurt at dinner to close your protein gap.",
                action = "Save Meal Idea"
            )
        }
        item {
            CoachInput(
                input = input,
                onInputChanged = { input = it },
                onSend = {
                    if (input.isNotBlank()) {
                        messages.add(CoachMessage("You", input))
                        messages.add(
                            CoachMessage(
                                "Fitty Coach",
                                "Start with one practical action today. I will adjust your plan as you log more data."
                            )
                        )
                        input = ""
                    }
                }
            )
        }
    }
}

private data class CoachMessage(
    val sender: String,
    val body: String
)

@Composable
private fun ChatBubble(message: CoachMessage) {
    val isUser = message.sender == "You"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                }
            ),
            border = if (isUser) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth(0.86f)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(message.sender, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text(message.body, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun RichSuggestionCard(icon: ImageVector, title: String, body: String, action: String) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
            AssistChip(onClick = { }, label = { Text(action) })
        }
    }
}

@Composable
private fun CoachInput(
    input: String,
    onInputChanged: (String) -> Unit,
    onSend: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { }) {
            Icon(Icons.Outlined.AttachFile, contentDescription = null)
        }
        OutlinedTextField(
            value = input,
            onValueChange = onInputChanged,
            label = { Text("Ask Fitty Coach") },
            modifier = Modifier.weight(1f),
            trailingIcon = {
                IconButton(onClick = onSend) {
                    Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = null)
                }
            }
        )
        IconButton(onClick = { }) {
            Icon(Icons.Outlined.Mic, contentDescription = null)
        }
    }
}
