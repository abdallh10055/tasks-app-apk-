package com.example.ui.screens

import android.Manifest
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.HabitViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: HabitViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val reminderHour by viewModel.reminderHour.collectAsStateWithLifecycle()
    val reminderMinute by viewModel.reminderMinute.collectAsStateWithLifecycle()
    val reminderEnabled by viewModel.reminderEnabled.collectAsStateWithLifecycle()
    val floatingOverlayEnabled by viewModel.floatingOverlayEnabled.collectAsStateWithLifecycle()
    val todayDay by viewModel.todayDayNumber.collectAsStateWithLifecycle()
    val completions by viewModel.allCompletions.collectAsStateWithLifecycle()
    val streakCount by viewModel.streakCount.collectAsStateWithLifecycle()

    var showResetDialog by remember { mutableStateOf(false) }
    var showApkGuideDialog by remember { mutableStateOf(false) }

    // Permission launcher for Android 13+ Notification permission
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.toggleReminderEnabled(true)
            Toast.makeText(context, "Notification permission granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Notification permission is required for daily reminders", Toast.LENGTH_LONG).show()
        }
    }

    val timePickerDialog = remember(reminderHour, reminderMinute) {
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                viewModel.updateReminderTime(hourOfDay, minute)
                Toast.makeText(
                    context,
                    "Daily reminder set to %02d:%02d".format(Locale.getDefault(), hourOfDay, minute),
                    Toast.LENGTH_SHORT
                ).show()
            },
            reminderHour,
            reminderMinute,
            false // 12-hour AM/PM format
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ninja Settings", fontWeight = FontWeight.Bold) }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Stats Overview Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("stats_overview_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "STATS & PROGRESS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatItem(
                            label = "Current Day",
                            value = "Day $todayDay",
                            modifier = Modifier.weight(1f)
                        )
                        StatItem(
                            label = "Completed",
                            value = "${completions.size} / $todayDay",
                            modifier = Modifier.weight(1f)
                        )
                        StatItem(
                            label = "Streak",
                            value = "$streakCount Days",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Notifications & Reminders",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Daily Reminder Toggle
            SettingsToggleCard(
                title = "Daily Push Notification",
                subtitle = "Receive today's nutrition tip & stealth workout",
                icon = Icons.Default.Notifications,
                isChecked = reminderEnabled,
                onCheckedChange = { checked ->
                    if (checked) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            viewModel.toggleReminderEnabled(true)
                        }
                    } else {
                        viewModel.toggleReminderEnabled(false)
                    }
                },
                testTag = "toggle_daily_notification"
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Notification Time Picker
            SettingsActionCard(
                title = "Preferred Reminder Time",
                subtitle = formatTime(reminderHour, reminderMinute),
                icon = Icons.Default.AccessTime,
                actionLabel = "Change",
                onClick = { timePickerDialog.show() },
                testTag = "change_reminder_time_button"
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Test Notification Now
            Button(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    viewModel.triggerTestNotification()
                    Toast.makeText(context, "Test notification dispatched!", Toast.LENGTH_SHORT).show()
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("test_notification_button")
            ) {
                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Test Daily Notification Now")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Stealth Floating Overlay",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Floating Window Overlay
            SettingsToggleCard(
                title = "Floating Stealth Widget",
                subtitle = "Display a draggable mini workout card over other apps",
                icon = Icons.Default.Layers,
                isChecked = floatingOverlayEnabled,
                onCheckedChange = { checked ->
                    if (checked) {
                        if (!Settings.canDrawOverlays(context)) {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                            Toast.makeText(
                                context,
                                "Please permit drawing over other apps to enable floating widget",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            viewModel.toggleFloatingOverlay(true)
                        }
                    } else {
                        viewModel.toggleFloatingOverlay(false)
                    }
                },
                testTag = "toggle_floating_overlay"
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Habit Journey & Data",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Reset start date to today
            SettingsActionCard(
                title = "Reset Start Date to Today",
                subtitle = "Resets Day calculation to Day 1 starting today",
                icon = Icons.Default.Today,
                actionLabel = "Reset",
                onClick = {
                    viewModel.resetStartDateToToday()
                    Toast.makeText(context, "Start date reset to today (Day 1)", Toast.LENGTH_SHORT).show()
                },
                testTag = "reset_start_date_button"
            )

            Spacer(modifier = Modifier.height(10.dp))

            // APK Packaging & Standalone Instructions Modal
            SettingsActionCard(
                title = "Standalone APK Build Info",
                subtitle = "Compilation commands and export guidance",
                icon = Icons.Default.Android,
                actionLabel = "View",
                onClick = { showApkGuideDialog = true },
                testTag = "apk_guide_button"
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Reset All Data
            OutlinedButton(
                onClick = { showResetDialog = true },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("clear_data_button")
            ) {
                Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Clear Completed Habits History")
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Habit History?") },
            text = { Text("This will clear all marked completed days and reset your start date to Day 1. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllData()
                        showResetDialog = false
                        Toast.makeText(context, "All history cleared", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Clear All", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showApkGuideDialog) {
        AlertDialog(
            onDismissRequest = { showApkGuideDialog = false },
            title = { Text("Standalone APK Compilation") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "1. AI Studio Direct Export:\n" +
                                "In the top-right Settings menu, choose 'Export Project (ZIP)' or 'Generate APK'.\n\n" +
                                "2. Local Terminal Build:\n" +
                                "Extract project ZIP and run:\n" +
                                "• Linux / macOS:\n   ./gradlew assembleRelease\n" +
                                "• Windows:\n   gradlew.bat assembleRelease\n\n" +
                                "The output APK will be located in:\n" +
                                "app/build/outputs/apk/release/app-release-unsigned.apk\n\n" +
                                "3. Fully Offline & Self-Contained:\n" +
                                "• All 1000 tips are bundled inside assets/tips_data.json\n" +
                                "• Offline SQLite storage via Room\n" +
                                "• Works 100% without internet access.",
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showApkGuideDialog = false }) {
                    Text("Got It")
                }
            }
        )
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingsToggleCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = isChecked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.testTag("${testTag}_switch")
            )
        }
    }
}

@Composable
private fun SettingsActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    actionLabel: String,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FilledTonalButton(
                onClick = onClick,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(actionLabel)
            }
        }
    }
}

private fun formatTime(hour: Int, minute: Int): String {
    val amPm = if (hour >= 12) "PM" else "AM"
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return "%d:%02d %s".format(Locale.getDefault(), displayHour, minute, amPm)
}
