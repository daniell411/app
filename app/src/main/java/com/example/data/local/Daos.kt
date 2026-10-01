package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AppLink
import com.example.data.model.Attachment
import com.example.data.model.ChecklistItem
import com.example.data.model.Note
import com.example.data.model.Notebook
import com.example.data.model.Reminder
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY datetime ASC")
    fun getAllReminders(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    suspend fun getReminderById(id: String): Reminder?

    @Query("SELECT * FROM reminders WHERE notebookId = :notebookId ORDER BY datetime ASC")
    fun getRemindersForNotebook(notebookId: String): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE datetime >= :startOfDay AND datetime <= :endOfDay ORDER BY datetime ASC")
    fun getRemindersForDateRange(startOfDay: Long, endOfDay: Long): Flow<List<Reminder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: Reminder)

    @Update
    suspend fun updateReminder(reminder: Reminder)

    @Delete
    suspend fun deleteReminder(reminder: Reminder)

    @Query("DELETE FROM reminders")
    suspend fun deleteAllReminders()

    @Query("DELETE FROM reminders WHERE isSample = 1")
    suspend fun deleteSampleReminders()

    @Query("UPDATE reminders SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("UPDATE reminders SET datetime = :newDateTime WHERE id = :id")
    suspend fun snoozeReminder(id: String, newDateTime: Long)

    @Query("SELECT * FROM reminders WHERE title LIKE '%' || :query || '%'")
    suspend fun searchReminders(query: String): List<Reminder>
}

@Dao
interface NotebookDao {
    @Query("SELECT * FROM notebooks ORDER BY createdAt DESC")
    fun getAllNotebooks(): Flow<List<Notebook>>

    @Query("SELECT * FROM notebooks WHERE id = :id LIMIT 1")
    suspend fun getNotebookById(id: String): Notebook?

    @Query("SELECT * FROM notebooks WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getNotebookByName(name: String): Notebook?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotebook(notebook: Notebook)

    @Update
    suspend fun updateNotebook(notebook: Notebook)

    @Delete
    suspend fun deleteNotebook(notebook: Notebook)

    @Query("DELETE FROM notebooks")
    suspend fun deleteAllNotebooks()

    @Query("DELETE FROM notebooks WHERE isSample = 1")
    suspend fun deleteSampleNotebooks()
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes")
    fun getAllNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE notebookId = :notebookId ORDER BY createdAt DESC")
    fun getNotesForNotebook(notebookId: String): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE notebookId = :notebookId")
    suspend fun getNotesSync(notebookId: String): List<Note>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    @Query("DELETE FROM notes")
    suspend fun deleteAllNotes()

    @Query("DELETE FROM notes WHERE isSample = 1")
    suspend fun deleteSampleNotes()

    @Query("SELECT * FROM notes WHERE text LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    suspend fun searchNotes(query: String): List<Note>
}

@Dao
interface AttachmentDao {
    @Query("SELECT * FROM attachments")
    fun getAllAttachments(): Flow<List<Attachment>>

    @Query("SELECT * FROM attachments WHERE notebookId = :notebookId ORDER BY createdAt DESC")
    fun getAttachmentsForNotebook(notebookId: String): Flow<List<Attachment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachment(attachment: Attachment)

    @Delete
    suspend fun deleteAttachment(attachment: Attachment)

    @Query("DELETE FROM attachments")
    suspend fun deleteAllAttachments()

    @Query("DELETE FROM attachments WHERE isSample = 1")
    suspend fun deleteSampleAttachments()
}

@Dao
interface AppLinkDao {
    @Query("SELECT * FROM app_links")
    fun getAllLinks(): Flow<List<AppLink>>

    @Query("SELECT * FROM app_links WHERE notebookId = :notebookId")
    fun getLinksForNotebook(notebookId: String): Flow<List<AppLink>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLink(link: AppLink)

    @Delete
    suspend fun deleteLink(link: AppLink)

    @Query("DELETE FROM app_links")
    suspend fun deleteAllLinks()

    @Query("DELETE FROM app_links WHERE isSample = 1")
    suspend fun deleteSampleLinks()
}

@Dao
interface ChecklistDao {
    @Query("SELECT * FROM checklist_items")
    fun getAllChecklistItems(): Flow<List<ChecklistItem>>

    @Query("SELECT * FROM checklist_items WHERE notebookId = :notebookId ORDER BY orderIndex ASC, id ASC")
    fun getChecklistForNotebook(notebookId: String): Flow<List<ChecklistItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ChecklistItem)

    @Update
    suspend fun updateItem(item: ChecklistItem)

    @Query("UPDATE checklist_items SET done = :done WHERE id = :id")
    suspend fun toggleDone(id: String, done: Boolean)

    @Delete
    suspend fun deleteItem(item: ChecklistItem)

    @Query("DELETE FROM checklist_items")
    suspend fun deleteAllChecklistItems()

    @Query("DELETE FROM checklist_items WHERE isSample = 1")
    suspend fun deleteSampleChecklistItems()
}
