package com.bsbeetle.bananaspaghetti.utils.fileHandling

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

data class RecentFile(
    val uri: String,
    val name: String,
    val timestamp: Long
)

object RecentFilesManager {
    private const val PREF_NAME = "recent_files_prefs"
    private const val KEY_RECENT_FILES = "recent_files_list"
    private const val MAX_RECENT_FILES = 10

    fun addFile(context: Context, uri: Uri, name: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val currentList = getRecentFiles(context).toMutableList()

        // Remove if already exists to move it to top
        currentList.removeAll { it.uri == uri.toString() }

        // Add to top
        currentList.add(0, RecentFile(uri.toString(), name, System.currentTimeMillis()))

        // Trim list
        if (currentList.size > MAX_RECENT_FILES) {
            currentList.removeAt(currentList.lastIndex)
        }

        saveList(context, currentList)
    }

    fun getRecentFiles(context: Context): List<RecentFile> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val jsonString = prefs.getString(KEY_RECENT_FILES, "[]")
        val list = mutableListOf<RecentFile>()

        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    RecentFile(
                        uri = obj.getString("uri"),
                        name = obj.getString("name"),
                        timestamp = obj.getLong("timestamp")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun saveList(context: Context, list: List<RecentFile>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val jsonArray = JSONArray()
        
        list.forEach { file ->
            val obj = JSONObject()
            obj.put("uri", file.uri)
            obj.put("name", file.name)
            obj.put("timestamp", file.timestamp)
            jsonArray.put(obj)
        }

        prefs.edit().putString(KEY_RECENT_FILES, jsonArray.toString()).apply()
    }
    
    fun clear(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().remove(KEY_RECENT_FILES).commit()
    }
}
