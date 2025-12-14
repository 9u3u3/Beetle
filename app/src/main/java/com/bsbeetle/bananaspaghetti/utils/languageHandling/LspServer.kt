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

        // For Rust (rust-analyzer), auto-generate Cargo.toml if not exists
        // This enables rust-analyzer to work with standalone .rs files
        if (slug == "rust") {
            try {
                val cargoToml = File(alpine, "home/Cargo.toml")
                if (!cargoToml.exists()) {
                    cargoToml.parentFile?.mkdirs()
                    cargoToml.writeText("""
[package]
name = "workspace"
version = "0.1.0"
edition = "2021"

[[bin]]
name = "main"
path = "main.rs"

[dependencies]
""".trimIndent())
                    Log.d("LspServer", "Created Cargo.toml at ${cargoToml.absolutePath}")
                }
            } catch (e: Exception) {
                Log.w("LspServer", "Failed to create Cargo.toml", e)
            }
        }

        // For Java (jdtls), auto-generate Eclipse project files if not exists
        // This enables jdtls to work with standalone .java files
        if (slug == "java") {
            try {
                val homeDir = File(alpine, "home")
                homeDir.mkdirs()
                
                // Create .project file (Eclipse project descriptor)
                val projectFile = File(homeDir, ".project")
                if (!projectFile.exists()) {
                    projectFile.writeText("""
<?xml version="1.0" encoding="UTF-8"?>
<projectDescription>
    <name>workspace</name>
    <comment></comment>
    <projects></projects>
    <buildSpec>
        <buildCommand>
            <name>org.eclipse.jdt.core.javabuilder</name>
            <arguments></arguments>
        </buildCommand>
    </buildSpec>
    <natures>
        <nature>org.eclipse.jdt.core.javanature</nature>
    </natures>
</projectDescription>
""".trimIndent())
                    Log.d("LspServer", "Created .project at ${projectFile.absolutePath}")
                }
                
                // Create .classpath file
                val classpathFile = File(homeDir, ".classpath")
                if (!classpathFile.exists()) {
                    classpathFile.writeText("""
<?xml version="1.0" encoding="UTF-8"?>
<classpath>
    <classpathentry kind="src" path="."/>
    <classpathentry kind="con" path="org.eclipse.jdt.launching.JRE_CONTAINER"/>
    <classpathentry kind="output" path="bin"/>
</classpath>
""".trimIndent())
                    Log.d("LspServer", "Created .classpath at ${classpathFile.absolutePath}")
                }
            } catch (e: Exception) {
                Log.w("LspServer", "Failed to create Java project files", e)
            }
        }

        // For Go (gopls), auto-generate go.mod if not exists
        // This enables gopls to work with standalone .go files
        if (slug == "go") {
            try {
                val goMod = File(alpine, "home/go.mod")
                if (!goMod.exists()) {
                    goMod.parentFile?.mkdirs()
                    goMod.writeText("""
module workspace

go 1.21
""".trimIndent())
                    Log.d("LspServer", "Created go.mod at ${goMod.absolutePath}")
                }
            } catch (e: Exception) {
                Log.w("LspServer", "Failed to create go.mod", e)
            }
        }

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
            val buffer = ByteArray(4 * 1024 * 1024) // 4MB buffer
            var bufferSize = 0
            val readBuf = ByteArray(65536)

            try {
                while (true) {
                    val n = input!!.read(readBuf)
                    if (n <= 0) break
                    
                    // Simple resize if needed (doubling)
                    if (bufferSize + n > buffer.size) {
                         Log.e("cLsp", "Buffer overflow, clearing buffer. Message too large?")
                         bufferSize = 0 // Resetting is drastic but prevents crash. ideally we'd resize.
                         // For now, let's assume 4MB is enough. If not, we drop data.
                         continue
                    }

                    System.arraycopy(readBuf, 0, buffer, bufferSize, n)
                    bufferSize += n

                    while (true) {
                        // Scan for \r\n\r\n
                        var headersEnd = -1
                        for (i in 0 until bufferSize - 3) {
                            if (buffer[i] == 13.toByte() && buffer[i+1] == 10.toByte() && 
                                buffer[i+2] == 13.toByte() && buffer[i+3] == 10.toByte()) {
                                headersEnd = i
                                break
                            }
                        }

                        if (headersEnd == -1) break

                        // Parse headers
                        val headersString = String(buffer, 0, headersEnd, Charsets.US_ASCII)
                        val match = Regex("Content-Length:\\s*(\\d+)", RegexOption.IGNORE_CASE).find(headersString)
                        
                        if (match == null) {
                            // No Content-Length found in what looked like headers. 
                            // Treat as garbage and skip past this \r\n\r\n
                            val consumed = headersEnd + 4
                            System.arraycopy(buffer, consumed, buffer, 0, bufferSize - consumed)
                            bufferSize -= consumed
                            continue
                        }

                        val contentLen = match.groupValues[1].toInt()
                        val messageStart = headersEnd + 4

                        if (bufferSize < messageStart + contentLen) break // Wait for more data

                        // Extract message
                        val messageBytes = ByteArray(contentLen)
                        System.arraycopy(buffer, messageStart, messageBytes, 0, contentLen)
                        val jsonBody = String(messageBytes, Charsets.UTF_8)

                        // Send to current session
                        val session = currentSession
                        if (session != null) {
                            try {
                                // Log first 200 chars of message for debugging
                                val preview = if (jsonBody.length > 200) jsonBody.take(200) + "..." else jsonBody
                                Log.d("cLsp", "LSP response (len=${jsonBody.length}): $preview")
                                session.outgoing.send(Frame.Text(jsonBody))
                            } catch (ex: Exception) {
                                Log.e("cLsp", "Failed to send to client: ${ex.message}")
                            }
                        }

                        // Shift buffer
                        val consumed = messageStart + contentLen
                        System.arraycopy(buffer, consumed, buffer, 0, bufferSize - consumed)
                        bufferSize -= consumed
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