package com.example.fitty.data.firebase

import com.example.fitty.domain.model.FittyStartupState
import com.example.fitty.domain.repository.StartupRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseStartupRepository @Inject constructor(
    private val remoteDataSource: FirebaseUserRemoteDataSource
) : StartupRepository {
    override suspend fun getStartupState(): FittyStartupState = remoteDataSource.getStartupState()
}
