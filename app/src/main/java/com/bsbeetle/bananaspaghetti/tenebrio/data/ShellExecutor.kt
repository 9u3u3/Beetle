package com.bsbeetle.bananaspaghetti.tenebrio.data

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.BufferedWriter
import kotlin.concurrent.thread

object ShellExecutor {

    var process: Process? = null
    var writer: BufferedWriter? = null
    var reader: BufferedReader? = null

    fun startShell(context: Context) {

        if(process != null) return

        val filesDir = context.filesDir.absolutePath

        val processBuilder = ProcessBuilder("sh")
        processBuilder.redirectErrorStream(true)
        val env = processBuilder.environment()
        env["PREFIX"] = filesDir
        env["FDROID"] = "false"
        env["NATIVE_DIR"] = context.applicationInfo.nativeLibraryDir

        process = processBuilder.start()
        writer = process!!.outputStream.bufferedWriter()
        reader = process!!.inputStream.bufferedReader()

        thread {
            reader!!.forEachLine { line ->
                Log.d("ShellOutput", line)
            }
        }

    }

    fun sendCommand(command: String) {
        writer?.apply {
            write(command)
            write("\n")
            flush()
        }
    }

    fun stopShell() {
        writer?.apply {
            write("exit\n")
            flush()
        }
        process?.waitFor()
        process = null
        writer = null
        reader = null
    }


}