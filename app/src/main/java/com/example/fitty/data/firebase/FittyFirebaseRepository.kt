package com.example.fitty.data.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class FittyFirebaseRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun createPasswordUser(
        username: String,
        email: String,
        password: String
    ): FittyAuthResult {
        val normalizedUsername = username.trim()
        val normalizedUsernameKey = normalizedUsername.lowercase(Locale.US)
        val normalizedEmail = email.trim().lowercase(Locale.US)
        if (normalizedUsername.isBlank()) {
            return FittyAuthResult(errorMessage = "Username is required")
        }

        return try {
            val authResult = auth.createUserWithEmailAndPassword(normalizedEmail, password).await()
            val firebaseUser = authResult.user ?: return FittyAuthResult(errorMessage = "Account could not be created")
            val userDoc = buildBaseUserDocument(
                user = firebaseUser,
                username = normalizedUsername,
                usernameKey = normalizedUsernameKey,
                authProvider = AUTH_PROVIDER_PASSWORD,
                guest = false
            )
            userDocument(firebaseUser.uid).set(userDoc, SetOptions.merge()).await()
            FittyAuthResult(user = getCurrentUser())
        } catch (error: Exception) {
            FittyAuthResult(errorMessage = error.message ?: "Could not create account")
        }
    }

    suspend fun signInWithPassword(
        identifier: String,
        password: String
    ): FittyAuthResult {
        val email = identifier.trim().lowercase(Locale.US)
        if ("@" !in email) {
            return FittyAuthResult(errorMessage = "Enter a valid email address")
        }

        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: return FittyAuthResult(errorMessage = "Could not sign in")
            ensureUserDocument(firebaseUser)
            FittyAuthResult(user = getCurrentUser())
        } catch (error: Exception) {
            FittyAuthResult(errorMessage = error.message ?: "Username/email or password is incorrect")
        }
    }

    suspend fun continueAsGuest(): FittyAuthResult {
        return try {
            val currentUser = auth.currentUser
            val firebaseUser = if (currentUser?.isAnonymous == true) {
                currentUser
            } else {
                auth.signInAnonymously().await().user
            } ?: return FittyAuthResult(errorMessage = "Guest mode is unavailable")

            val guestName = "Guest"
            val userDoc = buildBaseUserDocument(
                user = firebaseUser,
                username = guestName,
                usernameKey = "",
                authProvider = AUTH_PROVIDER_GUEST,
                guest = true
            )
            userDocument(firebaseUser.uid).set(userDoc, SetOptions.merge()).await()
            FittyAuthResult(user = getCurrentUser())
        } catch (error: Exception) {
            FittyAuthResult(errorMessage = error.message ?: "Guest mode is unavailable")
        }
    }

    suspend fun saveOnboardingAnswers(
        uid: String,
        answers: FittyOnboardingAnswers
    ) {
        val normalizedWorkoutDays = answers.workoutDays
            .map { it.lowercase(Locale.US).take(3) }
            .sorted()

        val userPayload = hashMapOf<String, Any?>(
            "onboardingCompleted" to false,
            "profile" to mapOf(
                "age" to answers.age,
                "gender" to "",
                "heightCm" to answers.heightCm,
                "weightKg" to answers.weightKg,
                "targetWeightKg" to answers.targetWeightKg,
                "activityLevel" to estimateActivityLevel(normalizedWorkoutDays.size),
                "fitnessLevel" to answers.fitnessLevel.toSchemaValue(),
                "primaryGoal" to answers.goal.toSchemaValue(),
                "injuryNote" to answers.injuryNote.trim()
            ),
            "onboarding" to mapOf(
                "workoutDays" to normalizedWorkoutDays,
                "workoutDurationMinutes" to answers.durationMinutes,
                "preferredTime" to answers.preferredTime.toSchemaValue(),
                "equipmentAccess" to answers.equipment.toSchemaValue(),
                "nutritionStyle" to answers.nutrition.toSchemaValue(),
                "dietaryRestrictions" to answers.restrictions.map { it.toSchemaValue() }.sorted()
            ),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        userDocument(uid).set(userPayload, SetOptions.merge()).await()

        saveReminders(uid = uid, reminders = answers.reminders)
        saveStarterPlan(uid = uid, answers = answers, normalizedWorkoutDays = normalizedWorkoutDays)
    }

    suspend fun markOnboardingCompleted(uid: String) {
        val payload = mapOf(
            "onboardingCompleted" to true,
            "updatedAt" to FieldValue.serverTimestamp(),
            "stats" to mapOf(
                "activePlanId" to STARTER_PLAN_ID
            )
        )
        userDocument(uid).set(payload, SetOptions.merge()).await()
        userDocument(uid)
            .collection(COLLECTION_PLAN_INSTANCES)
            .document(STARTER_PLAN_ID)
            .set(
                mapOf(
                    "status" to PLAN_STATUS_ACTIVE,
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )
            .await()
    }

    suspend fun getStartupState(): FittyStartupState {
        val firebaseUser = auth.currentUser ?: return FittyStartupState()
        ensureUserDocument(firebaseUser)
        val user = getCurrentUser(uid = firebaseUser.uid)
        return FittyStartupState(
            uid = user?.uid ?: firebaseUser.uid,
            displayName = user?.displayName.orEmpty(),
            isGuest = user?.guest ?: firebaseUser.isAnonymous,
            isSignedIn = !(user?.guest ?: firebaseUser.isAnonymous),
            onboardingCompleted = user?.onboardingCompleted ?: false
        )
    }

    suspend fun getCurrentUser(uid: String? = auth.currentUser?.uid): FittyUser? {
        val resolvedUid = uid ?: return null
        val snapshot = userDocument(resolvedUid).get().await()
        return if (snapshot.exists()) snapshot.toFittyUser() else null
    }

    fun signOut() {
        auth.signOut()
    }

    suspend fun syncNotificationToken(token: String) {
        val currentUid = auth.currentUser?.uid ?: return
        val normalizedToken = token.trim()
        if (normalizedToken.isBlank()) return

        userDocument(currentUid)
            .collection(COLLECTION_NOTIFICATION_TOKENS)
            .document(normalizedToken)
            .set(
                mapOf(
                    "token" to normalizedToken,
                    "platform" to "android",
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )
            .await()
    }

    private suspend fun ensureUserDocument(user: FirebaseUser) {
        val docRef = userDocument(user.uid)
        val snapshot = docRef.get().await()
        if (snapshot.exists()) {
            docRef.set(
                mapOf(
                    "email" to (user.email ?: snapshot.getString("email").orEmpty()),
                    "displayName" to displayNameFor(user, snapshot.getString("displayName")),
                    "guest" to user.isAnonymous,
                    "authProvider" to snapshot.getString("authProvider").orEmpty().ifBlank {
                        if (user.isAnonymous) AUTH_PROVIDER_GUEST else AUTH_PROVIDER_PASSWORD
                    },
                    "lastLoginAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            ).await()
            return
        }

        val emailLocalPart = user.email
            ?.substringBefore("@")
            ?.replace(Regex("[^A-Za-z0-9_]"), "_")
            ?.takeIf { it.isNotBlank() }
            ?: if (user.isAnonymous) "guest" else "fitty_user"
        docRef.set(
            buildBaseUserDocument(
                user = user,
                username = emailLocalPart,
                usernameKey = if (user.isAnonymous) "" else emailLocalPart.lowercase(Locale.US),
                authProvider = if (user.isAnonymous) AUTH_PROVIDER_GUEST else AUTH_PROVIDER_PASSWORD,
                guest = user.isAnonymous
            ),
            SetOptions.merge()
        ).await()
    }

    private suspend fun saveReminders(uid: String, reminders: Set<String>) {
        val reminderCollection = userDocument(uid).collection(COLLECTION_REMINDERS)
        val existing = reminderCollection.get().await()
        existing.documents.forEach { it.reference.delete() }
        reminders.forEach { reminder ->
            val reminderId = reminder.toSchemaValue().replace("_reminder", "")
            reminderCollection.document(reminderId).set(
                mapOf(
                    "type" to reminderId,
                    "enabled" to true,
                    "label" to reminder.trim(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            ).await()
        }
    }

    private suspend fun saveStarterPlan(
        uid: String,
        answers: FittyOnboardingAnswers,
        normalizedWorkoutDays: List<String>
    ) {
        val planRef = userDocument(uid)
            .collection(COLLECTION_PLAN_INSTANCES)
            .document(STARTER_PLAN_ID)
        val nextWorkoutDate = computeNextWorkoutDate(normalizedWorkoutDays)
        planRef.set(
            mapOf(
                "sourceProgramId" to "starter_template",
                "name" to "Starter Plan",
                "goal" to answers.goal.toSchemaValue(),
                "durationWeeks" to 4,
                "workoutsPerWeek" to normalizedWorkoutDays.size.coerceAtLeast(1),
                "equipment" to answers.equipment.toSchemaValue(),
                "trainingStyle" to trainingStyleForGoal(answers.goal),
                "status" to PLAN_STATUS_DRAFT,
                "explanation" to "Generated from onboarding answers.",
                "currentWeek" to 1,
                "nextWorkoutDate" to nextWorkoutDate,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            SetOptions.merge()
        ).await()

        val scheduledWorkoutTitles = listOf(
            "Full Body Basics",
            "Cardio + Core",
            "Strength Foundations",
            "Mobility Reset"
        )
        val scheduledCollection = planRef.collection(COLLECTION_SCHEDULED_WORKOUTS)
        val existingDocs = scheduledCollection.get().await()
        existingDocs.documents.forEach { it.reference.delete() }

        val today = LocalDate.now(appZoneId())
        normalizedWorkoutDays.forEachIndexed { index, day ->
            val date = nextDateForDay(today, day)
            val workoutId = "${date.format(DATE_KEY_FORMATTER)}_${day}"
            scheduledCollection.document(workoutId).set(
                mapOf(
                    "dateKey" to date.format(DATE_KEY_FORMATTER),
                    "weekNumber" to 1,
                    "orderInWeek" to index + 1,
                    "title" to scheduledWorkoutTitles[index % scheduledWorkoutTitles.size],
                    "durationMinutes" to answers.durationMinutes,
                    "estimatedCalories" to estimateCalories(answers.durationMinutes),
                    "difficulty" to answers.fitnessLevel.toSchemaValue(),
                    "equipment" to answers.equipment.toSchemaValue(),
                    "status" to "scheduled",
                    "explanation" to buildWorkoutExplanation(answers),
                    "replacedFromWorkoutId" to null,
                    "exercises" to starterExercisesForGoal(answers.goal),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            ).await()
        }
    }

    private fun buildBaseUserDocument(
        user: FirebaseUser,
        username: String,
        usernameKey: String,
        authProvider: String,
        guest: Boolean
    ): Map<String, Any?> {
        return mapOf(
            "email" to user.email.orEmpty(),
            "displayName" to displayNameFor(user, username),
            "username" to username,
            "usernameNormalized" to usernameKey,
            "photoUrl" to user.photoUrl?.toString(),
            "authProvider" to authProvider,
            "guest" to guest,
            "onboardingCompleted" to false,
            "profile" to mapOf(
                "age" to null,
                "gender" to "",
                "heightCm" to null,
                "weightKg" to null,
                "targetWeightKg" to null,
                "activityLevel" to "",
                "fitnessLevel" to "",
                "primaryGoal" to "",
                "injuryNote" to ""
            ),
            "onboarding" to mapOf(
                "workoutDays" to emptyList<String>(),
                "workoutDurationMinutes" to null,
                "preferredTime" to "",
                "equipmentAccess" to "",
                "nutritionStyle" to "",
                "dietaryRestrictions" to emptyList<String>()
            ),
            "settings" to mapOf(
                "language" to "en",
                "themeMode" to "system",
                "weightUnit" to "kg",
                "heightUnit" to "cm",
                "energyUnit" to "kcal",
                "aiConsent" to true,
                "photoStorageEnabled" to true
            ),
            "stats" to mapOf(
                "activePlanId" to "",
                "currentStreak" to 0,
                "bestStreak" to 0,
                "totalWorkouts" to 0,
                "mealsLogged" to 0,
                "achievementsUnlocked" to 0
            ),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
            "lastLoginAt" to FieldValue.serverTimestamp()
        )
    }

    private fun displayNameFor(user: FirebaseUser, fallback: String?): String {
        return user.displayName
            ?.takeIf { it.isNotBlank() }
            ?: fallback.orEmpty().ifBlank {
                user.email?.substringBefore("@") ?: "Fitty User"
            }
    }

    private fun computeNextWorkoutDate(workoutDays: List<String>): String {
        if (workoutDays.isEmpty()) {
            return LocalDate.now(appZoneId()).format(DATE_KEY_FORMATTER)
        }
        val today = LocalDate.now(appZoneId())
        val nextDate = workoutDays
            .map { nextDateForDay(today, it) }
            .minOrNull()
            ?: today
        return nextDate.format(DATE_KEY_FORMATTER)
    }

    private fun nextDateForDay(startDate: LocalDate, dayKey: String): LocalDate {
        val targetIndex = DAY_ORDER.indexOf(dayKey.lowercase(Locale.US).take(3)).takeIf { it >= 0 } ?: 0
        val currentIndex = startDate.dayOfWeek.value % 7
        val delta = (targetIndex - currentIndex + 7) % 7
        return startDate.plusDays(delta.toLong())
    }

    private fun estimateActivityLevel(workoutDayCount: Int): String = when {
        workoutDayCount >= 5 -> "high"
        workoutDayCount >= 3 -> "moderate"
        workoutDayCount >= 1 -> "light"
        else -> "sedentary"
    }

    private fun trainingStyleForGoal(goal: String): String = when (goal.toSchemaValue()) {
        "gain_muscle" -> "strength"
        "improve_endurance" -> "cardio"
        "improve_flexibility" -> "mobility"
        else -> "full_body"
    }

    private fun buildWorkoutExplanation(answers: FittyOnboardingAnswers): String {
        return "Selected for your ${answers.goal.lowercase(Locale.US)} goal, ${answers.fitnessLevel.lowercase(Locale.US)} level, and ${answers.preferredTime.lowercase(Locale.US)} schedule."
    }

    private fun estimateCalories(durationMinutes: Int): Int = (durationMinutes * 5.5).toInt()

    private fun starterExercisesForGoal(goal: String): List<Map<String, Any?>> {
        return when (goal.toSchemaValue()) {
            "gain_muscle" -> listOf(
                starterExercise("push_up", "Push Up", 3, "10"),
                starterExercise("split_squat", "Split Squat", 3, "10"),
                starterExercise("plank", "Plank", 3, durationSeconds = 30)
            )
            "improve_flexibility" -> listOf(
                starterExercise("cat_cow", "Cat Cow", 2, "10"),
                starterExercise("worlds_greatest_stretch", "World's Greatest Stretch", 2, "8"),
                starterExercise("dead_bug", "Dead Bug", 3, "10")
            )
            else -> listOf(
                starterExercise("bodyweight_squat", "Bodyweight Squat", 3, "12"),
                starterExercise("incline_push_up", "Incline Push Up", 3, "10"),
                starterExercise("marching_glute_bridge", "Marching Glute Bridge", 3, "12")
            )
        }
    }

    private fun starterExercise(
        exerciseId: String,
        name: String,
        sets: Int,
        reps: String? = null,
        durationSeconds: Int? = null
    ): Map<String, Any?> {
        return mapOf(
            "exerciseId" to exerciseId,
            "name" to name,
            "sets" to sets,
            "reps" to reps,
            "durationSeconds" to durationSeconds
        )
    }

    private fun appZoneId(): ZoneId = ZoneId.systemDefault()

    private fun userDocument(uid: String) = firestore.collection(COLLECTION_USERS).document(uid)

    private fun com.google.firebase.firestore.DocumentSnapshot.toFittyUser(): FittyUser {
        val profileMap = get("profile") as? Map<*, *> ?: emptyMap<String, Any?>()
        val onboardingMap = get("onboarding") as? Map<*, *> ?: emptyMap<String, Any?>()
        val statsMap = get("stats") as? Map<*, *> ?: emptyMap<String, Any?>()
        val settingsMap = get("settings") as? Map<*, *> ?: emptyMap<String, Any?>()

        return FittyUser(
            uid = id,
            email = getString("email").orEmpty(),
            displayName = getString("displayName").orEmpty().ifBlank { "Fitty User" },
            username = getString("username").orEmpty(),
            photoUrl = getString("photoUrl"),
            authProvider = getString("authProvider").orEmpty(),
            guest = getBoolean("guest") ?: false,
            onboardingCompleted = getBoolean("onboardingCompleted") ?: false,
            profile = FittyProfile(
                age = profileMap.intValue("age"),
                gender = profileMap.stringValue("gender"),
                heightCm = profileMap.intValue("heightCm"),
                weightKg = profileMap.intValue("weightKg"),
                targetWeightKg = profileMap.intValue("targetWeightKg"),
                activityLevel = profileMap.stringValue("activityLevel"),
                fitnessLevel = profileMap.stringValue("fitnessLevel"),
                primaryGoal = profileMap.stringValue("primaryGoal"),
                injuryNote = profileMap.stringValue("injuryNote")
            ),
            onboarding = FittyOnboarding(
                workoutDays = onboardingMap.stringListValue("workoutDays"),
                workoutDurationMinutes = onboardingMap.intValue("workoutDurationMinutes"),
                preferredTime = onboardingMap.stringValue("preferredTime"),
                equipmentAccess = onboardingMap.stringValue("equipmentAccess"),
                nutritionStyle = onboardingMap.stringValue("nutritionStyle"),
                dietaryRestrictions = onboardingMap.stringListValue("dietaryRestrictions")
            ),
            stats = FittyStats(
                activePlanId = statsMap.stringValue("activePlanId"),
                currentStreak = statsMap.intValue("currentStreak") ?: 0,
                bestStreak = statsMap.intValue("bestStreak") ?: 0,
                totalWorkouts = statsMap.intValue("totalWorkouts") ?: 0,
                mealsLogged = statsMap.intValue("mealsLogged") ?: 0,
                achievementsUnlocked = statsMap.intValue("achievementsUnlocked") ?: 0
            ),
            settings = FittySettings(
                language = settingsMap.stringValue("language").ifBlank { "en" },
                themeMode = settingsMap.stringValue("themeMode").ifBlank { "system" },
                weightUnit = settingsMap.stringValue("weightUnit").ifBlank { "kg" },
                heightUnit = settingsMap.stringValue("heightUnit").ifBlank { "cm" },
                energyUnit = settingsMap.stringValue("energyUnit").ifBlank { "kcal" },
                aiConsent = settingsMap.booleanValue("aiConsent") ?: true,
                photoStorageEnabled = settingsMap.booleanValue("photoStorageEnabled") ?: true
            )
        )
    }

    private fun Map<*, *>.stringValue(key: String): String = this[key] as? String ?: ""

    private fun Map<*, *>.intValue(key: String): Int? = when (val value = this[key]) {
        is Int -> value
        is Long -> value.toInt()
        is Double -> value.toInt()
        else -> null
    }

    private fun Map<*, *>.booleanValue(key: String): Boolean? = this[key] as? Boolean

    private fun Map<*, *>.stringListValue(key: String): List<String> =
        (this[key] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()

    private fun String.toSchemaValue(): String {
        return trim()
            .lowercase(Locale.US)
            .replace("&", "and")
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
    }

    private companion object {
        const val COLLECTION_USERS = "users"
        const val COLLECTION_REMINDERS = "reminders"
        const val COLLECTION_PLAN_INSTANCES = "plan_instances"
        const val COLLECTION_SCHEDULED_WORKOUTS = "scheduled_workouts"
        const val COLLECTION_NOTIFICATION_TOKENS = "notification_tokens"
        const val AUTH_PROVIDER_PASSWORD = "password"
        const val AUTH_PROVIDER_GUEST = "guest"
        const val PLAN_STATUS_ACTIVE = "active"
        const val PLAN_STATUS_DRAFT = "draft"
        const val STARTER_PLAN_ID = "starter_plan"
        val DAY_ORDER = listOf("sun", "mon", "tue", "wed", "thu", "fri", "sat")
        val DATE_KEY_FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    }
}
