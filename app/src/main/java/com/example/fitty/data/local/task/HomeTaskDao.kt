package com.example.fitty.data.local.task

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HomeTaskDao {
    @Query(
        """
        SELECT * FROM home_tasks
        WHERE ownerId = :ownerId AND dateKey = :dateKey
        ORDER BY timeMinutes ASC, createdAt ASC
        """
    )
    fun observeTasks(ownerId: String, dateKey: String): Flow<List<HomeTaskEntity>>

    @Query("SELECT * FROM home_tasks WHERE id = :taskId LIMIT 1")
    suspend fun getTaskById(taskId: Long): HomeTaskEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM home_task_day_seed WHERE ownerId = :ownerId AND dateKey = :dateKey)")
    suspend fun hasSeed(ownerId: String, dateKey: String): Boolean

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTask(task: HomeTaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTasks(tasks: List<HomeTaskEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSeed(seed: HomeTaskDaySeedEntity)

    @Update
    suspend fun updateTask(task: HomeTaskEntity)

    @Query("DELETE FROM home_tasks WHERE id = :taskId")
    suspend fun deleteTask(taskId: Long)

    @Transaction
    suspend fun seedTasksIfNeeded(
        ownerId: String,
        dateKey: String,
        tasks: List<HomeTaskEntity>,
        nowMillis: Long
    ) {
        if (hasSeed(ownerId, dateKey)) return
        insertTasks(tasks)
        insertSeed(HomeTaskDaySeedEntity(ownerId = ownerId, dateKey = dateKey, seededAt = nowMillis))
    }
}
