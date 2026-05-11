package com.example.fitty.di

import android.content.Context
import com.example.fitty.data.firebase.FirebaseAuthRepository
import com.example.fitty.data.firebase.FirebaseCoachRepository
import com.example.fitty.data.firebase.FirebaseNotificationTokenRepository
import com.example.fitty.data.firebase.FirebaseOnboardingRepository
import com.example.fitty.data.firebase.FirebasePlanRepository
import com.example.fitty.data.firebase.FirebaseStartupRepository
import com.example.fitty.data.firebase.FirebaseTrackingRepository
import com.example.fitty.data.firebase.FirebaseUserRepository
import com.example.fitty.data.firebase.FirebaseWorkoutSessionRepository
import com.example.fitty.data.remote.GeminiCoachEngine
import com.example.fitty.data.remote.GeminiMealAnalysisEngine
import com.example.fitty.data.remote.GeminiBodyScanAnalysisEngine
import com.example.fitty.data.preferences.AppPreferencesDataSource
import com.example.fitty.data.preferences.PreferencesSessionRepository
import com.example.fitty.domain.model.BodyScanAnalysisEngine
import com.example.fitty.domain.model.CoachEngine
import com.example.fitty.domain.model.MealAnalysisEngine
import com.example.fitty.domain.repository.AuthRepository
import com.example.fitty.domain.repository.CoachRepository
import com.example.fitty.domain.repository.NotificationTokenRepository
import com.example.fitty.domain.repository.OnboardingRepository
import com.example.fitty.domain.repository.PlanRepository
import com.example.fitty.domain.repository.SessionRepository
import com.example.fitty.domain.repository.StartupRepository
import com.example.fitty.domain.repository.TrackingRepository
import com.example.fitty.domain.repository.UserRepository
import com.example.fitty.domain.repository.WorkoutSessionRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton
    abstract fun bindAuthRepository(impl: FirebaseAuthRepository): AuthRepository

    @Binds @Singleton
    abstract fun bindStartupRepository(impl: FirebaseStartupRepository): StartupRepository

    @Binds @Singleton
    abstract fun bindOnboardingRepository(impl: FirebaseOnboardingRepository): OnboardingRepository

    @Binds @Singleton
    abstract fun bindUserRepository(impl: FirebaseUserRepository): UserRepository

    @Binds @Singleton
    abstract fun bindNotificationTokenRepository(impl: FirebaseNotificationTokenRepository): NotificationTokenRepository

    @Binds @Singleton
    abstract fun bindSessionRepository(impl: PreferencesSessionRepository): SessionRepository

    @Binds @Singleton
    abstract fun bindPlanRepository(impl: FirebasePlanRepository): PlanRepository

    @Binds @Singleton
    abstract fun bindTrackingRepository(impl: FirebaseTrackingRepository): TrackingRepository

    @Binds @Singleton
    abstract fun bindCoachRepository(impl: FirebaseCoachRepository): CoachRepository

    @Binds @Singleton
    abstract fun bindWorkoutSessionRepository(impl: FirebaseWorkoutSessionRepository): WorkoutSessionRepository

    @Binds @Singleton
    abstract fun bindCoachEngine(impl: GeminiCoachEngine): CoachEngine

    @Binds @Singleton
    abstract fun bindMealAnalysisEngine(impl: GeminiMealAnalysisEngine): MealAnalysisEngine

    @Binds @Singleton
    abstract fun bindBodyScanAnalysisEngine(impl: GeminiBodyScanAnalysisEngine): BodyScanAnalysisEngine
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton
    fun provideAppPreferencesDataSource(
        @ApplicationContext context: Context
    ): AppPreferencesDataSource = AppPreferencesDataSource(context)

    @Provides @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides @Singleton
    fun provideFirebaseMessaging(): FirebaseMessaging = FirebaseMessaging.getInstance()
}
