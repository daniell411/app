# APUNTA - Asistente de Recordatorios por Voz con Cuadernos

**APUNTA** es una aplicación móvil nativa para Android (Kotlin + Jetpack Compose) diseñada como un asistente de recordatorios por voz donde cada recordatorio posee su propio cuaderno enriquecido (notas, archivos, enlaces/apps y lista de tareas).

---

## 🎨 Sistema de Diseño
- **Estética:** Cálida como un cuaderno analógico, ágil como una app moderna. Esquinas redondeadas (16 dp en tarjetas, 28 dp en modales), sombras suaves y tipografía redondeada de alta legibilidad.
- **Paleta oficial:**
  - Fondo modo claro: `#FAF6EE` (crema suave)
  - Fondo modo oscuro: `#12151C` (azul noche)
  - Color principal y botón de voz: `#4F46E5` (índigo)
  - Acento de acción: `#F59E0B` (naranja cálido)
  - Completado: `#10B981` (verde)
  - Urgente / vencido: `#EF4444` (rojo coral)
  - Texto principal claro: `#1F2937` | Texto principal oscuro: `#F3F4F6`
- **Paleta de 8 colores para Cuadernos:**
  - Índigo (`#4F46E5`), Naranja (`#F59E0B`), Verde (`#10B981`), Rosa (`#EC4899`), Celeste (`#06B6D4`), Morado (`#8B5CF6`), Amarillo (`#EAB308`), Coral (`#F43F5E`).

---

## 📱 Pantallas y Flujos
1. **Hoy (Línea de tiempo principal):**
   - Saludo dinámico con modismos y voseo: *"Buenas, Daniel. Te quedan N cosas hoy"* (se adapta según la hora: Buen día / Buenas tardes / Buenas noches).
   - Próximos recordatorios arriba (destacados), recordatorios cumplidos tachados en verde tenue al final.
   - Cada tarjeta muestra la hora, título, barra lateral con el color del cuaderno y distintivos de notas, PDFs, enlaces y checklist.
   - Acciones rápidas: marcar hecho, aplazar 20 min y abrir cuaderno.

2. **Escuchando (Hoja inferior / Modal Bottom Sheet):**
   - Onda de sonido animada índigo (`WaveformVisualizer`) sensible al volumen y transcripción en vivo.
   - Tarjeta de confirmación interactiva: título, fecha, hora, aviso previo y sugerencia inteligente de cuaderno.
   - Barra de progreso con cuenta regresiva de 3 segundos para autoguardar, con botones de **"Deshacer"** y **"Editar"**.
   - Preguntas conversacionales por voz: si falta la hora, la app pregunta y habla: *¿A qué hora?* y espera la respuesta.

3. **Cuaderno del Recordatorio (`NotebookDetailScreen`):**
   - Banner superior con el color del cuaderno, título editable y estado.
   - 4 pestañas internas:
     - **Notas:** editor de notas con fecha y botón de dictado.
     - **Archivos:** subida de PDFs e imágenes con miniatura y apertura en visor del sistema.
     - **Apps y links:** accesos directos a Google Drive, Notion, Classroom, WhatsApp y navegador web.
     - **Checklist:** subtareas interactivas con casillas de verificación y barra de progreso.

4. **Cuadernos (Biblioteca):**
   - Cuadrícula de 2 columnas con tarjetas estilizadas en forma de cuaderno (lomo de color y conteos).
   - Filtros por materia/etiqueta (Robótica, Datos, Personal, Trading, etc.).
   - Sección de **Archivo** con recordatorios completados y búsqueda de texto.

5. **Calendario:**
   - Vistas Semanal y Mensual.
   - Días marcados con puntos de color del cuaderno correspondiente.
   - Lista detallada de recordatorios del día seleccionado.

6. **Ajustes:**
   - Métodos de activación: botón flotante, asistente del sistema y palabra de activación ("Oye Apunta" beta).
   - Voz e idioma en español (es-SV / es-419).
   - Alertas escalonadas (repite a los 5 y 10 minutos si no se marca como hecho).
   - Horario de "No molestar" (22:00 a 07:00).
   - Resumen matutino hablado mediante Text-to-Speech (TTS).
   - Respaldo / exportación en formato JSON y texto.
   - Configuración de privacidad ("Procesar voz en el dispositivo").

---

## 🛠️ Estructura del Proyecto
```
app/src/main/java/com/example/
├── MainActivity.kt
├── data/
│   ├── model/
│   │   └── Entities.kt (Notebook, Reminder, Note, Attachment, AppLink, ChecklistItem)
│   └── local/
│       ├── Daos.kt (ReminderDao, NotebookDao, NoteDao, etc.)
│       └── ApuntaDatabase.kt (Base de datos Room con datos iniciales)
├── voice/
│   ├── VoiceReminderParser.kt (Parseo en español es-SV con reglas de fechas/horas y comandos)
│   ├── SpeechManager.kt (Gestor de SpeechRecognizer con simulación y fallback)
│   └── TtsManager.kt (Síntesis de voz en español para preguntas y resúmenes)
├── notification/
│   └── NotificationHelper.kt (AlarmManager, canales y alertas escalonadas)
├── receiver/
│   └── ReminderNotificationReceiver.kt (Recepción de alarmas y acciones de notificación)
└── ui/
    ├── ApuntaApp.kt (Scaffold principal con botón flotante de micrófono)
    ├── components/
    │   ├── CountdownProgressBar.kt
    │   ├── NotebookCard.kt
    │   ├── ReminderCard.kt
    │   └── WaveformVisualizer.kt
    ├── screens/
    │   ├── CalendarScreen.kt
    │   ├── ListeningBottomSheet.kt
    │   ├── NotebookDetailScreen.kt
    │   ├── NotebooksScreen.kt
    │   ├── SettingsScreen.kt
    │   └── TodayScreen.kt
    └── theme/
        ├── Color.kt
        ├── Theme.kt
        └── Type.kt
```

---

## 🚀 Instrucciones para Ejecutar
1. Compilar y ejecutar en Android Studio o directamente mediante Gradle:
   ```bash
   gradle :app:assembleDebug
   ```
2. Para ejecutar las pruebas unitarias de parseo de voz:
   ```bash
   gradle :app:testDebugUnitTest
   ```
