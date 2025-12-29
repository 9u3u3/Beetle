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
 * Ubuntu SSH Key Manager - Generates ED25519 keys using Ubuntu's native ssh-keygen
 *
 * This manager uses Ubuntu Linux's native ssh-keygen command to generate proper ED25519 keys, which
 * are the latest and most secure SSH key format.
 *
 * Keys are stored encrypted and auto-configured in Ubuntu's ~/.ssh/ directory.
 */
class UbuntuSshKeyManager(private val context: Context) {

    companion object {
        private const val TAG = "UbuntuSshKeyManager"
        private const val PREFS_NAME = "ubuntu_ssh_keys"
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

    /** Generate ED25519 SSH key pair using Ubuntu's native ssh-keygen via proot */
    suspend fun generateKeyPair(): Boolean =
            withContext(Dispatchers.IO) {
                try {
                    Log.d(TAG, "🔑 Generating ED25519 SSH key pair using Ubuntu/proot...")

                    // Path configuration - same as Tenebrio uses
                    val PREFIX = context.filesDir
                    val nativeDir = context.applicationInfo.nativeLibraryDir
                    val ubuntu = java.io.File(PREFIX, "ubuntu")
                    val tmp = java.io.File(PREFIX, "tmp")
                    val proot = java.io.File(nativeDir, "libproot-xed.so")
                    val prootLoader = java.io.File(nativeDir, "libproot.so")
                    val prootLoader32 = java.io.File(nativeDir, "libproot32.so")

                    // Check if Ubuntu is installed
                    val ubuntuBash = java.io.File(ubuntu, "bin/bash")
                    if (!ubuntuBash.exists()) {
                        Log.e(
                                TAG,
                                "❌ Ubuntu not installed - please open terminal once to initialize"
                        )
                        throw Exception(
                                "Ubuntu not initialized. Please open a Terminal once to set up Ubuntu, then try again."
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

                    // Copy SSH key generation script from assets to Ubuntu tmp
                    val scriptFile = java.io.File(ubuntu, "tmp/gen_ssh_key.sh")
                    scriptFile.parentFile?.mkdirs()
                    context.assets.open("scripts/gen_ssh_key.sh").use { input ->
                        scriptFile.outputStream().use { output -> input.copyTo(output) }
                    }
                    scriptFile.setExecutable(true)

                    Log.d(TAG, "🔐 Running SSH key generation script...")

                    // Execute the script through /bin/bash
                    val keygenResult =
                            runProotCommand(proot, ubuntu, envMap, "/bin/bash", "/tmp/gen_ssh_key.sh")
                    Log.d(TAG, "Script output: $keygenResult")

                    // Read generated keys from Ubuntu filesystem
                    val ubuntuPrivateKeyPath =
                            java.io.File(context.filesDir, "ubuntu/home/.ssh/id_ed25519")
                    val ubuntuPublicKeyPath =
                            java.io.File(context.filesDir, "ubuntu/home/.ssh/id_ed25519.pub")

                    // Wait for files with retry
                    var retries = 10
                    while (retries > 0 &&
                            (!ubuntuPrivateKeyPath.exists() || !ubuntuPublicKeyPath.exists())) {
                        Log.d(TAG, "⏳ Waiting for key files... (retries: $retries)")
                        delay(500)
                        retries--
                    }

                    if (!ubuntuPrivateKeyPath.exists() || !ubuntuPublicKeyPath.exists()) {
                        Log.e(TAG, "❌ Key files not found after generation")
                        Log.e(
                                TAG,
                                "Private key path: ${ubuntuPrivateKeyPath.absolutePath}, exists: ${ubuntuPrivateKeyPath.exists()}"
                        )
                        Log.e(
                                TAG,
                                "Public key path: ${ubuntuPublicKeyPath.absolutePath}, exists: ${ubuntuPublicKeyPath.exists()}"
                        )
                        return@withContext false
                    }

                    val privateKey = ubuntuPrivateKeyPath.readText()
                    val publicKey = ubuntuPublicKeyPath.readText()

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

    /** Run a command inside Ubuntu using proot */
    private fun runProotCommand(
            proot: java.io.File,
            ubuntu: java.io.File,
            env: Map<String, String>,
            vararg command: String
    ): String {
        val pb =
                ProcessBuilder(
                        proot.absolutePath,
                        "--link2symlink",
                        "-0", // Run as root
                        "-r",
                        ubuntu.absolutePath, // Root filesystem
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
