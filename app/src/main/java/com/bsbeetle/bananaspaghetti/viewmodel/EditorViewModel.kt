package com.bsbeetle.bananaspaghetti.viewmodel

// Editor view model is only One in the whole app so it should switch up between all the webviews
// Editor view model is made to only keep the webview persistent

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.bsbeetle.bananaspaghetti.utils.assetHandling.AssetServer
import com.bsbeetle.bananaspaghetti.utils.webViewHandling.CustomWebView

/*TODO*/
// Implementation of the sessions
// Write functions to swap the webViews on call

import androidx.lifecycle.viewModelScope
import com.bsbeetle.bananaspaghetti.utils.languageHandling.LspServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.util.Log

class EditorViewModel(app: Application) : AndroidViewModel(app) {
    // Map of slug -> LspServer
    private val lspServers = mutableMapOf<String, LspServer>()
    // Map of slug -> Port
    private val lspPorts = mutableMapOf<String, Int>()
    
    // LRU list to track usage (slugs)
    private val lruSlugs = mutableListOf<String>()
    
    // Available ports (simple pool)
    private val availablePorts = mutableListOf(8081, 8082)

    init { 
        AssetServer(getApplication(), 8080).start() 
    }

    fun startLsp(slug: String): Int {
        // If already running, move to front of LRU and return port
        if (lspServers.containsKey(slug)) {
            lruSlugs.remove(slug)
            lruSlugs.add(slug)
            return lspPorts[slug]!!
        }

        // If max servers (2) reached, stop the least recently used one
        if (lspServers.size >= 2) {
            val victimSlug = lruSlugs.removeAt(0)
            val victimServer = lspServers.remove(victimSlug)
            val victimPort = lspPorts.remove(victimSlug)
            
            victimServer?.stop()
            if (victimPort != null) {
                availablePorts.add(victimPort)
            }
            Log.d("EditorViewModel", "Stopped LSP for $victimSlug to free up resources")
        }

        // Start new server - check if ports are available
        if (availablePorts.isEmpty()) {
            Log.w("EditorViewModel", "No available ports for LSP, reusing lowest port")
            // Force stop the first server if no ports available
            if (lruSlugs.isNotEmpty()) {
                val victimSlug = lruSlugs.removeAt(0)
                val victimServer = lspServers.remove(victimSlug)
                val victimPort = lspPorts.remove(victimSlug)
                victimServer?.stop()
                if (victimPort != null) {
                    availablePorts.add(victimPort)
                }
            } else {
                // Fallback - just use 8081
                availablePorts.add(8081)
            }
        }
        
        val port = availablePorts.removeAt(0)
        val newServer = LspServer(getApplication(), port)
        
        viewModelScope.launch(Dispatchers.IO) {
            newServer.start(slug)
        }
        
        lspServers[slug] = newServer
        lspPorts[slug] = port
        lruSlugs.add(slug)
        
        return port
    }
    @SuppressLint("StaticFieldLeak")
    var webView: CustomWebView? = null

    // Call this value explicitly as there can be any type of session in the active session
    fun setEditorWebView(webView: CustomWebView) {this.webView = webView}
}