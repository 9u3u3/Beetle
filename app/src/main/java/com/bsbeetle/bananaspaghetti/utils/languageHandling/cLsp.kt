package com.bsbeetle.bananaspaghetti.utils.languageHandling

import android.content.Context
import android.os.ParcelFileDescriptor
import android.util.Log
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
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
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.JsonArray
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import kotlin.time.Duration.Companion.seconds

class cLsp(private val context: Context) {

    companion object {
        init {
            System.loadLibrary("lsp-lib")
        }
    }

    external fun getCppLsp(
        prefix: String,
        jNativeDirPath: String,
        jProotTmpDir: String,
        jProotLoader: String,
        jProotLoader32: String,
        jProot: String,
        jPublic: String,
        jAlpineTmp: String,
        jAlpine: String,
    ): Int

    private var gMasterFd: Int = 0
    private var pfd: ParcelFileDescriptor? = null
    private var input: FileInputStream? = null
    private var output: FileOutputStream? = null

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

    fun init() {
        Log.d("cLsp", "Initializing LSP server")

        val libtallocLink = File(PREFIX, "libtalloc.so.2")
        try {
            Files.deleteIfExists(libtallocLink.toPath())
            Files.createSymbolicLink(
                libtallocLink.toPath(),
                File(nativeDir, "libtalloc.so").toPath()
            )
            Log.d("cLsp", "Recreated libtalloc.so.2 symbolic link")
        } catch (e: Exception) {
            Log.e("cLsp", "Failed to create symlink", e)
        }

        // Copy init-lsp.sh to filesDir
        try {
            com.bsbeetle.bananaspaghetti.utils.assetHandling.copyAssetsTo(
                PREFIX,
                "scripts/init-lsp.sh",
                context
            )
            Log.d("cLsp", "Copied init-lsp.sh to filesDir")
        } catch (e: Exception) {
            Log.e("cLsp", "Failed to copy init-lsp.sh", e)
        }

        gMasterFd = getCppLsp(
            PREFIX.absolutePath,
            nativeDir,
            tmp.absolutePath,
            prootLoader.absolutePath,
            prootLoader32.absolutePath,
            proot.absolutePath,
            "${public.absolutePath}:/public",
            "${alpineTmp.absolutePath}:/dev/shm",
            alpine.absolutePath,
        )

        // Check if clangd exists in the alpine environment
        val clangdPath = File(alpine, "usr/bin/clangd")
        if (!clangdPath.exists()) {
            Log.e("cLsp", "clangd not found at ${clangdPath.absolutePath}")
            return
        }
        clangdPath.setExecutable(true)

        // Ensure init-lsp.sh is executable
        File(PREFIX, "init-lsp.sh").setExecutable(true)

        if (gMasterFd <= 0) {
            Log.e("cLsp", "Failed to get master FD")
            return
        }
        Log.d("cLsp", "clangd master FD: $gMasterFd")

        pfd = ParcelFileDescriptor.adoptFd(gMasterFd)
        output = FileOutputStream(pfd!!.fileDescriptor)
        input = FileInputStream(pfd!!.fileDescriptor)

        embeddedServer(Netty, port = 8081, host = "0.0.0.0") {
            install(WebSockets.Plugin) {
                pingPeriod = 15.seconds
                timeout = 30.seconds
                maxFrameSize = Long.MAX_VALUE
                masking = false
            }

            routing {
                webSocket("/clsp") {
                    Log.d("cLsp", "Client connected to /clsp")

                    val readScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
                    val sendScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

                    readScope.launch {
                        // Increase buffer size to 64KB as requested
                        val buf = ByteArray(65536)
                        val sb = StringBuilder()

                        try {
                            while (true) {
                                val n = input!!.read(buf)
                                if (n <= 0) {
                                    Log.d("cLsp", "End of stream from clangd (n=$n)")
                                    break
                                }
                                val chunk = String(buf, 0, n, Charsets.UTF_8)
                                // Log.v("cLsp", "Raw chunk from clangd: $chunk") // Verbose log
                                sb.append(chunk)

                                while (true) {
                                    val headersEnd = sb.indexOf("\r\n\r\n")
                                    if (headersEnd == -1) break

                                    val headers = sb.substring(0, headersEnd)
                                    val match =
                                        Regex("Content-Length:\\s*(\\d+)", RegexOption.IGNORE_CASE)
                                            .find(headers) ?: break

                                    val contentLen = match.groupValues[1].toInt()
                                    val messageStart = headersEnd + 4
                                    if (sb.length < messageStart + contentLen) break

                                    val jsonBody =
                                        sb.substring(messageStart, messageStart + contentLen)
                                    sb.delete(0, messageStart + contentLen)

                                    val messageType = when {
                                        jsonBody.contains("\"method\":\"textDocument/publishDiagnostics\"") -> "diagnostics"
                                        jsonBody.contains("\"result\"") && jsonBody.contains("\"items\"") -> "completion_response" // Identify completion response
                                        jsonBody.contains("\"result\"") -> "response"
                                        else -> "other"
                                    }
                                    // Log.d("cLsp", "clangd → [$messageType] ${jsonBody.take(150)}")

                                    sendScope.launch {
                                        try {
                                            var payloadToSend = jsonBody
                                            
                                            // SMART FILTER: Truncate completion list if it's too large
                                            if (messageType == "completion_response") {
                                                try {
                                                    val jsonElement = json.parseToJsonElement(jsonBody)
                                                    if (jsonElement is JsonObject) {
                                                        val result = jsonElement["result"]
                                                        if (result is JsonObject && result.containsKey("items")) {
                                                            // Handle { result: { items: [...] } }
                                                            val items = result["items"]?.jsonArray
                                                            if (items != null && items.size > 10) {
                                                                val topItems = JsonArray(items.take(10))
                                                                val newResult = JsonObject(result + ("items" to topItems))
                                                                val newJson = JsonObject(jsonElement + ("result" to newResult))
                                                                payloadToSend = json.encodeToString(newJson)
                                                                Log.i("cLsp", "Optimized completion: ${items.size} -> 10 items")
                                                            }
                                                        } else if (result is JsonArray) {
                                                            // Handle { result: [...] }
                                                            if (result.size > 10) {
                                                                val topItems = JsonArray(result.take(10))
                                                                val newJson = JsonObject(jsonElement + ("result" to topItems))
                                                                payloadToSend = json.encodeToString(newJson)
                                                                Log.i("cLsp", "Optimized completion: ${result.size} -> 10 items")
                                                            }
                                                        }
                                                    }
                                                } catch (e: Exception) {
                                                    Log.e("cLsp", "Failed to optimize completion", e)
                                                }
                                            }

                                            outgoing.send(Frame.Text(payloadToSend))
                                        } catch (ex: Exception) {
                                            Log.e("cLsp", "Failed to send to client: ${ex.message}")
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("cLsp", "Reader loop error", e)
                        } finally {
                            Log.d("cLsp", "Reader stopped")
                            input?.close()
                        }
                    }

                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            val text = frame.readText()

                                val requestType = when {
                                    text.contains("\"method\":\"textDocument/completion\"") -> "completion"
                                    text.contains("\"method\":\"textDocument/didChange\"") -> "didChange"
                                    text.contains("\"method\":\"initialize\"") -> "initialize"
                                    else -> "other"
                                }
                                // Log.d("cLsp", "Client → [$requestType] ${text.take(200)}") // Increased log length

                            try {
                                val jsonElement = json.parseToJsonElement(text)
                                val modifiedJson = injectFakeUri(jsonElement)

                                val finalBytes =
                                    json.encodeToString(modifiedJson).toByteArray(Charsets.UTF_8)
                                val header = "Content-Length: ${finalBytes.size}\r\n\r\n"
                                
                                // Log.d("cLsp", "Writing to clangd: ${header.trim()} + JSON body")

                                synchronized(output!!) {
                                    output!!.write(header.toByteArray(Charsets.UTF_8))
                                    output!!.write(finalBytes)
                                    output!!.flush()
                                }
                            } catch (e: Exception) {
                                Log.e("cLsp", "Failed to write to clangd", e)
                            }
                        }
                    }

                    sendScope.cancel()
                    readScope.cancel()
                }
            }
        }.start(wait = true)
    }

    private fun injectFakeUri(jsonElement: JsonElement): JsonElement {
        try {
            if (jsonElement is JsonObject) {
                val method = jsonElement["method"]?.jsonPrimitive?.contentOrNull
                if (method != null && method.startsWith("textDocument")) {
                    val params = jsonElement["params"]?.jsonObject
                    if (params != null) {
                        val td = params["textDocument"]?.jsonObject
                        if (td != null && !td.containsKey("uri")) {
                            val modifiedTextDoc = td + ("uri" to JsonPrimitive("file:///main.cpp"))
                            val modifiedParams = params + ("textDocument" to JsonObject(
                                modifiedTextDoc
                            ))
                            return JsonObject(jsonElement + ("params" to JsonObject(modifiedParams)))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("cLsp", "injectFakeUri error", e)
        }
        return jsonElement
    }
}