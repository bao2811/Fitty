package com.example.fitty.domain.repository

import com.example.fitty.domain.model.FittyAuthResult

interface AuthRepository {
    suspend fun createPasswordUser(username: String, email: String, password: String): FittyAuthResult
    suspend fun signInWithPassword(identifier: String, password: String): FittyAuthResult
    suspend fun signInWithGoogle(idToken: String): FittyAuthResult
    suspend fun continueAsGuest(): FittyAuthResult
    fun signOut()
}
