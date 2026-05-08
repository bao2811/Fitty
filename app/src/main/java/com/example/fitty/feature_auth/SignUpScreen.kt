package com.example.fitty.feature_auth

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Email
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
import com.example.fitty.domain.usecase.auth.SignInWithGoogleUseCase
import com.example.fitty.domain.usecase.auth.SignUpUseCase
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

data class SignUpUiState(
    val username: String = "", val email: String = "", val password: String = "",
    val confirmPassword: String = "", val formError: String? = null, val isSubmitting: Boolean = false
)

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val signUpUseCase: SignUpUseCase,
    private val signInWithGoogleUseCase: SignInWithGoogleUseCase,
    private val continueAsGuestUseCase: ContinueAsGuestUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = _uiState

    fun update(transform: SignUpUiState.() -> SignUpUiState) { _uiState.update { it.transform().copy(formError = null) } }

    fun submit(onSuccess: () -> Unit) {
        val error = validate(_uiState.value)
        if (error != null) { _uiState.update { it.copy(formError = error) }; return }
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isSubmitting = true, formError = null) }
                val result = signUpUseCase(
                    username = _uiState.value.username,
                    email = _uiState.value.email,
                    password = _uiState.value.password
                )
                if (result.user == null) { _uiState.update { it.copy(isSubmitting = false, formError = result.errorMessage) }; return@launch }
                _uiState.update { it.copy(isSubmitting = false) }; onSuccess()
            } catch (error: Exception) { _uiState.update { it.copy(isSubmitting = false, formError = error.message ?: context.getString(R.string.auth_create_account_failed)) } }
        }
    }

    fun continueAsGuest(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, formError = null) }
            val result = continueAsGuestUseCase()
            if (result.user == null) { _uiState.update { it.copy(isSubmitting = false, formError = result.errorMessage) }; return@launch }
            _uiState.update { it.copy(isSubmitting = false) }; onSuccess()
        }
    }

    fun submitGoogle(idToken: String, onSuccess: (Boolean) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, formError = null) }
            val result = signInWithGoogleUseCase(idToken)
            val user = result.user
            if (user == null) {
                _uiState.update { it.copy(isSubmitting = false, formError = result.errorMessage ?: context.getString(R.string.auth_google_failed)) }
                return@launch
            }
            _uiState.update { it.copy(isSubmitting = false) }
            onSuccess(user.onboardingCompleted)
        }
    }

    fun showGoogleError(message: String) {
        _uiState.update { it.copy(formError = message) }
    }

    private fun validate(state: SignUpUiState): String? = when {
        state.username.isBlank() -> context.getString(R.string.auth_username_required)
        state.username.length < 3 -> context.getString(R.string.auth_username_min)
        state.email.isBlank() || "@" !in state.email -> context.getString(R.string.auth_enter_valid_email)
        state.password.length < 6 -> context.getString(R.string.auth_password_min)
        state.password != state.confirmPassword -> context.getString(R.string.auth_passwords_mismatch)
        else -> null
    }
}

@Composable
fun SignUpRoute(onBack: () -> Unit, onSignedUp: (Boolean) -> Unit, onContinueAsGuest: () -> Unit, viewModel: SignUpViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    SignUpScreen(state = state, onBack = onBack, onUsernameChanged = { viewModel.update { copy(username = it) } },
        onEmailChanged = { viewModel.update { copy(email = it) } }, onPasswordChanged = { viewModel.update { copy(password = it) } },
        onConfirmPasswordChanged = { viewModel.update { copy(confirmPassword = it) } },
        onSubmit = { viewModel.submit { onSignedUp(false) } }, onGuestSignUp = { viewModel.continueAsGuest(onContinueAsGuest) },
        onGoogleToken = { viewModel.submitGoogle(it, onSignedUp) },
        onGoogleError = viewModel::showGoogleError)
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
    onGuestSignUp: () -> Unit,
    onGoogleToken: (String) -> Unit,
    onGoogleError: (String) -> Unit
) {
    val fc = OutlinedTextFieldDefaults.colors(focusedBorderColor = FittyPink, focusedLabelColor = FittyPink, cursorColor = FittyPink)
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.auth_create_account_title), fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.auth_back)) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent))
        }
    ) { padding ->
        FittyLazyScreen {
            item { Spacer(modifier = Modifier.height(padding.calculateTopPadding())) }
            item {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp)
                    .background(Brush.horizontalGradient(listOf(FittyGradientStart, FittyGradientEnd)), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.auth_join_fitty), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(stringResource(R.string.auth_create_account_subtitle), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                    }
                }
            }
            item { OutlinedTextField(value = state.username, onValueChange = onUsernameChanged, label = { Text(stringResource(R.string.auth_username)) }, leadingIcon = { Icon(Icons.Outlined.AlternateEmail, null) }, shape = RoundedCornerShape(16.dp), colors = fc, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(value = state.email, onValueChange = onEmailChanged, label = { Text(stringResource(R.string.auth_email)) }, leadingIcon = { Icon(Icons.Outlined.Email, null) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), shape = RoundedCornerShape(16.dp), colors = fc, modifier = Modifier.fillMaxWidth()) }
            item { PasswordField(stringResource(R.string.auth_password), state.password, onPasswordChanged) }
            item { PasswordField(stringResource(R.string.auth_confirm_password), state.confirmPassword, onConfirmPasswordChanged) }
            item { state.formError?.let { Text(text = it, color = MaterialTheme.colorScheme.error) } }
            item { FittyPrimaryButton(text = stringResource(R.string.auth_create_account_title), onClick = onSubmit, loading = state.isSubmitting) }
            item {
                GoogleAuthButton(
                    loading = state.isSubmitting,
                    enabled = !state.isSubmitting,
                    onIdToken = onGoogleToken,
                    onError = onGoogleError,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item { FittySecondaryButton(text = stringResource(R.string.auth_continue_as_guest), onClick = onGuestSignUp, enabled = !state.isSubmitting) }
        }
    }
}

@Composable
private fun PasswordField(label: String, value: String, onValueChange: (String) -> Unit) {
    var visible by rememberSaveable { mutableStateOf(false) }
    OutlinedTextField(value = value, onValueChange = onValueChange, label = { Text(label) },
        leadingIcon = { Icon(Icons.Outlined.Lock, null) },
        trailingIcon = { IconButton(onClick = { visible = !visible }) { Icon(if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, contentDescription = if (visible) stringResource(R.string.auth_hide_password) else stringResource(R.string.auth_show_password)) } },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = FittyPink, focusedLabelColor = FittyPink, cursorColor = FittyPink),
        modifier = Modifier.fillMaxWidth())
}
