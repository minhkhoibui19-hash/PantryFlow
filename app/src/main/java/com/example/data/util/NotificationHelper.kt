package com.example.data.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.PantryItem

object NotificationHelper {
    private const val CHANNEL_ID = "pantryflow_expiry_reminders"
    private const val CHANNEL_NAME = "Nhắc nhở hạn sử dụng thực phẩm"
    private const val CHANNEL_DESC = "Thông báo nhắc dùng sớm các thực phẩm sắp đến hạn trong tủ đồ"
    private const val NOTIFICATION_ID_TEST = 1001

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = CHANNEL_DESC
                    enableVibration(true)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

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

    fun buildReminderSentence(item: PantryItem): String {
        val days = DateUtils.daysUntil(item.expiryDate)
        val location = item.storageLocation.label
        return when {
            days < 0 -> "${item.name} trong $location đã quá hạn ${-days} ngày"
            days == 0 -> "${item.name} trong $location nên dùng trong hôm nay"
            days == 1 -> "${item.name} trong $location nên dùng trước ngày mai"
            else -> "${item.name} trong $location sắp hết hạn sau $days ngày"
        }
    }

    /**
     * Dispatches a real Android system notification listing upcoming expiring pantry items.
     * Returns true if sent successfully, false if permission is missing.
     */
    fun sendExpiryReminderNotification(
        context: Context,
        expiringItems: List<PantryItem>,
        reminderDaysBefore: Int,
        reminderTimeText: String
    ): Boolean {
        ensureNotificationChannel(context)
        if (!hasNotificationPermission(context)) {
            return false
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (expiringItems.isEmpty()) {
            "PantryFlow • Tủ thực phẩm đang rất tươi!"
        } else {
            "PantryFlow • ${expiringItems.size} món cần chú ý hạn dùng"
        }

        val summaryLine = if (expiringItems.isEmpty()) {
            "Không có thực phẩm nào sắp hết hạn trong $reminderDaysBefore ngày tới (lịch nhắc $reminderTimeText)."
        } else {
            buildReminderSentence(expiringItems.first())
        }

        val inboxStyle = NotificationCompat.InboxStyle()
            .setBigContentTitle(title)
            .setSummaryText("Nhắc trước $reminderDaysBefore ngày • $reminderTimeText")

        if (expiringItems.isEmpty()) {
            inboxStyle.addLine(summaryLine)
        } else {
            expiringItems.take(5).forEach { item ->
                inboxStyle.addLine("• ${buildReminderSentence(item)}")
            }
            if (expiringItems.size > 5) {
                inboxStyle.addLine("...và ${expiringItems.size - 5} món khác")
            }
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(summaryLine)
            .setStyle(inboxStyle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_TEST, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }
}
