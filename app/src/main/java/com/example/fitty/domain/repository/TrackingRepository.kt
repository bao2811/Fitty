package com.example.fitty.domain.repository

import com.example.fitty.domain.model.BodyMeasurement
import com.example.fitty.domain.model.BodyScan
import com.example.fitty.domain.model.DailySummary
import com.example.fitty.domain.model.MealLog
import com.example.fitty.domain.model.ProgressStats

interface TrackingRepository {
    // Meal logs
    suspend fun saveMealLog(uid: String, mealLog: MealLog): Result<String>
    suspend fun getMealLogs(uid: String, dateKey: String): List<MealLog>
    suspend fun getMealLog(uid: String, mealId: String): MealLog?
    suspend fun deleteMealLog(uid: String, mealId: String): Result<Unit>

    // Body scans
    suspend fun saveBodyScan(uid: String, bodyScan: BodyScan): Result<String>
    suspend fun getBodyScans(uid: String, limit: Int = 10): List<BodyScan>
    suspend fun getLatestBodyScan(uid: String): BodyScan?

    // Body measurements
    suspend fun saveBodyMeasurement(uid: String, measurement: BodyMeasurement): Result<String>
    suspend fun getBodyMeasurements(uid: String, limit: Int = 30): List<BodyMeasurement>

    // Daily summaries
    suspend fun getDailySummary(uid: String, dateKey: String): DailySummary?
    suspend fun getDailySummaries(uid: String, fromDate: String, toDate: String): List<DailySummary>
    suspend fun updateDailySummary(uid: String, dateKey: String, summary: DailySummary): Result<Unit>

    // Aggregated stats
    suspend fun getProgressStats(uid: String, days: Int = 30): ProgressStats
}
