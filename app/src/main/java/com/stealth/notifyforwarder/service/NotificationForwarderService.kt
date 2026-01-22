package com.stealth.notifyforwarder.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.stealth.notifyforwarder.MainApplication
import com.stealth.notifyforwarder.R
import com.stealth.notifyforwarder.data.NotificationEntity
import com.stealth.notifyforwarder.network.TelegramSender
import com.stealth.notifyforwarder.ui.MainActivity
import com.stealth.notifyforwarder.util.ServiceChecker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * NotificationListenerService that captures all notifications and forwards them to Telegram
 * Runs as a foreground service to ensure reliable operation
 */
class NotificationForwarderService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var telegramSender: TelegramSender
    private var isServiceConnected = false

    override fun onCreate() {
        super.onCreate()
        telegramSender = TelegramSender(applicationContext)
        Log.d(TAG, "NotificationForwarderService created")

        // Start as foreground service
        startForegroundService()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        isServiceConnected = true
        Log.d(TAG, "Notification listener connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isServiceConnected = false
        Log.w(TAG, "Notification listener disconnected")

        // Try to reconnect
        requestRebind(ComponentName(this, NotificationForwarderService::class.java))
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        // Skip our own notifications to avoid infinite loop
        if (sbn.packageName == packageName) return

        // Skip system notifications (settings, battery, etc.)
        if (isSystemNotification(sbn)) return

        // Check if service is connected
        if (!isServiceConnected) {
            Log.w(TAG, "Service not connected, queuing notification")
            queueNotificationLocally(sbn)
            return
        }

        // Process the notification
        processNotification(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Optionally log removed notifications
        // Log.d(TAG, "Notification removed: ${sbn?.packageName}")
    }

    /**
     * Processes a notification and forwards it to Telegram
     */
    private fun processNotification(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        val notification = sbn.notification

        // Extract notification data
        val extras = notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: text

        // Get app name
        val appName = getAppName(packageName)

        // Get timestamp
        val timestamp = sbn.postTime

        // Create notification entity
        val notificationEntity = NotificationEntity(
            packageName = packageName,
            appName = appName,
            title = title,
            message = bigText,
            subText = subText,
            timestamp = timestamp,
            isRead = false,
            retryCount = 0
        )

        // Forward to Telegram
        forwardToTelegram(notificationEntity)
    }

    /**
     * Forwards notification to Telegram
     */
    private fun forwardToTelegram(notification: NotificationEntity) {
        serviceScope.launch {
            try {
                val success = telegramSender.sendNotification(notification)
                if (!success) {
                    // Queue for retry
                    queueNotificationLocally(notification)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending to Telegram", e)
                queueNotificationLocally(notification)
            }
        }
    }

    /**
     * Queues notification locally for later retry
     */
    private fun queueNotificationLocally(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: text

        val entity = NotificationEntity(
            packageName = packageName,
            appName = getAppName(packageName),
            title = title,
            message = bigText,
            subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: "",
            timestamp = sbn.postTime,
            isRead = false,
            retryCount = 0
        )

        queueNotificationLocally(entity)
    }

    /**
     * Queues notification entity for later retry
     */
    private fun queueNotificationLocally(notification: NotificationEntity) {
        serviceScope.launch {
            try {
                val database = (application as MainApplication).database
                database.notificationDao().insert(notification)

                // Schedule retry work
                val retryWork = OneTimeWorkRequestBuilder<com.stealth.notifyforwarder.util.RetrySenderWorker>()
                    .build()
                WorkManager.getInstance(applicationContext).enqueue(retryWork)
            } catch (e: Exception) {
                Log.e(TAG, "Error queueing notification", e)
            }
        }
    }

    /**
     * Checks if notification is a system notification
     */
    private fun isSystemNotification(sbn: StatusBarNotification): Boolean {
        val packageName = sbn.packageName

        // List of system package names to skip
        val systemPackages = listOf(
            "com.android.systemui",
            "com.android.launcher",
            "com.android.settings",
            "com.android.phone",
            "com.android.server.telecom",
            "android",
            "com.google.android.systemui"
        )

        return systemPackages.any { packageName.startsWith(it) }
    }

    /**
     * Gets the app name from package name
     */
    private fun getAppName(packageName: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            // Extract app name from package
            packageName.substringAfterLast(".").replaceFirstChar { it.uppercase() }
        }
    }

    /**
     * Starts the service as a foreground service
     */
    private fun startForegroundService() {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, MainApplication.CHANNEL_SERVICE_ID)
            .setContentTitle(getString(R.string.foreground_notification_title))
            .setContentText(getString(R.string.foreground_notification_text))
            .setSmallIcon(R.drawable.ic_search)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return super.onBind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d(TAG, "NotificationForwarderService destroyed")
    }

    companion object {
        private const val TAG = "NotificationForwarder"
        private const val NOTIFICATION_ID = 1001
    }
}
