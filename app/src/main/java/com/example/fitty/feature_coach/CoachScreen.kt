package com.example.fitty.feature_coach

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.viewModelScope
import com.example.fitty.domain.model.CoachSuggestion
import com.example.fitty.domain.usecase.coach.ApplyCoachSuggestionUseCase
import com.example.fitty.domain.usecase.coach.SendCoachMessageUseCase
import com.example.fitty.ui.theme.FittyGradientEnd
import com.example.fitty.ui.theme.FittyGradientStart
import com.example.fitty.ui.theme.FittyPink
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Composable
fun CoachRoute(viewModel: CoachViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    CoachScreen(
        state = state,
        onPromptSelected = viewModel::selectPrompt,
        onInputChanged = viewModel::updateInput,
        onSend = viewModel::sendMessage,
        onApplySuggestion = viewModel::applySuggestion
    )
}

internal data class CoachMessageUi(val sender: String, val body: String, val suggestions: List<CoachSuggestionUi> = emptyList())

internal enum class CoachSuggestionType { Workout, Meal, General }

internal data class CoachSuggestionUi(
    val type: CoachSuggestionType,
    val title: String,
    val body: String,
    val action: String,
    val domainSuggestion: CoachSuggestion? = null
)

internal data class CoachUiState(
    val messages: List<CoachMessageUi> = listOf(
        CoachMessageUi(
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
    val input: String = "",
    val isSending: Boolean = false,
    val isApplying: Boolean = false,
    val error: String? = null,
    val threadId: String? = null
)

@HiltViewModel
class CoachViewModel @Inject constructor(
    private val sendCoachMessageUseCase: SendCoachMessageUseCase,
    private val applyCoachSuggestionUseCase: ApplyCoachSuggestionUseCase
) : ViewModel() {
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
        if (currentInput.isBlank() || _uiState.value.isSending) return

        _uiState.update { state ->
            state.copy(
                messages = state.messages + CoachMessageUi("You", currentInput),
                input = "",
                isSending = true,
                error = null
            )
        }

        viewModelScope.launch {
            sendCoachMessageUseCase(currentInput, _uiState.value.threadId)
                .onSuccess { (_, aiResponse) ->
                    val suggestions = aiResponse.suggestions.map { it.toUi() }
                    _uiState.update { state ->
                        state.copy(
                            messages = state.messages + CoachMessageUi(
                                sender = "Fitty Coach",
                                body = aiResponse.text,
                                suggestions = suggestions
                            ),
                            isSending = false,
                            threadId = aiResponse.threadId.ifBlank { state.threadId }
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { state ->
                        state.copy(
                            messages = state.messages + CoachMessageUi(
                                "Fitty Coach",
                                "Sorry, I couldn't process that. Please try again."
                            ),
                            isSending = false,
                            error = e.message
                        )
                    }
                }
        }
    }

    internal fun applySuggestion(suggestion: CoachSuggestionUi) {
        val domainSuggestion = suggestion.domainSuggestion ?: return
        if (_uiState.value.isApplying) return

        _uiState.update { it.copy(isApplying = true, error = null) }

        viewModelScope.launch {
            applyCoachSuggestionUseCase(domainSuggestion)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            messages = state.messages + CoachMessageUi(
                                "Fitty Coach",
                                "Done! \"${suggestion.title}\" has been applied."
                            ),
                            isApplying = false
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isApplying = false, error = e.message) }
                }
        }
    }

    private fun CoachSuggestion.toUi(): CoachSuggestionUi = when (this) {
        is CoachSuggestion.PlanAdjustment -> CoachSuggestionUi(
            type = CoachSuggestionType.Workout, title = title, body = "Reschedule workout from $moveFromDate to $moveToDate",
            action = actionLabel, domainSuggestion = this
        )
        is CoachSuggestion.MealIdea -> CoachSuggestionUi(
            type = CoachSuggestionType.Meal, title = title, body = "$description (~${estimatedCalories} kcal, ${estimatedProtein}g protein)",
            action = actionLabel, domainSuggestion = this
        )
        is CoachSuggestion.General -> CoachSuggestionUi(
            type = CoachSuggestionType.General, title = title, body = "", action = actionLabel, domainSuggestion = this
        )
    }
}

@Composable
private fun CoachScreen(
    state: CoachUiState,
    onPromptSelected: (String) -> Unit,
    onInputChanged: (String) -> Unit,
    onSend: () -> Unit,
    onApplySuggestion: (CoachSuggestionUi) -> Unit
) {
    val listState = rememberLazyListState()
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.size - 1)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        CoachHeader()
        PromptRow(prompts = state.prompts, onPromptSelected = onPromptSelected)
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(state.messages) { message ->
                ChatBubble(message)
                message.suggestions.forEach { suggestion ->
                    if (suggestion.type != CoachSuggestionType.General || suggestion.body.isNotBlank()) {
                        RichSuggestionCard(
                            icon = suggestionIcon(suggestion.type),
                            title = suggestion.title,
                            body = suggestion.body,
                            action = suggestion.action,
                            onAction = { onApplySuggestion(suggestion) }
                        )
                    }
                }
            }
            if (state.isSending) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth(0.5f)
                        ) {
                            Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = FittyPink)
                                Text("Thinking...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
        CoachInput(
            input = state.input,
            onInputChanged = onInputChanged,
            onSend = onSend,
            isSending = state.isSending
        )
    }
}

@Composable
private fun CoachHeader() {
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

@Composable
private fun PromptRow(
    prompts: List<String>,
    onPromptSelected: (String) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState())
    ) {
        prompts.forEach { prompt ->
            AssistChip(
                onClick = { onPromptSelected(prompt) },
                label = { Text(prompt, style = MaterialTheme.typography.labelMedium) },
                shape = RoundedCornerShape(14.dp)
            )
        }
    }
}

private fun suggestionIcon(type: CoachSuggestionType): ImageVector {
    return when (type) {
        CoachSuggestionType.Workout -> Icons.Outlined.FitnessCenter
        CoachSuggestionType.Meal -> Icons.Outlined.Restaurant
        CoachSuggestionType.General -> Icons.Outlined.Psychology
    }
}

@Composable
private fun ChatBubble(message: CoachMessageUi) {
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
private fun RichSuggestionCard(icon: ImageVector, title: String, body: String, action: String, onAction: () -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(FittyPink.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = FittyPink, modifier = Modifier.size(20.dp))
                }
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            if (body.isNotBlank()) {
                Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            }
            AssistChip(onClick = onAction, label = { Text(action, fontWeight = FontWeight.SemiBold) }, shape = RoundedCornerShape(12.dp))
        }
    }
}

@Composable
private fun CoachInput(input: String, onInputChanged: (String) -> Unit, onSend: () -> Unit, isSending: Boolean = false) {
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
            // Mic disabled in v1 scope
            IconButton(onClick = { }, enabled = false) {
                Icon(Icons.Outlined.Mic, contentDescription = "Voice input (coming soon)", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f))
            }
            OutlinedTextField(
                value = input, onValueChange = onInputChanged,
                placeholder = { Text("Ask Fitty Coach...", style = MaterialTheme.typography.bodyMedium) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FittyPink, unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant, cursorColor = FittyPink
                ),
                singleLine = true,
                enabled = !isSending
            )
            IconButton(
                onClick = onSend,
                enabled = input.isNotBlank() && !isSending,
                colors = IconButtonDefaults.iconButtonColors(containerColor = FittyPink, contentColor = Color.White),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = null, modifier = Modifier.size(20.dp))
            }
        }
    }
}
