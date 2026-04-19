# Fitty Database Schema

Fitty currently uses a local SQLite database named `fitty.db` for demo auth and onboarding storage.

## Tables

### `users`

Stores account identity.

| Column | Type | Notes |
| --- | --- | --- |
| `id` | INTEGER PRIMARY KEY | Local user id |
| `username` | TEXT UNIQUE NOT NULL | Login username |
| `email` | TEXT UNIQUE NOT NULL | Login email |
| `password_hash` | TEXT NULL | SHA-256 hash for password accounts; null for Google/guest demo accounts |
| `auth_provider` | TEXT NOT NULL | `password`, `google`, or `guest` |
| `created_at` | INTEGER NOT NULL | Unix time in milliseconds |

### `user_profiles`

Stores body metrics collected during onboarding.

| Column | Type | Notes |
| --- | --- | --- |
| `user_id` | INTEGER PRIMARY KEY | References `users.id` |
| `age` | INTEGER NULL | User age |
| `gender` | TEXT NULL | Reserved for future profile step |
| `height_cm` | INTEGER NULL | Height in centimeters |
| `weight_kg` | INTEGER NULL | Current weight |
| `target_weight_kg` | INTEGER NULL | Target weight |
| `updated_at` | INTEGER NOT NULL | Unix time in milliseconds |

### `onboarding_preferences`

Stores one-row summary preferences for plan generation.

| Column | Type | Notes |
| --- | --- | --- |
| `user_id` | INTEGER PRIMARY KEY | References `users.id` |
| `primary_goal` | TEXT NOT NULL | Main fitness goal |
| `fitness_level` | TEXT NOT NULL | Beginner, intermediate, advanced |
| `workout_duration_minutes` | INTEGER NOT NULL | Preferred session length |
| `preferred_time` | TEXT NOT NULL | Morning, afternoon, evening |
| `equipment` | TEXT NOT NULL | Training location/equipment |
| `injury_note` | TEXT NULL | Optional limitation note |
| `nutrition_style` | TEXT NOT NULL | Eating style |
| `updated_at` | INTEGER NOT NULL | Unix time in milliseconds |

### `workout_availability`

Stores the separated answer for "When can you work out?"

| Column | Type | Notes |
| --- | --- | --- |
| `id` | INTEGER PRIMARY KEY | Availability row id |
| `user_id` | INTEGER NOT NULL | References `users.id` |
| `day_of_week` | TEXT NOT NULL | Mon, Tue, Wed, Thu, Fri, Sat, Sun |
| `preferred_time` | TEXT NOT NULL | Morning, afternoon, evening |
| `duration_minutes` | INTEGER NOT NULL | Session duration |

### `dietary_restrictions`

Stores optional nutrition restrictions.

| Column | Type | Notes |
| --- | --- | --- |
| `id` | INTEGER PRIMARY KEY | Restriction row id |
| `user_id` | INTEGER NOT NULL | References `users.id` |
| `restriction_name` | TEXT NOT NULL | Restriction label |

### `reminders`

Stores selected reminder categories.

| Column | Type | Notes |
| --- | --- | --- |
| `id` | INTEGER PRIMARY KEY | Reminder row id |
| `user_id` | INTEGER NOT NULL | References `users.id` |
| `reminder_type` | TEXT NOT NULL | Workout, meal, water, sleep |

## Icon Mapping

| Area | Icon |
| --- | --- |
| App identity | `SelfImprovement` |
| Workout plan | `FitnessCenter` |
| Nutrition | `Restaurant` |
| Progress | `Insights` |
| Email/username | `AlternateEmail` |
| Email | `Email` |
| Password | `Lock` |
| Workout day | `CalendarMonth` |
| Workout time | `Schedule` |
| Session duration | `Timer` |
| Bottom navigation home | `Home` |
| Bottom navigation plan | `CalendarMonth` |
| Bottom navigation track | `Restaurant` |
| Bottom navigation coach | `Chat` |
| Bottom navigation profile | `Person` |

For production Google sign-in, replace the local demo provider with Firebase Authentication or Google Identity Services, then use `users.auth_provider = 'google'` with the provider uid stored in a new `provider_user_id` column.
