package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.ListeningBottomSheet
import com.example.ui.screens.NotebookDetailScreen
import com.example.ui.screens.NotebooksScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TodayScreen
import com.example.ui.theme.ApuntaIndigo
import com.example.ui.theme.ApuntaTheme
import com.example.ui.viewmodel.ApuntaTab
import com.example.ui.viewmodel.ApuntaViewModel

@Composable
fun ApuntaApp(viewModel: ApuntaViewModel) {
    val themeMode by viewModel.themeMode.collectAsState()
    val isSystemDark = isSystemInDarkTheme()
    val useDarkTheme = when (themeMode) {
        "DARK" -> true
        "LIGHT" -> false
        else -> isSystemDark
    }

    val currentTab by viewModel.currentTab.collectAsState()
    val selectedNotebookId by viewModel.selectedNotebookId.collectAsState()
    val isListeningSheetOpen by viewModel.isListeningSheetOpen.collectAsState()

    ApuntaTheme(darkTheme = useDarkTheme) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            if (selectedNotebookId != null) {
                // Cuaderno Detail Screen
                NotebookDetailScreen(
                    notebookId = selectedNotebookId!!,
                    viewModel = viewModel,
                    onBack = { viewModel.closeNotebook() }
                )
            } else {
                // Main Tab Scaffold
                Scaffold(
                    bottomBar = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding(),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            // Bottom navigation bar with 4 tabs
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(72.dp),
                                shadowElevation = 10.dp,
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceAround,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Tab 1: Hoy
                                    BottomNavItem(
                                        icon = Icons.Default.Today,
                                        label = "Hoy",
                                        isSelected = currentTab == ApuntaTab.HOY,
                                        onClick = { viewModel.selectTab(ApuntaTab.HOY) },
                                        tag = "tab_hoy"
                                    )

                                    // Tab 2: Cuadernos
                                    BottomNavItem(
                                        icon = Icons.Default.MenuBook,
                                        label = "Cuadernos",
                                        isSelected = currentTab == ApuntaTab.CUADERNOS,
                                        onClick = { viewModel.selectTab(ApuntaTab.CUADERNOS) },
                                        tag = "tab_cuadernos"
                                    )

                                    // Center gap for floating mic button
                                    Spacer(modifier = Modifier.width(60.dp))

                                    // Tab 3: Calendario
                                    BottomNavItem(
                                        icon = Icons.Default.CalendarMonth,
                                        label = "Calendario",
                                        isSelected = currentTab == ApuntaTab.CALENDARIO,
                                        onClick = { viewModel.selectTab(ApuntaTab.CALENDARIO) },
                                        tag = "tab_calendario"
                                    )

                                    // Tab 4: Ajustes
                                    BottomNavItem(
                                        icon = Icons.Default.Settings,
                                        label = "Ajustes",
                                        isSelected = currentTab == ApuntaTab.AJUSTES,
                                        onClick = { viewModel.selectTab(ApuntaTab.AJUSTES) },
                                        tag = "tab_ajustes"
                                    )
                                }
                            }

                            // Large circular floating indigo microphone button, centered on top of bottom bar
                            Box(
                                modifier = Modifier
                                    .offset(y = (-24).dp)
                                    .size(64.dp)
                                    .shadow(elevation = 10.dp, shape = CircleShape)
                                    .clip(CircleShape)
                                    .background(ApuntaIndigo)
                                    .clickable { viewModel.openListeningSheet() }
                                    .testTag("floating_mic_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Hablar con Apunta",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (currentTab) {
                            ApuntaTab.HOY -> TodayScreen(
                                viewModel = viewModel,
                                onOpenNotebook = { viewModel.openNotebook(it) }
                            )
                            ApuntaTab.CUADERNOS -> NotebooksScreen(
                                viewModel = viewModel,
                                onOpenNotebook = { viewModel.openNotebook(it) }
                            )
                            ApuntaTab.CALENDARIO -> CalendarScreen(
                                viewModel = viewModel,
                                onOpenNotebook = { viewModel.openNotebook(it) }
                            )
                            ApuntaTab.AJUSTES -> SettingsScreen(
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }

            // Listening Bottom Sheet modal
            if (isListeningSheetOpen) {
                ListeningBottomSheet(
                    viewModel = viewModel,
                    onDismiss = { viewModel.closeListeningSheet() }
                )
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) ApuntaIndigo else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) ApuntaIndigo else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}
