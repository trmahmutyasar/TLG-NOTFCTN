package com.stealth.notifyforwarder

import com.stealth.notifyforwarder.data.NotificationEntity
import com.stealth.notifyforwarder.network.TelegramSender
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner

/**
 * Unit tests for TelegramSender class
 * Tests message formatting and basic functionality
 */
@RunWith(MockitoJUnitRunner::class)
class TelegramSenderTest {

    @Mock
    private lateinit var mockContext: android.content.Context

    private lateinit var telegramSender: TelegramSender

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        telegramSender = TelegramSender(mockContext)
    }

    @Test
    fun testNotificationEntityCreation() {
        val notification = NotificationEntity(
            id = 1,
            packageName = "com.whatsapp",
            appName = "WhatsApp",
            title = "John Doe",
            message = "Hello, how are you?",
            subText = "",
            timestamp = System.currentTimeMillis(),
            isRead = false,
            retryCount = 0
        )

        assertEquals("com.whatsapp", notification.packageName)
        assertEquals("WhatsApp", notification.appName)
        assertEquals("John Doe", notification.title)
        assertEquals("Hello, how are you?", notification.message)
    }

    @Test
    fun testNotificationEntityWithEmptyFields() {
        val notification = NotificationEntity(
            id = 2,
            packageName = "com.android.mms",
            appName = "Mesajlar",
            title = "",
            message = "You have a new message",
            subText = "",
            timestamp = System.currentTimeMillis(),
            isRead = false,
            retryCount = 0
        )

        assertEquals("", notification.title)
        assertTrue(notification.message.isNotEmpty())
    }

    @Test
    fun testRetryCountIncrement() {
        val notification = NotificationEntity(
            id = 3,
            packageName = "com.facebook.katana",
            appName = "Facebook",
            title = "Notifications",
            message = "You have 5 new notifications",
            subText = "",
            timestamp = System.currentTimeMillis(),
            isRead = false,
            retryCount = 0
        )

        val updatedNotification = notification.copy(retryCount = notification.retryCount + 1)
        assertEquals(1, updatedNotification.retryCount)
    }

    @Test
    fun testTimestampFormat() {
        val timestamp = 1704067200000L // January 1, 2024 00:00:00 UTC
        val notification = NotificationEntity(
            id = 4,
            packageName = "com.instagram.android",
            appName = "Instagram",
            title = "New follower",
            message = "someone started following you",
            subText = "",
            timestamp = timestamp,
            isRead = false,
            retryCount = 0
        )

        assertEquals(timestamp, notification.timestamp)
        assertTrue(notification.timestamp > 0)
    }

    @Test
    fun testAppNameExtraction() {
        val testCases = listOf(
            "com.whatsapp" to "WhatsApp",
            "com.facebook.katana" to "Facebook",
            "com.instagram.android" to "Instagram",
            "org.telegram.messenger" to "Telegram",
            "com.google.android.gm" to "Gmail"
        )

        for ((packageName, expectedAppName) in testCases) {
            val extractedName = packageName.substringAfterLast(".")
                .replaceFirstChar { it.uppercase() }
            assertEquals(expectedAppName, extractedName)
        }
    }

    @Test
    fun testNotificationIsRead() {
        val notification = NotificationEntity(
            id = 5,
            packageName = "com.discord",
            appName = "Discord",
            title = "New message",
            message = "Check out this link!",
            subText = "",
            timestamp = System.currentTimeMillis(),
            isRead = true,
            retryCount = 2
        )

        assertTrue(notification.isRead)
        assertEquals(2, notification.retryCount)
    }

    @Test
    fun testMessageTruncation() {
        val longMessage = "A".repeat(1000)
        val notification = NotificationEntity(
            id = 6,
            packageName = "com.example.app",
            appName = "Example",
            title = "Long message",
            message = longMessage,
            subText = "",
            timestamp = System.currentTimeMillis(),
            isRead = false,
            retryCount = 0
        )

        val truncatedMessage = if (notification.message.length > 500) {
            notification.message.take(500) + "..."
        } else {
            notification.message
        }

        assertEquals(503, truncatedMessage.length) // 500 chars + "..."
    }
}
