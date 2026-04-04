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

/**
 * @param session         Active JSch session.
 * @param onSessionDown   Called (once) when the session is detected as disconnected so that
 *                        callers (e.g. SshViewModel) can surface a reconnect prompt.
 */
class SshFileSystem(
    private val session: Session,
    private val onSessionDown: () -> Unit = {}
) {

    companion object {
        private const val TAG = "SshFileSystem"
    }

    private var sftp: ChannelSftp = openChannel()

    private fun openChannel(): ChannelSftp =
        (session.openChannel("sftp") as ChannelSftp).also {
            it.connect()
            Log.d(TAG, "SFTP channel connected")
        }

    /** True as long as the underlying JSch session reports it is still connected. */
    val isSessionConnected: Boolean get() = session.isConnected

    /**
     * Execute [op] on the current SFTP channel. If it throws for any reason (stale channel,
     * server-side close, network hiccup, etc.) the channel is discarded, a fresh one is opened,
     * and [op] is attempted once more. Only propagates on the second failure.
     *
     * If the JSch Session itself is down the [onSessionDown] callback is invoked before throwing.
     */
    private fun <T> withRetry(op: (ChannelSftp) -> T): T {
        if (!session.isConnected) {
            onSessionDown()
            throw Exception("SSH session is disconnected — please reconnect.")
        }
        return try {
            op(sftp)
        } catch (first: Exception) {
            Log.w(TAG, "SFTP op failed (${first.message}), reconnecting channel and retrying…")
            try { sftp.disconnect() } catch (_: Exception) {}
            // isConnected can still return true even when the TCP socket is broken (JSch's flag
            // is updated lazily).  If opening a fresh channel throws, the session is effectively
            // down regardless of what isConnected says.
            if (!session.isConnected) {
                onSessionDown()
                throw Exception("SSH session is disconnected — please reconnect.")
            }
            try {
                sftp = openChannel()
                op(sftp)
            } catch (retryEx: Exception) {
                // openChannel or the second op failed — TCP socket is broken.
                onSessionDown()
                throw retryEx
            }
        }
    }

    fun listDirectory(path: String): List<RemoteDirEntry> {
        return try {
            withRetry { ch ->
                @Suppress("UNCHECKED_CAST")
                val entries = ch.ls(path) as java.util.Vector<ChannelSftp.LsEntry>
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
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to list directory after retry: $path", e)
            emptyList()
        }
    }

    fun readFile(path: String): String =
        withRetry { ch -> ch.get(path).bufferedReader(Charsets.UTF_8).use { it.readText() } }

    fun writeFile(path: String, content: String) {
        withRetry { ch ->
            val bytes = content.toByteArray(Charsets.UTF_8)
            ch.put(bytes.inputStream(), path, ChannelSftp.OVERWRITE)
            Log.d(TAG, "Wrote ${bytes.size} bytes to $path")
        }
    }

    fun disconnect() {
        try {
            sftp.disconnect()
            Log.d(TAG, "SFTP channel disconnected")
        } catch (_: Exception) {}
    }
}
