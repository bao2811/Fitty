package com.example.fitty.domain.repository

import com.example.fitty.domain.model.Exercise
import com.example.fitty.domain.model.PlanInstance
import com.example.fitty.domain.model.ProgramTemplate
import com.example.fitty.domain.model.ScheduledWorkout

interface PlanRepository {
    suspend fun getActivePlan(uid: String): PlanInstance?
    suspend fun getPlanInstance(uid: String, planId: String): PlanInstance?
    suspend fun getAllPlans(uid: String): List<PlanInstance>
    suspend fun savePlanInstance(uid: String, plan: PlanInstance): Result<String>
    suspend fun updatePlanStatus(uid: String, planId: String, status: String): Result<Unit>
    suspend fun deletePlan(uid: String, planId: String): Result<Unit>

    suspend fun getScheduledWorkouts(uid: String, planId: String, dateKey: String? = null): List<ScheduledWorkout>
    suspend fun getScheduledWorkout(uid: String, planId: String, workoutId: String): ScheduledWorkout?
    suspend fun saveScheduledWorkout(uid: String, planId: String, workout: ScheduledWorkout): Result<String>
    suspend fun updateScheduledWorkoutStatus(uid: String, planId: String, workoutId: String, status: String): Result<Unit>
    suspend fun replaceScheduledWorkout(uid: String, planId: String, workoutId: String, newWorkout: ScheduledWorkout): Result<String>

    suspend fun getExerciseLibrary(): List<Exercise>
    suspend fun getExercise(exerciseId: String): Exercise?
    suspend fun searchExercises(query: String, muscleGroup: String? = null, difficulty: String? = null, equipment: String? = null): List<Exercise>

    suspend fun getProgramTemplates(goal: String? = null, difficulty: String? = null, equipment: String? = null): List<ProgramTemplate>
    suspend fun getProgramTemplate(programId: String): ProgramTemplate?
}
