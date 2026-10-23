package com.bsbeetle.bananaspaghetti.utils.ssh

import android.content.Context
import android.util.Log
import com.bsbeetle.bananaspaghetti.directGit.utils.GitSshKeyManager
import com.bsbeetle.bananaspaghetti.directGit.utils.UbuntuSshKeyManager
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.jcraft.jsch.SocketFactory
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket

class SshConnectionManager(private val context: Context) {

    companion object {
        private const val TAG = "SshConnectionManager"
        private const val CONNECT_TIMEOUT_MS = 15_000

        init {
            // Android has no EdDSA provider for apps, so use JSch's Bouncy Castle implementation
            // for the ED25519 keys that Settings generates.
            JSch.setConfig("ssh-ed25519", "com.jcraft.jsch.bc.SignatureEd25519")
            JSch.setConfig("keypairgen.eddsa", "com.jcraft.jsch.bc.KeyPairGenEdDSA")
            JSch.setConfig("keypairgen_fromprivate.eddsa", "com.jcraft.jsch.bc.KeyPairGenEdDSA")
        }
    }

    /**
     * Socket factory that enables TCP-level SO_KEEPALIVE and TCP_NODELAY.
     * SO_KEEPALIVE asks the OS to send periodic TCP probes on idle connections, preventing
     * Android's WiFi power-saving mode from silently breaking the socket while the user is
     * in another session.
     */
    private class KeepaliveSocketFactory : SocketFactory {
        override fun createSocket(host: String, port: Int): Socket =
            Socket(host, port).also {
                it.keepAlive = true   // OS-level TCP keepalive probes
                it.tcpNoDelay = true  // flush immediately, no Nagle buffering
            }
        override fun getInputStream(socket: Socket): InputStream = socket.getInputStream()
        override fun getOutputStream(socket: Socket): OutputStream = socket.getOutputStream()
    }

    fun connect(params: SshConnectionParams): Session {
        val jsch = JSch()

        when (params.authType) {
            SshConnectionParams.AuthType.APP_SSH_KEYS -> {
                // Settings generates the ED25519 key with ssh-keygen (UbuntuSshKeyManager). The
                // older RSA key (GitSshKeyManager) is only used when that one is missing.
                val privateKeyBytes =
                    UbuntuSshKeyManager(context).getPrivateKeyPEM()?.toByteArray()
                        ?: GitSshKeyManager(context).getPrivateKeyBytes()
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
        session.setSocketFactory(KeepaliveSocketFactory())

        if (params.authType == SshConnectionParams.AuthType.PASSWORD) {
            session.setPassword(params.password)
        }

        val config = java.util.Properties()
        config["StrictHostKeyChecking"] = "no"
        session.setConfig(config)

        // Application-level SSH keep-alives (in addition to the TCP-level SO_KEEPALIVE set
        // in KeepaliveSocketFactory).  Together they prevent both the SSH server and Android's
        // WiFi power management from silently killing idle connections.
        session.setServerAliveInterval(30_000) // send SSH keep-alive every 30 s
        session.setServerAliveCountMax(6)       // 6 × 30 s = 3 min grace before declaring dead

        Log.d(TAG, "Connecting to ${params.username}@${params.host}:${params.port}")
        session.connect(CONNECT_TIMEOUT_MS)
        Log.d(TAG, "Connected successfully")

        return session
    }
}
