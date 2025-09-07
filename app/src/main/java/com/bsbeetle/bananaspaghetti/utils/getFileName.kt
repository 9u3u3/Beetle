package com.bsbeetle.bananaspaghetti.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.FileNotFoundException

fun getFileName(context: Context, uri: Uri?): String {
    val contentResolver = context.contentResolver // The gatekeeper to access the data related to
    //... the files
    if(uri != null) {
        val cursor = contentResolver.query( // making a query to the content resolver, which returns
            //... a data set (like spreadsheet)
            uri, // search for this uri
            null, // all these are filters, in uri filter is not required as uri is
            //... unique
            null,
            null,
            null
        )
        cursor?.use { // use block as cursor is a system resource that holds the connection to the
            //... data
           if(it.moveToFirst()) { // moves to the first (one and only) row, returns true if
               //... if the row exists else false
               // the column index where the Display name is stored
               val displayNameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
               val fileName = it.getString(displayNameIndex)
               return fileName
           } else {
               throw FileNotFoundException("NULL URI")
           }
        }
    } else {
        throw FileNotFoundException("NULL URI")
    }
    return ""
}