package com.example.fitty.data.firebase

import android.net.Uri
import com.example.fitty.domain.model.*
import com.example.fitty.domain.repository.TrackingRepository
import com.google.firebase.firestore.*
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseTrackingRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : TrackingRepository {

    private fun userDoc(uid: String) = firestore.collection("users").document(uid)

    override suspend fun saveMealLog(uid: String, mealLog: MealLog): Result<String> = try {
        val ref = if (mealLog.id.isBlank()) userDoc(uid).collection("meal_logs").document()
        else userDoc(uid).collection("meal_logs").document(mealLog.id)
        ref.set(mealLog.toMap(), SetOptions.merge()).await(); Result.success(ref.id)
    } catch (e: Exception) { Result.failure(e) }

    @Suppress("UNCHECKED_CAST")
    override suspend fun getMealLogs(uid: String, dateKey: String): List<MealLog> =
        userDoc(uid).collection("meal_logs").whereEqualTo("dateKey", dateKey)
            .orderBy("loggedAt", Query.Direction.DESCENDING).get().await()
            .documents.mapNotNull { it.toMealLog() }

    override suspend fun getMealLog(uid: String, mealId: String): MealLog? {
        val doc = userDoc(uid).collection("meal_logs").document(mealId).get().await()
        return if (doc.exists()) doc.toMealLog() else null
    }

    override suspend fun deleteMealLog(uid: String, mealId: String): Result<Unit> = try {
        userDoc(uid).collection("meal_logs").document(mealId).delete().await(); Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }

    // ── Meal Scan History ────────────────────────────────────────────────

    override suspend fun uploadScanImage(uid: String, localImageUri: String): Result<String> = try {
        val fileName = "meal_scan_${System.currentTimeMillis()}.jpg"
        val ref = storage.reference.child("users/$uid/meal_scans/$fileName")
        ref.putFile(Uri.parse(localImageUri)).await()
        val downloadUrl = ref.downloadUrl.await().toString()
        Result.success(downloadUrl)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun saveMealScanRecord(uid: String, record: MealScanRecord): Result<String> = try {
        val ref = if (record.id.isBlank()) userDoc(uid).collection("meal_scan_history").document()
        else userDoc(uid).collection("meal_scan_history").document(record.id)
        ref.set(record.toScanMap(), SetOptions.merge()).await()
        Result.success(ref.id)
    } catch (e: Exception) {
        Result.failure(e)
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun getMealScanHistory(uid: String, limit: Int): List<MealScanRecord> =
        userDoc(uid).collection("meal_scan_history")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get().await()
            .documents.mapNotNull { it.toMealScanRecord() }

    // ── Body Scans ───────────────────────────────────────────────────────

    override suspend fun saveBodyScan(uid: String, bodyScan: BodyScan): Result<String> = try {
        val ref = if (bodyScan.id.isBlank()) userDoc(uid).collection("body_scans").document()
        else userDoc(uid).collection("body_scans").document(bodyScan.id)
        ref.set(bodyScan.toMap(), SetOptions.merge()).await(); Result.success(ref.id)
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun getBodyScans(uid: String, limit: Int): List<BodyScan> =
        userDoc(uid).collection("body_scans").orderBy("capturedAt", Query.Direction.DESCENDING)
            .limit(limit.toLong()).get().await().documents.mapNotNull { it.toBodyScan() }

    override suspend fun getLatestBodyScan(uid: String): BodyScan? =
        getBodyScans(uid, 1).firstOrNull()

    override suspend fun saveBodyMeasurement(uid: String, measurement: BodyMeasurement): Result<String> = try {
        val ref = if (measurement.id.isBlank()) userDoc(uid).collection("body_measurements").document()
        else userDoc(uid).collection("body_measurements").document(measurement.id)
        ref.set(measurement.toMap(), SetOptions.merge()).await(); Result.success(ref.id)
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun getBodyMeasurements(uid: String, limit: Int): List<BodyMeasurement> =
        userDoc(uid).collection("body_measurements").orderBy("measuredAt", Query.Direction.DESCENDING)
            .limit(limit.toLong()).get().await().documents.mapNotNull { it.toBodyMeasurement() }

    override suspend fun getDailySummary(uid: String, dateKey: String): DailySummary? {
        val doc = userDoc(uid).collection("daily_summaries").document(dateKey).get().await()
        return if (doc.exists()) doc.toDailySummary() else null
    }

    override suspend fun getDailySummaries(uid: String, fromDate: String, toDate: String): List<DailySummary> =
        userDoc(uid).collection("daily_summaries")
            .whereGreaterThanOrEqualTo("dateKey", fromDate).whereLessThanOrEqualTo("dateKey", toDate)
            .orderBy("dateKey", Query.Direction.DESCENDING).get().await()
            .documents.mapNotNull { it.toDailySummary() }

    override suspend fun updateDailySummary(uid: String, dateKey: String, summary: DailySummary): Result<Unit> = try {
        userDoc(uid).collection("daily_summaries").document(dateKey)
            .set(summary.toMap(), SetOptions.merge()).await(); Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun getProgressStats(uid: String, days: Int): ProgressStats {
        val user = firestore.collection("users").document(uid).get().await()
        val statsMap = user.get("stats") as? Map<*, *> ?: emptyMap<String, Any>()
        val measurements = getBodyMeasurements(uid, days)
        val latestWeight = measurements.firstOrNull()?.weightKg
        val targetWeightVal = (user.getDouble("targetWeightKg"))?.toFloat()
        val heightCm = (user.getDouble("heightCm"))?.toFloat()
        val bmiVal = if (latestWeight != null && heightCm != null && heightCm > 0) {
            val heightM = heightCm / 100f
            latestWeight / (heightM * heightM)
        } else null
        return ProgressStats(
            totalWorkouts = (statsMap["totalWorkouts"] as? Number)?.toInt() ?: 0,
            totalMealsLogged = (statsMap["mealsLogged"] as? Number)?.toInt() ?: 0,
            currentStreak = (statsMap["currentStreak"] as? Number)?.toInt() ?: 0,
            bestStreak = (statsMap["bestStreak"] as? Number)?.toInt() ?: 0,
            latestWeight = latestWeight,
            latestBodyFat = measurements.firstOrNull()?.bodyFatPercent,
            bodyMeasurements = measurements,
            targetWeight = targetWeightVal,
            bmi = bmiVal
        )
    }

    // ── Mapping helpers ──────────────────────────────────────────────────

    @Suppress("UNCHECKED_CAST")
    private fun DocumentSnapshot.toMealLog(): MealLog? {
        if (!exists()) return null
        val items = (get("foodItems") as? List<Map<String, Any?>>)?.map { m ->
            FoodItem(name = m["name"] as? String ?: "", quantity = (m["quantity"] as? Number)?.toInt() ?: 0,
                unit = m["unit"] as? String ?: "g", calories = (m["calories"] as? Number)?.toInt() ?: 0,
                protein = (m["protein"] as? Number)?.toInt() ?: 0, carbs = (m["carbs"] as? Number)?.toInt() ?: 0,
                fat = (m["fat"] as? Number)?.toInt() ?: 0, confidence = (m["confidence"] as? Number)?.toFloat() ?: 0f)
        } ?: emptyList()
        return MealLog(id = id, mealType = getString("mealType").orEmpty(), source = getString("source") ?: "scan",
            imageUrl = getString("imageUrl"), dateKey = getString("dateKey").orEmpty(),
            totalCalories = getLong("totalCalories")?.toInt() ?: 0, totalProtein = getLong("totalProtein")?.toInt() ?: 0,
            totalCarbs = getLong("totalCarbs")?.toInt() ?: 0, totalFat = getLong("totalFat")?.toInt() ?: 0,
            confidence = getDouble("confidence")?.toFloat() ?: 0f, foodItems = items, notes = getString("notes"))
    }

    @Suppress("UNCHECKED_CAST")
    private fun DocumentSnapshot.toMealScanRecord(): MealScanRecord? {
        if (!exists()) return null
        val items = (get("foodItems") as? List<Map<String, Any?>>)?.map { m ->
            FoodItem(
                name = m["name"] as? String ?: "",
                quantity = (m["quantity"] as? Number)?.toInt() ?: 0,
                unit = m["unit"] as? String ?: "g",
                calories = (m["calories"] as? Number)?.toInt() ?: 0,
                protein = (m["protein"] as? Number)?.toInt() ?: 0,
                carbs = (m["carbs"] as? Number)?.toInt() ?: 0,
                fat = (m["fat"] as? Number)?.toInt() ?: 0,
                confidence = (m["confidence"] as? Number)?.toFloat() ?: 0f
            )
        } ?: emptyList()
        return MealScanRecord(
            id = id,
            imageUrl = getString("imageUrl").orEmpty(),
            localImagePath = getString("localImagePath").orEmpty(),
            mealLogId = getString("mealLogId").orEmpty(),
            totalCalories = getLong("totalCalories")?.toInt() ?: 0,
            totalProtein = getLong("totalProtein")?.toInt() ?: 0,
            totalCarbs = getLong("totalCarbs")?.toInt() ?: 0,
            totalFat = getLong("totalFat")?.toInt() ?: 0,
            confidence = getDouble("confidence")?.toFloat() ?: 0f,
            foodItems = items,
            timestamp = getLong("timestamp") ?: 0L,
            dateKey = getString("dateKey").orEmpty()
        )
    }

    private fun DocumentSnapshot.toBodyScan(): BodyScan? {
        if (!exists()) return null
        val metrics = get("metrics") as? Map<*, *>
        return BodyScan(id = id, capturedAt = getLong("capturedAt") ?: 0L,
            frontImageUrl = getString("frontImageUrl"), sideImageUrl = getString("sideImageUrl"),
            summary = getString("summary").orEmpty(), confidence = getDouble("confidence")?.toFloat() ?: 0f,
            estimatedBodyFatPercent = (metrics?.get("estimatedBodyFatPercent") as? Number)?.toFloat(),
            postureScore = (metrics?.get("postureScore") as? Number)?.toInt(),
            status = getString("status") ?: "pending")
    }

    private fun DocumentSnapshot.toBodyMeasurement(): BodyMeasurement? {
        if (!exists()) return null
        return BodyMeasurement(id = id, dateKey = getString("dateKey").orEmpty(),
            weightKg = getDouble("weightKg")?.toFloat(), bodyFatPercent = getDouble("bodyFatPercent")?.toFloat(),
            waistCm = getDouble("waistCm")?.toFloat(), chestCm = getDouble("chestCm")?.toFloat(),
            hipCm = getDouble("hipCm")?.toFloat(), source = getString("source") ?: "manual")
    }

    @Suppress("UNCHECKED_CAST")
    private fun DocumentSnapshot.toDailySummary(): DailySummary? {
        if (!exists()) return null
        val t = get("targets") as? Map<*, *> ?: emptyMap<String, Any>()
        val p = get("progress") as? Map<*, *> ?: emptyMap<String, Any>()
        val s = get("summaries") as? Map<*, *> ?: emptyMap<String, Any>()
        return DailySummary(dateKey = id,
            targets = DailySummaryTargets(calories = (t["calories"] as? Number)?.toInt() ?: 2100,
                waterMl = (t["waterMl"] as? Number)?.toInt() ?: 2500, workouts = (t["workouts"] as? Number)?.toInt() ?: 1,
                steps = (t["steps"] as? Number)?.toInt() ?: 8000),
            progress = DailySummaryProgress(caloriesConsumed = (p["caloriesConsumed"] as? Number)?.toInt() ?: 0,
                waterMl = (p["waterMl"] as? Number)?.toInt() ?: 0, workoutsCompleted = (p["workoutsCompleted"] as? Number)?.toInt() ?: 0,
                steps = (p["steps"] as? Number)?.toInt() ?: 0),
            todayWorkoutTitle = s["todayWorkoutTitle"] as? String ?: "",
            mealsLoggedCount = (s["mealsLoggedCount"] as? Number)?.toInt() ?: 0,
            currentStreak = (s["currentStreak"] as? Number)?.toInt() ?: 0,
            insightText = s["insightText"] as? String ?: "")
    }

    private fun MealLog.toMap(): Map<String, Any?> = mapOf("mealType" to mealType, "source" to source, "imageUrl" to imageUrl,
        "dateKey" to dateKey, "loggedAt" to FieldValue.serverTimestamp(), "totalCalories" to totalCalories,
        "totalProtein" to totalProtein, "totalCarbs" to totalCarbs, "totalFat" to totalFat, "confidence" to confidence,
        "foodItems" to foodItems.map { mapOf("name" to it.name, "quantity" to it.quantity, "unit" to it.unit,
            "calories" to it.calories, "protein" to it.protein, "carbs" to it.carbs, "fat" to it.fat, "confidence" to it.confidence) },
        "notes" to notes, "updatedAt" to FieldValue.serverTimestamp())

    private fun MealScanRecord.toScanMap(): Map<String, Any?> = mapOf(
        "imageUrl" to imageUrl,
        "localImagePath" to localImagePath,
        "mealLogId" to mealLogId,
        "totalCalories" to totalCalories,
        "totalProtein" to totalProtein,
        "totalCarbs" to totalCarbs,
        "totalFat" to totalFat,
        "confidence" to confidence,
        "foodItems" to foodItems.map {
            mapOf("name" to it.name, "quantity" to it.quantity, "unit" to it.unit,
                "calories" to it.calories, "protein" to it.protein, "carbs" to it.carbs,
                "fat" to it.fat, "confidence" to it.confidence)
        },
        "timestamp" to timestamp,
        "dateKey" to dateKey,
        "createdAt" to FieldValue.serverTimestamp()
    )

    private fun BodyScan.toMap(): Map<String, Any?> = mapOf("capturedAt" to capturedAt,
        "frontImageUrl" to frontImageUrl, "sideImageUrl" to sideImageUrl, "summary" to summary, "confidence" to confidence,
        "metrics" to mapOf("estimatedBodyFatPercent" to estimatedBodyFatPercent, "postureScore" to postureScore), "status" to status)

    private fun BodyMeasurement.toMap(): Map<String, Any?> = mapOf("dateKey" to dateKey, "measuredAt" to FieldValue.serverTimestamp(),
        "weightKg" to weightKg, "bodyFatPercent" to bodyFatPercent, "waistCm" to waistCm, "chestCm" to chestCm, "hipCm" to hipCm, "source" to source)

    private fun DailySummary.toMap(): Map<String, Any?> = mapOf("dateKey" to dateKey,
        "targets" to mapOf("calories" to targets.calories, "waterMl" to targets.waterMl, "workouts" to targets.workouts, "steps" to targets.steps),
        "progress" to mapOf("caloriesConsumed" to progress.caloriesConsumed, "waterMl" to progress.waterMl, "workoutsCompleted" to progress.workoutsCompleted, "steps" to progress.steps),
        "summaries" to mapOf("todayWorkoutTitle" to todayWorkoutTitle, "mealsLoggedCount" to mealsLoggedCount, "currentStreak" to currentStreak, "insightText" to insightText),
        "updatedAt" to FieldValue.serverTimestamp())
}
