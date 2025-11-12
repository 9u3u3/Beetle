package com.bsbeetle.bananaspaghetti.ui.browser

import android.webkit.WebView
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Compact top bar - optimized spacing
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0A0A0A))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Navigation buttons - compact design
            CompactIconButton(
                onClick = { webView?.goBack(); updateNavigation() },
                enabled = canGoBack,
                icon = Icons.Default.ArrowBack
            )

            CompactIconButton(
                onClick = { webView?.goForward(); updateNavigation() },
                enabled = canGoForward,
                icon = Icons.Default.ArrowForward
            )

            CompactIconButton(
                onClick = { webView?.reload() },
                enabled = true,
                icon = Icons.Default.Refresh
            )

            // URL bar - takes maximum available space
            OutlinedTextField(
                value = urlInput,
                onValueChange = { urlInput = it },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 52.dp),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF3D3D3D),
                    unfocusedBorderColor = Color(0xFF2A2A2A),
                    focusedContainerColor = Color(0xFF1A1A1A),
                    unfocusedContainerColor = Color(0xFF151515),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color(0xFFE0E0E0),
                    cursorColor = Color.White
                ),
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal
                ),
                placeholder = {
                    Text(
                        "Search or enter URL",
                        color = Color(0xFF666666),
                        fontSize = 14.sp
                    )
                },
                trailingIcon = {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .padding(end = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = { navigateTo(urlInput) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.ArrowForward,
                                null,
                                tint = Color(0xFFBBBBBB),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            )

            // Dev tools button
            CompactIconButton(
                onClick = toggleDevTools,
                enabled = true,
                icon = Icons.Default.Build,
                isActive = showDevTools
            )
        }

        // Minimal progress indicator - no wasted space
        AnimatedVisibility(
            visible = isLoading,
            enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
            exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
        ) {
            LinearProgressIndicator(
                progress = { loadProgress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = Color(0xFF6366F1),
                trackColor = Color(0xFF1A1A1A)
            )
        }

        // WebView - maximum screen usage
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.White),
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
                    wv.loadUrl(url)
                }
            }
        )
    }
}

@Composable
private fun CompactIconButton(
    onClick: () -> Unit,
    enabled: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean = false
) {
    val backgroundColor = when {
        isActive -> Color(0xFF6366F1)
        enabled -> Color(0xFF1A1A1A)
        else -> Color(0xFF0F0F0F)
    }

    val iconColor = when {
        isActive -> Color.White
        enabled -> Color(0xFFBBBBBB)
        else -> Color(0xFF444444)
    }

    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(40.dp),
        shape = RoundedCornerShape(10.dp),
        color = backgroundColor
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon,
                null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}