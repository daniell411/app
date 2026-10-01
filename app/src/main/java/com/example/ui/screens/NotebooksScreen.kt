package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NotebookWithCounts
import com.example.ui.components.NotebookCard
import com.example.ui.components.ReminderCard
import com.example.ui.theme.ApuntaGreen
import com.example.ui.theme.ApuntaIndigo
import com.example.ui.theme.NotebookPalette
import com.example.ui.viewmodel.ApuntaViewModel

@Composable
fun NotebooksScreen(
    viewModel: ApuntaViewModel,
    onOpenNotebook: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val notebooksWithCounts by viewModel.notebooksWithCounts.collectAsState()
    val allReminders by viewModel.todayReminders.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategoryFilter.collectAsState()

    var showNewNotebookDialog by remember { mutableStateOf(false) }
    var showArchiveSection by remember { mutableStateOf(false) }

    // Filter notebooks
    val filteredNotebooks = remember(notebooksWithCounts, searchQuery, selectedCategory) {
        notebooksWithCounts.filter { item ->
            val matchesCategory = selectedCategory == "Todos" ||
                    item.notebook.label.equals(selectedCategory, ignoreCase = true) ||
                    item.notebook.name.equals(selectedCategory, ignoreCase = true)
            val matchesSearch = searchQuery.isBlank() ||
                    item.notebook.name.contains(searchQuery, ignoreCase = true) ||
                    item.notebook.label.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    // Filter archived / completed reminders
    val completedReminders = remember(allReminders, searchQuery) {
        allReminders.filter { it.reminder.status == "DONE" && (searchQuery.isBlank() || it.reminder.title.contains(searchQuery, ignoreCase = true)) }
    }

    val categories = remember(notebooksWithCounts) {
        val uniqueLabels = notebooksWithCounts.map { it.notebook.label }.filter { it.isNotBlank() }.distinct()
        listOf("Todos") + (if (uniqueLabels.contains("Robótica")) emptyList() else listOf("Robótica")) +
                (if (uniqueLabels.contains("Datos")) emptyList() else listOf("Datos")) +
                (if (uniqueLabels.contains("Personal")) emptyList() else listOf("Personal")) +
                (if (uniqueLabels.contains("Trading")) emptyList() else listOf("Trading")) +
                uniqueLabels
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("notebooks_screen")
    ) {
        // Search bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Buscar en cuadernos y notas...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Buscar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("notebooks_search_bar"),
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ApuntaIndigo
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = { showNewNotebookDialog = true },
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(ApuntaIndigo)
                    .testTag("new_notebook_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Nuevo Cuaderno",
                    tint = Color.White
                )
            }
        }

        // Category filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { viewModel.setCategoryFilter(cat) },
                    label = { Text(cat, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ApuntaIndigo,
                        selectedLabelColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }

        // 2-Column Grid of Notebooks
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredNotebooks, key = { it.notebook.id }) { item ->
                NotebookCard(
                    item = item,
                    onClick = { onOpenNotebook(item.notebook.id) }
                )
            }

            // Section "Archivo" button & list
            item(span = { GridItemSpan(2) }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .clickable { showArchiveSection = !showArchiveSection },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Archive,
                                contentDescription = null,
                                tint = ApuntaGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Archivo de recordatorios",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${completedReminders.size} recordatorios cumplidos",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Text(
                            text = if (showArchiveSection) "Ocultar" else "Ver",
                            style = MaterialTheme.typography.labelSmall,
                            color = ApuntaIndigo,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (showArchiveSection) {
                if (completedReminders.isEmpty()) {
                    item(span = { GridItemSpan(2) }) {
                        Text(
                            text = "No hay recordatorios archivados.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    items(completedReminders, span = { GridItemSpan(2) }) { item ->
                        ReminderCard(
                            item = item,
                            onOpenNotebook = {
                                item.reminder.notebookId?.let { nbId ->
                                    onOpenNotebook(nbId)
                                }
                            },
                            onToggleDone = { viewModel.toggleReminderDone(item.reminder.id) },
                            onSnooze = { viewModel.snoozeReminder(item.reminder.id) }
                        )
                    }
                }
            }
        }
    }

    // New Notebook Dialog
    if (showNewNotebookDialog) {
        var notebookName by remember { mutableStateOf("") }
        var notebookLabel by remember { mutableStateOf("") }
        var selectedColorHex by remember { mutableStateOf(NotebookPalette.first().first) }

        AlertDialog(
            onDismissRequest = { showNewNotebookDialog = false },
            title = { Text("Nuevo Cuaderno", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = notebookName,
                        onValueChange = { notebookName = it },
                        label = { Text("Título del cuaderno") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = notebookLabel,
                        onValueChange = { notebookLabel = it },
                        label = { Text("Materia o etiqueta (opcional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Elegí el color del cuaderno (8 colores):",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // 8-color palette selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NotebookPalette.forEach { (hex, color) ->
                            val isSelected = selectedColorHex == hex
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColorHex = hex }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (notebookName.isNotBlank()) {
                            viewModel.createNotebook(
                                name = notebookName.trim(),
                                colorHex = selectedColorHex,
                                label = notebookLabel.trim()
                            )
                            showNewNotebookDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ApuntaIndigo),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Crear cuaderno")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewNotebookDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
