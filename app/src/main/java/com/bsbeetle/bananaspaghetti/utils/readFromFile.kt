package com.bsbeetle.bananaspaghetti.utils

import android.content.Context
import android.net.Uri
import java.io.BufferedReader
import java.io.FileNotFoundException
import java.io.InputStreamReader

fun readFromFile(context: Context, uri: Uri?): String {
    val contentResolver = context.contentResolver

    if (uri != null) {
        // inputStream gives raw bytes, so we usw InputStreamReader and BufferedReader reads the
        //... the text in efficient chunks
        val inputStream = contentResolver.openInputStream(uri) // get the input stream from the
        //... contentResolver
        inputStream?.use { stream ->
            BufferedReader(InputStreamReader(stream)).use { reader ->
                // Read all lines and then join them into one string
                return reader.readText()
            }
        }
    } else {
        throw FileNotFoundException("File not Found, please check the passed URI")
    }
    return ""
}