package com.os11.app.platform.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface NotificationPlatformDispatcher {
    fun postNotification(
        channelId: String,
        notificationId: Int,
        title: String,
        content: String,
        taskId: String? = null,
    )
    fun cancelNotification(notificationId: Int)
}

@Singleton
class SystemNotificationPlatformDispatcher @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : NotificationPlatformDispatcher {

    init {
        OS11NotificationChannels.createChannels(context)
    }

    override fun postNotification(
        channelId: String,
        notificationId: Int,
        title: String,
        content: String,
        taskId: String?,
    ) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) {
            return
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val permissionGranted = ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!permissionGranted) {
                return
            }
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(content)
            .setAutoCancel(true)
            .setPriority(
                if (channelId == OS11NotificationChannels.CHANNEL_REMINDERS) {
                    NotificationCompat.PRIORITY_HIGH
                } else {
                    NotificationCompat.PRIORITY_DEFAULT
                },
            )

        manager.notify(notificationId, builder.build())
    }

    override fun cancelNotification(notificationId: Int) {
        val manager = NotificationManagerCompat.from(context)
        manager.cancel(notificationId)
    }
}
