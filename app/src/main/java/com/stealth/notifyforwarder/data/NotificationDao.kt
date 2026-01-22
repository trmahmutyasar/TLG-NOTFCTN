package com.stealth.notifyforwarder.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for notification operations
 * Provides methods to interact with the notifications table
 */
@Dao
interface NotificationDao {

    /**
     * Inserts a new notification
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: NotificationEntity): Long

    /**
     * Updates an existing notification
     */
    @Update
    suspend fun update(notification: NotificationEntity)

    /**
     * Deletes a notification
     */
    @Delete
    suspend fun delete(notification: NotificationEntity)

    /**
     * Gets all unread notifications
     */
    @Query("SELECT * FROM notifications WHERE isRead = 0 ORDER BY timestamp DESC")
    suspend fun getUnreadNotifications(): List<NotificationEntity>

    /**
     * Gets all notifications
     */
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    /**
     * Gets a notification by ID
     */
    @Query("SELECT * FROM notifications WHERE id = :id")
    suspend fun getNotificationById(id: Long): NotificationEntity?

    /**
     * Marks a notification as read
     */
    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Long)

    /**
     * Marks all notifications as read
     */
    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllAsRead()

    /**
     * Clears all notifications from the queue
     */
    @Query("DELETE FROM notifications")
    suspend fun clearQueue()

    /**
     * Deletes old notifications (older than specified timestamp)
     */
    @Query("DELETE FROM notifications WHERE timestamp < :timestamp")
    suspend fun deleteOldNotifications(timestamp: Long)

    /**
     * Gets the count of unread notifications
     */
    @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0")
    suspend fun getUnreadCount(): Int

    /**
     * Updates retry count for a notification
     */
    @Query("UPDATE notifications SET retryCount = retryCount + 1 WHERE id = :id")
    suspend fun incrementRetryCount(id: Long)

    /**
     * Gets failed notifications (retry count >= max)
     */
    @Query("SELECT * FROM notifications WHERE retryCount >= 3 ORDER BY timestamp DESC")
    suspend fun getFailedNotifications(): List<NotificationEntity>

    /**
     * Deletes a notification by ID
     */
    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteById(id: Long)
}
