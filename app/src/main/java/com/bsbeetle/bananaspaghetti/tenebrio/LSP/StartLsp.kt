package com.bsbeetle.bananaspaghetti.tenebrio.LSP

import android.content.Context
import android.os.ParcelFileDescriptor
import android.util.Log
import com.bsbeetle.bananaspaghetti.utils.copyAssetsTo
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

// TODO
// 1. Fork a new lsp server using native code
// 2. get the i/o file descriptors
// 3. Create a websocket
// 4. Connect the websocket to the code editors, lsp client

class StartLsp(private val context: Context) {

    companion object {
        init {
            System.loadLibrary("lsp-lib")
        }
    }

    private var gMasterfd: Int? = null

    // Make sure you do this

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
        jAlpineInit: String
    ): Int


    private val nativeDir: String = context.applicationInfo.nativeLibraryDir
    private val PREFIX: File = context.filesDir
    private val tmp: File = File(PREFIX, "tmp")
    private val alpine: File = File(PREFIX, "alpine")
    private val alpineTmp: File = File(alpine, "tmp")
    private val public: File = File(PREFIX, "public")
    private val prootLoader: File = File(nativeDir, "libproot.so")
    private val prootLoader32: File = File(nativeDir, "libproot32.so")
    private val proot: File = File(nativeDir, "libproot-xed.so")

    fun init(): Unit {

        Log.d("NativeLSP", "Called init")

        copyAssetsTo(PREFIX, "scripts/init-alpine.sh", context)

        val masterFd = getCppLsp(
            PREFIX.absolutePath.toString(),
            nativeDir,
            tmp.absolutePath.toString(),
            prootLoader.absolutePath.toString(),
            prootLoader32.absolutePath.toString(),
            proot.absolutePath.toString(),
            "${public.absolutePath}:/public",
            "${alpineTmp.absolutePath}:/dev/shm",
            alpine.absolutePath.toString(),
            "${PREFIX.absolutePath}/init-alpine.sh",
        )
        Log.d("NativeLSP", "$masterFd")

        if (masterFd < 0) {
            Log.e("NativeLSP", "Failed to get the master fd: $masterFd")
            return
        } else {
            Log.d("NativeLSP", "Got the masterFd")
            gMasterfd = masterFd
        }

        val pfd = ParcelFileDescriptor.adoptFd(masterFd)
        val output = FileOutputStream(pfd!!.fileDescriptor)
        val input = FileInputStream(pfd!!.fileDescriptor)

        Thread {
            try {
                // Construct a minimal LSP "initialize" message
                val initJson = """{
            "jsonrpc": "2.0",
            "id": 1,
            "method": "initialize",
            "params": {}
        }""".trimIndent()

                // According to the LSP spec, messages are framed with "Content-Length"
                val msg = "Content-Length: ${initJson.toByteArray().size}\r\n\r\n$initJson"

                // Send it
                output.write(msg.toByteArray())
                output.flush()
                Log.d("NativeLSP", "Sent LSP initialize request")

                // Try reading a response
                val buffer = ByteArray(4096)
                val readBytes = input.read(buffer)
                if (readBytes > 0) {
                    val response = String(buffer, 0, readBytes)
                    Log.d("NativeLSP", "LSP response: $response")
                } else {
                    Log.e("NativeLSP", "No response from LSP")
                }

            } catch (e: Exception) {
                Log.e("NativeLSP", "Error communicating with LSP: ${e.message}", e)
            }
        }.start()

    }

}