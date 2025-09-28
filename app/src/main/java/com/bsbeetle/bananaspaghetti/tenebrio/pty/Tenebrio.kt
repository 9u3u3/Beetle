package com.bsbeetle.bananaspaghetti.tenebrio.pty

import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.concurrent.thread

class Tenebrio {

    companion object {
        init {
            System.loadLibrary("pty-lib")
        }
    }

    external fun getAlpineFork(): Int

    // Make these class-level to prevent GC
    private var pfd: ParcelFileDescriptor? = null
    private var input: FileInputStream? = null
    private var output: FileOutputStream? = null

    fun testShell() {
        val masterFd = getAlpineFork()

        if(masterFd < 0) {
            Log.e("Tenebrio", "Failed to get the master fd: $masterFd")
            return
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
                    if(bytesRead > 0) {
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

                for(command in commands) {
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