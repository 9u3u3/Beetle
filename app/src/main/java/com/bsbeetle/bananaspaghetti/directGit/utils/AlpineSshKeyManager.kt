package com.bsbeetle.bananaspaghetti.directGit.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
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

    /** Check if SSH keys exist */
    fun hasKeys(): Boolean {
        val hasPrivate = encryptedPrefs.contains(KEY_PRIVATE)
        val hasPublic = encryptedPrefs.contains(KEY_PUBLIC)
        return hasPrivate && hasPublic
    }

    /** Generate ED25519 SSH key pair using Alpine's native ssh-keygen via proot */
    suspend fun generateKeyPair(): Boolean =
            withContext(Dispatchers.IO) {
                try {
                    Log.d(TAG, "🔑 Generating ED25519 SSH key pair using Alpine/proot...")

                    // Path configuration - same as Tenebrio uses
                    val PREFIX = context.filesDir
                    val nativeDir = context.applicationInfo.nativeLibraryDir
                    val alpine = java.io.File(PREFIX, "alpine")
                    val tmp = java.io.File(PREFIX, "tmp")
                    val proot = java.io.File(nativeDir, "libproot-xed.so")
                    val prootLoader = java.io.File(nativeDir, "libproot.so")
                    val prootLoader32 = java.io.File(nativeDir, "libproot32.so")

                    // Check if Alpine is installed
                    val alpineBusybox = java.io.File(alpine, "bin/busybox")
                    if (!alpineBusybox.exists()) {
                        Log.e(
                                TAG,
                                "❌ Alpine not installed - please open terminal once to initialize"
                        )
                        throw Exception(
                                "Alpine not initialized. Please open a Terminal once to set up Alpine, then try again."
                        )
                    }

                    // Ensure tmp directory exists
                    if (!tmp.exists()) tmp.mkdirs()

                    // Ensure libtalloc symlink exists (delete and recreate if needed)
                    val libtallocLink = java.io.File(PREFIX, "libtalloc.so.2")
                    val libtallocTarget = java.io.File(nativeDir, "libtalloc.so")
                    // Check using NOFOLLOW_LINKS to detect broken symlinks too
                    val linkPath = libtallocLink.toPath()
                    if (java.nio.file.Files.exists(
                                    linkPath,
                                    java.nio.file.LinkOption.NOFOLLOW_LINKS
                            )
                    ) {
                        // Delete existing file/symlink to ensure clean state
                        java.nio.file.Files.delete(linkPath)
                    }
                    java.nio.file.Files.createSymbolicLink(linkPath, libtallocTarget.toPath())

                    // Environment for proot
                    val envMap =
                            mapOf(
                                    "PREFIX" to PREFIX.absolutePath,
                                    "LD_LIBRARY_PATH" to PREFIX.absolutePath,
                                    "PROOT_TMP_DIR" to tmp.absolutePath,
                                    "PROOT_LOADER" to prootLoader.absolutePath,
                                    "PROOT_LOADER_32" to prootLoader32.absolutePath,
                                    "HOME" to "/home",
                                    "TERM" to "xterm-256color",
                                    "PATH" to
                                            "/bin:/sbin:/usr/bin:/usr/sbin:/usr/local/bin:/usr/local/sbin"
                            )

                    // Create the SSH key generation script
                    val sshGenScript =
                            """
                        #!/bin/sh
                        set -e
                        
                        # Set PATH explicitly for Alpine
                        export PATH=/bin:/sbin:/usr/bin:/usr/sbin:/usr/local/bin:/usr/local/sbin
                        export HOME=/home
                        
                        # Create .ssh directory
                        mkdir -p /home/.ssh
                        chmod 700 /home/.ssh
                        
                        # Install openssh if ssh-keygen not found
                        if ! command -v ssh-keygen > /dev/null 2>&1; then
                            echo "Installing openssh..."
                            apk update
                            apk add openssh
                        fi
                        
                        # Remove existing keys if present
                        rm -f /home/.ssh/id_ed25519 /home/.ssh/id_ed25519.pub
                        
                        # Generate ED25519 key pair
                        ssh-keygen -t ed25519 -f /home/.ssh/id_ed25519 -N "" -C "bananaspaghetti@android"
                        
                        # Verify keys were created
                        if [ -f /home/.ssh/id_ed25519 ] && [ -f /home/.ssh/id_ed25519.pub ]; then
                            echo "SUCCESS: Keys generated"
                            cat /home/.ssh/id_ed25519.pub
                        else
                            echo "FAILED: Keys not found"
                            exit 1
                        fi
                    """.trimIndent()

                    // Write script to Alpine filesystem
                    val scriptFile = java.io.File(alpine, "tmp/gen_ssh_key.sh")
                    scriptFile.parentFile?.mkdirs()
                    scriptFile.writeText(sshGenScript)
                    scriptFile.setExecutable(true)

                    Log.d(TAG, "🔐 Running SSH key generation script...")

                    // Execute the script through /bin/sh
                    val keygenResult =
                            runProotCommand(proot, alpine, envMap, "/bin/sh", "/tmp/gen_ssh_key.sh")
                    Log.d(TAG, "Script output: $keygenResult")

                    // Read generated keys from Alpine filesystem
                    val alpinePrivateKeyPath =
                            java.io.File(context.filesDir, "alpine/home/.ssh/id_ed25519")
                    val alpinePublicKeyPath =
                            java.io.File(context.filesDir, "alpine/home/.ssh/id_ed25519.pub")

                    // Wait for files with retry
                    var retries = 10
                    while (retries > 0 &&
                            (!alpinePrivateKeyPath.exists() || !alpinePublicKeyPath.exists())) {
                        Log.d(TAG, "⏳ Waiting for key files... (retries: $retries)")
                        delay(500)
                        retries--
                    }

                    if (!alpinePrivateKeyPath.exists() || !alpinePublicKeyPath.exists()) {
                        Log.e(TAG, "❌ Key files not found after generation")
                        Log.e(
                                TAG,
                                "Private key path: ${alpinePrivateKeyPath.absolutePath}, exists: ${alpinePrivateKeyPath.exists()}"
                        )
                        Log.e(
                                TAG,
                                "Public key path: ${alpinePublicKeyPath.absolutePath}, exists: ${alpinePublicKeyPath.exists()}"
                        )
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

    /** Run a command inside Alpine using proot */
    private fun runProotCommand(
            proot: java.io.File,
            alpine: java.io.File,
            env: Map<String, String>,
            vararg command: String
    ): String {
        val pb =
                ProcessBuilder(
                        proot.absolutePath,
                        "--link2symlink",
                        "-0", // Run as root
                        "-r",
                        alpine.absolutePath, // Root filesystem
                        "-b",
                        "/dev",
                        "-b",
                        "/proc",
                        "-b",
                        "/sys",
                        "-w",
                        "/home", // Working directory
                        *command
                )
        pb.environment().putAll(env)
        pb.redirectErrorStream(true)

        val process = pb.start()
        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()

        if (exitCode != 0) {
            Log.w(TAG, "Command ${command.joinToString(" ")} exited with code $exitCode")
            Log.w(TAG, "Output: $output")
        }

        return output
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
