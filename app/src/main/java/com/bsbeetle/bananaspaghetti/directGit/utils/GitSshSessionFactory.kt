package com.bsbeetle.bananaspaghetti.directGit.utils

import com.jcraft.jsch.JSch
import com.jcraft.jsch.JSchException
import com.jcraft.jsch.Session
import org.eclipse.jgit.transport.ssh.jsch.JschConfigSessionFactory
import org.eclipse.jgit.transport.ssh.jsch.OpenSshConfig
import org.eclipse.jgit.util.FS

/**
 * Git SSH Session Factory
 *
 * Configures JGit to use SSH keys from GitSshKeyManager for authentication. This factory is used by
 * JGit for all SSH-based Git operations.
 *
 * Key responsibilities:
 * - Load SSH private key from secure storage
 * - Configure SSH session for Git operations
 * - Disable strict host key checking (for ease of use)
 * - Handle SSH authentication automatically
 *
 * Usage:
 * ```kotlin
 * val keyManager = GitSshKeyManager(context)
 * val sessionFactory = GitSshSessionFactory(keyManager)
 * SshSessionFactory.setInstance(sessionFactory)
 *
 * // Now all Git operations use SSH automatically
 * git.push().call()
 * ```
 */
class GitSshSessionFactory(private val sshKeyManager: GitSshKeyManager) :
        JschConfigSessionFactory() {

    /**
     * Configures the SSH session.
     *
     * Disables strict host key checking to avoid "unknown host" errors. In production, you might
     * want to verify known_hosts for security.
     *
     * @param hc Host configuration
     * @param session SSH session to configure
     */
    override fun configure(hc: OpenSshConfig.Host, session: Session) {
        // Disable strict host key checking
        // This allows connecting to GitHub without manual host verification
        session.setConfig("StrictHostKeyChecking", "no")

        // Optional: Set connection timeout (30 seconds)
        session.timeout = 30000
    }

    /**
     * Creates JSch instance with our SSH key loaded.
     *
     * This is called by JGit when establishing SSH connections. Loads the private key from
     * GitSshKeyManager and adds it to JSch.
     *
     * @param fs File system (unused, required by interface)
     * @return Configured JSch instance
     * @throws JSchException if key loading fails
     */
    override fun createDefaultJSch(fs: FS): JSch {
        val jsch = super.createDefaultJSch(fs)

        // Load private key from secure storage
        val privateKeyBytes = sshKeyManager.getPrivateKeyBytes()

        if (privateKeyBytes != null) {
            try {
                // Add identity (private key) to JSch
                // null passphrase because we don't encrypt the key with a password
                jsch.addIdentity(
                        "git-ssh-key", // Key name (arbitrary)
                        privateKeyBytes, // Private key bytes
                        null, // Public key (not needed)
                        null // Passphrase (none)
                )
            } catch (e: JSchException) {
                throw JSchException("Failed to load SSH key: ${e.message}", e)
            }
        } else {
            throw JSchException("SSH key not found. Please generate SSH keys first.")
        }

        return jsch
    }
}
