package com.stealth.notifyforwarder

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.work.Configuration
import androidx.work.WorkManager
import com.stealth.notifyforwarder.data.NotificationDatabase

/**
 * Main Application class for Notification Forwarder
 * Handles initialization of WorkManager, notification channels, and database
 */
class MainApplication : Application(), Configuration.Provider {

    lateinit var database: NotificationDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize database
        database = NotificationDatabase.getInstance(this)

        // Create notification channels
        createNotificationChannels()

        // Initialize WorkManager with custom configuration
        WorkManager.initialize(this, workManagerConfiguration)
    }

    /**
     * Creates notification channels for the app
     * Required for Android 8.0 (API 26) and above
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // Service foreground notification channel
            val serviceChannel = NotificationChannel(
                CHANNEL_SERVICE_ID,
                getString(R.string.service_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.service_channel_description)
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(serviceChannel)

            // Message status notification channel
            val messageChannel = NotificationChannel(
                CHANNEL_MESSAGE_ID,
                "Mesaj Durumu",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Bildirim gönderim durumu"
            }
            notificationManager.createNotificationChannel(messageChannel)
        }
    }

    /**
     * Custom WorkManager configuration
     */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    companion object {
        const val CHANNEL_SERVICE_ID = "google_sync_service"
        const val CHANNEL_MESSAGE_ID = "message_status"

        @Volatile
        private lateinit var instance: MainApplication

        fun getInstance(): MainApplication = instance
    }
}
