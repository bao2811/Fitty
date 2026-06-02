package com.example.fitty.data.firebase

import com.example.fitty.data.content.StarterPlanBuilder
import com.example.fitty.domain.model.FittyAuthResult
import com.example.fitty.domain.model.FittyOnboarding
import com.example.fitty.domain.model.FittyOnboardingAnswers
import com.example.fitty.domain.model.FittyProfile
import com.example.fitty.domain.model.FittySettings
import com.example.fitty.domain.model.FittyStartupState
import com.example.fitty.domain.model.FittyStats
import com.example.fitty.domain.model.FittyUser
import com.example.fitty.domain.model.preferredDisplayName
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseUserRemoteDataSource @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val starterPlanBuilder: StarterPlanBuilder,
    private val sessionRepository: com.example.fitty.domain.repository.SessionRepository
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
            val firebaseUser = authResult.user
                ?: return FittyAuthResult(errorMessage = "Account could not be created")
            firebaseUser.updateProfile(
                UserProfileChangeRequest.Builder()
                    .setDisplayName(normalizedUsername)
                    .build()
            ).await()
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
            FittyAuthResult(errorMessage = error.toCreateAccountMessage())
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
            FittyAuthResult(errorMessage = error.toSignInMessage())
        }
    }

    suspend fun signInWithGoogle(idToken: String): FittyAuthResult {
        val normalizedToken = idToken.trim()
        if (normalizedToken.isBlank()) {
            return FittyAuthResult(errorMessage = "Google sign-in token is missing")
        }

        return try {
            val credential = GoogleAuthProvider.getCredential(normalizedToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user
                ?: return FittyAuthResult(errorMessage = "Could not sign in with Google")
            ensureUserDocument(firebaseUser, authProvider = AUTH_PROVIDER_GOOGLE)
            FittyAuthResult(user = getCurrentUser())
        } catch (error: Exception) {
            FittyAuthResult(errorMessage = error.message ?: "Google sign-in failed")
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
        val calorieTarget = computeCalorieTarget(answers.weightKg, answers.goal.toSchemaValue())
        val waterGoalMl = computeWaterGoalMl(answers.weightKg)

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
            "settings" to mapOf(
                "calorieTarget" to calorieTarget,
                "waterGoalMl" to waterGoalMl,
                "mealTargetPerDay" to DEFAULT_MEAL_TARGET_PER_DAY
            ),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        userDocument(uid).set(userPayload, SetOptions.merge()).await()

        saveReminders(uid = uid, reminders = answers.reminders)
        saveStarterPlan(uid = uid, answers = answers)
    }

    suspend fun markOnboardingCompleted(uid: String) {
        val payload = mapOf(
            "onboardingCompleted" to true,
            "updatedAt" to FieldValue.serverTimestamp(),
            "stats" to mapOf("activePlanId" to STARTER_PLAN_ID)
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
            displayName = user?.preferredDisplayName().orEmpty(),
            isGuest = user?.guest ?: firebaseUser.isAnonymous,
            isSignedIn = !(user?.guest ?: firebaseUser.isAnonymous),
            onboardingCompleted = user?.onboardingCompleted ?: false
        )
    }

    suspend fun getCurrentUser(uid: String? = auth.currentUser?.uid): FittyUser? {
        val resolvedUid = uid ?: return null
        val firebaseUser = auth.currentUser?.takeIf { it.uid == resolvedUid }
        if (firebaseUser != null) {
            ensureUserDocument(firebaseUser)
        }
        val snapshot = userDocument(resolvedUid).get().await()
        return if (snapshot.exists()) snapshot.toFittyUser(firebaseUser) else null
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

    private suspend fun ensureUserDocument(
        user: FirebaseUser,
        authProvider: String? = null
    ) {
        val docRef = userDocument(user.uid)
        val snapshot = docRef.get().await()
        val emailLocalPart = user.email
            ?.substringBefore("@")
            ?.replace(Regex("[^A-Za-z0-9_]"), "_")
            ?.takeIf { it.isNotBlank() }
            ?: if (user.isAnonymous) "guest" else "fitty_user"
        val defaultUsername = if (user.isAnonymous) {
            "guest"
        } else {
            user.displayName
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: emailLocalPart
        }
        if (snapshot.exists()) {
            val resolvedUsername = snapshot.getString("username")
                .orEmpty()
                .takeUnless { it.isPlaceholderUserName() }
                .orEmpty()
                .ifBlank { defaultUsername }
            val resolvedUsernameKey = if (user.isAnonymous) "" else resolvedUsername.lowercase(Locale.US)
            docRef.set(
                mapOf(
                    "email" to (user.email ?: snapshot.getString("email").orEmpty()),
                    "displayName" to displayNameFor(
                        user,
                        snapshot.getString("displayName").orEmpty().ifBlank { resolvedUsername }
                    ),
                    "username" to resolvedUsername,
                    "usernameNormalized" to resolvedUsernameKey,
                    "photoUrl" to (user.photoUrl?.toString() ?: snapshot.getString("photoUrl")),
                    "guest" to user.isAnonymous,
                    "authProvider" to (authProvider ?: snapshot.getString("authProvider").orEmpty().ifBlank {
                        if (user.isAnonymous) AUTH_PROVIDER_GUEST else AUTH_PROVIDER_PASSWORD
                    }),
                    "lastLoginAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            ).await()
            return
        }

        docRef.set(
            buildBaseUserDocument(
                user = user,
                username = defaultUsername,
                usernameKey = if (user.isAnonymous) "" else defaultUsername.lowercase(Locale.US),
                authProvider = authProvider ?: if (user.isAnonymous) AUTH_PROVIDER_GUEST else AUTH_PROVIDER_PASSWORD,
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
        answers: FittyOnboardingAnswers
    ) {
        val language = sessionRepository.getAppLanguage().orEmpty().ifBlank { "en" }
        val buildResult = starterPlanBuilder.buildForAnswers(answers, language)
        val planRef = userDocument(uid)
            .collection(COLLECTION_PLAN_INSTANCES)
            .document(STARTER_PLAN_ID)
        planRef.set(
            mapOf(
                "sourceProgramId" to buildResult.plan.sourceProgramId,
                "name" to buildResult.plan.name,
                "goal" to buildResult.plan.goal,
                "durationWeeks" to buildResult.plan.durationWeeks,
                "workoutsPerWeek" to buildResult.plan.workoutsPerWeek,
                "equipment" to buildResult.plan.equipment,
                "trainingStyle" to buildResult.plan.trainingStyle,
                "status" to PLAN_STATUS_DRAFT,
                "explanation" to buildResult.plan.explanation,
                "currentWeek" to 1,
                "nextWorkoutDate" to buildResult.plan.nextWorkoutDate,
                "previewTitle" to buildResult.preview.title,
                "previewSubtitle" to buildResult.preview.subtitle,
                "previewDetails" to buildResult.preview.details.map { detail ->
                    mapOf(
                        "iconKey" to detail.iconKey,
                        "title" to detail.title,
                        "body" to detail.body
                    )
                },
                "previewExercises" to buildResult.preview.exercises.map { exercise ->
                    mapOf(
                        "exerciseId" to exercise.exerciseId,
                        "name" to exercise.name,
                        "sets" to exercise.sets,
                        "reps" to exercise.reps,
                        "durationSeconds" to exercise.durationSeconds,
                        "targetWeightKg" to exercise.targetWeightKg
                    )
                },
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            SetOptions.merge()
        ).await()

        val scheduledCollection = planRef.collection(COLLECTION_SCHEDULED_WORKOUTS)
        val existingDocs = scheduledCollection.get().await()
        existingDocs.documents.forEach { it.reference.delete() }

        buildResult.scheduledWorkouts.forEach { workout ->
            scheduledCollection.document(workout.id).set(
                mapOf(
                    "dateKey" to workout.dateKey,
                    "weekNumber" to workout.weekNumber,
                    "orderInWeek" to workout.orderInWeek,
                    "title" to workout.title,
                    "durationMinutes" to workout.durationMinutes,
                    "estimatedCalories" to workout.estimatedCalories,
                    "difficulty" to workout.difficulty,
                    "equipment" to workout.equipment,
                    "status" to workout.status,
                    "explanation" to workout.explanation,
                    "replacedFromWorkoutId" to null,
                    "exercises" to workout.exercises.map { exercise ->
                        mapOf(
                            "exerciseId" to exercise.exerciseId,
                            "name" to exercise.name,
                            "sets" to exercise.sets,
                            "reps" to exercise.reps,
                            "durationSeconds" to exercise.durationSeconds,
                            "targetWeightKg" to exercise.targetWeightKg
                        )
                    },
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
                "calorieTarget" to null,
                "waterGoalMl" to null,
                "mealTargetPerDay" to null,
                "aiConsent" to true,
                "photoStorageEnabled" to true
            ),
            "stats" to mapOf(
                "activePlanId" to "",
                "currentStreak" to 0,
                "bestStreak" to 0,
                "totalWorkouts" to 0,
                "mealsLogged" to 0,
                "achievementsUnlocked" to 0,
                "lastActiveDate" to "",
                "streakActiveDates" to emptyList<String>()
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

    private fun appZoneId(): ZoneId = ZoneId.systemDefault()

    private fun userDocument(uid: String) = firestore.collection(COLLECTION_USERS).document(uid)

    private fun DocumentSnapshot.toFittyUser(authUser: FirebaseUser? = null): FittyUser {
        val profileMap = get("profile") as? Map<*, *> ?: emptyMap<String, Any?>()
        val onboardingMap = get("onboarding") as? Map<*, *> ?: emptyMap<String, Any?>()
        val statsMap = get("stats") as? Map<*, *> ?: emptyMap<String, Any?>()
        val settingsMap = get("settings") as? Map<*, *> ?: emptyMap<String, Any?>()
        val resolvedEmail = getString("email").orEmpty().ifBlank { authUser?.email.orEmpty() }
        val resolvedDisplayName = getString("displayName").orEmpty().ifBlank { authUser?.displayName.orEmpty() }
        val resolvedUsername = getString("username")
            .orEmpty()
            .takeUnless { it.isPlaceholderUserName() }
            .orEmpty()
            .ifBlank { resolvedDisplayName }
            .ifBlank { resolvedEmail.substringBefore("@") }
        val resolvedPhotoUrl = getString("photoUrl") ?: authUser?.photoUrl?.toString()

        return FittyUser(
            uid = id,
            email = resolvedEmail,
            displayName = resolvedDisplayName,
            username = resolvedUsername,
            photoUrl = resolvedPhotoUrl,
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
                achievementsUnlocked = statsMap.intValue("achievementsUnlocked") ?: 0,
                lastActiveDate = statsMap.stringValue("lastActiveDate"),
                streakActiveDates = statsMap.stringListValue("streakActiveDates")
            ),
            settings = FittySettings(
                language = settingsMap.stringValue("language").ifBlank { "en" },
                themeMode = settingsMap.stringValue("themeMode").ifBlank { "system" },
                weightUnit = settingsMap.stringValue("weightUnit").ifBlank { "kg" },
                heightUnit = settingsMap.stringValue("heightUnit").ifBlank { "cm" },
                energyUnit = settingsMap.stringValue("energyUnit").ifBlank { "kcal" },
                calorieTarget = settingsMap.intValue("calorieTarget"),
                waterGoalMl = settingsMap.intValue("waterGoalMl"),
                mealTargetPerDay = settingsMap.intValue("mealTargetPerDay"),
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

    private fun String.isPlaceholderUserName(): Boolean {
        return trim().lowercase(Locale.US) in PLACEHOLDER_USER_NAMES
    }

    private fun computeCalorieTarget(weightKg: Int?, goal: String): Int? {
        val weight = weightKg ?: return null
        return when (goal) {
            "gain_muscle" -> weight * 34
            "lose_weight" -> weight * 28
            else -> weight * 30
        }
    }

    private fun computeWaterGoalMl(weightKg: Int?): Int? {
        val weight = weightKg ?: return null
        return weight * 35
    }

    private companion object {
        const val COLLECTION_USERS = "users"
        const val COLLECTION_REMINDERS = "reminders"
        const val COLLECTION_PLAN_INSTANCES = "plan_instances"
        const val COLLECTION_SCHEDULED_WORKOUTS = "scheduled_workouts"
        const val COLLECTION_NOTIFICATION_TOKENS = "notification_tokens"
        const val AUTH_PROVIDER_PASSWORD = "password"
        const val AUTH_PROVIDER_GOOGLE = "google"
        const val AUTH_PROVIDER_GUEST = "guest"
        const val PLAN_STATUS_ACTIVE = "active"
        const val PLAN_STATUS_DRAFT = "draft"
        const val STARTER_PLAN_ID = "starter_plan"
        const val DEFAULT_MEAL_TARGET_PER_DAY = 3
        val PLACEHOLDER_USER_NAMES = setOf("fitty_user", "fitty user", "fittyuser")
        val DAY_ORDER = listOf("sun", "mon", "tue", "wed", "thu", "fri", "sat")
        val DATE_KEY_FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    }
}

private fun Exception.toCreateAccountMessage(): String = when (this) {
    is FirebaseAuthUserCollisionException -> "Account already exists"
    is FirebaseAuthInvalidCredentialsException -> "Enter a valid email address"
    else -> message ?: "Could not create account"
}

private fun Exception.toSignInMessage(): String = when (this) {
    is FirebaseAuthInvalidUserException,
    is FirebaseAuthInvalidCredentialsException -> "Email or password is incorrect"
    else -> message ?: "Email or password is incorrect"
}
