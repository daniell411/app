package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLink
import com.example.data.model.Attachment
import com.example.data.model.ChecklistItem
import com.example.data.model.Note
import com.example.data.model.Notebook
import com.example.ui.components.ReminderCard
import com.example.ui.theme.ApuntaGreen
import com.example.ui.theme.ApuntaIndigo
import com.example.ui.theme.ApuntaOrange
import com.example.ui.theme.parseHexColor
import com.example.ui.viewmodel.ApuntaViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotebookDetailScreen(
    notebookId: String,
    viewModel: ApuntaViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val allNotebooks by viewModel.allNotebooks.collectAsState()
    val notebook = allNotebooks.firstOrNull { it.id == notebookId } ?: Notebook(name = "Cuaderno", color = "#4F46E5")

    val notebookColor = remember(notebook.color) {
        parseHexColor(notebook.color)
    }

    val allReminders by viewModel.todayReminders.collectAsState()
    val notebookReminders = remember(allReminders, notebookId) {
        allReminders.filter { it.reminder.notebookId == notebookId }
    }

    val notes by viewModel.getNotesForNotebook(notebookId).collectAsState(initial = emptyList())
    val attachments by viewModel.getAttachmentsForNotebook(notebookId).collectAsState(initial = emptyList())
    val appLinks by viewModel.getAppLinksForNotebook(notebookId).collectAsState(initial = emptyList())
    val checklist by viewModel.getChecklistForNotebook(notebookId).collectAsState(initial = emptyList())

    var selectedInternalTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Recordatorios", "Notas", "Archivos", "Apps y Links", "Checklist")

    // Filter inside Notebook's Recordatorios
    var reminderFilter by remember { mutableStateOf("TODOS") }

    // State for creating new items
    var newNoteText by remember { mutableStateOf("") }
    var newChecklistText by remember { mutableStateOf("") }
    var showInlineAddLink by remember { mutableStateOf(false) }

    // Visual media picker for photos/attachments
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = "Adjunto_${System.currentTimeMillis() % 10000}.jpg"
            viewModel.addAttachmentToNotebook(
                notebookId = notebookId,
                type = "image",
                uri = uri.toString(),
                name = fileName,
                size = "1.8 MB"
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("notebook_detail_screen")
    ) {
        // Top banner in the notebook's color
        Surface(
            color = notebookColor,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 16.dp, top = 16.dp, bottom = 18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("notebook_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = notebook.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Cuaderno • ${notebook.label}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Stats badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.22f)
                    ) {
                        Text(
                            text = "${notebookReminders.size} recordatorios",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.18f)
                    ) {
                        Text(
                            text = "${notes.size} notas",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.18f)
                    ) {
                        Text(
                            text = "${checklist.size} tareas",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // 5 Internal Sub-tabs (Recordatorios, Notas, Archivos, Apps y Links, Checklist)
        ScrollableTabRow(
            selectedTabIndex = selectedInternalTab,
            edgePadding = 16.dp,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = notebookColor,
            modifier = Modifier.fillMaxWidth()
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedInternalTab == index,
                    onClick = { selectedInternalTab = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedInternalTab == index) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                )
            }
        }

        // Sub-tab content area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (selectedInternalTab) {
                // TAB 0: RECORDATORIOS (incluye filtro de Completados)
                0 -> {
                    val filteredRem = remember(notebookReminders, reminderFilter) {
                        when (reminderFilter) {
                            "PENDIENTES" -> notebookReminders.filter { it.reminder.status != "DONE" }
                            "COMPLETADOS" -> notebookReminders.filter { it.reminder.status == "DONE" }
                            else -> notebookReminders
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (notebookReminders.isNotEmpty()) {
                            item {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("TODOS" to "Todos", "PENDIENTES" to "Pendientes", "COMPLETADOS" to "Completados").forEach { (code, label) ->
                                        FilterChip(
                                            selected = reminderFilter == code,
                                            onClick = { reminderFilter = code },
                                            label = { Text(label, fontSize = 12.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = notebookColor,
                                                selectedLabelColor = Color.White
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        if (filteredRem.isEmpty()) {
                            item {
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = if (reminderFilter == "COMPLETADOS") "No tenés recordatorios completados en este cuaderno"
                                            else "No hay recordatorios registrados en este cuaderno",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Button(
                                            onClick = { viewModel.openListeningSheet() },
                                            colors = ButtonDefaults.buttonColors(containerColor = notebookColor),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Apuntar recordatorio", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        } else {
                            items(filteredRem, key = { it.reminder.id }) { item ->
                                ReminderCard(
                                    item = item,
                                    onOpenNotebook = {},
                                    onToggleDone = { viewModel.toggleReminderDone(item.reminder.id) },
                                    onSnooze = { viewModel.snoozeReminder(item.reminder.id, 20) }
                                )
                            }
                        }
                    }
                }

                // TAB 1: NOTAS
                1 -> {
                    var newLinkLabel by remember { mutableStateOf("") }
                    var newLinkUrl by remember { mutableStateOf("") }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            // Free text note editor
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    OutlinedTextField(
                                        value = newNoteText,
                                        onValueChange = { newNoteText = it },
                                        placeholder = { Text("Escribí una nota...") },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(96.dp)
                                            .testTag("notebook_note_input"),
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Button(
                                                onClick = { viewModel.openListeningSheet() },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                                ),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Dictar con voz", fontSize = 12.sp)
                                            }

                                            Button(
                                                onClick = { showInlineAddLink = !showInlineAddLink },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                                ),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Vincular app/link", fontSize = 12.sp)
                                            }
                                        }

                                        Button(
                                            onClick = {
                                                if (newNoteText.isNotBlank()) {
                                                    viewModel.addNoteToNotebook(notebookId, newNoteText)
                                                    newNoteText = ""
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = notebookColor),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.testTag("add_note_btn")
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Guardar", fontSize = 12.sp)
                                        }
                                    }

                                    // Inline option to attach a link/app to this notebook
                                    if (showInlineAddLink) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Text("Vincular App o Enlace Web", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                                OutlinedTextField(
                                                    value = newLinkLabel,
                                                    onValueChange = { newLinkLabel = it },
                                                    placeholder = { Text("Nombre (ej. Documentos, Notion)") },
                                                    singleLine = true,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                                OutlinedTextField(
                                                    value = newLinkUrl,
                                                    onValueChange = { newLinkUrl = it },
                                                    placeholder = { Text("URL (ej. https://...)") },
                                                    singleLine = true,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                                Button(
                                                    onClick = {
                                                        if (newLinkLabel.isNotBlank() && newLinkUrl.isNotBlank()) {
                                                            viewModel.addAppLinkToNotebook(
                                                                notebookId = notebookId,
                                                                label = newLinkLabel.trim(),
                                                                url = newLinkUrl.trim(),
                                                                icon = "web"
                                                            )
                                                            newLinkLabel = ""
                                                            newLinkUrl = ""
                                                            showInlineAddLink = false
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = notebookColor),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.align(Alignment.End)
                                                ) {
                                                    Text("Guardar enlace", fontSize = 12.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (notes.isEmpty()) {
                            item {
                                Text(
                                    text = "No tenés notas en este cuaderno. Escribí una arriba o dictala con voz.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 16.dp)
                                )
                            }
                        } else {
                            items(notes, key = { it.id }) { note ->
                                NoteCard(
                                    note = note,
                                    onDelete = { viewModel.deleteNote(note) }
                                )
                            }
                        }
                    }
                }

                // TAB 2: ARCHIVOS
                2 -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Button(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        androidx.activity.result.PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = notebookColor),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Adjuntar imagen de la galería", fontSize = 13.sp)
                            }
                        }

                        if (attachments.isEmpty()) {
                            item {
                                Text(
                                    text = "No hay archivos adjuntos en este cuaderno.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 20.dp)
                                )
                            }
                        } else {
                            items(attachments, key = { it.id }) { attachment ->
                                AttachmentCard(
                                    attachment = attachment,
                                    onOpen = {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                                data = Uri.parse("https://www.google.com")
                                            }
                                            context.startActivity(intent)
                                        } catch (_: Exception) {}
                                    }
                                )
                            }
                        }
                    }
                }

                // TAB 3: APPS Y LINKS
                3 -> {
                    var newLinkLabel by remember { mutableStateOf("") }
                    var newLinkUrl by remember { mutableStateOf("") }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "Vincular App o Enlace Web",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = newLinkLabel,
                                        onValueChange = { newLinkLabel = it },
                                        label = { Text("Nombre (ej. Notion, Google Drive)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp),
                                        singleLine = true
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = newLinkUrl,
                                        onValueChange = { newLinkUrl = it },
                                        label = { Text("URL o enlace (ej. https://...)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp),
                                        singleLine = true
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = {
                                            if (newLinkLabel.isNotBlank() && newLinkUrl.isNotBlank()) {
                                                viewModel.addAppLinkToNotebook(
                                                    notebookId = notebookId,
                                                    label = newLinkLabel.trim(),
                                                    url = newLinkUrl.trim(),
                                                    icon = "web"
                                                )
                                                newLinkLabel = ""
                                                newLinkUrl = ""
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = notebookColor),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.align(Alignment.End)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Agregar link")
                                    }
                                }
                            }
                        }

                        if (appLinks.isEmpty()) {
                            item {
                                Text(
                                    text = "No tenés enlaces ni apps vinculadas en este cuaderno.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 16.dp)
                                )
                            }
                        } else {
                            items(appLinks, key = { it.id }) { link ->
                                AppLinkCard(
                                    link = link,
                                    onOpen = {
                                        try {
                                            val uri = if (link.url.startsWith("http://") || link.url.startsWith("https://")) {
                                                Uri.parse(link.url)
                                            } else {
                                                Uri.parse("https://${link.url}")
                                            }
                                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                        } catch (_: Exception) {}
                                    }
                                )
                            }
                        }
                    }
                }

                // TAB 4: CHECKLIST
                4 -> {
                    val doneCount = checklist.count { it.done }
                    val totalCount = checklist.size
                    val progress = if (totalCount > 0) doneCount.toFloat() / totalCount.toFloat() else 0f

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Progress bar card
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Progreso del checklist",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "$doneCount de $totalCount hechos",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (doneCount == totalCount && totalCount > 0) ApuntaGreen else ApuntaOrange,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    LinearProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = if (doneCount == totalCount && totalCount > 0) ApuntaGreen else notebookColor,
                                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                    )
                                }
                            }
                        }

                        // Add new checklist item input
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = newChecklistText,
                                    onValueChange = { newChecklistText = it },
                                    placeholder = { Text("Nueva subtarea...") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("new_checklist_input"),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        if (newChecklistText.isNotBlank()) {
                                            viewModel.addChecklistItem(notebookId, newChecklistText)
                                            newChecklistText = ""
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = notebookColor),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("add_checklist_btn")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Agregar")
                                }
                            }
                        }

                        items(checklist, key = { it.id }) { item ->
                            ChecklistItemRow(
                                item = item,
                                notebookColor = notebookColor,
                                onToggle = { viewModel.toggleChecklistItem(item) },
                                onDelete = { viewModel.deleteChecklistItem(item) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteCard(note: Note, onDelete: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("d MMM, h:mm a", Locale("es", "ES")) }
    val dateStr = remember(note.createdAt) { dateFormat.format(Date(note.createdAt)) }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar nota",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = note.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun AttachmentCard(attachment: Attachment, onOpen: () -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(ApuntaIndigo.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (attachment.type == "pdf") Icons.Default.PictureAsPdf else Icons.Default.AttachFile,
                    contentDescription = null,
                    tint = ApuntaIndigo,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = attachment.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${attachment.type.uppercase()} • ${attachment.sizeFormatted}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.OpenInNew,
                contentDescription = "Abrir",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun AppLinkCard(link: AppLink, onOpen: () -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(ApuntaOrange.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = null,
                    tint = ApuntaOrange,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = link.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = link.url,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Icon(
                imageVector = Icons.Default.OpenInBrowser,
                contentDescription = "Abrir en navegador",
                tint = ApuntaIndigo,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ChecklistItemRow(
    item: ChecklistItem,
    notebookColor: Color,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.done) ApuntaGreen.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.done,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = ApuntaGreen,
                    checkmarkColor = Color.White
                )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = item.text,
                style = MaterialTheme.typography.bodyMedium,
                textDecoration = if (item.done) TextDecoration.LineThrough else TextDecoration.None,
                color = if (item.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Eliminar",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
