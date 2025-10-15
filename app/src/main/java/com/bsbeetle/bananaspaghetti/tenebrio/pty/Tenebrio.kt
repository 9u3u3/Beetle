package com.bsbeetle.bananaspaghetti.tenebrio.pty

import android.content.Context
import android.os.ParcelFileDescriptor
import android.util.Log
import com.bsbeetle.bananaspaghetti.tenebrio.data.outputfd
import com.bsbeetle.bananaspaghetti.tenebrio.data.termSize
import com.bsbeetle.bananaspaghetti.utils.TerminalWebView
import com.bsbeetle.bananaspaghetti.utils.copyAssetsTo
import com.bsbeetle.bananaspaghetti.utils.downloadFile
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.concurrent.thread

class Tenebrio(private val context: Context) {

    companion object {
        init {
            System.loadLibrary("pty-lib")
        }
    }

    external fun startAlpine(
        prefix: String,
        jNativeDirPath: String,
        jProotTmpDir: String,
        jProotLoader: String,
        jProotLoader32: String,
        jProot: String,
        jPublic: String,
        jAlpineTmp: String,
        jAlpine: String,
        jAlpineInit: String,
        jRows: Int,
        jCols: Int
    ) : Int

    external fun resizePty(masterFd: Int, rows: Int, cols: Int): Int

    var gMasterFd: Int = -1
    val alpineUrl = "https://dl-cdn.alpinelinux.org/alpine/v3.21/releases/aarch64/alpine-minirootfs-3.21.0-aarch64.tar.gz"
    private val nativeDir: String = context.applicationInfo.nativeLibraryDir
    private var pfd: ParcelFileDescriptor? = null
    private var input: FileInputStream? = null
    private var output: FileOutputStream? = null
    private val PREFIX: File = context.filesDir
    private val tmp: File = File(PREFIX, "tmp")
    private val alpine: File = File(PREFIX, "alpine")
    private val alpineTmp: File = File(alpine, "tmp")
    private val public: File = File(PREFIX, "public")
    private val dotDownloaded: File = File(PREFIX, ".downloaded")
    private val prootLoader: File = File(nativeDir, "libproot.so")
    private val prootLoader32: File = File(nativeDir, "libproot32.so")
    private val libtalloc: File = File(PREFIX, "libtalloc.so.2")
    private val proot: File = File(nativeDir, "libproot-xed.so")

    fun resizeTerminal(rows: Int, cols: Int) {
        if (gMasterFd > 0) {
            resizePty(gMasterFd, rows, cols)
        }
    }

    fun init() {
        Log.d("Tenebrio", "Initializing")
        if(!dotDownloaded.exists()) {
            downloadFile(
                context,
                alpineUrl,
                "alpine.tar.gz"
            ) { file ->

                if(file == null) {
                    Log.e("Tenebrio: ", "Failed to download alpine root file system")
                }
                copyAssetsTo(PREFIX, "scripts/init-alpine.sh", context)
                val dotDownloaded = File(PREFIX, ".downloaded")
                dotDownloaded.mkdirs()
                Log.d("Tenebrio", "Download complete")
                // Creating necessary folders
                // Always to be run before executing getAlpineFork()
                if (!tmp.exists()) tmp.mkdirs()
                if (!alpine.exists()) alpine.mkdirs()
                if (!alpineTmp.exists()) alpineTmp.mkdirs()
                if (!public.exists()) public.mkdirs()
                val process = Runtime.getRuntime().exec("tar --no-same-owner -xf ${PREFIX}/alpine.tar.gz -C ${alpine.absolutePath}")
                process.waitFor()
                Log.d("Tenebrio", "Extracted the rootfs")
                val resolvconf: File = File(alpine, "/etc/resolv.conf")
                resolvconf.createNewFile()
                resolvconf.writeText("nameserver 8.8.4.4 \nnameserver 8.8.8.8")
                if(!prootLoader.exists()) Log.e("INIT_SHELL", "PROOT_LOADER doesn't exists")
                if(!prootLoader32.exists()) Log.e("INIT_SHELL", "PROOT_LOADER32 doesn't exists")
                if(libtalloc.exists()) libtalloc.delete()
                val libtallocLink = File(PREFIX, "libtalloc.so.2")
                if (libtallocLink.exists()) libtallocLink.delete()
                java.nio.file.Files.createSymbolicLink(
                    libtallocLink.toPath(),
                    File(nativeDir, "libtalloc.so").toPath()
                )
                Log.d("Tenebrio", "Linked the libtalloc.so")
            }
        } else {
            // FIXED: Force delete the symlink even if it appears broken
            val libtallocLink = File(PREFIX, "libtalloc.so.2")
            try {
                // This will delete the symlink even if it's broken
                java.nio.file.Files.deleteIfExists(libtallocLink.toPath())
            } catch (e: Exception) {
                Log.e("Tenebrio", "Failed to delete existing symlink", e)
            }
            // Now create the fresh symlink
            try {
                java.nio.file.Files.createSymbolicLink(
                    libtallocLink.toPath(),
                    File(nativeDir, "libtalloc.so").toPath()
                )
                Log.d("Tenebrio", "Recreated libtalloc.so.2 symbolic link")
            } catch (e: Exception) {
                Log.e("Tenebrio", "Failed to create symlink", e)
                return
            }
            if(termSize.row == null || termSize.cols == null) {
                Log.d("Tenebrio", "Null sizes")
            }
            val masterFd = startAlpine(
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
                termSize.row!! , termSize.cols!!
            )
            if (masterFd < 0) {
                Log.e("Tenebrio", "Failed to get the master fd: $masterFd")
                return
            } else {
                Log.d("Tenebrio", "Got the masterFd")
                gMasterFd = masterFd
            }
            pfd = ParcelFileDescriptor.adoptFd(masterFd)
            output = FileOutputStream(pfd!!.fileDescriptor)
            input = FileInputStream(pfd!!.fileDescriptor)
            outputfd.outputFd = output
            val terminalWebView = TerminalWebView.getWebview(context, this)
            thread {
                try {
                    val buffer = ByteArray(1024)
                    while (true) {
                        val bytesRead = input!!.read(buffer)
                        if (bytesRead > 0) {
                            val text = String(buffer, 0, bytesRead)
                            Log.d("Shell", "Output: $text")
                            terminalWebView.post {
                                val jsCommand = "term.write(${JSONObject.quote(text)});"
                                terminalWebView.evaluateJavascript(jsCommand, null)
                            }
                        } else if (bytesRead < 0) {
                            break // EOF or error
                        }
                    }
                } catch (e: Exception) {
                    Log.e("TenebrioShell", "Error reading", e)
                }
            }
        }
    }
}