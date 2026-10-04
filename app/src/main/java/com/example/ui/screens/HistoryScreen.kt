package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TipItem
import com.example.ui.components.HabitFocusCard
import com.example.ui.components.NutritionCard
import com.example.ui.components.StealthExerciseCard
import com.example.ui.viewmodel.HabitViewModel
import com.example.ui.viewmodel.HistoryFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HabitViewModel,
    onNavigateToHomeDay: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val todayDay by viewModel.todayDayNumber.collectAsStateWithLifecycle()
    val completions by viewModel.allCompletions.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filter by viewModel.historyFilter.collectAsStateWithLifecycle()

    var showCodexMode by remember { mutableStateOf(false) } // False = unlocked up to today; True = all 1000 tips
    var selectedDetailTip by remember { mutableStateOf<TipItem?>(null) }

    val completedDaySet = remember(completions) {
        completions.map { it.dayNumber }.toSet()
    }

    // Filter tips based on search, completion state, and codex vs unlocked
    val maxDayAvailable = if (showCodexMode) 1000 else todayDay

    val displayedTips = remember(searchQuery, filter, maxDayAvailable, completedDaySet) {
        (1..maxDayAvailable).mapNotNull { day ->
            val tip = viewModel.getTipForDay(day)
            val isDone = completedDaySet.contains(day)

            // Filter match
            val matchesFilter = when (filter) {
                HistoryFilter.ALL -> true
                HistoryFilter.COMPLETED_ONLY -> isDone
                HistoryFilter.INCOMPLETE_ONLY -> !isDone
            }

            // Search query match
            val matchesSearch = if (searchQuery.isBlank()) {
                true
            } else {
                tip.nutritionTip.contains(searchQuery, ignoreCase = true) ||
                        tip.stealthExercise.contains(searchQuery, ignoreCase = true) ||
                        tip.habitTrackerFocus.contains(searchQuery, ignoreCase = true) ||
                        "Day $day".contains(searchQuery, ignoreCase = true)
            }

            if (matchesFilter && matchesSearch) tip to isDone else null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (showCodexMode) "Ninja Codex (1-1000)" else "Habit Journey Log",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    TextButton(
                        onClick = { showCodexMode = !showCodexMode },
                        modifier = Modifier.testTag("toggle_codex_button")
                    ) {
                        Text(
                            text = if (showCodexMode) "Show Journey" else "View All 1000",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search by topic, e.g. hydration, posture, wrist...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("history_search_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filter == HistoryFilter.ALL,
                    onClick = { viewModel.setHistoryFilter(HistoryFilter.ALL) },
                    label = { Text("All (${displayedTips.size})") },
                    modifier = Modifier.testTag("filter_all")
                )
                FilterChip(
                    selected = filter == HistoryFilter.COMPLETED_ONLY,
                    onClick = { viewModel.setHistoryFilter(HistoryFilter.COMPLETED_ONLY) },
                    label = { Text("Done (${completedDaySet.count { it <= maxDayAvailable }})") },
                    modifier = Modifier.testTag("filter_completed")
                )
                FilterChip(
                    selected = filter == HistoryFilter.INCOMPLETE_ONLY,
                    onClick = { viewModel.setHistoryFilter(HistoryFilter.INCOMPLETE_ONLY) },
                    label = { Text("Pending") },
                    modifier = Modifier.testTag("filter_incomplete")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (displayedTips.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No matching tips found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Try clearing your search query or switching filters",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("history_list"),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(
                        items = displayedTips,
                        key = { it.first.id }
                    ) { (tip, isDone) ->
                        HistoryTipCard(
                            day = tip.id,
                            tip = tip,
                            isDone = isDone,
                            isFuture = tip.id > todayDay,
                            onCardClick = { selectedDetailTip = tip },
                            onToggleComplete = { viewModel.toggleDayCompletion(tip.id) }
                        )
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet for inspecting detail and jumping to home
    if (selectedDetailTip != null) {
        val detailTip = selectedDetailTip!!
        val isDone = completedDaySet.contains(detailTip.id)

        ModalBottomSheet(
            onDismissRequest = { selectedDetailTip = null },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "DAY ${detailTip.id}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (detailTip.id == todayDay) "Today's Tip" else if (detailTip.id < todayDay) "Past Day" else "Future Codex Item",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    FilledTonalButton(
                        onClick = {
                            viewModel.toggleDayCompletion(detailTip.id)
                        }
                    ) {
                        Text(if (isDone) "Mark Incomplete" else "Mark Done ✓")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                NutritionCard(title = detailTip.nutritionTitle, body = detailTip.nutritionBody)

                Spacer(modifier = Modifier.height(12.dp))

                StealthExerciseCard(
                    title = detailTip.exerciseTitle,
                    body = detailTip.exerciseBody,
                    onStartTimer = {
                        selectedDetailTip = null
                        onNavigateToHomeDay(detailTip.id)
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                HabitFocusCard(title = detailTip.habitTitle, body = detailTip.habitBody)

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        selectedDetailTip = null
                        onNavigateToHomeDay(detailTip.id)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("View on Today Dashboard")
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun HistoryTipCard(
    day: Int,
    tip: TipItem,
    isDone: Boolean,
    isFuture: Boolean,
    onCardClick: () -> Unit,
    onToggleComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick)
            .testTag("history_item_card_$day"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDone) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Day Badge
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "$day",
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = if (isDone) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = tip.exerciseTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (isFuture) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Upcoming",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier
                                .size(14.dp)
                                .padding(start = 4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${tip.nutritionTitle} • ${tip.habitTitle}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onToggleComplete,
                modifier = Modifier.testTag("toggle_item_$day")
            ) {
                Icon(
                    imageVector = if (isDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = if (isDone) "Completed" else "Incomplete",
                    tint = if (isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
