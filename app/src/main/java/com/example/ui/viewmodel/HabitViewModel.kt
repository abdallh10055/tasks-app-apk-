package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.HabitCompletion
import com.example.data.model.TipItem
import com.example.data.repository.HabitRepository
import com.example.notification.AlarmScheduler
import com.example.service.NinjaOverlayService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class HistoryFilter {
    ALL,
    COMPLETED_ONLY,
    INCOMPLETE_ONLY
}

class HabitViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = HabitRepository.getInstance(application)

    // Current real-life Day (1..1000) calculated from (today - start_date) % 1000
    private val _todayDayNumber = MutableStateFlow(1)
    val todayDayNumber: StateFlow<Int> = _todayDayNumber.asStateFlow()

    // The day the user is actively viewing on Home (defaults to today, can browse next/prev)
    private val _selectedDayNumber = MutableStateFlow(1)
    val selectedDayNumber: StateFlow<Int> = _selectedDayNumber.asStateFlow()

    val allCompletions: StateFlow<List<HabitCompletion>> = repository.allCompletions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val startDateEpochDays: StateFlow<Long> = repository.startDateEpochDay
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LocalDate.now().toEpochDay())

    val reminderHour: StateFlow<Int> = repository.reminderHour
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 9)

    val reminderMinute: StateFlow<Int> = repository.reminderMinute
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val reminderEnabled: StateFlow<Boolean> = repository.reminderEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val floatingOverlayEnabled: StateFlow<Boolean> = repository.floatingOverlayEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Search and filters for history/codex
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _historyFilter = MutableStateFlow(HistoryFilter.ALL)
    val historyFilter: StateFlow<HistoryFilter> = _historyFilter.asStateFlow()

    init {
        refreshCurrentDay()
    }

    fun refreshCurrentDay() {
        viewModelScope.launch {
            val todayEpoch = LocalDate.now().toEpochDay()
            val startEpoch = repository.getStartDateEpochDaySync()
            val day = repository.calculateDayNumber(todayEpoch, startEpoch)
            _todayDayNumber.value = day
            _selectedDayNumber.value = day
        }
    }

    fun selectDay(dayNumber: Int) {
        val safeDay = dayNumber.coerceIn(1, repository.totalTips)
        _selectedDayNumber.value = safeDay
    }

    fun selectNextDay() {
        if (_selectedDayNumber.value < repository.totalTips) {
            _selectedDayNumber.value += 1
        }
    }

    fun selectPreviousDay() {
        if (_selectedDayNumber.value > 1) {
            _selectedDayNumber.value -= 1
        }
    }

    fun selectToday() {
        _selectedDayNumber.value = _todayDayNumber.value
    }

    fun getTipForDay(dayNumber: Int): TipItem {
        return repository.getTipForDay(dayNumber)
    }

    fun isDayCompleted(dayNumber: Int, completions: List<HabitCompletion>): Boolean {
        return completions.any { it.dayNumber == dayNumber }
    }

    fun toggleDayCompletion(dayNumber: Int) {
        viewModelScope.launch {
            repository.toggleDayCompletion(dayNumber)
        }
    }

    // Consecutive streak calculation
    val streakCount: StateFlow<Int> = combine(todayDayNumber, allCompletions) { today, completions ->
        val completedSet = completions.map { it.dayNumber }.toSet()
        var streak = 0
        // Check backwards from today (or today-1 if today not yet checked)
        var checkDay = if (completedSet.contains(today)) today else today - 1
        while (checkDay > 0 && completedSet.contains(checkDay)) {
            streak++
            checkDay--
        }
        streak
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalCompletedCount: StateFlow<Int> = allCompletions.combine(todayDayNumber) { list, _ ->
        list.size
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setHistoryFilter(filter: HistoryFilter) {
        _historyFilter.value = filter
    }

    fun updateReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            repository.setReminderTime(hour, minute)
            if (reminderEnabled.value) {
                AlarmScheduler.scheduleDailyReminder(getApplication(), hour, minute)
            }
        }
    }

    fun toggleReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setReminderEnabled(enabled)
            if (enabled) {
                val hour = reminderHour.value
                val minute = reminderMinute.value
                AlarmScheduler.scheduleDailyReminder(getApplication(), hour, minute)
            } else {
                AlarmScheduler.cancelReminder(getApplication())
            }
        }
    }

    fun toggleFloatingOverlay(enabled: Boolean) {
        viewModelScope.launch {
            repository.setFloatingOverlayEnabled(enabled)
            if (enabled) {
                NinjaOverlayService.startOverlay(getApplication())
            } else {
                NinjaOverlayService.stopOverlay(getApplication())
            }
        }
    }

    fun resetStartDateToToday() {
        viewModelScope.launch {
            repository.resetStartDateToToday()
            refreshCurrentDay()
        }
    }

    fun setCustomStartDate(epochDays: Long) {
        viewModelScope.launch {
            repository.setStartDateEpochDay(epochDays)
            refreshCurrentDay()
        }
    }

    fun triggerTestNotification() {
        AlarmScheduler.triggerTestNotificationNow(getApplication())
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            refreshCurrentDay()
        }
    }
}
