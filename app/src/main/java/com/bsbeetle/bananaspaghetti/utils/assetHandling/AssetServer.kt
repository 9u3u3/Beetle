package com.bsbeetle.bananaspaghetti.utils.assetHandling

import android.content.Context
import fi.iki.elonen.NanoHTTPD
import java.io.InputStream
import java.net.SocketException
import java.net.SocketTimeoutException

class AssetServer(private val context: Context, port: Int = 8080) : NanoHTTPD(port) {

    override fun serve(session: IHTTPSession?): Response {
        val uri = session?.uri ?: "/index.html"
        
        // Serve plugins from filesDir to allow dynamic loading via HTTP
        if (uri.startsWith("/plugins/")) {
            val relativePath = uri.removePrefix("/")
            val file = java.io.File(context.filesDir, relativePath)
            if (file.exists()) {
                val mime = "application/javascript"
                return newFixedLengthResponse(Response.Status.OK, mime, java.io.FileInputStream(file), file.length())
            }
        }

        val assetPath = "editor$uri" // maps /index.html -> assets/editor/index.html

        return try {
            val stream: InputStream = context.assets.open(assetPath)
            val mime = when {
                uri.endsWith(".html") -> "text/html"
                uri.endsWith(".js") -> "application/javascript"
                uri.endsWith(".css") -> "text/css"
                else -> "application/octet-stream"
            }
            newFixedLengthResponse(Response.Status.OK, mime, stream, stream.available().toLong())
        } catch (e: Exception) {
            if(e is SocketException || e is SocketTimeoutException) {
                newFixedLengthResponse(Response.Status.NO_CONTENT, "text/plain", "")
            }
            newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "File not found")
        }
    }
}