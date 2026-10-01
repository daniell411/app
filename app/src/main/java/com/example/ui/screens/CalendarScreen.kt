package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ReminderCard
import com.example.ui.theme.ApuntaIndigo
import com.example.ui.theme.parseHexColor
import com.example.ui.viewmodel.ApuntaViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun CalendarScreen(
    viewModel: ApuntaViewModel,
    onOpenNotebook: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val reminders by viewModel.todayReminders.collectAsState()
    val allNotebooks by viewModel.allNotebooks.collectAsState()
    val selectedDateMillis by viewModel.selectedCalendarDate.collectAsState()

    var viewMode by remember { mutableIntStateOf(0) } // 0: Semanal, 1: Mensual
    var currentMonthOffset by remember { mutableIntStateOf(0) }

    val calendar = remember(selectedDateMillis) {
        Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
    }

    val selectedYear = calendar.get(Calendar.YEAR)
    val selectedMonth = calendar.get(Calendar.MONTH)
    val selectedDay = calendar.get(Calendar.DAY_OF_MONTH)

    val monthFormatter = remember { SimpleDateFormat("MMMM yyyy", Locale("es", "ES")) }
    val currentMonthTitle = remember(currentMonthOffset) {
        val cal = Calendar.getInstance().apply { add(Calendar.MONTH, currentMonthOffset) }
        monthFormatter.format(cal.time).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
    }

    // Reminders for selected date
    val dayReminders = remember(reminders, selectedYear, selectedMonth, selectedDay) {
        reminders.filter { item ->
            val remCal = Calendar.getInstance().apply { timeInMillis = item.reminder.datetime }
            remCal.get(Calendar.YEAR) == selectedYear &&
                    remCal.get(Calendar.MONTH) == selectedMonth &&
                    remCal.get(Calendar.DAY_OF_MONTH) == selectedDay
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("calendar_screen")
    ) {
        // Top header & Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Calendario",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = currentMonthTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Month navigation arrows
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { currentMonthOffset-- }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Mes anterior")
                }
                IconButton(onClick = { currentMonthOffset++ }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Mes siguiente")
                }
            }
        }

        // View toggle: Semanal vs Mensual
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            TabRow(
                selectedTabIndex = viewMode,
                containerColor = Color.Transparent,
                indicator = {},
                divider = {}
            ) {
                listOf("Semanal", "Mensual").forEachIndexed { index, text ->
                    val isSelected = viewMode == index
                    Tab(
                        selected = isSelected,
                        onClick = { viewMode = index },
                        modifier = Modifier
                            .padding(4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) ApuntaIndigo else Color.Transparent)
                    ) {
                        Text(
                            text = text,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(vertical = 8.dp),
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Days Header (L, M, X, J, V, S, D)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            listOf("L", "M", "M", "J", "V", "S", "D").forEach { dayLetter ->
                Text(
                    text = dayLetter,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(36.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Calendar Grid View (Weekly or Monthly)
        AnimatedContent(targetState = viewMode, label = "calendar_view") { mode ->
            if (mode == 0) {
                // Weekly carousel
                WeeklyView(
                    selectedDateMillis = selectedDateMillis,
                    reminders = reminders,
                    onSelectDate = { viewModel.setSelectedCalendarDate(it) }
                )
            } else {
                // Monthly grid
                MonthlyView(
                    currentMonthOffset = currentMonthOffset,
                    selectedDateMillis = selectedDateMillis,
                    reminders = reminders,
                    onSelectDate = { viewModel.setSelectedCalendarDate(it) }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Selected Day Reminders Header
        val fullDateFormat = remember { SimpleDateFormat("EEEE, d 'de' MMMM", Locale("es", "ES")) }
        val dateLabel = remember(selectedDateMillis) {
            fullDateFormat.format(Date(selectedDateMillis)).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = dateLabel,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "${dayReminders.size} cosas",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Day Reminders List
        if (dayReminders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No hay recordatorios para este día.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { viewModel.openListeningSheet() },
                        colors = ButtonDefaults.buttonColors(containerColor = ApuntaIndigo),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Apuntar para esta fecha")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(dayReminders, key = { it.reminder.id }) { item ->
                    ReminderCard(
                        item = item,
                        onOpenNotebook = {
                            item.reminder.notebookId?.let { onOpenNotebook(it) }
                        },
                        onToggleDone = { viewModel.toggleReminderDone(item.reminder.id) },
                        onSnooze = { viewModel.snoozeReminder(item.reminder.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun WeeklyView(
    selectedDateMillis: Long,
    reminders: List<com.example.data.model.ReminderWithNotebook>,
    onSelectDate: (Long) -> Unit
) {
    val cal = Calendar.getInstance().apply {
        timeInMillis = selectedDateMillis
        firstDayOfWeek = Calendar.MONDAY
        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        for (i in 0..6) {
            val dateCal = cal.clone() as Calendar
            dateCal.add(Calendar.DAY_OF_YEAR, i)
            val dayMillis = dateCal.timeInMillis
            val dayNum = dateCal.get(Calendar.DAY_OF_MONTH)

            val selCal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
            val isSelected = dateCal.get(Calendar.YEAR) == selCal.get(Calendar.YEAR) &&
                    dateCal.get(Calendar.DAY_OF_YEAR) == selCal.get(Calendar.DAY_OF_YEAR)

            val dayDots = reminders.filter {
                val remCal = Calendar.getInstance().apply { timeInMillis = it.reminder.datetime }
                remCal.get(Calendar.YEAR) == dateCal.get(Calendar.YEAR) &&
                        remCal.get(Calendar.DAY_OF_YEAR) == dateCal.get(Calendar.DAY_OF_YEAR)
            }.mapNotNull { it.notebook?.color }

            DayCell(
                dayNumber = dayNum,
                isSelected = isSelected,
                dotColors = dayDots,
                onClick = { onSelectDate(dayMillis) }
            )
        }
    }
}

@Composable
private fun MonthlyView(
    currentMonthOffset: Int,
    selectedDateMillis: Long,
    reminders: List<com.example.data.model.ReminderWithNotebook>,
    onSelectDate: (Long) -> Unit
) {
    val cal = Calendar.getInstance().apply {
        add(Calendar.MONTH, currentMonthOffset)
        set(Calendar.DAY_OF_MONTH, 1)
        firstDayOfWeek = Calendar.MONDAY
    }

    val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = (cal.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        var dayCounter = 1
        for (row in 0..5) {
            if (dayCounter > maxDays) break
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                for (col in 0..6) {
                    if (row == 0 && col < firstDayOfWeek) {
                        Spacer(modifier = Modifier.width(36.dp))
                    } else if (dayCounter <= maxDays) {
                        val cellCal = cal.clone() as Calendar
                        cellCal.set(Calendar.DAY_OF_MONTH, dayCounter)
                        val cellMillis = cellCal.timeInMillis

                        val selCal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
                        val isSelected = cellCal.get(Calendar.YEAR) == selCal.get(Calendar.YEAR) &&
                                cellCal.get(Calendar.MONTH) == selCal.get(Calendar.MONTH) &&
                                cellCal.get(Calendar.DAY_OF_MONTH) == selCal.get(Calendar.DAY_OF_MONTH)

                        val dayDots = reminders.filter {
                            val remCal = Calendar.getInstance().apply { timeInMillis = it.reminder.datetime }
                            remCal.get(Calendar.YEAR) == cellCal.get(Calendar.YEAR) &&
                                    remCal.get(Calendar.DAY_OF_YEAR) == cellCal.get(Calendar.DAY_OF_YEAR)
                        }.mapNotNull { it.notebook?.color }

                        DayCell(
                            dayNumber = dayCounter,
                            isSelected = isSelected,
                            dotColors = dayDots,
                            onClick = { onSelectDate(cellMillis) }
                        )
                        dayCounter++
                    } else {
                        Spacer(modifier = Modifier.width(36.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    dayNumber: Int,
    isSelected: Boolean,
    dotColors: List<String>,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) ApuntaIndigo else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$dayNumber",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(3.dp))

        // Dots representing reminders with notebook colors
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.height(6.dp)
        ) {
            val displayDots = dotColors.take(3)
            if (displayDots.isEmpty()) {
                Spacer(modifier = Modifier.size(5.dp))
            } else {
                displayDots.forEach { colorHex ->
                    val color = parseHexColor(colorHex)
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) Color.White else color)
                    )
                }
            }
        }
    }
}
