package com.example.llmui.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "progress")
data class ProgressEntity(
    @PrimaryKey val id: Long = 0,
    val lastExerciseId: Long?,
    val totalMinutes: Int
)
