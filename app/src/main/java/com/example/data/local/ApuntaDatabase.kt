package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.AppLink
import com.example.data.model.Attachment
import com.example.data.model.ChecklistItem
import com.example.data.model.Note
import com.example.data.model.Notebook
import com.example.data.model.Reminder
import java.util.Calendar

@Database(
    entities = [
        Notebook::class,
        Reminder::class,
        Note::class,
        Attachment::class,
        AppLink::class,
        ChecklistItem::class
    ],
    version = 2,
    exportSchema = false
)
abstract class ApuntaDatabase : RoomDatabase() {
    abstract fun notebookDao(): NotebookDao
    abstract fun reminderDao(): ReminderDao
    abstract fun noteDao(): NoteDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun appLinkDao(): AppLinkDao
    abstract fun checklistDao(): ChecklistDao

    companion object {
        @Volatile
        private var INSTANCE: ApuntaDatabase? = null

        fun getDatabase(context: Context): ApuntaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ApuntaDatabase::class.java,
                    "apunta_database"
                )
                    .fallbackToDestructiveMigration()
                    // Starts empty without auto-seeding
                    .build()
                INSTANCE = instance
                instance
            }
        }

        suspend fun clearAllData(database: ApuntaDatabase) {
            database.reminderDao().deleteAllReminders()
            database.noteDao().deleteAllNotes()
            database.attachmentDao().deleteAllAttachments()
            database.appLinkDao().deleteAllLinks()
            database.checklistDao().deleteAllChecklistItems()
            database.notebookDao().deleteAllNotebooks()
        }

        suspend fun clearSampleData(database: ApuntaDatabase) {
            database.reminderDao().deleteSampleReminders()
            database.noteDao().deleteSampleNotes()
            database.attachmentDao().deleteSampleAttachments()
            database.appLinkDao().deleteSampleLinks()
            database.checklistDao().deleteSampleChecklistItems()
            database.notebookDao().deleteSampleNotebooks()
        }

        suspend fun seedSampleData(database: ApuntaDatabase) {
            val notebookDao = database.notebookDao()
            val reminderDao = database.reminderDao()
            val noteDao = database.noteDao()
            val attachmentDao = database.attachmentDao()
            val appLinkDao = database.appLinkDao()
            val checklistDao = database.checklistDao()

            val trabajoId = "nb-trabajo"
            val estudioId = "nb-estudio"
            val personalId = "nb-personal"

            // Cuadernos de ejemplo
            notebookDao.insertNotebook(
                Notebook(
                    id = trabajoId,
                    name = "Trabajo",
                    color = "#4F46E5",
                    label = "Trabajo",
                    isSample = true
                )
            )
            notebookDao.insertNotebook(
                Notebook(
                    id = estudioId,
                    name = "Estudio",
                    color = "#06B6D4",
                    label = "Estudio",
                    isSample = true
                )
            )
            notebookDao.insertNotebook(
                Notebook(
                    id = personalId,
                    name = "Personal",
                    color = "#F59E0B",
                    label = "Personal",
                    isSample = true
                )
            )

            val cal = Calendar.getInstance()

            // Recordatorio 1: Hoy a las 15:00
            cal.set(Calendar.HOUR_OF_DAY, 15)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            val r1Id = "rem-sample-1"
            reminderDao.insertReminder(
                Reminder(
                    id = r1Id,
                    title = "Revisión trimestral de proyectos",
                    datetime = cal.timeInMillis,
                    leadTimeMinutes = 30,
                    repeatRule = "NONE",
                    status = "PENDING",
                    notebookId = trabajoId,
                    escalated = true,
                    isSample = true
                )
            )
            noteDao.insertNote(
                Note(
                    notebookId = trabajoId,
                    reminderId = r1Id,
                    text = "Preparar métricas de avance y resumen ejecutivo para el equipo.",
                    isSample = true
                )
            )
            appLinkDao.insertLink(
                AppLink(
                    notebookId = trabajoId,
                    label = "Google Drive",
                    url = "https://drive.google.com",
                    icon = "drive",
                    isSample = true
                )
            )
            checklistDao.insertItem(
                ChecklistItem(
                    notebookId = trabajoId,
                    text = "Verificar datos consolidados",
                    done = true,
                    orderIndex = 0,
                    isSample = true
                )
            )
            checklistDao.insertItem(
                ChecklistItem(
                    notebookId = trabajoId,
                    text = "Generar gráfico de rendimiento",
                    done = false,
                    orderIndex = 1,
                    isSample = true
                )
            )

            // Recordatorio 2: Mañana a las 10:00
            val cal2 = Calendar.getInstance()
            cal2.add(Calendar.DAY_OF_YEAR, 1)
            cal2.set(Calendar.HOUR_OF_DAY, 10)
            cal2.set(Calendar.MINUTE, 0)
            val r2Id = "rem-sample-2"
            reminderDao.insertReminder(
                Reminder(
                    id = r2Id,
                    title = "Repasar para el examen final",
                    datetime = cal2.timeInMillis,
                    leadTimeMinutes = 60,
                    repeatRule = "NONE",
                    status = "PENDING",
                    notebookId = estudioId,
                    escalated = true,
                    isSample = true
                )
            )
            noteDao.insertNote(
                Note(
                    notebookId = estudioId,
                    reminderId = r2Id,
                    text = "Revisar los capítulos 4, 5 y los resúmenes clave de clase.",
                    isSample = true
                )
            )
            checklistDao.insertItem(
                ChecklistItem(
                    notebookId = estudioId,
                    text = "Resolver ejercicios prácticos",
                    done = false,
                    orderIndex = 0,
                    isSample = true
                )
            )

            // Recordatorio 3: Hoy a las 18:30
            val cal3 = Calendar.getInstance()
            cal3.set(Calendar.HOUR_OF_DAY, 18)
            cal3.set(Calendar.MINUTE, 30)
            val r3Id = "rem-sample-3"
            reminderDao.insertReminder(
                Reminder(
                    id = r3Id,
                    title = "Comprar víveres para la semana",
                    datetime = cal3.timeInMillis,
                    leadTimeMinutes = 15,
                    repeatRule = "NONE",
                    status = "PENDING",
                    notebookId = personalId,
                    isSample = true
                )
            )
            checklistDao.insertItem(
                ChecklistItem(
                    notebookId = personalId,
                    text = "Frutas y verduras frescas",
                    done = true,
                    orderIndex = 0,
                    isSample = true
                )
            )
            checklistDao.insertItem(
                ChecklistItem(
                    notebookId = personalId,
                    text = "Café y avena",
                    done = false,
                    orderIndex = 1,
                    isSample = true
                )
            )

            // Recordatorio 4: Completado
            val cal4 = Calendar.getInstance()
            cal4.set(Calendar.HOUR_OF_DAY, 8)
            cal4.set(Calendar.MINUTE, 30)
            val r4Id = "rem-sample-4"
            reminderDao.insertReminder(
                Reminder(
                    id = r4Id,
                    title = "Organizar el escritorio y notas",
                    datetime = cal4.timeInMillis,
                    leadTimeMinutes = 0,
                    repeatRule = "NONE",
                    status = "DONE",
                    notebookId = personalId,
                    isSample = true
                )
            )
        }
    }
}
