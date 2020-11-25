package com.halil.ozel.huaweipushkitapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel1 = NotificationChannel(
                Constant.NotificationChannel1.ID,
                Constant.NotificationChannel1.NAME,
                NotificationManager.IMPORTANCE_HIGH
            )
            channel1.description = Constant.NotificationChannel1.DESCRIPTION
            val channel2 = NotificationChannel(
                Constant.NotificationChannel2.ID,
                Constant.NotificationChannel2.NAME,
                NotificationManager.IMPORTANCE_LOW
            )
            channel2.description = Constant.NotificationChannel2.DESCRIPTION
            val manager: NotificationManager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel1)
            manager.createNotificationChannel(channel2)
        }
    }
}