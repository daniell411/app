package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ReminderWithNotebook
import com.example.ui.theme.ApuntaCoral
import com.example.ui.theme.ApuntaGreen
import com.example.ui.theme.ApuntaIndigo
import com.example.ui.theme.ApuntaOrange
import com.example.ui.theme.parseHexColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun ReminderCard(
    item: ReminderWithNotebook,
    onOpenNotebook: () -> Unit,
    onToggleDone: () -> Unit,
    onSnooze: () -> Unit,
    modifier: Modifier = Modifier
) {
    val reminder = item.reminder
    val notebook = item.notebook
    val isDone = reminder.status == "DONE"
    val isOverdue = !isDone && reminder.datetime < System.currentTimeMillis()

    val notebookColor = remember(notebook?.color) {
        parseHexColor(notebook?.color ?: "#4F46E5")
    }

    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val timeStr = remember(reminder.datetime) {
        timeFormat.format(Date(reminder.datetime))
    }

    val dateFormat = remember { SimpleDateFormat("d MMM", Locale("es", "ES")) }
    val dateStr = remember(reminder.datetime) {
        dateFormat.format(Date(reminder.datetime))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onOpenNotebook)
            .testTag("reminder_card_${reminder.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isDone -> ApuntaGreen.copy(alpha = 0.08f)
                isOverdue -> ApuntaCoral.copy(alpha = 0.08f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDone) 0.5.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(108.dp)
        ) {
            // Lateral colored spine bar with the notebook color
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .fillMaxHeight()
                    .background(if (isDone) ApuntaGreen else if (isOverdue) ApuntaCoral else notebookColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header row: Time badge + Notebook badge + Overdue chip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when {
                                isDone -> ApuntaGreen.copy(alpha = 0.18f)
                                isOverdue -> ApuntaCoral.copy(alpha = 0.2f)
                                else -> MaterialTheme.colorScheme.primaryContainer
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isOverdue) Icons.Default.Alarm else Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = when {
                                        isDone -> ApuntaGreen
                                        isOverdue -> ApuntaCoral
                                        else -> MaterialTheme.colorScheme.primary
                                    },
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "$dateStr, $timeStr",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = when {
                                        isDone -> ApuntaGreen
                                        isOverdue -> ApuntaCoral
                                        else -> MaterialTheme.colorScheme.onPrimaryContainer
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (notebook != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = notebookColor.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = notebook.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = notebookColor,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    if (isOverdue) {
                        Text(
                            text = "Vencido",
                            color = ApuntaCoral,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Title
                Text(
                    text = reminder.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
                    color = when {
                        isDone -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        isOverdue -> ApuntaCoral
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                    fontWeight = if (isDone) FontWeight.Normal else FontWeight.SemiBold
                )

                // Bottom row: Notebook content badges (Notes, PDFs, Links, Checklist)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (item.notesCount > 0) {
                            BadgeIcon(
                                icon = Icons.Default.Description,
                                label = "${item.notesCount}",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (item.attachmentsCount > 0) {
                            BadgeIcon(
                                icon = Icons.Default.AttachFile,
                                label = "${item.attachmentsCount}",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (item.appLinksCount > 0) {
                            BadgeIcon(
                                icon = Icons.Default.Link,
                                label = "${item.appLinksCount}",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (item.checklistCount > 0) {
                            BadgeIcon(
                                icon = Icons.Default.Checklist,
                                label = "${item.checklistDoneCount}/${item.checklistCount}",
                                tint = if (item.checklistDoneCount == item.checklistCount) ApuntaGreen else ApuntaOrange
                            )
                        }
                    }

                    // Quick action buttons for convenience and accessibility
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!isDone) {
                            IconButton(
                                onClick = onSnooze,
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("snooze_btn_${reminder.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreTime,
                                    contentDescription = "Aplazar 20 min",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }

                        IconButton(
                            onClick = onToggleDone,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (isDone) ApuntaGreen.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant)
                                .testTag("toggle_done_btn_${reminder.id}")
                        ) {
                            Icon(
                                imageVector = if (isDone) Icons.Default.CheckCircle else Icons.Default.Check,
                                contentDescription = if (isDone) "Completado" else "Marcar como hecho",
                                tint = if (isDone) ApuntaGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BadgeIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = tint,
            fontWeight = FontWeight.Medium
        )
    }
}
