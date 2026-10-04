package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habit_completions ORDER BY dayNumber ASC")
    fun getAllCompletions(): Flow<List<HabitCompletion>>

    @Query("SELECT * FROM habit_completions ORDER BY dayNumber ASC")
    suspend fun getAllCompletionsSync(): List<HabitCompletion>

    @Query("SELECT * FROM habit_completions WHERE dayNumber = :dayNumber LIMIT 1")
    fun getCompletionForDay(dayNumber: Int): Flow<HabitCompletion?>

    @Query("SELECT * FROM habit_completions WHERE dayNumber = :dayNumber LIMIT 1")
    suspend fun getCompletionForDaySync(dayNumber: Int): HabitCompletion?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompletion(completion: HabitCompletion)

    @Query("DELETE FROM habit_completions WHERE dayNumber = :dayNumber")
    suspend fun deleteCompletionForDay(dayNumber: Int)

    @Query("DELETE FROM habit_completions")
    suspend fun deleteAllCompletions()

    @Query("SELECT * FROM user_preferences WHERE `key` = :key LIMIT 1")
    fun getPreference(key: String): Flow<UserPreference?>

    @Query("SELECT * FROM user_preferences WHERE `key` = :key LIMIT 1")
    suspend fun getPreferenceSync(key: String): UserPreference?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreference(preference: UserPreference)

    @Query("DELETE FROM user_preferences WHERE `key` = :key")
    suspend fun deletePreference(key: String)
}
