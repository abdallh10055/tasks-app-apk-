package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habit_completions")
data class HabitCompletion(
    @PrimaryKey val dayNumber: Int,
    val dateString: String,
    val completedTimestamp: Long,
    val nutritionCompleted: Boolean = true,
    val exerciseCompleted: Boolean = true,
    val habitCompleted: Boolean = true,
    val note: String = ""
)
