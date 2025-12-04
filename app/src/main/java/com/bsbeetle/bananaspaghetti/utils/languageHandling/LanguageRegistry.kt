package com.bsbeetle.bananaspaghetti.utils.languageHandling

import android.content.Context
import org.json.JSONObject
import java.io.File

/**
 * Stores the languages installed by the user
 * **/
object LanguageRegistry {
    private const val FILE_NAME = "languages.json"
    private lateinit var registryFile: File
    private var json = JSONObject()

    /** Must be called once on app launch */
    fun init(context: Context) {
        registryFile = File(context.filesDir, FILE_NAME)

        if (registryFile.exists()) {
            json = JSONObject(registryFile.readText())
        } else {
            registryFile.writeText("{}")
            json = JSONObject()
        }
    }

    /** Returns true if lang installed */
    fun isInstalled(language: String): Boolean {
        return json.optJSONObject(language)?.optBoolean("installed") ?: false
    }

    /** Returns version or null */
    fun getVersion(language: String): String? {
        return json.optJSONObject(language)?.optString("version", null)
    }

    /** Mark installed with version */
    fun setInstalled(language: String, version: String) {
        val entry = JSONObject()
        entry.put("installed", true)
        entry.put("version", version)
        json.put(language, entry)
        persist()
    }

    /** Mark language as not installed */
    fun uninstall(language: String) {
        val entry = JSONObject()
        entry.put("installed", false)
        json.put(language, entry)
        persist()
    }

    /** Save JSON to file */
    private fun persist() {
        registryFile.writeText(json.toString(4))
    }
}