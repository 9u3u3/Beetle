package com.bsbeetle.bananaspaghetti.utils.ssh

import android.util.Log
import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.Session
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

/**
 * Mirrors LspServer.kt exactly, but bridges a remote LSP process (started via SSH exec channel)
 * instead of a local proot process. The Ktor WebSocket server is identical — the editor JS
 * connects to ws://localhost:PORT/clsp exactly as it does for local LSP.
 */
class RemoteLspBridge(val port: Int = 8083) {

    companion object {
        private const val TAG = "RemoteLspBridge"
        private const val INITIAL_BUFFER_SIZE = 4 * 1024 * 1024  // 4MB
        private const val MAX_BUFFER_SIZE = 16 * 1024 * 1024     // 16MB

        fun getLspCommand(slug: String): String = when (slug) {
            "python"     -> "pylsp"
            "typescript" -> "typescript-language-server --stdio"
            "javascript" -> "typescript-language-server --stdio"
            "rust"       -> "rust-analyzer"
            "go"         -> "gopls"
            "c"          -> "clangd"
            "cpp"        -> "clangd"
            "java"       -> "jdtls"
            "kotlin"     -> "kotlin-language-server"
            "ruby"       -> "solargraph stdio"
            "php"        -> "intelephense --stdio"
            "html"       -> "vscode-html-language-server --stdio"
            "css"        -> "vscode-css-language-server --stdio"
            "json"       -> "vscode-json-language-server --stdio"
            "yaml"       -> "yaml-language-server --stdio"
            "lua"        -> "lua-language-server"
            "bash"       -> "bash-language-server start"
            else         -> slug
        }
    }

    val serverReady = CompletableDeferred<Unit>()

    @Volatile
    private var currentSession: DefaultWebSocketServerSession? = null
    private var readerScope: CoroutineScope? = null
    private var server: io.ktor.server.engine.EmbeddedServer<*, *>? = null
    private var execChannel: ChannelExec? = null

    fun start(slug: String, sshSession: Session) {
        stop()
        Log.d(TAG, "Starting remote LSP for $slug on port $port")

        val command = getLspCommand(slug)
        Log.d(TAG, "Remote LSP command: $command")

        val channel = sshSession.openChannel("exec") as ChannelExec
        channel.setCommand(command)
        channel.connect()
        execChannel = channel

        val lspIn = channel.inputStream
        val lspOut = channel.outputStream

        readerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        readerScope!!.launch {
            var buffer = ByteArray(INITIAL_BUFFER_SIZE)
            var bufferSize = 0
            val readBuf = ByteArray(65536)
            val contentLengthRegex = Regex("Content-Length:\\s*(\\d+)", RegexOption.IGNORE_CASE)

            try {
                while (true) {
                    val n = lspIn.read(readBuf)
                    if (n <= 0) break

                    if (bufferSize + n > buffer.size) {
                        if (buffer.size < MAX_BUFFER_SIZE) {
                            val newSize = minOf(buffer.size * 2, MAX_BUFFER_SIZE)
                            val newBuffer = ByteArray(newSize)
                            System.arraycopy(buffer, 0, newBuffer, 0, bufferSize)
                            buffer = newBuffer
                            Log.d(TAG, "Buffer grew to ${newSize / 1024}KB")
                        } else {
                            Log.e(TAG, "Buffer exceeded ${MAX_BUFFER_SIZE / 1024 / 1024}MB, dropping data")
                            bufferSize = 0
                            continue
                        }
                    }

                    System.arraycopy(readBuf, 0, buffer, bufferSize, n)
                    bufferSize += n

                    while (true) {
                        var headersEnd = -1
                        for (i in 0 until bufferSize - 3) {
                            if (buffer[i] == 13.toByte() && buffer[i + 1] == 10.toByte() &&
                                buffer[i + 2] == 13.toByte() && buffer[i + 3] == 10.toByte()
                            ) {
                                headersEnd = i
                                break
                            }
                        }
                        if (headersEnd == -1) break

                        val headersString = String(buffer, 0, headersEnd, Charsets.US_ASCII)
                        val match = contentLengthRegex.find(headersString)

                        if (match == null) {
                            val consumed = headersEnd + 4
                            System.arraycopy(buffer, consumed, buffer, 0, bufferSize - consumed)
                            bufferSize -= consumed
                            continue
                        }

                        val contentLen = match.groupValues[1].toInt()
                        val messageStart = headersEnd + 4

                        if (bufferSize < messageStart + contentLen) break

                        val messageBytes = ByteArray(contentLen)
                        System.arraycopy(buffer, messageStart, messageBytes, 0, contentLen)
                        val jsonBody = String(messageBytes, Charsets.UTF_8)

                        val session = currentSession
                        if (session != null) {
                            try {
                                session.outgoing.send(Frame.Text(jsonBody))
                            } catch (ex: Exception) {
                                Log.e(TAG, "Failed to send to WebSocket client", ex)
                            }
                        }

                        val consumed = messageStart + contentLen
                        System.arraycopy(buffer, consumed, buffer, 0, bufferSize - consumed)
                        bufferSize -= consumed
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Reader loop error", e)
            }
            Log.w(TAG, "Remote LSP reader loop exited for port $port")
        }

        try {
            server = embeddedServer(Netty, port = port, host = "0.0.0.0") {
                install(WebSockets.Plugin) {
                    pingPeriod = 15.seconds
                    timeout = 30.seconds
                    maxFrameSize = Long.MAX_VALUE
                    masking = false
                }
                routing {
                    webSocket("/clsp") {
                        Log.d(TAG, "Client connected to /clsp on port $port")

                        val oldSession = currentSession
                        currentSession = this
                        if (oldSession != null && oldSession != this) {
                            try {
                                oldSession.close(
                                    CloseReason(CloseReason.Codes.GOING_AWAY, "New client connected")
                                )
                            } catch (_: Exception) {}
                        }

                        for (frame in incoming) {
                            if (frame is Frame.Text) {
                                val text = frame.readText()
                                try {
                                    val finalBytes = text.toByteArray(Charsets.UTF_8)
                                    val header = "Content-Length: ${finalBytes.size}\r\n\r\n"
                                    synchronized(lspOut) {
                                        lspOut.write(header.toByteArray(Charsets.UTF_8))
                                        lspOut.write(finalBytes)
                                        lspOut.flush()
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Failed to write to remote LSP process", e)
                                }
                            }
                        }

                        if (currentSession == this) currentSession = null
                        Log.d(TAG, "Client disconnected from /clsp on port $port")
                    }
                }
            }.start(wait = false)

            serverReady.complete(Unit)
            Log.d(TAG, "WebSocket server ready on port $port")
        } catch (e: java.net.BindException) {
            Log.w(TAG, "Port $port already in use, server may already be running")
            serverReady.complete(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start WebSocket server on port $port", e)
            serverReady.complete(Unit)
        }
    }

    fun stop() {
        try {
            readerScope?.cancel()
            readerScope = null
            currentSession = null
            server?.stop(100, 100)
            server = null
            execChannel?.disconnect()
            execChannel = null
            Log.d(TAG, "Remote LSP bridge stopped on port $port")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping bridge", e)
        }
    }
}
