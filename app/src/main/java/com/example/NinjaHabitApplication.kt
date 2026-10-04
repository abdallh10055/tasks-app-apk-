package com.example

import android.app.Application
import com.example.data.repository.HabitRepository
import com.example.notification.AlarmScheduler
import com.example.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class NinjaHabitApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannel(this)

        val repository = HabitRepository.getInstance(this)
        CoroutineScope(Dispatchers.IO).launch {
            val enabled = repository.reminderEnabled.first()
            if (enabled) {
                val hour = repository.reminderHour.first()
                val minute = repository.reminderMinute.first()
                AlarmScheduler.scheduleDailyReminder(this@NinjaHabitApplication, hour, minute)
            }
        }
    }
}
