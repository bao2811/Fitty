package com.example.fitty.feature_auth

import android.app.Application
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AlternateEmail
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

data class SignInUiState(
    val identifier: String = "",
    val password: String = "",
    val identifierError: String? = null,
    val passwordError: String? = null,
    val errorMessage: String? = null,
    val isSubmitting: Boolean = false
)

class SignInViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = AppPreferencesDataSource(application.applicationContext)
    private val repository = FittyLocalRepository(application.applicationContext)
    private val _uiState = MutableStateFlow(SignInUiState())
    val uiState: StateFlow<SignInUiState> = _uiState

    fun onIdentifierChanged(value: String) {
        _uiState.update { it.copy(identifier = value, identifierError = null, errorMessage = null) }
    }

    fun onPasswordChanged(value: String) {
        _uiState.update { it.copy(password = value, passwordError = null, errorMessage = null) }
    }

    fun submit(onSuccess: () -> Unit) {
        val current = _uiState.value
        val identifierError = validateIdentifier(current.identifier)
        val passwordError = validatePassword(current.password)
        if (identifierError != null || passwordError != null) {
            _uiState.update {
                it.copy(identifierError = identifierError, passwordError = passwordError)
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            val result = repository.signInWithPassword(current.identifier, current.password)
            if (result.user == null) {
                _uiState.update { it.copy(isSubmitting = false, errorMessage = result.errorMessage) }
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
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            val result = repository.continueWithGoogleDemo()
            if (result.user == null) {
                _uiState.update { it.copy(isSubmitting = false, errorMessage = result.errorMessage) }
                return@launch
            }
            preferences.setCurrentUserId(result.user.id)
            preferences.setSignedIn(true)
            preferences.setGuestModeEnabled(false)
            _uiState.update { it.copy(isSubmitting = false) }
            onSuccess()
        }
    }

    private fun validateIdentifier(value: String): String? = when {
        value.isBlank() -> "Email or username is required"
        else -> null
    }

    private fun validatePassword(value: String): String? = when {
        value.isBlank() -> "Password is required"
        value.length < 6 -> "Password must be at least 6 characters"
        else -> null
    }
}

@Composable
fun SignInRoute(
    onBack: () -> Unit,
    onCreateAccount: () -> Unit,
    onSignedIn: () -> Unit,
    viewModel: SignInViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    SignInScreen(
        state = state,
        onBack = onBack,
        onCreateAccount = onCreateAccount,
        onIdentifierChanged = viewModel::onIdentifierChanged,
        onPasswordChanged = viewModel::onPasswordChanged,
        onSubmit = { viewModel.submit(onSignedIn) },
        onGoogleSignIn = { viewModel.continueWithGoogle(onSignedIn) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignInScreen(
    state: SignInUiState,
    onBack: () -> Unit,
    onCreateAccount: () -> Unit,
    onIdentifierChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onGoogleSignIn: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sign In") },
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
            item {
                OutlinedTextField(
                    value = state.identifier,
                    onValueChange = onIdentifierChanged,
                    label = { Text("Email or username") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.AlternateEmail,
                            contentDescription = null
                        )
                    },
                    isError = state.identifierError != null,
                    supportingText = { state.identifierError?.let { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = state.password,
                    onValueChange = onPasswordChanged,
                    label = { Text("Password") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null
                        )
                    },
                    isError = state.passwordError != null,
                    supportingText = { state.passwordError?.let { Text(it) } },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                TextButton(onClick = { }) {
                    Text("Forgot password?")
                }
            }
            item {
                state.errorMessage?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.error)
                }
            }
            item {
                FittyPrimaryButton(
                    text = "Sign In",
                    onClick = onSubmit,
                    loading = state.isSubmitting
                )
            }
            item {
                FittySecondaryButton(
                    text = "Continue with Google",
                    onClick = onGoogleSignIn,
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
            item {
                TextButton(onClick = onCreateAccount) {
                    Text("Do not have an account? Create one")
                }
            }
        }
    }
}
