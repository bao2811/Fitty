package com.example.fitty.domain.usecase.startup

import androidx.test.core.app.ApplicationProvider
import com.example.fitty.domain.model.AppNotification
import com.example.fitty.domain.model.AppNotificationType
import com.example.fitty.domain.model.FittyAuthResult
import com.example.fitty.domain.model.FittyOnboarding
import com.example.fitty.domain.model.FittyProfile
import com.example.fitty.domain.model.FittySettings
import com.example.fitty.domain.model.FittyStartupState
import com.example.fitty.domain.model.FittyStats
import com.example.fitty.domain.model.FittyUser
import com.example.fitty.domain.model.StartupDestination
import com.example.fitty.domain.repository.AppNotificationRepository
import com.example.fitty.domain.repository.AuthRepository
import com.example.fitty.domain.repository.NotificationTokenRepository
import com.example.fitty.domain.repository.SessionRepository
import com.example.fitty.domain.repository.StartupRepository
import com.example.fitty.domain.repository.UserRepository
import com.example.fitty.domain.usecase.user.UpdateGoalUseCase
import com.example.fitty.domain.usecase.user.UpdateProfileInfoUseCase
import com.example.fitty.domain.usecase.user.UpdateProfileUseCase
import com.example.fitty.domain.usecase.user.UpdateSettingsUseCase
import com.example.fitty.domain.usecase.user.UpdateStreakUseCase
import com.example.fitty.notifications.FittyBannerController
import com.example.fitty.notifications.FittyMessagingCoordinator
import com.example.fitty.notifications.FittyNotificationDispatcher
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [35])
class StartupAndProfileUseCasesTest {

    @Test
    fun `resolve startup sends completed guest to main and persists startup state`() = runTest {
        val startupState = FittyStartupState(
            uid = "guest",
            displayName = "Guest",
            isGuest = true,
            isSignedIn = true,
            onboardingCompleted = true
        )
        val sessionRepository = FakeStartupSessionRepository()
        val useCase = startupUseCase(startupState = startupState, sessionRepository = sessionRepository)

        val destination = useCase()

        assertEquals(StartupDestination.Main, destination)
        assertEquals(startupState, sessionRepository.savedStartupState)
        assertFalse(sessionRepository.cleared)
    }

    @Test
    fun `resolve startup sends signed in incomplete session to onboarding`() = runTest {
        val useCase = startupUseCase(
            startupState = FittyStartupState(
                uid = "uid",
                displayName = "Fitty",
                isGuest = true,
                isSignedIn = true,
                onboardingCompleted = false
            )
        )

        assertEquals(StartupDestination.Onboarding, useCase())
    }

    @Test
    fun `resolve startup clears expired signed in session and returns sign in`() = runTest {
        val sessionRepository = FakeStartupSessionRepository(expired = true)
        val authRepository = FakeStartupAuthRepository()
        val useCase = startupUseCase(
            startupState = FittyStartupState(uid = "uid", isSignedIn = true, onboardingCompleted = true),
            sessionRepository = sessionRepository,
            authRepository = authRepository
        )

        val destination = useCase()

        assertEquals(StartupDestination.SignIn, destination)
        assertTrue(authRepository.signedOut)
        assertTrue(sessionRepository.cleared)
    }

    @Test
    fun `profile use cases fail when no session exists`() = runTest {
        val sessionRepository = FakeStartupSessionRepository(currentUserId = null)
        val userRepository = FakeStartupUserRepository()

        assertTrue(UpdateProfileInfoUseCase(userRepository, sessionRepository).updateName("Fitty").isFailure)
        assertTrue(UpdateProfileUseCase(userRepository, sessionRepository)(FittyProfile()).isFailure)
        assertTrue(UpdateSettingsUseCase(userRepository, sessionRepository)(FittySettings()).isFailure)
        assertTrue(UpdateGoalUseCase(userRepository, sessionRepository)("gain_strength", 80).isFailure)
    }

    @Test
    fun `profile use cases call user repository with current uid`() = runTest {
        val sessionRepository = FakeStartupSessionRepository(currentUserId = "uid")
        val userRepository = FakeStartupUserRepository()

        UpdateProfileInfoUseCase(userRepository, sessionRepository).updateName("New Name")
        UpdateProfileInfoUseCase(userRepository, sessionRepository).uploadAvatar("content://avatar")
        UpdateProfileUseCase(userRepository, sessionRepository)(
            profile = FittyProfile(heightCm = 175),
            onboarding = FittyOnboarding(preferredTime = "morning")
        )
        UpdateSettingsUseCase(userRepository, sessionRepository)(FittySettings(language = "en"))
        UpdateGoalUseCase(userRepository, sessionRepository)("lose_weight", 68)

        assertEquals("uid" to "New Name", userRepository.displayNameUpdate)
        assertEquals("uid" to "content://avatar", userRepository.avatarUpload)
        assertEquals(175, userRepository.profileUpdate?.second?.heightCm)
        assertEquals("morning", userRepository.onboardingUpdate?.second?.preferredTime)
        assertEquals("en", userRepository.settingsUpdate?.second?.language)
        assertEquals(Triple("uid", "lose_weight", 68), userRepository.goalUpdate)
    }

    private fun startupUseCase(
        startupState: FittyStartupState,
        sessionRepository: FakeStartupSessionRepository = FakeStartupSessionRepository(),
        authRepository: FakeStartupAuthRepository = FakeStartupAuthRepository()
    ): ResolveStartupDestinationUseCase {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val userRepository = FakeStartupUserRepository()
        val notificationRepository = FakeStartupNotificationRepository()
        val messagingCoordinator = FittyMessagingCoordinator(
            firebaseMessaging = FirebaseMessaging.getInstance(),
            notificationTokenRepository = FakeStartupNotificationTokenRepository(),
            sessionRepository = sessionRepository,
            notificationDispatcher = FittyNotificationDispatcher(
                context = context,
                bannerController = FittyBannerController(),
                appNotificationRepository = notificationRepository
            ),
            updateStreakUseCase = UpdateStreakUseCase(userRepository, sessionRepository)
        )
        return ResolveStartupDestinationUseCase(
            startupRepository = FakeStartupRepository(startupState),
            sessionRepository = sessionRepository,
            authRepository = authRepository,
            messagingCoordinator = messagingCoordinator
        )
    }
}

private class FakeStartupRepository(private val state: FittyStartupState) : StartupRepository {
    override suspend fun getStartupState(): FittyStartupState = state
}

private class FakeStartupAuthRepository : AuthRepository {
    var signedOut = false

    override suspend fun createPasswordUser(username: String, email: String, password: String): FittyAuthResult = FittyAuthResult()
    override suspend fun signInWithPassword(identifier: String, password: String): FittyAuthResult = FittyAuthResult()
    override suspend fun signInWithGoogle(idToken: String): FittyAuthResult = FittyAuthResult()
    override suspend fun continueAsGuest(): FittyAuthResult = FittyAuthResult()
    override suspend fun sendPasswordReset(email: String): Result<Unit> = Result.success(Unit)
    override suspend fun deleteAccount(): Result<Unit> = Result.success(Unit)
    override suspend fun reauthenticateWithPassword(password: String): Result<Unit> = Result.success(Unit)
    override suspend fun reauthenticateWithGoogle(idToken: String): Result<Unit> = Result.success(Unit)
    override fun signOut() {
        signedOut = true
    }
}

private class FakeStartupSessionRepository(
    private val currentUserId: String? = "uid",
    private val expired: Boolean = false
) : SessionRepository {
    var savedStartupState: FittyStartupState? = null
    var cleared = false

    override suspend fun saveStartupState(state: FittyStartupState) {
        savedStartupState = state
    }
    override suspend fun saveUserSession(user: FittyUser) = Unit
    override suspend fun setOnboardingCompleted(completed: Boolean) = Unit
    override suspend fun getCurrentUserId(): String? = currentUserId
    override fun observeCurrentUserId(): Flow<String?> = MutableStateFlow(currentUserId)
    override suspend fun getAppLanguage(): String? = "en"
    override suspend fun setAppLanguage(language: String) = Unit
    override suspend fun clearSession() {
        cleared = true
    }
    override suspend fun isSignedInSessionExpired(nowMillis: Long, maxAgeMillis: Long): Boolean = expired
    override suspend fun shouldShowWelcomeNotification(nowMillis: Long, cooldownMillis: Long): Boolean = false
    override suspend fun setLastWelcomeNotificationAt(timestampMillis: Long) = Unit
}

private class FakeStartupUserRepository : UserRepository {
    var displayNameUpdate: Pair<String, String>? = null
    var avatarUpload: Pair<String, String>? = null
    var profileUpdate: Pair<String, FittyProfile>? = null
    var onboardingUpdate: Pair<String, FittyOnboarding>? = null
    var settingsUpdate: Pair<String, FittySettings>? = null
    var goalUpdate: Triple<String, String, Int?>? = null

    override suspend fun getCurrentUser(uid: String?): FittyUser? = FittyUser(
        uid = uid ?: "uid",
        email = "fitty@example.com",
        displayName = "Fitty",
        username = "fitty",
        authProvider = "password",
        guest = false,
        onboardingCompleted = true
    )
    override suspend fun updateProfile(uid: String, profile: FittyProfile): Result<Unit> {
        profileUpdate = uid to profile
        return Result.success(Unit)
    }
    override suspend fun updateOnboarding(uid: String, onboarding: FittyOnboarding): Result<Unit> {
        onboardingUpdate = uid to onboarding
        return Result.success(Unit)
    }
    override suspend fun updateSettings(uid: String, settings: FittySettings): Result<Unit> {
        settingsUpdate = uid to settings
        return Result.success(Unit)
    }
    override suspend fun updateGoal(uid: String, primaryGoal: String, targetWeightKg: Int?): Result<Unit> {
        goalUpdate = Triple(uid, primaryGoal, targetWeightKg)
        return Result.success(Unit)
    }
    override suspend fun updateStats(uid: String, stats: FittyStats): Result<Unit> = Result.success(Unit)
    override suspend fun deleteUserData(uid: String): Result<Unit> = Result.success(Unit)
    override suspend fun updateDisplayName(uid: String, name: String): Result<Unit> {
        displayNameUpdate = uid to name
        return Result.success(Unit)
    }
    override suspend fun uploadProfilePhoto(uid: String, imageUri: String): Result<String> {
        avatarUpload = uid to imageUri
        return Result.success("https://avatar")
    }
}

private class FakeStartupNotificationTokenRepository : NotificationTokenRepository {
    override suspend fun syncNotificationToken(token: String) = Unit
}

private class FakeStartupNotificationRepository : AppNotificationRepository {
    override fun observeNotifications(): Flow<List<AppNotification>> = MutableStateFlow(emptyList())
    override fun observeUnreadCount(): Flow<Int> = MutableStateFlow(0)
    override suspend fun addNotification(title: String, message: String, type: AppNotificationType) = Unit
    override suspend fun markAsRead(notificationId: Long) = Unit
    override suspend fun markAllAsRead() = Unit
    override suspend fun deleteNotification(notificationId: Long) = Unit
}
