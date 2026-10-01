package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.example.ui.theme.ApuntaGreen
import com.example.ui.theme.ApuntaIndigo
import com.example.ui.theme.ApuntaOrange
import com.example.ui.viewmodel.ApuntaViewModel
import java.util.Calendar

@Composable
fun TodayScreen(
    viewModel: ApuntaViewModel,
    onOpenNotebook: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val reminders by viewModel.todayReminders.collectAsState()
    val userPreferences by viewModel.userPreferences.collectAsState()
    val voiceFeedback by viewModel.voiceFeedback.collectAsState()
    val todayFilter by viewModel.todayFilter.collectAsState()

    val userName = userPreferences.userName.ifBlank { "amigo" }

    val pendingCount = remember(reminders) {
        reminders.count { it.reminder.status != "DONE" }
    }
    val completedCount = remember(reminders) {
        reminders.count { it.reminder.status == "DONE" }
    }
    val totalCount = reminders.size

    val progress = remember(completedCount, totalCount) {
        if (totalCount == 0) 0f else completedCount.toFloat() / totalCount.toFloat()
    }

    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 5..11 -> "Buen día"
            in 12..18 -> "Buenas tardes"
            else -> "Buenas noches"
        }
    }

    // Filter reminders based on selected filter
    val filteredReminders = remember(reminders, todayFilter) {
        when (todayFilter) {
            "PENDIENTES" -> reminders.filter { it.reminder.status != "DONE" }
            "COMPLETADOS" -> reminders.filter { it.reminder.status == "DONE" }
            else -> reminders
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("today_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Dynamic greeting header
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "$greeting, $userName",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (pendingCount == 0 && totalCount > 0) "¡Día cumplido! Todo completado"
                            else if (pendingCount == 0) "Día libre. No tenés pendientes"
                            else if (pendingCount == 1) "Te queda 1 cosa hoy"
                            else "Te quedan $pendingCount cosas hoy",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (pendingCount > 0) ApuntaOrange else ApuntaGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Quick TTS listen button for morning summary
                    IconButton(
                        onClick = { viewModel.speakMorningSummary() },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(ApuntaIndigo.copy(alpha = 0.12f))
                            .testTag("listen_morning_summary_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Escuchar resumen del día",
                            tint = ApuntaIndigo,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // Daily Progress Bar (Barra de progreso del día)
        if (totalCount > 0) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Progreso del día",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "$completedCount de $totalCount completados (${(progress * 100).toInt()}%)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (progress >= 1f) ApuntaGreen else ApuntaIndigo
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (progress >= 1f) ApuntaGreen else ApuntaIndigo,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }

        // In-app voice feedback banner
        if (voiceFeedback != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ApuntaIndigo.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = ApuntaIndigo,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = voiceFeedback ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ApuntaIndigo,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Filter chips: [ Pendientes · Completados · Todos ]
        if (totalCount > 0) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = todayFilter == "TODOS",
                        onClick = { viewModel.setTodayFilter("TODOS") },
                        label = { Text("Todos ($totalCount)", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ApuntaIndigo,
                            selectedLabelColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    FilterChip(
                        selected = todayFilter == "PENDIENTES",
                        onClick = { viewModel.setTodayFilter("PENDIENTES") },
                        label = { Text("Pendientes ($pendingCount)", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ApuntaOrange,
                            selectedLabelColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    FilterChip(
                        selected = todayFilter == "COMPLETADOS",
                        onClick = { viewModel.setTodayFilter("COMPLETADOS") },
                        label = { Text("Completados ($completedCount)", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ApuntaGreen,
                            selectedLabelColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        }

        // Empty state when no reminders exist
        if (reminders.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(ApuntaGreen.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Celebration,
                                contentDescription = null,
                                tint = ApuntaGreen,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Día libre",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Tocá el micrófono para apuntar algo con tu voz o escribirlo.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = { viewModel.openListeningSheet() },
                            colors = ButtonDefaults.buttonColors(containerColor = ApuntaIndigo),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.testTag("empty_state_mic_btn")
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Apuntar recordatorio")
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = { viewModel.loadSampleData() },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.testTag("load_sample_data_btn")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cargar ejemplos")
                        }
                    }
                }
            }
        } else if (filteredReminders.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (todayFilter == "COMPLETADOS") "Aún no tenés recordatorios completados" else "No tenés recordatorios pendientes",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            // Reminders list: overdue first, pending by time, completed at end
            items(
                items = filteredReminders,
                key = { it.reminder.id }
            ) { item ->
                ReminderCard(
                    item = item,
                    onOpenNotebook = {
                        item.reminder.notebookId?.let { nbId ->
                            onOpenNotebook(nbId)
                        }
                    },
                    onToggleDone = {
                        viewModel.toggleReminderDone(item.reminder.id)
                    },
                    onSnooze = {
                        viewModel.snoozeReminder(item.reminder.id, 20)
                    }
                )
            }
        }
    }
}
