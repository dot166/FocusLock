package io.github.dot166.focuslock.core

import android.app.NotificationChannel
import android.app.NotificationManager

class Application : android.app.Application() {
    companion object {
        const val BLOCKER_CHANNEL_ID = "AppBlockServiceChannel"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            BLOCKER_CHANNEL_ID,
            "App Blocker Service",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
    }
}