package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "notebooks")
data class Notebook(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val color: String, // hex, e.g. #4F46E5
    val label: String = "", // e.g. Trabajo, Estudio, Personal
    val isSample: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = Notebook::class,
            parentColumns = ["id"],
            childColumns = ["notebookId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["notebookId"])]
)
data class Reminder(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val datetime: Long, // epoch millis
    val leadTimeMinutes: Int = 0, // e.g., 60 for 1 hour before
    val repeatRule: String = "NONE", // NONE, DAILY, WEEKLY, MONTHLY
    val status: String = "PENDING", // PENDING, IN_PROGRESS, DONE
    val notebookId: String? = null,
    val escalated: Boolean = false,
    val isSample: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val locationTrigger: String = ""
)

@Entity(
    tableName = "notes",
    indices = [Index(value = ["notebookId"]), Index(value = ["reminderId"])]
)
data class Note(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val notebookId: String,
    val reminderId: String? = null,
    val text: String,
    val isSample: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "attachments",
    indices = [Index(value = ["notebookId"])]
)
data class Attachment(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val notebookId: String,
    val type: String, // pdf, image, doc
    val uri: String,
    val name: String,
    val sizeFormatted: String = "1.2 MB",
    val isSample: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "app_links",
    indices = [Index(value = ["notebookId"])]
)
data class AppLink(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val notebookId: String,
    val label: String, // Drive, Notion, Classroom, WhatsApp, Web
    val url: String, // url or scheme
    val icon: String, // "drive", "notion", "classroom", "whatsapp", "web"
    val isSample: Boolean = false
)

@Entity(
    tableName = "checklist_items",
    indices = [Index(value = ["notebookId"])]
)
data class ChecklistItem(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val notebookId: String,
    val text: String,
    val done: Boolean = false,
    val orderIndex: Int = 0,
    val isSample: Boolean = false
)

data class ReminderWithNotebook(
    val reminder: Reminder,
    val notebook: Notebook? = null,
    val notesCount: Int = 0,
    val attachmentsCount: Int = 0,
    val appLinksCount: Int = 0,
    val checklistCount: Int = 0,
    val checklistDoneCount: Int = 0
)

data class NotebookWithCounts(
    val notebook: Notebook,
    val remindersCount: Int = 0,
    val notesCount: Int = 0,
    val attachmentsCount: Int = 0,
    val appLinksCount: Int = 0,
    val checklistCount: Int = 0,
    val checklistDoneCount: Int = 0
)
