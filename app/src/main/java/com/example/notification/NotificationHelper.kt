package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.TipItem
import com.example.receiver.DailyReminderReceiver

object NotificationHelper {
    const val CHANNEL_ID = "ninja_daily_habits_channel"
    const val NOTIFICATION_ID = 1001

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Ninja Daily Habits & Workouts"
            val descriptionText = "Daily nutrition tip and stealth exercise reminders"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 200, 100, 200)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showDailyTipNotification(context: Context, tip: TipItem, dayNumber: Int) {
        createNotificationChannel(context)

        // Main app launch intent
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_DAY_NUMBER", dayNumber)
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            dayNumber,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Mark Done action intent
        val markDoneIntent = Intent(context, DailyReminderReceiver::class.java).apply {
            action = DailyReminderReceiver.ACTION_MARK_DONE
            putExtra(DailyReminderReceiver.EXTRA_DAY_NUMBER, dayNumber)
        }
        val markDonePendingIntent = PendingIntent.getBroadcast(
            context,
            dayNumber + 5000,
            markDoneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val bigText = buildString {
            append("🥗 NUTRITION:\n")
            append(tip.nutritionTip)
            append("\n\n🥋 STEALTH WORKOUT:\n")
            append(tip.stealthExercise)
            append("\n\n🎯 HABIT FOCUS:\n")
            append(tip.habitTrackerFocus)
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ninja_habit_icon_1791128794368)
            .setContentTitle("🥷 Day $dayNumber: Ninja Habit & Workout")
            .setContentText("${tip.nutritionTitle} • ${tip.exerciseTitle}")
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openPendingIntent)
            .setAutoCancel(true)
            .addAction(
                android.R.drawable.checkbox_on_background,
                "Mark Done ✓",
                markDonePendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_view,
                "Open Codex",
                openPendingIntent
            )

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun cancelNotification(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }
}
