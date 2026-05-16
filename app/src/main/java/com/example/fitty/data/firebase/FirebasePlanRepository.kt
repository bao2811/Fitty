package com.example.fitty.data.firebase

import com.example.fitty.domain.model.*
import com.example.fitty.domain.repository.PlanRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebasePlanRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : PlanRepository {

    private fun userPlans(uid: String) = firestore.collection("users").document(uid).collection("plan_instances")
    private fun scheduledWorkouts(uid: String, planId: String) = userPlans(uid).document(planId).collection("scheduled_workouts")

    override suspend fun getActivePlan(uid: String): PlanInstance? {
        return userPlans(uid).whereEqualTo("status", "active").limit(1).get().await()
            .documents.firstOrNull()?.toPlanInstance()
    }

    override suspend fun getPlanInstance(uid: String, planId: String): PlanInstance? {
        val doc = userPlans(uid).document(planId).get().await()
        return if (doc.exists()) doc.toPlanInstance() else null
    }

    override suspend fun getAllPlans(uid: String): List<PlanInstance> =
        userPlans(uid).orderBy("updatedAt", Query.Direction.DESCENDING).get().await()
            .documents.mapNotNull { it.toPlanInstance() }

    override suspend fun savePlanInstance(uid: String, plan: PlanInstance): Result<String> = try {
        val ref = if (plan.id.isBlank()) userPlans(uid).document() else userPlans(uid).document(plan.id)
        ref.set(plan.toMap(), SetOptions.merge()).await()
        Result.success(ref.id)
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun updatePlanStatus(uid: String, planId: String, status: String): Result<Unit> = try {
        userPlans(uid).document(planId).update("status", status, "updatedAt", FieldValue.serverTimestamp()).await()
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun deletePlan(uid: String, planId: String): Result<Unit> = try {
        userPlans(uid).document(planId).delete().await(); Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun getScheduledWorkouts(uid: String, planId: String, dateKey: String?): List<ScheduledWorkout> {
        var q: Query = scheduledWorkouts(uid, planId)
        if (dateKey != null) q = q.whereEqualTo("dateKey", dateKey)
        return q.orderBy("orderInWeek").get().await().documents.mapNotNull { it.toScheduledWorkout(planId) }
    }

    override suspend fun getScheduledWorkout(uid: String, planId: String, workoutId: String): ScheduledWorkout? {
        val doc = scheduledWorkouts(uid, planId).document(workoutId).get().await()
        return if (doc.exists()) doc.toScheduledWorkout(planId) else null
    }

    override suspend fun saveScheduledWorkout(uid: String, planId: String, workout: ScheduledWorkout): Result<String> = try {
        val ref = if (workout.id.isBlank()) scheduledWorkouts(uid, planId).document() else scheduledWorkouts(uid, planId).document(workout.id)
        ref.set(workout.toMap(), SetOptions.merge()).await(); Result.success(ref.id)
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun updateScheduledWorkoutStatus(uid: String, planId: String, workoutId: String, status: String): Result<Unit> = try {
        scheduledWorkouts(uid, planId).document(workoutId).update("status", status, "updatedAt", FieldValue.serverTimestamp()).await()
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun replaceScheduledWorkout(uid: String, planId: String, workoutId: String, newWorkout: ScheduledWorkout): Result<String> = try {
        updateScheduledWorkoutStatus(uid, planId, workoutId, "replaced")
        saveScheduledWorkout(uid, planId, newWorkout.copy(replacedFromWorkoutId = workoutId))
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun getExerciseLibrary(): List<Exercise> =
        firestore.collection("exercises").get().await().documents.mapNotNull { it.toExercise() }

    override suspend fun getExercise(exerciseId: String): Exercise? {
        val doc = firestore.collection("exercises").document(exerciseId).get().await()
        return if (doc.exists()) doc.toExercise() else null
    }

    override suspend fun searchExercises(query: String, muscleGroup: String?, difficulty: String?, equipment: String?): List<Exercise> {
        return getExerciseLibrary().filter { ex ->
            (query.isBlank() || ex.name.contains(query, true) || ex.primaryMuscleGroup.contains(query, true)) &&
            (muscleGroup == null || ex.primaryMuscleGroup.equals(muscleGroup, true)) &&
            (difficulty == null || ex.difficulty.equals(difficulty, true)) &&
            (equipment == null || ex.equipment.equals(equipment, true))
        }
    }

    override suspend fun getProgramTemplates(goal: String?, difficulty: String?, equipment: String?): List<ProgramTemplate> {
        var q: Query = firestore.collection("program_templates")
        if (goal != null) q = q.whereEqualTo("goal", goal)
        if (difficulty != null) q = q.whereEqualTo("difficulty", difficulty)
        return q.get().await().documents.mapNotNull { it.toProgramTemplate() }
    }

    override suspend fun getProgramTemplate(programId: String): ProgramTemplate? {
        val doc = firestore.collection("program_templates").document(programId).get().await()
        return if (doc.exists()) doc.toProgramTemplate() else null
    }

    // Mapping helpers
    @Suppress("UNCHECKED_CAST")
    private fun DocumentSnapshot.toPlanInstance(): PlanInstance? {
        if (!exists()) return null
        return PlanInstance(id = id, sourceProgramId = getString("sourceProgramId").orEmpty(), name = getString("name").orEmpty(),
            goal = getString("goal").orEmpty(), durationWeeks = getLong("durationWeeks")?.toInt() ?: 4,
            workoutsPerWeek = getLong("workoutsPerWeek")?.toInt() ?: 3, equipment = getString("equipment").orEmpty(),
            trainingStyle = getString("trainingStyle").orEmpty(), status = getString("status").orEmpty(),
            explanation = getString("explanation").orEmpty(), currentWeek = getLong("currentWeek")?.toInt() ?: 1,
            nextWorkoutDate = getString("nextWorkoutDate").orEmpty())
    }

    @Suppress("UNCHECKED_CAST")
    private fun DocumentSnapshot.toScheduledWorkout(planId: String): ScheduledWorkout? {
        if (!exists()) return null
        val exList = (get("exercises") as? List<Map<String, Any?>>)?.map { m ->
            WorkoutExercise(exerciseId = m["exerciseId"] as? String ?: "", name = m["name"] as? String ?: "",
                sets = (m["sets"] as? Number)?.toInt() ?: 3, reps = m["reps"] as? String, durationSeconds = (m["durationSeconds"] as? Number)?.toInt())
        } ?: emptyList()
        return ScheduledWorkout(id = id, planId = planId, dateKey = getString("dateKey").orEmpty(),
            weekNumber = getLong("weekNumber")?.toInt() ?: 1, orderInWeek = getLong("orderInWeek")?.toInt() ?: 1,
            title = getString("title").orEmpty(), durationMinutes = getLong("durationMinutes")?.toInt() ?: 30,
            estimatedCalories = getLong("estimatedCalories")?.toInt() ?: 0, difficulty = getString("difficulty").orEmpty(),
            equipment = getString("equipment").orEmpty(), status = getString("status").orEmpty(),
            explanation = getString("explanation").orEmpty(), replacedFromWorkoutId = getString("replacedFromWorkoutId"), exercises = exList)
    }

    private fun DocumentSnapshot.toExercise(): Exercise? {
        if (!exists()) return null
        val bodyPart = getString("bodyPart").orEmpty().ifBlank { getString("primaryMuscleGroup").orEmpty() }
        val target = getString("target").orEmpty()
        val gifUrl = getString("gifUrl").orEmpty().ifBlank { getString("mediaUrl").orEmpty() }
        val updatedAt = when (val raw = get("updatedAt")) {
            is String -> raw
            is com.google.firebase.Timestamp -> raw.toDate().toInstant().toString()
            else -> ""
        }
        return Exercise(
            id = getString("id").orEmpty().ifBlank { id },
            name = getString("name").orEmpty(),
            bodyPart = bodyPart,
            target = target,
            description = getString("description").orEmpty(),
            difficulty = getString("difficulty").orEmpty(),
            primaryMuscleGroup = bodyPart,
            targetMuscles = (get("targetMuscles") as? List<*>)?.filterIsInstance<String>()
                ?: listOf(target).filter { it.isNotBlank() },
            equipment = getString("equipment").orEmpty(),
            gifUrl = gifUrl,
            gifStoragePath = getString("gifStoragePath").orEmpty(),
            gifVersion = getLong("gifVersion")?.toInt() ?: 0,
            updatedAt = updatedAt,
            defaultRepsText = getString("defaultRepsText").orEmpty(),
            defaultDurationSeconds = getLong("defaultDurationSeconds")?.toInt(),
            mediaUrl = gifUrl,
            mediaType = "gif",
            steps = (get("steps") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            mistakes = (get("mistakes") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            tips = (get("tips") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            variations = (get("variations") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        )
    }

    private fun DocumentSnapshot.toProgramTemplate(): ProgramTemplate? {
        if (!exists()) return null
        return ProgramTemplate(id = id, title = getString("title").orEmpty(), goal = getString("goal").orEmpty(),
            difficulty = getString("difficulty").orEmpty(), weeks = getLong("weeks")?.toInt() ?: 4,
            workoutsPerWeek = getLong("workoutsPerWeek")?.toInt() ?: 3, averageDurationMinutes = getLong("averageDurationMinutes")?.toInt() ?: 30,
            equipment = getString("equipment").orEmpty(), description = getString("description").orEmpty(),
            thumbnailUrl = getString("thumbnailUrl"), tags = (get("tags") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            explanationTemplate = getString("explanationTemplate").orEmpty())
    }

    private fun PlanInstance.toMap(): Map<String, Any?> = mapOf("sourceProgramId" to sourceProgramId, "name" to name, "goal" to goal,
        "durationWeeks" to durationWeeks, "workoutsPerWeek" to workoutsPerWeek, "equipment" to equipment, "trainingStyle" to trainingStyle,
        "status" to status, "explanation" to explanation, "currentWeek" to currentWeek, "nextWorkoutDate" to nextWorkoutDate, "updatedAt" to FieldValue.serverTimestamp())

    private fun ScheduledWorkout.toMap(): Map<String, Any?> = mapOf("dateKey" to dateKey, "weekNumber" to weekNumber, "orderInWeek" to orderInWeek,
        "title" to title, "durationMinutes" to durationMinutes, "estimatedCalories" to estimatedCalories, "difficulty" to difficulty,
        "equipment" to equipment, "status" to status, "explanation" to explanation, "replacedFromWorkoutId" to replacedFromWorkoutId,
        "exercises" to exercises.map { mapOf("exerciseId" to it.exerciseId, "name" to it.name, "sets" to it.sets, "reps" to it.reps, "durationSeconds" to it.durationSeconds) },
        "updatedAt" to FieldValue.serverTimestamp())
}
