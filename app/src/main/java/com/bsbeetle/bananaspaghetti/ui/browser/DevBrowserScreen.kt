package com.bsbeetle.bananaspaghetti.ui.browser

import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevBrowserScreen() {
    var url by rememberSaveable { mutableStateOf("https://google.com") }
    var urlInput by rememberSaveable { mutableStateOf(url) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var loadProgress by remember { mutableStateOf(0) }
    var showDevTools by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current

    val navigateTo: (String) -> Unit = { newUrl ->
        var finalUrl = newUrl.trim()
        if (!finalUrl.startsWith("http://") && !finalUrl.startsWith("https://")) {
            finalUrl = "https://$finalUrl"
        }
        url = finalUrl
        urlInput = finalUrl
        webView?.loadUrl(finalUrl)
    }


    val updateNavigation: () -> Unit = {
        webView?.let { wv ->
            canGoBack = wv.canGoBack()
            canGoForward = wv.canGoForward()
            // Update URL bar to match current page
            wv.url?.let { currentUrl ->
                url = currentUrl
                urlInput = currentUrl
            }
        }
    }

    val toggleDevTools: () -> Unit = {
        showDevTools = !showDevTools
        webView?.evaluateJavascript(
            if (showDevTools) "eruda.show();" else "eruda.hide();",
            null
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // URL Bar
        Box(
            modifier = Modifier
                .background(color = Color.Black)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button
                IconButton(
                    onClick = { webView?.goBack(); updateNavigation() },
                    enabled = canGoBack
                ) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                }

                // Forward Button
                IconButton(
                    onClick = { webView?.goForward(); updateNavigation() },
                    enabled = canGoForward
                ) {
                    Icon(Icons.Default.ArrowForward, "Forward", tint = Color.White)
                }

                // Reload Button
                IconButton(onClick = { webView?.reload() }) {
                    Icon(Icons.Default.Refresh, "Reload", tint = Color.White)
                }

                // URL TextField
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("Enter URL...") },
                    trailingIcon = {
                        IconButton(onClick = { navigateTo(urlInput) }) {
                            Icon(Icons.Default.Search, "Go")
                        }
                    }
                )

                // Dev Tools Toggle
                IconButton(
                    onClick = toggleDevTools,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (showDevTools) MaterialTheme.colorScheme.primary
                        else Color.Transparent
                    )
                ) {
                    Icon(
                        Icons.Default.Build,
                        "Dev Tools",
                        tint = if (showDevTools) Color.White
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Loading Progress
        if (isLoading) {
            LinearProgressIndicator(
                progress = { loadProgress / 100f },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // WebView
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            factory = { ctx ->
                DevWebViewManager.createWebView(
                    context = ctx,
                    onPageFinished = {
                        updateNavigation()
                        isLoading = false
                    },
                    onProgressChanged = { progress ->
                        loadProgress = progress
                        isLoading = progress < 100
                    },
                    onFrameworkDetected = { _, _ -> },
                    onNetworkRequest = { },
                    onConsoleLog = { },
                    onUrlChanged = { newUrl ->
                        url = newUrl
                        urlInput = newUrl
                        updateNavigation()
                    }
                ).also { wv ->
                    webView = wv
                    wv.loadUrl(url)  // Load initial URL
                }
            }
        )
    }
}