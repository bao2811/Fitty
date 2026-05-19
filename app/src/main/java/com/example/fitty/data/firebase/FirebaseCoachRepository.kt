package com.example.fitty.data.firebase

import com.example.fitty.domain.model.*
import com.example.fitty.domain.repository.CoachRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.*
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseCoachRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : CoachRepository {

    private fun threads(uid: String) = firestore.collection("users").document(uid).collection("coach_threads")
    private fun messages(uid: String, threadId: String) = threads(uid).document(threadId).collection("messages")

    override suspend fun getOrCreateThread(uid: String): CoachThread {
        val existing = threads(uid).orderBy("lastMessageAt", Query.Direction.DESCENDING).limit(1).get().await()
        if (existing.documents.isNotEmpty()) {
            return existing.documents.first().toCoachThread()!!
        }
        val ref = threads(uid).document()
        val thread = mapOf("title" to "Chat", "lastMessagePreview" to "", "lastMessageAt" to FieldValue.serverTimestamp(),
            "messageCount" to 0, "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp())
        ref.set(thread).await()
        return CoachThread(id = ref.id, title = "Chat")
    }

    override suspend fun getThread(uid: String, threadId: String): CoachThread? {
        val doc = threads(uid).document(threadId).get().await()
        return if (doc.exists()) doc.toCoachThread() else null
    }

    override suspend fun getThreads(uid: String): List<CoachThread> =
        threads(uid).orderBy("lastMessageAt", Query.Direction.DESCENDING).get().await()
            .documents.mapNotNull { it.toCoachThread() }

    @Suppress("UNCHECKED_CAST")
    override suspend fun getMessages(uid: String, threadId: String, limit: Int): List<CoachMessage> =
        messages(uid, threadId).orderBy("createdAt", Query.Direction.ASCENDING).limit(limit.toLong()).get().await()
            .documents.mapNotNull { it.toCoachMessage(threadId) }

    override suspend fun saveMessage(uid: String, threadId: String, message: CoachMessage): Result<String> = try {
        val ref = if (message.id.isBlank()) messages(uid, threadId).document() else messages(uid, threadId).document(message.id)
        ref.set(message.toMap(), SetOptions.merge()).await(); Result.success(ref.id)
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun updateThreadPreview(uid: String, threadId: String, preview: String, messageCount: Int): Result<Unit> = try {
        threads(uid).document(threadId).update(mapOf("lastMessagePreview" to preview, "messageCount" to messageCount,
            "lastMessageAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp())).await()
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }

    private fun DocumentSnapshot.toCoachThread(): CoachThread? {
        if (!exists()) return null
        return CoachThread(id = id, title = getString("title").orEmpty(),
            lastMessagePreview = getString("lastMessagePreview").orEmpty(),
            lastMessageAt = getEpochMillis("lastMessageAt"),
            messageCount = getLong("messageCount")?.toInt() ?: 0)
    }

    @Suppress("UNCHECKED_CAST")
    private fun DocumentSnapshot.toCoachMessage(threadId: String): CoachMessage? {
        if (!exists()) return null
        val sugList = (get("suggestions") as? List<Map<String, Any?>>)?.map { s ->
            when (s["type"] as? String) {
                "plan_adjustment" -> {
                    val payload = s["payload"] as? Map<String, Any?> ?: emptyMap()
                    CoachSuggestion.PlanAdjustment(title = s["title"] as? String ?: "", actionLabel = s["actionLabel"] as? String ?: "Apply to Plan",
                        targetPlanId = payload["targetPlanId"] as? String ?: "", moveFromDate = payload["moveFromDate"] as? String ?: "",
                        moveToDate = payload["moveToDate"] as? String ?: "")
                }
                "meal_idea" -> {
                    val payload = s["payload"] as? Map<String, Any?> ?: emptyMap()
                    CoachSuggestion.MealIdea(title = s["title"] as? String ?: "", actionLabel = s["actionLabel"] as? String ?: "Save Meal Idea",
                        mealType = payload["mealType"] as? String ?: "", description = payload["description"] as? String ?: "",
                        estimatedCalories = (payload["estimatedCalories"] as? Number)?.toInt() ?: 0,
                        estimatedProtein = (payload["estimatedProtein"] as? Number)?.toInt() ?: 0)
                }
                else -> CoachSuggestion.General(title = s["title"] as? String ?: "", actionLabel = s["actionLabel"] as? String ?: "Got it")
            }
        } ?: emptyList()
        return CoachMessage(id = id, threadId = threadId, role = getString("role") ?: "user",
            text = getString("text").orEmpty(), suggestions = sugList, createdAt = getEpochMillis("createdAt"))
    }

    private fun CoachMessage.toMap(): Map<String, Any?> = mapOf("role" to role, "text" to text,
        "attachments" to attachments.map { mapOf("type" to it.type, "url" to it.url, "label" to it.label) },
        "suggestions" to suggestions.map { s ->
            when (s) {
                is CoachSuggestion.PlanAdjustment -> mapOf("type" to "plan_adjustment", "title" to s.title, "actionLabel" to s.actionLabel,
                    "payload" to mapOf("targetPlanId" to s.targetPlanId, "moveFromDate" to s.moveFromDate, "moveToDate" to s.moveToDate))
                is CoachSuggestion.MealIdea -> mapOf("type" to "meal_idea", "title" to s.title, "actionLabel" to s.actionLabel,
                    "payload" to mapOf("mealType" to s.mealType, "description" to s.description, "estimatedCalories" to s.estimatedCalories, "estimatedProtein" to s.estimatedProtein))
                is CoachSuggestion.General -> mapOf("type" to "general", "title" to s.title, "actionLabel" to s.actionLabel)
            }
        }, "createdAt" to FieldValue.serverTimestamp())

    private fun DocumentSnapshot.getEpochMillis(field: String): Long {
        val raw = get(field) ?: return 0L
        return when (raw) {
            is Timestamp -> raw.toDate().time
            is Date -> raw.time
            is Number -> raw.toLong()
            is String -> raw.toLongOrNull() ?: 0L
            else -> 0L
        }
    }
}
