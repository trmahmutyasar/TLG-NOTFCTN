package com.stealth.notifyforwarder.util

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.stealth.notifyforwarder.MainApplication
import com.stealth.notifyforwarder.data.NotificationEntity
import com.stealth.notifyforwarder.network.TelegramSender

/**
 * WorkManager Worker for retrying failed notification sends
 * Handles offline notifications when network becomes available
 */
class RetrySenderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "Retry sender worker started")

        return try {
            val database = (applicationContext as MainApplication).database
            val telegramSender = TelegramSender(applicationContext)

            // Get all unread notifications
            val pendingNotifications = database.notificationDao().getUnreadNotifications()

            if (pendingNotifications.isEmpty()) {
                Log.d(TAG, "No pending notifications to send")
                return Result.success()
            }

            Log.d(TAG, "Found ${pendingNotifications.size} pending notifications")

            var successCount = 0
            var failCount = 0

            for (notification in pendingNotifications) {
                try {
                    val success = telegramSender.sendNotification(notification)

                    if (success) {
                        // Mark as sent
                        database.notificationDao().delete(notification)
                        successCount++
                    } else {
                        // Increment retry count
                        database.notificationDao().incrementRetryCount(notification.id)

                        // If too many retries, remove the notification
                        if (notification.retryCount >= MAX_RETRY_COUNT) {
                            Log.w(TAG, "Notification ${notification.id} exceeded max retries, removing")
                            database.notificationDao().delete(notification)
                            failCount++
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing notification ${notification.id}", e)
                    database.notificationDao().incrementRetryCount(notification.id)
                    failCount++
                }
            }

            Log.d(TAG, "Retry complete: $successCount sent, $failCount failed")

            if (failCount > 0 && successCount == 0) {
                return Result.retry()
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in retry sender worker", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "RetrySenderWorker"
        private const val MAX_RETRY_COUNT = 3
    }
}
