package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R

object NotificationHelper {
    const val CHANNEL_ID = "aniwerti_social_channel"
    private const val CHANNEL_NAME = "Друзья и Сообщения"
    private const val CHANNEL_DESC = "Уведомления о входящих заявках в друзья и сообщениях в чате"

    fun initChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun showFriendRequestNotification(context: Context, fromName: String, fromUserId: String) {
        try {
            initChannel(context)
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("navigate_to", "friends")
                putExtra("from_user_id", fromUserId)
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                fromUserId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Новая заявка в друзья!")
                .setContentText("$fromName хочет добавить вас в друзья")
                .setStyle(NotificationCompat.BigTextStyle().bigText("$fromName ($fromUserId) отправил вам заявку в друзья в ANIWERTI. Откройте приложение, чтобы принять."))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            val manager = NotificationManagerCompat.from(context)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                manager.notify(fromUserId.hashCode(), builder.build())
            }
        } catch (_: Throwable) {}
    }

    fun showChatMessageNotification(context: Context, friendName: String, friendId: String, text: String) {
        try {
            initChannel(context)
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("navigate_to", "chat")
                putExtra("friend_id", friendId)
                putExtra("friend_name", friendName)
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                friendId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(friendName)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            val manager = NotificationManagerCompat.from(context)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                manager.notify(friendId.hashCode() + 1000, builder.build())
            }
        } catch (_: Throwable) {}
    }
}
