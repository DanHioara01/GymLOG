package com.example.gymlog2

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.util.Calendar

class WaterReminderReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "water_reminders"
        const val ACTION_SCHEDULE_NEXT = "com.example.gymlog2.SCHEDULE_NEXT_WATER_REMINDER"

        fun notificationIdForAlarm(alarmId: Int): Int = 7000 + alarmId
        fun requestCodeForAlarm(alarmId: Int): Int = 7000 + alarmId
    }

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra("alarm_id", -1)

        if (intent.action == ACTION_SCHEDULE_NEXT && alarmId >= 0) {
            scheduleAlarm(context, alarmId)
            return
        }

        val prefs = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
        val prefsManager = PreferencesManager(context)
        val alarms = prefsManager.getWaterReminders()
        val alarm = alarms.find { it.id == alarmId && it.enabled } ?: return

        createNotificationChannel(context)
        showNotification(context, alarm)
        scheduleAlarm(context, alarmId)
    }

    private fun showNotification(context: Context, alarm: WaterAlarm) {
        val strings = LanguageManager.getStrings(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationIdForAlarm(alarm.id),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(strings.waterReminderTitle)
            .setContentText(strings.waterReminderText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationIdForAlarm(alarm.id), notification)
        } catch (_: SecurityException) { }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Water Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications to remind you to drink water"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
            }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    fun scheduleAlarm(context: Context, alarmId: Int) {
        val prefsManager = PreferencesManager(context)
        val alarm = prefsManager.getWaterReminders().find { it.id == alarmId } ?: return
        if (!alarm.enabled) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, WaterReminderReceiver::class.java).apply {
            putExtra("alarm_id", alarmId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCodeForAlarm(alarmId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    fun cancelAlarm(context: Context, alarmId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, WaterReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCodeForAlarm(alarmId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun scheduleAllEnabledAlarms(context: Context) {
        val prefsManager = PreferencesManager(context)
        prefsManager.getWaterReminders().filter { it.enabled }.forEach { alarm ->
            scheduleAlarm(context, alarm.id)
        }
    }

    fun cancelAllAlarms(context: Context) {
        val prefsManager = PreferencesManager(context)
        prefsManager.getWaterReminders().forEach { alarm ->
            cancelAlarm(context, alarm.id)
        }
    }
}
