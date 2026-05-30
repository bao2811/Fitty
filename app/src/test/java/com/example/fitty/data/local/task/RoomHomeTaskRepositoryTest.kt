package com.example.fitty.data.local.task

import android.app.AlarmManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.fitty.domain.model.FittyStartupState
import com.example.fitty.domain.model.FittyUser
import com.example.fitty.domain.model.HomeTask
import com.example.fitty.domain.model.HomeTaskCategory
import com.example.fitty.domain.model.HomeTaskDraft
import com.example.fitty.domain.model.HomeTaskStatus
import com.example.fitty.domain.repository.SessionRepository
import com.example.fitty.notifications.TaskReminderReceiver
import com.example.fitty.notifications.TaskReminderScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowAlarmManager
import org.robolectric.shadows.ShadowPendingIntent
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [35])
class RoomHomeTaskRepositoryTest {

    @Before
    fun resetAlarms() {
        ShadowAlarmManager.reset()
        ShadowAlarmManager.setCanScheduleExactAlarms(false)
    }

    @Test
    fun `observe tasks switches owner scope when session changes`() = runTest {
        val dateKey = "2026-05-29"
        val dao = FakeHomeTaskDao(
            initialTasks = listOf(
                task(id = 1, ownerId = "user-a", title = "A workout", dateKey = dateKey),
                task(id = 2, ownerId = "user-b", title = "B workout", dateKey = dateKey)
            )
        )
        val sessionRepository = SwitchingSessionRepository("user-a")
        val scheduler = TaskReminderScheduler(ApplicationProvider.getApplicationContext<Context>())
        val repository = RoomHomeTaskRepository(
            taskDao = dao,
            sessionRepository = sessionRepository,
            reminderScheduler = scheduler
        )

        val firstTasks = repository.observeTasks(dateKey).first()
        sessionRepository.setCurrentUserId("user-b")
        val secondTasks = repository.observeTasks(dateKey).first()

        assertEquals(listOf("A workout"), firstTasks.map(HomeTask::title))
        assertEquals(listOf("B workout"), secondTasks.map(HomeTask::title))
    }

    @Test
    fun `turning on workout reminder schedules alarm with workout payload`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dateKey = LocalDate.now().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)
        val dao = FakeHomeTaskDao(
            initialTasks = listOf(
                task(
                    id = 7,
                    ownerId = "user-a",
                    title = "Workout session",
                    description = "Use today's workout focus as your main session.",
                    dateKey = dateKey,
                    reminderEnabled = false
                )
            )
        )
        val repository = RoomHomeTaskRepository(
            taskDao = dao,
            sessionRepository = SwitchingSessionRepository("user-a"),
            reminderScheduler = TaskReminderScheduler(context)
        )

        repository.updateTaskReminder(taskId = 7, enabled = true)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadowAlarmManager = Shadow.extract<ShadowAlarmManager>(alarmManager)
        val alarm = requireNotNull(shadowAlarmManager.peekNextScheduledAlarm())
        assertNotNull(alarm)
        assertEquals(AlarmManager.RTC_WAKEUP, alarm.type)
        assertTrue(alarm.isAllowWhileIdle)

        val shadowPendingIntent = Shadow.extract<ShadowPendingIntent>(alarm.operation)
        val intent = shadowPendingIntent.savedIntent
        assertEquals(TaskReminderReceiver::class.java.name, intent.component?.className)
        assertEquals(7L, intent.getLongExtra(TaskReminderReceiver.EXTRA_TASK_ID, 0L))
        assertEquals("Workout session", intent.getStringExtra(TaskReminderReceiver.EXTRA_TASK_TITLE))
        assertEquals(
            "Use today's workout focus as your main session.",
            intent.getStringExtra(TaskReminderReceiver.EXTRA_TASK_MESSAGE)
        )
    }

    @Test
    fun `turning off workout reminder cancels scheduled alarm`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dateKey = LocalDate.now().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)
        val dao = FakeHomeTaskDao(
            initialTasks = listOf(
                task(
                    id = 9,
                    ownerId = "user-a",
                    title = "Workout session",
                    dateKey = dateKey,
                    reminderEnabled = false
                )
            )
        )
        val repository = RoomHomeTaskRepository(
            taskDao = dao,
            sessionRepository = SwitchingSessionRepository("user-a"),
            reminderScheduler = TaskReminderScheduler(context)
        )

        repository.updateTaskReminder(taskId = 9, enabled = true)
        repository.updateTaskReminder(taskId = 9, enabled = false)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadowAlarmManager = Shadow.extract<ShadowAlarmManager>(alarmManager)
        assertTrue(shadowAlarmManager.scheduledAlarms.isEmpty())
    }

    private fun task(
        id: Long,
        ownerId: String,
        title: String,
        dateKey: String,
        description: String = "",
        reminderEnabled: Boolean = false
    ): HomeTaskEntity {
        return HomeTaskEntity(
            id = id,
            ownerId = ownerId,
            title = title,
            description = description,
            dateKey = dateKey,
            timeMinutes = 480,
            category = HomeTaskCategory.Workout.name,
            status = HomeTaskStatus.Todo.name,
            reminderEnabled = reminderEnabled,
            isDefault = false,
            createdAt = 1L,
            updatedAt = 1L
        )
    }
}

private class FakeHomeTaskDao(
    initialTasks: List<HomeTaskEntity> = emptyList()
) : HomeTaskDao {
    private val tasks = MutableStateFlow(initialTasks)
    private val seededDays = mutableSetOf<Pair<String, String>>()

    override fun observeTasks(ownerId: String, dateKey: String): Flow<List<HomeTaskEntity>> {
        return tasks.map { current ->
            current.filter { it.ownerId == ownerId && it.dateKey == dateKey }
                .sortedWith(compareBy(HomeTaskEntity::timeMinutes, HomeTaskEntity::createdAt))
        }
    }

    override suspend fun getTaskById(taskId: Long): HomeTaskEntity? = tasks.value.firstOrNull { it.id == taskId }

    override suspend fun getTasksForDate(ownerId: String, dateKey: String): List<HomeTaskEntity> {
        return tasks.value.filter { it.ownerId == ownerId && it.dateKey == dateKey }
    }

    override suspend fun getActiveReminderTasks(): List<HomeTaskEntity> {
        return tasks.value.filter { it.reminderEnabled && it.status != HomeTaskStatus.Completed.name }
    }

    override suspend fun hasSeed(ownerId: String, dateKey: String): Boolean = (ownerId to dateKey) in seededDays

    override suspend fun insertTask(task: HomeTaskEntity): Long {
        tasks.value = tasks.value + task
        return task.id
    }

    override suspend fun insertTasks(tasks: List<HomeTaskEntity>) {
        this.tasks.value = this.tasks.value + tasks
    }

    override suspend fun insertSeed(seed: HomeTaskDaySeedEntity) {
        seededDays += seed.ownerId to seed.dateKey
    }

    override suspend fun updateTask(task: HomeTaskEntity) {
        tasks.value = tasks.value.map { current -> if (current.id == task.id) task else current }
    }

    override suspend fun deleteTask(taskId: Long) {
        tasks.value = tasks.value.filterNot { it.id == taskId }
    }
}

private class SwitchingSessionRepository(
    initialUserId: String?
) : SessionRepository {
    private val currentUserIdFlow = MutableStateFlow(initialUserId)

    fun setCurrentUserId(userId: String?) {
        currentUserIdFlow.value = userId
    }

    override suspend fun saveStartupState(state: FittyStartupState) = Unit
    override suspend fun saveUserSession(user: FittyUser) = Unit
    override suspend fun setOnboardingCompleted(completed: Boolean) = Unit
    override suspend fun getCurrentUserId(): String? = currentUserIdFlow.value
    override fun observeCurrentUserId(): Flow<String?> = currentUserIdFlow
    override suspend fun getAppLanguage(): String? = "en"
    override suspend fun setAppLanguage(language: String) = Unit
    override suspend fun clearSession() = Unit
    override suspend fun isSignedInSessionExpired(nowMillis: Long, maxAgeMillis: Long): Boolean = false
    override suspend fun shouldShowWelcomeNotification(nowMillis: Long, cooldownMillis: Long): Boolean = false
    override suspend fun setLastWelcomeNotificationAt(timestampMillis: Long) = Unit
}
