package com.example.fitty.feature_auth

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitty.core.designsystem.component.FittyPrimaryButton
import com.example.fitty.core.designsystem.component.FittySecondaryButton
import com.example.fitty.core.ui.FittyLazyScreen
import com.example.fitty.domain.usecase.auth.SendPasswordResetUseCase
import com.example.fitty.ui.theme.FittyGradientEnd
import com.example.fitty.ui.theme.FittyGradientStart
import com.example.fitty.ui.theme.FittyPink
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ForgotPasswordUiState(
    val email: String = "",
    val emailError: String? = null,
    val errorMessage: String? = null,
    val isSubmitting: Boolean = false,
    val emailSent: Boolean = false
)

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val sendPasswordResetUseCase: SendPasswordResetUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState

    fun onEmailChanged(value: String) {
        _uiState.update { it.copy(email = value, emailError = null, errorMessage = null) }
    }

    fun submit() {
        val current = _uiState.value
        if (current.email.isBlank() || "@" !in current.email) {
            _uiState.update { it.copy(emailError = "Enter a valid email address") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            val result = sendPasswordResetUseCase(current.email)
            if (result.isSuccess) {
                _uiState.update { it.copy(isSubmitting = false, emailSent = true) }
            } else {
                _uiState.update { it.copy(isSubmitting = false, errorMessage = result.exceptionOrNull()?.message ?: "Failed to send reset email") }
            }
        }
    }
}

@Composable
fun ForgotPasswordRoute(
    onBack: () -> Unit,
    viewModel: ForgotPasswordViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    ForgotPasswordScreen(
        state = state,
        onBack = onBack,
        onEmailChanged = viewModel::onEmailChanged,
        onSubmit = viewModel::submit
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ForgotPasswordScreen(
    state: ForgotPasswordUiState,
    onBack: () -> Unit,
    onEmailChanged: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reset Password", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        FittyLazyScreen {
            item { Spacer(modifier = Modifier.height(padding.calculateTopPadding())) }
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().height(100.dp)
                        .background(Brush.horizontalGradient(listOf(FittyGradientStart, FittyGradientEnd)), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Forgot your password?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("We'll send you a reset link", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                    }
                }
            }

            if (state.emailSent) {
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                    ) {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = FittyPink,
                            modifier = Modifier.padding(bottom = 12.dp))
                        Text("Reset email sent!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Check your inbox at ${state.email} and follow the link to reset your password.",
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp))
                    }
                }
                item {
                    FittySecondaryButton(text = "Back to Sign In", onClick = onBack)
                }
            } else {
                item {
                    OutlinedTextField(
                        value = state.email, onValueChange = onEmailChanged,
                        label = { Text("Email address") },
                        leadingIcon = { Icon(Icons.Outlined.AlternateEmail, contentDescription = null) },
                        isError = state.emailError != null,
                        supportingText = { state.emailError?.let { Text(it) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = FittyPink, focusedLabelColor = FittyPink, cursorColor = FittyPink),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item { state.errorMessage?.let { Text(text = it, color = MaterialTheme.colorScheme.error) } }
                item { FittyPrimaryButton(text = "Send Reset Link", onClick = onSubmit, loading = state.isSubmitting) }
                item { FittySecondaryButton(text = "Back to Sign In", onClick = onBack, enabled = !state.isSubmitting) }
            }
        }
    }
}
