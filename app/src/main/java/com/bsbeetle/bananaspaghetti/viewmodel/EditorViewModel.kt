package com.bsbeetle.bananaspaghetti.viewmodel

// Editor view model is only One in the whole app so it should switch up between all the webviews
// Editor view model is made to only keep the webview persistent

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bsbeetle.bananaspaghetti.utils.assetHandling.AssetServer
import com.bsbeetle.bananaspaghetti.utils.languageHandling.LspServer
import com.bsbeetle.bananaspaghetti.utils.ssh.RemoteLspBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.bsbeetle.bananaspaghetti.utils.webViewHandling.CustomWebView

class EditorViewModel(app: Application) : AndroidViewModel(app) {
    // Map of slug -> LspServer (local)
    private val lspServers = mutableMapOf<String, LspServer>()
    // Map of slug -> Port (local)
    private val lspPorts = mutableMapOf<String, Int>()

    // Remote LSP bridges (slug -> bridge, always on port 8083)
    private val remoteLspBridges = mutableMapOf<String, RemoteLspBridge>()

    // LRU list to track usage (slugs)
    private val lruSlugs = mutableListOf<String>()

    // Available ports (simple pool)
    private val availablePorts = mutableListOf(8081, 8082)

    init {
        AssetServer.startIfNotRunning(getApplication())
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

    /**
     * Suspends until the LSP server for the given slug is ready to accept WebSocket connections.
     * Call this after startLsp() before the editor tries to connect.
     */
    suspend fun awaitLspReady(slug: String) {
        lspServers[slug]?.serverReady?.await()
    }

    /**
     * Starts a remote LSP bridge for the given slug using an SSH session.
     * The bridge proxies the remote LSP process's stdin/stdout over a local WebSocket on port 8083.
     * All bridges share port 8083, so all existing ones are stopped on the IO thread before the
     * new one binds the port.
     * @return The port (8083) the WebSocket server is listening on.
     */
    fun startRemoteLsp(slug: String, sshSession: com.jcraft.jsch.Session): Int {
        val port = 8083
        // Collect all existing bridges to stop (they all share port 8083)
        val oldBridges = remoteLspBridges.values.toList()
        remoteLspBridges.clear()

        val bridge = RemoteLspBridge(port)
        remoteLspBridges[slug] = bridge

        viewModelScope.launch(Dispatchers.IO) {
            // Stop old bridges first so port 8083 is free before new bridge binds it
            oldBridges.forEach { it.stop() }
            bridge.start(slug, sshSession)
        }
        Log.d("EditorViewModel", "Remote LSP bridge starting for $slug on port $port")
        return port
    }

    /** Suspends until the remote LSP bridge WebSocket server is ready. */
    suspend fun awaitRemoteLspReady(slug: String) {
        remoteLspBridges[slug]?.serverReady?.await()
    }

    /** Stops all remote LSP bridges (called when disconnecting from SSH). */
    fun stopAllRemoteLsp() {
        remoteLspBridges.values.forEach { it.stop() }
        remoteLspBridges.clear()
    }

    // IMPORTANT: Using StateFlow so UI recomposes when webView changes
    // StateFlow is the recommended pattern for ViewModels
    @Suppress("StaticFieldLeak")
    private val _webView = MutableStateFlow<CustomWebView?>(null)
    val webViewFlow: StateFlow<CustomWebView?> = _webView.asStateFlow()

    // Direct access for compatibility (non-reactive)
    val webView: CustomWebView? get() = _webView.value

    // Call this value explicitly as there can be any type of session in the active session
    fun setEditorWebView(webView: CustomWebView) {
        Log.d("EditorViewModel", "setEditorWebView called with webView for session: ${webView.session?.name}")
        Log.d("EditorViewModel", "Previous webView was for session: ${_webView.value?.session?.name}")
        _webView.value = webView
        Log.d("EditorViewModel", "webViewFlow updated. New value: ${_webView.value?.session?.name}")
    }
}
