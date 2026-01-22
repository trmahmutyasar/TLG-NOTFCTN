package com.stealth.notifyforwarder.util

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.stealth.notifyforwarder.network.TelegramSender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * WorkManager Worker for sending test messages to verify Telegram connection
 */
class TestMessageWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "Test message worker started")

        return withContext(Dispatchers.IO) {
            try {
                val telegramSender = TelegramSender(applicationContext)
                val success = telegramSender.sendTestMessage()

                if (success) {
                    Log.d(TAG, "Test message sent successfully")
                    Result.success()
                } else {
                    Log.e(TAG, "Failed to send test message")
                    Result.retry()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending test message", e)
                Result.retry()
            }
        }
    }

    companion object {
        private const val TAG = "TestMessageWorker"
    }
}
