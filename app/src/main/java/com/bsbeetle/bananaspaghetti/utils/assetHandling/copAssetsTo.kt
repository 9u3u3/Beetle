package com.bsbeetle.bananaspaghetti.utils.assetHandling

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream

/**
 * Copies the files to specified location
 * @param dir destination
 * @param assetName name of the asset to be saved
 * @param context context of the app*/
fun copyAssetsTo(
    dir: File,
    assetName: String,
    context: Context
) {
    Log.d("NativeLSP", "Called Copy Asset")
    val outFile = File(context.filesDir, assetName.substringAfter('/'))
    context.assets.open(assetName).use { inputStream ->
        FileOutputStream(outFile).use { outputStream ->
            inputStream.copyTo(outputStream)
        }
    }
}