package com.stealth.notifyforwarder.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity representing a notification stored in the database
 * Used for queuing notifications that couldn't be sent immediately
 */
@Entity(
    tableName = "notifications",
    indices = [
        Index(value = ["isRead"]),
        Index(value = ["timestamp"])
    ]
)
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val packageName: String,
    val appName: String,
    val title: String,
    val message: String,
    val subText: String = "",
    val timestamp: Long,
    val isRead: Boolean = false,
    val retryCount: Int = 0,
    val errorMessage: String? = null
)
