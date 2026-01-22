package com.stealth.notifyforwarder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import com.stealth.notifyforwarder.service.NotificationForwarderService
import com.stealth.notifyforwarder.util.ServiceChecker

/**
 * BroadcastReceiver that starts the notification service on device boot
 * Ensures the service runs automatically after device restart
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action

        Log.d(TAG, "Boot receiver received action: $action")

        when (action) {
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON" -> {
                onBootCompleted(context)
            }
        }
    }

    /**
     * Handles device boot completion
     */
    private fun onBootCompleted(context: Context) {
        Log.d(TAG, "Device boot completed")

        // Check if notification listener service is enabled
        if (ServiceChecker.isNotificationServiceEnabled(context)) {
            startNotificationService(context)
            Log.d(TAG, "Service started after boot")
        } else {
            Log.w(TAG, "Notification listener not enabled, cannot start service")
        }
    }

    /**
     * Starts the notification forwarding service
     */
    private fun startNotificationService(context: Context) {
        try {
            val serviceIntent = Intent(context, NotificationForwarderService::class.java)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting service", e)
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
