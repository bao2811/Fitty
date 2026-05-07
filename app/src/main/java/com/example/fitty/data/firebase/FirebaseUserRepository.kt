package com.example.fitty.data.firebase

import com.example.fitty.domain.model.FittyUser
import com.example.fitty.domain.repository.UserRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseUserRepository @Inject constructor(
    private val remoteDataSource: FirebaseUserRemoteDataSource
) : UserRepository {
    override suspend fun getCurrentUser(uid: String?): FittyUser? = remoteDataSource.getCurrentUser(uid)
}
