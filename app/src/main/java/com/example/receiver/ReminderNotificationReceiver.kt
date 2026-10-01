package com.example.receiver

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.local.ApuntaDatabase
import com.example.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val reminderId = intent.getStringExtra(NotificationHelper.EXTRA_REMINDER_ID) ?: return

        val database = ApuntaDatabase.getDatabase(context)

        when (action) {
            NotificationHelper.ACTION_TRIGGER_REMINDER -> {
                val title = intent.getStringExtra(NotificationHelper.EXTRA_REMINDER_TITLE) ?: "Recordatorio"
                val notebookName = intent.getStringExtra(NotificationHelper.EXTRA_NOTEBOOK_NAME) ?: "Apunta"
                val colorHex = intent.getStringExtra(NotificationHelper.EXTRA_COLOR_HEX) ?: "#4F46E5"
                val isEscalated = intent.getBooleanExtra(NotificationHelper.EXTRA_IS_ESCALATED, false)
                val step = intent.getIntExtra(NotificationHelper.EXTRA_ESCALATION_STEP, 0)

                NotificationHelper.showNotification(
                    context = context,
                    reminderId = reminderId,
                    title = title,
                    notebookName = notebookName,
                    colorHex = colorHex,
                    isEscalated = isEscalated,
                    escalationStep = step
                )

                // Staggered alerts (alertas escalonadas): If enabled and step < 2, schedule re-alerts at 5 min and 10 min
                if (isEscalated && step < 2) {
                    val nextStep = step + 1
                    val nextDelayMinutes = if (nextStep == 1) 5 else 10
                    val nextTrigger = System.currentTimeMillis() + (nextDelayMinutes * 60 * 1000L)

                    val nextIntent = Intent(context, ReminderNotificationReceiver::class.java).apply {
                        this.action = NotificationHelper.ACTION_TRIGGER_REMINDER
                        putExtra(NotificationHelper.EXTRA_REMINDER_ID, reminderId)
                        putExtra(NotificationHelper.EXTRA_REMINDER_TITLE, title)
                        putExtra(NotificationHelper.EXTRA_NOTEBOOK_NAME, notebookName)
                        putExtra(NotificationHelper.EXTRA_COLOR_HEX, colorHex)
                        putExtra(NotificationHelper.EXTRA_IS_ESCALATED, true)
                        putExtra(NotificationHelper.EXTRA_ESCALATION_STEP, nextStep)
                    }

                    val nextPending = PendingIntent.getBroadcast(
                        context,
                        reminderId.hashCode() + 100 + nextStep,
                        nextIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (alarmManager.canScheduleExactAlarms()) {
                            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTrigger, nextPending)
                        } else {
                            alarmManager.set(AlarmManager.RTC_WAKEUP, nextTrigger, nextPending)
                        }
                    } else {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTrigger, nextPending)
                    }
                }
            }

            NotificationHelper.ACTION_COMPLETE_REMINDER -> {
                // Cancel notification
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.cancel(reminderId.hashCode())

                CoroutineScope(Dispatchers.IO).launch {
                    database.reminderDao().updateStatus(reminderId, "DONE")
                }
            }

            NotificationHelper.ACTION_SNOOZE_REMINDER -> {
                // Snooze 15 minutes
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.cancel(reminderId.hashCode())

                val newTime = System.currentTimeMillis() + (15 * 60 * 1000L)
                CoroutineScope(Dispatchers.IO).launch {
                    database.reminderDao().snoozeReminder(reminderId, newTime)
                    val reminder = database.reminderDao().getReminderById(reminderId)
                    if (reminder != null) {
                        NotificationHelper.scheduleReminder(context, reminder)
                    }
                }
            }
        }
    }
}
