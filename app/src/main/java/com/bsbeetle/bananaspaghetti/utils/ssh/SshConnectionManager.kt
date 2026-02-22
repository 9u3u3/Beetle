package com.bsbeetle.bananaspaghetti.utils.ssh

import android.content.Context
import android.util.Log
import com.bsbeetle.bananaspaghetti.directGit.utils.GitSshKeyManager
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session

class SshConnectionManager(private val context: Context) {

    companion object {
        private const val TAG = "SshConnectionManager"
        private const val CONNECT_TIMEOUT_MS = 15_000
    }

    fun connect(params: SshConnectionParams): Session {
        val jsch = JSch()

        when (params.authType) {
            SshConnectionParams.AuthType.APP_SSH_KEYS -> {
                val keyManager = GitSshKeyManager(context)
                val privateKeyBytes = keyManager.getPrivateKeyBytes()
                if (privateKeyBytes != null) {
                    jsch.addIdentity(
                        "beetle-ssh-key",
                        privateKeyBytes,
                        null,
                        null
                    )
                    Log.d(TAG, "Loaded app SSH key for authentication")
                } else {
                    throw Exception("No SSH keys found. Generate keys in Settings → SSH Keys first.")
                }
            }
            SshConnectionParams.AuthType.PASSWORD -> {
                // Password set on session below
            }
        }

        val session = jsch.getSession(params.username, params.host, params.port)

        if (params.authType == SshConnectionParams.AuthType.PASSWORD) {
            session.setPassword(params.password)
        }

        val config = java.util.Properties()
        config["StrictHostKeyChecking"] = "no"
        session.setConfig(config)

        Log.d(TAG, "Connecting to ${params.username}@${params.host}:${params.port}")
        session.connect(CONNECT_TIMEOUT_MS)
        Log.d(TAG, "Connected successfully")

        return session
    }
}
