package com.halil.ozel.huaweipushkitapp

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.huawei.hms.push.HmsMessageService
import com.huawei.hms.push.RemoteMessage


class HuaweiPushService : HmsMessageService() {

    @SuppressLint("UnspecifiedImmutableFlag")
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.i(TAG, "onMessageReceived()")
        val notificationData = remoteMessage.dataOfMap
        if (notificationData.isEmpty()) {
            Log.e(TAG, "onMessageReceived: notification data is empty")
            return
        }
        val icon = R.mipmap.ic_launcher
        val title = notificationData[TITLE]
        val text = notificationData[TEXT]
        var channelId = notificationData[CHANNEL_ID]
        if (channelId == null) {
            channelId = Constants.NotificationChannelTwo.ID
        }
        if (channelId != Constants.NotificationChannelOne.ID) {
            channelId = Constants.NotificationChannelTwo.ID
        }
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, 0)
        val notificationManager = NotificationManagerCompat.from(this)
        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setColor(this.resources.getColor(R.color.black, null))
            .build()
        notificationManager.notify(1, notification)
    }

    companion object {
        private const val TAG = "HuaweiPushService"
        private const val TITLE = "title"
        private const val TEXT = "text"
        private const val CHANNEL_ID = "channel_id"
    }
}