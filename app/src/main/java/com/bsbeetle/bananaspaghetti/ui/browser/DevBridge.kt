package com.bsbeetle.bananaspaghetti.ui.browser


import android.util.Log
import android.webkit.JavascriptInterface
import org.json.JSONObject

class DevBridge(
    private val onFrameworkDetected: (String, String) -> Unit,
    private val onNetworkRequest: () -> Unit,
    private val onConsoleLog: () -> Unit
) {

    @JavascriptInterface
    fun logNetwork(requestData: String) {
        try {
            val json = JSONObject(requestData)
            Log.d("Network", """
                📡 ${json.optString("method")} ${json.optString("url")}
                Status: ${json.optInt("status")} (${json.optLong("duration")}ms)
                Type: ${json.optString("type")}
            """.trimIndent())
            onNetworkRequest()
        } catch (e: Exception) {
            Log.e("DevBridge", "Network parse error: ${e.message}")
        }
    }

    @JavascriptInterface
    fun detectFramework(name: String, version: String) {
        Log.i("Framework", "🎯 Detected: $name $version")
        onFrameworkDetected(name, version)
    }

    @JavascriptInterface
    fun logConsole(level: String, message: String) {
        Log.d("Console", "[$level] $message")
        onConsoleLog()
    }
}