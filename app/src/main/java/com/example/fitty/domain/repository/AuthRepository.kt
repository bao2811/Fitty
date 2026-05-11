package com.example.fitty.domain.repository

import com.example.fitty.domain.model.FittyAuthResult

interface AuthRepository {
    suspend fun createPasswordUser(username: String, email: String, password: String): FittyAuthResult
    suspend fun signInWithPassword(identifier: String, password: String): FittyAuthResult
    suspend fun signInWithGoogle(idToken: String): FittyAuthResult
    suspend fun continueAsGuest(): FittyAuthResult
    suspend fun sendPasswordReset(email: String): Result<Unit>
    suspend fun deleteAccount(): Result<Unit>
    suspend fun reauthenticateWithPassword(password: String): Result<Unit>
    suspend fun reauthenticateWithGoogle(idToken: String): Result<Unit>
    fun signOut()
}
