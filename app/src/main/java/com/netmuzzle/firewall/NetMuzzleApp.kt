package com.netmuzzle.firewall

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class NetMuzzleApp : Application() {

    companion object {
        const val CHANNEL_ID = "netmuzzle_status_channel"
        lateinit var instance: NetMuzzleApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.notification_channel_name)
            val descriptionText = getString(R.string.notification_channel_desc)
            // IMPORTANCE_LOW: ciche powiadomienie, nie wydaje dźwięków ani wibracji
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(false)
            }
            val notificationManager: NotificationManager =
                getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}
