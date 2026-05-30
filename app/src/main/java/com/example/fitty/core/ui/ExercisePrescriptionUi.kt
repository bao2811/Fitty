package com.example.fitty.core.ui

import android.content.Context
import com.example.fitty.R
import com.example.fitty.domain.model.ExercisePrescriptionRecommendation
import com.example.fitty.domain.model.WorkoutExercise
import java.util.Locale

fun ExercisePrescriptionRecommendation.toDisplaySummary(context: Context): String {
    return formatExercisePrescriptionSummary(
        context = context,
        sets = sets,
        reps = reps,
        durationSeconds = durationSeconds,
        targetWeightKg = targetWeightKg,
        targetWeightLabel = targetWeightLabel
    )
}

fun ExercisePrescriptionRecommendation.toDisplayBadges(context: Context): List<String> {
    return buildExercisePrescriptionBadges(
        context = context,
        sets = sets,
        reps = reps,
        durationSeconds = durationSeconds,
        targetWeightKg = targetWeightKg,
        targetWeightLabel = targetWeightLabel
    )
}

fun WorkoutExercise.toDisplaySummary(context: Context): String {
    return formatExercisePrescriptionSummary(
        context = context,
        sets = sets,
        reps = reps,
        durationSeconds = durationSeconds,
        targetWeightKg = targetWeightKg,
        targetWeightLabel = null
    )
}

fun WorkoutExercise.toDisplayBadges(context: Context): List<String> {
    return buildExercisePrescriptionBadges(
        context = context,
        sets = sets,
        reps = reps,
        durationSeconds = durationSeconds,
        targetWeightKg = targetWeightKg,
        targetWeightLabel = null
    )
}

private fun formatExercisePrescriptionSummary(
    context: Context,
    sets: Int,
    reps: String?,
    durationSeconds: Int?,
    targetWeightKg: Float?,
    targetWeightLabel: String?
): String {
    val repsLabel = reps?.takeIf { it.isNotBlank() }
    val weightLabel = targetWeightLabel ?: formatWeightKg(targetWeightKg)
    return when {
        sets > 0 && repsLabel != null && weightLabel != null ->
            context.getString(R.string.workout_sets_summary_with_weight, sets, repsLabel, weightLabel)

        sets > 0 && repsLabel != null ->
            context.getString(R.string.workout_sets_summary, sets, repsLabel)

        durationSeconds != null && durationSeconds > 0 ->
            context.getString(R.string.workout_details_duration_summary, sets.coerceAtLeast(1), durationSeconds)

        else -> context.getString(R.string.workout_details_sets_only, sets.coerceAtLeast(1))
    }
}

private fun buildExercisePrescriptionBadges(
    context: Context,
    sets: Int,
    reps: String?,
    durationSeconds: Int?,
    targetWeightKg: Float?,
    targetWeightLabel: String?
): List<String> {
    return listOfNotNull(
        if (sets > 0) context.getString(R.string.workout_details_sets_only, sets) else null,
        reps?.takeIf { it.isNotBlank() },
        durationSeconds?.takeIf { it > 0 }?.let { context.getString(R.string.exercise_prescription_duration, it) },
        targetWeightLabel ?: formatWeightKg(targetWeightKg)
    )
}

private fun formatWeightKg(weightKg: Float?): String? {
    val weight = weightKg ?: return null
    return if (weight % 1f == 0f) {
        "${weight.toInt()} kg"
    } else {
        String.format(Locale.US, "%.1f kg", weight)
    }
}
