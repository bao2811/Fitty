package com.example.fitty.data.remote

import com.example.fitty.BuildConfig
import com.example.fitty.domain.model.CoachContext
import com.example.fitty.domain.model.CoachEngine
import com.example.fitty.domain.model.CoachMessage
import com.example.fitty.domain.model.CoachSuggestion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiCoachEngine @Inject constructor() : CoachEngine {

    companion object {
        private const val API_KEY = BuildConfig.GEMINI_API_KEY
        private val MODELS = listOf(
            "gemini-2.5-flash",
            "gemini-2.0-flash-lite",
            "gemini-2.0-flash"
        )
        private const val BASE_URL_TEMPLATE =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent"
        private const val MAX_RETRIES = 2
        private const val RETRY_DELAY_MS = 2000L
    }

    override suspend fun generateResponse(
        context: CoachContext,
        messages: List<CoachMessage>,
        userMessage: String
    ): CoachMessage = withContext(Dispatchers.IO) {
        require(API_KEY.isNotBlank()) { "Gemini API key is missing" }
        val systemPrompt = buildSystemPrompt(context)
        val requestBody = buildRequestBody(systemPrompt, messages, userMessage)
        val responseText = callWithFallback(requestBody)
        parseResponse(responseText, userMessage)
    }

    private fun buildSystemPrompt(context: CoachContext): String {
        return buildString {
            append("You are Fitty Coach, a friendly and knowledgeable fitness and nutrition AI coach inside the Fitty mobile app. ")
            append("Keep responses concise, practical, and limited to 2-4 sentences.\n\n")
            append("User context:\n")
            if (context.userName.isNotBlank()) append("- Name: ${context.userName}\n")
            if (context.goal.isNotBlank()) append("- Goal: ${context.goal}\n")
            if (context.fitnessLevel.isNotBlank()) append("- Fitness level: ${context.fitnessLevel}\n")
            if (context.activePlanName.isNotBlank()) append("- Active plan: ${context.activePlanName}\n")
            if (context.todayWorkoutTitle.isNotBlank()) append("- Today's workout: ${context.todayWorkoutTitle}\n")
            if (context.currentStreak > 0) append("- Current streak: ${context.currentStreak} days\n")
            if (context.mealsLoggedToday > 0) append("- Meals logged today: ${context.mealsLoggedToday}\n")
            context.latestWeight?.let { append("- Latest weight: ${it}kg\n") }
            if (context.recentInsight.isNotBlank()) append("- Recent insight: ${context.recentInsight}\n")
            append("\nRespond in the same language as the user. If the user writes Vietnamese, respond in Vietnamese.")
        }
    }

    private fun buildRequestBody(
        systemPrompt: String,
        messages: List<CoachMessage>,
        userMessage: String
    ): JSONObject {
        val contents = JSONArray()

        messages.takeLast(10).forEach { msg ->
            val role = if (msg.role == "user") "user" else "model"
            contents.put(
                JSONObject().apply {
                    put("role", role)
                    put("parts", JSONArray().put(JSONObject().put("text", msg.text)))
                }
            )
        }

        contents.put(
            JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().put(JSONObject().put("text", userMessage)))
            }
        )

        return JSONObject().apply {
            put(
                "systemInstruction",
                JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().put("text", systemPrompt)))
                }
            )
            put("contents", contents)
            put(
                "generationConfig",
                JSONObject().apply {
                    put("temperature", 0.7)
                    put("maxOutputTokens", 300)
                    put("topP", 0.9)
                }
            )
        }
    }

    private fun callWithFallback(requestBody: JSONObject): String {
        var lastException: Exception? = null

        for (model in MODELS) {
            for (attempt in 1..MAX_RETRIES) {
                try {
                    return callGeminiApi(model, requestBody)
                } catch (e: GeminiRetryableException) {
                    lastException = e
                    if (attempt < MAX_RETRIES) {
                        Thread.sleep(RETRY_DELAY_MS)
                    }
                } catch (e: Exception) {
                    throw e // Non-retryable, fail immediately
                }
            }
            // Model exhausted retries, try next model
        }

        throw lastException ?: RuntimeException("All Gemini models failed")
    }

    private class GeminiRetryableException(message: String) : RuntimeException(message)

    private fun callGeminiApi(model: String, requestBody: JSONObject): String {
        val url = URL(String.format(BASE_URL_TEMPLATE, model) + "?key=$API_KEY")
        val connection = url.openConnection() as HttpURLConnection

        connection.apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            doOutput = true
            connectTimeout = 15_000
            readTimeout = 60_000
        }

        connection.outputStream.use { os ->
            os.write(requestBody.toString().toByteArray(Charsets.UTF_8))
        }

        val responseCode = connection.responseCode
        val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
        val responseText = BufferedReader(InputStreamReader(stream)).use { it.readText() }

        if (responseCode in 200..299) return responseText

        // Retryable errors: 429 (quota), 503 (overloaded)
        if (responseCode == 429 || responseCode == 503) {
            throw GeminiRetryableException(mapGeminiError(responseCode, responseText))
        }

        throw RuntimeException(mapGeminiError(responseCode, responseText))
    }

    private fun mapGeminiError(responseCode: Int, responseText: String): String {
        return when (responseCode) {
            400 -> "Yêu cầu không hợp lệ. Kiểm tra format."
            401, 403 -> "API key không hợp lệ hoặc không có quyền."
            429 -> "Hết quota Gemini. Đợi reset hoặc kiểm tra billing."
            503 -> "Gemini đang quá tải. Đang thử lại với model khác..."
            else -> "Gemini lỗi ($responseCode): ${responseText.take(160)}"
        }
    }

    private fun parseResponse(responseJson: String, userMessage: String): CoachMessage {
        val json = JSONObject(responseJson)
        val text = json.optJSONArray("candidates")
            ?.optJSONObject(0)
            ?.optJSONObject("content")
            ?.optJSONArray("parts")
            ?.optJSONObject(0)
            ?.optString("text", "")
            .orEmpty()

        if (text.isBlank()) {
            throw IllegalStateException("Gemini returned an empty response")
        }

        return CoachMessage(
            role = "assistant",
            text = text.trim(),
            suggestions = inferSuggestions(userMessage, text),
            createdAt = System.currentTimeMillis()
        )
    }

    private fun inferSuggestions(userMessage: String, aiResponse: String): List<CoachSuggestion> {
        val lower = userMessage.lowercase()
        return when {
            lower.contains("miss") || lower.contains("skip") || lower.contains("bo") || lower.contains("bỏ") ->
                listOf(CoachSuggestion.PlanAdjustment(title = "Reschedule workout"))

            lower.contains("meal") || lower.contains("dinner") || lower.contains("eat") ||
                lower.contains("an") || lower.contains("ăn") || lower.contains("bua") || lower.contains("bữa") ->
                listOf(
                    CoachSuggestion.MealIdea(
                        title = "Save meal suggestion",
                        description = aiResponse.take(100),
                        estimatedCalories = 500,
                        estimatedProtein = 35
                    )
                )

            else -> emptyList()
        }
    }
}
