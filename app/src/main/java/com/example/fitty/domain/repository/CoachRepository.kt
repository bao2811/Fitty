package com.example.fitty.domain.repository

import com.example.fitty.domain.model.CoachMessage
import com.example.fitty.domain.model.CoachThread

interface CoachRepository {
    suspend fun getOrCreateThread(uid: String): CoachThread
    suspend fun getThread(uid: String, threadId: String): CoachThread?
    suspend fun getThreads(uid: String): List<CoachThread>

    suspend fun getMessages(uid: String, threadId: String, limit: Int = 50): List<CoachMessage>
    suspend fun saveMessage(uid: String, threadId: String, message: CoachMessage): Result<String>

    suspend fun updateThreadPreview(uid: String, threadId: String, preview: String, messageCount: Int): Result<Unit>
}
