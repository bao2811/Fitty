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
        private const val BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"
    }

    override suspend fun generateResponse(
        context: CoachContext,
        messages: List<CoachMessage>,
        userMessage: String
    ): CoachMessage = withContext(Dispatchers.IO) {
        try {
            val systemPrompt = buildSystemPrompt(context)
            val requestBody = buildRequestBody(systemPrompt, messages, userMessage)
            val responseText = callGeminiApi(requestBody)
            parseResponse(responseText, userMessage)
        } catch (e: Exception) {
            CoachMessage(
                role = "assistant",
                text = "I'm having trouble connecting right now. Please try again in a moment. (${e.message})",
                createdAt = System.currentTimeMillis()
            )
        }
    }

    private fun buildSystemPrompt(context: CoachContext): String {
        return buildString {
            append("You are Fitty Coach — a friendly, knowledgeable fitness and nutrition AI coach inside the Fitty mobile app. ")
            append("Keep responses concise (2-4 sentences max). Be encouraging and practical.\n\n")
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
            append("\nRespond in the same language the user writes in. If they write Vietnamese, reply in Vietnamese.")
        }
    }

    private fun buildRequestBody(
        systemPrompt: String,
        messages: List<CoachMessage>,
        userMessage: String
    ): JSONObject {
        val contents = JSONArray()

        // Add system instruction as first user turn
        val systemContent = JSONObject().apply {
            put("role", "user")
            put("parts", JSONArray().put(JSONObject().put("text", "System: $systemPrompt")))
        }
        contents.put(systemContent)

        // Acknowledge system prompt
        val ackContent = JSONObject().apply {
            put("role", "model")
            put("parts", JSONArray().put(JSONObject().put("text", "Understood. I'm Fitty Coach, ready to help!")))
        }
        contents.put(ackContent)

        // Add conversation history (last 10 messages for context window)
        val recentMessages = messages.takeLast(10)
        for (msg in recentMessages) {
            val role = if (msg.role == "user") "user" else "model"
            val content = JSONObject().apply {
                put("role", role)
                put("parts", JSONArray().put(JSONObject().put("text", msg.text)))
            }
            contents.put(content)
        }

        // Add current user message
        val userContent = JSONObject().apply {
            put("role", "user")
            put("parts", JSONArray().put(JSONObject().put("text", userMessage)))
        }
        contents.put(userContent)

        // Generation config
        val generationConfig = JSONObject().apply {
            put("temperature", 0.7)
            put("maxOutputTokens", 300)
            put("topP", 0.9)
        }

        return JSONObject().apply {
            put("contents", contents)
            put("generationConfig", generationConfig)
        }
    }

    private fun callGeminiApi(requestBody: JSONObject): String {
        val url = URL("$BASE_URL?key=$API_KEY")
        val connection = url.openConnection() as HttpURLConnection

        connection.apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            doOutput = true
            connectTimeout = 15_000
            readTimeout = 30_000
        }

        connection.outputStream.use { os ->
            os.write(requestBody.toString().toByteArray(Charsets.UTF_8))
        }

        val responseCode = connection.responseCode
        val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream

        val responseText = BufferedReader(InputStreamReader(stream)).use { it.readText() }

        if (responseCode !in 200..299) {
            throw RuntimeException("Gemini API error ($responseCode): ${responseText.take(200)}")
        }

        return responseText
    }

    private fun parseResponse(responseJson: String, userMessage: String): CoachMessage {
        val json = JSONObject(responseJson)
        val candidates = json.optJSONArray("candidates")
        val firstCandidate = candidates?.optJSONObject(0)
        val content = firstCandidate?.optJSONObject("content")
        val parts = content?.optJSONArray("parts")
        val text = parts?.optJSONObject(0)?.optString("text", "") ?: ""

        if (text.isBlank()) {
            return CoachMessage(
                role = "assistant",
                text = "I couldn't generate a response. Please try rephrasing your question.",
                createdAt = System.currentTimeMillis()
            )
        }

        // Generate contextual suggestions based on user message keywords
        val suggestions = inferSuggestions(userMessage, text)

        return CoachMessage(
            role = "assistant",
            text = text.trim(),
            suggestions = suggestions,
            createdAt = System.currentTimeMillis()
        )
    }

    private fun inferSuggestions(userMessage: String, aiResponse: String): List<CoachSuggestion> {
        val lower = userMessage.lowercase()
        return when {
            lower.contains("miss") || lower.contains("skip") || lower.contains("bỏ") ->
                listOf(CoachSuggestion.PlanAdjustment(title = "Reschedule workout"))

            lower.contains("meal") || lower.contains("dinner") || lower.contains("eat") ||
                lower.contains("ăn") || lower.contains("bữa") ->
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
