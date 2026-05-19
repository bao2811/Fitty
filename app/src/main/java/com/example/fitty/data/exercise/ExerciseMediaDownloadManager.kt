package com.example.fitty.data.exercise

import com.example.fitty.data.remote.exercise.ExerciseApiService
import com.example.fitty.domain.model.Exercise
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okio.buffer
import okio.sink
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExerciseMediaDownloadManager @Inject constructor(
    private val apiService: ExerciseApiService,
    private val fileStore: ExerciseMediaFileStore,
    private val firebaseStorage: FirebaseStorage
) {
    suspend fun cacheThumbnail(exercise: Exercise): Pair<String, Int> = withContext(Dispatchers.IO) {
        val version = exercise.remoteVersion.ifBlank { exercise.updatedAt.ifBlank { "v1" } }
        downloadFromStorageIfNeeded(
            storagePath = exercise.thumbnailStoragePath,
            existingPath = exercise.localThumbnailPath,
            expectedFile = fileStore.finalFileFor(exercise.id, ExerciseMediaType.IMAGE, version),
            tempFile = fileStore.tempFileFor(exercise.id, ExerciseMediaType.IMAGE)
        )
    }

    suspend fun downloadVideo(exercise: Exercise): Pair<String, Int> = withContext(Dispatchers.IO) {
        val version = exercise.remoteVersion.ifBlank { exercise.updatedAt.ifBlank { "v1" } }
        downloadIfNeeded(
            remoteUrl = exercise.videoUrl,
            existingPath = exercise.localVideoPath,
            expectedFile = fileStore.finalFileFor(exercise.id, ExerciseMediaType.VIDEO, version),
            tempFile = fileStore.tempFileFor(exercise.id, ExerciseMediaType.VIDEO)
        )
    }

    suspend fun downloadGif(exercise: Exercise): Pair<String, Int> = withContext(Dispatchers.IO) {
        val version = exercise.remoteVersion.ifBlank { exercise.updatedAt.ifBlank { "v1" } }
        val expectedFile = fileStore.finalFileFor(exercise.id, ExerciseMediaType.GIF, version)
        val tempFile = fileStore.tempFileFor(exercise.id, ExerciseMediaType.GIF)
        downloadFromStorageIfNeeded(
            storagePath = exercise.gifStoragePath,
            existingPath = exercise.localGifPath,
            expectedFile = expectedFile,
            tempFile = tempFile
        )
    }

    private suspend fun downloadIfNeeded(
        remoteUrl: String,
        existingPath: String,
        expectedFile: File,
        tempFile: File
    ): Pair<String, Int> {
        if (remoteUrl.isBlank()) return "" to 0
        fileStore.existingFile(expectedFile.absolutePath)?.let {
            fileStore.clearIfDifferent(existingPath, expectedFile.absolutePath)
            return expectedFile.absolutePath to 0
        }

        expectedFile.parentFile?.mkdirs()
        tempFile.parentFile?.mkdirs()
        apiService.downloadFile(remoteUrl).use { responseBody ->
            tempFile.sink().buffer().use { sink ->
                sink.writeAll(responseBody.source())
            }
        }
        if (expectedFile.exists()) expectedFile.delete()
        tempFile.copyTo(expectedFile, overwrite = true)
        tempFile.delete()
        fileStore.clearIfDifferent(existingPath, expectedFile.absolutePath)
        return expectedFile.absolutePath to 1
    }

    private suspend fun downloadFromStorageIfNeeded(
        storagePath: String,
        existingPath: String,
        expectedFile: File,
        tempFile: File
    ): Pair<String, Int> {
        if (storagePath.isBlank()) return "" to 0
        fileStore.existingFile(expectedFile.absolutePath)?.let {
            fileStore.clearIfDifferent(existingPath, expectedFile.absolutePath)
            return expectedFile.absolutePath to 0
        }

        expectedFile.parentFile?.mkdirs()
        tempFile.parentFile?.mkdirs()
        firebaseStorage.reference.child(storagePath).getFile(tempFile).await()
        if (expectedFile.exists()) expectedFile.delete()
        tempFile.copyTo(expectedFile, overwrite = true)
        tempFile.delete()
        fileStore.clearIfDifferent(existingPath, expectedFile.absolutePath)
        return expectedFile.absolutePath to 1
    }
}
