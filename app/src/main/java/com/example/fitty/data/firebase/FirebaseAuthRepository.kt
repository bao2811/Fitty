package com.example.fitty.data.firebase

import com.example.fitty.domain.model.FittyAuthResult
import com.example.fitty.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthRepository @Inject constructor(
    private val remoteDataSource: FirebaseUserRemoteDataSource
) : AuthRepository {
    override suspend fun createPasswordUser(
        username: String,
        email: String,
        password: String
    ): FittyAuthResult = remoteDataSource.createPasswordUser(username, email, password)

    override suspend fun signInWithPassword(
        identifier: String,
        password: String
    ): FittyAuthResult = remoteDataSource.signInWithPassword(identifier, password)

    override suspend fun continueAsGuest(): FittyAuthResult = remoteDataSource.continueAsGuest()

    override fun signOut() {
        remoteDataSource.signOut()
    }
}
