package com.example.fitty.data.firebase

data class FittyUser(
    val uid: String,
    val email: String,
    val displayName: String,
    val username: String,
    val photoUrl: String? = null,
    val authProvider: String,
    val guest: Boolean,
    val onboardingCompleted: Boolean,
    val profile: FittyProfile = FittyProfile(),
    val onboarding: FittyOnboarding = FittyOnboarding(),
    val stats: FittyStats = FittyStats(),
    val settings: FittySettings = FittySettings()
)

data class FittyProfile(
    val age: Int? = null,
    val gender: String = "",
    val heightCm: Int? = null,
    val weightKg: Int? = null,
    val targetWeightKg: Int? = null,
    val activityLevel: String = "",
    val fitnessLevel: String = "",
    val primaryGoal: String = "",
    val injuryNote: String = ""
)

data class FittyOnboarding(
    val workoutDays: List<String> = emptyList(),
    val workoutDurationMinutes: Int? = null,
    val preferredTime: String = "",
    val equipmentAccess: String = "",
    val nutritionStyle: String = "",
    val dietaryRestrictions: List<String> = emptyList()
)

data class FittyStats(
    val activePlanId: String = "",
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val totalWorkouts: Int = 0,
    val mealsLogged: Int = 0,
    val achievementsUnlocked: Int = 0
)

data class FittySettings(
    val language: String = "en",
    val themeMode: String = "system",
    val weightUnit: String = "kg",
    val heightUnit: String = "cm",
    val energyUnit: String = "kcal",
    val aiConsent: Boolean = true,
    val photoStorageEnabled: Boolean = true
)

data class FittyAuthResult(
    val user: FittyUser? = null,
    val errorMessage: String? = null
)

data class FittyOnboardingAnswers(
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

data class FittyStartupState(
    val uid: String? = null,
    val displayName: String = "",
    val isGuest: Boolean = false,
    val isSignedIn: Boolean = false,
    val onboardingCompleted: Boolean = false
)
