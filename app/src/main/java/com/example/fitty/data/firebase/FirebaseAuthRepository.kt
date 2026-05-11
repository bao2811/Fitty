package com.example.fitty.data.firebase

import com.example.fitty.domain.model.FittyAuthResult
import com.example.fitty.domain.repository.AuthRepository
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthRepository @Inject constructor(
    private val remoteDataSource: FirebaseUserRemoteDataSource,
    private val auth: FirebaseAuth
) : AuthRepository {
    override suspend fun createPasswordUser(
        username: String, email: String, password: String
    ): FittyAuthResult = remoteDataSource.createPasswordUser(username, email, password)

    override suspend fun signInWithPassword(
        identifier: String, password: String
    ): FittyAuthResult = remoteDataSource.signInWithPassword(identifier, password)

    override suspend fun signInWithGoogle(idToken: String): FittyAuthResult =
        remoteDataSource.signInWithGoogle(idToken)

    override suspend fun continueAsGuest(): FittyAuthResult = remoteDataSource.continueAsGuest()

    override suspend fun sendPasswordReset(email: String): Result<Unit> = try {
        auth.sendPasswordResetEmail(email).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deleteAccount(): Result<Unit> = try {
        auth.currentUser?.delete()?.await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun reauthenticateWithPassword(password: String): Result<Unit> = try {
        val user = auth.currentUser ?: throw IllegalStateException("Not signed in")
        val credential = EmailAuthProvider.getCredential(user.email.orEmpty(), password)
        user.reauthenticate(credential).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun reauthenticateWithGoogle(idToken: String): Result<Unit> = try {
        val user = auth.currentUser ?: throw IllegalStateException("Not signed in")
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        user.reauthenticate(credential).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override fun signOut() {
        remoteDataSource.signOut()
    }
}

