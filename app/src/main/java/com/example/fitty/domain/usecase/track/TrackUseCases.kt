package com.example.fitty.domain.usecase.track

import com.example.fitty.domain.model.BodyScan
import com.example.fitty.domain.model.BodyScanAnalysisEngine
import com.example.fitty.domain.model.BodyScanAnalysisResult
import com.example.fitty.domain.model.MealAnalysisEngine
import com.example.fitty.domain.model.MealAnalysisResult
import com.example.fitty.domain.model.MealLog
import com.example.fitty.domain.model.ProgressStats
import com.example.fitty.domain.repository.SessionRepository
import com.example.fitty.domain.repository.TrackingRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class AnalyzeMealImageUseCase @Inject constructor(
    private val mealAnalysisEngine: MealAnalysisEngine
) {
    suspend operator fun invoke(imageUri: String): Result<MealAnalysisResult> {
        return try {
            val result = mealAnalysisEngine.analyzeMealImage(imageUri)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class ConfirmMealLogUseCase @Inject constructor(
    private val trackingRepository: TrackingRepository,
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(mealLog: MealLog): Result<String> {
        val uid = sessionRepository.getCurrentUserId()
            ?: return Result.failure(IllegalStateException("Not signed in"))
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val logWithDate = if (mealLog.dateKey.isBlank()) mealLog.copy(dateKey = today) else mealLog
        val saveResult = trackingRepository.saveMealLog(uid, logWithDate)
        if (saveResult.isSuccess) {
            // Update daily summary meal count
            val summary = trackingRepository.getDailySummary(uid, today)
            if (summary != null) {
                val updated = summary.copy(
                    mealsLoggedCount = summary.mealsLoggedCount + 1,
                    progress = summary.progress.copy(
                        caloriesConsumed = summary.progress.caloriesConsumed + logWithDate.totalCalories
                    )
                )
                trackingRepository.updateDailySummary(uid, today, updated)
            }
        }
        return saveResult
    }
}

class AnalyzeBodyScanUseCase @Inject constructor(
    private val bodyScanAnalysisEngine: BodyScanAnalysisEngine
) {
    suspend operator fun invoke(frontImageUri: String, sideImageUri: String? = null): Result<BodyScanAnalysisResult> {
        return try {
            val result = bodyScanAnalysisEngine.analyzeBodyScan(frontImageUri, sideImageUri)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class SaveBodyScanUseCase @Inject constructor(
    private val trackingRepository: TrackingRepository,
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(bodyScan: BodyScan): Result<String> {
        val uid = sessionRepository.getCurrentUserId()
            ?: return Result.failure(IllegalStateException("Not signed in"))
        return trackingRepository.saveBodyScan(uid, bodyScan)
    }
}

class GetProgressStatsUseCase @Inject constructor(
    private val trackingRepository: TrackingRepository,
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(days: Int = 30): ProgressStats {
        val uid = sessionRepository.getCurrentUserId() ?: return ProgressStats()
        return trackingRepository.getProgressStats(uid, days)
    }
}

class GetMealLogsUseCase @Inject constructor(
    private val trackingRepository: TrackingRepository,
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(dateKey: String? = null): List<MealLog> {
        val uid = sessionRepository.getCurrentUserId() ?: return emptyList()
        val today = dateKey ?: LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        return trackingRepository.getMealLogs(uid, today)
    }
}
