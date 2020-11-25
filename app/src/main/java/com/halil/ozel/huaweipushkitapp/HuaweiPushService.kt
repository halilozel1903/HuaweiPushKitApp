package com.halil.ozel.huaweipushkitapp

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.huawei.hms.push.HmsMessageService
import com.huawei.hms.push.RemoteMessage


class HuaweiPushService : HmsMessageService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.i(TAG, "onMessageReceived()")
        val notificationData = remoteMessage.dataOfMap
        if (notificationData.isEmpty()) {
            Log.e(TAG, "onMessageReceived: notification data is empty")
            return
        }
        val icon = R.mipmap.ic_launcher
        val title = notificationData["title"]
        val text = notificationData["text"]
        var channelId = notificationData["channel_id"]
        if (channelId == null) {
            channelId = Constant.NotificationChannel2.ID
        }
        if (channelId != Constant.NotificationChannel1.ID) {
            channelId = Constant.NotificationChannel2.ID
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
                .setColor(this.resources.getColor(R.color.black))
                .build()
        notificationManager.notify(1, notification)
    }

    companion object {
        private const val TAG = "HuaweiPushService"
    }
}