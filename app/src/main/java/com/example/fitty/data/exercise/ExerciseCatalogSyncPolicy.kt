package com.example.fitty.data.exercise

import com.example.fitty.data.firebase.ExerciseFirestoreDocument
import java.util.Locale

internal object ExerciseCatalogSyncPolicy {
    const val STATUS_SUCCESS = "success"
    const val STATUS_EMPTY_REMOTE_DATA = "empty_remote_data"
    const val STATUS_INVALID_REMOTE_MAPPING = "invalid_remote_mapping"
    const val STATUS_OFFLINE_CACHED = "offline_cached"
    const val STATUS_NETWORK_ERROR = "network_error"

    private val supportedBodyParts = setOf(
        "chest",
        "back",
        "shoulders",
        "upper legs",
        "lower legs",
        "upper arms",
        "lower arms",
        "waist",
        "cardio",
        "neck"
    )

    private val aliases = mapOf(
        "abs" to "waist",
        "core" to "waist"
    )

    data class ValidationSummary(
        val fetched: Int = 0,
        val usable: Int = 0,
        val droppedMissingId: Int = 0,
        val droppedMissingName: Int = 0,
        val droppedMissingBodyPart: Int = 0,
        val droppedInvalidBodyPart: Int = 0
    ) {
        val statusCode: String?
            get() = when {
                fetched == 0 -> STATUS_EMPTY_REMOTE_DATA
                usable == 0 && droppedInvalidBodyPart > 0 -> STATUS_INVALID_REMOTE_MAPPING
                usable == 0 -> STATUS_EMPTY_REMOTE_DATA
                else -> STATUS_SUCCESS
            }
    }

    data class ValidationResult(
        val normalized: ExerciseFirestoreDocument? = null,
        val issue: String? = null
    )

    fun validate(document: ExerciseFirestoreDocument): ValidationResult {
        val id = document.id.trim()
        if (id.isBlank()) return ValidationResult(issue = "missing_id")

        val name = document.name.trim()
        if (name.isBlank()) return ValidationResult(issue = "missing_name")

        val rawBodyPart = document.bodyPart.ifBlank { document.muscleGroup }.trim()
        if (rawBodyPart.isBlank()) return ValidationResult(issue = "missing_body_part")

        val normalizedBodyPart = normalizeBodyPart(rawBodyPart)
            ?: return ValidationResult(issue = "invalid_body_part")

        return ValidationResult(
            normalized = document.copy(
                id = id,
                name = name,
                bodyPart = normalizedBodyPart,
                muscleGroup = normalizedBodyPart,
                target = document.target.trim(),
                equipment = document.equipment.trim(),
                difficulty = document.difficulty.trim(),
                description = document.description.trim(),
                instructions = document.instructions.trim(),
                thumbnailUrl = document.thumbnailUrl.trim(),
                thumbnailStoragePath = document.thumbnailStoragePath.trim(),
                gifUrl = document.gifUrl.trim(),
                gifStoragePath = document.gifStoragePath.trim(),
                videoUrl = document.videoUrl.trim(),
                updatedAt = document.updatedAt.trim()
            )
        )
    }

    private fun normalizeBodyPart(value: String): String? {
        val normalized = value
            .trim()
            .lowercase(Locale.ROOT)
            .replace('_', ' ')
            .replace(Regex("\\s+"), " ")
        val resolved = aliases[normalized] ?: normalized
        return resolved.takeIf { it in supportedBodyParts }
    }
}
