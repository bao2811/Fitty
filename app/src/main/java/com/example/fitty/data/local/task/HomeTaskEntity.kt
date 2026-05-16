package com.example.fitty.data.local.task

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "home_tasks")
data class HomeTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val ownerId: String,
    val title: String,
    val description: String,
    val dateKey: String,
    val timeMinutes: Int,
    val category: String,
    val status: String,
    val reminderEnabled: Boolean,
    val isDefault: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "home_task_day_seed",
    primaryKeys = ["ownerId", "dateKey"]
)
data class HomeTaskDaySeedEntity(
    val ownerId: String,
    val dateKey: String,
    val seededAt: Long
)
