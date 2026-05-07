package com.example.fitty.data.firebase

import com.example.fitty.domain.repository.NotificationTokenRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseNotificationTokenRepository @Inject constructor(
    private val remoteDataSource: FirebaseUserRemoteDataSource
) : NotificationTokenRepository {
    override suspend fun syncNotificationToken(token: String) {
        remoteDataSource.syncNotificationToken(token)
    }
}
