package com.example.fitty.domain.repository

import com.example.fitty.domain.model.FittyStartupState

interface StartupRepository {
    suspend fun getStartupState(): FittyStartupState
}
