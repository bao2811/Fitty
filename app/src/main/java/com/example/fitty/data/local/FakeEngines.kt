package com.example.fitty.data.local

import com.example.fitty.domain.model.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeCoachEngine @Inject constructor() : CoachEngine {
    override suspend fun generateResponse(context: CoachContext, messages: List<CoachMessage>, userMessage: String): CoachMessage {
        val lowerMsg = userMessage.lowercase()
        val vi = context.language.lowercase().startsWith("vi")
        val (text, suggestions) = when {
            lowerMsg.contains("miss") || lowerMsg.contains("skip") -> Pair(
                if (vi) {
                    "Không sao. Bỏ lỡ một ngày là bình thường. Mình sẽ dời buổi tập hôm nay sang ngày phù hợp tiếp theo để bạn giữ nhịp mà không bị quá tải."
                } else {
                    "No worries! Missing a day happens. Let's move today's workout to your next available day so you stay on track without overloading."
                },
                listOf(
                    CoachSuggestion.PlanAdjustment(
                        title = if (vi) "Dời buổi tập hôm nay" else "Reschedule today's workout",
                        actionLabel = if (vi) "Áp dụng" else "Apply to Plan",
                        targetPlanId = context.activePlanName
                    )
                )
            )
            lowerMsg.contains("meal") || lowerMsg.contains("dinner") || lowerMsg.contains("eat") -> Pair(
                if (vi) {
                    "Với mục tiêu ${context.goal.ifBlank { "sức khỏe" }}, hãy ưu tiên một bữa cân bằng: đạm nạc, tinh bột phức hợp và rau xanh. Đây là một gợi ý nhanh."
                } else {
                    "For your ${context.goal.ifBlank { "fitness" }} goal, try a balanced plate: lean protein, complex carbs, and vegetables. Here's a quick idea."
                },
                listOf(
                    CoachSuggestion.MealIdea(
                        title = if (vi) "Gợi ý bữa tối giàu đạm" else "High-protein dinner idea",
                        actionLabel = if (vi) "Lưu gợi ý" else "Save Meal Idea",
                        mealType = "dinner",
                        description = if (vi) {
                            "Ức gà nướng ăn cùng quinoa và bông cải hấp"
                        } else {
                            "Grilled chicken breast with quinoa and steamed broccoli"
                        },
                        estimatedCalories = 520,
                        estimatedProtein = 45
                    )
                )
            )
            lowerMsg.contains("adjust") || lowerMsg.contains("change") || lowerMsg.contains("plan") -> Pair(
                if (vi) {
                    "Mình có thể điều chỉnh kế hoạch. Dựa trên chuỗi ${context.currentStreak} ngày và mục tiêu ${context.goal.ifBlank { "duy trì thể lực" }}, bạn nên giữ nhịp tập ổn định và chỉ thay đổi từng phần nhỏ."
                } else {
                    "I can adjust your plan. Based on your current streak of ${context.currentStreak} days and ${context.goal.ifBlank { "general fitness" }} goal, I'd suggest keeping consistency while making small tweaks."
                },
                listOf(CoachSuggestion.General(title = if (vi) "Đã hiểu" else "Understood", actionLabel = if (vi) "Đồng ý" else "Got it"))
            )
            else -> Pair(
                if (vi) {
                    "Hãy bắt đầu bằng một hành động nhỏ hôm nay. Dựa trên mức ${context.fitnessLevel.ifBlank { "hiện tại" }} và mục tiêu ${context.goal.ifBlank { "sức khỏe" }}, mình sẽ điều chỉnh khi bạn ghi thêm dữ liệu."
                } else {
                    "Start with one practical action today. Based on your ${context.fitnessLevel.ifBlank { "current" }} level and ${context.goal.ifBlank { "fitness" }} goal, I'll adjust your plan as you log more data."
                },
                listOf(CoachSuggestion.General(title = if (vi) "Đã ghi nhận" else "Noted", actionLabel = if (vi) "Đồng ý" else "Got it"))
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
