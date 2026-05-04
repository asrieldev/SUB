package com.studentutilityhub.app

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in supportedActions) return
        createNotificationChannel(context)
        rescheduleClasses(context)
        rescheduleReminders(context)
    }

    private fun rescheduleClasses(context: Context) {
        readItems(context, "classes").forEach { item ->
            scheduleNotification(
                context = context,
                requestCode = positiveRequestCode(item.id, 10_000),
                triggerAtMillis = item.startTimeMillis - 15 * 60_000L,
                title = "Class soon",
                message = "${item.title} starts at ${timeFormat.format(Date(item.startTimeMillis))}"
            )
        }
    }

    private fun rescheduleReminders(context: Context) {
        readItems(context, "reminders").forEach { item ->
            scheduleNotification(
                context = context,
                requestCode = positiveRequestCode(item.id, 20_000),
                triggerAtMillis = item.startTimeMillis,
                title = "Reminder",
                message = item.title
            )
        }
    }

    private fun readItems(context: Context, key: String): List<ScheduledItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(key, null) ?: return emptyList()
        val items = mutableListOf<ScheduledItem>()
        val json = JSONArray(raw)
        for (index in 0 until json.length()) {
            val value = json.get(index)
            if (value is JSONObject) {
                items.add(ScheduledItem(
                    id = value.optLong("id", System.currentTimeMillis() + index),
                    title = value.getString("title"),
                    startTimeMillis = value.getLong("startTimeMillis")
                ))
            }
        }
        return items
    }

    private fun scheduleNotification(
        context: Context,
        requestCode: Int,
        triggerAtMillis: Long,
        title: String,
        message: String
    ) {
        if (triggerAtMillis <= System.currentTimeMillis()) return
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra(NotificationReceiver.EXTRA_NOTIFICATION_ID, requestCode)
            putExtra(NotificationReceiver.EXTRA_TITLE, title)
            putExtra(NotificationReceiver.EXTRA_MESSAGE, message)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NotificationReceiver.CHANNEL_ID,
                "Student reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Class and reminder alerts from StudentUtilityHub"
            }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun positiveRequestCode(id: Long, offset: Int): Int = ((id % 1_000_000L).toInt() + offset).coerceAtLeast(offset)

    private data class ScheduledItem(val id: Long, val title: String, val startTimeMillis: Long)

    companion object {
        private const val PREFS_NAME = "student_utility_hub"
        private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        private val supportedActions = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED
        )
    }
}
