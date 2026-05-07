package com.example.fitty.domain.usecase.user

import com.example.fitty.domain.model.FittyUser
import com.example.fitty.domain.repository.UserRepository
import javax.inject.Inject

class GetCurrentUserUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(uid: String? = null): FittyUser? = userRepository.getCurrentUser(uid)
}
