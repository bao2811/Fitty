package com.example.fitty.feature_auth

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
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitty.R
import com.example.fitty.core.designsystem.component.FittyPrimaryButton
import com.example.fitty.core.designsystem.component.FittySecondaryButton
import com.example.fitty.core.ui.FittyLazyScreen
import com.example.fitty.domain.usecase.auth.ContinueAsGuestUseCase
import com.example.fitty.domain.usecase.auth.SignInUseCase
import com.example.fitty.domain.usecase.auth.SignInWithGoogleUseCase
import com.example.fitty.ui.theme.FittyGradientEnd
import com.example.fitty.ui.theme.FittyGradientStart
import com.example.fitty.ui.theme.FittyPink
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SignInUiState(
    val identifier: String = "",
    val password: String = "",
    val identifierError: String? = null,
    val passwordError: String? = null,
    val errorMessage: String? = null,
    val isSubmitting: Boolean = false
)

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val signInUseCase: SignInUseCase,
    private val signInWithGoogleUseCase: SignInWithGoogleUseCase,
    private val continueAsGuestUseCase: ContinueAsGuestUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _uiState = MutableStateFlow(SignInUiState())
    val uiState: StateFlow<SignInUiState> = _uiState

    fun onIdentifierChanged(value: String) { _uiState.update { it.copy(identifier = value, identifierError = null, errorMessage = null) } }
    fun onPasswordChanged(value: String) { _uiState.update { it.copy(password = value, passwordError = null, errorMessage = null) } }

    fun submit(onSuccess: (Boolean) -> Unit) {
        val current = _uiState.value
        val identifierError = validateIdentifier(current.identifier)
        val passwordError = validatePassword(current.password)
        if (identifierError != null || passwordError != null) {
            _uiState.update { it.copy(identifierError = identifierError, passwordError = passwordError) }; return
        }
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
                val result = signInUseCase(current.identifier, current.password)
                if (result.user == null) { _uiState.update { it.copy(isSubmitting = false, errorMessage = result.errorMessage) }; return@launch }
                _uiState.update { it.copy(isSubmitting = false) }; onSuccess(result.user.onboardingCompleted)
            } catch (error: Exception) {
                _uiState.update { it.copy(isSubmitting = false, errorMessage = error.message ?: context.getString(R.string.auth_sign_in_failed)) }
            }
        }
    }

    fun continueAsGuest(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            val result = continueAsGuestUseCase()
            if (result.user == null) { _uiState.update { it.copy(isSubmitting = false, errorMessage = result.errorMessage) }; return@launch }
            _uiState.update { it.copy(isSubmitting = false) }; onSuccess()
        }
    }

    fun submitGoogle(idToken: String, onSuccess: (Boolean) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            val result = signInWithGoogleUseCase(idToken)
            val user = result.user
            if (user == null) {
                _uiState.update { it.copy(isSubmitting = false, errorMessage = result.errorMessage ?: context.getString(R.string.auth_google_failed)) }
                return@launch
            }
            _uiState.update { it.copy(isSubmitting = false) }
            onSuccess(user.onboardingCompleted)
        }
    }

    fun showGoogleError(message: String) {
        _uiState.update { it.copy(errorMessage = message) }
    }

    private fun validateIdentifier(value: String): String? = when {
        value.isBlank() -> context.getString(R.string.auth_email_required)
        "@" !in value -> context.getString(R.string.auth_valid_email)
        else -> null
    }
    private fun validatePassword(value: String): String? = when {
        value.isBlank() -> context.getString(R.string.auth_password_required)
        value.length < 6 -> context.getString(R.string.auth_password_min)
        else -> null
    }
}

@Composable
fun SignInRoute(onBack: () -> Unit, onCreateAccount: () -> Unit, onSignedIn: (Boolean) -> Unit, onContinueAsGuest: () -> Unit, onForgotPassword: () -> Unit = {}, viewModel: SignInViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    SignInScreen(state = state, onBack = onBack, onCreateAccount = onCreateAccount, onIdentifierChanged = viewModel::onIdentifierChanged,
        onPasswordChanged = viewModel::onPasswordChanged, onSubmit = { viewModel.submit(onSignedIn) },
        onGuestSignIn = { viewModel.continueAsGuest(onContinueAsGuest) },
        onGoogleToken = { viewModel.submitGoogle(it, onSignedIn) },
        onGoogleError = viewModel::showGoogleError,
        onForgotPassword = onForgotPassword)
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
    onGuestSignIn: () -> Unit,
    onGoogleToken: (String) -> Unit,
    onGoogleError: (String) -> Unit,
    onForgotPassword: () -> Unit
) {
    val fieldColors = OutlinedTextFieldDefaults.colors(focusedBorderColor = FittyPink, focusedLabelColor = FittyPink, cursorColor = FittyPink)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.auth_sign_in_title), fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.auth_back)) } },
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
                        Text(stringResource(R.string.auth_sign_in_welcome_back), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(stringResource(R.string.auth_sign_in_subtitle), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                    }
                }
            }
            item {
                OutlinedTextField(value = state.identifier, onValueChange = onIdentifierChanged, label = { Text(stringResource(R.string.auth_email)) },
                    leadingIcon = { Icon(Icons.Outlined.AlternateEmail, contentDescription = null) },
                    isError = state.identifierError != null, supportingText = { state.identifierError?.let { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = RoundedCornerShape(16.dp), colors = fieldColors, modifier = Modifier.fillMaxWidth())
            }
            item { SignInPasswordField(value = state.password, onValueChange = onPasswordChanged, error = state.passwordError) }
            item { TextButton(onClick = onForgotPassword) { Text(stringResource(R.string.auth_forgot_password), color = FittyPink) } }
            item { state.errorMessage?.let { Text(text = it, color = MaterialTheme.colorScheme.error) } }
            item { FittyPrimaryButton(text = stringResource(R.string.auth_sign_in_title), onClick = onSubmit, loading = state.isSubmitting) }
            item {
                GoogleAuthButton(
                    loading = state.isSubmitting,
                    enabled = !state.isSubmitting,
                    onIdToken = onGoogleToken,
                    onError = onGoogleError,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item { FittySecondaryButton(text = stringResource(R.string.auth_continue_as_guest), onClick = onGuestSignIn, enabled = !state.isSubmitting) }
            item {
                TextButton(onClick = onCreateAccount, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.auth_no_account), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(stringResource(R.string.auth_create_one), color = FittyPink, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SignInPasswordField(value: String, onValueChange: (String) -> Unit, error: String?) {
    var visible by rememberSaveable { mutableStateOf(false) }
    OutlinedTextField(value = value, onValueChange = onValueChange, label = { Text(stringResource(R.string.auth_password)) },
        leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
        isError = error != null, supportingText = { error?.let { Text(it) } },
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, contentDescription = if (visible) stringResource(R.string.auth_hide_password) else stringResource(R.string.auth_show_password))
            }
        },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = FittyPink, focusedLabelColor = FittyPink, cursorColor = FittyPink),
        modifier = Modifier.fillMaxWidth())
}
