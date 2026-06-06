package com.example.fitty.data.firebase

import com.example.fitty.domain.model.ExerciseLog
import com.example.fitty.domain.model.WorkoutSession
import com.example.fitty.domain.repository.WorkoutSessionRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseWorkoutSessionRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : WorkoutSessionRepository {

    private fun sessions(uid: String) = firestore.collection("users").document(uid).collection("workout_sessions")

    override suspend fun startSession(uid: String, session: WorkoutSession): Result<String> = try {
        val startedAt = session.startedAt.takeIf { it > 0L } ?: System.currentTimeMillis()
        val ref = sessions(uid).document()
        ref.set(mapOf("planId" to session.planId, "scheduledWorkoutId" to session.scheduledWorkoutId,
            "title" to session.title, "source" to session.source, "status" to "in_progress",
            "plannedExercises" to session.plannedExercises.map { exercise ->
                mapOf(
                    "exerciseId" to exercise.exerciseId,
                    "name" to exercise.name,
                    "sets" to exercise.sets,
                    "reps" to exercise.reps,
                    "durationSeconds" to exercise.durationSeconds,
                    "targetWeightKg" to exercise.targetWeightKg
                )
            },
            "startedAt" to startedAt, "createdAt" to startedAt,
            "updatedAt" to startedAt), SetOptions.merge()).await()
        // Save exercise logs as subcollection
        session.exercises.forEach { ex ->
            val exRef = ref.collection("exercise_logs").document()
            exRef.set(mapOf("exerciseId" to ex.exerciseId, "name" to ex.name, "orderIndex" to ex.orderIndex,
                "plannedSets" to ex.plannedSets, "completedSets" to 0, "repsBySet" to emptyList<Int>(),
                "weightKgBySet" to emptyList<Float>(), "completed" to false)).await()
        }
        Result.success(ref.id)
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun getSession(uid: String, sessionId: String): WorkoutSession? {
        val doc = sessions(uid).document(sessionId).get().await()
        if (!doc.exists()) return null
        val exDocs = sessions(uid).document(sessionId).collection("exercise_logs")
            .orderBy("orderIndex").get().await()
        val exercises = exDocs.documents.map { e ->
            @Suppress("UNCHECKED_CAST")
            ExerciseLog(id = e.id, exerciseId = e.getString("exerciseId").orEmpty(), name = e.getString("name").orEmpty(),
                orderIndex = e.getLong("orderIndex")?.toInt() ?: 0, plannedSets = e.getLong("plannedSets")?.toInt() ?: 0,
                completedSets = e.getLong("completedSets")?.toInt() ?: 0,
                repsBySet = (e.get("repsBySet") as? List<*>)?.mapNotNull { (it as? Number)?.toInt() } ?: emptyList(),
                weightKgBySet = (e.get("weightKgBySet") as? List<*>)?.mapNotNull { (it as? Number)?.toFloat() } ?: emptyList(),
                durationSeconds = e.getLong("durationSeconds")?.toInt(), completed = e.getBoolean("completed") ?: false)
        }
        @Suppress("UNCHECKED_CAST")
        val plannedExercises = (doc.get("plannedExercises") as? List<Map<String, Any?>>)?.map { exercise ->
            com.example.fitty.domain.model.WorkoutExercise(
                exerciseId = exercise["exerciseId"] as? String ?: "",
                name = exercise["name"] as? String ?: "",
                sets = (exercise["sets"] as? Number)?.toInt() ?: 0,
                reps = exercise["reps"] as? String,
                durationSeconds = (exercise["durationSeconds"] as? Number)?.toInt(),
                targetWeightKg = (exercise["targetWeightKg"] as? Number)?.toFloat()
            )
        } ?: emptyList()
        return WorkoutSession(id = doc.id, planId = doc.getString("planId").orEmpty(),
            scheduledWorkoutId = doc.getString("scheduledWorkoutId").orEmpty(), title = doc.getString("title").orEmpty(),
            source = doc.getString("source") ?: "plan", status = doc.getString("status") ?: "in_progress",
            startedAt = doc.getLong("startedAt") ?: 0L, endedAt = doc.getLong("endedAt"),
            durationMinutes = doc.getLong("durationMinutes")?.toInt() ?: 0,
            caloriesBurned = doc.getLong("caloriesBurned")?.toInt() ?: 0,
            completionRate = doc.getDouble("completionRate")?.toFloat() ?: 0f,
            perceivedEffort = doc.getLong("perceivedEffort")?.toInt(), notes = doc.getString("notes"),
            plannedExercises = plannedExercises,
            exercises = exercises)
    }

    override suspend fun getActiveSessions(uid: String): List<WorkoutSession> =
        sessions(uid).whereEqualTo("status", "in_progress").get().await()
            .documents.map { WorkoutSession(id = it.id, title = it.getString("title").orEmpty(), status = "in_progress") }

    override suspend fun completeSession(uid: String, sessionId: String, durationMinutes: Int,
        caloriesBurned: Int, completionRate: Float, perceivedEffort: Int?, exercises: List<ExerciseLog>): Result<Unit> = try {
        val endedAt = System.currentTimeMillis()
        sessions(uid).document(sessionId).set(mapOf("status" to "completed", "endedAt" to endedAt,
            "durationMinutes" to durationMinutes, "caloriesBurned" to caloriesBurned, "completionRate" to completionRate,
            "perceivedEffort" to perceivedEffort, "updatedAt" to endedAt), SetOptions.merge()).await()
        exercises.forEach { ex ->
            if (ex.id.isNotBlank()) {
                sessions(uid).document(sessionId).collection("exercise_logs").document(ex.id)
                    .update(mapOf("completedSets" to ex.completedSets, "repsBySet" to ex.repsBySet,
                        "weightKgBySet" to ex.weightKgBySet, "durationSeconds" to ex.durationSeconds,
                        "completed" to ex.completed)).await()
            }
        }
        // Note: stats update is handled by CompleteWorkoutSessionUseCase
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun updateExerciseLog(uid: String, sessionId: String, exercise: ExerciseLog): Result<Unit> = try {
        if (exercise.id.isBlank()) {
            throw IllegalArgumentException("Missing exercise log id")
        }

        val now = System.currentTimeMillis()
        sessions(uid).document(sessionId).collection("exercise_logs").document(exercise.id)
            .set(
                mapOf(
                    "exerciseId" to exercise.exerciseId,
                    "name" to exercise.name,
                    "orderIndex" to exercise.orderIndex,
                    "plannedSets" to exercise.plannedSets,
                    "completedSets" to exercise.completedSets,
                    "repsBySet" to exercise.repsBySet,
                    "weightKgBySet" to exercise.weightKgBySet,
                    "durationSeconds" to exercise.durationSeconds,
                    "completed" to exercise.completed,
                    "updatedAt" to now
                ),
                SetOptions.merge()
            ).await()
        sessions(uid).document(sessionId)
            .set(mapOf("updatedAt" to now), SetOptions.merge()).await()
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun abandonSession(uid: String, sessionId: String): Result<Unit> = try {
        sessions(uid).document(sessionId).update("status", "abandoned", "updatedAt", FieldValue.serverTimestamp()).await()
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun getRecentSessions(uid: String, limit: Int): List<WorkoutSession> =
        sessions(uid).orderBy("startedAt", Query.Direction.DESCENDING).limit(limit.toLong()).get().await()
            .documents.map { d -> WorkoutSession(id = d.id, title = d.getString("title").orEmpty(),
                status = d.getString("status") ?: "", durationMinutes = d.getLong("durationMinutes")?.toInt() ?: 0,
                caloriesBurned = d.getLong("caloriesBurned")?.toInt() ?: 0) }
}
