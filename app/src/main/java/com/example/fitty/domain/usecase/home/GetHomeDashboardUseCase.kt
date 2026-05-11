package com.example.fitty.domain.usecase.home

import com.example.fitty.domain.model.DailySummary
import com.example.fitty.domain.model.FittyUser
import com.example.fitty.domain.model.ScheduledWorkout
import com.example.fitty.domain.repository.PlanRepository
import com.example.fitty.domain.repository.SessionRepository
import com.example.fitty.domain.repository.TrackingRepository
import com.example.fitty.domain.repository.UserRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class HomeDashboard(
    val user: FittyUser,
    val dailySummary: DailySummary?,
    val todayWorkout: ScheduledWorkout?,
    val activePlanName: String?
)

class GetHomeDashboardUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val planRepository: PlanRepository,
    private val trackingRepository: TrackingRepository,
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(): HomeDashboard? {
        val uid = sessionRepository.getCurrentUserId() ?: return null
        val user = userRepository.getCurrentUser(uid) ?: return null
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

        val dailySummary = trackingRepository.getDailySummary(uid, today)

        val activePlanId = user.stats.activePlanId
        var todayWorkout: ScheduledWorkout? = null
        var activePlanName: String? = null

        if (activePlanId.isNotBlank()) {
            val plan = planRepository.getPlanInstance(uid, activePlanId)
            activePlanName = plan?.name
            val workouts = planRepository.getScheduledWorkouts(uid, activePlanId, today)
            todayWorkout = workouts.firstOrNull { it.status == "scheduled" }
        }

        return HomeDashboard(
            user = user,
            dailySummary = dailySummary,
            todayWorkout = todayWorkout,
            activePlanName = activePlanName
        )
    }
}
