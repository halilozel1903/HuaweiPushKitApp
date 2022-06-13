package com.halil.ozel.huaweipushkitapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelOne = NotificationChannel(
                Constants.NotificationChannelOne.ID,
                Constants.NotificationChannelOne.NAME,
                NotificationManager.IMPORTANCE_HIGH
            )
            channelOne.description = Constants.NotificationChannelOne.DESCRIPTION
            val channelTwo = NotificationChannel(
                Constants.NotificationChannelTwo.ID,
                Constants.NotificationChannelTwo.NAME,
                NotificationManager.IMPORTANCE_LOW
            )
            channelTwo.description = Constants.NotificationChannelTwo.DESCRIPTION
            val manager: NotificationManager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channelOne)
            manager.createNotificationChannel(channelTwo)
        }
    }
}