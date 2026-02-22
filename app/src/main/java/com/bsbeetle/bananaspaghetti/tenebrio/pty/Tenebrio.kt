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
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
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

    // Lifecycle management
    @Volatile
    private var isRunning = true
    private var readerThread: Thread? = null

    fun resizeTerminal(rows: Int, cols: Int) {
        if (gMasterFd > 0) {
            resizePty(gMasterFd, rows, cols)
        }
    }

    fun destroy() {
        Log.d("Tenebrio", "Destroying tenebrio instance")
        isRunning = false

        // Close streams — this causes the reader thread to get EOF/IOException and exit
        try { input?.close() } catch (e: Exception) { Log.w("Tenebrio", "Error closing input", e) }
        try { output?.close() } catch (e: Exception) { Log.w("Tenebrio", "Error closing output", e) }
        // Closing pfd closes the master FD, which triggers proot's --kill-on-exit
        try { pfd?.close() } catch (e: Exception) { Log.w("Tenebrio", "Error closing pfd", e) }

        input = null
        output = null
        pfd = null
        gMasterFd = -1

        // Wait briefly for reader thread to finish
        try {
            readerThread?.join(500)
        } catch (e: InterruptedException) {
            Log.w("Tenebrio", "Interrupted waiting for reader thread", e)
        }
        readerThread = null
    }

    fun init() {
        Log.d("Tenebrio", "Initializing")

        // Always copy the code-runner script so updates take effect immediately
        copyAssetsTo(PREFIX, "scripts/code-runner.sh", context)

        // Check if Ubuntu is properly extracted by verifying /bin/bash exists
        val ubuntuBash = File(ubuntu, "bin/bash")

        if (!ubuntuBash.exists()) {
            // Get terminal session for progress messages
            val terminalSession = session as? TerminalSession

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

            // Show installation message to user
            terminalSession?.sendInstallationMessage("\u001b[36m[SYSTEM]\u001b[0m Starting Download...")
            terminalSession?.sendInstallationMessage("\u001b[33m[INFO]\u001b[0m Installing Ubuntu Linux for the first time")
            terminalSession?.sendInstallationMessage("\u001b[33m[INFO]\u001b[0m Downloading Ubuntu base system (~30MB)...")

            // Clean up any partial extraction
            if (dotDownloaded.exists()) {
                Log.w("Tenebrio", "Found .downloaded marker but /bin/bash missing - re-extracting")
                dotDownloaded.delete()
            }

            // Clean up corrupted ubuntu directory
            if (ubuntu.exists()) {
                Log.d("Tenebrio", "Cleaning up corrupted ubuntu directory...")
                terminalSession?.sendInstallationMessage("\u001b[33m[INFO]\u001b[0m Cleaning up previous installation...")
                ubuntu.deleteRecursively()
            }

            downloadFile(context, ubuntuUrl, "ubuntu.tar.gz") { file ->
                try {
                    if (file == null) {
                        Log.e("Tenebrio", "Failed to download Ubuntu root file system")
                        terminalSession?.sendInstallationMessage("\u001b[31m[ERROR]\u001b[0m Download failed. Please check your internet connection.")
                        return@downloadFile
                    }

                    terminalSession?.sendInstallationMessage("\u001b[32m[SUCCESS]\u001b[0m Download complete!")
                    terminalSession?.sendInstallationMessage("\u001b[33m[INFO]\u001b[0m Extracting root filesystem...")

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

                    terminalSession?.sendInstallationMessage("\u001b[32m[SUCCESS]\u001b[0m Extraction complete!")
                    terminalSession?.sendInstallationMessage("\u001b[33m[INFO]\u001b[0m Setting up environment...")

                    val resolvconf: File = File(ubuntu, "/etc/resolv.conf")
                    resolvconf.createNewFile()
                    resolvconf.writeText("nameserver 8.8.4.4 \nnameserver 8.8.8.8")

                    if (!prootLoader.exists()) Log.e("INIT_SHELL", "PROOT_LOADER doesn't exists")
                    if (!prootLoader32.exists())
                            Log.e("INIT_SHELL", "PROOT_LOADER32 doesn't exists")

                    // libtalloc symlink was already created earlier for proot extraction

                    // Create marker ONLY after successful extraction
                    dotDownloaded.mkdirs()
                    terminalSession?.sendInstallationMessage("\u001b[32m[SUCCESS]\u001b[0m Installation complete!")
                    terminalSession?.sendInstallationMessage("\u001b[36m[SYSTEM]\u001b[0m Tap on the screen to start the shell")
                    terminalSession?.sendInstallationMessage("")
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

            // Output batching: accumulate chunks and flush at ~60fps to reduce JS bridge calls
            val accumulator = StringBuilder()
            val flushLock = Any()
            val FLUSH_INTERVAL_NS = 16_000_000L // 16ms = ~60fps

            // Scheduled executor for deferred flush — ensures the last batch always gets
            // displayed even when no more data follows (e.g. shell prompt waiting for input)
            val flushScheduler = Executors.newSingleThreadScheduledExecutor { r ->
                Thread(r, "Tenebrio-flusher-${session.id}").apply { isDaemon = true }
            }
            var pendingFlush: ScheduledFuture<*>? = null

            readerThread = thread(name = "Tenebrio-reader-${session.id}") {
                try {
                    val buffer = ByteArray(8192)
                    var lastFlush = System.nanoTime()

                    while (isRunning) {
                        val bytesRead = input?.read(buffer) ?: break
                        if (bytesRead > 0) {
                            val text = String(buffer, 0, bytesRead)

                            synchronized(flushLock) {
                                accumulator.append(text)
                            }

                            val now = System.nanoTime()
                            // Check if more data is already available in the PTY buffer.
                            // If not, this is likely the end of a command's output (e.g. the
                            // shell prompt is the last thing sent). Flush immediately so the
                            // prompt appears without waiting for the next timer tick.
                            val moreDataAvailable = (input?.available() ?: 0) > 0

                            if (!moreDataAvailable || now - lastFlush >= FLUSH_INTERVAL_NS) {
                                // Either no more data buffered (prompt/idle) or rate limit
                                // exceeded — flush immediately
                                pendingFlush?.cancel(false)
                                pendingFlush = null
                                flushToWebView(terminalSession, accumulator, flushLock)
                                lastFlush = now
                            } else {
                                // More data is already queued and within rate limit —
                                // schedule a deferred flush to batch it
                                if (pendingFlush == null || pendingFlush!!.isDone) {
                                    val delayMs = ((FLUSH_INTERVAL_NS - (now - lastFlush)) / 1_000_000)
                                        .coerceAtLeast(1)
                                    pendingFlush = flushScheduler.schedule({
                                        flushToWebView(terminalSession, accumulator, flushLock)
                                        lastFlush = System.nanoTime()
                                    }, delayMs, TimeUnit.MILLISECONDS)
                                }
                            }

                            // Always invoke onOutput callback if set (for code runner to detect MISSING_PKG)
                            terminalSession.onOutput?.invoke(text)
                        } else if (bytesRead < 0) {
                            break // EOF or error
                        }
                    }

                    // Final flush of any remaining output
                    pendingFlush?.cancel(false)
                    flushToWebView(terminalSession, accumulator, flushLock)

                } catch (e: Exception) {
                    if (isRunning) {
                        Log.e("TenebrioShell", "Error reading", e)
                    }
                }

                // Process exited — notify user if this wasn't an intentional destroy
                if (isRunning) {
                    Log.w("Tenebrio", "PTY process exited unexpectedly")
                    val webView = terminalSession.view
                    webView?.post {
                        val msg = "\r\n\u001b[31m[Process exited]\u001b[0m\r\n"
                        webView.evaluateJavascript(
                            "term.write(${JSONObject.quote(msg)});", null
                        )
                    }
                    terminalSession.onProcessExited?.invoke()
                }
            }
        }
    }

    private fun flushToWebView(
        session: TerminalSession,
        accumulator: StringBuilder,
        lock: Any
    ) {
        val chunk: String
        synchronized(lock) {
            if (accumulator.isEmpty()) return
            chunk = accumulator.toString()
            accumulator.clear()
        }
        val webView = session.view ?: return
        webView.post {
            webView.evaluateJavascript("term.write(${JSONObject.quote(chunk)});", null)
        }
    }
}
