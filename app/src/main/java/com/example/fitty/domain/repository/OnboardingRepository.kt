package com.example.fitty.domain.repository

import com.example.fitty.domain.model.FittyOnboardingAnswers

interface OnboardingRepository {
    suspend fun saveOnboardingAnswers(uid: String, answers: FittyOnboardingAnswers): Result<Unit>
    suspend fun markOnboardingCompleted(uid: String): Result<Unit>
}
