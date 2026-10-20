package com.bsbeetle.bananaspaghetti.utils.fileHandling

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import javax.net.ssl.HttpsURLConnection

fun downloadFile(context: Context, url: String, fileName: String, onDone: (File?) -> Unit) {

    CoroutineScope(Dispatchers.IO).launch {
        try {

            val connection = URL(url).openConnection() as HttpsURLConnection
            connection.connect()

            // Save in filesDir. Close the stream and disconnect even if the copy throws.
            val file = File(context.filesDir, fileName)
            try {
                connection.inputStream.use { input ->
                    FileOutputStream(file).use { output -> input.copyTo(output) }
                }
            } finally {
                connection.disconnect()
            }
            onDone(file)

        } catch (e: Exception) {

            e.printStackTrace()
            onDone(null)

        }
    }
}