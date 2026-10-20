package com.bsbeetle.bananaspaghetti.utils.assetHandling

import android.content.Context
import android.util.Log
import fi.iki.elonen.NanoHTTPD
import java.io.InputStream
import java.net.SocketException
import java.net.SocketTimeoutException
import java.util.concurrent.Executors

class AssetServer(private val context: Context, port: Int = 8080) : NanoHTTPD(port) {

    companion object {
        private const val TAG = "AssetServer"

        @Volatile private var instance: AssetServer? = null

        /**
         * Start the AssetServer only if it isn't already running.
         * Guards against EADDRINUSE when the ViewModel is recreated
         * (e.g. after tapping the terminal notification).
         */
        fun startIfNotRunning(context: Context, port: Int = 8080) {
            if (instance?.isAlive == true) return
            synchronized(this) {
                if (instance?.isAlive == true) return
                try {
                    instance = AssetServer(context.applicationContext, port).also { it.start() }
                    Log.d(TAG, "AssetServer started on port $port")
                } catch (e: Exception) {
                    Log.w(TAG, "AssetServer failed to start on port $port — may already be bound", e)
                }
            }
        }
    }

    // Use a thread pool to handle multiple concurrent connections
    // This is critical for supporting multiple WebView instances
    private val executor = Executors.newFixedThreadPool(4)

    init {
        // Enable async mode to allow multiple concurrent connections
        setAsyncRunner(
                object : AsyncRunner {
                    override fun closeAll() {
                        executor.shutdownNow()
                    }

                    override fun closed(clientHandler: ClientHandler?) {
                        // No-op, executor handles cleanup
                    }

                    override fun exec(code: ClientHandler?) {
                        code?.let { executor.execute(it) }
                    }
                }
        )
    }

    override fun serve(session: IHTTPSession?): Response {
        val uri = session?.uri ?: "/index.html"

        // Serve plugins from filesDir to allow dynamic loading via HTTP
        if (uri.startsWith("/plugins/")) {
            val relativePath = uri.removePrefix("/")
            val file = java.io.File(context.filesDir, relativePath)
            if (file.exists()) {
                val mime = "application/javascript"
                return newFixedLengthResponse(
                        Response.Status.OK,
                        mime,
                        java.io.FileInputStream(file),
                        file.length()
                )
            }
        }

        val assetPath = "editor$uri" // maps /index.html -> assets/editor/index.html

        return try {
            val stream: InputStream = context.assets.open(assetPath)
            val mime =
                    when {
                        uri.endsWith(".html") -> "text/html"
                        uri.endsWith(".js") -> "application/javascript"
                        uri.endsWith(".css") -> "text/css"
                        else -> "application/octet-stream"
                    }
            // Read fully so Content-Length is the true asset size — InputStream.available()
            // is NOT the file length and could serve truncated editor/terminal assets.
            val bytes = stream.use { it.readBytes() }
            newFixedLengthResponse(Response.Status.OK, mime, bytes.inputStream(), bytes.size.toLong())
        } catch (e: Exception) {
            if (e is SocketException || e is SocketTimeoutException) {
                return newFixedLengthResponse(Response.Status.NO_CONTENT, "text/plain", "")
            }
            newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "File not found")
        }
    }
}
