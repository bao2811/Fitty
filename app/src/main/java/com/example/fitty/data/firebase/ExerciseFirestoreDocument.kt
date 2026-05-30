package com.example.fitty.data.firebase

import com.example.fitty.domain.model.Exercise
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

data class ExerciseFirestoreDocument(
    val id: String,
    val name: String,
    val bodyPart: String,
    val muscleGroup: String,
    val target: String,
    val equipment: String,
    val difficulty: String,
    val description: String,
    val instructions: String,
    val thumbnailUrl: String,
    val thumbnailStoragePath: String,
    val gifUrl: String,
    val gifStoragePath: String,
    val videoUrl: String,
    val gifVersion: Int,
    val updatedAt: String
) {
    fun toDomain(): Exercise = Exercise(
        id = id,
        name = name,
        bodyPart = bodyPart,
        target = target,
        muscleGroup = muscleGroup.ifBlank { bodyPart },
        difficulty = difficulty,
        description = description,
        instructions = instructions.ifBlank { description },
        primaryMuscleGroup = muscleGroup.ifBlank { bodyPart },
        targetMuscles = listOf(target).filter { it.isNotBlank() },
        equipment = equipment,
        thumbnailUrl = thumbnailUrl,
        thumbnailStoragePath = thumbnailStoragePath,
        gifUrl = gifUrl,
        gifStoragePath = gifStoragePath,
        videoUrl = videoUrl,
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

    return ExerciseFirestoreDocument(
        id = getString("id").orEmpty().ifBlank { id },
        name = getString("name").orEmpty(),
        bodyPart = getString("bodyPart").orEmpty(),
        muscleGroup = getString("muscleGroup").orEmpty(),
        target = getString("target").orEmpty(),
        equipment = getString("equipment").orEmpty(),
        difficulty = getString("difficulty").orEmpty(),
        description = getString("description").orEmpty(),
        instructions = getString("instructions").orEmpty(),
        thumbnailUrl = getString("thumbnailUrl").orEmpty(),
        thumbnailStoragePath = getString("thumbnailStoragePath").orEmpty(),
        gifUrl = getString("gifUrl").orEmpty(),
        gifStoragePath = getString("gifStoragePath").orEmpty(),
        videoUrl = getString("videoUrl").orEmpty(),
        gifVersion = getLong("gifVersion")?.toInt() ?: 0,
        updatedAt = updatedAtText
    )
}
