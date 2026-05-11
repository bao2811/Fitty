package com.example.fitty.domain.usecase.plan

import com.example.fitty.domain.model.Exercise
import com.example.fitty.domain.model.PlanInstance
import com.example.fitty.domain.model.ProgramTemplate
import com.example.fitty.domain.model.ScheduledWorkout
import com.example.fitty.domain.repository.PlanRepository
import com.example.fitty.domain.repository.SessionRepository
import javax.inject.Inject

class GetCurrentPlanUseCase @Inject constructor(
    private val planRepository: PlanRepository,
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(): PlanInstance? {
        val uid = sessionRepository.getCurrentUserId() ?: return null
        return planRepository.getActivePlan(uid)
    }
}

class GetScheduledWorkoutsUseCase @Inject constructor(
    private val planRepository: PlanRepository,
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(planId: String, dateKey: String? = null): List<ScheduledWorkout> {
        val uid = sessionRepository.getCurrentUserId() ?: return emptyList()
        return planRepository.getScheduledWorkouts(uid, planId, dateKey)
    }
}

class GetExerciseLibraryUseCase @Inject constructor(
    private val planRepository: PlanRepository
) {
    suspend operator fun invoke(
        query: String = "",
        muscleGroup: String? = null,
        difficulty: String? = null,
        equipment: String? = null
    ): List<Exercise> {
        return if (query.isBlank() && muscleGroup == null && difficulty == null && equipment == null) {
            planRepository.getExerciseLibrary()
        } else {
            planRepository.searchExercises(query, muscleGroup, difficulty, equipment)
        }
    }
}

class GetProgramTemplatesUseCase @Inject constructor(
    private val planRepository: PlanRepository
) {
    suspend operator fun invoke(
        goal: String? = null,
        difficulty: String? = null,
        equipment: String? = null
    ): List<ProgramTemplate> {
        return planRepository.getProgramTemplates(goal, difficulty, equipment)
    }
}

class SavePlanInstanceUseCase @Inject constructor(
    private val planRepository: PlanRepository,
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(plan: PlanInstance): Result<String> {
        val uid = sessionRepository.getCurrentUserId()
            ?: return Result.failure(IllegalStateException("Not signed in"))
        return planRepository.savePlanInstance(uid, plan)
    }
}

class SaveScheduledWorkoutUseCase @Inject constructor(
    private val planRepository: PlanRepository,
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(planId: String, workout: ScheduledWorkout): Result<String> {
        val uid = sessionRepository.getCurrentUserId()
            ?: return Result.failure(IllegalStateException("Not signed in"))
        return planRepository.saveScheduledWorkout(uid, planId, workout)
    }
}
