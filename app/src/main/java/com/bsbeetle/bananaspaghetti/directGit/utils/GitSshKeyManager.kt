package com.bsbeetle.bananaspaghetti.directGit.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.jcraft.jsch.JSch
import com.jcraft.jsch.KeyPair
import java.io.ByteArrayOutputStream

/**
 * Git SSH Key Manager
 *
 * Manages SSH key generation, storage, and retrieval for Git authentication. Uses Android Keystore
 * for secure private key storage and SharedPreferences for public key storage.
 *
 * Key responsibilities:
 * - Generate ED25519 SSH key pairs
 * - Store private keys securely in encrypted storage
 * - Export public keys for users to add to GitHub
 * - Check if keys exist
 * - Delete and regenerate keys
 *
 * Security:
 * - Private keys are encrypted using Android's EncryptedSharedPreferences
 * - Keys are backed by Android Keystore (hardware-backed when available)
 * - Public keys are safe to share and display
 *
 * Usage:
 * ```kotlin
 * val keyManager = GitSshKeyManager(context)
 *
 * // Generate keys (first time setup)
 * if (keyManager.generateKeyPair()) {
 *     val publicKey = keyManager.getPublicKey()
 *     // Show public key to user to copy to GitHub
 * }
 *
 * // Check if setup is complete
 * if (keyManager.hasKeys()) {
 *     // SSH is ready to use
 * }
 * ```
 */
class GitSshKeyManager(private val context: Context) {

    companion object {
        private const val TAG = "GitSshKeyManager"

        // SharedPreferences keys
        private const val PREFS_NAME = "git_ssh_keys"
        private const val KEY_PRIVATE = "ssh_private_key"
        private const val KEY_PUBLIC = "ssh_public_key"
        private const val KEY_USERNAME = "git_username"

        // SSH key type - RSA 4096-bit (JSch doesn't support ED25519)
        // RSA-4096 is still very secure and widely supported
        private const val KEY_TYPE = KeyPair.RSA
        private const val KEY_SIZE = 4096
    }

    // Encrypted SharedPreferences for secure key storage
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

    // ========================================================================
    // Key Generation
    // ========================================================================

    /**
     * Generates a new SSH key pair.
     *
     * Creates an ED25519 key pair using JSch, stores the private key in encrypted storage, and
     * saves the public key in OpenSSH format.
     *
     * @return True if generation succeeded, false otherwise
     *
     * Example:
     * ```kotlin
     * if (keyManager.generateKeyPair()) {
     *     println("Keys generated successfully!")
     *     val publicKey = keyManager.getPublicKey()
     *     // Show publicKey to user
     * } else {
     *     println("Failed to generate keys")
     * }
     * ```
     */
    fun generateKeyPair(): Boolean {
        return try {
            Log.d(TAG, "Generating RSA-4096 SSH key pair...")

            // Create JSch instance and generate RSA key pair
            val jsch = JSch()
            val keyPair = KeyPair.genKeyPair(jsch, KEY_TYPE, KEY_SIZE)

            // Export private key to byte array
            val privateKeyStream = ByteArrayOutputStream()
            keyPair.writePrivateKey(privateKeyStream)
            val privateKeyBytes = privateKeyStream.toByteArray()

            // Export public key in OpenSSH format
            val publicKeyStream = ByteArrayOutputStream()
            keyPair.writePublicKey(publicKeyStream, "git@android")
            val publicKey = publicKeyStream.toString("UTF-8")

            // Store keys securely
            encryptedPrefs
                    .edit()
                    .putString(
                            KEY_PRIVATE,
                            android.util.Base64.encodeToString(
                                    privateKeyBytes,
                                    android.util.Base64.NO_WRAP
                            )
                    )
                    .putString(KEY_PUBLIC, publicKey)
                    .apply()

            // Clean up
            keyPair.dispose()

            Log.d(TAG, "SSH key pair generated successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate SSH key pair", e)
            false
        }
    }

    // ========================================================================
    // Key Retrieval
    // ========================================================================

    /**
     * Gets the public key in OpenSSH format.
     *
     * Returns the public key that the user should copy to GitHub. The key is in the format:
     * "ssh-rsa AAAA... git@android"
     *
     * @return Public key string, or null if not generated
     *
     * Example:
     * ```kotlin
     * val publicKey = keyManager.getPublicKey()
     * if (publicKey != null) {
     *     // Copy to clipboard or display to user
     *     clipboard.setText(publicKey)
     * }
     * ```
     */
    fun getPublicKey(): String? {
        return encryptedPrefs.getString(KEY_PUBLIC, null)
    }

    /**
     * Gets the private key bytes for JGit.
     *
     * Internal use only - retrieves the private key for SSH authentication. The key is decrypted
     * from secure storage.
     *
     * @return Private key bytes, or null if not generated
     */
    internal fun getPrivateKeyBytes(): ByteArray? {
        val encoded = encryptedPrefs.getString(KEY_PRIVATE, null) ?: return null
        return try {
            android.util.Base64.decode(encoded, android.util.Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode private key", e)
            null
        }
    }

    // ========================================================================
    // Key Management
    // ========================================================================

    /**
     * Checks if SSH keys have been generated.
     *
     * @return True if both public and private keys exist
     *
     * Example:
     * ```kotlin
     * if (!keyManager.hasKeys()) {
     *     // Show SSH setup dialog
     *     showSshSetupDialog()
     * }
     * ```
     */
    fun hasKeys(): Boolean {
        return getPublicKey() != null && getPrivateKeyBytes() != null
    }

    /**
     * Deletes all stored SSH keys.
     *
     * Use this when regenerating keys or resetting SSH setup. The user will need to remove the old
     * public key from GitHub and add the new one.
     *
     * Example:
     * ```kotlin
     * // Regenerate keys
     * keyManager.deleteKeys()
     * keyManager.generateKeyPair()
     * ```
     */
    fun deleteKeys() {
        encryptedPrefs.edit().remove(KEY_PRIVATE).remove(KEY_PUBLIC).apply()
        Log.d(TAG, "SSH keys deleted")
    }

    // ========================================================================
    // Username Management
    // ========================================================================

    /**
     * Saves the GitHub username.
     *
     * Used for constructing SSH URLs (git@github.com:username/repo.git)
     *
     * @param username GitHub username
     *
     * Example:
     * ```kotlin
     * keyManager.saveUsername("octocat")
     * ```
     */
    fun saveUsername(username: String) {
        encryptedPrefs.edit().putString(KEY_USERNAME, username).apply()
    }

    /**
     * Gets the stored GitHub username.
     *
     * @return GitHub username, or null if not set
     */
    fun getUsername(): String? {
        return encryptedPrefs.getString(KEY_USERNAME, null)
    }

    // ========================================================================
    // Utility Functions
    // ========================================================================

    /**
     * Gets a formatted display of the public key.
     *
     * Useful for showing the key to users in a readable format.
     *
     * @return Formatted public key with line breaks
     */
    fun getFormattedPublicKey(): String? {
        val key = getPublicKey() ?: return null
        return key.trim()
    }

    /**
     * Gets the key fingerprint for verification.
     *
     * The fingerprint can be used to verify the key on GitHub matches the one stored in the app.
     *
     * @return Key fingerprint, or null if keys don't exist
     */
    fun getKeyFingerprint(): String? {
        val privateKeyBytes = getPrivateKeyBytes() ?: return null
        return try {
            val jsch = JSch()
            val keyPair = KeyPair.load(jsch, privateKeyBytes, null)
            val fingerprint = keyPair.getFingerPrint()
            keyPair.dispose()
            fingerprint
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get fingerprint", e)
            null
        }
    }
}
