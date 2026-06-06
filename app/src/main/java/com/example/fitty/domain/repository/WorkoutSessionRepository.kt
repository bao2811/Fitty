package com.example.fitty.domain.repository

import com.example.fitty.domain.model.ExerciseLog
import com.example.fitty.domain.model.WorkoutSession

interface WorkoutSessionRepository {
    suspend fun startSession(uid: String, session: WorkoutSession): Result<String>
    suspend fun getSession(uid: String, sessionId: String): WorkoutSession?
    suspend fun getActiveSessions(uid: String): List<WorkoutSession>
    suspend fun completeSession(
        uid: String,
        sessionId: String,
        durationMinutes: Int,
        caloriesBurned: Int,
        completionRate: Float,
        perceivedEffort: Int?,
        exercises: List<ExerciseLog>
    ): Result<Unit>
    suspend fun updateExerciseLog(uid: String, sessionId: String, exercise: ExerciseLog): Result<Unit>
    suspend fun abandonSession(uid: String, sessionId: String): Result<Unit>
    suspend fun getRecentSessions(uid: String, limit: Int = 10): List<WorkoutSession>
}
