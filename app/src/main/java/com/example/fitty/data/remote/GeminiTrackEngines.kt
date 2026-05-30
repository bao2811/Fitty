package com.example.fitty.data.remote

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.example.fitty.BuildConfig
import com.example.fitty.domain.model.BodyScan
import com.example.fitty.domain.model.BodyScanAnalysisEngine
import com.example.fitty.domain.model.BodyScanAnalysisResult
import com.example.fitty.domain.model.FoodItem
import com.example.fitty.domain.model.MealAnalysisEngine
import com.example.fitty.domain.model.MealAnalysisResult
import com.example.fitty.domain.model.MealLog
import dagger.hilt.android.qualifiers.ApplicationContext
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

private data class GeminiImagePayload(
    val base64: String,
    val mimeType: String
)

private class GeminiRetryableException(message: String) : RuntimeException(message)

private fun mapGeminiError(responseCode: Int, responseText: String): String {
    val detail = runCatching {
        JSONObject(responseText)
            .optJSONObject("error")
            ?.optString("message")
            .orEmpty()
    }.getOrDefault("").ifBlank { responseText.take(220) }

    return when (responseCode) {
        400 -> "Request Gemini không hợp lệ: $detail"
        401, 403 -> "API key Gemini không hợp lệ, bị giới hạn quyền, hoặc Generative Language API chưa bật."
        429 -> "Gemini hết quota hoặc bị rate limit. Kiểm tra billing/quota trong Google AI Studio."
        503 -> "Gemini đang quá tải. Thử lại sau hoặc đổi model."
        else -> "Gemini lỗi HTTP $responseCode: $detail"
    }
}

private fun Throwable.userFacingMessage(): String {
    val root = generateSequence(this) { it.cause }.last()
    return root.message?.takeIf { it.isNotBlank() } ?: message ?: "Lỗi không xác định."
}

private fun extractStringField(source: String, field: String): String? {
    val pattern = Regex("\"$field\"\\s*:\\s*\"([^\"]*)\"")
    return pattern.find(source)?.groupValues?.getOrNull(1)
}

private fun extractIntField(source: String, field: String): Int? {
    val pattern = Regex("\"$field\"\\s*:\\s*(-?\\d+)")
    return pattern.find(source)?.groupValues?.getOrNull(1)?.toIntOrNull()
}

private fun extractFloatField(source: String, field: String): Float? {
    val pattern = Regex("\"$field\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)")
    return pattern.find(source)?.groupValues?.getOrNull(1)?.toFloatOrNull()
}

@Singleton
class GeminiMealAnalysisEngine @Inject constructor(
    @ApplicationContext private val context: Context
) : MealAnalysisEngine {

    companion object {
        private const val API_KEY = BuildConfig.GEMINI_API_KEY
        private val MODELS = listOf(
            "gemini-2.5-flash",
            "gemini-2.0-flash",
            "gemini-2.0-flash-lite"
        )
        private const val BASE_URL_TEMPLATE =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent"
    }

    override suspend fun analyzeMealImage(imageUri: String): MealAnalysisResult =
        withContext(Dispatchers.IO) {
            try {
                val prompt = buildString {
                    append("You are a nutrition analysis AI. Analyze the meal in this photo.\n")
                    append("Identify all visible food items, estimate portion sizes, and calculate nutritional values.\n")
                    append("Respond ONLY with valid JSON in this exact format (no markdown, no explanation):\n")
                    append("{\n")
                    append("  \"mealType\": \"lunch\",\n")
                    append("  \"totalCalories\": 610,\n")
                    append("  \"totalProtein\": 42,\n")
                    append("  \"totalCarbs\": 68,\n")
                    append("  \"totalFat\": 18,\n")
                    append("  \"confidence\": 0.85,\n")
                    append("  \"foodItems\": [\n")
                    append("    {\"name\": \"Chicken breast\", \"quantity\": 150, \"unit\": \"g\", \"calories\": 248, \"protein\": 46, \"carbs\": 0, \"fat\": 5, \"confidence\": 0.9},\n")
                    append("    {\"name\": \"Rice\", \"quantity\": 200, \"unit\": \"g\", \"calories\": 260, \"protein\": 5, \"carbs\": 58, \"fat\": 1, \"confidence\": 0.88}\n")
                    append("  ]\n")
                    append("}\n")
                    append("Analyze the actual food in the photo. Determine mealType from time context or food type (breakfast/lunch/dinner/snack). List each distinct food item you can identify.")
                    append(" If there is no visible food, still return valid JSON with total values 0, confidence 0.1, and an empty foodItems array.")
                }

                val image = readImagePayload(imageUri)
                val responseText = callGeminiWithImage(prompt, image)
                parseMealResponse(responseText, imageUri)
            } catch (e: Exception) {
                throw IllegalStateException(
                    "Không phân tích được ảnh bữa ăn: ${e.userFacingMessage()}",
                    e
                )
            }
        }

    private fun readImagePayload(imageUri: String): GeminiImagePayload {
        return try {
            val uri = Uri.parse(imageUri)
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw IllegalStateException("Không mở được file ảnh.")
            val bytes = inputStream.use { it.readBytes() }
            if (bytes.isEmpty()) throw IllegalStateException("File ảnh rỗng.")
            GeminiImagePayload(
                base64 = Base64.encodeToString(bytes, Base64.NO_WRAP),
                mimeType = context.contentResolver.getType(uri)?.takeIf { it.startsWith("image/") }
                    ?: "image/jpeg"
            )
        } catch (e: Exception) {
            throw IllegalStateException("Không đọc được ảnh đã chụp. Hãy chụp lại hoặc chọn ảnh khác.", e)
        }
    }

    private fun callGeminiWithImage(prompt: String, image: GeminiImagePayload): String {
        val parts = JSONArray().apply {
            put(JSONObject().put("text", prompt))
            put(JSONObject().put("inlineData", JSONObject().apply {
                put("mimeType", image.mimeType)
                put("data", image.base64)
            }))
        }

        val requestBody = JSONObject().apply {
            put("contents", JSONArray().put(
                JSONObject().apply {
                    put("role", "user")
                    put("parts", parts)
                }
            ))
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.4)
                put("maxOutputTokens", 1400)
                put("responseMimeType", "application/json")
                put("responseSchema", mealResponseSchema())
            })
        }

        return executeGeminiRequest(requestBody)
    }

    private fun callGeminiTextOnly(prompt: String): String {
        val requestBody = JSONObject().apply {
            put("contents", JSONArray().put(
                JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().put(JSONObject().put("text", prompt)))
                }
            ))
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.8)
                put("maxOutputTokens", 500)
                put("responseMimeType", "application/json")
                put("responseSchema", mealResponseSchema())
            })
        }

        return executeGeminiRequest(requestBody)
    }

    private fun mealResponseSchema(): JSONObject = JSONObject().apply {
        put("type", "object")
        put("properties", JSONObject().apply {
            put("mealType", JSONObject().put("type", "string"))
            put("totalCalories", JSONObject().put("type", "integer"))
            put("totalProtein", JSONObject().put("type", "integer"))
            put("totalCarbs", JSONObject().put("type", "integer"))
            put("totalFat", JSONObject().put("type", "integer"))
            put("confidence", JSONObject().put("type", "number"))
            put("foodItems", JSONObject().apply {
                put("type", "array")
                put("items", JSONObject().apply {
                    put("type", "object")
                    put("properties", JSONObject().apply {
                        put("name", JSONObject().put("type", "string"))
                        put("quantity", JSONObject().put("type", "integer"))
                        put("unit", JSONObject().put("type", "string"))
                        put("calories", JSONObject().put("type", "integer"))
                        put("protein", JSONObject().put("type", "integer"))
                        put("carbs", JSONObject().put("type", "integer"))
                        put("fat", JSONObject().put("type", "integer"))
                        put("confidence", JSONObject().put("type", "number"))
                    })
                    put(
                        "required",
                        JSONArray().put("name").put("quantity").put("unit").put("calories")
                            .put("protein").put("carbs").put("fat").put("confidence")
                    )
                })
            })
        })
        put(
            "required",
            JSONArray().put("mealType").put("totalCalories").put("totalProtein")
                .put("totalCarbs").put("totalFat").put("confidence").put("foodItems")
        )
    }

    private fun executeGeminiRequest(requestBody: JSONObject): String {
        require(API_KEY.isNotBlank()) { "Thiếu GEMINI_API_KEY trong file .env." }
        var lastException: Exception? = null
        for (model in MODELS) {
            try {
                return executeGeminiRequest(model, requestBody)
            } catch (e: GeminiRetryableException) {
                lastException = e
            }
        }
        throw lastException ?: RuntimeException("All Gemini image analysis models failed")
    }

    private fun executeGeminiRequest(model: String, requestBody: JSONObject): String {
        val url = URL(String.format(BASE_URL_TEMPLATE, model) + "?key=$API_KEY")
        val connection = url.openConnection() as HttpURLConnection
        connection.apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            doOutput = true
            connectTimeout = 30_000
            readTimeout = 60_000
        }
        connection.outputStream.use { it.write(requestBody.toString().toByteArray(Charsets.UTF_8)) }

        val responseCode = connection.responseCode
        val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
        val responseText = BufferedReader(InputStreamReader(stream)).use { it.readText() }
        if (responseCode in 200..299) return responseText

        val mappedError = mapGeminiError(responseCode, responseText)
        if (responseCode == 429 || responseCode == 503) {
            throw GeminiRetryableException(mappedError)
        }
        throw RuntimeException(mappedError)
    }

    private fun parseMealResponse(responseJson: String, imageUri: String): MealAnalysisResult {
        val json = JSONObject(responseJson)
        val text = json.optJSONArray("candidates")
            ?.optJSONObject(0)?.optJSONObject("content")
            ?.optJSONArray("parts")?.optJSONObject(0)?.optString("text", "") ?: ""
        if (text.isBlank()) {
            val finishReason = json.optJSONArray("candidates")
                ?.optJSONObject(0)?.optString("finishReason", "").orEmpty()
            throw IllegalStateException(
                if (finishReason.isNotBlank()) "Gemini không trả nội dung phân tích. finishReason=$finishReason."
                else "Gemini không trả nội dung phân tích."
            )
        }

        // Extract JSON from response (may be wrapped in markdown code block)
        val cleanJson = text.replace("```json", "").replace("```", "").trim()
        if (cleanJson.isBlank()) {
            throw IllegalStateException("Gemini returned an empty meal analysis payload.")
        }

        return try {
            val meal = JSONObject(cleanJson)
            val foodItems = mutableListOf<FoodItem>()
            val itemsArray = meal.optJSONArray("foodItems") ?: JSONArray()
            for (i in 0 until itemsArray.length()) {
                val item = itemsArray.getJSONObject(i)
                foodItems.add(FoodItem(
                    name = item.optString("name", "Unknown"),
                    quantity = item.optInt("quantity", 100),
                    unit = item.optString("unit", "g"),
                    calories = item.optInt("calories", 0),
                    protein = item.optInt("protein", 0),
                    carbs = item.optInt("carbs", 0),
                    fat = item.optInt("fat", 0),
                    confidence = item.optDouble("confidence", 0.8).toFloat()
                ))
            }
            val confidence = meal.optDouble("confidence", 0.85).toFloat()
            MealAnalysisResult(
                mealLog = MealLog(
                    mealType = meal.optString("mealType", "meal"),
                    source = "gemini",
                    imageUrl = imageUri,
                    totalCalories = meal.optInt("totalCalories", foodItems.sumOf { it.calories }),
                    totalProtein = meal.optInt("totalProtein", foodItems.sumOf { it.protein }),
                    totalCarbs = meal.optInt("totalCarbs", foodItems.sumOf { it.carbs }),
                    totalFat = meal.optInt("totalFat", foodItems.sumOf { it.fat }),
                    confidence = confidence,
                    foodItems = foodItems
                ),
                confidence = confidence
            )
        } catch (e: Exception) {
            parsePartialMealResponse(cleanJson, imageUri)
                ?: throw IllegalStateException(
                    "Gemini trả kết quả dinh dưỡng không hoàn chỉnh. Hãy bấm Phân tích lại.",
                    e
                )
        }
    }

    private fun parsePartialMealResponse(cleanJson: String, imageUri: String): MealAnalysisResult? {
        val mealType = extractStringField(cleanJson, "mealType") ?: return null
        val totalCalories = extractIntField(cleanJson, "totalCalories") ?: return null
        val totalProtein = extractIntField(cleanJson, "totalProtein") ?: 0
        val totalCarbs = extractIntField(cleanJson, "totalCarbs") ?: 0
        val totalFat = extractIntField(cleanJson, "totalFat") ?: 0
        val confidence = extractFloatField(cleanJson, "confidence") ?: 0.5f

        return MealAnalysisResult(
            mealLog = MealLog(
                mealType = mealType,
                source = "gemini_partial",
                imageUrl = imageUri,
                totalCalories = totalCalories,
                totalProtein = totalProtein,
                totalCarbs = totalCarbs,
                totalFat = totalFat,
                confidence = confidence,
                foodItems = emptyList()
            ),
            confidence = confidence
        )
    }
}

@Singleton
class GeminiBodyScanAnalysisEngine @Inject constructor(
    @ApplicationContext private val context: Context
) : BodyScanAnalysisEngine {

    companion object {
        private const val API_KEY = BuildConfig.GEMINI_API_KEY
        private val MODELS = listOf(
            "gemini-2.5-flash",
            "gemini-2.0-flash",
            "gemini-2.0-flash-lite"
        )
        private const val BASE_URL_TEMPLATE =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent"
    }

    override suspend fun analyzeBodyScan(frontImageUri: String, sideImageUri: String?): BodyScanAnalysisResult =
        withContext(Dispatchers.IO) {
            try {
                val prompt = buildString {
                    append("You are a body composition analysis AI for a Vietnamese fitness app.\n")
                    append("Analyze this body progress photo, evaluate the physique, estimate body fat percentage, and assess posture.\n")
                    append("Respond ONLY with valid JSON in Vietnamese, with no markdown and no explanation.\n")
                    append("{\n")
                    append("  \"summary\": \"Tư thế tổng thể khá tốt, phần thân trên phát triển tương đối cân đối.\",\n")
                    append("  \"estimatedBodyFatPercent\": 18.5,\n")
                    append("  \"postureScore\": 75,\n")
                    append("  \"confidence\": 0.78\n")
                    append("}\n")
                    append("The summary field must be written in Vietnamese. Provide a realistic body fat estimate (12-30%), posture score (50-95), and a concise Vietnamese observation summary.")
                }

                val image = readImagePayload(frontImageUri)
                val responseText = callGeminiWithImage(prompt, image)
                parseBodyResponse(responseText, frontImageUri, sideImageUri)
            } catch (e: Exception) {
                throw IllegalStateException(
                    "Không phân tích được ảnh cơ thể: ${e.userFacingMessage()}",
                    e
                )
            }
        }

    private fun readImagePayload(imageUri: String): GeminiImagePayload {
        return try {
            val uri = Uri.parse(imageUri)
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw IllegalStateException("Không mở được file ảnh.")
            val bytes = inputStream.use { it.readBytes() }
            if (bytes.isEmpty()) throw IllegalStateException("File ảnh rỗng.")
            GeminiImagePayload(
                base64 = Base64.encodeToString(bytes, Base64.NO_WRAP),
                mimeType = context.contentResolver.getType(uri)?.takeIf { it.startsWith("image/") }
                    ?: "image/jpeg"
            )
        } catch (e: Exception) {
            throw IllegalStateException("Không đọc được ảnh đã chụp. Hãy chụp lại hoặc chọn ảnh khác.", e)
        }
    }

    private fun callGeminiWithImage(prompt: String, image: GeminiImagePayload): String {
        val parts = JSONArray().apply {
            put(JSONObject().put("text", prompt))
            put(JSONObject().put("inlineData", JSONObject().apply {
                put("mimeType", image.mimeType)
                put("data", image.base64)
            }))
        }

        val requestBody = JSONObject().apply {
            put("contents", JSONArray().put(
                JSONObject().apply {
                    put("role", "user")
                    put("parts", parts)
                }
            ))
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.4)
                put("maxOutputTokens", 900)
                put("responseMimeType", "application/json")
                put("responseSchema", bodyScanResponseSchema())
            })
        }

        return executeGeminiRequest(requestBody)
    }

    private fun callGeminiTextOnly(prompt: String): String {
        val requestBody = JSONObject().apply {
            put("contents", JSONArray().put(
                JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().put(JSONObject().put("text", prompt)))
                }
            ))
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.8)
                put("maxOutputTokens", 300)
                put("responseMimeType", "application/json")
                put("responseSchema", bodyScanResponseSchema())
            })
        }

        return executeGeminiRequest(requestBody)
    }

    private fun bodyScanResponseSchema(): JSONObject = JSONObject().apply {
        put("type", "object")
        put("properties", JSONObject().apply {
            put("summary", JSONObject().put("type", "string"))
            put("estimatedBodyFatPercent", JSONObject().put("type", "number"))
            put("postureScore", JSONObject().put("type", "integer"))
            put("confidence", JSONObject().put("type", "number"))
        })
        put(
            "required",
            JSONArray().put("summary").put("estimatedBodyFatPercent").put("postureScore").put("confidence")
        )
    }

    private fun executeGeminiRequest(requestBody: JSONObject): String {
        require(API_KEY.isNotBlank()) { "Thiếu GEMINI_API_KEY trong file .env." }
        var lastException: Exception? = null
        for (model in MODELS) {
            try {
                return executeGeminiRequest(model, requestBody)
            } catch (e: GeminiRetryableException) {
                lastException = e
            }
        }
        throw lastException ?: RuntimeException("All Gemini body scan models failed")
    }

    private fun executeGeminiRequest(model: String, requestBody: JSONObject): String {
        val url = URL(String.format(BASE_URL_TEMPLATE, model) + "?key=$API_KEY")
        val connection = url.openConnection() as HttpURLConnection
        connection.apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            doOutput = true
            connectTimeout = 30_000
            readTimeout = 60_000
        }
        connection.outputStream.use { it.write(requestBody.toString().toByteArray(Charsets.UTF_8)) }

        val responseCode = connection.responseCode
        val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
        val responseText = BufferedReader(InputStreamReader(stream)).use { it.readText() }
        if (responseCode in 200..299) return responseText

        val mappedError = mapGeminiError(responseCode, responseText)
        if (responseCode == 429 || responseCode == 503) {
            throw GeminiRetryableException(mappedError)
        }
        throw RuntimeException(mappedError)
    }

    private fun parseBodyResponse(responseJson: String, frontUri: String, sideUri: String?): BodyScanAnalysisResult {
        val json = JSONObject(responseJson)
        val text = json.optJSONArray("candidates")
            ?.optJSONObject(0)?.optJSONObject("content")
            ?.optJSONArray("parts")?.optJSONObject(0)?.optString("text", "") ?: ""
        if (text.isBlank()) {
            val finishReason = json.optJSONArray("candidates")
                ?.optJSONObject(0)?.optString("finishReason", "").orEmpty()
            throw IllegalStateException(
                if (finishReason.isNotBlank()) "Gemini không trả nội dung phân tích. finishReason=$finishReason."
                else "Gemini không trả nội dung phân tích."
            )
        }

        val cleanJson = text.replace("```json", "").replace("```", "").trim()
        if (cleanJson.isBlank()) {
            throw IllegalStateException("Gemini returned an empty body scan payload.")
        }

        return try {
            val body = JSONObject(cleanJson)
            val confidence = body.optDouble("confidence", 0.78).toFloat()
            BodyScanAnalysisResult(
                bodyScan = BodyScan(
                    capturedAt = System.currentTimeMillis(),
                    frontImageUrl = frontUri, sideImageUrl = sideUri,
                    summary = body.optString("summary", "Analysis complete"),
                    confidence = confidence,
                    estimatedBodyFatPercent = body.optDouble("estimatedBodyFatPercent", 20.0).toFloat(),
                    postureScore = body.optInt("postureScore", 70),
                    status = "processed"
                ),
                confidence = confidence
            )
        } catch (e: Exception) {
            throw IllegalStateException(
                "Gemini trả kết quả body scan không hoàn chỉnh. Hãy bấm Phân tích lại.",
                e
            )
        }
    }
}
