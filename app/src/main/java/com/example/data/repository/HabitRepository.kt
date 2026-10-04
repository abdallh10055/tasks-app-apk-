package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.HabitCompletion
import com.example.data.local.HabitDao
import com.example.data.local.UserPreference
import com.example.data.model.TipItem
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class HabitRepository(
    private val context: Context,
    private val habitDao: HabitDao = AppDatabase.getDatabase(context).habitDao()
) {
    private val repositoryScope = CoroutineScope(Dispatchers.IO)
    private var cachedTips: List<TipItem> = emptyList()

    init {
        loadTipsFromAssets()
    }

    private fun loadTipsFromAssets() {
        try {
            val jsonString = context.assets.open("tips_data.json").bufferedReader().use { it.readText() }
            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
            val listType = Types.newParameterizedType(List::class.java, TipItem::class.java)
            val adapter = moshi.adapter<List<TipItem>>(listType)
            cachedTips = adapter.fromJson(jsonString) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback emergency tip if asset reading had issues
            cachedTips = listOf(
                TipItem(
                    id = 1,
                    nutritionTip = "[Hydration & Focus] Drink 500ml room temp water with lemon immediately upon waking to clear neural brain fog.",
                    stealthExercise = "[Seated Core Vacuum] Exhale completely and pull your navel into your spine for 15s. 4 reps.",
                    habitTrackerFocus = "[20-20-20 Vision Defense] Every 20 minutes look at an object 20 feet away for 20 seconds."
                )
            )
        }
    }

    val totalTips: Int
        get() = if (cachedTips.isNotEmpty()) cachedTips.size else 1000

    fun getTipForDay(dayNumber: Int): TipItem {
        if (cachedTips.isEmpty()) {
            loadTipsFromAssets()
        }
        val safeIndex = ((dayNumber - 1).coerceAtLeast(0)) % cachedTips.size
        return cachedTips.getOrElse(safeIndex) {
            TipItem(
                id = dayNumber,
                nutritionTip = "[Stealth Nutrition] Keep hydrated and prioritize fresh whole foods.",
                stealthExercise = "[Desk Posture] Roll shoulders back and engage your deep core.",
                habitTrackerFocus = "[Mindful Code] Take purposeful micro-breaks between tasks."
            )
        }
    }

    fun getAllTips(): List<TipItem> = cachedTips

    val allCompletions: Flow<List<HabitCompletion>> = habitDao.getAllCompletions()

    suspend fun getAllCompletionsSync(): List<HabitCompletion> = withContext(Dispatchers.IO) {
        habitDao.getAllCompletionsSync()
    }

    fun isDayCompleted(dayNumber: Int): Flow<Boolean> {
        return habitDao.getCompletionForDay(dayNumber).map { it != null }
    }

    suspend fun toggleDayCompletion(dayNumber: Int) = withContext(Dispatchers.IO) {
        val existing = habitDao.getCompletionForDaySync(dayNumber)
        if (existing != null) {
            habitDao.deleteCompletionForDay(dayNumber)
        } else {
            val dateStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            habitDao.insertCompletion(
                HabitCompletion(
                    dayNumber = dayNumber,
                    dateString = dateStr,
                    completedTimestamp = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun setDayCompletion(dayNumber: Int, completed: Boolean) = withContext(Dispatchers.IO) {
        if (completed) {
            val dateStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            habitDao.insertCompletion(
                HabitCompletion(
                    dayNumber = dayNumber,
                    dateString = dateStr,
                    completedTimestamp = System.currentTimeMillis()
                )
            )
        } else {
            habitDao.deleteCompletionForDay(dayNumber)
        }
    }

    // Start Date management
    val startDateEpochDay: Flow<Long> = habitDao.getPreference(KEY_START_DATE).map { pref ->
        pref?.value?.toLongOrNull() ?: LocalDate.now().toEpochDay()
    }

    suspend fun getStartDateEpochDaySync(): Long = withContext(Dispatchers.IO) {
        habitDao.getPreferenceSync(KEY_START_DATE)?.value?.toLongOrNull() ?: run {
            val today = LocalDate.now().toEpochDay()
            habitDao.insertPreference(UserPreference(KEY_START_DATE, today.toString()))
            today
        }
    }

    suspend fun setStartDateEpochDay(epochDay: Long) = withContext(Dispatchers.IO) {
        habitDao.insertPreference(UserPreference(KEY_START_DATE, epochDay.toString()))
    }

    suspend fun resetStartDateToToday() = withContext(Dispatchers.IO) {
        val today = LocalDate.now().toEpochDay()
        habitDao.insertPreference(UserPreference(KEY_START_DATE, today.toString()))
    }

    fun calculateDayNumber(todayEpochDay: Long, startEpochDay: Long): Int {
        val daysDiff = todayEpochDay - startEpochDay
        val safeDiff = if (daysDiff < 0) 0L else daysDiff
        return ((safeDiff % totalTips).toInt()) + 1
    }

    suspend fun getCurrentDayNumberSync(): Int {
        val today = LocalDate.now().toEpochDay()
        val start = getStartDateEpochDaySync()
        return calculateDayNumber(today, start)
    }

    // Reminder Time management
    val reminderHour: Flow<Int> = habitDao.getPreference(KEY_REMINDER_HOUR).map {
        it?.value?.toIntOrNull() ?: 9
    }

    val reminderMinute: Flow<Int> = habitDao.getPreference(KEY_REMINDER_MINUTE).map {
        it?.value?.toIntOrNull() ?: 0
    }

    val reminderEnabled: Flow<Boolean> = habitDao.getPreference(KEY_REMINDER_ENABLED).map {
        it?.value?.toBooleanStrictOrNull() ?: true
    }

    val floatingOverlayEnabled: Flow<Boolean> = habitDao.getPreference(KEY_FLOATING_OVERLAY).map {
        it?.value?.toBooleanStrictOrNull() ?: false
    }

    suspend fun setReminderTime(hour: Int, minute: Int) = withContext(Dispatchers.IO) {
        habitDao.insertPreference(UserPreference(KEY_REMINDER_HOUR, hour.toString()))
        habitDao.insertPreference(UserPreference(KEY_REMINDER_MINUTE, minute.toString()))
    }

    suspend fun setReminderEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        habitDao.insertPreference(UserPreference(KEY_REMINDER_ENABLED, enabled.toString()))
    }

    suspend fun setFloatingOverlayEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        habitDao.insertPreference(UserPreference(KEY_FLOATING_OVERLAY, enabled.toString()))
    }

    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        habitDao.deleteAllCompletions()
        resetStartDateToToday()
    }

    companion object {
        private const val KEY_START_DATE = "pref_start_date_epoch_days"
        private const val KEY_REMINDER_HOUR = "pref_reminder_hour"
        private const val KEY_REMINDER_MINUTE = "pref_reminder_minute"
        private const val KEY_REMINDER_ENABLED = "pref_reminder_enabled"
        private const val KEY_FLOATING_OVERLAY = "pref_floating_overlay_enabled"

        @Volatile
        private var INSTANCE: HabitRepository? = null

        fun getInstance(context: Context): HabitRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = HabitRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
