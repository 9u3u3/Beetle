package com.bsbeetle.bananaspaghetti.utils

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

            val inputStream = connection.inputStream

            // Save in filesDir
            val file = File(context.filesDir, fileName)
            FileOutputStream(file).use { outputStream ->
                inputStream.copyTo(outputStream)
            }

            inputStream.close()
            onDone(file)

        } catch (e: Exception) {

            e.printStackTrace()
            onDone(null)

        }
    }
}