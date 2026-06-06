package com.example.fitty.feature_auth

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PersonAddAlt1
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.example.fitty.domain.usecase.auth.SignInWithGoogleUseCase
import com.example.fitty.domain.usecase.auth.SignUpUseCase
import com.example.fitty.ui.theme.FittyPink
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SignUpUiState(
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val formError: String? = null,
    val isSubmitting: Boolean = false
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
        if (error != null) {
            _uiState.update { it.copy(formError = error) }
            return
        }
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isSubmitting = true, formError = null) }
                val result = signUpUseCase(
                    username = _uiState.value.username,
                    email = _uiState.value.email,
                    password = _uiState.value.password
                )
                if (result.user == null) {
                    _uiState.update { it.copy(isSubmitting = false, formError = result.errorMessage) }
                    return@launch
                }
                _uiState.update { it.copy(isSubmitting = false) }
                onSuccess()
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        formError = error.message ?: context.getString(R.string.auth_create_account_failed)
                    )
                }
            }
        }
    }

    fun continueAsGuest(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, formError = null) }
            val result = continueAsGuestUseCase()
            if (result.user == null) {
                _uiState.update { it.copy(isSubmitting = false, formError = result.errorMessage) }
                return@launch
            }
            _uiState.update { it.copy(isSubmitting = false) }
            onSuccess()
        }
    }

    fun submitGoogle(idToken: String, onSuccess: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                _uiState.update {
                    it.copy(isSubmitting = true, formError = null)
                }
                val result = signInWithGoogleUseCase(idToken)
                val user = result.user
                if (user == null) {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            formError = result.errorMessage ?: context.getString(R.string.auth_google_failed)
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
                        formError = error.message ?: context.getString(R.string.auth_google_failed)
                    )
                }
            }
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
fun SignUpRoute(
    onBack: () -> Unit,
    onSignedUp: (Boolean) -> Unit,
    onContinueAsGuest: () -> Unit,
    viewModel: SignUpViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    SignUpScreen(
        state = state,
        onBack = onBack,
        onUsernameChanged = { viewModel.update { copy(username = it) } },
        onEmailChanged = { viewModel.update { copy(email = it) } },
        onPasswordChanged = { viewModel.update { copy(password = it) } },
        onConfirmPasswordChanged = { viewModel.update { copy(confirmPassword = it) } },
        onSubmit = { viewModel.submit { onSignedUp(false) } },
        onGuestSignUp = { viewModel.continueAsGuest(onContinueAsGuest) },
        onGoogleToken = { viewModel.submitGoogle(it, onSignedUp) },
        onGoogleError = viewModel::showGoogleError
    )
}

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
    var acceptedTerms by rememberSaveable { mutableStateOf(true) }

    Scaffold(
        containerColor = Color(0xFFF8F5FB),
        contentWindowInsets = WindowInsets.statusBars
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFFFFAFD), Color(0xFFF6F3FB), Color.White)
                    )
                )
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AuthBackButtonCard(onBack = onBack)
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .shadow(18.dp, RoundedCornerShape(22.dp), ambientColor = FittyPink.copy(alpha = 0.15f), spotColor = FittyPink.copy(alpha = 0.15f))
                        .clip(RoundedCornerShape(22.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_fitty_logo),
                        contentDescription = stringResource(R.string.auth_logo_content_desc),
                        modifier = Modifier.size(84.dp),
                        contentScale = ContentScale.Crop
                    )
                }
                Text(
                    text = stringResource(R.string.auth_create_account_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(R.string.auth_sign_up_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            FittyAuthTextField(
                value = state.username,
                onValueChange = onUsernameChanged,
                label = stringResource(R.string.auth_username),
                icon = Icons.Outlined.PersonOutline,
                keyboardType = KeyboardType.Text
            )
            FittyAuthTextField(
                value = state.email,
                onValueChange = onEmailChanged,
                label = stringResource(R.string.auth_email),
                icon = Icons.Outlined.Email,
                keyboardType = KeyboardType.Email
            )
            SignUpPasswordField(
                label = stringResource(R.string.auth_password),
                value = state.password,
                onValueChange = onPasswordChanged
            )
            SignUpPasswordField(
                label = stringResource(R.string.auth_confirm_password),
                value = state.confirmPassword,
                onValueChange = onConfirmPasswordChanged
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Checkbox(
                    checked = acceptedTerms,
                    onCheckedChange = { acceptedTerms = it }
                )
                Text(
                    text = stringResource(R.string.auth_terms_acceptance),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 12.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            state.formError?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            if (!acceptedTerms) {
                Text(
                    text = stringResource(R.string.auth_terms_required),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            FittyAuthPrimaryButton(
                text = stringResource(R.string.auth_create_account_title),
                onClick = onSubmit,
                loading = state.isSubmitting,
                enabled = acceptedTerms
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
                icon = Icons.Outlined.TaskAlt,
                enabled = !state.isSubmitting,
                onClick = onGuestSignUp
            )
        }
    }
}

@Composable
private fun SignUpPasswordField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    var visible by rememberSaveable { mutableStateOf(false) }
    FittyAuthTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        icon = Icons.Outlined.Lock,
        keyboardType = KeyboardType.Password,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingContent = {
            androidx.compose.material3.IconButton(onClick = { visible = !visible }) {
                androidx.compose.material3.Icon(
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
