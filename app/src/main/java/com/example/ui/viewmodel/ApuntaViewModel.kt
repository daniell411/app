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
import java.util.Calendar
import java.util.UUID

enum class ApuntaTab {
    HOY,
    CUADERNOS,
    CALENDARIO,
    AJUSTES
}

class ApuntaViewModel(application: Application) : AndroidViewModel(application) {

    private val database = ApuntaDatabase.getDatabase(application, viewModelScope)
    private val reminderDao = database.reminderDao()
    private val notebookDao = database.notebookDao()
    private val noteDao = database.noteDao()
    private val attachmentDao = database.attachmentDao()
    private val appLinkDao = database.appLinkDao()
    private val checklistDao = database.checklistDao()

    val speechManager = SpeechManager(application)
    val ttsManager = TtsManager(application)

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

    // Settings State
    private val _userName = MutableStateFlow("Daniel")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _themeMode = MutableStateFlow("SYSTEM") // SYSTEM, LIGHT, DARK
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _staggeredAlerts = MutableStateFlow(true)
    val staggeredAlerts: StateFlow<Boolean> = _staggeredAlerts.asStateFlow()

    private val _morningSummaryEnabled = MutableStateFlow(true)
    val morningSummaryEnabled: StateFlow<Boolean> = _morningSummaryEnabled.asStateFlow()

    private val _morningSummaryTime = MutableStateFlow("08:00")
    val morningSummaryTime: StateFlow<String> = _morningSummaryTime.asStateFlow()

    private val _onDeviceVoiceOnly = MutableStateFlow(true)
    val onDeviceVoiceOnly: StateFlow<Boolean> = _onDeviceVoiceOnly.asStateFlow()

    private val _wakeWordEnabled = MutableStateFlow(false)
    val wakeWordEnabled: StateFlow<Boolean> = _wakeWordEnabled.asStateFlow()

    // Search query for Notebooks & Archive
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

    // Flow of Reminders with associated Notebook and counts for Today's timeline
    val todayReminders: StateFlow<List<ReminderWithNotebook>> = combine(
        reminderDao.getAllReminders(),
        notebookDao.getAllNotebooks()
    ) { reminders, notebooks ->
        val notebookMap = notebooks.associateBy { it.id }

        // Filter and sort for Today:
        // Upcoming/in-progress first (ordered by time), done items crossed-out and at the end
        val combined = reminders.map { reminder ->
            val nb = reminder.notebookId?.let { notebookMap[it] }
            ReminderWithNotebook(
                reminder = reminder,
                notebook = nb,
                notesCount = 1, // standard preview count or enriched
                attachmentsCount = if (reminder.title.contains("informe", ignoreCase = true) || reminder.title.contains("datos", ignoreCase = true)) 1 else 0,
                appLinksCount = if (reminder.notebookId == "nb-robotica") 2 else if (reminder.notebookId == "nb-datos") 1 else 0,
                checklistCount = if (reminder.notebookId == "nb-robotica") 3 else if (reminder.notebookId == "nb-personal") 2 else 0,
                checklistDoneCount = if (reminder.notebookId == "nb-robotica") 1 else 0
            )
        }

        val pending = combined.filter { it.reminder.status != "DONE" }.sortedBy { it.reminder.datetime }
        val done = combined.filter { it.reminder.status == "DONE" }.sortedBy { it.reminder.datetime }
        pending + done
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Flow of Notebooks with counts
    val notebooksWithCounts: StateFlow<List<NotebookWithCounts>> = combine(
        notebookDao.getAllNotebooks(),
        reminderDao.getAllReminders()
    ) { notebooks, reminders ->
        notebooks.map { nb ->
            val countRem = reminders.count { it.notebookId == nb.id }
            val countNotes = if (nb.id == "nb-robotica") 2 else if (nb.id == "nb-datos") 2 else 1
            val countAtt = if (nb.id == "nb-robotica") 1 else if (nb.id == "nb-datos") 1 else 0
            val countLinks = if (nb.id == "nb-robotica") 2 else if (nb.id == "nb-datos") 1 else 0
            val countCheck = if (nb.id == "nb-robotica") 3 else if (nb.id == "nb-personal") 2 else 1
            val countDone = if (nb.id == "nb-robotica") 1 else 0

            NotebookWithCounts(
                notebook = nb,
                remindersCount = countRem,
                notesCount = countNotes,
                attachmentsCount = countAtt,
                appLinksCount = countLinks,
                checklistCount = countCheck,
                checklistDoneCount = countDone
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
                    // Conversational voice question: ask "¿A qué hora?"
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
                escalated = _staggeredAlerts.value
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
        val user = _userName.value
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

    private fun getGreetingTime(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..11 -> "Buen día"
            in 12..18 -> "Buenas tardes"
            else -> "Buenas noches"
        }
    }

    // Settings modifiers
    fun setUserName(name: String) {
        _userName.value = name
    }

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
    }

    fun setStaggeredAlerts(enabled: Boolean) {
        _staggeredAlerts.value = enabled
    }

    fun setMorningSummaryEnabled(enabled: Boolean) {
        _morningSummaryEnabled.value = enabled
    }

    fun setMorningSummaryTime(time: String) {
        _morningSummaryTime.value = time
    }

    fun setOnDeviceVoiceOnly(enabled: Boolean) {
        _onDeviceVoiceOnly.value = enabled
    }

    fun setWakeWordEnabled(enabled: Boolean) {
        _wakeWordEnabled.value = enabled
    }

    override fun onCleared() {
        super.onCleared()
        speechManager.stopListening()
        ttsManager.shutdown()
    }
}
