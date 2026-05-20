package com.example.fitty.feature_coach

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitty.domain.model.CoachSuggestion
import com.example.fitty.domain.usecase.coach.ApplyCoachSuggestionUseCase
import com.example.fitty.domain.usecase.coach.SendCoachMessageUseCase
import com.example.fitty.ui.theme.FittyGradientEnd
import com.example.fitty.ui.theme.FittyGradientStart
import com.example.fitty.ui.theme.FittyPink
import com.example.fitty.ui.theme.FittyPinkLight
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
            body = "Chào bạn! Tôi là Fitty Coach — trợ lý AI của bạn về tập luyện, dinh dưỡng và hồi phục. Hãy hỏi tôi bất cứ điều gì! 💪"
        )
    ),
    val prompts: List<String> = listOf(
        "Bữa sau tập",
        "Điều chỉnh hôm nay",
        "Tôi bỏ buổi tập",
        "Ý tưởng bữa tối"
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
                    val displayError = e.message.toCoachErrorMessage()
                    _uiState.update { state ->
                        state.copy(
                            messages = state.messages + CoachMessageUi(
                                "Fitty Coach",
                                displayError
                            ),
                            isSending = false,
                            error = displayError
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
                                "Done! \"${suggestion.title}\" has been applied. ✅"
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

private fun String?.toCoachErrorMessage(): String {
    val message = this.orEmpty()
    return when {
        message.contains("quota exceeded", ignoreCase = true) || message.contains("429", ignoreCase = true) ->
            "⚠️ Gemini API đã hết quota. Vui lòng kiểm tra billing hoặc đổi API key."
        message.contains("api key", ignoreCase = true) && message.contains("invalid", ignoreCase = true) ->
            "🔑 Gemini API key không hợp lệ hoặc chưa được cấp quyền."
        message.contains("not signed in", ignoreCase = true) ->
            "🔒 Bạn cần đăng nhập để dùng Fitty Coach."
        message.isBlank() ->
            "❌ Không thể kết nối Gemini lúc này. Thử lại sau."
        else -> message
    }
}

// ─── UI Composables ──────────────────────────────────────────────────────────

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
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Header ───────────────────────────────────────────────
        CoachHeader()

        // ── Error Banner ─────────────────────────────────────────
        state.error?.let { errorMsg ->
            ErrorBanner(errorMsg)
        }

        // ── Quick Prompts ────────────────────────────────────────
        PromptRow(prompts = state.prompts, onPromptSelected = onPromptSelected)

        // ── Messages ─────────────────────────────────────────────
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(state.messages) { message ->
                ChatBubble(message)
                message.suggestions.forEach { suggestion ->
                    if (suggestion.type != CoachSuggestionType.General || suggestion.body.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        SuggestionCard(
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
                item { TypingIndicator() }
            }
        }

        // ── Input Bar ────────────────────────────────────────────
        ChatInputBar(
            input = state.input,
            onInputChanged = onInputChanged,
            onSend = onSend,
            isSending = state.isSending
        )
    }
}

@Composable
private fun CoachHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    listOf(FittyGradientStart, FittyGradientEnd, FittyPink.copy(alpha = 0.85f))
                )
            )
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // AI Avatar
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .shadow(8.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.SmartToy,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            Column {
                Text(
                    "Fitty Coach",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4ADE80))
                    )
                    Text(
                        "Online • AI-powered",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorBanner(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFFFFF3CD),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = Color(0xFFB45309),
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF92400E),
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PromptRow(prompts: List<String>, onPromptSelected: (String) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        prompts.forEach { prompt ->
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onPromptSelected(prompt) },
                color = FittyPink.copy(alpha = 0.08f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = prompt,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = FittyPink,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ChatBubble(message: CoachMessageUi) {
    val isUser = message.sender == "You"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // Sender label
        Text(
            text = message.sender,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (isUser) FittyPink else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )

        // Bubble
        Box(
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            if (isUser) {
                // User bubble — gradient pink
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(
                            RoundedCornerShape(
                                topStart = 20.dp, topEnd = 20.dp,
                                bottomStart = 20.dp, bottomEnd = 6.dp
                            )
                        )
                        .background(
                            Brush.horizontalGradient(
                                listOf(FittyPink, FittyGradientEnd)
                            )
                        )
                        .padding(14.dp)
                ) {
                    Text(
                        message.body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        lineHeight = 20.sp
                    )
                }
            } else {
                // Coach bubble — elevated card
                Card(
                    shape = RoundedCornerShape(
                        topStart = 6.dp, topEnd = 20.dp,
                        bottomStart = 20.dp, bottomEnd = 20.dp
                    ),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Text(
                            message.body,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TypingIndicator() {
    val transition = rememberInfiniteTransition(label = "typing")
    val dot1 = transition.animateFloat(
        initialValue = 0f, targetValue = -6f,
        animationSpec = infiniteRepeatable(tween(400), RepeatMode.Reverse),
        label = "dot1"
    )
    val dot2 = transition.animateFloat(
        initialValue = 0f, targetValue = -6f,
        animationSpec = infiniteRepeatable(tween(400, delayMillis = 100), RepeatMode.Reverse),
        label = "dot2"
    )
    val dot3 = transition.animateFloat(
        initialValue = 0f, targetValue = -6f,
        animationSpec = infiniteRepeatable(tween(400, delayMillis = 200), RepeatMode.Reverse),
        label = "dot3"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Card(
            shape = RoundedCornerShape(topStart = 6.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(dot1, dot2, dot3).forEach { anim ->
                    Box(
                        modifier = Modifier
                            .offset(y = anim.value.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(FittyPink.copy(alpha = 0.6f))
                    )
                }
            }
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
private fun SuggestionCard(icon: ImageVector, title: String, body: String, action: String, onAction: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .padding(start = 4.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(FittyPink.copy(alpha = 0.15f), FittyPinkLight.copy(alpha = 0.08f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = FittyPink, modifier = Modifier.size(20.dp))
                }
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            if (body.isNotBlank()) {
                Text(
                    body,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    lineHeight = 18.sp
                )
            }
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FittyPink),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text(action, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun ChatInputBar(input: String, onInputChanged: (String) -> Unit, onSend: () -> Unit, isSending: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = onInputChanged,
                placeholder = {
                    Text(
                        "Hỏi Fitty Coach...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FittyPink,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    cursorColor = FittyPink,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                ),
                singleLine = true,
                enabled = !isSending
            )
            // Send button
            IconButton(
                onClick = onSend,
                enabled = input.isNotBlank() && !isSending,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (input.isNotBlank() && !isSending) FittyPink else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (input.isNotBlank() && !isSending) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                ),
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = FittyPink
                    )
                } else {
                    Icon(
                        Icons.AutoMirrored.Outlined.Send,
                        contentDescription = "Send",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
