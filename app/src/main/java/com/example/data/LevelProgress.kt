package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "level_progress",
    indices = [
        Index(value = ["completed"]),
        Index(value = ["stars"]),
        Index(value = ["highScore"])
    ]
)
data class LevelProgress(
    @PrimaryKey val levelId: Int,
    val stars: Int = 0,
    val highScore: Int = 0,
    val completed: Boolean = false
)
