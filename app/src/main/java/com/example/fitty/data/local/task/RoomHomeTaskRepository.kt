package com.example.fitty.data.local.task

import com.example.fitty.domain.model.HomeTask
import com.example.fitty.domain.model.HomeTaskCategory
import com.example.fitty.domain.model.HomeTaskDraft
import com.example.fitty.domain.model.HomeTaskStatus
import com.example.fitty.domain.repository.HomeTaskRepository
import com.example.fitty.domain.repository.SessionRepository
import com.example.fitty.notifications.TaskReminderScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class RoomHomeTaskRepository @Inject constructor(
    private val taskDao: HomeTaskDao,
    private val sessionRepository: SessionRepository,
    private val reminderScheduler: TaskReminderScheduler
) : HomeTaskRepository {

    override fun observeTasks(dateKey: String): Flow<List<HomeTask>> {
        return sessionRepository.observeCurrentUserId()
            .map { ownerIdOrNull -> ownerIdOrNull.orEmpty().ifBlank { GUEST_OWNER_ID } }
            .distinctUntilChanged()
            .flatMapLatest { ownerId ->
                taskDao.observeTasks(ownerId = ownerId, dateKey = dateKey)
            }
            .map { tasks -> tasks.map { task -> task.toDomain() } }
    }

    override suspend fun ensureTasks(dateKey: String, defaults: List<HomeTaskDraft>) {
        val ownerId = currentOwnerId()
        val now = System.currentTimeMillis()
        val entities = defaults.map { draft ->
            HomeTaskEntity(
                ownerId = ownerId,
                title = draft.title,
                description = draft.description,
                dateKey = draft.dateKey,
                timeMinutes = draft.timeMinutes,
                category = draft.category.name,
                status = draft.status.name,
                reminderEnabled = draft.reminderEnabled,
                isDefault = draft.isDefault,
                createdAt = now,
                updatedAt = now
            )
        }
        taskDao.seedTasksIfNeeded(ownerId = ownerId, dateKey = dateKey, tasks = entities, nowMillis = now)
        taskDao.getTasksForDate(ownerId = ownerId, dateKey = dateKey).forEach { task ->
            syncReminder(task)
        }
    }

    override suspend fun addTask(task: HomeTaskDraft) {
        val ownerId = currentOwnerId()
        val now = System.currentTimeMillis()
        val taskId = taskDao.insertTask(
            HomeTaskEntity(
                ownerId = ownerId,
                title = task.title,
                description = task.description,
                dateKey = task.dateKey,
                timeMinutes = task.timeMinutes,
                category = task.category.name,
                status = task.status.name,
                reminderEnabled = task.reminderEnabled,
                isDefault = task.isDefault,
                createdAt = now,
                updatedAt = now
            )
        )
        if (taskId > 0L) {
            val insertedTask = taskDao.getTaskById(taskId)
            if (insertedTask != null) {
                syncReminder(insertedTask)
            }
        }
    }

    override suspend fun updateTaskStatus(taskId: Long, status: HomeTaskStatus) {
        val task = taskDao.getTaskById(taskId) ?: return
        val updated = task.copy(status = status.name, updatedAt = System.currentTimeMillis())
        taskDao.updateTask(updated)
        syncReminder(updated)
    }

    override suspend fun updateTaskReminder(taskId: Long, enabled: Boolean) {
        val task = taskDao.getTaskById(taskId) ?: return
        val updated = task.copy(reminderEnabled = enabled, updatedAt = System.currentTimeMillis())
        taskDao.updateTask(updated)
        syncReminder(updated)
    }

    override suspend fun deleteTask(taskId: Long) {
        reminderScheduler.cancel(taskId)
        taskDao.deleteTask(taskId)
    }

    private suspend fun syncReminder(task: HomeTaskEntity) {
        val status = task.status.toHomeTaskStatus()
        if (task.reminderEnabled && status != HomeTaskStatus.Completed) {
            reminderScheduler.schedule(
                taskId = task.id,
                title = task.title,
                message = task.description,
                dateKey = task.dateKey,
                timeMinutes = task.timeMinutes
            )
        } else {
            reminderScheduler.cancel(task.id)
        }
    }

    private suspend fun currentOwnerId(): String = sessionRepository.getCurrentUserId().orEmpty().ifBlank { GUEST_OWNER_ID }

    private fun HomeTaskEntity.toDomain(): HomeTask = HomeTask(
        id = id,
        title = title,
        description = description,
        dateKey = dateKey,
        timeMinutes = timeMinutes,
        category = category.toHomeTaskCategory(),
        status = status.toHomeTaskStatus(),
        reminderEnabled = reminderEnabled,
        isDefault = isDefault
    )

    private fun String.toHomeTaskCategory(): HomeTaskCategory = runCatching {
        enumValueOf<HomeTaskCategory>(this)
    }.getOrDefault(HomeTaskCategory.Custom)

    private fun String.toHomeTaskStatus(): HomeTaskStatus = runCatching {
        enumValueOf<HomeTaskStatus>(this)
    }.getOrDefault(HomeTaskStatus.Todo)

    private companion object {
        const val GUEST_OWNER_ID = "guest"
    }
}
