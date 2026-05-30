package com.example.fitty.data.firebase

import com.example.fitty.domain.model.FittyOnboardingAnswers
import com.example.fitty.domain.repository.OnboardingRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseOnboardingRepository @Inject constructor(
    private val remoteDataSource: FirebaseUserRemoteDataSource
) : OnboardingRepository {
    override suspend fun saveOnboardingAnswers(uid: String, answers: FittyOnboardingAnswers): Result<Unit> =
        runCatching { remoteDataSource.saveOnboardingAnswers(uid, answers) }

    override suspend fun markOnboardingCompleted(uid: String): Result<Unit> =
        runCatching { remoteDataSource.markOnboardingCompleted(uid) }
}
