package com.example.fitty.data.local.exercise

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercise_history")
data class ExerciseHistoryEntity(
    @PrimaryKey
    @ColumnInfo(name = "exerciseId")
    val exerciseId: String,
    @ColumnInfo(name = "lastViewedAt")
    val lastViewedAt: String
)
