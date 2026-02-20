package com.example.llmui.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class ExerciseUsage(
    val exerciseId: Long,
    val count: Int,
    val totalDurationSeconds: Int
)

@Dao
interface ExerciseSessionDao {

    @Query("SELECT * FROM exercise_sessions ORDER BY startTimeMillis DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<ExerciseSessionEntity>>

    @Insert
    suspend fun insert(session: ExerciseSessionEntity)

    @Query(
        """
        SELECT exerciseId AS exerciseId,
               COUNT(*) AS count,
               SUM(durationSeconds) AS totalDurationSeconds
        FROM exercise_sessions
        WHERE exerciseId IS NOT NULL
        GROUP BY exerciseId
        """
    )
    fun getExerciseUsage(): Flow<List<ExerciseUsage>>
}
