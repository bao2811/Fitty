# Fitty Firestore Schema

This document defines a practical Firestore design for Fitty after moving auth to Firebase Authentication and app data to Cloud Firestore.

Primary key rule:

- Use Firebase Auth `uid` as the canonical user id.
- Use `serverTimestamp()` for `createdAt` and `updatedAt`.
- Keep shared catalog data in top-level collections.
- Keep user-owned data under `users/{uid}` subcollections.
- Denormalize small summaries for Home, Plan, and Progress to reduce read count.

## 1. Top-Level Collections

```text
users/{uid}
exercise_library/{exerciseId}
program_templates/{programId}
achievement_definitions/{achievementId}
food_catalog/{foodId}
```

Use top-level collections only for shared read-mostly data. Everything private to one user should live under that user's document.

## 2. User Tree

```text
users/{uid}
  reminders/{reminderId}
  health_connections/{provider}
  plan_instances/{planId}
    scheduled_workouts/{scheduledWorkoutId}
  workout_sessions/{sessionId}
    exercise_logs/{exerciseLogId}
  meal_logs/{mealId}
  body_measurements/{measurementId}
  body_scans/{scanId}
  water_logs/{waterLogId}
  daily_check_ins/{dateKey}
  daily_summaries/{dateKey}
  achievements/{achievementId}
  habits/{dateKey}
  coach_threads/{threadId}
    messages/{messageId}
```

Recommended `dateKey` format: `YYYY-MM-DD`, for example `2026-04-23`.

## 3. Core User Document

Document path: `users/{uid}`

Suggested shape:

```js
{
  email: "alex@example.com",
  displayName: "Alex",
  photoUrl: null,
  authProvider: "password", // password | google | guest
  guest: false,
  onboardingCompleted: true,
  profile: {
    age: 27,
    gender: "male",
    heightCm: 175,
    weightKg: 78.4,
    targetWeightKg: 72,
    bodyFatPercent: null,
    activityLevel: "moderate",
    fitnessLevel: "beginner",
    primaryGoal: "fat_loss",
    injuryNote: null
  },
  onboarding: {
    workoutDays: ["mon", "wed", "fri"],
    workoutDurationMinutes: 45,
    preferredTime: "morning",
    equipmentAccess: "home_basic",
    nutritionStyle: "high_protein",
    dietaryRestrictions: ["lactose_free"]
  },
  settings: {
    language: "en",
    themeMode: "system",
    weightUnit: "kg",
    heightUnit: "cm",
    energyUnit: "kcal",
    aiConsent: true,
    photoStorageEnabled: true
  },
  stats: {
    activePlanId: "plan_2026_04_a",
    currentStreak: 4,
    bestStreak: 11,
    totalWorkouts: 23,
    mealsLogged: 40,
    achievementsUnlocked: 5
  },
  createdAt: <timestamp>,
  updatedAt: <timestamp>,
  lastLoginAt: <timestamp>
}
```

Why keep `profile`, `onboarding`, `settings`, and `stats` inside one user document:

- Home and Profile screens can load in one read.
- These fields change relatively infrequently.
- This avoids extra joins that Firestore does not support.

## 4. Shared Catalog Collections

### `exercise_library/{exerciseId}`

Shared exercise metadata used by plans, workout detail, and library browse.

```js
{
  name: "Bodyweight Squat",
  description: "Foundational lower-body movement.",
  difficulty: "beginner",
  primaryMuscleGroup: "legs",
  targetMuscles: ["quads", "glutes", "core"],
  equipment: "none",
  defaultRepsText: "3 x 12",
  defaultDurationSeconds: null,
  mediaUrl: "https://media.giphy.com/media/1C1ipHPEs4Vjwglwza/giphy.gif",
  mediaType: "gif", // gif | image | video
  steps: ["Stand tall", "Lower hips", "Drive up"],
  mistakes: ["Knees collapse inward"],
  tips: ["Keep chest up"],
  variations: ["Box squat", "Jump squat"],
  createdAt: <timestamp>,
  updatedAt: <timestamp>
}
```

### `program_templates/{programId}`

Shared ready-made programs that users can preview and copy into their own plan instance.

```js
{
  title: "Beginner Fat Loss Starter",
  goal: "fat_loss",
  difficulty: "beginner",
  weeks: 4,
  workoutsPerWeek: 3,
  averageDurationMinutes: 40,
  equipment: "home_basic",
  description: "Starter plan for new users.",
  thumbnailUrl: null,
  tags: ["starter", "home"],
  explanationTemplate: "Built for fat loss, beginner level, and limited equipment.",
  weekSummaries: [
    { weekNumber: 1, focus: "full_body" },
    { weekNumber: 2, focus: "consistency" }
  ],
  createdAt: <timestamp>,
  updatedAt: <timestamp>
}
```

If a template needs detailed workout structure, store it in a map or array only if the payload stays small. If it grows large, add a subcollection:

```text
program_templates/{programId}/scheduled_workouts/{scheduledWorkoutId}
```

### `achievement_definitions/{achievementId}`

```js
{
  title: "First Workout",
  description: "Complete your first workout session.",
  type: "count",
  metric: "workout_sessions_completed",
  target: 1,
  iconKey: "first_workout",
  sortOrder: 10
}
```

### `food_catalog/{foodId}`

Optional shared food reference for meal scan correction and manual logging.

```js
{
  name: "Boiled Egg",
  servingUnit: "piece",
  servingSize: 1,
  calories: 78,
  protein: 6.3,
  carbs: 0.6,
  fat: 5.3,
  source: "seed"
}
```

## 5. User-Owned Collections

### `users/{uid}/reminders/{reminderId}`

```js
{
  type: "workout", // workout | meal | water | sleep
  enabled: true,
  timeMinutes: 420,
  repeatRule: "mon,wed,fri",
  label: "Morning workout",
  updatedAt: <timestamp>
}
```

### `users/{uid}/health_connections/{provider}`

Use provider id as document id, for example `health_connect`.

```js
{
  provider: "health_connect",
  status: "connected",
  scopes: ["steps", "heart_rate", "weight"],
  lastSyncAt: <timestamp>,
  updatedAt: <timestamp>
}
```

### `users/{uid}/plan_instances/{planId}`

This is the user's copy of a program template, or a fully custom plan. Keep enough data here so the plan stays stable even if the global template changes later.

```js
{
  sourceProgramId: "starter_fat_loss_01",
  name: "My Home Fat Loss Plan",
  goal: "fat_loss",
  durationWeeks: 4,
  workoutsPerWeek: 3,
  equipment: "home_basic",
  trainingStyle: "full_body",
  status: "active", // draft | active | completed | archived
  startedAt: <timestamp>,
  completedAt: null,
  explanation: "Generated from onboarding answers.",
  currentWeek: 2,
  nextWorkoutDate: "2026-04-24",
  createdAt: <timestamp>,
  updatedAt: <timestamp>
}
```

### `users/{uid}/plan_instances/{planId}/scheduled_workouts/{scheduledWorkoutId}`

Query this subcollection for Today and This Week screens.

```js
{
  dateKey: "2026-04-24",
  weekNumber: 2,
  orderInWeek: 1,
  title: "Full Body Basics",
  durationMinutes: 40,
  estimatedCalories: 220,
  difficulty: "beginner",
  equipment: "home_basic",
  status: "scheduled", // scheduled | completed | skipped | replaced
  explanation: "Selected for your fat loss goal and morning schedule.",
  replacedFromWorkoutId: null,
  exercises: [
    {
      exerciseId: "bodyweight_squat",
      name: "Bodyweight Squat",
      sets: 3,
      reps: "12",
      durationSeconds: null
    }
  ],
  createdAt: <timestamp>,
  updatedAt: <timestamp>
}
```

Embedding `exercises` here is intentional. Workout detail should not need to join multiple collections for every item.

### `users/{uid}/workout_sessions/{sessionId}`

Store one document per actual workout attempt.

```js
{
  planId: "plan_2026_04_a",
  scheduledWorkoutId: "2026_04_24_full_body",
  title: "Full Body Basics",
  source: "plan", // plan | library | coach | custom
  status: "completed", // in_progress | completed | abandoned
  startedAt: <timestamp>,
  endedAt: <timestamp>,
  durationMinutes: 38,
  caloriesBurned: 214,
  completionRate: 0.92,
  perceivedEffort: 7,
  notes: null,
  createdAt: <timestamp>,
  updatedAt: <timestamp>
}
```

### `users/{uid}/workout_sessions/{sessionId}/exercise_logs/{exerciseLogId}`

```js
{
  exerciseId: "bodyweight_squat",
  name: "Bodyweight Squat",
  orderIndex: 1,
  plannedSets: 3,
  completedSets: 3,
  repsBySet: [12, 12, 10],
  weightKgBySet: [0, 0, 0],
  durationSeconds: null,
  completed: true
}
```

### `users/{uid}/meal_logs/{mealId}`

Embed food items because each meal usually stays small.

```js
{
  mealType: "lunch",
  source: "scan", // scan | manual | coach
  imageUrl: "gs://fitty-app/users/uid/meals/meal_01.jpg",
  loggedAt: <timestamp>,
  dateKey: "2026-04-23",
  totalCalories: 640,
  totalProtein: 35,
  totalCarbs: 58,
  totalFat: 24,
  confidence: 0.82,
  foodItems: [
    {
      name: "Chicken breast",
      quantity: 150,
      unit: "g",
      calories: 248,
      protein: 46,
      carbs: 0,
      fat: 5.4,
      confidence: 0.91
    }
  ],
  notes: null,
  createdAt: <timestamp>,
  updatedAt: <timestamp>
}
```

### `users/{uid}/body_measurements/{measurementId}`

```js
{
  measuredAt: <timestamp>,
  dateKey: "2026-04-23",
  weightKg: 77.8,
  bodyFatPercent: 19.1,
  waistCm: 83,
  chestCm: 98,
  hipCm: 94,
  source: "manual" // manual | health_connect | body_scan
}
```

### `users/{uid}/body_scans/{scanId}`

```js
{
  capturedAt: <timestamp>,
  frontImageUrl: "gs://fitty-app/users/uid/body-scans/scan_01/front.jpg",
  sideImageUrl: null,
  summary: "Waist reduced and posture improved.",
  confidence: 0.76,
  metrics: {
    estimatedBodyFatPercent: 18.9,
    postureScore: 72
  },
  status: "processed" // pending | processed | failed
}
```

### `users/{uid}/water_logs/{waterLogId}`

```js
{
  loggedAt: <timestamp>,
  dateKey: "2026-04-23",
  amountMl: 350,
  source: "manual"
}
```

### `users/{uid}/daily_check_ins/{dateKey}`

```js
{
  energyLevel: 3,
  sleepQuality: 4,
  soreness: 2,
  motivation: 5,
  note: "Felt good after sleep.",
  createdAt: <timestamp>,
  updatedAt: <timestamp>
}
```

### `users/{uid}/daily_summaries/{dateKey}`

This collection is the main denormalized layer for Home and Progress. Update it whenever workout, meal, water, or habit data changes.

```js
{
  dateKey: "2026-04-23",
  targets: {
    calories: 2100,
    waterMl: 2500,
    workouts: 1,
    steps: 8000
  },
  progress: {
    caloriesConsumed: 1480,
    waterMl: 1400,
    workoutsCompleted: 1,
    steps: 6200
  },
  summaries: {
    todayWorkoutTitle: "Full Body Basics",
    mealsLoggedCount: 2,
    currentStreak: 4,
    insightText: "Protein is on track. Water is below target."
  },
  generatedAt: <timestamp>,
  updatedAt: <timestamp>
}
```

### `users/{uid}/achievements/{achievementId}`

```js
{
  title: "First Workout",
  unlocked: true,
  unlockedAt: <timestamp>,
  progress: 1,
  target: 1,
  sourceMetric: "workout_sessions_completed"
}
```

Store a small user snapshot here so the Stats screen does not need to join `achievement_definitions` for every row. The global definition collection still stays useful for seed data and admin management.

### `users/{uid}/habits/{dateKey}`

```js
{
  dateKey: "2026-04-23",
  items: {
    drankEnoughWater: true,
    trainedToday: true,
    sleptBefore11pm: false,
    noSugaryDrinks: true,
    walked8kSteps: false
  },
  updatedAt: <timestamp>
}
```

### `users/{uid}/coach_threads/{threadId}`

```js
{
  title: "Weekly plan adjustments",
  lastMessagePreview: "I missed yesterday's session",
  lastMessageAt: <timestamp>,
  messageCount: 12,
  createdAt: <timestamp>,
  updatedAt: <timestamp>
}
```

### `users/{uid}/coach_threads/{threadId}/messages/{messageId}`

```js
{
  role: "assistant", // user | assistant | system
  text: "You can swap today with a recovery walk.",
  attachments: [],
  suggestions: [
    {
      type: "plan_adjustment",
      title: "Move strength workout to Friday",
      actionLabel: "Apply to Plan",
      payload: {
        targetPlanId: "plan_2026_04_a",
        moveFromDate: "2026-04-23",
        moveToDate: "2026-04-25"
      }
    }
  ],
  createdAt: <timestamp>
}
```

Use a subcollection here because chat history can grow without bound.

## 6. Read Patterns

Recommended read patterns by screen:

- Splash/Auth bootstrap:
  - `users/{uid}`
- Home:
  - `users/{uid}`
  - `users/{uid}/daily_summaries/{today}`
  - active plan's next scheduled workout
- Plan Today/Week:
  - `users/{uid}/plan_instances/{activePlanId}`
  - `scheduled_workouts` filtered by `dateKey`
- Workout session:
  - one `scheduled_workouts` doc
  - later write one `workout_sessions` doc plus `exercise_logs`
- Meals:
  - `meal_logs` filtered by `dateKey` or `loggedAt`
- Progress:
  - `daily_summaries` range query
  - `body_measurements` range query
- Coach:
  - `coach_threads` ordered by `lastMessageAt desc`
  - `messages` inside current thread

## 7. Indexes To Create

Start with these composite indexes:

1. `program_templates`
   - `goal ASC`, `difficulty ASC`, `equipment ASC`
2. `users/{uid}/plan_instances`
   - `status ASC`, `updatedAt DESC`
3. `users/{uid}/plan_instances/{planId}/scheduled_workouts`
   - `dateKey ASC`, `status ASC`
4. `users/{uid}/workout_sessions`
   - `startedAt DESC`, `status ASC`
5. `users/{uid}/meal_logs`
   - `dateKey ASC`, `loggedAt DESC`
6. `users/{uid}/body_measurements`
   - `measuredAt DESC`
7. `users/{uid}/daily_summaries`
   - `dateKey DESC`
8. `users/{uid}/coach_threads`
   - `lastMessageAt DESC`

Adjust indexes later based on real failed-query prompts from Firestore.

## 8. Security Rules Direction

Basic rule model:

- A signed-in user can read and write only `users/{request.auth.uid}` and its subcollections.
- Shared catalog collections are read-only for normal users.
- Only admin users can write `exercise_library`, `program_templates`, `achievement_definitions`, and `food_catalog`.
- Reject writes if `request.auth == null`.

Rule intent:

```text
match /users/{userId} {
  allow read, write: if request.auth != null && request.auth.uid == userId;
}

match /exercise_library/{docId} {
  allow read: if request.auth != null;
  allow write: if request.auth.token.admin == true;
}
```

You can tighten this later with field validation for protected values such as `stats`, `activePlanId`, or admin-managed documents.

## 9. Storage Paths

Put large images in Cloud Storage, not Firestore:

```text
users/{uid}/meals/{mealId}.jpg
users/{uid}/body-scans/{scanId}/front.jpg
users/{uid}/body-scans/{scanId}/side.jpg
users/{uid}/avatars/profile.jpg
```

Store only the file URL or storage path in Firestore.

## 10. Mapping From Current Local Schema

Suggested migration mapping from the current SQLite design:

- `users` + `user_profiles` + `user_settings` + `onboarding_preferences`
  - `users/{uid}`
- `workout_availability`
  - inside `users/{uid}.onboarding.workoutDays` or `users/{uid}/reminders`
- `reminders`
  - `users/{uid}/reminders/{reminderId}`
- `exercises` and related detail tables
  - `exercise_library/{exerciseId}`
- `programs` + `program_weeks` + `program_sessions`
  - `program_templates/{programId}` and optional `scheduled_workouts`
- `user_plans` + `user_plan_days` + `user_workouts`
  - `users/{uid}/plan_instances/{planId}` and `scheduled_workouts`
- `workout_logs` + `exercise_logs`
  - `users/{uid}/workout_sessions/{sessionId}` and `exercise_logs`
- `meals` + `meal_items`
  - `users/{uid}/meal_logs/{mealId}`
- `body_measurements`
  - `users/{uid}/body_measurements/{measurementId}`
- `body_scan_records`
  - `users/{uid}/body_scans/{scanId}`
- `water_logs`
  - `users/{uid}/water_logs/{waterLogId}`
- `daily_tasks` + `streaks`
  - `users/{uid}/daily_summaries/{dateKey}` and `users/{uid}.stats`
- `achievements` + `user_achievements`
  - `achievement_definitions/{achievementId}` and `users/{uid}/achievements/{achievementId}`
- `ai_messages` + `ai_insights`
  - `users/{uid}/coach_threads/{threadId}/messages/{messageId}` and `daily_summaries`

## 11. Practical Notes

- Do not over-normalize in Firestore. Duplicate small display fields on purpose.
- Keep documents small and query-friendly.
- Prefer one write to `daily_summaries` after important events so Home can render fast.
- Use Cloud Functions or client-side transaction/batch writes for streak and achievement updates.
- If offline support matters, this schema works well with Firestore local persistence.

This schema is a strong starting point for the current Fitty scope. It is optimized for the app flows already described in the use-case docs, not for a generic fitness product.
