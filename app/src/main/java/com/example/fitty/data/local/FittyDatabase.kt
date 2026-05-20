package com.example.fitty.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.fitty.data.local.exercise.ExerciseDao
import com.example.fitty.data.local.exercise.ExerciseEntity
import com.example.fitty.data.local.exercise.ExerciseHistoryDao
import com.example.fitty.data.local.exercise.ExerciseHistoryEntity
import com.example.fitty.data.local.exercise.ExerciseSyncStateDao
import com.example.fitty.data.local.exercise.ExerciseSyncStateEntity
import com.example.fitty.data.local.notification.AppNotificationDao
import com.example.fitty.data.local.notification.AppNotificationEntity
import com.example.fitty.data.local.task.HomeTaskDao
import com.example.fitty.data.local.task.HomeTaskDaySeedEntity
import com.example.fitty.data.local.task.HomeTaskEntity

@Database(
    entities = [
        HomeTaskEntity::class,
        HomeTaskDaySeedEntity::class,
        AppNotificationEntity::class,
        ExerciseEntity::class,
        ExerciseSyncStateEntity::class,
        ExerciseHistoryEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class FittyDatabase : RoomDatabase() {
    abstract fun homeTaskDao(): HomeTaskDao
    abstract fun appNotificationDao(): AppNotificationDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun exerciseSyncStateDao(): ExerciseSyncStateDao
    abstract fun exerciseHistoryDao(): ExerciseHistoryDao
}
