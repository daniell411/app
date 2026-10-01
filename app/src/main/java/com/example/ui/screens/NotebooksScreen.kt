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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MenuBook
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.NotebookCard
import com.example.ui.theme.ApuntaIndigo
import com.example.ui.theme.NotebookPalette
import com.example.ui.theme.parseHexColor
import com.example.ui.viewmodel.ApuntaViewModel

@Composable
fun NotebooksScreen(
    viewModel: ApuntaViewModel,
    onOpenNotebook: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val notebooksWithCounts by viewModel.notebooksWithCounts.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategoryFilter.collectAsState()

    var showNewNotebookDialog by remember { mutableStateOf(false) }

    // Filter notebooks cleanly
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

    // Dynamic category list from user's actual notebooks (no hardcoded personal labels)
    val categories = remember(notebooksWithCounts) {
        val uniqueLabels = notebooksWithCounts
            .map { it.notebook.label.trim() }
            .filter { it.isNotBlank() }
            .distinct()
        listOf("Todos") + uniqueLabels
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("notebooks_screen")
    ) {
        // Search bar & Add Notebook button on top
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

        // Category Filter Chips
        if (categories.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { category ->
                    val isSelected = selectedCategory == category
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setCategoryFilter(category) },
                        label = { Text(category, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ApuntaIndigo,
                            selectedLabelColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        } else {
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Notebooks Grid / Empty state
        if (filteredNotebooks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(ApuntaIndigo.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = ApuntaIndigo,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = if (searchQuery.isNotBlank()) "No se encontraron cuadernos" else "Aún no tenés cuadernos",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (searchQuery.isNotBlank()) "Probá con otra palabra de búsqueda"
                            else "Cada recordatorio tiene su propio cuaderno para notas, archivos y checklists.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { showNewNotebookDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = ApuntaIndigo),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Crear cuaderno")
                            }

                            if (notebooksWithCounts.isEmpty()) {
                                OutlinedButton(
                                    onClick = { viewModel.loadSampleData() },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Cargar ejemplos")
                                }
                            }
                        }
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = filteredNotebooks,
                    key = { it.notebook.id }
                ) { item ->
                    NotebookCard(
                        item = item,
                        onClick = { onOpenNotebook(item.notebook.id) }
                    )
                }
            }
        }
    }

    // Dialog for creating a new Notebook
    if (showNewNotebookDialog) {
        var newNotebookName by remember { mutableStateOf("") }
        var newNotebookLabel by remember { mutableStateOf("") }
        var selectedColorIdx by remember { mutableIntStateOf(0) }

        AlertDialog(
            onDismissRequest = { showNewNotebookDialog = false },
            title = {
                Text("Nuevo Cuaderno", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newNotebookName,
                        onValueChange = { newNotebookName = it },
                        label = { Text("Nombre del cuaderno") },
                        placeholder = { Text("Ej. Finanzas, Trabajo...") },
                        modifier = Modifier.fillMaxWidth().testTag("dialog_notebook_name"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = newNotebookLabel,
                        onValueChange = { newNotebookLabel = it },
                        label = { Text("Etiqueta o Categoría (opcional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Text(
                        text = "Color del lomo:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        NotebookPalette.forEachIndexed { index, pair ->
                            val color = pair.second
                            val isSelected = selectedColorIdx == index
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable { selectedColorIdx = index }
                                    .border(
                                        width = if (isSelected) 3.dp else 0.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = newNotebookName.trim()
                        if (trimmed.isNotBlank()) {
                            val color = NotebookPalette.getOrElse(selectedColorIdx) { NotebookPalette[0] }.first
                            val label = newNotebookLabel.trim().ifBlank { trimmed }
                            viewModel.createNotebook(trimmed, color, label)
                            showNewNotebookDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ApuntaIndigo),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("dialog_create_notebook_btn")
                ) {
                    Text("Crear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewNotebookDialog = false }) {
                    Text("Cancelar")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}
