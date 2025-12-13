package com.bsbeetle.bananaspaghetti.directGit.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.SessionList
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.TerminalSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Alpine SSH Key Manager - Generates ED25519 keys using Alpine's native ssh-keygen
 *
 * This manager uses Alpine Linux's native ssh-keygen command to generate proper ED25519 keys, which
 * are the latest and most secure SSH key format.
 *
 * Keys are stored encrypted and auto-configured in Alpine's ~/.ssh/ directory.
 */
class AlpineSshKeyManager(private val context: Context) {

    companion object {
        private const val TAG = "AlpineSshKeyManager"
        private const val PREFS_NAME = "alpine_ssh_keys"
        private const val KEY_PRIVATE = "ed25519_private_key"
        private const val KEY_PUBLIC = "ed25519_public_key"
    }

    private val encryptedPrefs: SharedPreferences by lazy {
        val masterKey =
                MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()

        EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private val terminalManager = GitTerminalManager(context)

    /** Check if SSH keys exist */
    fun hasKeys(): Boolean {
        val hasPrivate = encryptedPrefs.contains(KEY_PRIVATE)
        val hasPublic = encryptedPrefs.contains(KEY_PUBLIC)
        return hasPrivate && hasPublic
    }

    /** Generate ED25519 SSH key pair using Alpine's native ssh-keygen */
    suspend fun generateKeyPair(): Boolean =
            withContext(Dispatchers.IO) {
                try {
                    Log.d(TAG, "🔑 Generating ED25519 SSH key pair using Alpine...")

                    // MUST use an existing terminal session where Alpine is already running
                    val session =
                            SessionList.sessions.filterIsInstance<TerminalSession>().firstOrNull {
                                it.outputFd != null
                            }

                    if (session == null) {
                        Log.e(TAG, "❌ No active terminal found! User must open a terminal first.")
                        throw Exception(
                                "Please open a Terminal first (tap + in bottom bar), wait for Alpine to load, then try again."
                        )
                    }

                    Log.d(TAG, "✅ Using existing terminal session (Alpine already running!)")
                    Log.d(TAG, "⚡ Executing commands immediately...")
                    delay(300) // Small delay to be safe

                    // Create .ssh directory
                    Log.d(TAG, "📁 Creating ~/.ssh directory...")
                    session.insertText("mkdir -p ~/.ssh\n")
                    delay(500)
                    session.insertText("chmod 700 ~/.ssh\n")
                    delay(500)

                    // Update package index and install openssh
                    Log.d(TAG, "📦 Updating package index...")
                    session.insertText("apk update\n")
                    delay(3000) // Wait for index update
                    Log.d(TAG, "📦 apk update command sent")

                    Log.d(TAG, "📦 Installing openssh...")
                    session.insertText("apk add openssh\n")
                    delay(7000) // Wait longer for installation
                    Log.d(TAG, "📦 apk add openssh command sent")

                    // Generate key using Alpine's ssh-keygen
                    Log.d(TAG, "🔐 Running ssh-keygen...")
                    session.insertText(
                            "ssh-keygen -t ed25519 -f ~/.ssh/id_ed25519 -N '' -C 'bananaspaghetti@android'\n"
                    )

                    // Wait for ssh-keygen to complete
                    delay(2000)

                    // Verify files were created
                    Log.d(TAG, "🔍 Verifying key files...")
                    session.insertText("ls -la ~/.ssh/\n")
                    delay(500)

                    Log.d(TAG, "✅ ssh-keygen commands executed")

                    // Read generated keys from Alpine filesystem
                    // NOTE: $HOME is /home in Alpine init script, so ~/.ssh = /home/.ssh
                    val alpinePrivateKeyPath =
                            java.io.File(context.filesDir, "alpine/home/.ssh/id_ed25519")
                    val alpinePublicKeyPath =
                            java.io.File(context.filesDir, "alpine/home/.ssh/id_ed25519.pub")

                    // Wait for files to appear (with retry)
                    var retries = 10
                    while (retries > 0 &&
                            (!alpinePrivateKeyPath.exists() || !alpinePublicKeyPath.exists())) {
                        Log.d(TAG, "⏳ Waiting for key files to appear... (retries left: $retries)")
                        delay(500)
                        retries--
                    }

                    if (!alpinePrivateKeyPath.exists() || !alpinePublicKeyPath.exists()) {
                        Log.e(TAG, "❌ Key files not found after generation")
                        Log.e(TAG, "Private key exists: ${alpinePrivateKeyPath.exists()}")
                        Log.e(TAG, "Public key exists: ${alpinePublicKeyPath.exists()}")
                        Log.e(TAG, "Private key path: ${alpinePrivateKeyPath.absolutePath}")
                        Log.e(TAG, "Public key path: ${alpinePublicKeyPath.absolutePath}")
                        return@withContext false
                    }

                    val privateKey = alpinePrivateKeyPath.readText()
                    val publicKey = alpinePublicKeyPath.readText()

                    // Store keys encrypted
                    encryptedPrefs
                            .edit()
                            .putString(KEY_PRIVATE, privateKey)
                            .putString(KEY_PUBLIC, publicKey)
                            .apply()

                    Log.d(TAG, "🎉 ED25519 keys generated and stored!")
                    Log.d(TAG, "Public key: $publicKey")

                    true
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Failed to generate key pair", e)
                    false
                }
            }

    /** Get public key in OpenSSH format */
    fun getPublicKey(): String? {
        return encryptedPrefs.getString(KEY_PUBLIC, null)
    }

    /** Get private key in PEM format */
    fun getPrivateKeyPEM(): String? {
        return encryptedPrefs.getString(KEY_PRIVATE, null)
    }

    /** Delete stored keys */
    fun deleteKeys(): Boolean {
        return try {
            encryptedPrefs.edit().remove(KEY_PRIVATE).remove(KEY_PUBLIC).apply()
            Log.d(TAG, "🗑️ Keys deleted")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to delete keys", e)
            false
        }
    }
}
