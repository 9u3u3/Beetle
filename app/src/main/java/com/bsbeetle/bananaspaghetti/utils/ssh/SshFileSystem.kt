package com.bsbeetle.bananaspaghetti.utils.ssh

import android.util.Log
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.Session

data class RemoteDirEntry(
    val fullPath: String,
    val name: String,
    val isDir: Boolean,
    val size: Long
)

class SshFileSystem(session: Session) {

    companion object {
        private const val TAG = "SshFileSystem"
    }

    private val sftp: ChannelSftp = (session.openChannel("sftp") as ChannelSftp).also {
        it.connect()
        Log.d(TAG, "SFTP channel connected")
    }

    fun listDirectory(path: String): List<RemoteDirEntry> {
        return try {
            @Suppress("UNCHECKED_CAST")
            val entries = sftp.ls(path) as java.util.Vector<ChannelSftp.LsEntry>
            entries.mapNotNull { entry ->
                val name = entry.filename
                if (name == "." || name == "..") return@mapNotNull null
                val fullPath = if (path.endsWith("/")) "$path$name" else "$path/$name"
                RemoteDirEntry(
                    fullPath = fullPath,
                    name = name,
                    isDir = entry.attrs.isDir,
                    size = entry.attrs.size
                )
            }.sortedWith(
                compareBy<RemoteDirEntry> { !it.isDir }.thenBy { it.name.lowercase() }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to list directory: $path", e)
            emptyList()
        }
    }

    fun readFile(path: String): String {
        return try {
            sftp.get(path).bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read file: $path", e)
            throw e
        }
    }

    fun writeFile(path: String, content: String) {
        try {
            val bytes = content.toByteArray(Charsets.UTF_8)
            sftp.put(bytes.inputStream(), path, ChannelSftp.OVERWRITE)
            Log.d(TAG, "Wrote ${bytes.size} bytes to $path")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write file: $path", e)
            throw e
        }
    }

    fun disconnect() {
        try {
            sftp.disconnect()
            Log.d(TAG, "SFTP channel disconnected")
        } catch (_: Exception) {}
    }
}
