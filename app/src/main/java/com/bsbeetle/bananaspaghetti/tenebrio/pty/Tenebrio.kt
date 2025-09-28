package com.bsbeetle.bananaspaghetti.tenebrio.pty

import android.content.Context
import android.os.ParcelFileDescriptor
import android.util.Log
import com.bsbeetle.bananaspaghetti.tenebrio.data.ShellExecutor
import com.bsbeetle.bananaspaghetti.utils.copyAssetsTo
import com.bsbeetle.bananaspaghetti.utils.downloadFile
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

    external fun getAlpineFork(
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
    ) : Int

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
                copyAssetsTo(PREFIX, "scripts/init-sandbox.sh", context)
                copyAssetsTo(PREFIX, "scripts/start-alpine.sh", context)
                copyAssetsTo(PREFIX, "scripts/boot.sh", context)
                copyAssetsTo(PREFIX, "scripts/proot-wrapper.sh", context)

                val dotDownloaded = File(PREFIX, ".downloaded")
                dotDownloaded.mkdirs()

                Log.d("Tenebrio", "Download complete")

                // Creating necessary folders
                // Always to be run before executing getAlpineFork()
                if (!tmp.exists()) tmp.mkdirs()
                if (!alpine.exists()) alpine.mkdirs()
                if (!alpineTmp.exists()) alpineTmp.mkdirs()
                if (!public.exists()) public.mkdirs()

                // ShellExecutor.sendCommand("tar --no-same-owner -xf ${PREFIX}/alpine.tar.gz -C ${alpine.absolutePath}")

                val process = Runtime.getRuntime().exec("tar --no-same-owner -xf ${PREFIX}/alpine.tar.gz -C ${alpine.absolutePath}")
                process.waitFor()  // <-- waits until extraction finishes

                Log.d("Tenebrio", "Extracted the rootfs")

                // Check if these files exists
                if(!prootLoader.exists()) Log.e("INIT_SHELL", "PROOT_LOADER doesn't exists")
                if(!prootLoader32.exists()) Log.e("INIT_SHELL", "PROOT_LOADER32 doesn't exists")
                if(libtalloc.exists()) libtalloc.delete()

                // ShellExecutor.sendCommand("ln -s $nativeDir/libtalloc.so $PREFIX/libtalloc.so.2")
                val libtallocLink = File(PREFIX, "libtalloc.so.2")
                if (libtallocLink.exists()) libtallocLink.delete()
                java.nio.file.Files.createSymbolicLink(
                    libtallocLink.toPath(),
                    File(nativeDir, "libtalloc.so").toPath()
                )

                Log.d("Tenebrio", "Linked the libtalloc.so")

                testShell()
            }

            Log.d("Tenebrio", "Exited downloadFile process")

        } else {

            Log.d("Tenebrio_INIT: ", "Already downloaded")

        }

    }

    fun testShell() {

        Log.d("Tenebrio", "Entered testShell")

        val masterFd = getAlpineFork(
            PREFIX.absolutePath.toString(),
            nativeDir,
            tmp.absolutePath.toString(),
            prootLoader.absolutePath.toString(),
            prootLoader32.absolutePath.toString(),
            proot.absolutePath.toString(),
            "${public.absolutePath}:/public",
            "${alpineTmp.absolutePath}:/dev/shm",
            alpine.absolutePath.toString(),
            "${PREFIX.absolutePath}/init-alpine.sh"
        )

        if (masterFd < 0) {
            Log.e("Tenebrio", "Failed to get the master fd: $masterFd")
            return
        } else {
            Log.d("Tenebrio", "Got the masterFd")
        }

        pfd = ParcelFileDescriptor.adoptFd(masterFd)
        output = FileOutputStream(pfd!!.fileDescriptor)
        input = FileInputStream(pfd!!.fileDescriptor)

        // reader thread
        thread {
            try {
                val buffer = ByteArray(1024)
                while (true) {
                    val bytesRead = input!!.read(buffer)
                    if (bytesRead > 0) {
                        val text = String(buffer, 0, bytesRead)
                        Log.d("Shell", "Output: $text")
                    } else if (bytesRead < 0) {
                        break // EOF or error
                    }
                }
            } catch (e: Exception) {
                Log.e("TenebrioShell", "Error reading", e)
            }
        }

        // Writer thread
        thread {
            try {
                Thread.sleep(1000) // Reduced wait time

                val commands = listOf(
                    "echo 'Hello from shell!'\n",
                    "pwd\n",
                    "ls /\n",
                    "whoami\n",
                    "uname -a\n"
                )

                for (command in commands) {
                    Log.d("Shell", "Sending: ${command.trim()}")
                    output!!.write(command.toByteArray())
                    output!!.flush()
                    Thread.sleep(2000)
                }

            } catch (e: Exception) {
                Log.e("TenebrioShell", "Error Sending", e)
            }
        }
    }

    // Optional: Add cleanup method
    fun cleanup() {
        try {
            input?.close()
            output?.close()
            pfd?.close()
        } catch (e: Exception) {
            Log.e("Tenebrio", "Error during cleanup", e)
        }
    }

}