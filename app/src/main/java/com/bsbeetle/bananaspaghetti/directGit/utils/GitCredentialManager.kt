package com.bsbeetle.bananaspaghetti.directGit.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.net.URI

class GitCredentialManager(context: Context) {

    private val sharedPreferences: SharedPreferences

    init {
        val masterKey =
                MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()

        sharedPreferences =
                EncryptedSharedPreferences.create(
                        context,
                        "git_credentials",
                        masterKey,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
    }

    fun saveCredentials(remoteUrl: String, username: String, token: String) {
        // Simple mapping: URL -> "username:token"
        // In a real app, might want more complex matching for different repos on same host
        val key = getHostFromUrl(remoteUrl)
        if (key.isNotEmpty()) {
            sharedPreferences.edit().putString(key, "$username:$token").apply()
        }
    }

    fun getCredentials(remoteUrl: String): Pair<String, String>? {
        val key = getHostFromUrl(remoteUrl)
        if (key.isNotEmpty()) {
            val stored = sharedPreferences.getString(key, null)
            if (stored != null) {
                val parts = stored.split(":", limit = 2)
                if (parts.size == 2) {
                    return Pair(parts[0], parts[1])
                }
            }
        }
        return null
    }

    fun clearCredentials(remoteUrl: String) {
        val key = getHostFromUrl(remoteUrl)
        if (key.isNotEmpty()) {
            sharedPreferences.edit().remove(key).apply()
        }
    }

    private fun getHostFromUrl(url: String): String {
        return try {
            // Extract host from URL (e.g., https://github.com/user/repo -> github.com)
            // Or just use the full URL if it's specific
            val uri = URI(url)
            uri.host ?: url
        } catch (e: Exception) {
            url
        }
    }
}
