package com.example.fitty.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import java.security.MessageDigest
import java.util.Locale

data class LocalUser(
    val id: Long,
    val username: String,
    val email: String,
    val authProvider: String
)

data class AuthResult(
    val user: LocalUser? = null,
    val errorMessage: String? = null
)

data class OnboardingAnswers(
    val goal: String,
    val age: Int?,
    val heightCm: Int?,
    val weightKg: Int?,
    val targetWeightKg: Int?,
    val fitnessLevel: String,
    val workoutDays: Set<String>,
    val durationMinutes: Int,
    val preferredTime: String,
    val equipment: String,
    val injuryNote: String,
    val nutrition: String,
    val restrictions: Set<String>,
    val reminders: Set<String>
)

class FittyLocalRepository(context: Context) {
    private val databaseHelper = FittyDatabaseHelper(context.applicationContext)

    fun createPasswordUser(username: String, email: String, password: String): AuthResult {
        val normalizedUsername = username.trim()
        val normalizedEmail = email.trim().lowercase(Locale.US)
        return try {
            val now = System.currentTimeMillis()
            val values = ContentValues().apply {
                put("username", normalizedUsername)
                put("email", normalizedEmail)
                put("password_hash", password.sha256())
                put("auth_provider", PASSWORD_PROVIDER)
                put("created_at", now)
            }
            val userId = databaseHelper.writableDatabase.insertOrThrow("users", null, values)
            AuthResult(LocalUser(userId, normalizedUsername, normalizedEmail, PASSWORD_PROVIDER))
        } catch (_: SQLiteConstraintException) {
            AuthResult(errorMessage = "Username or email already exists")
        }
    }

    fun signInWithPassword(identifier: String, password: String): AuthResult {
        val normalizedIdentifier = identifier.trim().lowercase(Locale.US)
        val cursor = databaseHelper.readableDatabase.query(
            "users",
            arrayOf("id", "username", "email", "password_hash", "auth_provider"),
            "LOWER(username) = ? OR LOWER(email) = ?",
            arrayOf(normalizedIdentifier, normalizedIdentifier),
            null,
            null,
            null,
            "1"
        )

        cursor.use {
            if (!it.moveToFirst()) {
                return AuthResult(errorMessage = "Account was not found")
            }
            val storedHash = it.getString(it.getColumnIndexOrThrow("password_hash"))
            val authProvider = it.getString(it.getColumnIndexOrThrow("auth_provider"))
            if (authProvider != PASSWORD_PROVIDER || storedHash != password.sha256()) {
                return AuthResult(errorMessage = "Username/email or password is incorrect")
            }
            return AuthResult(it.toLocalUser())
        }
    }

    fun continueWithGoogleDemo(): AuthResult {
        val existingUser = findUserByEmail(GOOGLE_DEMO_EMAIL)
        if (existingUser != null) return AuthResult(existingUser)

        return try {
            val values = ContentValues().apply {
                put("username", GOOGLE_DEMO_USERNAME)
                put("email", GOOGLE_DEMO_EMAIL)
                putNull("password_hash")
                put("auth_provider", GOOGLE_PROVIDER)
                put("created_at", System.currentTimeMillis())
            }
            val userId = databaseHelper.writableDatabase.insertOrThrow("users", null, values)
            AuthResult(LocalUser(userId, GOOGLE_DEMO_USERNAME, GOOGLE_DEMO_EMAIL, GOOGLE_PROVIDER))
        } catch (_: SQLiteConstraintException) {
            AuthResult(errorMessage = "Google demo account could not be created")
        }
    }

    fun continueAsGuest(): LocalUser {
        val username = "guest_${System.currentTimeMillis()}"
        val email = "$username@fitty.local"
        val values = ContentValues().apply {
            put("username", username)
            put("email", email)
            putNull("password_hash")
            put("auth_provider", GUEST_PROVIDER)
            put("created_at", System.currentTimeMillis())
        }
        val userId = databaseHelper.writableDatabase.insertOrThrow("users", null, values)
        return LocalUser(userId, username, email, GUEST_PROVIDER)
    }

    fun saveOnboardingAnswers(userId: Long, answers: OnboardingAnswers) {
        val db = databaseHelper.writableDatabase
        db.beginTransaction()
        try {
            val now = System.currentTimeMillis()
            val profileValues = ContentValues().apply {
                put("user_id", userId)
                putNullableInt("age", answers.age)
                putNull("gender")
                putNullableInt("height_cm", answers.heightCm)
                putNullableInt("weight_kg", answers.weightKg)
                putNullableInt("target_weight_kg", answers.targetWeightKg)
                put("updated_at", now)
            }
            db.insertWithOnConflict(
                "user_profiles",
                null,
                profileValues,
                android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
            )

            val preferenceValues = ContentValues().apply {
                put("user_id", userId)
                put("primary_goal", answers.goal)
                put("fitness_level", answers.fitnessLevel)
                put("workout_duration_minutes", answers.durationMinutes)
                put("preferred_time", answers.preferredTime)
                put("equipment", answers.equipment)
                answers.injuryNote.takeIf { it.isNotBlank() }?.let {
                    put("injury_note", it)
                } ?: putNull("injury_note")
                put("nutrition_style", answers.nutrition)
                put("updated_at", now)
            }
            db.insertWithOnConflict(
                "onboarding_preferences",
                null,
                preferenceValues,
                android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
            )

            db.delete("workout_availability", "user_id = ?", arrayOf(userId.toString()))
            answers.workoutDays.forEach { day ->
                db.insert(
                    "workout_availability",
                    null,
                    ContentValues().apply {
                        put("user_id", userId)
                        put("day_of_week", day)
                        put("preferred_time", answers.preferredTime)
                        put("duration_minutes", answers.durationMinutes)
                    }
                )
            }

            replaceStringSet(db, "dietary_restrictions", "restriction_name", userId, answers.restrictions)
            replaceStringSet(db, "reminders", "reminder_type", userId, answers.reminders)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun replaceStringSet(
        db: android.database.sqlite.SQLiteDatabase,
        table: String,
        valueColumn: String,
        userId: Long,
        values: Set<String>
    ) {
        db.delete(table, "user_id = ?", arrayOf(userId.toString()))
        values.forEach { value ->
            db.insert(
                table,
                null,
                ContentValues().apply {
                    put("user_id", userId)
                    put(valueColumn, value)
                }
            )
        }
    }

    private fun findUserByEmail(email: String): LocalUser? {
        val cursor = databaseHelper.readableDatabase.query(
            "users",
            arrayOf("id", "username", "email", "auth_provider"),
            "email = ?",
            arrayOf(email),
            null,
            null,
            null,
            "1"
        )
        cursor.use {
            return if (it.moveToFirst()) it.toLocalUser() else null
        }
    }

    private fun android.database.Cursor.toLocalUser(): LocalUser =
        LocalUser(
            id = getLong(getColumnIndexOrThrow("id")),
            username = getString(getColumnIndexOrThrow("username")),
            email = getString(getColumnIndexOrThrow("email")),
            authProvider = getString(getColumnIndexOrThrow("auth_provider"))
        )

    private fun ContentValues.putNullableInt(key: String, value: Int?) {
        if (value == null) {
            putNull(key)
        } else {
            put(key, value)
        }
    }

    private fun String.sha256(): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(toByteArray())
        return digest.joinToString("") { byte -> "%02x".format(byte) }
    }

    private companion object {
        const val PASSWORD_PROVIDER = "password"
        const val GOOGLE_PROVIDER = "google"
        const val GUEST_PROVIDER = "guest"
        const val GOOGLE_DEMO_USERNAME = "google_user"
        const val GOOGLE_DEMO_EMAIL = "google_user@fitty.local"
    }
}
