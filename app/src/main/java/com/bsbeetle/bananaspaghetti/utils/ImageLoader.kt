package com.bsbeetle.bananaspaghetti.utils

import android.content.Context
import coil.ImageLoader
import coil.decode.SvgDecoder

object ImageLoaderSingleton {
    @Volatile
    private var instance: ImageLoader? = null

    fun get(context: Context): ImageLoader {
        return instance ?: synchronized(this) {
            instance ?: ImageLoader.Builder(context)
                .components { add(SvgDecoder.Factory()) }
                .build().also { instance = it }
        }
    }
}
