package com.bsbeetle.bananaspaghetti.utils.git

import android.net.Uri

/**
 * Git URI Converter Utility
 *
 * Converts Android DocumentFile content URIs to real file system paths. This is necessary because
 * Android's Storage Access Framework uses content URIs (like
 * content://com.android.externalstorage.documents/tree/primary%3Afolder) instead of direct file
 * paths.
 *
 * Key responsibilities:
 * - Convert content URIs to file system paths
 * - Handle URL encoding (%3A, %2F, etc.)
 * - Support external storage paths
 *
 * Dependencies:
 * - Android Uri class
 */
object GitUriConverter {

    /**
     * Extracts real file system path from DocumentFile content URI.
     *
     * Handles Android's Storage Access Framework URIs and converts them to actual file paths that
     * can be used with File operations.
     *
     * Supported URI formats:
     * - content://...primary:folder → /storage/emulated/0/folder
     * - content://...primary%3Afolder → /storage/emulated/0/folder
     * - content://...primary:path/to/folder → /storage/emulated/0/path/to/folder
     *
     * @param uri Android content URI from DocumentFile
     * @return Real file system path, or null if conversion fails
     *
     * Example:
     * ```kotlin
     * val uri = Uri.parse("content://...primary%3Amyrepo")
     * val path = GitUriConverter.uriToPath(uri)
     * // path = "/storage/emulated/0/myrepo"
     * ```
     */
    fun uriToPath(uri: Uri): String? {
        val uriString = uri.toString()

        // Extract the path after "primary:" or "primary%3A"
        return when {
            // Handle unencoded "primary:"
            uriString.contains("primary:") -> {
                val pathPart =
                        uriString
                                .substringAfter("primary:")
                                .substringBefore("/document/")
                                .replace("%2F", "/") // Decode forward slashes
                                .replace("%3A", ":") // Decode colons
                "/storage/emulated/0/$pathPart"
            }

            // Handle URL-encoded "primary%3A"
            uriString.contains("primary%3A") -> {
                val pathPart =
                        uriString
                                .substringAfter("primary%3A")
                                .substringBefore("/document/")
                                .replace("%2F", "/") // Decode forward slashes
                                .replace("%3A", ":") // Decode colons
                "/storage/emulated/0/$pathPart"
            }

            // Unknown URI format
            else -> null
        }
    }

    /**
     * Checks if a URI represents external storage.
     *
     * External storage is typically /storage/emulated/0/ or /sdcard/ and is accessible to the user.
     *
     * @param uri Android content URI
     * @return True if URI points to external storage
     *
     * Example:
     * ```kotlin
     * val uri = Uri.parse("content://...primary%3Afolder")
     * if (GitUriConverter.isExternalStorage(uri)) {
     *     // Safe to use for Git repos
     * }
     * ```
     */
    fun isExternalStorage(uri: Uri): Boolean {
        val uriString = uri.toString()
        return uriString.contains("primary") || uriString.contains("externalstorage")
    }

    /**
     * Decodes URL-encoded path components.
     *
     * Converts URL encoding (%XX) back to regular characters. Common encodings:
     * - %2F → /
     * - %3A → :
     * - %20 → space
     *
     * @param encoded URL-encoded string
     * @return Decoded string
     *
     * Example:
     * ```kotlin
     * val decoded = GitUriConverter.decodePathComponent("folder%2Fsubfolder")
     * // decoded = "folder/subfolder"
     * ```
     */
    fun decodePathComponent(encoded: String): String {
        return encoded.replace("%2F", "/")
                .replace("%3A", ":")
                .replace("%20", " ")
                .replace("%2E", ".")
                .replace("%2D", "-")
                .replace("%5F", "_")
    }
}
