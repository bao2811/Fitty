package com.example.fitty.domain.model

data class MealLog(
    val id: String = "",
    val mealType: String = "",
    val source: String = "scan",
    val imageUrl: String? = null,
    val dateKey: String = "",
    val totalCalories: Int = 0,
    val totalProtein: Int = 0,
    val totalCarbs: Int = 0,
    val totalFat: Int = 0,
    val confidence: Float = 0f,
    val foodItems: List<FoodItem> = emptyList(),
    val notes: String? = null
)

data class FoodItem(
    val name: String = "",
    val quantity: Int = 0,
    val unit: String = "g",
    val calories: Int = 0,
    val protein: Int = 0,
    val carbs: Int = 0,
    val fat: Int = 0,
    val confidence: Float = 0f
)

data class BodyScan(
    val id: String = "",
    val capturedAt: Long = 0L,
    val frontImageUrl: String? = null,
    val sideImageUrl: String? = null,
    val summary: String = "",
    val confidence: Float = 0f,
    val estimatedBodyFatPercent: Float? = null,
    val postureScore: Int? = null,
    val status: String = "pending"
)

data class BodyMeasurement(
    val id: String = "",
    val dateKey: String = "",
    val weightKg: Float? = null,
    val bodyFatPercent: Float? = null,
    val waistCm: Float? = null,
    val chestCm: Float? = null,
    val hipCm: Float? = null,
    val source: String = "manual"
)

data class DailySummary(
    val dateKey: String = "",
    val targets: DailySummaryTargets = DailySummaryTargets(),
    val progress: DailySummaryProgress = DailySummaryProgress(),
    val todayWorkoutTitle: String = "",
    val mealsLoggedCount: Int = 0,
    val currentStreak: Int = 0,
    val insightText: String = ""
)

data class DailySummaryTargets(
    val calories: Int = 2100,
    val waterMl: Int = 2500,
    val workouts: Int = 1,
    val steps: Int = 8000
)

data class DailySummaryProgress(
    val caloriesConsumed: Int = 0,
    val caloriesBurned: Int = 0,
    val waterMl: Int = 0,
    val workoutsCompleted: Int = 0,
    val mealsLogged: Int = 0,
    val steps: Int = 0,
    val proteinGrams: Int = 0,
    val carbsGrams: Int = 0,
    val fatGrams: Int = 0
)

data class MealAnalysisResult(
    val mealLog: MealLog,
    val confidence: Float = 0f
)

data class BodyScanAnalysisResult(
    val bodyScan: BodyScan,
    val confidence: Float = 0f
)

data class ProgressStats(
    val dailySummaries: List<DailySummary> = emptyList(),
    val bodyMeasurements: List<BodyMeasurement> = emptyList(),
    val totalWorkouts: Int = 0,
    val totalMealsLogged: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val latestWeight: Float? = null,
    val latestBodyFat: Float? = null,
    val targetWeight: Float? = null,
    val bmi: Float? = null
)

/**
 * Represents a saved meal scan record in the user's scan history.
 * Each scan stores the uploaded image URL and the full analysis result.
 */
data class MealScanRecord(
    val id: String = "",
    val imageUrl: String = "",
    val localImagePath: String = "",
    val mealLogId: String = "",
    val totalCalories: Int = 0,
    val totalProtein: Int = 0,
    val totalCarbs: Int = 0,
    val totalFat: Int = 0,
    val confidence: Float = 0f,
    val foodItems: List<FoodItem> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
    val dateKey: String = ""
)
