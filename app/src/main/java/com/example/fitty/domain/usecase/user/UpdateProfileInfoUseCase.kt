package com.example.fitty.domain.usecase.user

import com.example.fitty.domain.repository.SessionRepository
import com.example.fitty.domain.repository.UserRepository
import javax.inject.Inject

class UpdateProfileInfoUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val sessionRepository: SessionRepository
) {
    suspend fun updateName(name: String): Result<Unit> {
        val uid = sessionRepository.getCurrentUserId()
            ?: return Result.failure(IllegalStateException("Not signed in"))
        return userRepository.updateDisplayName(uid, name)
    }

    suspend fun uploadAvatar(imageUri: String): Result<String> {
        val uid = sessionRepository.getCurrentUserId()
            ?: return Result.failure(IllegalStateException("Not signed in"))
        return userRepository.uploadProfilePhoto(uid, imageUri)
    }
}
