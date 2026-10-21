package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

object RepairNotificationHelper {

    const val CHANNEL_ID = "repair_ready_notifications"
    private const val CHANNEL_NAME = "Уведомления о готовности ремонта"
    private const val CHANNEL_DESCRIPTION = "Оповещения мастера автосервиса при переводе записи в статус «Готова»"
    const val EXTRA_RECORD_ID = "extra_open_record_id"

    /**
     * Создаёт канал уведомлений (Android 8.0+) в системном NotificationManager.
     */
    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    ?: return

            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Проверяет, выдано ли разрешение POST_NOTIFICATIONS на Android 13+.
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Отправляет локальное уведомление через NotificationManager о том,
     * что ремонт детали завершён и статус изменён на «Готова».
     */
    fun showRepairReadyNotification(
        context: Context,
        recordId: Long,
        partNumber: String,
        repairType: String,
        clientName: String,
        phone: String
    ) {
        if (!hasNotificationPermission(context)) return

        ensureNotificationChannel(context)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (recordId > 0L) {
                putExtra(EXTRA_RECORD_ID, recordId)
            }
        }

        val pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0

        val notificationId = if (recordId > 0L) recordId.toInt() else partNumber.hashCode()
        val contentIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openIntent,
            pendingIntentFlags
        )

        val title = "Ремонт завершён: $partNumber"
        val shortText = buildString {
            append("Статус изменён на «Готова» ($repairType)")
            if (clientName.isNotBlank()) {
                append(" • Клиент: $clientName")
            }
        }
        val expandedText = buildString {
            append("Деталь: $partNumber\n")
            append("Вид ремонта: $repairType\n")
            append("Статус: Готова")
            if (clientName.isNotBlank()) {
                append("\nКлиент: $clientName")
            }
            if (phone.isNotBlank()) {
                append(" ($phone)")
            }
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(shortText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(expandedText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        notificationManager.notify(notificationId, notification)
    }
}
