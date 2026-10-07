package com.halil.ozel.huaweipushkitapp

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.huawei.hms.push.HmsMessageService
import com.huawei.hms.push.RemoteMessage


class HuaweiPushService : HmsMessageService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.i(TAG, "onMessageReceived()")
        val notificationData = remoteMessage.dataOfMap
        if (notificationData.isNotEmpty()) {
            val title = notificationData[TITLE].orEmpty()
            val text = notificationData[TEXT].orEmpty()
            val channelId = channelFor(notificationData[CHANNEL_ID])
            recordAndShow(KIND_DATA, channelId, title, text)
            return
        }
        val notification = remoteMessage.notification
        if (notification != null) {
            recordAndShow(
                KIND_NOTIFICATION,
                Constants.NotificationChannelOne.ID,
                notification.title.orEmpty(),
                notification.body.orEmpty(),
            )
            return
        }
        recordAndShow(KIND_EMPTY, Constants.NotificationChannelTwo.ID, "", "")
    }

    private fun recordAndShow(kind: String, channelId: String, title: String, text: String) {
        PushStore(this).addMessage(kind, channelId, title, text)
        if (title.isEmpty() && text.isEmpty()) {
            return
        }
        showNotification(channelId, title, text)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i(TAG, "onNewToken()")
        PushStore(this).saveToken(token)
    }

    override fun onNewToken(token: String, bundle: Bundle) {
        super.onNewToken(token, bundle)
        Log.i(TAG, "onNewToken()")
        PushStore(this).saveToken(token)
    }

    override fun onTokenError(exception: Exception) {
        super.onTokenError(exception)
        Log.e(TAG, "onTokenError: ${exception.message}")
    }

    private fun channelFor(requested: String?): String {
        return if (requested == Constants.NotificationChannelOne.ID) {
            Constants.NotificationChannelOne.ID
        } else {
            Constants.NotificationChannelTwo.ID
        }
    }

    private fun showNotification(channelId: String, title: String, text: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "POST_NOTIFICATIONS is not granted")
            return
        }
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setColor(ContextCompat.getColor(this, R.color.black))
            .build()
        NotificationManagerCompat.from(this).notify(notificationId(title, text), notification)
    }

    companion object {
        private const val TAG = "HuaweiPushService"
        private const val TITLE = "title"
        private const val TEXT = "text"
        private const val CHANNEL_ID = "channel_id"
        private const val KIND_DATA = "data"
        private const val KIND_NOTIFICATION = "notification"
        private const val KIND_EMPTY = "empty"

        private fun notificationId(title: String, text: String): Int {
            return (title + text + System.currentTimeMillis()).hashCode()
        }
    }
}