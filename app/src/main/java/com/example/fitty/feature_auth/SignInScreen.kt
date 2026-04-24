package com.example.fitty.feature_auth

import android.app.Application
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fitty.core.designsystem.component.FittyPrimaryButton
import com.example.fitty.core.designsystem.component.FittySecondaryButton
import com.example.fitty.core.ui.FittyLazyScreen
import com.example.fitty.data.firebase.FittyFirebaseRepository
import com.example.fitty.data.preferences.AppPreferencesDataSource
import com.example.fitty.notifications.FittyMessagingCoordinator
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
    private val repository = FittyFirebaseRepository()
    private val messagingCoordinator = FittyMessagingCoordinator(
        context = application.applicationContext,
        repository = repository,
        preferences = preferences
    )
    private val _uiState = MutableStateFlow(SignInUiState())
    val uiState: StateFlow<SignInUiState> = _uiState

    fun onIdentifierChanged(value: String) {
        _uiState.update { it.copy(identifier = value, identifierError = null, errorMessage = null) }
    }

    fun onPasswordChanged(value: String) {
        _uiState.update { it.copy(password = value, passwordError = null, errorMessage = null) }
    }

    fun submit(onSuccess: (Boolean) -> Unit) {
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
            try {
                _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
                val result = repository.signInWithPassword(current.identifier, current.password)
                if (result.user == null) {
                    _uiState.update { it.copy(isSubmitting = false, errorMessage = result.errorMessage) }
                    return@launch
                }
                preferences.setCurrentUserId(result.user.uid)
                preferences.setSignedIn(!result.user.guest)
                preferences.setGuestModeEnabled(result.user.guest)
                preferences.setOnboardingCompleted(result.user.onboardingCompleted)
                runCatching {
                    messagingCoordinator.syncTokenAndWelcomeUser(
                        user = result.user,
                        forceNotification = true
                    )
                }
                _uiState.update { it.copy(isSubmitting = false) }
                onSuccess(result.user.onboardingCompleted)
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = error.message ?: "Sign in failed"
                    )
                }
            }
        }
    }

    fun continueAsGuest(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            val result = repository.continueAsGuest()
            if (result.user == null) {
                _uiState.update { it.copy(isSubmitting = false, errorMessage = result.errorMessage) }
                return@launch
            }
            preferences.setCurrentUserId(result.user.uid)
            preferences.setSignedIn(false)
            preferences.setGuestModeEnabled(true)
            preferences.setOnboardingCompleted(result.user.onboardingCompleted)
            _uiState.update { it.copy(isSubmitting = false) }
            onSuccess()
        }
    }

    private fun validateIdentifier(value: String): String? = when {
        value.isBlank() -> "Email is required"
        "@" !in value -> "Enter a valid email address"
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
    onSignedIn: (Boolean) -> Unit,
    onContinueAsGuest: () -> Unit,
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
        onGuestSignIn = { viewModel.continueAsGuest(onContinueAsGuest) }
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
    onGuestSignIn: () -> Unit
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
                    label = { Text("Email") },
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
                SignInPasswordField(
                    value = state.password,
                    onValueChange = onPasswordChanged,
                    error = state.passwordError
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
                    text = "Continue as Guest",
                    onClick = onGuestSignIn,
                    enabled = !state.isSubmitting
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

@Composable
private fun SignInPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    error: String?
) {
    var visible by rememberSaveable { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("Password") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null
            )
        },
        isError = error != null,
        supportingText = { error?.let { Text(it) } },
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    imageVector = if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = if (visible) "Hide password" else "Show password"
                )
            }
        },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth()
    )
}
