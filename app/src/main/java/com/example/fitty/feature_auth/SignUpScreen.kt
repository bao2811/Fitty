package com.example.fitty.feature_auth

import android.app.Application
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fitty.core.designsystem.component.FittyPrimaryButton
import com.example.fitty.core.ui.FittyLazyScreen
import com.example.fitty.data.preferences.AppPreferencesDataSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SignUpUiState(
    val fullName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val age: String = "",
    val gender: String = "",
    val height: String = "",
    val weight: String = "",
    val acceptedTerms: Boolean = false,
    val formError: String? = null,
    val isSubmitting: Boolean = false
)

class SignUpViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = AppPreferencesDataSource(application.applicationContext)
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
            delay(450)
            preferences.setSignedIn(true)
            preferences.setGuestModeEnabled(false)
            _uiState.update { it.copy(isSubmitting = false) }
            onSuccess()
        }
    }

    private fun validate(state: SignUpUiState): String? = when {
        state.fullName.isBlank() -> "Full name is required"
        state.email.isBlank() || "@" !in state.email -> "Enter a valid email"
        state.password.length < 6 -> "Password must be at least 6 characters"
        state.password != state.confirmPassword -> "Passwords do not match"
        state.age.toIntOrNull() == null -> "Age must be a number"
        state.height.toFloatOrNull() == null -> "Height must be a number"
        state.weight.toFloatOrNull() == null -> "Weight must be a number"
        !state.acceptedTerms -> "You need to agree to the terms"
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
        onFullNameChanged = { viewModel.update { copy(fullName = it) } },
        onEmailChanged = { viewModel.update { copy(email = it) } },
        onPasswordChanged = { viewModel.update { copy(password = it) } },
        onConfirmPasswordChanged = { viewModel.update { copy(confirmPassword = it) } },
        onAgeChanged = { viewModel.update { copy(age = it) } },
        onGenderChanged = { viewModel.update { copy(gender = it) } },
        onHeightChanged = { viewModel.update { copy(height = it) } },
        onWeightChanged = { viewModel.update { copy(weight = it) } },
        onTermsChanged = { viewModel.update { copy(acceptedTerms = it) } },
        onSubmit = { viewModel.submit(onSignedUp) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    state: SignUpUiState,
    onBack: () -> Unit,
    onFullNameChanged: (String) -> Unit,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onConfirmPasswordChanged: (String) -> Unit,
    onAgeChanged: (String) -> Unit,
    onGenderChanged: (String) -> Unit,
    onHeightChanged: (String) -> Unit,
    onWeightChanged: (String) -> Unit,
    onTermsChanged: (Boolean) -> Unit,
    onSubmit: () -> Unit
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
            item { Field("Full name", state.fullName, onFullNameChanged) }
            item { Field("Email", state.email, onEmailChanged, KeyboardType.Email) }
            item { PasswordField("Password", state.password, onPasswordChanged) }
            item { PasswordField("Confirm password", state.confirmPassword, onConfirmPasswordChanged) }
            item { Field("Age", state.age, onAgeChanged, KeyboardType.Number) }
            item { Field("Gender", state.gender, onGenderChanged) }
            item { Field("Height cm", state.height, onHeightChanged, KeyboardType.Number) }
            item { Field("Weight kg", state.weight, onWeightChanged, KeyboardType.Number) }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = state.acceptedTerms, onCheckedChange = onTermsChanged)
                    Text("I agree to the terms")
                }
            }
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
        }
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
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
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
    )
}
