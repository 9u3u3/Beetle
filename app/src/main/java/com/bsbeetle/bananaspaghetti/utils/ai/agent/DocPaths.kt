package com.bsbeetle.bananaspaghetti.utils.ai.agent

import android.net.Uri
import android.provider.DocumentsContract
import java.net.URLDecoder

/**
 * Turns the content URIs the file tree uses into real file system paths.
 *
 * The AI tools work on [java.io.File], and the Ubuntu environment binds `/storage` and `/data` at
 * the same paths, so one path works for both. Two document ID shapes occur:
 * - external storage: `primary:Projects/demo/main.c`, or `1A2B-3C4D:...` for an SD card;
 * - Beetle's own provider: the absolute path, for example `/data/user/0/.../ubuntu/home/a.py`.
 */
object DocPaths {

    /** An SD card or USB volume, for example `1A2B-3C4D`. */
    private val VOLUME_ID = Regex("[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}")

    fun toPath(uri: Uri?): String? {
        if (uri == null) return null
        if (uri.scheme == "file") return uri.path

        val id =
                runCatching { DocumentsContract.getDocumentId(uri) }.getOrNull()
                        ?: runCatching { DocumentsContract.getTreeDocumentId(uri) }.getOrNull()
                        ?: return null
        return idToPath(id)
    }

    fun toPath(uriString: String?): String? =
            if (uriString.isNullOrBlank()) null
            else if (uriString.startsWith("/")) uriString else toPath(Uri.parse(uriString))

    /** Pure mapping from a document ID to a path, kept separate so unit tests can call it. */
    fun idToPath(rawId: String): String? {
        val id = if (rawId.contains('%')) URLDecoder.decode(rawId, "UTF-8") else rawId
        return when {
            id.startsWith("/") -> id
            id.startsWith("raw:") -> id.removePrefix("raw:")
            id.startsWith("primary:") -> "/storage/emulated/0/" + id.removePrefix("primary:")
            VOLUME_ID.matches(id.substringBefore(':')) && id.contains(':') ->
                    "/storage/" + id.substringBefore(':') + "/" + id.substringAfter(':')
            // Other providers, such as Downloads (`msf:123`), have no file path.
            else -> null
        }?.trimEnd('/')
    }
}
