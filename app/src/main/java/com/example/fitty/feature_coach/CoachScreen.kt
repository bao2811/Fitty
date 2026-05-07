package com.example.fitty.feature_coach

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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.example.fitty.core.ui.FittyLazyScreen
import com.example.fitty.ui.theme.FittyGradientEnd
import com.example.fitty.ui.theme.FittyGradientStart
import com.example.fitty.ui.theme.FittyPink
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@Composable
fun CoachRoute(viewModel: CoachViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    CoachScreen(
        state = state,
        onPromptSelected = viewModel::selectPrompt,
        onInputChanged = viewModel::updateInput,
        onSend = viewModel::sendMessage
    )
}

internal data class CoachMessage(val sender: String, val body: String)

internal enum class CoachSuggestionType {
    Workout,
    Meal
}

internal data class CoachSuggestionUi(
    val type: CoachSuggestionType,
    val title: String,
    val body: String,
    val action: String
)

internal data class CoachUiState(
    val messages: List<CoachMessage> = listOf(
        CoachMessage(
            sender = "Fitty Coach",
            body = "Tell me what feels hard today and I will adjust your training, meals, or recovery."
        )
    ),
    val prompts: List<String> = listOf(
        "Post-workout meal",
        "Adjust today",
        "I missed a session",
        "Dinner idea"
    ),
    val suggestions: List<CoachSuggestionUi> = listOf(
        CoachSuggestionUi(
            type = CoachSuggestionType.Workout,
            title = "Workout suggestion",
            body = "If your legs feel tired, switch today's strength block to a 20-minute mobility reset.",
            action = "Apply to Plan"
        ),
        CoachSuggestionUi(
            type = CoachSuggestionType.Meal,
            title = "Meal suggestion",
            body = "Add chicken, tofu, eggs, or Greek yogurt at dinner to close your protein gap.",
            action = "Save Meal Idea"
        )
    ),
    val input: String = ""
)

@HiltViewModel
class CoachViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(CoachUiState())
    internal val uiState: StateFlow<CoachUiState> = _uiState

    internal fun selectPrompt(prompt: String) {
        _uiState.update { it.copy(input = prompt) }
    }

    internal fun updateInput(value: String) {
        _uiState.update { it.copy(input = value) }
    }

    internal fun sendMessage() {
        val currentInput = _uiState.value.input.trim()
        if (currentInput.isBlank()) return

        _uiState.update { state ->
            state.copy(
                messages = state.messages + listOf(
                    CoachMessage("You", currentInput),
                    CoachMessage(
                        "Fitty Coach",
                        "Start with one practical action today. I will adjust your plan as you log more data."
                    )
                ),
                input = ""
            )
        }
    }
}

@Composable
private fun CoachScreen(
    state: CoachUiState,
    onPromptSelected: (String) -> Unit,
    onInputChanged: (String) -> Unit,
    onSend: () -> Unit
) {

    FittyLazyScreen {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(48.dp).shadow(8.dp, CircleShape).clip(CircleShape)
                        .background(Brush.linearGradient(listOf(FittyGradientStart, FittyGradientEnd))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Psychology, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
                }
                Column {
                    Text("Fitty Coach", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Training, meals, and recovery support", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                state.prompts.forEach { prompt ->
                    AssistChip(
                        onClick = { onPromptSelected(prompt) },
                        label = { Text(prompt, style = MaterialTheme.typography.labelMedium) },
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            }
        }
        items(state.messages.size) { index -> ChatBubble(state.messages[index]) }
        state.suggestions.forEach { suggestion ->
            item {
                RichSuggestionCard(
                    icon = suggestionIcon(suggestion.type),
                    title = suggestion.title,
                    body = suggestion.body,
                    action = suggestion.action
                )
            }
        }
        item {
            CoachInput(
                input = state.input,
                onInputChanged = onInputChanged,
                onSend = onSend
            )
        }
    }
}

private fun suggestionIcon(type: CoachSuggestionType): ImageVector {
    return when (type) {
        CoachSuggestionType.Workout -> Icons.Outlined.FitnessCenter
        CoachSuggestionType.Meal -> Icons.Outlined.Restaurant
    }
}

@Composable
private fun ChatBubble(message: CoachMessage) {
    val isUser = message.sender == "You"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 20.dp, topEnd = 20.dp,
                bottomStart = if (isUser) 20.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 20.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) FittyPink else MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isUser) 4.dp else 2.dp),
            modifier = Modifier.fillMaxWidth(0.82f)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(message.sender, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold,
                    color = if (isUser) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurface)
                Text(message.body, style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun RichSuggestionCard(icon: ImageVector, title: String, body: String, action: String) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = FittyPink.copy(alpha = 0.06f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(FittyPink.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = FittyPink, modifier = Modifier.size(20.dp))
                }
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            AssistChip(onClick = { }, label = { Text(action, fontWeight = FontWeight.SemiBold) }, shape = RoundedCornerShape(12.dp))
        }
    }
}

@Composable
private fun CoachInput(input: String, onInputChanged: (String) -> Unit, onSend: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { }) { Icon(Icons.Outlined.AttachFile, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            OutlinedTextField(
                value = input, onValueChange = onInputChanged,
                placeholder = { Text("Ask Fitty Coach...", style = MaterialTheme.typography.bodyMedium) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FittyPink, unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant, cursorColor = FittyPink
                ),
                singleLine = true
            )
            IconButton(onClick = { }) { Icon(Icons.Outlined.Mic, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            IconButton(
                onClick = onSend,
                colors = IconButtonDefaults.iconButtonColors(containerColor = FittyPink, contentColor = Color.White),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = null, modifier = Modifier.size(20.dp))
            }
        }
    }
}
