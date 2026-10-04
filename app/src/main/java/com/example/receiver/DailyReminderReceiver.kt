package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.data.repository.HabitRepository
import com.example.notification.AlarmScheduler
import com.example.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DailyReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val repository = HabitRepository.getInstance(context)
        val scope = CoroutineScope(Dispatchers.IO)

        when (intent.action) {
            ACTION_DAILY_REMINDER -> {
                scope.launch {
                    val dayNumber = repository.getCurrentDayNumberSync()
                    val tip = repository.getTipForDay(dayNumber)
                    NotificationHelper.showDailyTipNotification(context, tip, dayNumber)

                    // Schedule next day
                    val hour = repository.reminderHour.first()
                    val minute = repository.reminderMinute.first()
                    val enabled = repository.reminderEnabled.first()
                    if (enabled) {
                        AlarmScheduler.scheduleDailyReminder(context, hour, minute)
                    }
                }
            }

            ACTION_MARK_DONE -> {
                val dayNumber = intent.getIntExtra(EXTRA_DAY_NUMBER, -1)
                if (dayNumber > 0) {
                    scope.launch {
                        repository.setDayCompletion(dayNumber, true)
                        NotificationHelper.cancelNotification(context)
                    }
                    Toast.makeText(
                        context,
                        "🥷 Day $dayNumber marked complete! Great discipline!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    companion object {
        const val ACTION_DAILY_REMINDER = "com.example.ninjahabit.ACTION_DAILY_REMINDER"
        const val ACTION_MARK_DONE = "com.example.ninjahabit.ACTION_MARK_DONE"
        const val EXTRA_DAY_NUMBER = "extra_day_number"
    }
}
