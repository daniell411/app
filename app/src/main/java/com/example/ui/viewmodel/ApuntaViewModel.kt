package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ApuntaDatabase
import com.example.data.model.AppLink
import com.example.data.model.Attachment
import com.example.data.model.ChecklistItem
import com.example.data.model.Note
import com.example.data.model.Notebook
import com.example.data.model.NotebookWithCounts
import com.example.data.model.Reminder
import com.example.data.model.ReminderWithNotebook
import com.example.data.preferences.UserPreferences
import com.example.data.preferences.UserPreferencesRepository
import com.example.notification.NotificationHelper
import com.example.voice.ParsedVoiceResult
import com.example.voice.SpeechManager
import com.example.voice.SpeechState
import com.example.voice.TtsManager
import com.example.voice.VoiceCommandType
import com.example.voice.VoiceReminderParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

// Ordering required by spec: Hoy · Calendario · Cuadernos · Ajustes
enum class ApuntaTab {
    HOY,
    CALENDARIO,
    CUADERNOS,
    AJUSTES
}

class ApuntaViewModel(application: Application) : AndroidViewModel(application) {

    private val database = ApuntaDatabase.getDatabase(application)
    private val reminderDao = database.reminderDao()
    private val notebookDao = database.notebookDao()
    private val noteDao = database.noteDao()
    private val attachmentDao = database.attachmentDao()
    private val appLinkDao = database.appLinkDao()
    private val checklistDao = database.checklistDao()

    val speechManager = SpeechManager(application)
    val ttsManager = TtsManager(application)
    val preferencesRepo = UserPreferencesRepository(application)

    // DataStore User Preferences
    val userPreferences: StateFlow<UserPreferences> = preferencesRepo.userPreferencesFlow.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        UserPreferences()
    )

    // Current navigation state
    private val _currentTab = MutableStateFlow(ApuntaTab.HOY)
    val currentTab: StateFlow<ApuntaTab> = _currentTab.asStateFlow()

    private val _selectedNotebookId = MutableStateFlow<String?>(null)
    val selectedNotebookId: StateFlow<String?> = _selectedNotebookId.asStateFlow()

    // Listening bottom sheet state
    private val _isListeningSheetOpen = MutableStateFlow(false)
    val isListeningSheetOpen: StateFlow<Boolean> = _isListeningSheetOpen.asStateFlow()

    private val _voiceDraft = MutableStateFlow<ParsedVoiceResult?>(null)
    val voiceDraft: StateFlow<ParsedVoiceResult?> = _voiceDraft.asStateFlow()

    private val _conversationalQuestion = MutableStateFlow<String?>(null)
    val conversationalQuestion: StateFlow<String?> = _conversationalQuestion.asStateFlow()

    private val _voiceFeedback = MutableStateFlow<String?>(null)
    val voiceFeedback: StateFlow<String?> = _voiceFeedback.asStateFlow()

    // Filter for Hoy: "TODOS", "PENDIENTES", "COMPLETADOS"
    private val _todayFilter = MutableStateFlow("TODOS")
    val todayFilter: StateFlow<String> = _todayFilter.asStateFlow()

    // Search query for Notebooks & Notes
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow("Todos")
    val selectedCategoryFilter: StateFlow<String> = _selectedCategoryFilter.asStateFlow()

    // Selected date for Calendar view (epoch millis)
    private val _selectedCalendarDate = MutableStateFlow(System.currentTimeMillis())
    val selectedCalendarDate: StateFlow<Long> = _selectedCalendarDate.asStateFlow()

    // Raw flows
    val allNotebooks = notebookDao.getAllNotebooks().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val allReminders = reminderDao.getAllReminders().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    private data class NotebookContent(
        val notes: List<Note>,
        val attachments: List<Attachment>,
        val links: List<AppLink>,
        val checklists: List<ChecklistItem>
    )

    private val notebookContentFlow = combine(
        noteDao.getAllNotes(),
        attachmentDao.getAllAttachments(),
        appLinkDao.getAllLinks(),
        checklistDao.getAllChecklistItems()
    ) { notes, attachments, links, checklists ->
        NotebookContent(notes, attachments, links, checklists)
    }

    // Flow of Reminders with associated Notebook and true dynamic counts
    val todayReminders: StateFlow<List<ReminderWithNotebook>> = combine(
        reminderDao.getAllReminders(),
        notebookDao.getAllNotebooks(),
        notebookContentFlow
    ) { reminders, notebooks, content ->
        val notebookMap = notebooks.associateBy { it.id }
        val notesByNb = content.notes.groupBy { it.notebookId }
        val attachmentsByNb = content.attachments.groupBy { it.notebookId }
        val linksByNb = content.links.groupBy { it.notebookId }
        val checklistsByNb = content.checklists.groupBy { it.notebookId }

        val combined = reminders.map { reminder ->
            val nbId = reminder.notebookId
            val nb = nbId?.let { notebookMap[it] }
            val nbNotes = nbId?.let { notesByNb[it] } ?: emptyList()
            val nbAtt = nbId?.let { attachmentsByNb[it] } ?: emptyList()
            val nbLinks = nbId?.let { linksByNb[it] } ?: emptyList()
            val nbCheck = nbId?.let { checklistsByNb[it] } ?: emptyList()

            ReminderWithNotebook(
                reminder = reminder,
                notebook = nb,
                notesCount = nbNotes.size,
                attachmentsCount = nbAtt.size,
                appLinksCount = nbLinks.size,
                checklistCount = nbCheck.size,
                checklistDoneCount = nbCheck.count { it.done }
            )
        }

        // Orden de la spec: Atrasados arriba, pendientes por hora, completados al final
        val now = System.currentTimeMillis()
        val overdue = combined.filter { it.reminder.status != "DONE" && it.reminder.datetime < now }.sortedBy { it.reminder.datetime }
        val upcoming = combined.filter { it.reminder.status != "DONE" && it.reminder.datetime >= now }.sortedBy { it.reminder.datetime }
        val done = combined.filter { it.reminder.status == "DONE" }.sortedByDescending { it.reminder.datetime }
        overdue + upcoming + done
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Flow of Notebooks with actual item counts
    val notebooksWithCounts: StateFlow<List<NotebookWithCounts>> = combine(
        notebookDao.getAllNotebooks(),
        reminderDao.getAllReminders(),
        notebookContentFlow
    ) { notebooks, reminders, content ->
        val remindersByNb = reminders.groupBy { it.notebookId }
        val notesByNb = content.notes.groupBy { it.notebookId }
        val attachmentsByNb = content.attachments.groupBy { it.notebookId }
        val linksByNb = content.links.groupBy { it.notebookId }
        val checklistsByNb = content.checklists.groupBy { it.notebookId }

        notebooks.map { nb ->
            val nbReminders = remindersByNb[nb.id] ?: emptyList()
            val nbNotes = notesByNb[nb.id] ?: emptyList()
            val nbAtt = attachmentsByNb[nb.id] ?: emptyList()
            val nbLinks = linksByNb[nb.id] ?: emptyList()
            val nbCheck = checklistsByNb[nb.id] ?: emptyList()

            NotebookWithCounts(
                notebook = nb,
                remindersCount = nbReminders.size,
                notesCount = nbNotes.size,
                attachmentsCount = nbAtt.size,
                appLinksCount = nbLinks.size,
                checklistCount = nbCheck.size,
                checklistDoneCount = nbCheck.count { it.done }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Collect speech recognizer results
        viewModelScope.launch {
            speechManager.speechState.collect { state ->
                when (state) {
                    is SpeechState.FinalResult -> {
                        if (state.text.isNotBlank()) {
                            processSpokenText(state.text)
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    fun selectTab(tab: ApuntaTab) {
        _currentTab.value = tab
        _selectedNotebookId.value = null
    }

    fun openNotebook(notebookId: String) {
        _selectedNotebookId.value = notebookId
    }

    fun closeNotebook() {
        _selectedNotebookId.value = null
    }

    fun setTodayFilter(filter: String) {
        _todayFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategoryFilter(category: String) {
        _selectedCategoryFilter.value = category
    }

    fun setSelectedCalendarDate(dateMillis: Long) {
        _selectedCalendarDate.value = dateMillis
    }

    // Voice recognition & Listening sheet controls
    fun openListeningSheet() {
        _voiceDraft.value = null
        _conversationalQuestion.value = null
        _voiceFeedback.value = null
        _isListeningSheetOpen.value = true
        speechManager.startListening()
    }

    fun closeListeningSheet() {
        speechManager.stopListening()
        speechManager.reset()
        _isListeningSheetOpen.value = false
        _voiceDraft.value = null
        _conversationalQuestion.value = null
    }

    fun processSpokenText(text: String) {
        val currentQuestion = _conversationalQuestion.value
        val existingDraft = _voiceDraft.value

        // If app was waiting for an answer to "¿A qué hora?"
        if (currentQuestion != null && existingDraft != null) {
            val combinedText = "${existingDraft.title} $text"
            val reParsed = VoiceReminderParser.parse(combinedText, allNotebooks.value)
            _conversationalQuestion.value = null
            _voiceDraft.value = reParsed.copy(
                title = existingDraft.title,
                suggestedNotebookId = existingDraft.suggestedNotebookId,
                suggestedNotebookName = existingDraft.suggestedNotebookName,
                isMissingTime = false
            )
            return
        }

        val parsed = VoiceReminderParser.parse(text, allNotebooks.value)

        when (parsed.commandType) {
            VoiceCommandType.CREATE_REMINDER -> {
                if (parsed.isMissingTime && parsed.title.isNotBlank()) {
                    _voiceDraft.value = parsed
                    _conversationalQuestion.value = "¿A qué hora querés que te recuerde?"
                    ttsManager.speak("¿A qué hora?")
                } else {
                    _voiceDraft.value = parsed
                    _conversationalQuestion.value = null
                }
            }
            VoiceCommandType.SNOOZE_REMINDER -> {
                executeSnoozeCommand(parsed.commandParam.toIntOrNull() ?: 20)
            }
            VoiceCommandType.MARK_DONE -> {
                executeMarkDoneCommand()
            }
            VoiceCommandType.ADD_NOTE -> {
                executeAddNoteCommand(parsed.commandParam)
            }
            VoiceCommandType.SEARCH_NOTES -> {
                executeSearchNotesCommand(parsed.commandParam)
            }
            VoiceCommandType.UNKNOWN -> {}
        }
    }

    private fun executeSnoozeCommand(minutes: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val nextUpcoming = todayReminders.value.firstOrNull { it.reminder.status != "DONE" }
            if (nextUpcoming != null) {
                val newTime = System.currentTimeMillis() + (minutes * 60 * 1000L)
                reminderDao.snoozeReminder(nextUpcoming.reminder.id, newTime)
                NotificationHelper.scheduleReminder(getApplication(), nextUpcoming.reminder.copy(datetime = newTime))
                _voiceFeedback.value = "Listo: aplazado $minutes minutos"
                ttsManager.speak("Aplazado $minutes minutos")
            } else {
                _voiceFeedback.value = "No tenés recordatorios pendientes para aplazar"
                ttsManager.speak("No tenés recordatorios pendientes para aplazar")
            }
        }
    }

    private fun executeMarkDoneCommand() {
        viewModelScope.launch(Dispatchers.IO) {
            val nextUpcoming = todayReminders.value.firstOrNull { it.reminder.status != "DONE" }
            if (nextUpcoming != null) {
                reminderDao.updateStatus(nextUpcoming.reminder.id, "DONE")
                _voiceFeedback.value = "Marcado como hecho: ${nextUpcoming.reminder.title}"
                ttsManager.speak("Marcado como hecho")
            } else {
                _voiceFeedback.value = "No hay recordatorios pendientes"
            }
        }
    }

    private fun executeAddNoteCommand(noteText: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val targetNotebook = _selectedNotebookId.value?.let { notebookDao.getNotebookById(it) }
                ?: allNotebooks.value.firstOrNull()

            if (targetNotebook != null && noteText.isNotBlank()) {
                val note = Note(
                    notebookId = targetNotebook.id,
                    text = noteText
                )
                noteDao.insertNote(note)
                _voiceFeedback.value = "Nota agregada en ${targetNotebook.name}: $noteText"
                ttsManager.speak("Nota agregada al cuaderno ${targetNotebook.name}")
            }
        }
    }

    private fun executeSearchNotesCommand(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val results = noteDao.searchNotes(query)
            if (results.isNotEmpty()) {
                val firstNote = results.first()
                val nb = notebookDao.getNotebookById(firstNote.notebookId)
                val reply = "En ${nb?.name ?: "tu cuaderno"} anotaste: ${firstNote.text}"
                _voiceFeedback.value = reply
                ttsManager.speak(reply)
            } else {
                val reply = "No encontré notas sobre $query"
                _voiceFeedback.value = reply
                ttsManager.speak(reply)
            }
        }
    }

    fun confirmAndSaveDraft(
        title: String,
        timestamp: Long,
        leadTimeMinutes: Int,
        notebookId: String?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            var targetNotebookId = notebookId

            // If no notebook exists or is selected, create or assign default
            if (targetNotebookId == null) {
                val existing = allNotebooks.value.firstOrNull()
                targetNotebookId = existing?.id ?: run {
                    val newNb = Notebook(
                        name = "General",
                        color = "#4F46E5",
                        label = "General"
                    )
                    notebookDao.insertNotebook(newNb)
                    newNb.id
                }
            }

            val newReminder = Reminder(
                id = UUID.randomUUID().toString(),
                title = title.ifBlank { "Nuevo recordatorio" },
                datetime = timestamp,
                leadTimeMinutes = leadTimeMinutes,
                repeatRule = "NONE",
                status = "PENDING",
                notebookId = targetNotebookId,
                escalated = userPreferences.value.staggeredAlerts
            )

            reminderDao.insertReminder(newReminder)

            val nb = targetNotebookId?.let { notebookDao.getNotebookById(it) }
            NotificationHelper.scheduleReminder(
                context = getApplication(),
                reminder = newReminder,
                notebookName = nb?.name,
                colorHex = nb?.color
            )

            closeListeningSheet()
        }
    }

    fun toggleReminderDone(reminderId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val reminder = reminderDao.getReminderById(reminderId) ?: return@launch
            val nextStatus = if (reminder.status == "DONE") "PENDING" else "DONE"
            reminderDao.updateStatus(reminderId, nextStatus)
        }
    }

    fun snoozeReminder(reminderId: String, minutes: Int = 20) {
        viewModelScope.launch(Dispatchers.IO) {
            val newTime = System.currentTimeMillis() + (minutes * 60 * 1000L)
            reminderDao.snoozeReminder(reminderId, newTime)
            val updated = reminderDao.getReminderById(reminderId)
            if (updated != null) {
                NotificationHelper.scheduleReminder(getApplication(), updated)
            }
        }
    }

    fun createNotebook(name: String, colorHex: String, label: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val newNb = Notebook(
                name = name,
                color = colorHex,
                label = label.ifBlank { name }
            )
            notebookDao.insertNotebook(newNb)
        }
    }

    // Cuaderno tabs content flows
    fun getNotesForNotebook(notebookId: String) = noteDao.getNotesForNotebook(notebookId)
    fun getAttachmentsForNotebook(notebookId: String) = attachmentDao.getAttachmentsForNotebook(notebookId)
    fun getAppLinksForNotebook(notebookId: String) = appLinkDao.getLinksForNotebook(notebookId)
    fun getChecklistForNotebook(notebookId: String) = checklistDao.getChecklistForNotebook(notebookId)

    fun addNoteToNotebook(notebookId: String, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            noteDao.insertNote(
                Note(
                    notebookId = notebookId,
                    text = text.trim()
                )
            )
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch(Dispatchers.IO) {
            noteDao.deleteNote(note)
        }
    }

    fun addAttachmentToNotebook(notebookId: String, type: String, uri: String, name: String, size: String) {
        viewModelScope.launch(Dispatchers.IO) {
            attachmentDao.insertAttachment(
                Attachment(
                    notebookId = notebookId,
                    type = type,
                    uri = uri,
                    name = name,
                    sizeFormatted = size
                )
            )
        }
    }

    fun addAppLinkToNotebook(notebookId: String, label: String, url: String, icon: String) {
        viewModelScope.launch(Dispatchers.IO) {
            appLinkDao.insertLink(
                AppLink(
                    notebookId = notebookId,
                    label = label,
                    url = url,
                    icon = icon
                )
            )
        }
    }

    fun addChecklistItem(notebookId: String, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            checklistDao.insertItem(
                ChecklistItem(
                    notebookId = notebookId,
                    text = text.trim(),
                    done = false
                )
            )
        }
    }

    fun toggleChecklistItem(item: ChecklistItem) {
        viewModelScope.launch(Dispatchers.IO) {
            checklistDao.toggleDone(item.id, !item.done)
        }
    }

    fun deleteChecklistItem(item: ChecklistItem) {
        viewModelScope.launch(Dispatchers.IO) {
            checklistDao.deleteItem(item)
        }
    }

    fun updateReminderDetails(reminder: Reminder) {
        viewModelScope.launch(Dispatchers.IO) {
            reminderDao.updateReminder(reminder)
        }
    }

    // Spoken Morning Summary feature (Resumen matutino hablado)
    fun speakMorningSummary() {
        val user = userPreferences.value.userName.ifBlank { "amigo" }
        val pendingCount = todayReminders.value.count { it.reminder.status != "DONE" }
        val greeting = getGreetingTime()

        val text = if (pendingCount == 0) {
            "$greeting, $user. Tenés el día libre, no hay recordatorios pendientes para hoy."
        } else {
            val titles = todayReminders.value
                .filter { it.reminder.status != "DONE" }
                .take(3)
                .joinToString(", ") { it.reminder.title }
            "$greeting, $user. Hoy tenés $pendingCount cosas pendientes: $titles."
        }

        ttsManager.speak(text)
    }

    fun getGreetingTime(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..11 -> "Buen día"
            in 12..18 -> "Buenas tardes"
            else -> "Buenas noches"
        }
    }

    // Sample data loaders
    fun loadSampleData() {
        viewModelScope.launch(Dispatchers.IO) {
            ApuntaDatabase.seedSampleData(database)
        }
    }

    fun clearSampleData() {
        viewModelScope.launch(Dispatchers.IO) {
            ApuntaDatabase.clearSampleData(database)
        }
    }

    // Data export functions
    fun generateExportText(): String {
        val reminders = allReminders.value
        val notebooks = allNotebooks.value
        val sb = StringBuilder()
        sb.append("=== APUNTA - RESPALDO DE RECORDATORIOS Y CUADERNOS ===\n")
        sb.append("Fecha de exportación: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}\n\n")

        sb.append("--- RECORDATORIOS (${reminders.size}) ---\n")
        if (reminders.isEmpty()) {
            sb.append("Sin recordatorios registrados.\n")
        } else {
            reminders.forEach { r ->
                val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(r.datetime))
                val state = if (r.status == "DONE") "[COMPLETADO]" else "[PENDIENTE]"
                sb.append("• $state ${r.title} - $dateStr\n")
            }
        }

        sb.append("\n--- CUADERNOS (${notebooks.size}) ---\n")
        if (notebooks.isEmpty()) {
            sb.append("Sin cuadernos creados.\n")
        } else {
            notebooks.forEach { nb ->
                sb.append("• Cuaderno: ${nb.name} (Categoría: ${nb.label})\n")
            }
        }
        return sb.toString()
    }

    fun generateExportJson(): String {
        val reminders = allReminders.value
        val notebooks = allNotebooks.value
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"version\": 1,\n")
        sb.append("  \"exportedAt\": ${System.currentTimeMillis()},\n")
        sb.append("  \"notebooks\": [\n")
        notebooks.forEachIndexed { i, nb ->
            sb.append("    {\"id\": \"${nb.id}\", \"name\": \"${escapeJson(nb.name)}\", \"color\": \"${nb.color}\", \"label\": \"${escapeJson(nb.label)}\"}")
            if (i < notebooks.size - 1) sb.append(",")
            sb.append("\n")
        }
        sb.append("  ],\n")
        sb.append("  \"reminders\": [\n")
        reminders.forEachIndexed { i, r ->
            sb.append("    {\"id\": \"${r.id}\", \"title\": \"${escapeJson(r.title)}\", \"datetime\": ${r.datetime}, \"status\": \"${r.status}\", \"notebookId\": \"${r.notebookId ?: ""}\"}")
            if (i < reminders.size - 1) sb.append(",")
            sb.append("\n")
        }
        sb.append("  ]\n")
        sb.append("}")
        return sb.toString()
    }

    private fun escapeJson(value: String): String {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
    }

    // Settings modifiers backed by DataStore
    fun completeOnboarding(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            preferencesRepo.completeOnboarding(name)
        }
    }

    fun setUserName(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            preferencesRepo.setUserName(name)
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch(Dispatchers.IO) {
            preferencesRepo.setThemeMode(mode)
        }
    }

    fun setDefaultLeadTimeMinutes(minutes: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            preferencesRepo.setDefaultLeadTimeMinutes(minutes)
        }
    }

    fun setStaggeredAlerts(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            preferencesRepo.setStaggeredAlerts(enabled)
        }
    }

    fun setMorningSummaryEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            preferencesRepo.setMorningSummaryEnabled(enabled)
        }
    }

    fun setMorningSummaryTime(time: String) {
        viewModelScope.launch(Dispatchers.IO) {
            preferencesRepo.setMorningSummaryTime(time)
        }
    }

    fun setOnDeviceVoiceOnly(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            preferencesRepo.setOnDeviceVoiceOnly(enabled)
        }
    }

    fun setWakeWordEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            preferencesRepo.setWakeWordEnabled(enabled)
        }
    }

    fun setVoiceDialect(dialect: String) {
        viewModelScope.launch(Dispatchers.IO) {
            preferencesRepo.setVoiceDialect(dialect)
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechManager.stopListening()
        ttsManager.shutdown()
    }
}
