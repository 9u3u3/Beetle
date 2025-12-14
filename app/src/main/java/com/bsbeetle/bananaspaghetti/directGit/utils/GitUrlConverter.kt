package com.bsbeetle.bananaspaghetti.directGit.utils

/**
 * Git URL Converter
 *
 * Converts between HTTPS and SSH Git URLs. Essential for SSH-only authentication as we need to
 * convert user-provided HTTPS URLs to SSH format.
 *
 * Supported conversions:
 * - GitHub: https://github.com/user/repo.git ↔ git@github.com:user/repo.git
 * - GitLab: https://gitlab.com/user/repo.git ↔ git@gitlab.com:user/repo.git
 * - Bitbucket: https://bitbucket.org/user/repo.git ↔ git@bitbucket.org:user/repo.git
 *
 * Usage:
 * ```kotlin
 * // User provides HTTPS URL
 * val httpsUrl = "https://github.com/user/repo.git"
 *
 * // Convert to SSH for authentication
 * val sshUrl = GitUrlConverter.httpsToSsh(httpsUrl)
 * // Result: "git@github.com:user/repo.git"
 *
 * // Use SSH URL for Git operations
 * git.clone().setURI(sshUrl).call()
 * ```
 */
object GitUrlConverter {

    /**
     * Converts HTTPS URL to SSH format.
     *
     * Transforms URLs from HTTPS format to SSH format for authentication. Supports GitHub, GitLab,
     * and Bitbucket.
     *
     * @param url HTTPS Git URL
     * @return SSH Git URL
     *
     * Examples:
     * ```kotlin
     * httpsToSsh("https://github.com/user/repo.git")
     * // Returns: "git@github.com:user/repo.git"
     *
     * httpsToSsh("https://gitlab.com/user/repo.git")
     * // Returns: "git@gitlab.com:user/repo.git"
     * ```
     */
    fun httpsToSsh(url: String): String {
        return when {
            // GitHub
            url.startsWith("https://github.com/") -> {
                val path = url.removePrefix("https://github.com/")
                "git@github.com:$path"
            }

            // GitLab
            url.startsWith("https://gitlab.com/") -> {
                val path = url.removePrefix("https://gitlab.com/")
                "git@gitlab.com:$path"
            }

            // Bitbucket
            url.startsWith("https://bitbucket.org/") -> {
                val path = url.removePrefix("https://bitbucket.org/")
                "git@bitbucket.org:$path"
            }

            // Already SSH or unknown format - return as-is
            else -> url
        }
    }

    /**
     * Converts SSH URL to HTTPS format.
     *
     * Useful for displaying URLs to users in a more familiar format.
     *
     * @param url SSH Git URL
     * @return HTTPS Git URL
     *
     * Examples:
     * ```kotlin
     * sshToHttps("git@github.com:user/repo.git")
     * // Returns: "https://github.com/user/repo.git"
     * ```
     */
    fun sshToHttps(url: String): String {
        return when {
            // GitHub
            url.startsWith("git@github.com:") -> {
                val path = url.removePrefix("git@github.com:")
                "https://github.com/$path"
            }

            // GitLab
            url.startsWith("git@gitlab.com:") -> {
                val path = url.removePrefix("git@gitlab.com:")
                "https://gitlab.com/$path"
            }

            // Bitbucket
            url.startsWith("git@bitbucket.org:") -> {
                val path = url.removePrefix("git@bitbucket.org:")
                "https://bitbucket.org/$path"
            }

            // Already HTTPS or unknown format - return as-is
            else -> url
        }
    }

    /**
     * Checks if URL is in SSH format.
     *
     * @param url Git URL to check
     * @return True if URL is SSH format
     *
     * Example:
     * ```kotlin
     * isSshUrl("git@github.com:user/repo.git")  // true
     * isSshUrl("https://github.com/user/repo.git")  // false
     * ```
     */
    fun isSshUrl(url: String): Boolean {
        return url.startsWith("git@")
    }

    /**
     * Checks if URL is in HTTPS format.
     *
     * @param url Git URL to check
     * @return True if URL is HTTPS format
     *
     * Example:
     * ```kotlin
     * isHttpsUrl("https://github.com/user/repo.git")  // true
     * isHttpsUrl("git@github.com:user/repo.git")  // false
     * ```
     */
    fun isHttpsUrl(url: String): Boolean {
        return url.startsWith("https://")
    }

    /**
     * Ensures URL is in SSH format.
     *
     * Converts to SSH if needed, otherwise returns as-is. Useful when you always want SSH format
     * regardless of input.
     *
     * @param url Git URL (HTTPS or SSH)
     * @return SSH Git URL
     *
     * Example:
     * ```kotlin
     * ensureSsh("https://github.com/user/repo.git")
     * // Returns: "git@github.com:user/repo.git"
     *
     * ensureSsh("git@github.com:user/repo.git")
     * // Returns: "git@github.com:user/repo.git" (unchanged)
     * ```
     */
    fun ensureSsh(url: String): String {
        return if (isHttpsUrl(url)) httpsToSsh(url) else url
    }
}
