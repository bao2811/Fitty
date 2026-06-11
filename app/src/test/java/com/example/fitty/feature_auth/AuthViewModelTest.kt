package com.example.fitty.feature_auth

import androidx.test.core.app.ApplicationProvider
import com.example.fitty.domain.model.FittyAuthResult
import com.example.fitty.domain.model.FittyStartupState
import com.example.fitty.domain.model.FittyUser
import com.example.fitty.domain.repository.AuthRepository
import com.example.fitty.domain.repository.SessionRepository
import com.example.fitty.domain.usecase.auth.ContinueAsGuestUseCase
import com.example.fitty.domain.usecase.auth.SendPasswordResetUseCase
import com.example.fitty.domain.usecase.auth.SignInUseCase
import com.example.fitty.domain.usecase.auth.SignInWithGoogleUseCase
import com.example.fitty.domain.usecase.auth.SignUpUseCase
import com.example.fitty.feature_track.MainDispatcherRule
import com.example.fitty.notifications.FittyBannerController
import com.example.fitty.notifications.FittyMessagingCoordinator
import com.example.fitty.notifications.FittyNotificationDispatcher
import com.example.fitty.domain.repository.AppNotificationRepository
import com.example.fitty.domain.repository.NotificationTokenRepository
import com.example.fitty.domain.model.AppNotification
import com.example.fitty.domain.model.AppNotificationType
import com.example.fitty.domain.usecase.user.UpdateStreakUseCase
import com.example.fitty.domain.repository.UserRepository
import com.example.fitty.domain.model.FittyOnboarding
import com.example.fitty.domain.model.FittyProfile
import com.example.fitty.domain.model.FittySettings
import com.example.fitty.domain.model.FittyStats
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [35])
class AuthViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `sign in rejects invalid email and password`() = runTest {
        val viewModel = authHarness().signInViewModel()

        viewModel.onIdentifierChanged("not-an-email")
        viewModel.onPasswordChanged("123")
        viewModel.submit {}

        val state = viewModel.uiState.value
        assertNotNull(state.identifierError)
        assertNotNull(state.passwordError)
    }

    @Test
    fun `sign in success calls callback with onboarding state`() = runTest {
        val authRepository = FakeAuthRepository(
            passwordSignInResult = FittyAuthResult(testUser(onboardingCompleted = true, guest = true))
        )
        val viewModel = authHarness(authRepository).signInViewModel()
        var callbackValue: Boolean? = null

        viewModel.onIdentifierChanged("fitty@example.com")
        viewModel.onPasswordChanged("secret1")
        viewModel.submit { callbackValue = it }
        advanceUntilIdle()

        assertEquals(true, callbackValue)
        assertFalse(viewModel.uiState.value.isSubmitting)
        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals("fitty@example.com", authRepository.lastPasswordIdentifier)
    }

    @Test
    fun `sign in failure shows repository error`() = runTest {
        val authRepository = FakeAuthRepository(
            passwordSignInResult = FittyAuthResult(errorMessage = "Bad credentials")
        )
        val viewModel = authHarness(authRepository).signInViewModel()

        viewModel.onIdentifierChanged("fitty@example.com")
        viewModel.onPasswordChanged("secret1")
        viewModel.submit {}
        advanceUntilIdle()

        assertEquals("Bad credentials", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isSubmitting)
    }

    @Test
    fun `sign in guest success saves session and calls callback`() = runTest {
        val authRepository = FakeAuthRepository(
            guestResult = FittyAuthResult(testUser(uid = "guest", guest = true))
        )
        val harness = authHarness(authRepository)
        val viewModel = harness.signInViewModel()
        var called = false

        viewModel.continueAsGuest { called = true }
        advanceUntilIdle()

        assertTrue(called)
        assertEquals("guest", harness.sessionRepository.savedUser?.uid)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `sign up validates username email password and confirmation`() = runTest {
        val viewModel = authHarness().signUpViewModel()

        viewModel.submit {}
        assertNotNull(viewModel.uiState.value.formError)

        viewModel.update { copy(username = "ab", email = "fitty@example.com", password = "secret1", confirmPassword = "secret1") }
        viewModel.submit {}
        assertNotNull(viewModel.uiState.value.formError)

        viewModel.update { copy(username = "fitty", email = "bad", password = "secret1", confirmPassword = "secret1") }
        viewModel.submit {}
        assertNotNull(viewModel.uiState.value.formError)

        viewModel.update { copy(username = "fitty", email = "fitty@example.com", password = "secret1", confirmPassword = "different") }
        viewModel.submit {}
        assertNotNull(viewModel.uiState.value.formError)
    }

    @Test
    fun `sign up success calls callback`() = runTest {
        val authRepository = FakeAuthRepository(
            signUpResult = FittyAuthResult(testUser(uid = "new-user", guest = true))
        )
        val harness = authHarness(authRepository)
        val viewModel = harness.signUpViewModel()
        var called = false

        viewModel.update {
            copy(
                username = "fitty",
                email = "fitty@example.com",
                password = "secret1",
                confirmPassword = "secret1"
            )
        }
        viewModel.submit { called = true }
        advanceUntilIdle()

        assertTrue(called)
        assertEquals("new-user", harness.sessionRepository.savedUser?.uid)
        assertEquals("fitty", authRepository.lastSignUpUsername)
    }

    @Test
    fun `sign up google failure shows form error`() = runTest {
        val authRepository = FakeAuthRepository(
            googleResult = FittyAuthResult(errorMessage = "Google failed")
        )
        val viewModel = authHarness(authRepository).signUpViewModel()

        viewModel.submitGoogle("token") {}
        advanceUntilIdle()

        assertEquals("Google failed", viewModel.uiState.value.formError)
    }

    @Test
    fun `sign up guest failure shows form error`() = runTest {
        val authRepository = FakeAuthRepository(
            guestResult = FittyAuthResult(errorMessage = "Guest disabled")
        )
        val viewModel = authHarness(authRepository).signUpViewModel()

        viewModel.continueAsGuest {}
        advanceUntilIdle()

        assertEquals("Guest disabled", viewModel.uiState.value.formError)
    }

    @Test
    fun `forgot password rejects invalid email`() = runTest {
        val viewModel = authHarness().forgotPasswordViewModel()

        viewModel.onEmailChanged("bad")
        viewModel.submit()

        assertEquals("Enter a valid email address", viewModel.uiState.value.emailError)
    }

    @Test
    fun `forgot password success marks email sent`() = runTest {
        val authRepository = FakeAuthRepository(resetResult = Result.success(Unit))
        val viewModel = authHarness(authRepository).forgotPasswordViewModel()

        viewModel.onEmailChanged("fitty@example.com")
        viewModel.submit()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.emailSent)
        assertEquals("fitty@example.com", authRepository.lastResetEmail)
    }

    @Test
    fun `forgot password failure shows error`() = runTest {
        val authRepository = FakeAuthRepository(
            resetResult = Result.failure(IllegalStateException("No user"))
        )
        val viewModel = authHarness(authRepository).forgotPasswordViewModel()

        viewModel.onEmailChanged("fitty@example.com")
        viewModel.submit()
        advanceUntilIdle()

        assertEquals("No user", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.emailSent)
    }

    private fun authHarness(authRepository: FakeAuthRepository = FakeAuthRepository()): AuthHarness {
        val sessionRepository = FakeAuthSessionRepository()
        val userRepository = FakeAuthUserRepository()
        val appNotificationRepository = FakeAuthNotificationRepository()
        val messagingCoordinator = FittyMessagingCoordinator(
            firebaseMessaging = FirebaseMessaging.getInstance(),
            notificationTokenRepository = FakeNotificationTokenRepository(),
            sessionRepository = sessionRepository,
            notificationDispatcher = FittyNotificationDispatcher(
                context = ApplicationProvider.getApplicationContext(),
                bannerController = FittyBannerController(),
                appNotificationRepository = appNotificationRepository
            ),
            updateStreakUseCase = UpdateStreakUseCase(userRepository, sessionRepository)
        )
        return AuthHarness(authRepository, sessionRepository, messagingCoordinator)
    }
}

private class AuthHarness(
    private val authRepository: FakeAuthRepository,
    val sessionRepository: FakeAuthSessionRepository,
    private val messagingCoordinator: FittyMessagingCoordinator
) {
    fun signInViewModel(): SignInViewModel = SignInViewModel(
        signInUseCase = SignInUseCase(authRepository, sessionRepository, messagingCoordinator),
        signInWithGoogleUseCase = SignInWithGoogleUseCase(authRepository, sessionRepository, messagingCoordinator),
        continueAsGuestUseCase = ContinueAsGuestUseCase(authRepository, sessionRepository),
        context = ApplicationProvider.getApplicationContext()
    )

    fun signUpViewModel(): SignUpViewModel = SignUpViewModel(
        signUpUseCase = SignUpUseCase(authRepository, sessionRepository, messagingCoordinator),
        signInWithGoogleUseCase = SignInWithGoogleUseCase(authRepository, sessionRepository, messagingCoordinator),
        continueAsGuestUseCase = ContinueAsGuestUseCase(authRepository, sessionRepository),
        context = ApplicationProvider.getApplicationContext()
    )

    fun forgotPasswordViewModel(): ForgotPasswordViewModel = ForgotPasswordViewModel(
        sendPasswordResetUseCase = SendPasswordResetUseCase(authRepository),
        context = ApplicationProvider.getApplicationContext()
    )
}

private class FakeAuthRepository(
    var signUpResult: FittyAuthResult = FittyAuthResult(testUser(guest = true)),
    var passwordSignInResult: FittyAuthResult = FittyAuthResult(testUser(guest = true)),
    var googleResult: FittyAuthResult = FittyAuthResult(testUser(guest = true)),
    var guestResult: FittyAuthResult = FittyAuthResult(testUser(uid = "guest", guest = true)),
    var resetResult: Result<Unit> = Result.success(Unit)
) : AuthRepository {
    var lastSignUpUsername: String? = null
    var lastPasswordIdentifier: String? = null
    var lastResetEmail: String? = null

    override suspend fun createPasswordUser(username: String, email: String, password: String): FittyAuthResult {
        lastSignUpUsername = username
        return signUpResult
    }

    override suspend fun signInWithPassword(identifier: String, password: String): FittyAuthResult {
        lastPasswordIdentifier = identifier
        return passwordSignInResult
    }

    override suspend fun signInWithGoogle(idToken: String): FittyAuthResult = googleResult
    override suspend fun continueAsGuest(): FittyAuthResult = guestResult
    override suspend fun sendPasswordReset(email: String): Result<Unit> {
        lastResetEmail = email
        return resetResult
    }
    override suspend fun deleteAccount(): Result<Unit> = Result.success(Unit)
    override suspend fun reauthenticateWithPassword(password: String): Result<Unit> = Result.success(Unit)
    override suspend fun reauthenticateWithGoogle(idToken: String): Result<Unit> = Result.success(Unit)
    override fun signOut() = Unit
}

private class FakeAuthSessionRepository : SessionRepository {
    var savedUser: FittyUser? = null
    var currentUserId: String? = "uid"

    override suspend fun saveStartupState(state: FittyStartupState) = Unit
    override suspend fun saveUserSession(user: FittyUser) {
        savedUser = user
        currentUserId = user.uid
    }
    override suspend fun setOnboardingCompleted(completed: Boolean) = Unit
    override suspend fun getCurrentUserId(): String? = currentUserId
    override fun observeCurrentUserId(): Flow<String?> = flowOf(currentUserId)
    override suspend fun getAppLanguage(): String? = "en"
    override suspend fun setAppLanguage(language: String) = Unit
    override suspend fun clearSession() {
        currentUserId = null
    }
    override suspend fun isSignedInSessionExpired(nowMillis: Long, maxAgeMillis: Long): Boolean = false
    override suspend fun shouldShowWelcomeNotification(nowMillis: Long, cooldownMillis: Long): Boolean = false
    override suspend fun setLastWelcomeNotificationAt(timestampMillis: Long) = Unit
}

private class FakeAuthUserRepository : UserRepository {
    override suspend fun getCurrentUser(uid: String?): FittyUser? = testUser(uid = uid ?: "uid", guest = true)
    override suspend fun updateProfile(uid: String, profile: FittyProfile): Result<Unit> = Result.success(Unit)
    override suspend fun updateOnboarding(uid: String, onboarding: FittyOnboarding): Result<Unit> = Result.success(Unit)
    override suspend fun updateSettings(uid: String, settings: FittySettings): Result<Unit> = Result.success(Unit)
    override suspend fun updateGoal(uid: String, primaryGoal: String, targetWeightKg: Int?): Result<Unit> = Result.success(Unit)
    override suspend fun updateStats(uid: String, stats: FittyStats): Result<Unit> = Result.success(Unit)
    override suspend fun deleteUserData(uid: String): Result<Unit> = Result.success(Unit)
    override suspend fun updateDisplayName(uid: String, name: String): Result<Unit> = Result.success(Unit)
    override suspend fun uploadProfilePhoto(uid: String, imageUri: String): Result<String> = Result.success(imageUri)
}

private class FakeNotificationTokenRepository : NotificationTokenRepository {
    override suspend fun syncNotificationToken(token: String) = Unit
}

private class FakeAuthNotificationRepository : AppNotificationRepository {
    override fun observeNotifications(): Flow<List<AppNotification>> = flowOf(emptyList())
    override fun observeUnreadCount(): Flow<Int> = flowOf(0)
    override suspend fun addNotification(title: String, message: String, type: AppNotificationType) = Unit
    override suspend fun markAsRead(notificationId: Long) = Unit
    override suspend fun markAllAsRead() = Unit
    override suspend fun deleteNotification(notificationId: Long) = Unit
}

private fun testUser(
    uid: String = "uid",
    onboardingCompleted: Boolean = false,
    guest: Boolean = false
): FittyUser = FittyUser(
    uid = uid,
    email = "$uid@example.com",
    displayName = "Fitty User",
    username = "fitty",
    authProvider = if (guest) "guest" else "password",
    guest = guest,
    onboardingCompleted = onboardingCompleted
)
