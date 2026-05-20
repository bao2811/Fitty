package com.example.fitty.domain.usecase.track

import com.example.fitty.domain.model.BodyMeasurement
import com.example.fitty.domain.model.BodyScan
import com.example.fitty.domain.model.BodyScanAnalysisEngine
import com.example.fitty.domain.model.BodyScanAnalysisResult
import com.example.fitty.domain.model.MealAnalysisEngine
import com.example.fitty.domain.model.MealAnalysisResult
import com.example.fitty.domain.model.MealLog
import com.example.fitty.domain.model.MealScanRecord
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
    suspend operator fun invoke(mealLog: MealLog, imageUri: String? = null): Result<String> {
        val uid = sessionRepository.getCurrentUserId()
            ?: return Result.failure(IllegalStateException("Not signed in"))
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val logWithDate = if (mealLog.dateKey.isBlank()) mealLog.copy(dateKey = today) else mealLog

        // Upload image and set imageUrl if available
        var finalLog = logWithDate
        var uploadedImageUrl: String? = null
        if (!imageUri.isNullOrBlank()) {
            val uploadResult = trackingRepository.uploadScanImage(uid, imageUri)
            if (uploadResult.isSuccess) {
                uploadedImageUrl = uploadResult.getOrNull()
                finalLog = finalLog.copy(imageUrl = uploadedImageUrl)
            }
        }

        val saveResult = trackingRepository.saveMealLog(uid, finalLog)
        if (saveResult.isSuccess) {
            // Save scan history record
            val scanRecord = MealScanRecord(
                imageUrl = uploadedImageUrl ?: "",
                localImagePath = imageUri ?: "",
                mealLogId = saveResult.getOrDefault(""),
                totalCalories = finalLog.totalCalories,
                totalProtein = finalLog.totalProtein,
                totalCarbs = finalLog.totalCarbs,
                totalFat = finalLog.totalFat,
                confidence = finalLog.confidence,
                foodItems = finalLog.foodItems,
                timestamp = System.currentTimeMillis(),
                dateKey = finalLog.dateKey
            )
            trackingRepository.saveMealScanRecord(uid, scanRecord)

            // Update daily summary meal count
            val summary = trackingRepository.getDailySummary(uid, today)
            if (summary != null) {
                val updated = summary.copy(
                    mealsLoggedCount = summary.mealsLoggedCount + 1,
                    progress = summary.progress.copy(
                        caloriesConsumed = summary.progress.caloriesConsumed + finalLog.totalCalories
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
        val result = trackingRepository.saveBodyScan(uid, bodyScan)
        if (result.isSuccess) {
            // Also save a body measurement record from the scan
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val measurement = BodyMeasurement(
                dateKey = today,
                bodyFatPercent = bodyScan.estimatedBodyFatPercent,
                source = "scan"
            )
            trackingRepository.saveBodyMeasurement(uid, measurement)

            // Update daily summary to reflect the body scan activity
            val summary = trackingRepository.getDailySummary(uid, today)
            if (summary != null) {
                val updated = summary.copy(
                    progress = summary.progress.copy(
                        mealsLogged = summary.progress.mealsLogged + 0 // keep as-is
                    ),
                    insightText = summary.insightText.ifBlank {
                        bodyScan.summary.ifBlank { "Body scan completed." }
                    }
                )
                trackingRepository.updateDailySummary(uid, today, updated)
            }
        }
        return result
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

class GetMealScanHistoryUseCase @Inject constructor(
    private val trackingRepository: TrackingRepository,
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(limit: Int = 20): List<MealScanRecord> {
        val uid = sessionRepository.getCurrentUserId() ?: return emptyList()
        return trackingRepository.getMealScanHistory(uid, limit)
    }
}
