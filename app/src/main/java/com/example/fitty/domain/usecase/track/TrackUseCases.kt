package com.example.fitty.domain.usecase.track

import com.example.fitty.domain.model.BodyMeasurement
import com.example.fitty.domain.model.DailySummary
import com.example.fitty.domain.model.BodyScan
import com.example.fitty.domain.model.BodyScanAnalysisEngine
import com.example.fitty.domain.model.BodyScanAnalysisResult
import com.example.fitty.domain.model.MealAnalysisEngine
import com.example.fitty.domain.model.MealAnalysisResult
import com.example.fitty.domain.model.MealLog
import com.example.fitty.domain.model.MealScanRecord
import com.example.fitty.domain.model.ProgressStats
import com.example.fitty.domain.model.withRecalculatedAchievements
import com.example.fitty.domain.repository.SessionRepository
import com.example.fitty.domain.repository.TrackingRepository
import com.example.fitty.domain.repository.UserRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

private fun Throwable.isStoragePermissionDenied(): Boolean {
    val message = generateSequence(this) { it.cause }
        .mapNotNull { it.message }
        .joinToString(" ")
        .lowercase()
    return "does not have permission" in message || "permission denied" in message || "unauthorized" in message
}

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
    private val sessionRepository: SessionRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(mealLog: MealLog, imageUri: String? = null): Result<String> {
        val uid = sessionRepository.getCurrentUserId()
            ?: return Result.failure(IllegalStateException("Not signed in"))
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val logWithDate = if (mealLog.dateKey.isBlank()) mealLog.copy(dateKey = today) else mealLog
        val summaryDateKey = logWithDate.dateKey

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
            val scanHistoryResult = trackingRepository.saveMealScanRecord(uid, scanRecord)

            // Update daily summary: meal count + macro fields
            val summary = trackingRepository.getDailySummary(uid, summaryDateKey)
            val user = userRepository.getCurrentUser(uid)
            val baseSummary = summary ?: DailySummary(
                dateKey = summaryDateKey,
                targets = com.example.fitty.domain.model.DailySummaryTargets(
                    calories = user?.settings?.calorieTarget ?: 2100,
                    waterMl = user?.settings?.waterGoalMl ?: 2500
                )
            )
            val updated = baseSummary.copy(
                mealsLoggedCount = baseSummary.mealsLoggedCount + 1,
                progress = baseSummary.progress.copy(
                    caloriesConsumed = baseSummary.progress.caloriesConsumed + finalLog.totalCalories,
                    mealsLogged = baseSummary.progress.mealsLogged + 1,
                    proteinGrams = baseSummary.progress.proteinGrams + finalLog.totalProtein,
                    carbsGrams = baseSummary.progress.carbsGrams + finalLog.totalCarbs,
                    fatGrams = baseSummary.progress.fatGrams + finalLog.totalFat
                )
            )
            trackingRepository.updateDailySummary(uid, summaryDateKey, updated)

            // Keep aggregate user stats in sync with Track/Home summary cards.
            runCatching {
                if (user != null) {
                    userRepository.updateStats(
                        uid,
                        user.stats.copy(mealsLogged = user.stats.mealsLogged + 1)
                            .withRecalculatedAchievements()
                    )
                }
            }

            if (scanHistoryResult.isFailure) {
                return Result.failure(
                    IllegalStateException(
                        "Đã lưu bữa ăn nhưng không lưu được lịch sử quét. Kiểm tra Firestore rules cho users/$uid/meal_scan_history.",
                        scanHistoryResult.exceptionOrNull()
                    )
                )
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
    private val sessionRepository: SessionRepository,
    private val userRepository: com.example.fitty.domain.repository.UserRepository
) {
    suspend operator fun invoke(bodyScan: BodyScan): Result<String> {
        val uid = sessionRepository.getCurrentUserId()
            ?: return Result.failure(IllegalStateException("Not signed in"))
        var finalScan = bodyScan
        val frontImage = bodyScan.frontImageUrl
        if (!frontImage.isNullOrBlank()) {
            val uploadedFront = trackingRepository.uploadBodyScanImage(uid, frontImage)
            if (uploadedFront.isSuccess) {
                finalScan = finalScan.copy(frontImageUrl = uploadedFront.getOrThrow())
            } else {
                val error = uploadedFront.exceptionOrNull()
                if (error == null || !error.isStoragePermissionDenied()) {
                    return Result.failure(error ?: IllegalStateException("Failed to upload front body scan image"))
                }
            }
        }
        val sideImage = bodyScan.sideImageUrl
        if (!sideImage.isNullOrBlank()) {
            val uploadedSide = trackingRepository.uploadBodyScanImage(uid, sideImage)
            if (uploadedSide.isSuccess) {
                finalScan = finalScan.copy(sideImageUrl = uploadedSide.getOrThrow())
            } else {
                val error = uploadedSide.exceptionOrNull()
                if (error == null || !error.isStoragePermissionDenied()) {
                    return Result.failure(error ?: IllegalStateException("Failed to upload side body scan image"))
                }
            }
        }

        val result = trackingRepository.saveBodyScan(uid, finalScan)
        if (result.isSuccess) {
            // Also save a body measurement record from the scan
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val measurement = BodyMeasurement(
                dateKey = today,
                bodyFatPercent = finalScan.estimatedBodyFatPercent,
                source = "scan"
            )
            trackingRepository.saveBodyMeasurement(uid, measurement)

            // Update daily summary to reflect the body scan activity
            val summary = trackingRepository.getDailySummary(uid, today)
            val baseSummary = summary ?: DailySummary(dateKey = today)
            val updated = baseSummary.copy(
                insightText = baseSummary.insightText.ifBlank {
                    finalScan.summary.ifBlank { "Body scan completed." }
                }
            )
            trackingRepository.updateDailySummary(uid, today, updated)

            // Update user profile with latest body metrics from scan
            runCatching {
                val user = userRepository.getCurrentUser(uid)
                if (user != null) {
                    val updatedProfile = user.profile.copy(
                        // Note: Body scan provides bodyFat%, not weight.
                        // Weight is only updated manually via the Home edit dialog.
                    )
                    // Only update if profile actually changed (future: add bodyFatPercent to profile)
                    userRepository.updateProfile(uid, updatedProfile)
                }
            }
        }
        return result
    }
}

class GetBodyScansUseCase @Inject constructor(
    private val trackingRepository: TrackingRepository,
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(limit: Int = 20): List<BodyScan> {
        val uid = sessionRepository.getCurrentUserId() ?: return emptyList()
        return trackingRepository.getBodyScans(uid, limit)
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
