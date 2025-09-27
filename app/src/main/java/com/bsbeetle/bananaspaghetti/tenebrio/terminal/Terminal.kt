package com.bsbeetle.bananaspaghetti.tenebrio.terminal

import android.content.Context
import android.util.Log
import com.bsbeetle.bananaspaghetti.tenebrio.data.ShellExecutor
import com.bsbeetle.bananaspaghetti.utils.copyAssetsTo
import com.bsbeetle.bananaspaghetti.utils.downloadFile
import java.io.File

/**
 * Terminal method implementations
 * @param context Context of the app*/
class Terminal (private val context: Context) {

    private val filesDirPath: String = context.filesDir.absolutePath
    private val filesDir = context.filesDir

    /**
     * Start the AXS (Some Acode thing) environment by writing init scripts and executing the sandbox
     * @param installing Whether AXS is being started during installation */
    fun startAxs(
        installing: Boolean = false,
    ) {

        // Copy the scripts to filesDir
        // 1. Copy the init-alpine.sh to filesDir
        copyAssetsTo(filesDir, "scripts/init-alpine.sh", context)
        // 2. Copy the init-sandbox.sh
        copyAssetsTo(filesDir, "scripts/init-sandbox.sh", context)
        // 3. Copy the start-alpine.sh
        copyAssetsTo(filesDir, "scripts/start-alpine.sh", context)
        // 4. Copy the boot.sh
        copyAssetsTo(filesDir, "scripts/boot.sh", context)
        // 5. Copy the boot.sh
        copyAssetsTo(filesDir, "scripts/proot-wrapper.sh", context)

        if(installing) {
            println("Inside Installing")
            ShellExecutor.sendCommand("source $filesDirPath/init-sandbox.sh --installing\n")
        } else {
            println("Inside Already Installing")
            ShellExecutor.sendCommand("source $filesDirPath/proot-wrapper.sh")
            ShellExecutor.sendCommand("apk --version")
            ShellExecutor.sendCommand("mkdir hello_using_proot_wrapper")
            ShellExecutor.sendCommand("touch ~/hello_using_proot_wrapper/hello.txt")
            ShellExecutor.sendCommand("fastfetch")
            ShellExecutor.sendCommand("apk add python3 && python3 -m http.server")
        }
    }

    /**
     * Installs alpine root file system and AXS, Extracts the root file system in the filesDir.
     * Extracts the alpine root file system in the files/alpine directory
     * @param context Context of the application*/
    fun installRootfs() {

        val axsUrl = "https://github.com/bajrangCoder/acodex_server/releases/latest/download/axs-musl-android-arm64"
        val alpineUrl = "https://dl-cdn.alpinelinux.org/alpine/v3.21/releases/aarch64/alpine-minirootfs-3.21.0-aarch64.tar.gz"

        downloadFile(
            context,
            axsUrl,
            "axs"
        ) { file ->
            if(file == null) {
                Log.d("Install", "Failed to download from $axsUrl")
            }
        }

        downloadFile(
            context,
            alpineUrl,
            "alpine.tar.gz"
        ) { file ->
            if(file == null) {
                Log.d("Install", "Failed to download from $alpineUrl")
            } else {

                val alpineDir = File(filesDirPath, "alpine")
                alpineDir.mkdirs()

                // filesDir/.downloaded (to confirm that it is downloaded)
                val dotDownloaded = File(filesDirPath, ".downloaded")
                dotDownloaded.mkdirs()

                ShellExecutor.sendCommand("tar --no-same-owner -xf ${filesDirPath}/alpine.tar.gz -C ${alpineDir.absolutePath}")
                startAxs(true)

            }
        }

    }

}