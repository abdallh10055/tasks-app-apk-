package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.components.ExerciseTimerDialog
import com.example.ui.components.HabitFocusCard
import com.example.ui.components.HabitStatsCard
import com.example.ui.components.NutritionCard
import com.example.ui.components.StealthExerciseCard
import com.example.ui.viewmodel.HabitViewModel

@Composable
fun HomeScreen(
    viewModel: HabitViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val todayDay by viewModel.todayDayNumber.collectAsStateWithLifecycle()
    val selectedDay by viewModel.selectedDayNumber.collectAsStateWithLifecycle()
    val completions by viewModel.allCompletions.collectAsStateWithLifecycle()
    val streakCount by viewModel.streakCount.collectAsStateWithLifecycle()

    val currentTip = remember(selectedDay) {
        viewModel.getTipForDay(selectedDay)
    }

    val isCompleted = remember(selectedDay, completions) {
        viewModel.isDayCompleted(selectedDay, completions)
    }

    var showTimerDialog by remember { mutableStateOf(false) }
    var showCelebrationToast by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    if (showTimerDialog) {
        ExerciseTimerDialog(
            exerciseTitle = currentTip.exerciseTitle,
            exerciseInstructions = currentTip.exerciseBody,
            initialDurationSeconds = 30,
            onDismiss = { showTimerDialog = false },
            onCompleted = {
                showTimerDialog = false
                if (!isCompleted) {
                    viewModel.toggleDayCompletion(selectedDay)
                }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    triggerHaptic(context)
                    viewModel.toggleDayCompletion(selectedDay)
                    if (!isCompleted) {
                        showCelebrationToast = true
                    }
                },
                expanded = true,
                icon = {
                    Icon(
                        imageVector = if (isCompleted) Icons.Default.Check else Icons.Default.Celebration,
                        contentDescription = if (isCompleted) "Mark Incomplete" else "Mark Done",
                        tint = if (isCompleted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onPrimary
                    )
                },
                text = {
                    Text(
                        text = if (isCompleted) "Mission Completed ✓" else "Mark Day Done",
                        fontWeight = FontWeight.Bold
                    )
                },
                containerColor = if (isCompleted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.primary,
                contentColor = if (isCompleted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .testTag("mark_done_button")
                    .padding(bottom = 8.dp)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Hero Visual Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .testTag("home_hero_banner")
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ninja_hero_banner_1791128807059),
                    contentDescription = "Ninja Habit Hero Banner",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Gradient Overlay for contrast
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                                ),
                                startY = 60f
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                    ) {
                        Text(
                            text = "STEALTH DISCIPLINE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Ninja Habit Builder",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Habit Stats & Progress Bar
            HabitStatsCard(
                currentDay = selectedDay,
                totalDays = 1000,
                streakCount = streakCount,
                isCompleted = isCompleted,
                isToday = selectedDay == todayDay,
                onPreviousClick = { viewModel.selectPreviousDay() },
                onNextClick = { viewModel.selectNextDay() },
                onJumpTodayClick = { viewModel.selectToday() }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Nutrition Tip Card
            NutritionCard(
                title = currentTip.nutritionTitle,
                body = currentTip.nutritionBody
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Stealth Workout Card
            StealthExerciseCard(
                title = currentTip.exerciseTitle,
                body = currentTip.exerciseBody,
                onStartTimer = { showTimerDialog = true }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Habit Focus Card
            HabitFocusCard(
                title = currentTip.habitTitle,
                body = currentTip.habitBody
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Share Daily Mission Button
            OutlinedButton(
                onClick = {
                    shareDailyMission(context, selectedDay, currentTip)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("share_mission_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share Day $selectedDay Mission")
            }

            Spacer(modifier = Modifier.height(80.dp)) // Space for FAB
        }
    }
}

private fun triggerHaptic(context: Context) {
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(50)
    }
}

private fun shareDailyMission(context: Context, dayNumber: Int, tip: com.example.data.model.TipItem) {
    val sendText = """
        🥷 Ninja Habit Builder — Day $dayNumber of 1000
        
        🥗 Nutrition: ${tip.nutritionTip}
        
        🥋 Stealth Workout: ${tip.stealthExercise}
        
        🎯 Habit Focus: ${tip.habitTrackerFocus}
        
        Level up daily offline habits!
    """.trimIndent()

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Ninja Habit - Day $dayNumber")
        putExtra(Intent.EXTRA_TEXT, sendText)
    }
    context.startActivity(Intent.createChooser(intent, "Share Ninja Mission"))
}
