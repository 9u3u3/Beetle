package com.bsbeetle.bananaspaghetti.utils.ssh

data class SshConnectionParams(
    val host: String,
    val port: Int = 22,
    val username: String,
    val authType: AuthType = AuthType.APP_SSH_KEYS,
    val password: String = ""
) {
    enum class AuthType {
        APP_SSH_KEYS,  // Use RSA keys stored in GitSshKeyManager (always works with JSch)
        PASSWORD
    }
}
