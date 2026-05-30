package com.example.fitty.data.content

import com.example.fitty.domain.model.Exercise
import com.example.fitty.domain.model.ExercisePrescriptionContent
import com.example.fitty.domain.model.ExercisePrescriptionRecommendation
import com.example.fitty.domain.model.ExercisePrescriptionRule
import com.example.fitty.domain.model.FittyUser
import com.example.fitty.domain.model.StarterExerciseTemplate
import com.example.fitty.domain.model.StarterPlanProfile
import java.util.Locale
import kotlin.math.round
import kotlin.math.pow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExercisePrescriptionResolver @Inject constructor(
    private val weightAdvisor: ExerciseWeightAdvisor
) {
    fun resolve(
        exercise: Exercise,
        user: FittyUser?,
        language: String,
        catalog: List<ExercisePrescriptionContent>
    ): ExercisePrescriptionRecommendation? {
        val profile = user?.toStarterPlanProfile(language)
        val content = catalog.firstOrNull { it.exerciseId.equals(exercise.id, ignoreCase = true) }
        val matchedRule = content?.rules
            .orEmpty()
            .mapNotNull { rule ->
                val score = rule.matchScore(profile) ?: return@mapNotNull null
                score to rule
            }
            .maxByOrNull { it.first }
            ?.second

        if (matchedRule != null) {
            return matchedRule.toRecommendation(profile, content?.note.orEmpty(), language, weightAdvisor)
        }

        buildStrengthFallback(exercise, profile, language)?.let { return it }

        val fallbackDuration = exercise.defaultDurationSeconds?.takeIf { it > 0 }
            ?: exercise.durationSeconds.takeIf { it > 0 }
        val fallbackReps = exercise.defaultRepsText.takeIf { it.isNotBlank() }
        if (fallbackReps == null && fallbackDuration == null) return null
        return ExercisePrescriptionRecommendation(
            sets = 3,
            reps = fallbackReps,
            durationSeconds = fallbackDuration
        )
    }

    private fun buildStrengthFallback(
        exercise: Exercise,
        profile: StarterPlanProfile?,
        language: String
    ): ExercisePrescriptionRecommendation? {
        val bodyPart = exercise.bodyPart.lowercase(Locale.US)
        val target = exercise.target.lowercase(Locale.US)
        val equipment = exercise.equipment.lowercase(Locale.US)
        val isCardioOrMobility = listOf("cardio", "neck", "stretch", "mobility", "warm").any { token ->
            bodyPart.contains(token) || target.contains(token) || exercise.name.lowercase(Locale.US).contains(token)
        }
        if (isCardioOrMobility) return null

        val targetWeightMode = when {
            equipment.contains("body") || equipment.contains("none") ->
                StarterExerciseTemplate.TargetWeightMode.Bodyweight

            listOf("upper legs", "lower legs", "glute", "hamstring", "quadriceps", "calf").any { token ->
                bodyPart.contains(token) || target.contains(token)
            } -> StarterExerciseTemplate.TargetWeightMode.LowerBody

            listOf("chest", "back", "shoulder", "arm", "biceps", "triceps", "lat", "pector").any { token ->
                bodyPart.contains(token) || target.contains(token)
            } -> StarterExerciseTemplate.TargetWeightMode.UpperBody

            else -> return null
        }

        val effectiveProfile = profile?.let {
            val equipmentOverride = when {
                equipment.contains("dumbbell") || equipment.contains("band") -> "home_basic"
                listOf("barbell", "cable", "machine", "smith", "leverage").any { token -> equipment.contains(token) } -> "gym"
                else -> it.equipment
            }
            it.copy(equipment = equipmentOverride)
        }
        val targetWeight = weightAdvisor.suggestTargetWeight(
            profile = effectiveProfile,
            targetWeightMode = targetWeightMode,
            fixedTargetWeightKg = null,
            bodyWeightMultiplier = null,
            minSuggestedWeightKg = null,
            maxSuggestedWeightKg = null
        )
        val targetWeightLabel = if (targetWeightMode == StarterExerciseTemplate.TargetWeightMode.Bodyweight) {
            if (language.lowercase(Locale.US).startsWith("vi")) "Tự trọng" else "Bodyweight"
        } else {
            null
        }
        val reps = exercise.defaultRepsText.takeIf { it.isNotBlank() }
            ?: when (profile?.fitnessLevel) {
                "advanced" -> "6-10"
                "beginner" -> "10-12"
                else -> "8-12"
            }
        val sets = when (profile?.fitnessLevel) {
            "advanced" -> 4
            "beginner" -> 3
            else -> 3
        }
        return ExercisePrescriptionRecommendation(
            sets = sets,
            reps = reps,
            targetWeightKg = targetWeight,
            targetWeightLabel = targetWeightLabel,
            debugSummary = "fallback=strength | prescription=$sets sets | reps=$reps" +
                (targetWeightLabel?.let { " | weight=$it" } ?: "") +
                (targetWeight?.let { " | resolvedWeight=${it}kg" } ?: "")
        )
    }

    private fun ExercisePrescriptionRule.matchScore(profile: StarterPlanProfile?): Int? {
        var score = 0
        if (goal.isNotBlank()) {
            val candidateGoal = profile?.goal ?: return null
            if (!goal.equals(candidateGoal, ignoreCase = true)) return null
            score += 8
        }
        if (fitnessLevels.isNotEmpty()) {
            val fitnessLevel = profile?.fitnessLevel ?: return null
            if (fitnessLevels.none { it.equals(fitnessLevel, ignoreCase = true) }) return null
            score += 4
        }
        if (equipments.isNotEmpty()) {
            val equipment = profile?.equipment ?: return null
            if (equipments.none { it.equals(equipment, ignoreCase = true) }) return null
            score += 4
        }
        if (minWeightKg != null || maxWeightKg != null) {
            val weightKg = profile?.weightKg ?: return null
            if (minWeightKg != null && weightKg < minWeightKg) return null
            if (maxWeightKg != null && weightKg > maxWeightKg) return null
            score += 6
        }
        return score
    }

    private fun ExercisePrescriptionRule.toRecommendation(
        profile: StarterPlanProfile?,
        note: String,
        language: String,
        weightAdvisor: ExerciseWeightAdvisor
    ): ExercisePrescriptionRecommendation {
        val targetWeight = weightAdvisor.suggestTargetWeight(
            profile = profile,
            targetWeightMode = targetWeightMode,
            fixedTargetWeightKg = fixedTargetWeightKg,
            bodyWeightMultiplier = bodyWeightMultiplier,
            minSuggestedWeightKg = minSuggestedWeightKg,
            maxSuggestedWeightKg = maxSuggestedWeightKg
        )
        val targetWeightLabel = if (targetWeightMode == StarterExerciseTemplate.TargetWeightMode.Bodyweight) {
            if (language.lowercase(Locale.US).startsWith("vi")) "Tự trọng" else "Bodyweight"
        } else {
            null
        }
        return ExercisePrescriptionRecommendation(
            sets = sets,
            reps = reps,
            durationSeconds = durationSeconds,
            targetWeightKg = targetWeight,
            targetWeightLabel = targetWeightLabel,
            note = note,
            debugSummary = buildDebugSummary(profile, targetWeight)
        )
    }

    private fun ExercisePrescriptionRule.buildDebugSummary(
        profile: StarterPlanProfile?,
        resolvedTargetWeightKg: Float?
    ): String {
        val parts = mutableListOf<String>()
        goal.takeIf { it.isNotBlank() }?.let { parts += "goal=$it" }
        if (fitnessLevels.isNotEmpty()) parts += "fitness=${fitnessLevels.joinToString("/")}"
        if (equipments.isNotEmpty()) parts += "equipment=${equipments.joinToString("/")}"
        if (minWeightKg != null || maxWeightKg != null) {
            parts += "weightRange=${minWeightKg ?: "-"}..${maxWeightKg ?: "-"}kg"
        }
        parts += "prescription=${sets} sets"
        reps?.let { parts += "reps=$it" }
        durationSeconds?.let { parts += "duration=${it}s" }
        parts += "weightMode=$targetWeightMode"
        fixedTargetWeightKg?.let { parts += "fixed=${it}kg" }
        bodyWeightMultiplier?.let { parts += "bodyWeightMultiplier=$it" }
        minSuggestedWeightKg?.let { parts += "minSuggested=${it}kg" }
        maxSuggestedWeightKg?.let { parts += "maxSuggested=${it}kg" }
        profile?.weightKg?.let { parts += "userWeight=${it}kg" }
        profile?.fitnessLevel?.takeIf { it.isNotBlank() }?.let { parts += "userFitness=$it" }
        profile?.equipment?.takeIf { it.isNotBlank() }?.let { parts += "userEquipment=$it" }
        resolvedTargetWeightKg?.let { parts += "resolvedWeight=${it}kg" }
        return parts.joinToString(" | ")
    }

    private fun FittyUser.toStarterPlanProfile(language: String): StarterPlanProfile {
        return StarterPlanProfile(
            goal = profile.primaryGoal,
            fitnessLevel = profile.fitnessLevel,
            equipment = onboarding.equipmentAccess,
            heightCm = profile.heightCm,
            weightKg = profile.weightKg,
            workoutDays = onboarding.workoutDays,
            durationMinutes = onboarding.workoutDurationMinutes,
            preferredTime = onboarding.preferredTime,
            language = language
        )
    }
}

@Singleton
class ExerciseWeightAdvisor @Inject constructor() {
    fun suggestTargetWeight(
        profile: StarterPlanProfile?,
        targetWeightMode: String,
        fixedTargetWeightKg: Float?,
        bodyWeightMultiplier: Float?,
        minSuggestedWeightKg: Float?,
        maxSuggestedWeightKg: Float?
    ): Float? {
        fixedTargetWeightKg?.let { return it.roundToHalfKg() }
        val bodyWeightKg = profile?.weightKg ?: return null
        val heightCm = profile.heightCm?.toFloat()
        val bmi = if (heightCm != null && heightCm > 0f) {
            val heightM = heightCm / 100f
            bodyWeightKg / heightM.pow(2)
        } else null
        bodyWeightMultiplier?.let { multiplier ->
            val suggested = bodyWeightKg * multiplier * heightAdjustment(heightCm) * bmiAdjustment(bmi)
            return suggested
                .coerceIn(
                    minSuggestedWeightKg ?: suggested,
                    maxSuggestedWeightKg ?: suggested
                )
                .roundToHalfKg()
        }
        val defaultMultiplier = when (targetWeightMode) {
            StarterExerciseTemplate.TargetWeightMode.UpperBody -> when (profile.fitnessLevel) {
                "advanced" -> 0.14f
                "intermediate" -> 0.1f
                else -> 0.06f
            }
            StarterExerciseTemplate.TargetWeightMode.LowerBody -> when (profile.fitnessLevel) {
                "advanced" -> 0.22f
                "intermediate" -> 0.16f
                else -> 0.1f
            }
            else -> return null
        }
        val suggested = bodyWeightKg * defaultMultiplier * heightAdjustment(heightCm) * bmiAdjustment(bmi)
        val equipmentBounds = when (profile.equipment) {
            "home_basic" -> if (targetWeightMode == StarterExerciseTemplate.TargetWeightMode.UpperBody) {
                2.5f to 12f
            } else {
                2.5f to 18f
            }
            "gym", "mix" -> if (targetWeightMode == StarterExerciseTemplate.TargetWeightMode.UpperBody) {
                5f to 20f
            } else {
                5f to 30f
            }
            else -> return null
        }
        return suggested
            .coerceIn(
                minSuggestedWeightKg ?: equipmentBounds.first,
                maxSuggestedWeightKg ?: equipmentBounds.second
            )
            .roundToHalfKg()
    }

    private fun heightAdjustment(heightCm: Float?): Float {
        if (heightCm == null || heightCm <= 0f) return 1f
        val deviation = (heightCm - 170f) / 100f
        return (1f + deviation).coerceIn(0.92f, 1.08f)
    }

    private fun bmiAdjustment(bmi: Float?): Float {
        if (bmi == null || bmi <= 0f) return 1f
        return when {
            bmi < 19f -> 0.9f
            bmi < 23f -> 0.97f
            bmi < 27f -> 1f
            else -> 1.05f
        }
    }

    private fun Float.roundToHalfKg(): Float = round(this * 2f) / 2f
}
