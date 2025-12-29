package com.bsbeetle.bananaspaghetti.tenebrio.pty

import android.content.Context
import android.os.ParcelFileDescriptor
import android.util.Log
import com.bsbeetle.bananaspaghetti.utils.assetHandling.copyAssetsTo
import com.bsbeetle.bananaspaghetti.utils.fileHandling.downloadFile
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.Session
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.TerminalSession
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import kotlin.concurrent.thread
import org.json.JSONObject

class Tenebrio(private val context: Context, private val session: Session) {

    companion object {
        init {
            System.loadLibrary("pty-lib")
        }

        // Lock to prevent multiple concurrent downloads/extractions
        @Volatile private var isDownloading = false
        private val downloadLock = Any()
    }

    external fun startUbuntu(
            prefix: String,
            jNativeDirPath: String,
            jProotTmpDir: String,
            jProotLoader: String,
            jProotLoader32: String,
            jProot: String,
            jPublic: String,
            jUbuntuTmp: String,
            jUbuntu: String,
            jUbuntuInit: String,
            jRows: Int,
            jCols: Int
    ): Int

    external fun resizePty(masterFd: Int, rows: Int, cols: Int): Int

    var gMasterFd: Int = -1
    val ubuntuUrl = "https://cdimage.ubuntu.com/ubuntu-base/releases/24.04/release/ubuntu-base-24.04.3-base-arm64.tar.gz"
    private val nativeDir: String = context.applicationInfo.nativeLibraryDir
    private var pfd: ParcelFileDescriptor? = null
    private var input: FileInputStream? = null
    private var output: FileOutputStream? = null
    private val PREFIX: File = context.filesDir
    private val tmp: File = File(PREFIX, "tmp")
    private val ubuntu: File = File(PREFIX, "ubuntu")
    private val ubuntuTmp: File = File(ubuntu, "tmp")
    private val public: File = File(PREFIX, "public")
    private val dotDownloaded: File = File(PREFIX, ".downloaded")
    private val prootLoader: File = File(nativeDir, "libproot.so")
    private val prootLoader32: File = File(nativeDir, "libproot32.so")
    private val proot: File = File(nativeDir, "libproot-xed.so")

    fun resizeTerminal(rows: Int, cols: Int) {
        if (gMasterFd > 0) {
            resizePty(gMasterFd, rows, cols)
        }
    }

    fun init() {
        Log.d("Tenebrio", "Initializing")

        // Always copy the code-runner script so updates take effect immediately
        copyAssetsTo(PREFIX, "scripts/code-runner.sh", context)

        // Check if Ubuntu is properly extracted by verifying /bin/bash exists
        val ubuntuBash = File(ubuntu, "bin/bash")

        if (!ubuntuBash.exists()) {
            // Use synchronized block to prevent multiple concurrent downloads
            synchronized(downloadLock) {
                // Double-check after acquiring lock
                if (ubuntuBash.exists()) {
                    Log.d("Tenebrio", "Ubuntu shell was extracted by another thread")
                    return
                }

                if (isDownloading) {
                    Log.d("Tenebrio", "Download already in progress, skipping...")
                    return
                }
                isDownloading = true
            }

            Log.d("Tenebrio", "Ubuntu shell not found, downloading and extracting...")

            // Clean up any partial extraction
            if (dotDownloaded.exists()) {
                Log.w("Tenebrio", "Found .downloaded marker but /bin/bash missing - re-extracting")
                dotDownloaded.delete()
            }

            // Clean up corrupted ubuntu directory
            if (ubuntu.exists()) {
                Log.d("Tenebrio", "Cleaning up corrupted ubuntu directory...")
                ubuntu.deleteRecursively()
            }

            downloadFile(context, ubuntuUrl, "ubuntu.tar.gz") { file ->
                try {
                    if (file == null) {
                        Log.e("Tenebrio", "Failed to download Ubuntu root file system")
                        return@downloadFile
                    }

                    copyAssetsTo(PREFIX, "scripts/init-ubuntu.sh", context)
                    copyAssetsTo(PREFIX, "scripts/code-runner.sh", context)
                    Log.d("Tenebrio", "Download complete, starting extraction...")

                    // Creating necessary folders
                    if (!tmp.exists()) tmp.mkdirs()
                    if (!ubuntu.exists()) ubuntu.mkdirs()
                    if (!ubuntuTmp.exists()) ubuntuTmp.mkdirs()
                    if (!public.exists()) public.mkdirs()

                    // Setup environment for proot
                    val env =
                            arrayOf(
                                    "PREFIX=${PREFIX.absolutePath}",
                                    "LD_LIBRARY_PATH=${PREFIX.absolutePath}",
                                    "PROOT_TMP_DIR=${tmp.absolutePath}",
                                    "PROOT_LOADER=${prootLoader.absolutePath}",
                                    "PROOT_LOADER_32=${prootLoader32.absolutePath}"
                            )

                    // First, setup libtalloc symlink needed by proot
                    val libtallocLink = File(PREFIX, "libtalloc.so.2")
                    try {
                        Files.deleteIfExists(libtallocLink.toPath())
                        Files.createSymbolicLink(
                                libtallocLink.toPath(),
                                File(nativeDir, "libtalloc.so").toPath()
                        )
                    } catch (e: Exception) {
                        Log.e("Tenebrio", "Failed to create libtalloc symlink", e)
                    }

                    // Use proot with --link2symlink to extract (handles symlinks on Android)
                    // proot translates symlinks to work on Android's restricted filesystem
                    val pb =
                            ProcessBuilder(
                                    proot.absolutePath,
                                    "--link2symlink",
                                    "-0",
                                    "-r",
                                    ubuntu.absolutePath,
                                    "-b",
                                    "/dev",
                                    "-b",
                                    "/proc",
                                    "-b",
                                    "/sys",
                                    "-w",
                                    "/",
                                    "tar",
                                    "--no-same-owner",
                                    "-xzf",
                                    "${PREFIX}/ubuntu.tar.gz"
                            )
                    pb.environment()
                            .putAll(
                                    env.associate {
                                        val parts = it.split("=", limit = 2)
                                        parts[0] to parts[1]
                                    }
                            )
                    pb.redirectErrorStream(true)
                    val process = pb.start()

                    // Read any output/errors
                    val output = process.inputStream.bufferedReader().readText()
                    val exitCode = process.waitFor()

                    if (exitCode != 0) {
                        Log.e("Tenebrio", "proot tar extraction failed with exit code: $exitCode")
                        Log.e("Tenebrio", "proot output: $output")

                        // Fallback: try direct tar anyway
                        Log.d("Tenebrio", "Falling back to direct tar extraction...")
                        val fallbackPb =
                                ProcessBuilder(
                                        "tar",
                                        "--no-same-owner",
                                        "-xzf",
                                        "${PREFIX}/ubuntu.tar.gz",
                                        "-C",
                                        ubuntu.absolutePath
                                )
                        fallbackPb.redirectErrorStream(true)
                        val fallbackProcess = fallbackPb.start()
                        fallbackProcess.inputStream.bufferedReader().readText()
                        fallbackProcess.waitFor()
                    }

                    // Check for bin directory and bash
                    val binDir = File(ubuntu, "bin")
                    val bashFile = File(binDir, "bash")
                    val shFile = File(binDir, "sh")

                    Log.d(
                            "Tenebrio",
                            "bin exists: ${binDir.exists()}, bash exists: ${bashFile.exists()}, sh exists: ${shFile.exists()}"
                    )

                    // If bin doesn't exist at all, the tar truly failed
                    if (!binDir.exists()) {
                        Log.e("Tenebrio", "bin directory doesn't exist after extraction!")
                        ubuntu.listFiles()?.forEach {
                            Log.d("Tenebrio", "Ubuntu contents: ${it.name}")
                        }
                        return@downloadFile
                    }

                    // List bin directory contents for debugging
                    binDir.listFiles()?.take(5)?.forEach {
                        Log.d(
                                "Tenebrio",
                                "bin/ contents: ${it.name} (symlink=${Files.isSymbolicLink(it.toPath())})"
                        )
                    }

                    // Verify extraction succeeded - check for bash
                    if (!bashFile.exists()) {
                        Log.e("Tenebrio", "Extraction completed but /bin/bash not found!")
                        return@downloadFile
                    }

                    Log.d("Tenebrio", "Extracted the rootfs successfully")

                    val resolvconf: File = File(ubuntu, "/etc/resolv.conf")
                    resolvconf.createNewFile()
                    resolvconf.writeText("nameserver 8.8.4.4 \nnameserver 8.8.8.8")

                    if (!prootLoader.exists()) Log.e("INIT_SHELL", "PROOT_LOADER doesn't exists")
                    if (!prootLoader32.exists())
                            Log.e("INIT_SHELL", "PROOT_LOADER32 doesn't exists")

                    // libtalloc symlink was already created earlier for proot extraction

                    // Create marker ONLY after successful extraction
                    dotDownloaded.mkdirs()
                    Log.d("Tenebrio", "Ubuntu setup complete!")
                } finally {
                    synchronized(downloadLock) { isDownloading = false }
                }
            }
        } else {
            // FIXED: Force delete the symlink even if it appears broken
            val libtallocLink = File(PREFIX, "libtalloc.so.2")
            try {
                // This will delete the symlink even if it's broken
                Files.deleteIfExists(libtallocLink.toPath())
            } catch (e: Exception) {
                Log.e("Tenebrio", "Failed to delete existing symlink", e)
            }
            // Now create the fresh symlink
            try {
                Files.createSymbolicLink(
                        libtallocLink.toPath(),
                        File(nativeDir, "libtalloc.so").toPath()
                )
                Log.d("Tenebrio", "Recreated libtalloc.so.2 symbolic link")
            } catch (e: Exception) {
                Log.e("Tenebrio", "Failed to create symlink", e)
                return
            }

            // Global make it session wise
            val terminalSession = session as TerminalSession
            if (terminalSession.rows == null || terminalSession.cols == null) {
                Log.d("Tenebrio", "Null sizes - waiting for WebView to report terminal dimensions")
                return // Will be retried when getRowsCols is called from JavaScript
            }

            val masterFd =
                    startUbuntu(
                            PREFIX.absolutePath.toString(),
                            nativeDir,
                            tmp.absolutePath.toString(),
                            prootLoader.absolutePath.toString(),
                            prootLoader32.absolutePath.toString(),
                            proot.absolutePath.toString(),
                            "${public.absolutePath}:/public",
                            "${ubuntuTmp.absolutePath}:/dev/shm",
                            ubuntu.absolutePath.toString(),
                            "${PREFIX.absolutePath}/init-ubuntu.sh",
                            terminalSession.rows!!,
                            terminalSession.cols!!
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

            terminalSession.outputFd = output

            thread {
                try {
                    val buffer = ByteArray(1024)
                    while (true) {
                        val bytesRead = input!!.read(buffer)
                        if (bytesRead > 0) {
                            val text = String(buffer, 0, bytesRead)
                            // Access view lazily - it may be created after init()
                            val terminalWebView = terminalSession.view
                            if (terminalWebView != null) {
                                terminalWebView.post {
                                    val jsCommand = "term.write(${JSONObject.quote(text)});"
                                    terminalWebView.evaluateJavascript(jsCommand, null)
                                }
                            }
                            // Always invoke onOutput callback if set (for code runner to detect
                            // MISSING_PKG)
                            terminalSession.onOutput?.invoke(text)
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
