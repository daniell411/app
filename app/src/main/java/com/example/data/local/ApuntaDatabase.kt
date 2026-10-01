package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.AppLink
import com.example.data.model.Attachment
import com.example.data.model.ChecklistItem
import com.example.data.model.Note
import com.example.data.model.Notebook
import com.example.data.model.Reminder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
    version = 1,
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

        fun getDatabase(context: Context, scope: CoroutineScope): ApuntaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ApuntaDatabase::class.java,
                    "apunta_database"
                )
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(private val scope: CoroutineScope) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        seedInitialData(database)
                    }
                }
            }
        }

        suspend fun seedInitialData(database: ApuntaDatabase) {
            val notebookDao = database.notebookDao()
            val reminderDao = database.reminderDao()
            val noteDao = database.noteDao()
            val attachmentDao = database.attachmentDao()
            val appLinkDao = database.appLinkDao()
            val checklistDao = database.checklistDao()

            val roboticaId = "nb-robotica"
            val datosId = "nb-datos"
            val personalId = "nb-personal"

            val cal = Calendar.getInstance()

            // 1. Notebooks
            notebookDao.insertNotebook(
                Notebook(
                    id = roboticaId,
                    name = "Robótica",
                    color = "#4F46E5", // Índigo
                    label = "Robótica"
                )
            )
            notebookDao.insertNotebook(
                Notebook(
                    id = datosId,
                    name = "Datos",
                    color = "#06B6D4", // Celeste
                    label = "Datos"
                )
            )
            notebookDao.insertNotebook(
                Notebook(
                    id = personalId,
                    name = "Personal",
                    color = "#F59E0B", // Naranja
                    label = "Personal"
                )
            )

            // 2. Reminders
            // Reminder 1: Today at 3:00 PM - Entregar informe de robótica a Eduardo
            cal.set(Calendar.HOUR_OF_DAY, 15)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            val timeReport = cal.timeInMillis

            val r1Id = "rem-1"
            reminderDao.insertReminder(
                Reminder(
                    id = r1Id,
                    title = "Entregar informe de robótica a Eduardo",
                    datetime = timeReport,
                    leadTimeMinutes = 60,
                    repeatRule = "NONE",
                    status = "PENDING",
                    notebookId = roboticaId,
                    escalated = true
                )
            )
            noteDao.insertNote(
                Note(
                    notebookId = roboticaId,
                    reminderId = r1Id,
                    text = "Verificar calibración de motores paso a paso y conclusiones del laboratorio con Eduardo antes de enviar."
                )
            )
            attachmentDao.insertAttachment(
                Attachment(
                    notebookId = roboticaId,
                    type = "pdf",
                    uri = "content://sample/informe_sensores_v2.pdf",
                    name = "informe_sensores_v2.pdf",
                    sizeFormatted = "2.4 MB"
                )
            )
            appLinkDao.insertLink(
                AppLink(
                    notebookId = roboticaId,
                    label = "Google Drive",
                    url = "https://drive.google.com",
                    icon = "drive"
                )
            )
            appLinkDao.insertLink(
                AppLink(
                    notebookId = roboticaId,
                    label = "Notion Lab",
                    url = "https://notion.so",
                    icon = "notion"
                )
            )
            checklistDao.insertItem(
                ChecklistItem(
                    notebookId = roboticaId,
                    text = "Revisar gráficos de par motor",
                    done = true,
                    orderIndex = 0
                )
            )
            checklistDao.insertItem(
                ChecklistItem(
                    notebookId = roboticaId,
                    text = "Imprimir conclusiones en formato IEEE",
                    done = false,
                    orderIndex = 1
                )
            )
            checklistDao.insertItem(
                ChecklistItem(
                    notebookId = roboticaId,
                    text = "Enviar copia en PDF a Eduardo",
                    done = false,
                    orderIndex = 2
                )
            )

            // Reminder 2: Tomorrow at 10:00 AM - Examen parcial de Minería de Datos
            val cal2 = Calendar.getInstance()
            cal2.add(Calendar.DAY_OF_YEAR, 1)
            cal2.set(Calendar.HOUR_OF_DAY, 10)
            cal2.set(Calendar.MINUTE, 0)
            val r2Id = "rem-2"
            reminderDao.insertReminder(
                Reminder(
                    id = r2Id,
                    title = "Examen parcial de Minería de Datos",
                    datetime = cal2.timeInMillis,
                    leadTimeMinutes = 120,
                    repeatRule = "NONE",
                    status = "PENDING",
                    notebookId = datosId,
                    escalated = true
                )
            )
            noteDao.insertNote(
                Note(
                    notebookId = datosId,
                    reminderId = r2Id,
                    text = "Temas clave: Árboles de decisión, Random Forest, métricas de evaluación (F1-score, AUC-ROC) y preprocesamiento de outliers."
                )
            )
            attachmentDao.insertAttachment(
                Attachment(
                    notebookId = datosId,
                    type = "pdf",
                    uri = "content://sample/guia_mineria_datos.pdf",
                    name = "guia_mineria_datos.pdf",
                    sizeFormatted = "3.8 MB"
                )
            )
            appLinkDao.insertLink(
                AppLink(
                    notebookId = datosId,
                    label = "Google Classroom",
                    url = "https://classroom.google.com",
                    icon = "classroom"
                )
            )
            checklistDao.insertItem(
                ChecklistItem(
                    notebookId = datosId,
                    text = "Resolver ejercicios de prueba en Python",
                    done = true,
                    orderIndex = 0
                )
            )
            checklistDao.insertItem(
                ChecklistItem(
                    notebookId = datosId,
                    text = "Preparar hoja de fórmulas permitida",
                    done = false,
                    orderIndex = 1
                )
            )

            // Reminder 3: Today at 6:30 PM - Pagar la factura de energía eléctrica
            val cal3 = Calendar.getInstance()
            cal3.set(Calendar.HOUR_OF_DAY, 18)
            cal3.set(Calendar.MINUTE, 30)
            val r3Id = "rem-3"
            reminderDao.insertReminder(
                Reminder(
                    id = r3Id,
                    title = "Pagar la factura de energía eléctrica",
                    datetime = cal3.timeInMillis,
                    leadTimeMinutes = 30,
                    repeatRule = "MONTHLY",
                    status = "PENDING",
                    notebookId = personalId
                )
            )
            checklistDao.insertItem(
                ChecklistItem(
                    notebookId = personalId,
                    text = "Ingresar a la app del banco",
                    done = false,
                    orderIndex = 0
                )
            )
            checklistDao.insertItem(
                ChecklistItem(
                    notebookId = personalId,
                    text = "Descargar comprobante en PDF",
                    done = false,
                    orderIndex = 1
                )
            )

            // Reminder 4: Today at 11:30 AM - Comprar sensores ultrasónicos HC-SR04
            val cal4 = Calendar.getInstance()
            cal4.set(Calendar.HOUR_OF_DAY, 11)
            cal4.set(Calendar.MINUTE, 30)
            val r4Id = "rem-4"
            reminderDao.insertReminder(
                Reminder(
                    id = r4Id,
                    title = "Comprar sensores ultrasónicos HC-SR04",
                    datetime = cal4.timeInMillis,
                    leadTimeMinutes = 15,
                    repeatRule = "NONE",
                    status = "IN_PROGRESS",
                    notebookId = roboticaId
                )
            )
            noteDao.insertNote(
                Note(
                    notebookId = roboticaId,
                    reminderId = r4Id,
                    text = "Comprar 4 unidades en la tienda de electrónica. Pedir factura con crédito fiscal para la universidad."
                )
            )

            // Reminder 5: Today at 8:00 AM - Revisar métricas del dataset (Done)
            val cal5 = Calendar.getInstance()
            cal5.set(Calendar.HOUR_OF_DAY, 8)
            cal5.set(Calendar.MINUTE, 0)
            val r5Id = "rem-5"
            reminderDao.insertReminder(
                Reminder(
                    id = r5Id,
                    title = "Revisar métricas iniciales del dataset",
                    datetime = cal5.timeInMillis,
                    leadTimeMinutes = 0,
                    repeatRule = "NONE",
                    status = "DONE",
                    notebookId = datosId
                )
            )
            noteDao.insertNote(
                Note(
                    notebookId = datosId,
                    reminderId = r5Id,
                    text = "Se limpiaron 1,420 filas nulas y se normalizaron los valores de timestamp a UTC-6."
                )
            )
        }
    }
}
