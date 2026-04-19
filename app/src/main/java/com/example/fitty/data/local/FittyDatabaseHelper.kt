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
        createIdentityTables(db)
        createPreferenceTables(db)
        createPracticeTables(db)
        createTrackingTables(db)
        createEngagementTables(db)
        createIndexes(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        dropAllTables(db)
        onCreate(db)
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    private fun createIdentityTables(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT NOT NULL UNIQUE,
                email TEXT NOT NULL UNIQUE,
                password_hash TEXT,
                auth_provider TEXT NOT NULL DEFAULT 'password',
                provider_user_id TEXT,
                display_name TEXT,
                avatar_uri TEXT,
                created_at INTEGER NOT NULL,
                updated_at INTEGER
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE user_profiles (
                user_id INTEGER PRIMARY KEY,
                full_name TEXT,
                age INTEGER,
                gender TEXT,
                height_cm INTEGER,
                weight_kg INTEGER,
                target_weight_kg INTEGER,
                activity_level TEXT,
                fitness_level TEXT,
                primary_goal TEXT,
                calorie_target INTEGER,
                water_goal_ml INTEGER,
                updated_at INTEGER NOT NULL,
                FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE user_settings (
                user_id INTEGER PRIMARY KEY,
                unit_weight TEXT NOT NULL DEFAULT 'kg',
                unit_height TEXT NOT NULL DEFAULT 'cm',
                unit_energy TEXT NOT NULL DEFAULT 'kcal',
                language TEXT NOT NULL DEFAULT 'en',
                dark_mode TEXT NOT NULL DEFAULT 'system',
                ai_data_enabled INTEGER NOT NULL DEFAULT 1,
                photo_storage_enabled INTEGER NOT NULL DEFAULT 0,
                updated_at INTEGER NOT NULL,
                FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE health_connections (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL,
                provider TEXT NOT NULL,
                connection_state TEXT NOT NULL,
                last_sync_at INTEGER,
                UNIQUE(user_id, provider),
                FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
    }

    private fun createPreferenceTables(db: SQLiteDatabase) {
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
                schedule_text TEXT,
                enabled INTEGER NOT NULL DEFAULT 1,
                time_minutes INTEGER,
                repeat_rule TEXT,
                UNIQUE(user_id, reminder_type),
                FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
    }

    private fun createPracticeTables(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE exercises (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL UNIQUE,
                description TEXT,
                difficulty TEXT NOT NULL,
                primary_muscle_group TEXT NOT NULL,
                equipment TEXT NOT NULL,
                default_reps TEXT,
                default_duration_seconds INTEGER,
                media_uri TEXT,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE exercise_steps (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                exercise_id INTEGER NOT NULL,
                step_order INTEGER NOT NULL,
                instruction TEXT NOT NULL,
                UNIQUE(exercise_id, step_order),
                FOREIGN KEY(exercise_id) REFERENCES exercises(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE exercise_mistakes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                exercise_id INTEGER NOT NULL,
                mistake TEXT NOT NULL,
                FOREIGN KEY(exercise_id) REFERENCES exercises(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE exercise_tips (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                exercise_id INTEGER NOT NULL,
                tip TEXT NOT NULL,
                FOREIGN KEY(exercise_id) REFERENCES exercises(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE exercise_variations (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                exercise_id INTEGER NOT NULL,
                variation_type TEXT NOT NULL,
                name TEXT NOT NULL,
                notes TEXT,
                FOREIGN KEY(exercise_id) REFERENCES exercises(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE exercise_target_muscles (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                exercise_id INTEGER NOT NULL,
                muscle_name TEXT NOT NULL,
                UNIQUE(exercise_id, muscle_name),
                FOREIGN KEY(exercise_id) REFERENCES exercises(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE favorite_exercises (
                user_id INTEGER NOT NULL,
                exercise_id INTEGER NOT NULL,
                created_at INTEGER NOT NULL,
                PRIMARY KEY(user_id, exercise_id),
                FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE,
                FOREIGN KEY(exercise_id) REFERENCES exercises(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE programs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                goal TEXT NOT NULL,
                difficulty TEXT NOT NULL,
                weeks INTEGER NOT NULL,
                workouts_per_week INTEGER NOT NULL,
                average_duration_minutes INTEGER NOT NULL,
                equipment TEXT NOT NULL,
                description TEXT,
                thumbnail_uri TEXT,
                is_template INTEGER NOT NULL DEFAULT 1,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE program_weeks (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                program_id INTEGER NOT NULL,
                week_number INTEGER NOT NULL,
                title TEXT,
                UNIQUE(program_id, week_number),
                FOREIGN KEY(program_id) REFERENCES programs(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE program_sessions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                program_week_id INTEGER NOT NULL,
                day_number INTEGER NOT NULL,
                title TEXT NOT NULL,
                focus_area TEXT NOT NULL,
                difficulty TEXT NOT NULL,
                duration_minutes INTEGER NOT NULL,
                estimated_calories INTEGER,
                notes TEXT,
                UNIQUE(program_week_id, day_number),
                FOREIGN KEY(program_week_id) REFERENCES program_weeks(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE session_exercises (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                session_id INTEGER NOT NULL,
                exercise_id INTEGER NOT NULL,
                exercise_order INTEGER NOT NULL,
                sets INTEGER,
                reps TEXT,
                duration_seconds INTEGER,
                rest_seconds INTEGER,
                notes TEXT,
                UNIQUE(session_id, exercise_order),
                FOREIGN KEY(session_id) REFERENCES program_sessions(id) ON DELETE CASCADE,
                FOREIGN KEY(exercise_id) REFERENCES exercises(id) ON DELETE RESTRICT
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE user_plans (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL,
                source_program_id INTEGER,
                name TEXT NOT NULL,
                goal TEXT NOT NULL,
                duration_weeks INTEGER NOT NULL,
                workouts_per_week INTEGER NOT NULL,
                equipment TEXT,
                training_style TEXT,
                status TEXT NOT NULL DEFAULT 'draft',
                started_at INTEGER,
                completed_at INTEGER,
                updated_at INTEGER NOT NULL,
                FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE,
                FOREIGN KEY(source_program_id) REFERENCES programs(id) ON DELETE SET NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE user_plan_days (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_plan_id INTEGER NOT NULL,
                day_of_week TEXT NOT NULL,
                user_workout_id INTEGER,
                is_rest_day INTEGER NOT NULL DEFAULT 0,
                UNIQUE(user_plan_id, day_of_week),
                FOREIGN KEY(user_plan_id) REFERENCES user_plans(id) ON DELETE CASCADE,
                FOREIGN KEY(user_workout_id) REFERENCES user_workouts(id) ON DELETE SET NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE user_workouts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL,
                name TEXT NOT NULL,
                focus_area TEXT,
                difficulty TEXT,
                estimated_duration_minutes INTEGER,
                structure_type TEXT,
                notes TEXT,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE user_workout_exercises (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_workout_id INTEGER NOT NULL,
                exercise_id INTEGER NOT NULL,
                exercise_order INTEGER NOT NULL,
                sets INTEGER,
                reps TEXT,
                duration_seconds INTEGER,
                rest_seconds INTEGER,
                notes TEXT,
                UNIQUE(user_workout_id, exercise_order),
                FOREIGN KEY(user_workout_id) REFERENCES user_workouts(id) ON DELETE CASCADE,
                FOREIGN KEY(exercise_id) REFERENCES exercises(id) ON DELETE RESTRICT
            )
            """.trimIndent()
        )
    }

    private fun createTrackingTables(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE workout_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL,
                user_workout_id INTEGER,
                program_session_id INTEGER,
                started_at INTEGER NOT NULL,
                completed_at INTEGER,
                duration_minutes INTEGER,
                calories_burned INTEGER,
                status TEXT NOT NULL DEFAULT 'in_progress',
                FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE,
                FOREIGN KEY(user_workout_id) REFERENCES user_workouts(id) ON DELETE SET NULL,
                FOREIGN KEY(program_session_id) REFERENCES program_sessions(id) ON DELETE SET NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE exercise_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                workout_log_id INTEGER NOT NULL,
                exercise_id INTEGER NOT NULL,
                sets_completed INTEGER,
                reps_completed TEXT,
                duration_seconds INTEGER,
                notes TEXT,
                FOREIGN KEY(workout_log_id) REFERENCES workout_logs(id) ON DELETE CASCADE,
                FOREIGN KEY(exercise_id) REFERENCES exercises(id) ON DELETE RESTRICT
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE meals (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL,
                meal_type TEXT NOT NULL,
                logged_at INTEGER NOT NULL,
                photo_uri TEXT,
                total_calories INTEGER,
                protein_g REAL,
                carbs_g REAL,
                fat_g REAL,
                ai_confidence REAL,
                notes TEXT,
                FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE meal_items (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                meal_id INTEGER NOT NULL,
                name TEXT NOT NULL,
                calories INTEGER,
                protein_g REAL,
                carbs_g REAL,
                fat_g REAL,
                serving_text TEXT,
                FOREIGN KEY(meal_id) REFERENCES meals(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE body_measurements (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL,
                measured_at INTEGER NOT NULL,
                weight_kg REAL,
                body_fat_percent REAL,
                waist_cm REAL,
                chest_cm REAL,
                hip_cm REAL,
                notes TEXT,
                FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE body_scan_records (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL,
                scanned_at INTEGER NOT NULL,
                front_photo_uri TEXT,
                side_photo_uri TEXT,
                back_photo_uri TEXT,
                ai_summary TEXT,
                privacy_mode TEXT NOT NULL DEFAULT 'private',
                FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE water_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL,
                logged_at INTEGER NOT NULL,
                amount_ml INTEGER NOT NULL,
                FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
    }

    private fun createEngagementTables(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE daily_tasks (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL,
                task_type TEXT NOT NULL,
                title TEXT NOT NULL,
                description TEXT,
                due_at INTEGER,
                status TEXT NOT NULL DEFAULT 'todo',
                related_entity_type TEXT,
                related_entity_id INTEGER,
                created_at INTEGER NOT NULL,
                completed_at INTEGER,
                FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE achievements (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                code TEXT NOT NULL UNIQUE,
                title TEXT NOT NULL,
                description TEXT,
                icon_name TEXT,
                requirement_text TEXT
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE user_achievements (
                user_id INTEGER NOT NULL,
                achievement_id INTEGER NOT NULL,
                unlocked_at INTEGER NOT NULL,
                PRIMARY KEY(user_id, achievement_id),
                FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE,
                FOREIGN KEY(achievement_id) REFERENCES achievements(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE streaks (
                user_id INTEGER PRIMARY KEY,
                current_days INTEGER NOT NULL DEFAULT 0,
                best_days INTEGER NOT NULL DEFAULT 0,
                last_action_date TEXT,
                updated_at INTEGER NOT NULL,
                FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE ai_messages (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL,
                role TEXT NOT NULL,
                message TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                related_feature TEXT,
                FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE ai_insights (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL,
                insight_type TEXT NOT NULL,
                title TEXT NOT NULL,
                body TEXT NOT NULL,
                action_label TEXT,
                status TEXT NOT NULL DEFAULT 'new',
                created_at INTEGER NOT NULL,
                dismissed_at INTEGER,
                FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
    }

    private fun createIndexes(db: SQLiteDatabase) {
        listOf(
            "CREATE INDEX idx_workout_availability_user ON workout_availability(user_id)",
            "CREATE INDEX idx_exercises_filters ON exercises(primary_muscle_group, difficulty, equipment)",
            "CREATE INDEX idx_programs_filters ON programs(goal, difficulty, equipment)",
            "CREATE INDEX idx_user_plans_user_status ON user_plans(user_id, status)",
            "CREATE INDEX idx_user_workouts_user ON user_workouts(user_id)",
            "CREATE INDEX idx_workout_logs_user_date ON workout_logs(user_id, started_at)",
            "CREATE INDEX idx_meals_user_date ON meals(user_id, logged_at)",
            "CREATE INDEX idx_body_measurements_user_date ON body_measurements(user_id, measured_at)",
            "CREATE INDEX idx_daily_tasks_user_status ON daily_tasks(user_id, status, due_at)",
            "CREATE INDEX idx_ai_messages_user_date ON ai_messages(user_id, created_at)",
            "CREATE INDEX idx_ai_insights_user_status ON ai_insights(user_id, status)"
        ).forEach(db::execSQL)
    }

    private fun dropAllTables(db: SQLiteDatabase) {
        listOf(
            "ai_insights",
            "ai_messages",
            "streaks",
            "user_achievements",
            "achievements",
            "daily_tasks",
            "water_logs",
            "body_scan_records",
            "body_measurements",
            "meal_items",
            "meals",
            "exercise_logs",
            "workout_logs",
            "user_workout_exercises",
            "user_plan_days",
            "user_workouts",
            "user_plans",
            "session_exercises",
            "program_sessions",
            "program_weeks",
            "programs",
            "favorite_exercises",
            "exercise_target_muscles",
            "exercise_variations",
            "exercise_tips",
            "exercise_mistakes",
            "exercise_steps",
            "exercises",
            "reminders",
            "dietary_restrictions",
            "workout_availability",
            "onboarding_preferences",
            "health_connections",
            "user_settings",
            "user_profiles",
            "users"
        ).forEach { tableName ->
            db.execSQL("DROP TABLE IF EXISTS $tableName")
        }
    }

    private companion object {
        const val DATABASE_NAME = "fitty.db"
        const val DATABASE_VERSION = 2
    }
}
