package com.bsbeetle.bananaspaghetti.directGit.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.StringWriter
import java.security.SecureRandom
import java.security.Security
import java.util.Base64
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.jcajce.JcaPEMWriter
import org.bouncycastle.util.io.pem.PemObject

/**
 * SSH Key Manager V2 - Manages ED25519 SSH keys for Git operations.
 *
 * Uses ED25519 keys with BouncyCastle for Android compatibility. Keys are stored securely using
 * EncryptedSharedPreferences.
 */
class GitSshKeyManagerV2(private val context: Context) {

    companion object {
        private const val TAG = "GitSshKeyManagerV2"
        private const val PREFS_NAME = "git_ssh_keys_v2"
        private const val KEY_PRIVATE = "private_key"
        private const val KEY_PUBLIC = "public_key"
        private const val KEY_USERNAME = "git_username"

        init {
            // Register BouncyCastle as security provider
            if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                Security.addProvider(BouncyCastleProvider())
            }
        }
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

    /**
     * Generate a new ED25519 key pair.
     *
     * @return true if generation succeeded, false otherwise
     */
    fun generateKeyPair(): Boolean {
        return try {
            Log.d(TAG, "Generating ED25519 SSH key pair...")

            // Generate ED25519 key pair using BouncyCastle's crypto API
            val random = SecureRandom()
            val privateKeyParams =
                    org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters(random)
            val publicKeyParams = privateKeyParams.generatePublicKey()

            // Convert to standard key format
            val privateKeyInfo =
                    org.bouncycastle.crypto.util.PrivateKeyInfoFactory.createPrivateKeyInfo(
                            privateKeyParams
                    )
            val publicKeyInfo =
                    org.bouncycastle.crypto.util.SubjectPublicKeyInfoFactory
                            .createSubjectPublicKeyInfo(publicKeyParams)

            // Get encoded keys
            val privateKey = privateKeyInfo.encoded
            val publicKey = publicKeyInfo.encoded

            // Convert to PEM format
            val privateKeyPEM = convertToPEM(privateKey, "OPENSSH PRIVATE KEY")
            val publicKeyOpenSSH = convertED25519PublicKeyToOpenSSH(publicKey)

            // Store keys
            encryptedPrefs
                    .edit()
                    .putString(KEY_PRIVATE, Base64.getEncoder().encodeToString(privateKey))
                    .putString(KEY_PUBLIC, publicKeyOpenSSH)
                    .apply()

            Log.d(TAG, "ED25519 key pair generated successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate key pair", e)
            false
        }
    }

    /** Convert ED25519 public key to OpenSSH format */
    private fun convertED25519PublicKeyToOpenSSH(publicKeyBytes: ByteArray): String {
        try {
            val publicKeyInfo = SubjectPublicKeyInfo.getInstance(publicKeyBytes)
            val keyData = publicKeyInfo.publicKeyData.bytes

            // OpenSSH ED25519 public key format:
            // ssh-ed25519 <base64-encoded-key> comment
            val keyType = "ssh-ed25519"
            val username = getUsername() ?: "git@android"

            // Build the SSH public key blob
            val blob = ByteArray(4 + keyType.length + 4 + keyData.size)
            var offset = 0

            // Write key type length
            blob[offset++] = (keyType.length shr 24).toByte()
            blob[offset++] = (keyType.length shr 16).toByte()
            blob[offset++] = (keyType.length shr 8).toByte()
            blob[offset++] = keyType.length.toByte()

            // Write key type
            System.arraycopy(keyType.toByteArray(), 0, blob, offset, keyType.length)
            offset += keyType.length

            // Write key data length
            blob[offset++] = (keyData.size shr 24).toByte()
            blob[offset++] = (keyData.size shr 16).toByte()
            blob[offset++] = (keyData.size shr 8).toByte()
            blob[offset++] = keyData.size.toByte()

            // Write key data
            System.arraycopy(keyData, 0, blob, offset, keyData.size)

            // Encode to base64
            val base64Key = Base64.getEncoder().encodeToString(blob)

            return "$keyType $base64Key $username"
        } catch (e: Exception) {
            Log.e(TAG, "Failed to convert public key", e)
            throw e
        }
    }

    /** Convert key bytes to PEM format */
    private fun convertToPEM(keyBytes: ByteArray, type: String): String {
        val writer = StringWriter()
        val pemWriter = JcaPEMWriter(writer)
        pemWriter.writeObject(PemObject(type, keyBytes))
        pemWriter.close()
        return writer.toString()
    }

    /** Get the private key in PEM format */
    fun getPrivateKeyPEM(): String? {
        val privateKeyBase64 = encryptedPrefs.getString(KEY_PRIVATE, null) ?: return null
        val privateKeyBytes = Base64.getDecoder().decode(privateKeyBase64)
        return convertToPEM(privateKeyBytes, "OPENSSH PRIVATE KEY")
    }

    /** Get the public key in OpenSSH format */
    fun getPublicKey(): String? {
        return encryptedPrefs.getString(KEY_PUBLIC, null)
    }

    /** Check if keys exist */
    fun hasKeys(): Boolean {
        return encryptedPrefs.contains(KEY_PRIVATE) && encryptedPrefs.contains(KEY_PUBLIC)
    }

    /** Save username */
    fun saveUsername(username: String) {
        encryptedPrefs.edit().putString(KEY_USERNAME, username).apply()
    }

    /** Get username */
    fun getUsername(): String? {
        return encryptedPrefs.getString(KEY_USERNAME, null)
    }

    /** Delete all keys */
    fun deleteKeys() {
        encryptedPrefs.edit().clear().apply()
        Log.d(TAG, "SSH keys deleted")
    }
}
