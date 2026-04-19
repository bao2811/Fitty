package com.example.fitty.feature_auth

import android.app.Application
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fitty.core.designsystem.component.FittyPrimaryButton
import com.example.fitty.core.designsystem.component.FittySecondaryButton
import com.example.fitty.core.ui.FittyLazyScreen
import com.example.fitty.data.local.FittyLocalRepository
import com.example.fitty.data.preferences.AppPreferencesDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SignUpUiState(
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val formError: String? = null,
    val isSubmitting: Boolean = false
)

class SignUpViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = AppPreferencesDataSource(application.applicationContext)
    private val repository = FittyLocalRepository(application.applicationContext)
    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = _uiState

    fun update(transform: SignUpUiState.() -> SignUpUiState) {
        _uiState.update { it.transform().copy(formError = null) }
    }

    fun submit(onSuccess: () -> Unit) {
        val error = validate(_uiState.value)
        if (error != null) {
            _uiState.update { it.copy(formError = error) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            val result = repository.createPasswordUser(
                username = _uiState.value.username,
                email = _uiState.value.email,
                password = _uiState.value.password
            )
            if (result.user == null) {
                _uiState.update { it.copy(isSubmitting = false, formError = result.errorMessage) }
                return@launch
            }
            preferences.setCurrentUserId(result.user.id)
            preferences.setSignedIn(true)
            preferences.setGuestModeEnabled(false)
            _uiState.update { it.copy(isSubmitting = false) }
            onSuccess()
        }
    }

    fun continueWithGoogle(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, formError = null) }
            val result = repository.continueWithGoogleDemo()
            if (result.user == null) {
                _uiState.update { it.copy(isSubmitting = false, formError = result.errorMessage) }
                return@launch
            }
            preferences.setCurrentUserId(result.user.id)
            preferences.setSignedIn(true)
            preferences.setGuestModeEnabled(false)
            _uiState.update { it.copy(isSubmitting = false) }
            onSuccess()
        }
    }

    private fun validate(state: SignUpUiState): String? = when {
        state.username.isBlank() -> "Username is required"
        state.username.length < 3 -> "Username must be at least 3 characters"
        state.email.isBlank() || "@" !in state.email -> "Enter a valid email"
        state.password.length < 6 -> "Password must be at least 6 characters"
        state.password != state.confirmPassword -> "Passwords do not match"
        else -> null
    }
}

@Composable
fun SignUpRoute(
    onBack: () -> Unit,
    onSignedUp: () -> Unit,
    viewModel: SignUpViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    SignUpScreen(
        state = state,
        onBack = onBack,
        onUsernameChanged = { viewModel.update { copy(username = it) } },
        onEmailChanged = { viewModel.update { copy(email = it) } },
        onPasswordChanged = { viewModel.update { copy(password = it) } },
        onConfirmPasswordChanged = { viewModel.update { copy(confirmPassword = it) } },
        onSubmit = { viewModel.submit(onSignedUp) },
        onGoogleSignUp = { viewModel.continueWithGoogle(onSignedUp) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    state: SignUpUiState,
    onBack: () -> Unit,
    onUsernameChanged: (String) -> Unit,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onConfirmPasswordChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onGoogleSignUp: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Account") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Back")
                    }
                }
            )
        }
    ) { padding ->
        FittyLazyScreen {
            item { Spacer(modifier = Modifier.height(padding.calculateTopPadding())) }
            item { Field("Username", state.username, onUsernameChanged, Icons.Outlined.AlternateEmail) }
            item { Field("Email", state.email, onEmailChanged, Icons.Outlined.Email, KeyboardType.Email) }
            item { PasswordField("Password", state.password, onPasswordChanged) }
            item { PasswordField("Confirm password", state.confirmPassword, onConfirmPasswordChanged) }
            item {
                state.formError?.let { Text(text = it, color = androidx.compose.material3.MaterialTheme.colorScheme.error) }
            }
            item {
                FittyPrimaryButton(
                    text = "Create Account",
                    onClick = onSubmit,
                    loading = state.isSubmitting
                )
            }
            item {
                FittySecondaryButton(
                    text = "Sign up with Google",
                    onClick = onGoogleSignUp,
                    enabled = !state.isSubmitting,
                    leadingIcon = {
                        Text(
                            text = "G",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = {
            Icon(imageVector = icon, contentDescription = null)
        },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun PasswordField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = {
            Icon(imageVector = Icons.Outlined.Lock, contentDescription = null)
        },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth()
    )
}
