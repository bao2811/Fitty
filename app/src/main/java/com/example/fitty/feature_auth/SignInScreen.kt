package com.example.fitty.feature_auth

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitty.R
import com.example.fitty.domain.usecase.auth.ContinueAsGuestUseCase
import com.example.fitty.domain.usecase.auth.SignInUseCase
import com.example.fitty.domain.usecase.auth.SignInWithGoogleUseCase
import com.example.fitty.ui.theme.FittyGradientEnd
import com.example.fitty.ui.theme.FittyGradientStart
import com.example.fitty.ui.theme.FittyPink
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
            _uiState.update { it.copy(identifierError = identifierError, passwordError = passwordError) }
            return
        }
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
                val result = signInUseCase(current.identifier.trim(), current.password)
                if (result.user == null) {
                    _uiState.update { it.copy(isSubmitting = false, errorMessage = result.errorMessage ?: context.getString(R.string.auth_sign_in_failed)) }
                    return@launch
                }
                _uiState.update { it.copy(isSubmitting = false) }
                onSuccess(result.user.onboardingCompleted)
            } catch (e: Exception) {
                _uiState.update { it.copy(isSubmitting = false, errorMessage = e.message ?: context.getString(R.string.auth_sign_in_failed)) }
            }
        }
    }

    fun submitGoogle(idToken: String, onSuccess: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                _uiState.update {
                    it.copy(isSubmitting = true, errorMessage = null)
                }
                val result = signInWithGoogleUseCase(idToken)
                val user = result.user
                if (user == null) {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = result.errorMessage ?: context.getString(R.string.auth_google_failed)
                        )
                    }
                    return@launch
                }
                _uiState.update { it.copy(isSubmitting = false) }
                onSuccess(user.onboardingCompleted)
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = error.message ?: context.getString(R.string.auth_google_failed)
                    )
                }
            }
        }
    }

    fun showGoogleError(message: String) {
        _uiState.update { it.copy(errorMessage = message) }
    }

    fun continueAsGuest(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            val result = continueAsGuestUseCase()
            if (result.user == null) {
                _uiState.update { it.copy(isSubmitting = false, errorMessage = result.errorMessage ?: context.getString(R.string.auth_sign_in_failed)) }
                return@launch
            }
            _uiState.update { it.copy(isSubmitting = false) }
            onSuccess()
        }
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
fun SignInRoute(
    onBack: () -> Unit,
    onCreateAccount: () -> Unit,
    onSignedIn: (Boolean) -> Unit,
    onContinueAsGuest: () -> Unit,
    onForgotPassword: () -> Unit = {},
    viewModel: SignInViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    SignInScreen(
        state = state,
        onBack = onBack,
        onCreateAccount = onCreateAccount,
        onIdentifierChanged = viewModel::onIdentifierChanged,
        onPasswordChanged = viewModel::onPasswordChanged,
        onSubmit = { viewModel.submit(onSignedIn) },
        onGuestSignIn = { viewModel.continueAsGuest(onContinueAsGuest) },
        onGoogleToken = { viewModel.submitGoogle(it, onSignedIn) },
        onGoogleError = viewModel::showGoogleError,
        onForgotPassword = onForgotPassword
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
    onGuestSignIn: () -> Unit,
    onGoogleToken: (String) -> Unit,
    onGoogleError: (String) -> Unit,
    onForgotPassword: () -> Unit
) {
    Scaffold(
        containerColor = Color(0xFFF8F5FB),
        contentWindowInsets = WindowInsets.statusBars
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFFFF9FD), Color(0xFFF6F3FB), Color.White)
                    )
                )
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            // App Icon
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .shadow(20.dp, RoundedCornerShape(24.dp), ambientColor = FittyPink.copy(alpha = 0.2f), spotColor = FittyPink.copy(alpha = 0.2f))
                    .clip(RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_fitty_logo),
                    contentDescription = stringResource(R.string.auth_logo_content_desc),
                    modifier = Modifier.size(100.dp),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            // Title
            Text(
                text = stringResource(R.string.auth_sign_in_welcome_back),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.auth_sign_in_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            // Form fields
            FittyAuthTextField(
                value = state.identifier,
                onValueChange = onIdentifierChanged,
                label = stringResource(R.string.auth_email),
                icon = Icons.Outlined.AlternateEmail,
                keyboardType = KeyboardType.Email,
                error = state.identifierError
            )
            SignInPasswordField(
                value = state.password,
                onValueChange = onPasswordChanged,
                error = state.passwordError
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = stringResource(R.string.auth_forgot_password),
                    color = FittyPink,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.clickable(onClick = onForgotPassword)
                )
            }
            state.errorMessage?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(modifier = Modifier.height(2.dp))
            FittyAuthPrimaryButton(
                text = stringResource(R.string.auth_sign_in_title),
                onClick = onSubmit,
                loading = state.isSubmitting
            )
            FittyAuthDivider()
            GoogleAuthButton(
                loading = state.isSubmitting,
                enabled = !state.isSubmitting,
                onIdToken = onGoogleToken,
                onError = onGoogleError,
                modifier = Modifier.fillMaxWidth()
            )
            FittyAuthOutlineActionButton(
                text = stringResource(R.string.auth_continue_as_guest),
                icon = Icons.Outlined.PersonOutline,
                enabled = !state.isSubmitting,
                onClick = onGuestSignIn
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.auth_no_account),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = onCreateAccount) {
                    Text(
                        text = stringResource(R.string.auth_create_one),
                        color = FittyPink,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SignInPasswordField(value: String, onValueChange: (String) -> Unit, error: String?) {
    var visible by rememberSaveable { mutableStateOf(false) }
    FittyAuthTextField(
        value = value,
        onValueChange = onValueChange,
        label = stringResource(R.string.auth_password),
        icon = Icons.Outlined.Lock,
        keyboardType = KeyboardType.Password,
        error = error,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingContent = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = if (visible) {
                        stringResource(R.string.auth_hide_password)
                    } else {
                        stringResource(R.string.auth_show_password)
                    }
                )
            }
        }
    )
}
