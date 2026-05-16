package com.example.fitty.data.firebase

import com.example.fitty.domain.model.Exercise
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

data class ExerciseFirestoreDocument(
    val id: String,
    val name: String,
    val bodyPart: String,
    val target: String,
    val equipment: String,
    val gifUrl: String,
    val gifStoragePath: String,
    val gifVersion: Int,
    val updatedAt: String
) {
    fun toDomain(): Exercise = Exercise(
        id = id,
        name = name,
        bodyPart = bodyPart,
        target = target,
        primaryMuscleGroup = bodyPart,
        targetMuscles = listOf(target).filter { it.isNotBlank() },
        equipment = equipment,
        gifUrl = gifUrl,
        gifStoragePath = gifStoragePath,
        gifVersion = gifVersion,
        updatedAt = updatedAt,
        mediaUrl = gifUrl,
        mediaType = "gif"
    )
}

fun DocumentSnapshot.toExerciseFirestoreDocument(): ExerciseFirestoreDocument? {
    if (!exists()) return null

    val updatedAtText = when (val raw = get("updatedAt")) {
        is String -> raw
        is Timestamp -> raw.toDate().toInstant().toString()
        else -> ""
    }

    val resolvedId = getString("id").orEmpty().ifBlank { id }
    if (resolvedId.isBlank()) return null

    return ExerciseFirestoreDocument(
        id = resolvedId,
        name = getString("name").orEmpty(),
        bodyPart = getString("bodyPart").orEmpty(),
        target = getString("target").orEmpty(),
        equipment = getString("equipment").orEmpty(),
        gifUrl = getString("gifUrl").orEmpty(),
        gifStoragePath = getString("gifStoragePath").orEmpty(),
        gifVersion = getLong("gifVersion")?.toInt() ?: 0,
        updatedAt = updatedAtText
    )
}
