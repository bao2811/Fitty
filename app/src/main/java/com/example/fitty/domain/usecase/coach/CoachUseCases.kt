package com.example.fitty.domain.usecase.coach

import com.example.fitty.domain.model.CoachContext
import com.example.fitty.domain.model.CoachEngine
import com.example.fitty.domain.model.CoachMessage
import com.example.fitty.domain.model.CoachSuggestion
import com.example.fitty.domain.model.MealLog
import com.example.fitty.domain.repository.CoachRepository
import com.example.fitty.domain.repository.PlanRepository
import com.example.fitty.domain.repository.SessionRepository
import com.example.fitty.domain.repository.TrackingRepository
import com.example.fitty.domain.repository.UserRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class BuildCoachContextUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val planRepository: PlanRepository,
    private val trackingRepository: TrackingRepository,
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(): CoachContext {
        val uid = sessionRepository.getCurrentUserId() ?: return CoachContext()
        val user = userRepository.getCurrentUser(uid) ?: return CoachContext()
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val summary = trackingRepository.getDailySummary(uid, today)
        val activePlanId = user.stats.activePlanId
        val planName = if (activePlanId.isNotBlank()) {
            planRepository.getPlanInstance(uid, activePlanId)?.name
        } else null

        return CoachContext(
            userName = user.displayName,
            goal = user.profile.primaryGoal,
            fitnessLevel = user.profile.fitnessLevel,
            activePlanName = planName ?: "",
            todayWorkoutTitle = summary?.todayWorkoutTitle ?: "",
            currentStreak = user.stats.currentStreak,
            mealsLoggedToday = summary?.mealsLoggedCount ?: 0,
            latestWeight = user.profile.weightKg?.toFloat(),
            recentInsight = summary?.insightText ?: ""
        )
    }
}

class SendCoachMessageUseCase @Inject constructor(
    private val coachRepository: CoachRepository,
    private val coachEngine: CoachEngine,
    private val buildCoachContextUseCase: BuildCoachContextUseCase,
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(userText: String, threadId: String? = null): Result<Pair<CoachMessage, CoachMessage>> {
        val uid = sessionRepository.getCurrentUserId()
            ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            val thread = if (threadId != null) {
                coachRepository.getThread(uid, threadId) ?: coachRepository.getOrCreateThread(uid)
            } else {
                coachRepository.getOrCreateThread(uid)
            }

            // Save user message
            val userMessage = CoachMessage(
                role = "user",
                text = userText,
                threadId = thread.id,
                createdAt = System.currentTimeMillis()
            )
            coachRepository.saveMessage(uid, thread.id, userMessage)

            // Get existing messages for context
            val history = coachRepository.getMessages(uid, thread.id)
            val context = buildCoachContextUseCase()

            // Generate AI response
            val aiResponse = coachEngine.generateResponse(context, history, userText)
            val savedResponse = aiResponse.copy(threadId = thread.id)
            coachRepository.saveMessage(uid, thread.id, savedResponse)

            // Update thread preview
            coachRepository.updateThreadPreview(
                uid, thread.id,
                savedResponse.text.take(100),
                history.size + 2
            )

            Result.success(Pair(userMessage, savedResponse))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class ApplyCoachSuggestionUseCase @Inject constructor(
    private val planRepository: PlanRepository,
    private val trackingRepository: TrackingRepository,
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(suggestion: CoachSuggestion): Result<Unit> {
        val uid = sessionRepository.getCurrentUserId()
            ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            when (suggestion) {
                is CoachSuggestion.PlanAdjustment -> {
                    if (suggestion.targetPlanId.isNotBlank()) {
                        // Move workout from one date to another
                        val workouts = planRepository.getScheduledWorkouts(
                            uid, suggestion.targetPlanId, suggestion.moveFromDate
                        )
                        val workoutToMove = workouts.firstOrNull()
                        if (workoutToMove != null) {
                            val movedWorkout = workoutToMove.copy(
                                dateKey = suggestion.moveToDate,
                                status = "scheduled"
                            )
                            planRepository.saveScheduledWorkout(uid, suggestion.targetPlanId, movedWorkout)
                            planRepository.updateScheduledWorkoutStatus(
                                uid, suggestion.targetPlanId, workoutToMove.id, "replaced"
                            )
                        }
                    }
                    Result.success(Unit)
                }
                is CoachSuggestion.MealIdea -> {
                    val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                    val mealLog = MealLog(
                        mealType = suggestion.mealType.ifBlank { "snack" },
                        source = "coach",
                        dateKey = today,
                        totalCalories = suggestion.estimatedCalories,
                        totalProtein = suggestion.estimatedProtein,
                        notes = suggestion.description
                    )
                    trackingRepository.saveMealLog(uid, mealLog)
                    Result.success(Unit)
                }
                is CoachSuggestion.General -> Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
