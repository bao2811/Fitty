package com.example.fitty.feature_entry

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fitty.data.firebase.FittyFirebaseRepository
import com.example.fitty.data.preferences.AppPreferencesDataSource
import com.example.fitty.notifications.FittyMessagingCoordinator
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class StartupDestination {
    Welcome,
    Onboarding,
    Main
}

data class SplashUiState(
    val destination: StartupDestination? = null
)

class SplashViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = AppPreferencesDataSource(application.applicationContext)
    private val repository = FittyFirebaseRepository()
    private val messagingCoordinator = FittyMessagingCoordinator(
        context = application.applicationContext,
        repository = repository,
        preferences = preferences
    )
    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState

    init {
        resolveStartupDestination()
    }

    private fun resolveStartupDestination() {
        viewModelScope.launch {
            delay(450)
            val session = repository.getStartupState()
            preferences.setCurrentUserId(session.uid)
            preferences.setGuestModeEnabled(session.isGuest)
            preferences.setSignedIn(session.isSignedIn)
            preferences.setOnboardingCompleted(session.onboardingCompleted)
            runCatching {
                messagingCoordinator.syncTokenAndWelcomeUser(session)
            }
            val destination = when {
                (session.isGuest || session.isSignedIn) && session.onboardingCompleted -> StartupDestination.Main
                session.isGuest || session.isSignedIn -> StartupDestination.Onboarding
                else -> StartupDestination.Welcome
            }
            _uiState.update { it.copy(destination = destination) }
        }
    }
}

@Composable
fun SplashRoute(
    onOpenWelcome: () -> Unit,
    onOpenOnboarding: () -> Unit,
    onOpenMain: () -> Unit,
    viewModel: SplashViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.destination) {
        when (state.destination) {
            StartupDestination.Welcome -> onOpenWelcome()
            StartupDestination.Onboarding -> onOpenOnboarding()
            StartupDestination.Main -> onOpenMain()
            null -> Unit
        }
    }

    SplashScreen()
}

@Composable
fun SplashScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Fitty",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Your AI fitness partner",
            style = MaterialTheme.typography.titleMedium,
            letterSpacing = 0.sp
        )
        Spacer(modifier = Modifier.height(28.dp))
        CircularProgressIndicator()
    }
}
