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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    val userName by viewModel.userName.collectAsState()
    val voiceFeedback by viewModel.voiceFeedback.collectAsState()

    val pendingCount = remember(reminders) {
        reminders.count { it.reminder.status != "DONE" }
    }

    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 5..11 -> "Buen día"
            in 12..18 -> "Buenas tardes"
            else -> "Buenas noches"
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
                    Column {
                        Text(
                            text = "$greeting, $userName",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (pendingCount == 0) "No tenés pendientes para hoy"
                            else if (pendingCount == 1) "Te queda 1 cosa hoy"
                            else "Te quedan $pendingCount cosas hoy",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (pendingCount > 0) ApuntaOrange else ApuntaGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Morning Summary quick TTS listen button
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
                            contentDescription = "Escuchar resumen matutino",
                            tint = ApuntaIndigo,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // In-app voice feedback banner (e.g. after "aplazalo 20 minutos" or "marcá como hecho")
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

        // Timeline header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Línea de tiempo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = "${reminders.size} recordatorios",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Empty state when no reminders
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
                            text = "Tocá el micrófono para apuntar algo",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { viewModel.openListeningSheet() },
                            colors = ButtonDefaults.buttonColors(containerColor = ApuntaIndigo),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.testTag("empty_state_mic_btn")
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Apuntar con voz")
                        }
                    }
                }
            }
        } else {
            // Reminders list: Upcoming first, completed at the end
            items(
                items = reminders,
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
