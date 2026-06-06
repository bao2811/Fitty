package com.example.fitty.data.exercise

import com.example.fitty.data.remote.exercise.ExerciseApiService
import com.example.fitty.domain.model.Exercise
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okio.buffer
import okio.sink
import retrofit2.HttpException
import java.io.File
import java.net.URLDecoder
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
            fallbackUrl = exercise.thumbnailUrl,
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
            fallbackUrl = exercise.gifUrl,
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
        fallbackUrl: String,
        existingPath: String,
        expectedFile: File,
        tempFile: File
    ): Pair<String, Int> {
        if (storagePath.isBlank() && fallbackUrl.isBlank()) return "" to 0
        fileStore.existingFile(expectedFile.absolutePath)?.let {
            fileStore.clearIfDifferent(existingPath, expectedFile.absolutePath)
            return expectedFile.absolutePath to 0
        }

        if (fallbackUrl.isFirebaseStorageUrl() || storagePath.isNotBlank()) return "" to 0

        val resolvedStoragePath = storagePath.ifBlank { fallbackUrl.firebaseStorageObjectPath().orEmpty() }

        if (fallbackUrl.isNotBlank()) {
            runCatching {
                return downloadIfNeeded(
                    remoteUrl = fallbackUrl,
                    existingPath = existingPath,
                    expectedFile = expectedFile,
                    tempFile = tempFile
                )
            }.onFailure { error ->
                if (!error.isStorageAccessDenied()) throw error
                tempFile.delete()
                if (resolvedStoragePath.isBlank()) return "" to 0
            }
        }

        if (resolvedStoragePath.isBlank()) return "" to 0
        return downloadFromFirebaseStorage(
            storagePath = resolvedStoragePath,
            existingPath = existingPath,
            expectedFile = expectedFile,
            tempFile = tempFile
        )
    }

    private suspend fun downloadFromFirebaseStorage(
        storagePath: String,
        existingPath: String,
        expectedFile: File,
        tempFile: File
    ): Pair<String, Int> {
        expectedFile.parentFile?.mkdirs()
        tempFile.parentFile?.mkdirs()
        runCatching {
            firebaseStorage.reference.child(storagePath).getFile(tempFile).await()
        }.onFailure { error ->
            tempFile.delete()
            if (error.isStorageAccessDenied()) return "" to 0
            throw error
        }
        if (expectedFile.exists()) expectedFile.delete()
        tempFile.copyTo(expectedFile, overwrite = true)
        tempFile.delete()
        fileStore.clearIfDifferent(existingPath, expectedFile.absolutePath)
        return expectedFile.absolutePath to 1
    }

    private fun Throwable.isStorageAccessDenied(): Boolean {
        val httpCode = (this as? HttpException)?.code()
        if (httpCode == 401 || httpCode == 403 || httpCode == 404) return true
        val storageErrorCode = (this as? StorageException)?.errorCode
        return storageErrorCode == StorageException.ERROR_NOT_AUTHENTICATED ||
            storageErrorCode == StorageException.ERROR_NOT_AUTHORIZED ||
            storageErrorCode == StorageException.ERROR_OBJECT_NOT_FOUND
    }

    private fun String.isFirebaseStorageUrl(): Boolean {
        return contains("firebasestorage.googleapis.com", ignoreCase = true)
    }

    private fun String.firebaseStorageObjectPath(): String? {
        if (!isFirebaseStorageUrl()) return null
        val marker = "/o/"
        val start = indexOf(marker).takeIf { it >= 0 }?.plus(marker.length) ?: return null
        val encodedPath = substring(start).substringBefore("?").ifBlank { return null }
        return URLDecoder.decode(encodedPath, Charsets.UTF_8.name())
    }
}
