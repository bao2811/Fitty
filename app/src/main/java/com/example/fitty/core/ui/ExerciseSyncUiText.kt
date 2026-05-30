package com.example.fitty.core.ui

import android.content.Context
import com.example.fitty.R
import com.example.fitty.data.exercise.ExerciseCatalogSyncPolicy
import com.example.fitty.domain.model.ExerciseSyncState

internal enum class ExerciseSyncSuccessStyle {
    Cached,
    Count
}

internal fun ExerciseSyncState.toStatusText(
    context: Context,
    successStyle: ExerciseSyncSuccessStyle = ExerciseSyncSuccessStyle.Cached
): String? {
    return when (statusCode) {
        ExerciseCatalogSyncPolicy.STATUS_SUCCESS -> when (successStyle) {
            ExerciseSyncSuccessStyle.Cached -> context.getString(R.string.exercise_sync_cached)
            ExerciseSyncSuccessStyle.Count -> context.getString(R.string.plan_sync_success, totalExercises)
        }
        ExerciseCatalogSyncPolicy.STATUS_EMPTY_REMOTE_DATA ->
            context.getString(R.string.exercise_sync_empty_remote)
        ExerciseCatalogSyncPolicy.STATUS_INVALID_REMOTE_MAPPING ->
            context.getString(R.string.exercise_sync_invalid_mapping)
        ExerciseCatalogSyncPolicy.STATUS_OFFLINE_CACHED ->
            context.getString(R.string.exercise_sync_offline_cached)
        ExerciseCatalogSyncPolicy.STATUS_NETWORK_ERROR ->
            lastErrorMessage ?: context.getString(R.string.plan_sync_cached_fallback)
        else -> lastErrorMessage
    }
}

internal fun exerciseCategoryEmptyHintText(
    context: Context,
    statusCode: String?
): String {
    return when (statusCode) {
        ExerciseCatalogSyncPolicy.STATUS_EMPTY_REMOTE_DATA ->
            context.getString(R.string.category_sync_hint_empty_remote)
        ExerciseCatalogSyncPolicy.STATUS_INVALID_REMOTE_MAPPING ->
            context.getString(R.string.category_sync_hint_invalid_mapping)
        ExerciseCatalogSyncPolicy.STATUS_OFFLINE_CACHED ->
            context.getString(R.string.exercise_sync_offline_cached)
        else ->
            context.getString(R.string.category_sync_hint)
    }
}
