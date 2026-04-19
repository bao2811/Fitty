package com.example.fitty.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class FittyDatabaseHelper(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT NOT NULL UNIQUE,
                email TEXT NOT NULL UNIQUE,
                password_hash TEXT,
                auth_provider TEXT NOT NULL DEFAULT 'password',
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE user_profiles (
                user_id INTEGER PRIMARY KEY,
                age INTEGER,
                gender TEXT,
                height_cm INTEGER,
                weight_kg INTEGER,
                target_weight_kg INTEGER,
                updated_at INTEGER NOT NULL,
                FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE onboarding_preferences (
                user_id INTEGER PRIMARY KEY,
                primary_goal TEXT NOT NULL,
                fitness_level TEXT NOT NULL,
                workout_duration_minutes INTEGER NOT NULL,
                preferred_time TEXT NOT NULL,
                equipment TEXT NOT NULL,
                injury_note TEXT,
                nutrition_style TEXT NOT NULL,
                updated_at INTEGER NOT NULL,
                FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE workout_availability (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL,
                day_of_week TEXT NOT NULL,
                preferred_time TEXT NOT NULL,
                duration_minutes INTEGER NOT NULL,
                UNIQUE(user_id, day_of_week, preferred_time),
                FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE dietary_restrictions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL,
                restriction_name TEXT NOT NULL,
                UNIQUE(user_id, restriction_name),
                FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE reminders (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL,
                reminder_type TEXT NOT NULL,
                UNIQUE(user_id, reminder_type),
                FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS reminders")
        db.execSQL("DROP TABLE IF EXISTS dietary_restrictions")
        db.execSQL("DROP TABLE IF EXISTS workout_availability")
        db.execSQL("DROP TABLE IF EXISTS onboarding_preferences")
        db.execSQL("DROP TABLE IF EXISTS user_profiles")
        db.execSQL("DROP TABLE IF EXISTS users")
        onCreate(db)
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    private companion object {
        const val DATABASE_NAME = "fitty.db"
        const val DATABASE_VERSION = 1
    }
}
