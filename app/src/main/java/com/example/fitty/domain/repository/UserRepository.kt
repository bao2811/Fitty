package com.example.fitty.domain.repository

import com.example.fitty.domain.model.FittyUser

interface UserRepository {
    suspend fun getCurrentUser(uid: String? = null): FittyUser?
}
