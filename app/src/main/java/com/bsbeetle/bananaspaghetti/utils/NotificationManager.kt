package com.bsbeetle.bananaspaghetti.utils

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Manages notification state and storage using SharedPreferences
 */
object NotificationManager {
    private const val PREFS_NAME = "beetle_notifications"
    private const val KEY_LAST_UPDATE_ID = "last_update_id"
    private const val KEY_LAST_UPDATE_TITLE = "last_update_title"
    private const val KEY_LAST_UPDATE_DESCRIPTION = "last_update_description"
    private const val KEY_HAS_UNREAD = "has_unread_notification"
    
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * Saves the latest update and determines if it's new (unread)
     * Returns true if this is a new/different update
     */
    fun saveUpdate(context: Context, id: Int, title: String, description: String): Boolean {
        val prefs = getPrefs(context)
        val lastId = prefs.getInt(KEY_LAST_UPDATE_ID, -1)
        
        val isNew = lastId != id
        
        prefs.edit().apply {
            putInt(KEY_LAST_UPDATE_ID, id)
            putString(KEY_LAST_UPDATE_TITLE, title)
            putString(KEY_LAST_UPDATE_DESCRIPTION, description)
            if (isNew) {
                putBoolean(KEY_HAS_UNREAD, true)
            }
            apply()
        }
        
        return isNew
    }
    
    /**
     * Gets the saved update details
     */
    fun getLastUpdate(context: Context): UpdateData? {
        val prefs = getPrefs(context)
        val id = prefs.getInt(KEY_LAST_UPDATE_ID, -1)
        if (id == -1) return null
        
        return UpdateData(
            id = id,
            title = prefs.getString(KEY_LAST_UPDATE_TITLE, "") ?: "",
            description = prefs.getString(KEY_LAST_UPDATE_DESCRIPTION, "") ?: ""
        )
    }
    
    /**
     * Checks if there's an unread notification
     */
    fun hasUnread(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_HAS_UNREAD, false)
    }
    
    /**
     * Marks the current notification as read
     */
    fun markAsRead(context: Context) {
        getPrefs(context).edit().putBoolean(KEY_HAS_UNREAD, false).apply()
    }
}

/**
 * Data class for update information
 */
data class UpdateData(
    val id: Int,
    val title: String,
    val description: String
)
