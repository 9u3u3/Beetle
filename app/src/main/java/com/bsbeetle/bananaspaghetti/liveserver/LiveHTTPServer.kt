package com.bsbeetle.bananaspaghetti.liveserver

import android.content.Context
import android.util.Log
import fi.iki.elonen.NanoHTTPD
import java.io.File
import java.io.FileInputStream
import java.net.SocketException
import java.net.SocketTimeoutException

/**
 * Live HTTP Server for serving static web projects with live-reload support. Serves files from a
 * project directory and injects refresh scripts into HTML files.
 */
class LiveHTTPServer(
        private val context: Context,
        private val projectRoot: File,
        port: Int = 5500,
        private val entryFile: String? =
                null // Optional entry file (e.g., "test.html" instead of index.html)
) : NanoHTTPD(port) {

    companion object {
        private const val TAG = "LiveHTTPServer"

        // Script injected into HTML files for live reload
        private val LIVE_RELOAD_SCRIPT =
                """
            <script>
            (function() {
                var lastModified = Date.now();
                var checkInterval = 1000;
                
                function checkForUpdates() {
                    fetch('/__livereload/check?t=' + lastModified, { cache: 'no-store' })
                        .then(function(response) { return response.json(); })
                        .then(function(data) {
                            if (data.shouldReload) {
                                console.log('🔄 Live Server: Reloading...');
                                location.reload();
                            }
                            lastModified = data.timestamp;
                        })
                        .catch(function(e) { 
                            console.log('Live Server: Connection lost, retrying...'); 
                        });
                }
                
                setInterval(checkForUpdates, checkInterval);
                console.log('🟢 Live Server: Connected');
            })();
            </script>
        """.trimIndent()
    }

    // Track last modification time for live reload
    @Volatile private var lastRefreshTimestamp: Long = System.currentTimeMillis()

    override fun serve(session: IHTTPSession?): Response {
        val uri = session?.uri ?: "/"

        // Handle live reload check endpoint
        if (uri.startsWith("/__livereload/check")) {
            val clientTime = session?.parms?.get("t")?.toLongOrNull() ?: 0L
            val shouldReload = lastRefreshTimestamp > clientTime

            val json = """{"shouldReload": $shouldReload, "timestamp": $lastRefreshTimestamp}"""
            return newFixedLengthResponse(Response.Status.OK, "application/json", json)
        }

        // Serve favicon (prevent 404s)
        if (uri == "/favicon.ico") {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "")
        }

        // Determine file path - use entryFile if accessing root
        val requestedPath =
                when {
                    (uri == "/" || uri.isEmpty()) && entryFile != null -> "/$entryFile"
                    uri == "/" || uri.isEmpty() -> "/index.html"
                    else -> uri
                }
        val file = File(projectRoot, requestedPath.removePrefix("/"))

        Log.d(TAG, "Serving: $uri -> ${file.absolutePath}")

        // If directory, look for index.html or entry file
        val targetFile =
                if (file.isDirectory) {
                    File(file, entryFile ?: "index.html")
                } else {
                    file
                }

        return try {
            if (!targetFile.exists()) {
                Log.w(TAG, "File not found: ${targetFile.absolutePath}")
                return newFixedLengthResponse(
                        Response.Status.NOT_FOUND,
                        "text/html",
                        "<html><body><h1>404 - File Not Found</h1><p>${requestedPath}</p></body></html>"
                )
            }

            val mimeType = getMimeType(targetFile.name)
            val inputStream = FileInputStream(targetFile)

            // Inject live reload script into HTML files
            if (mimeType == "text/html") {
                val content = inputStream.bufferedReader().readText()
                inputStream.close()

                val injectedContent = injectLiveReloadScript(content)
                newFixedLengthResponse(Response.Status.OK, mimeType, injectedContent)
            } else {
                newFixedLengthResponse(
                        Response.Status.OK,
                        mimeType,
                        inputStream,
                        targetFile.length()
                )
            }
        } catch (e: Exception) {
            if (e is SocketException || e is SocketTimeoutException) {
                newFixedLengthResponse(Response.Status.NO_CONTENT, "text/plain", "")
            } else {
                Log.e(TAG, "Error serving file: ${e.message}", e)
                newFixedLengthResponse(
                        Response.Status.INTERNAL_ERROR,
                        "text/plain",
                        "Server error: ${e.message}"
                )
            }
        }
    }

    /** Trigger a reload on all connected clients */
    fun triggerReload() {
        lastRefreshTimestamp = System.currentTimeMillis()
        Log.d(TAG, "Triggered reload at $lastRefreshTimestamp")
    }

    /** Inject live reload script before </body> or at the end of the HTML */
    private fun injectLiveReloadScript(html: String): String {
        return if (html.contains("</body>", ignoreCase = true)) {
            html.replace("</body>", "$LIVE_RELOAD_SCRIPT\n</body>", ignoreCase = true)
        } else if (html.contains("</html>", ignoreCase = true)) {
            html.replace("</html>", "$LIVE_RELOAD_SCRIPT\n</html>", ignoreCase = true)
        } else {
            html + "\n" + LIVE_RELOAD_SCRIPT
        }
    }

    /** Get MIME type based on file extension */
    private fun getMimeType(fileName: String): String {
        return when {
            fileName.endsWith(".html", true) || fileName.endsWith(".htm", true) -> "text/html"
            fileName.endsWith(".css", true) -> "text/css"
            fileName.endsWith(".js", true) -> "application/javascript"
            fileName.endsWith(".json", true) -> "application/json"
            fileName.endsWith(".png", true) -> "image/png"
            fileName.endsWith(".jpg", true) || fileName.endsWith(".jpeg", true) -> "image/jpeg"
            fileName.endsWith(".gif", true) -> "image/gif"
            fileName.endsWith(".svg", true) -> "image/svg+xml"
            fileName.endsWith(".ico", true) -> "image/x-icon"
            fileName.endsWith(".woff", true) -> "font/woff"
            fileName.endsWith(".woff2", true) -> "font/woff2"
            fileName.endsWith(".ttf", true) -> "font/ttf"
            fileName.endsWith(".eot", true) -> "application/vnd.ms-fontobject"
            fileName.endsWith(".mp4", true) -> "video/mp4"
            fileName.endsWith(".webm", true) -> "video/webm"
            fileName.endsWith(".mp3", true) -> "audio/mpeg"
            fileName.endsWith(".wav", true) -> "audio/wav"
            fileName.endsWith(".xml", true) -> "application/xml"
            fileName.endsWith(".txt", true) -> "text/plain"
            fileName.endsWith(".md", true) -> "text/markdown"
            else -> "application/octet-stream"
        }
    }
}
