package com.example.fitty.data.local

import com.example.fitty.domain.model.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeCoachEngine @Inject constructor() : CoachEngine {
    override suspend fun generateResponse(context: CoachContext, messages: List<CoachMessage>, userMessage: String): CoachMessage {
        val lowerMsg = userMessage.lowercase()
        val (text, suggestions) = when {
            lowerMsg.contains("miss") || lowerMsg.contains("skip") -> Pair(
                "No worries! Missing a day happens. Let's move today's workout to your next available day so you stay on track without overloading.",
                listOf(CoachSuggestion.PlanAdjustment(title = "Reschedule today's workout", targetPlanId = context.activePlanName))
            )
            lowerMsg.contains("meal") || lowerMsg.contains("dinner") || lowerMsg.contains("eat") -> Pair(
                "For your ${context.goal.ifBlank { "fitness" }} goal, try a balanced plate: lean protein, complex carbs, and vegetables. Here's a quick idea.",
                listOf(CoachSuggestion.MealIdea(title = "High-protein dinner idea", mealType = "dinner",
                    description = "Grilled chicken breast with quinoa and steamed broccoli", estimatedCalories = 520, estimatedProtein = 45))
            )
            lowerMsg.contains("adjust") || lowerMsg.contains("change") || lowerMsg.contains("plan") -> Pair(
                "I can adjust your plan. Based on your current streak of ${context.currentStreak} days and ${context.goal.ifBlank { "general fitness" }} goal, I'd suggest keeping consistency while making small tweaks.",
                listOf(CoachSuggestion.General(title = "Understood", actionLabel = "Got it"))
            )
            else -> Pair(
                "Start with one practical action today. Based on your ${context.fitnessLevel.ifBlank { "current" }} level and ${context.goal.ifBlank { "fitness" }} goal, I'll adjust your plan as you log more data.",
                listOf(CoachSuggestion.General(title = "Noted", actionLabel = "Got it"))
            )
        }
        return CoachMessage(role = "assistant", text = text, suggestions = suggestions, createdAt = System.currentTimeMillis())
    }
}

@Singleton
class FakeMealAnalysisEngine @Inject constructor() : MealAnalysisEngine {
    override suspend fun analyzeMealImage(imageUri: String): MealAnalysisResult {
        // Simulate analysis delay
        kotlinx.coroutines.delay(800)
        return MealAnalysisResult(
            mealLog = MealLog(mealType = "lunch", source = "scan", imageUrl = imageUri,
                totalCalories = 610, totalProtein = 42, totalCarbs = 68, totalFat = 18, confidence = 0.86f,
                foodItems = listOf(
                    FoodItem(name = "Chicken breast", quantity = 150, unit = "g", calories = 248, protein = 46, carbs = 0, fat = 5, confidence = 0.91f),
                    FoodItem(name = "Rice", quantity = 200, unit = "g", calories = 260, protein = 5, carbs = 58, fat = 1, confidence = 0.88f),
                    FoodItem(name = "Vegetables", quantity = 100, unit = "g", calories = 45, protein = 3, carbs = 8, fat = 1, confidence = 0.82f)
                )),
            confidence = 0.86f
        )
    }
}

@Singleton
class FakeBodyScanAnalysisEngine @Inject constructor() : BodyScanAnalysisEngine {
    override suspend fun analyzeBodyScan(frontImageUri: String, sideImageUri: String?): BodyScanAnalysisResult {
        kotlinx.coroutines.delay(1000)
        return BodyScanAnalysisResult(
            bodyScan = BodyScan(capturedAt = System.currentTimeMillis(), frontImageUrl = frontImageUri, sideImageUrl = sideImageUri,
                summary = "Good posture detected. Consistent body composition compared to previous scan.", confidence = 0.78f,
                estimatedBodyFatPercent = 18.9f, postureScore = 72, status = "processed"),
            confidence = 0.78f
        )
    }
}
