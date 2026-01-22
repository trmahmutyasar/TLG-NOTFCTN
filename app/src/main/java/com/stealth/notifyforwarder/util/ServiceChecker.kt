package com.stealth.notifyforwarder.util

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.text.TextUtils
import com.stealth.notifyforwarder.service.NotificationForwarderService

/**
 * Utility class for checking service status and permissions
 */
object ServiceChecker {

    private const val TAG = "ServiceChecker"

    /**
     * Checks if the notification listener service is enabled
     */
    fun isNotificationServiceEnabled(context: Context): Boolean {
        val packageName = context.packageName
        val flat = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        )

        if (TextUtils.isEmpty(flat)) {
            return false
        }

        val names = flat.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (name in names) {
            val cn = ComponentName.unflattenFromString(name)
            if (cn != null && cn.packageName == packageName) {
                return true
            }
        }

        return false
    }

    /**
     * Checks if the notification forwarding service is running
     */
    fun isNotificationServiceRunning(context: Context): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        @Suppress("DEPRECATION")
        val services = manager.getRunningServices(Int.MAX_VALUE)

        val serviceName = NotificationForwarderService::class.java.name

        for (service in services) {
            if (service.service.className == serviceName) {
                return true
            }
        }

        return false
    }

    /**
     * Gets the list of enabled notification listeners
     */
    fun getEnabledNotificationListeners(context: Context): List<String> {
        val flat = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        )

        if (TextUtils.isEmpty(flat)) {
            return emptyList()
        }

        return flat.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            .mapNotNull { ComponentName.unflattenFromString(it)?.className }
    }

    /**
     * Checks if the app has notification permission (Android 13+)
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return android.content.pm.PackageManager.PERMISSION_GRANTED ==
            android.content.pm.PackageManager.checkPermission(
                android.Manifest.permission.POST_NOTIFICATIONS,
                context.packageName
            )
    }

    /**
     * Gets the status of notification listener service as a string
     */
    fun getServiceStatusString(context: Context): String {
        return when {
            !hasNotificationPermission(context) -> "İzin yok"
            !isNotificationServiceEnabled(context) -> "Servis devre dışı"
            !isNotificationServiceRunning(context) -> "Servis çalışmıyor"
            else -> "Aktif"
        }
    }
}
