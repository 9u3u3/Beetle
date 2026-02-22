package com.bsbeetle.bananaspaghetti.utils.resourceHandling

import android.content.Context
import coil.ImageLoader
import coil.decode.SvgDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache

object ImageLoaderSingleton {
    @Volatile
    private var instance: ImageLoader? = null

    fun get(context: Context): ImageLoader {
        return instance ?: synchronized(this) {
            instance ?: ImageLoader.Builder(context.applicationContext)
                .components { add(SvgDecoder.Factory()) }
                .memoryCache {
                    MemoryCache.Builder(context.applicationContext)
                        .maxSizePercent(0.15) // 15% of available memory
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(context.applicationContext.cacheDir.resolve("extension_icons"))
                        .maxSizeBytes(20L * 1024 * 1024) // 20 MB
                        .build()
                }
                .build().also { instance = it }
        }
    }
}
