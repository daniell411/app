package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ApuntaGreen
import com.example.ui.theme.ApuntaIndigo
import com.example.ui.theme.ApuntaOrange
import com.example.ui.viewmodel.ApuntaViewModel

@Composable
fun SettingsScreen(
    viewModel: ApuntaViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val userPreferences by viewModel.userPreferences.collectAsState()

    val userName = userPreferences.userName
    val themeMode = userPreferences.themeMode
    val defaultLeadTime = userPreferences.defaultLeadTimeMinutes
    val staggeredAlerts = userPreferences.staggeredAlerts
    val wakeWordEnabled = userPreferences.wakeWordEnabled
    val morningSummaryEnabled = userPreferences.morningSummaryEnabled
    val morningSummaryTime = userPreferences.morningSummaryTime
    val voiceDialect = userPreferences.voiceDialect

    var editingName by remember(userName) { mutableStateOf(userName) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Ajustes",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // SECCIÓN 1: PERFIL
        item {
            SettingsSectionCard(title = "Perfil", icon = Icons.Default.Person) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Nombre",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Tu nombre usado para el saludo y la voz.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = editingName,
                        onValueChange = {
                            editingName = it
                            viewModel.setUserName(it)
                        },
                        placeholder = { Text("Escribí tu nombre") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("username_setting_field"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            }
        }

        // SECCIÓN 2: VOZ
        item {
            SettingsSectionCard(title = "Voz", icon = Icons.Default.Mic) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Método de activación
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Método de activación", fontWeight = FontWeight.SemiBold)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = ApuntaIndigo.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "Botón flotante",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ApuntaIndigo,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Text(
                            text = "Botón índigo central de micrófono accesible en todas las pestañas.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Palabra de activación
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Palabra de activación ('Ey Apunta')", fontWeight = FontWeight.SemiBold)
                            Switch(
                                checked = wakeWordEnabled,
                                onCheckedChange = { viewModel.setWakeWordEnabled(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = ApuntaIndigo)
                            )
                        }
                        Text(
                            text = "Activá el dictado diciendo 'Ey Apunta' mientras usás la app.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Lectura automática del resumen matutino
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Lectura automática del resumen matutino", fontWeight = FontWeight.SemiBold)
                            Switch(
                                checked = morningSummaryEnabled,
                                onCheckedChange = { viewModel.setMorningSummaryEnabled(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = ApuntaIndigo)
                            )
                        }
                        Text(
                            text = "Escuchá un resumen hablado de tus recordatorios al despertar ($morningSummaryTime).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (morningSummaryEnabled) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = { viewModel.speakMorningSummary() },
                                colors = ButtonDefaults.buttonColors(containerColor = ApuntaIndigo),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Probar resumen con voz", fontSize = 12.sp)
                            }
                        }
                    }

                    // Idioma de voz
                    Column {
                        Text("Idioma de voz", fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "Reconocimiento y entonación adaptados a tu región con voseo.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("es-SV" to "Español El Salvador (es-SV)", "es-419" to "Español Latino (es-419)").forEach { (code, label) ->
                                FilterChip(
                                    selected = voiceDialect == code,
                                    onClick = { viewModel.setVoiceDialect(code) },
                                    label = { Text(label, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ApuntaIndigo,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // SECCIÓN 3: RECORDATORIOS
        item {
            SettingsSectionCard(title = "Recordatorios", icon = Icons.Default.NotificationsActive) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Aviso previo
                    Column {
                        Text("Aviso previo predeterminado", fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "Tiempo de anticipación para las notificaciones programadas.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(0 to "A la hora", 15 to "15 min antes", 30 to "30 min antes", 60 to "1 hora antes").forEach { (mins, label) ->
                                FilterChip(
                                    selected = defaultLeadTime == mins,
                                    onClick = { viewModel.setDefaultLeadTimeMinutes(mins) },
                                    label = { Text(label, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ApuntaIndigo,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }

                    // Insistencia / Alertas escalonadas
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Insistencia (Alertas escalonadas)", fontWeight = FontWeight.SemiBold)
                            Switch(
                                checked = staggeredAlerts,
                                onCheckedChange = { viewModel.setStaggeredAlerts(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = ApuntaIndigo)
                            )
                        }
                        Text(
                            text = "Aviso suave inicial, luego insistencia automática a los 5 y a los 10 minutos.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // SECCIÓN 4: APARIENCIA
        item {
            SettingsSectionCard(title = "Apariencia", icon = Icons.Default.DarkMode) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Tema de la aplicación", fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "Elegí si la app sigue el sistema o usa un modo fijo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("SYSTEM" to "Automático", "LIGHT" to "Claro", "DARK" to "Oscuro").forEach { (mode, label) ->
                            FilterChip(
                                selected = themeMode == mode,
                                onClick = { viewModel.setThemeMode(mode) },
                                label = { Text(label, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ApuntaIndigo,
                                    selectedLabelColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }
            }
        }

        // SECCIÓN 5: DATOS
        item {
            SettingsSectionCard(title = "Datos", icon = Icons.Default.Backup) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Exportar texto
                    Column {
                        Text("Exportar texto", fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "Copia tus recordatorios, notas y cuadernos en texto plano.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = {
                                val text = viewModel.generateExportText()
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Apunta Texto", text)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Resumen copiado al portapapeles", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Copiar texto al portapapeles", fontSize = 12.sp)
                        }
                    }

                    // Exportar JSON
                    Column {
                        Text("Exportar JSON", fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "Genera una copia de seguridad completa en formato JSON.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = {
                                val json = viewModel.generateExportJson()
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Apunta JSON", json)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "JSON de respaldo copiado al portapapeles", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ApuntaIndigo),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Copiar JSON de respaldo", fontSize = 12.sp)
                        }
                    }

                    // Restaurar / Ejemplos
                    Column {
                        Text("Restaurar / Ejemplos", fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "Probá la app con datos de muestra o limpiá los ejemplos de prueba.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.loadSampleData()
                                    Toast.makeText(context, "Ejemplos cargados", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Cargar ejemplos", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    viewModel.clearSampleData()
                                    Toast.makeText(context, "Ejemplos eliminados", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Borrar ejemplos", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // SECCIÓN 6: PRIVACIDAD
        item {
            SettingsSectionCard(title = "Privacidad", icon = Icons.Default.Security) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Almacenamiento y procesamiento transparente",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Tus recordatorios, notas, fotos y checklists se guardan localmente en tu teléfono. El micrófono solo se abre cuando tocás el botón de dictar o activás la palabra clave. Tus notas y datos no salen de tu dispositivo, excepto cuando elegís usar reconocimiento de voz en la nube de Google o consultar la IA de Gemini, en cuyo caso solo se envía el texto de tu consulta de forma segura. No recopilamos ni vendemos tu información personal.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ApuntaGreen.copy(alpha = 0.12f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = ApuntaGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Sin rastreadores ni publicidad",
                                style = MaterialTheme.typography.labelSmall,
                                color = ApuntaGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(ApuntaIndigo.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = ApuntaIndigo,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            content()
        }
    }
}
