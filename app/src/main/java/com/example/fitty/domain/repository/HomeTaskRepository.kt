package com.example.fitty.domain.repository

import com.example.fitty.domain.model.HomeTask
import com.example.fitty.domain.model.HomeTaskDraft
import com.example.fitty.domain.model.HomeTaskStatus
import kotlinx.coroutines.flow.Flow

interface HomeTaskRepository {
    fun observeTasks(dateKey: String): Flow<List<HomeTask>>
    suspend fun ensureTasks(dateKey: String, defaults: List<HomeTaskDraft>)
    suspend fun addTask(task: HomeTaskDraft)
    suspend fun updateTaskStatus(taskId: Long, status: HomeTaskStatus)
    suspend fun updateTaskReminder(taskId: Long, enabled: Boolean)
    suspend fun deleteTask(taskId: Long)
}
