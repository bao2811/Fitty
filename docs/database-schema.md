# Fitty Database Schema

Fitty uses a local SQLite database named `fitty.db`. The schema is designed around the current product features:

- Authentication and profile
- Onboarding and personalization
- Practice tab: Today, Programs, Exercise Library, custom plans
- Workout tracking
- Meal, body, water, and progress tracking
- Coach chat and AI insights
- Daily tasks, streaks, achievements, reminders, settings, privacy

For a production backend, the same structure can be moved to Firebase/Firestore, Supabase, PostgreSQL, or a REST API. In that case, keep the same entity boundaries but replace local ids with server ids.

## Entity Groups

### 1. Identity And Profile

#### `users`

Stores login identity.

| Column | Type | Notes |
| --- | --- | --- |
| `id` | INTEGER PRIMARY KEY | Local user id |
| `username` | TEXT UNIQUE NOT NULL | Username login |
| `email` | TEXT UNIQUE NOT NULL | Email login |
| `password_hash` | TEXT NULL | Password auth only |
| `auth_provider` | TEXT NOT NULL | `password`, `google`, `guest` |
| `provider_user_id` | TEXT NULL | Google/Firebase uid later |
| `display_name` | TEXT NULL | Name shown in UI |
| `avatar_uri` | TEXT NULL | Local/avatar image uri |
| `created_at` | INTEGER NOT NULL | Unix ms |
| `updated_at` | INTEGER NULL | Unix ms |

#### `user_profiles`

Stores health profile and goal information.

| Column | Type | Notes |
| --- | --- | --- |
| `user_id` | INTEGER PRIMARY KEY | FK `users.id` |
| `full_name` | TEXT NULL | Profile name |
| `age` | INTEGER NULL | Age |
| `gender` | TEXT NULL | Gender |
| `height_cm` | INTEGER NULL | Height |
| `weight_kg` | INTEGER NULL | Current weight |
| `target_weight_kg` | INTEGER NULL | Target weight |
| `activity_level` | TEXT NULL | Sedentary, active, etc. |
| `fitness_level` | TEXT NULL | Beginner/intermediate/advanced |
| `primary_goal` | TEXT NULL | Fat loss, muscle gain, etc. |
| `calorie_target` | INTEGER NULL | Daily kcal target |
| `water_goal_ml` | INTEGER NULL | Daily water target |
| `updated_at` | INTEGER NOT NULL | Unix ms |

#### `user_settings`

Stores app-level preferences and privacy options.

| Column | Type | Notes |
| --- | --- | --- |
| `user_id` | INTEGER PRIMARY KEY | FK `users.id` |
| `unit_weight` | TEXT | `kg` by default |
| `unit_height` | TEXT | `cm` by default |
| `unit_energy` | TEXT | `kcal` by default |
| `language` | TEXT | App language |
| `dark_mode` | TEXT | `system`, `light`, `dark` |
| `ai_data_enabled` | INTEGER | 1/0 |
| `photo_storage_enabled` | INTEGER | 1/0 |
| `updated_at` | INTEGER | Unix ms |

#### `health_connections`

Stores linked health/device integrations.

| Column | Type | Notes |
| --- | --- | --- |
| `id` | INTEGER PRIMARY KEY | Row id |
| `user_id` | INTEGER NOT NULL | FK `users.id` |
| `provider` | TEXT NOT NULL | Health Connect, smartwatch, etc. |
| `connection_state` | TEXT NOT NULL | Connected/not connected |
| `last_sync_at` | INTEGER NULL | Unix ms |

### 2. Onboarding And Personalization

#### `onboarding_preferences`

Stores one-row summary of onboarding answers for plan generation.

| Column | Type | Notes |
| --- | --- | --- |
| `user_id` | INTEGER PRIMARY KEY | FK `users.id` |
| `primary_goal` | TEXT NOT NULL | Main goal |
| `fitness_level` | TEXT NOT NULL | Level |
| `workout_duration_minutes` | INTEGER NOT NULL | Preferred session duration |
| `preferred_time` | TEXT NOT NULL | Morning/afternoon/evening |
| `equipment` | TEXT NOT NULL | Home/gym/no equipment |
| `injury_note` | TEXT NULL | Optional note |
| `nutrition_style` | TEXT NOT NULL | Diet preference |
| `updated_at` | INTEGER NOT NULL | Unix ms |

#### `workout_availability`

Stores separated answers for “When can you work out?”.

| Column | Type | Notes |
| --- | --- | --- |
| `id` | INTEGER PRIMARY KEY | Row id |
| `user_id` | INTEGER NOT NULL | FK `users.id` |
| `day_of_week` | TEXT NOT NULL | Mon-Sun |
| `preferred_time` | TEXT NOT NULL | Morning/afternoon/evening |
| `duration_minutes` | INTEGER NOT NULL | Session duration |

#### `dietary_restrictions`

Stores multiple dietary restrictions per user.

| Column | Type | Notes |
| --- | --- | --- |
| `id` | INTEGER PRIMARY KEY | Row id |
| `user_id` | INTEGER NOT NULL | FK `users.id` |
| `restriction_name` | TEXT NOT NULL | Lactose-free, nut allergy, etc. |

#### `reminders`

Stores reminder settings shown in Profile.

| Column | Type | Notes |
| --- | --- | --- |
| `id` | INTEGER PRIMARY KEY | Row id |
| `user_id` | INTEGER NOT NULL | FK `users.id` |
| `reminder_type` | TEXT NOT NULL | Workout, meal, water, sleep, streak |
| `schedule_text` | TEXT NULL | Human-readable schedule |
| `enabled` | INTEGER | 1/0 |
| `time_minutes` | INTEGER NULL | Minutes after midnight |
| `repeat_rule` | TEXT NULL | Daily, every 2 hours, etc. |

### 3. Exercise Library

#### `exercises`

Stores reusable exercise definitions.

| Column | Type | Notes |
| --- | --- | --- |
| `id` | INTEGER PRIMARY KEY | Exercise id |
| `name` | TEXT UNIQUE NOT NULL | Bodyweight Squat, Plank |
| `description` | TEXT NULL | Short summary |
| `difficulty` | TEXT NOT NULL | Beginner/intermediate/advanced |
| `primary_muscle_group` | TEXT NOT NULL | Legs, core, chest |
| `equipment` | TEXT NOT NULL | No equipment, dumbbells, gym |
| `default_reps` | TEXT NULL | `15 reps`, `3 x 12` |
| `default_duration_seconds` | INTEGER NULL | Timed exercises |
| `media_uri` | TEXT NULL | Image/video/gif |
| `created_at` | INTEGER NOT NULL | Unix ms |

Supporting exercise detail tables:

| Table | Purpose |
| --- | --- |
| `exercise_steps` | Ordered “How to do it” steps |
| `exercise_mistakes` | Common mistakes |
| `exercise_tips` | Trainer tips |
| `exercise_variations` | Easier/harder versions |
| `exercise_target_muscles` | Quadriceps, glutes, core, etc. |
| `favorite_exercises` | Saved exercises per user |

### 4. Practice Programs

#### `programs`

Stores ready-made guided programs.

| Column | Type | Notes |
| --- | --- | --- |
| `id` | INTEGER PRIMARY KEY | Program id |
| `title` | TEXT NOT NULL | Beginner Fat Loss Starter |
| `goal` | TEXT NOT NULL | Fat loss, strength, mobility |
| `difficulty` | TEXT NOT NULL | Beginner/intermediate/advanced |
| `weeks` | INTEGER NOT NULL | Program length |
| `workouts_per_week` | INTEGER NOT NULL | Frequency |
| `average_duration_minutes` | INTEGER NOT NULL | Average session length |
| `equipment` | TEXT NOT NULL | Home, gym, no equipment |
| `description` | TEXT NULL | Program summary |
| `thumbnail_uri` | TEXT NULL | Program image |
| `is_template` | INTEGER | 1 for ready-made template |
| `created_at` | INTEGER NOT NULL | Unix ms |

Program structure tables:

| Table | Purpose |
| --- | --- |
| `program_weeks` | Week 1, Week 2, etc. |
| `program_sessions` | Day 1 Full Body Basics, Day 2 Recovery Stretch |
| `session_exercises` | Ordered exercises inside a program session |

### 5. Custom Plans And Custom Workouts

#### `user_plans`

Stores plans created by the user or copied from a template.

| Column | Type | Notes |
| --- | --- | --- |
| `id` | INTEGER PRIMARY KEY | Plan id |
| `user_id` | INTEGER NOT NULL | FK `users.id` |
| `source_program_id` | INTEGER NULL | FK `programs.id` if copied from template |
| `name` | TEXT NOT NULL | My Home Fat Loss Plan |
| `goal` | TEXT NOT NULL | Goal |
| `duration_weeks` | INTEGER NOT NULL | 1, 2, 4, 8 |
| `workouts_per_week` | INTEGER NOT NULL | 2-6 |
| `equipment` | TEXT NULL | Selected equipment |
| `training_style` | TEXT NULL | Full body, PPL, HIIT, etc. |
| `status` | TEXT | Draft, active, completed, template |
| `started_at` | INTEGER NULL | Unix ms |
| `completed_at` | INTEGER NULL | Unix ms |
| `updated_at` | INTEGER NOT NULL | Unix ms |

Custom plan/workout tables:

| Table | Purpose |
| --- | --- |
| `user_plan_days` | Monday-Sunday workout schedule |
| `user_workouts` | A custom workout session |
| `user_workout_exercises` | Ordered exercises inside custom workout |

### 6. Tracking And Progress

Workout tracking:

| Table | Purpose |
| --- | --- |
| `workout_logs` | Started/completed workout sessions |
| `exercise_logs` | Actual sets/reps/duration completed |

Nutrition tracking:

| Table | Purpose |
| --- | --- |
| `meals` | Meal scan/manual meal log with kcal/macros |
| `meal_items` | Foods detected or entered inside a meal |

Body and hydration tracking:

| Table | Purpose |
| --- | --- |
| `body_measurements` | Weight, body fat, waist, chest, hip over time |
| `body_scan_records` | AI body scan photos and summary |
| `water_logs` | Water intake events |

### 7. Daily App Experience

#### `daily_tasks`

Stores Home checklist tasks.

| Column | Type | Notes |
| --- | --- | --- |
| `id` | INTEGER PRIMARY KEY | Task id |
| `user_id` | INTEGER NOT NULL | FK `users.id` |
| `task_type` | TEXT NOT NULL | Workout, meal, water, stretch, coach |
| `title` | TEXT NOT NULL | Task title |
| `description` | TEXT NULL | Short detail |
| `due_at` | INTEGER NULL | Unix ms |
| `status` | TEXT NOT NULL | Todo, in_progress, done, snoozed |
| `related_entity_type` | TEXT NULL | workout, meal, program |
| `related_entity_id` | INTEGER NULL | Related row id |
| `created_at` | INTEGER NOT NULL | Unix ms |
| `completed_at` | INTEGER NULL | Unix ms |

#### `streaks`

Stores current and best streak for fast Home/Profile display.

| Column | Type | Notes |
| --- | --- | --- |
| `user_id` | INTEGER PRIMARY KEY | FK `users.id` |
| `current_days` | INTEGER | Current streak |
| `best_days` | INTEGER | Best streak |
| `last_action_date` | TEXT NULL | YYYY-MM-DD |
| `updated_at` | INTEGER | Unix ms |

#### `achievements` and `user_achievements`

`achievements` stores badge definitions. `user_achievements` stores unlocked badges.

### 8. AI Coach

| Table | Purpose |
| --- | --- |
| `ai_messages` | User/assistant chat history |
| `ai_insights` | Home AI insight cards, status, dismiss/apply state |

## Important Relationships

- `users` 1-1 `user_profiles`
- `users` 1-1 `user_settings`
- `users` 1-many `workout_availability`
- `users` 1-many `reminders`
- `programs` 1-many `program_weeks`
- `program_weeks` 1-many `program_sessions`
- `program_sessions` many-many `exercises` through `session_exercises`
- `users` 1-many `user_plans`
- `user_plans` 1-many `user_plan_days`
- `user_workouts` many-many `exercises` through `user_workout_exercises`
- `users` 1-many `workout_logs`, `meals`, `body_measurements`, `daily_tasks`, `ai_messages`
- `users` many-many `achievements` through `user_achievements`

## Why This Schema Fits Fitty

- New users can start from `programs`.
- Experienced users can build their own `user_plans` and `user_workouts`.
- Exercise guidance is normalized through `exercises`, `exercise_steps`, `exercise_mistakes`, `exercise_tips`, and `exercise_variations`.
- Home dashboard can load fast from `daily_tasks`, `streaks`, `workout_logs`, `meals`, and `ai_insights`.
- Profile can read goal, metrics, preferences, reminders, achievements, privacy, and linked devices from separate focused tables.
- Tracking is separated by domain, so workout, meal, body scan, water, and AI data do not become one overloaded table.

## Notes For Production

- Do not use plain SHA-256 for real password storage. Use Firebase Auth, Google Identity, or a backend with salted password hashing such as Argon2/bcrypt.
- Keep `provider_user_id` when using Google/Firebase.
- Replace local photo paths with cloud storage URLs if syncing across devices.
- Add migration scripts instead of dropping tables once real user data exists.
