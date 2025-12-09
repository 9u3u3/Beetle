package com.bsbeetle.bananaspaghetti.utils.languageHandling

import android.content.Context
import android.os.ParcelFileDescriptor
import android.util.Log
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import kotlin.time.Duration.Companion.seconds

class LspServer(private val context: Context, private val port: Int = 8081) {

    companion object {
        init {
            System.loadLibrary("lsp-lib")
        }
    }

    external fun startNativeLsp(
        prefix: String,
        jNativeDirPath: String,
        jProotTmpDir: String,
        jProotLoader: String,
        jProotLoader32: String,
        jProot: String,
        jPublic: String,
        jAlpineTmp: String,
        jAlpine: String,
        jScriptName: String
    ): Int

    private var gMasterFd: Int = 0
    private var pfd: ParcelFileDescriptor? = null
    private var input: FileInputStream? = null
    private var output: FileOutputStream? = null
    private var server: io.ktor.server.engine.EmbeddedServer<*, *>? = null
    
    // Single persistent reader scope
    private var readerScope: CoroutineScope? = null
    
    // Current active WebSocket session (volatile for thread safety)
    @Volatile
    private var currentSession: DefaultWebSocketServerSession? = null

    private val nativeDir = context.applicationInfo.nativeLibraryDir
    private val PREFIX = context.filesDir
    private val tmp = File(PREFIX, "tmp")
    private val alpine = File(PREFIX, "alpine")
    private val alpineTmp = File(alpine, "tmp")
    private val public = File(PREFIX, "public")
    private val prootLoader = File(nativeDir, "libproot.so")
    private val prootLoader32 = File(nativeDir, "libproot32.so")
    private val proot = File(nativeDir, "libproot-xed.so")

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    fun stop() {
        try {
            readerScope?.cancel()
            readerScope = null
            currentSession = null
            server?.stop(100, 100)
            server = null
            input?.close()
            output?.close()
            pfd?.close()
            Log.d("LspServer", "LSP server stopped")
        } catch (e: Exception) {
            Log.e("LspServer", "Error stopping LSP server", e)
        }
    }

    fun start(slug: String) {
        stop() // Stop previous instance if any
        Log.d("LspServer", "Starting LSP for $slug on port $port")

        val libtallocLink = File(PREFIX, "libtalloc.so.2")
        try {
            Files.deleteIfExists(libtallocLink.toPath())
            Files.createSymbolicLink(
                libtallocLink.toPath(),
                File(nativeDir, "libtalloc.so").toPath()
            )
        } catch (e: Exception) {
            Log.e("LspServer", "Failed to create symlink", e)
        }

        // Cleanup old init-lsp.sh if it exists
        val initScript = File(PREFIX, "init-lsp.sh")
        if (initScript.exists()) {
            initScript.delete()
        }

        // Determine script path
        val scriptRelativePath = "plugins/$slug/$slug.sh"
        val targetScript = File(PREFIX, scriptRelativePath)
        
        if (!targetScript.exists()) {
            Log.e("LspServer", "Script for $slug not found at ${targetScript.absolutePath}")
            return
        }
        targetScript.setExecutable(true)

        gMasterFd = startNativeLsp(
            PREFIX.absolutePath,
            nativeDir,
            tmp.absolutePath,
            prootLoader.absolutePath,
            prootLoader32.absolutePath,
            proot.absolutePath,
            "${public.absolutePath}:/public",
            "${alpineTmp.absolutePath}:/dev/shm",
            alpine.absolutePath,
            scriptRelativePath
        )

        if (gMasterFd <= 0) {
            Log.e("cLsp", "Failed to get master FD")
            return
        }
        Log.d("cLsp", "LSP master FD: $gMasterFd")

        pfd = ParcelFileDescriptor.adoptFd(gMasterFd)
        output = FileOutputStream(pfd!!.fileDescriptor)
        input = FileInputStream(pfd!!.fileDescriptor)

        // Start persistent reader ONCE when LSP process starts
        readerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        readerScope!!.launch {
            val buf = ByteArray(65536)
            val sb = StringBuilder()

            try {
                while (true) {
                    val n = input!!.read(buf)
                    if (n <= 0) break
                    val chunk = String(buf, 0, n, Charsets.UTF_8)
                    sb.append(chunk)

                    while (true) {
                        val headersEnd = sb.indexOf("\r\n\r\n")
                        if (headersEnd == -1) break

                        val headers = sb.substring(0, headersEnd)
                        val match = Regex("Content-Length:\\s*(\\d+)", RegexOption.IGNORE_CASE).find(headers) ?: break
                        val contentLen = match.groupValues[1].toInt()
                        val messageStart = headersEnd + 4
                        if (sb.length < messageStart + contentLen) break

                        val jsonBody = sb.substring(messageStart, messageStart + contentLen)
                        sb.delete(0, messageStart + contentLen)

                        // Send to current session (if any)
                        val session = currentSession
                        if (session != null) {
                            try {
                                session.outgoing.send(Frame.Text(jsonBody))
                            } catch (ex: Exception) {
                                Log.e("cLsp", "Failed to send to client: ${ex.message}")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("cLsp", "Reader loop error", e)
            }
        }

        server = embeddedServer(Netty, port = port, host = "0.0.0.0") {
            install(WebSockets.Plugin) {
                pingPeriod = 15.seconds
                timeout = 30.seconds
                maxFrameSize = Long.MAX_VALUE
                masking = false
            }

            routing {
                webSocket("/clsp") {
                    Log.d("cLsp", "Client connected to /clsp")
                    
                    // Register this session as the current one
                    currentSession = this

                    // Handle incoming messages from client
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            val text = frame.readText()
                            try {
                                val finalBytes = text.toByteArray(Charsets.UTF_8)
                                val header = "Content-Length: ${finalBytes.size}\r\n\r\n"
                                synchronized(output!!) {
                                    output!!.write(header.toByteArray(Charsets.UTF_8))
                                    output!!.write(finalBytes)
                                    output!!.flush()
                                }
                            } catch (e: Exception) {
                                Log.e("cLsp", "Failed to write to LSP", e)
                            }
                        }
                    }
                    
                    // Client disconnected
                    if (currentSession == this) {
                        currentSession = null
                    }
                    Log.d("cLsp", "Client disconnected from /clsp")
                }
            }
        }.start(wait = false)
    }
}