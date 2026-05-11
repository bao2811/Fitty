package com.example.fitty.domain.model

/**
 * Interface for the AI coach engine. In scope v1, this is backed by FakeCoachEngine.
 * Replace with a remote API implementation later.
 */
interface CoachEngine {
    suspend fun generateResponse(
        context: CoachContext,
        messages: List<CoachMessage>,
        userMessage: String
    ): CoachMessage
}

/**
 * Interface for meal image analysis. In scope v1, this is backed by FakeAnalysisEngine.
 * Replace with a remote API implementation later.
 */
interface MealAnalysisEngine {
    suspend fun analyzeMealImage(imageUri: String): MealAnalysisResult
}

/**
 * Interface for body scan analysis. In scope v1, this is backed by FakeAnalysisEngine.
 * Replace with a remote API implementation later.
 */
interface BodyScanAnalysisEngine {
    suspend fun analyzeBodyScan(frontImageUri: String, sideImageUri: String? = null): BodyScanAnalysisResult
}
