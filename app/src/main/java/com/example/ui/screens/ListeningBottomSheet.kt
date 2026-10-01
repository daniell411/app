package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.components.CountdownProgressBar
import com.example.ui.components.WaveformVisualizer
import com.example.ui.theme.ApuntaGreen
import com.example.ui.theme.ApuntaIndigo
import com.example.ui.theme.ApuntaOrange
import com.example.ui.theme.NotebookPalette
import com.example.ui.theme.parseHexColor
import com.example.ui.viewmodel.ApuntaViewModel
import com.example.voice.SpeechState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class CreationMode {
    RECORDATORIO,
    NOTA,
    CUADERNO
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ListeningBottomSheet(
    viewModel: ApuntaViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val speechState by viewModel.speechManager.speechState.collectAsState()
    val soundRms by viewModel.speechManager.soundRms.collectAsState()
    val voiceDraft by viewModel.voiceDraft.collectAsState()
    val conversationalQuestion by viewModel.conversationalQuestion.collectAsState()
    val notebooks by viewModel.allNotebooks.collectAsState()

    var creationMode by remember { mutableStateOf(CreationMode.RECORDATORIO) }
    var singleInputText by remember { mutableStateOf("") }
    var isCountdownPaused by remember { mutableStateOf(false) }
    var showMicRationale by remember { mutableStateOf(false) }

    // Draft edit fields for Reminder
    var editableTitle by remember(voiceDraft?.title) {
        mutableStateOf(voiceDraft?.title ?: "")
    }
    var editableTimestamp by remember(voiceDraft?.timestampMillis) {
        mutableLongStateOf(voiceDraft?.timestampMillis ?: (System.currentTimeMillis() + 3600000L))
    }
    var editableLeadTime by remember(voiceDraft?.leadTimeMinutes) {
        mutableIntStateOf(voiceDraft?.leadTimeMinutes ?: 0)
    }
    var editableNotebookId by remember(voiceDraft?.suggestedNotebookId, notebooks) {
        mutableStateOf(voiceDraft?.suggestedNotebookId ?: notebooks.firstOrNull()?.id)
    }

    // State for Notebook creation
    var selectedColorIndex by remember { mutableIntStateOf(0) }

    // State for Note target notebook
    var selectedNoteNotebookId by remember(notebooks) {
        mutableStateOf(notebooks.firstOrNull()?.id)
    }

    val isListening = speechState is SpeechState.Listening || speechState is SpeechState.PartialResult
    val liveText = when (val state = speechState) {
        is SpeechState.PartialResult -> state.text
        is SpeechState.FinalResult -> state.text
        is SpeechState.Listening -> "Escuchando... hablá con naturalidad"
        is SpeechState.Error -> state.message
        SpeechState.Idle -> "Tocá el micrófono para dictar o escribí abajo"
    }

    // Permission launcher for microphone
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        showMicRationale = false
        if (isGranted) {
            viewModel.speechManager.startListening()
        }
    }

    fun handleMicClick() {
        val hasMicPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasMicPermission) {
            showMicRationale = true
        } else {
            if (isListening) {
                viewModel.speechManager.stopListening()
            } else {
                viewModel.speechManager.startListening()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp)
                .verticalScroll(rememberScrollState())
                .testTag("listening_bottom_sheet"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Drag pill & close button header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp).testTag("close_sheet_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Selector de tipo: [ Recordatorio | Nota | Cuaderno ]
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                TabRow(
                    selectedTabIndex = creationMode.ordinal,
                    containerColor = Color.Transparent,
                    indicator = {},
                    divider = {}
                ) {
                    CreationMode.values().forEach { mode ->
                        val isSelected = creationMode == mode
                        val label = when (mode) {
                            CreationMode.RECORDATORIO -> "Recordatorio"
                            CreationMode.NOTA -> "Nota"
                            CreationMode.CUADERNO -> "Cuaderno"
                        }
                        Tab(
                            selected = isSelected,
                            onClick = {
                                creationMode = mode
                                isCountdownPaused = true
                            },
                            modifier = Modifier
                                .padding(4.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) ApuntaIndigo else Color.Transparent)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Microphone Permission Rationale card if required
            if (showMicRationale) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ApuntaIndigo.copy(alpha = 0.12f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Acceso al micrófono",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = ApuntaIndigo
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Para dictar tus recordatorios y notas con la voz, necesitamos permiso para acceder al micrófono de tu teléfono.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                                colors = ButtonDefaults.buttonColors(containerColor = ApuntaIndigo),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Permitir micrófono", fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = { showMicRationale = false },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Ahora no", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Animated Indigo Soundwave + Mic state
            if (creationMode != CreationMode.CUADERNO) {
                WaveformVisualizer(
                    isListening = isListening,
                    amplitude = soundRms,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                // Conversational prompt if data is missing (e.g. "¿A qué hora?")
                if (conversationalQuestion != null && creationMode == CreationMode.RECORDATORIO) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = ApuntaOrange.copy(alpha = 0.15f),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(ApuntaOrange),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = conversationalQuestion ?: "¿A qué hora?",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Decí por ejemplo: 'A las 3 de la tarde' o 'En 30 minutos'",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Live recognized speech text
                Text(
                    text = liveText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (isListening) ApuntaIndigo else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    minLines = 1
                )
            }

            // Mode-specific options
            when (creationMode) {
                CreationMode.RECORDATORIO -> {
                    // Confirmation Card when draft is ready
                    if (voiceDraft != null && conversationalQuestion == null) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Recordatorio detectado",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = ApuntaGreen.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "Listo para guardar",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ApuntaGreen,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = editableTitle,
                                    onValueChange = {
                                        editableTitle = it
                                        isCountdownPaused = true
                                    },
                                    label = { Text("Título") },
                                    modifier = Modifier.fillMaxWidth().testTag("draft_title_field"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ApuntaIndigo)
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                                val dateFormat = SimpleDateFormat("EEEE d 'de' MMMM", Locale("es", "ES"))
                                val timeDisplay = timeFormat.format(Date(editableTimestamp))
                                val dateDisplay = dateFormat.format(Date(editableTimestamp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier.weight(1f).clickable {
                                            editableTimestamp += 3600000L
                                            isCountdownPaused = true
                                        }
                                    ) {
                                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.AccessTime, contentDescription = null, tint = ApuntaIndigo, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column {
                                                Text("Hora", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text(timeDisplay, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                            }
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier.weight(1f).clickable {
                                            editableTimestamp += 86400000L
                                            isCountdownPaused = true
                                        }
                                    ) {
                                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = ApuntaIndigo, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column {
                                                Text("Fecha", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text(dateDisplay.take(12), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text("Aviso previo", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(0 to "A la hora", 15 to "15 min antes", 30 to "30 min antes", 60 to "1 hora antes").forEach { (mins, label) ->
                                        FilterChip(
                                            selected = editableLeadTime == mins,
                                            onClick = {
                                                editableLeadTime = mins
                                                isCountdownPaused = true
                                            },
                                            label = { Text(label, fontSize = 12.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = ApuntaIndigo,
                                                selectedLabelColor = Color.White
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text("Cuaderno", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    notebooks.forEach { nb ->
                                        val nbColor = parseHexColor(nb.color)
                                        val isSelected = editableNotebookId == nb.id
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                editableNotebookId = nb.id
                                                isCountdownPaused = true
                                            },
                                            leadingIcon = {
                                                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(nbColor))
                                            },
                                            label = { Text(nb.name, fontSize = 12.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = nbColor.copy(alpha = 0.2f),
                                                selectedLabelColor = nbColor
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        if (!isCountdownPaused) {
                            CountdownProgressBar(
                                onTimeout = {
                                    viewModel.confirmAndSaveDraft(
                                        title = editableTitle,
                                        timestamp = editableTimestamp,
                                        leadTimeMinutes = editableLeadTime,
                                        notebookId = editableNotebookId
                                    )
                                },
                                onUndo = { onDismiss() },
                                onEdit = { isCountdownPaused = true }
                            )
                        } else {
                            Button(
                                onClick = {
                                    viewModel.confirmAndSaveDraft(
                                        title = editableTitle,
                                        timestamp = editableTimestamp,
                                        leadTimeMinutes = editableLeadTime,
                                        notebookId = editableNotebookId
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ApuntaIndigo),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_now_btn")
                            ) {
                                Icon(Icons.Default.Done, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Guardar recordatorio", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                CreationMode.NOTA -> {
                    // Notebook destination picker for Note
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Text(
                            text = "Guardar en el cuaderno:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            notebooks.forEach { nb ->
                                val nbColor = parseHexColor(nb.color)
                                val isSelected = selectedNoteNotebookId == nb.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedNoteNotebookId = nb.id },
                                    leadingIcon = {
                                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(nbColor))
                                    },
                                    label = { Text(nb.name, fontSize = 12.sp) }
                                )
                            }
                        }
                    }
                }

                CreationMode.CUADERNO -> {
                    // Color palette selector for Notebook
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Text(
                            text = "Color del cuaderno:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            NotebookPalette.forEachIndexed { index, pair ->
                                val color = pair.second
                                val isSelected = selectedColorIndex == index
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .clickable { selectedColorIndex = index }
                                        .border(
                                            width = if (isSelected) 3.dp else 0.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // SINGLE UNIFIED INPUT FIELD (Unified text and voice action)
            val placeholderText = when (creationMode) {
                CreationMode.RECORDATORIO -> "Decí o escribí un recordatorio..."
                CreationMode.NOTA -> "Decí o escribí una nota..."
                CreationMode.CUADERNO -> "Nombre del cuaderno..."
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = singleInputText,
                    onValueChange = { singleInputText = it },
                    placeholder = { Text(placeholderText) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("single_input_field"),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = creationMode == CreationMode.CUADERNO,
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ApuntaIndigo)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Mic toggle button (available for Recordatorio and Nota)
                if (creationMode != CreationMode.CUADERNO) {
                    IconButton(
                        onClick = { handleMicClick() },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (isListening) ApuntaOrange else ApuntaIndigo.copy(alpha = 0.15f))
                            .testTag("mic_toggle_btn")
                    ) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = if (isListening) "Detener" else "Dictar",
                            tint = if (isListening) Color.White else ApuntaIndigo
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }

                // Send action button
                IconButton(
                    onClick = {
                        val input = singleInputText.trim()
                        if (input.isNotBlank()) {
                            when (creationMode) {
                                CreationMode.RECORDATORIO -> {
                                    viewModel.processSpokenText(input)
                                    singleInputText = ""
                                }
                                CreationMode.NOTA -> {
                                    val targetId = selectedNoteNotebookId ?: notebooks.firstOrNull()?.id ?: run {
                                        viewModel.createNotebook("General", "#4F46E5", "General")
                                        "General"
                                    }
                                    viewModel.addNoteToNotebook(targetId, input)
                                    singleInputText = ""
                                    onDismiss()
                                }
                                CreationMode.CUADERNO -> {
                                    val colorHex = NotebookPalette.getOrElse(selectedColorIndex) { NotebookPalette[0] }.first
                                    viewModel.createNotebook(input, colorHex, input)
                                    singleInputText = ""
                                    onDismiss()
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(ApuntaIndigo)
                        .testTag("send_input_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Guardar",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick speech simulation chips (clean, non-personal examples)
            if (creationMode == CreationMode.RECORDATORIO) {
                Text(
                    text = "Ejemplos rápidos:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "Mañana a las 3 reunión de equipo, y recordámelo una hora antes",
                        "En 20 minutos llamar al médico",
                        "El viernes a las 8 de la mañana revisar presupuesto",
                        "Apunta comprar café",
                        "aplazalo 20 minutos",
                        "marcá como hecho"
                    ).forEach { sample ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable {
                                viewModel.processSpokenText(sample)
                            }
                        ) {
                            Text(
                                text = sample.take(34) + if (sample.length > 34) "..." else "",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
