package com.example.fitty.domain.repository

import com.example.fitty.domain.model.FittyProfile
import com.example.fitty.domain.model.FittyOnboarding
import com.example.fitty.domain.model.FittySettings
import com.example.fitty.domain.model.FittyStats
import com.example.fitty.domain.model.FittyUser

interface UserRepository {
    suspend fun getCurrentUser(uid: String? = null): FittyUser?
    suspend fun updateProfile(uid: String, profile: FittyProfile): Result<Unit>
    suspend fun updateOnboarding(uid: String, onboarding: FittyOnboarding): Result<Unit>
    suspend fun updateSettings(uid: String, settings: FittySettings): Result<Unit>
    suspend fun updateGoal(uid: String, primaryGoal: String, targetWeightKg: Int?): Result<Unit>
    suspend fun updateStats(uid: String, stats: FittyStats): Result<Unit>
    suspend fun deleteUserData(uid: String): Result<Unit>
    suspend fun updateDisplayName(uid: String, name: String): Result<Unit>
    suspend fun uploadProfilePhoto(uid: String, imageUri: String): Result<String>
}
