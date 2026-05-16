package com.example.fitty.data.exercise

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

enum class ExerciseMediaType(val folderName: String, val defaultExtension: String) {
    IMAGE("images", "jpg"),
    GIF("gifs", "gif"),
    VIDEO("videos", "mp4")
}

@Singleton
class ExerciseMediaFileStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun finalFileFor(exerciseId: String, type: ExerciseMediaType, version: String): File {
        val directory = File(context.filesDir, "exercise-media/${type.folderName}").apply { mkdirs() }
        return File(directory, "${exerciseId}_${sanitize(version)}.${type.defaultExtension}")
    }

    fun tempFileFor(exerciseId: String, type: ExerciseMediaType): File {
        val directory = File(context.cacheDir, "exercise-media-temp/${type.folderName}").apply { mkdirs() }
        return File(directory, "${exerciseId}.${type.defaultExtension}.download")
    }

    fun existingFile(path: String): File? {
        if (path.isBlank()) return null
        val file = File(path)
        return file.takeIf { it.exists() && it.isFile }
    }

    fun clearIfDifferent(existingPath: String, expectedPath: String) {
        if (existingPath.isBlank() || existingPath == expectedPath) return
        val file = File(existingPath)
        if (file.exists()) file.delete()
    }

    private fun sanitize(value: String): String =
        value.ifBlank { "v1" }.replace(Regex("[^a-zA-Z0-9._-]"), "_")
}
