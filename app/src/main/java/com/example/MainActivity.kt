package com.example

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.HabitViewModel

enum class ScreenTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    TODAY("Today", Icons.Filled.Today, Icons.Outlined.Today, "tab_today"),
    CODEX("Codex", Icons.Filled.MenuBook, Icons.Outlined.MenuBook, "tab_codex"),
    SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings, "tab_settings")
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val extraDay = intent?.getIntExtra("EXTRA_DAY_NUMBER", -1) ?: -1

        setContent {
            MyApplicationTheme {
                val viewModel: HabitViewModel = viewModel()

                LaunchedEffect(extraDay) {
                    if (extraDay > 0) {
                        viewModel.selectDay(extraDay)
                    }
                }

                // Android 13+ Notification permission request on launch
                val reminderEnabled by viewModel.reminderEnabled.collectAsStateWithLifecycle()
                val requestPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { _ -> }

                LaunchedEffect(Unit) {
                    if (reminderEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                NinjaMainScreen(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val extraDay = intent.getIntExtra("EXTRA_DAY_NUMBER", -1)
        if (extraDay > 0) {
            // Updated via intent
        }
    }
}

@Composable
fun NinjaMainScreen(
    viewModel: HabitViewModel,
    modifier: Modifier = Modifier
) {
    var currentTab by remember { mutableStateOf(ScreenTab.TODAY) }

    // If on a secondary tab, Back button returns to Today tab
    BackHandler(enabled = currentTab != ScreenTab.TODAY) {
        currentTab = ScreenTab.TODAY
    }

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars,
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("main_bottom_nav"),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                ScreenTab.values().forEach { tab ->
                    val selected = currentTab == tab
                    NavigationBarItem(
                        selected = selected,
                        onClick = { currentTab = tab },
                        icon = {
                            Icon(
                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.title
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier.testTag(tab.testTag)
                    )
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        when (currentTab) {
            ScreenTab.TODAY -> {
                HomeScreen(
                    viewModel = viewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            ScreenTab.CODEX -> {
                HistoryScreen(
                    viewModel = viewModel,
                    onNavigateToHomeDay = { day ->
                        viewModel.selectDay(day)
                        currentTab = ScreenTab.TODAY
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            ScreenTab.SETTINGS -> {
                SettingsScreen(
                    viewModel = viewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}
