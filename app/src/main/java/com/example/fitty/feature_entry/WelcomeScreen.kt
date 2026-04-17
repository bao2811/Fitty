package com.example.fitty.feature_entry

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fitty.core.designsystem.component.FittyInfoCard
import com.example.fitty.core.designsystem.component.FittyPrimaryButton
import com.example.fitty.core.designsystem.component.FittySecondaryButton
import com.example.fitty.data.preferences.AppPreferencesDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WelcomeUiState(
    val isContinuingAsGuest: Boolean = false
)

class WelcomeViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = AppPreferencesDataSource(application.applicationContext)
    private val _uiState = MutableStateFlow(WelcomeUiState())
    val uiState: StateFlow<WelcomeUiState> = _uiState

    fun continueAsGuest(onComplete: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isContinuingAsGuest = true) }
            preferences.setGuestModeEnabled(true)
            preferences.setSignedIn(false)
            _uiState.update { it.copy(isContinuingAsGuest = false) }
            onComplete()
        }
    }
}

@Composable
fun WelcomeRoute(
    onCreateAccount: () -> Unit,
    onSignIn: () -> Unit,
    onContinueAsGuest: () -> Unit,
    viewModel: WelcomeViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    WelcomeScreen(
        isContinuingAsGuest = state.isContinuingAsGuest,
        onCreateAccount = onCreateAccount,
        onSignIn = onSignIn,
        onContinueAsGuest = { viewModel.continueAsGuest(onContinueAsGuest) }
    )
}

@Composable
fun WelcomeScreen(
    isContinuingAsGuest: Boolean,
    onCreateAccount: () -> Unit,
    onSignIn: () -> Unit,
    onContinueAsGuest: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Spacer(modifier = Modifier.height(48.dp))
            FittyInfoCard(
                title = "Train smarter",
                body = "Build a personal fitness plan, log meals faster, and keep your progress visible."
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Train smarter, eat better, stay consistent",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Fitty turns your goal into daily actions for workouts, nutrition, and recovery.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        Column {
            FittyPrimaryButton(text = "Create Account", onClick = onCreateAccount)
            Spacer(modifier = Modifier.height(12.dp))
            FittySecondaryButton(text = "Sign In", onClick = onSignIn)
            TextButton(
                onClick = onContinueAsGuest,
                enabled = !isContinuingAsGuest,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(if (isContinuingAsGuest) "Preparing guest mode..." else "Continue as Guest")
            }
        }
    }
}
