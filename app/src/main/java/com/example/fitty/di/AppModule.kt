package com.example.fitty.di

import android.content.Context
import com.example.fitty.data.firebase.FirebaseAuthRepository
import com.example.fitty.data.firebase.FirebaseNotificationTokenRepository
import com.example.fitty.data.firebase.FirebaseOnboardingRepository
import com.example.fitty.data.firebase.FirebaseStartupRepository
import com.example.fitty.data.firebase.FirebaseUserRepository
import com.example.fitty.data.preferences.AppPreferencesDataSource
import com.example.fitty.data.preferences.PreferencesSessionRepository
import com.example.fitty.domain.repository.AuthRepository
import com.example.fitty.domain.repository.NotificationTokenRepository
import com.example.fitty.domain.repository.OnboardingRepository
import com.example.fitty.domain.repository.SessionRepository
import com.example.fitty.domain.repository.StartupRepository
import com.example.fitty.domain.repository.UserRepository
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
    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: FirebaseAuthRepository): AuthRepository

    @Binds
    @Singleton
    abstract fun bindStartupRepository(impl: FirebaseStartupRepository): StartupRepository

    @Binds
    @Singleton
    abstract fun bindOnboardingRepository(impl: FirebaseOnboardingRepository): OnboardingRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: FirebaseUserRepository): UserRepository

    @Binds
    @Singleton
    abstract fun bindNotificationTokenRepository(
        impl: FirebaseNotificationTokenRepository
    ): NotificationTokenRepository

    @Binds
    @Singleton
    abstract fun bindSessionRepository(impl: PreferencesSessionRepository): SessionRepository
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideAppPreferencesDataSource(
        @ApplicationContext context: Context
    ): AppPreferencesDataSource = AppPreferencesDataSource(context)

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseMessaging(): FirebaseMessaging = FirebaseMessaging.getInstance()
}
