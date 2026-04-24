package com.example.fitty.feature_entry

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fitty.core.designsystem.component.FittyPrimaryButton
import com.example.fitty.core.designsystem.component.FittySecondaryButton
import com.example.fitty.data.firebase.FittyFirebaseRepository
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
    private val repository = FittyFirebaseRepository()
    private val _uiState = MutableStateFlow(WelcomeUiState())
    val uiState: StateFlow<WelcomeUiState> = _uiState

    fun continueAsGuest(onComplete: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isContinuingAsGuest = true) }
            val result = repository.continueAsGuest()
            val guestUser = result.user
            if (guestUser != null) {
                preferences.setCurrentUserId(guestUser.uid)
                preferences.setGuestModeEnabled(true)
                preferences.setSignedIn(false)
                preferences.setOnboardingCompleted(guestUser.onboardingCompleted)
                _uiState.update { it.copy(isContinuingAsGuest = false) }
                onComplete()
            } else {
                _uiState.update { it.copy(isContinuingAsGuest = false) }
            }
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
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        AppMark()
        Text(
            text = "Fitty keeps training, food, and progress in one place",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Answer a few fitness questions, get a starter plan, then track workouts, meals, reminders, and weekly progress.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FeatureRow(
            icon = Icons.Outlined.FitnessCenter,
            title = "Workout plan",
            body = "Sessions matched to your goal, schedule, and equipment."
        )
        FeatureRow(
            icon = Icons.Outlined.Restaurant,
            title = "Nutrition tracking",
            body = "Meal notes and calorie habits stay connected to your goal."
        )
        FeatureRow(
            icon = Icons.Outlined.Insights,
            title = "Progress view",
            body = "See consistency, body metrics, and plan changes clearly."
        )
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

@Composable
private fun AppMark() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.SelfImprovement,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        }
        Column {
            Text(
                text = "Fitty",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Personal fitness companion",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FeatureRow(
    icon: ImageVector,
    title: String,
    body: String
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
