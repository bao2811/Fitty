package com.example.fitty.data.firebase

import android.net.Uri
import com.example.fitty.domain.model.FittyOnboarding
import com.example.fitty.domain.model.FittyProfile
import com.example.fitty.domain.model.FittySettings
import com.example.fitty.domain.model.FittyStats
import com.example.fitty.domain.model.FittyUser
import com.example.fitty.domain.repository.UserRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseUserRepository @Inject constructor(
    private val remoteDataSource: FirebaseUserRemoteDataSource,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : UserRepository {

    override suspend fun getCurrentUser(uid: String?): FittyUser? =
        remoteDataSource.getCurrentUser(uid)

    override suspend fun updateProfile(uid: String, profile: FittyProfile): Result<Unit> = try {
        firestore.collection("users").document(uid).update(mapOf(
            "profile.age" to profile.age,
            "profile.gender" to profile.gender,
            "profile.heightCm" to profile.heightCm,
            "profile.weightKg" to profile.weightKg,
            "profile.targetWeightKg" to profile.targetWeightKg,
            "profile.activityLevel" to profile.activityLevel,
            "profile.fitnessLevel" to profile.fitnessLevel,
            "profile.primaryGoal" to profile.primaryGoal,
            "profile.injuryNote" to profile.injuryNote,
            "updatedAt" to FieldValue.serverTimestamp()
        )).await()
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun updateOnboarding(uid: String, onboarding: FittyOnboarding): Result<Unit> = try {
        firestore.collection("users").document(uid).update(mapOf(
            "onboarding.workoutDays" to onboarding.workoutDays,
            "onboarding.workoutDurationMinutes" to onboarding.workoutDurationMinutes,
            "onboarding.preferredTime" to onboarding.preferredTime,
            "onboarding.equipmentAccess" to onboarding.equipmentAccess,
            "onboarding.nutritionStyle" to onboarding.nutritionStyle,
            "onboarding.dietaryRestrictions" to onboarding.dietaryRestrictions,
            "updatedAt" to FieldValue.serverTimestamp()
        )).await()
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun updateSettings(uid: String, settings: FittySettings): Result<Unit> = try {
        firestore.collection("users").document(uid).update(mapOf(
            "settings.language" to settings.language,
            "settings.themeMode" to settings.themeMode,
            "settings.weightUnit" to settings.weightUnit,
            "settings.heightUnit" to settings.heightUnit,
            "settings.energyUnit" to settings.energyUnit,
            "settings.aiConsent" to settings.aiConsent,
            "settings.photoStorageEnabled" to settings.photoStorageEnabled,
            "updatedAt" to FieldValue.serverTimestamp()
        )).await()
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun updateGoal(uid: String, primaryGoal: String, targetWeightKg: Int?): Result<Unit> = try {
        val updates = mutableMapOf<String, Any?>(
            "profile.primaryGoal" to primaryGoal,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (targetWeightKg != null) updates["profile.targetWeightKg"] = targetWeightKg
        firestore.collection("users").document(uid).update(updates).await()
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun updateStats(uid: String, stats: FittyStats): Result<Unit> = try {
        firestore.collection("users").document(uid).update(mapOf(
            "stats.currentStreak" to stats.currentStreak,
            "stats.bestStreak" to stats.bestStreak,
            "stats.totalWorkouts" to stats.totalWorkouts,
            "stats.mealsLogged" to stats.mealsLogged,
            "stats.achievementsUnlocked" to stats.achievementsUnlocked,
            "stats.lastActiveDate" to stats.lastActiveDate,
            "stats.streakActiveDates" to stats.streakActiveDates,
            "updatedAt" to FieldValue.serverTimestamp()
        )).await()
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun deleteUserData(uid: String): Result<Unit> = try {
        val subcollections = listOf("plan_instances", "workout_sessions", "meal_logs",
            "body_scans", "body_measurements", "daily_summaries", "coach_threads")
        subcollections.forEach { collName ->
            val docs = firestore.collection("users").document(uid).collection(collName).get().await()
            docs.documents.forEach { doc -> doc.reference.delete().await() }
        }
        firestore.collection("users").document(uid).delete().await()
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun updateDisplayName(uid: String, name: String): Result<Unit> = try {
        firestore.collection("users").document(uid).update(mapOf(
            "username" to name,
            "displayName" to name,
            "updatedAt" to FieldValue.serverTimestamp()
        )).await()
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun uploadProfilePhoto(uid: String, imageUri: String): Result<String> = try {
        val ref = storage.reference.child("profile_photos/$uid.jpg")
        ref.putFile(Uri.parse(imageUri)).await()
        val downloadUrl = ref.downloadUrl.await().toString()
        firestore.collection("users").document(uid).update(mapOf(
            "photoUrl" to downloadUrl,
            "updatedAt" to FieldValue.serverTimestamp()
        )).await()
        Result.success(downloadUrl)
    } catch (e: Exception) { Result.failure(e) }
}
