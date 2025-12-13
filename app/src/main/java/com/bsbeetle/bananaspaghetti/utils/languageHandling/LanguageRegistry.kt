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

    /** Mark installed with version and ID */
    fun setInstalled(language: String, version: String, id: Int) {
        val entry = JSONObject()
        entry.put("installed", true)
        entry.put("version", version)
        entry.put("id", id)
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

    fun getSlugForExtension(ext: String): String {
        return when (ext) {
            "cpp", "c", "h", "hpp" -> "cpp"
            "py" -> "python"
            "html" -> "html"
            "css" -> "css"
            "js" -> "js"  // Match the plugin slug from server
            "ts" -> "ts"  // Match the plugin slug from server
            else -> "plaintext"
        }
    }
    
    /** Check if a language is installed, trying multiple possible slugs */
    fun isInstalledForExt(ext: String): Boolean {
        // Try the direct slug first
        val slug = getSlugForExtension(ext)
        if (isInstalled(slug)) return true
        
        // Try common aliases
        val aliases = when (ext) {
            "js" -> listOf("javascript", "js")
            "ts" -> listOf("typescript", "ts")
            else -> emptyList()
        }
        return aliases.any { isInstalled(it) }
    }
    
    /** Get the actual installed slug for an extension */
    fun getInstalledSlugForExt(ext: String): String? {
        val slug = getSlugForExtension(ext)
        if (isInstalled(slug)) return slug
        
        // Try common aliases
        val aliases = when (ext) {
            "js" -> listOf("javascript", "js")
            "ts" -> listOf("typescript", "ts")
            else -> emptyList()
        }
        return aliases.find { isInstalled(it) }
    }
}