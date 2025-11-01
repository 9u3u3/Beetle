package com.bsbeetle.bananaspaghetti.utils

import android.content.Context
import fi.iki.elonen.NanoHTTPD
import java.io.InputStream

class AssetServer(private val context: Context, port: Int = 8080) : NanoHTTPD(port) {

    override fun serve(session: IHTTPSession?): Response {
        val uri = session?.uri ?: "/index.html"
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
            if(e is java.net.SocketException || e is java.net.SocketTimeoutException) {
                newFixedLengthResponse(Response.Status.NO_CONTENT, "text/plain", "")
            }
            newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "File not found")
        }
    }
}
