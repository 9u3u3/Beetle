package com.bsbeetle.bananaspaghetti.ui.browser

import android.webkit.WebView
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevBrowserScreen(modifier: Modifier = Modifier) {
    var url by rememberSaveable { mutableStateOf("https://github.com/harshf40") }
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
        modifier = modifier
            .fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
                .background(Color(0xFF1B1B1B))
        ) {

            // LEFT edge divider
            Box(
                Modifier
                    .align(Alignment.CenterStart)   // ✔ Correct here
                    .fillMaxHeight()
                    .width(1.dp)
                    .background(Color(0xFF3E3E42))
            )

            // RIGHT edge divider
            Box(
                Modifier
                    .align(Alignment.CenterEnd)     // ✔ Correct here
                    .fillMaxHeight()
                    .width(1.dp)
                    .background(Color(0xFF3E3E42))
            )

            // Compact top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {

                // Back button
                CompactIconButton(
                    onClick = { webView?.goBack(); updateNavigation() },
                    enabled = canGoBack,
                    icon = Icons.Default.ArrowBack
                )

                // Forward button
                CompactIconButton(
                    onClick = { webView?.goForward(); updateNavigation() },
                    enabled = canGoForward,
                    icon = Icons.Default.ArrowForward
                )

                // Refresh button
                CompactIconButton(
                    onClick = { webView?.reload() },
                    enabled = true,
                    icon = Icons.Default.Refresh
                )

                CompactTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    placeholder = "Search or enter URL",
                    modifier = Modifier
                        .weight(1f)
                        .height(18.dp)
                )

                // Dev Tools button
                CompactIconButton(
                    onClick = toggleDevTools,
                    enabled = true,
                    icon = Icons.Default.Build,
                    isActive = showDevTools
                )
            }

        }



        // Progress indicator
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
                color = Color(0xFF007ACC),
                trackColor = Color(0xFF1A1A1A)
            )
        }

        // WebView
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
    val iconColor = when {
        isActive -> Color(0xFF007ACC)
        enabled -> Color(0xFFCCCCCC)
        else -> Color(0xFF6A6A6A)
    }

    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(20.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(12.dp)
        )
    }
}

@Composable
private fun CompactTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        cursorBrush = SolidColor(Color.White),
        textStyle = TextStyle(
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 11.sp   // <-- Centers text vertically
        ),
        modifier = modifier
            .background(Color(0xFF2D2D30), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 0.dp)  // <-- No vertical padding
            .fillMaxHeight(),  // takes full 18dp height
        decorationBox = { inner ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.CenterStart  // <-- Perfect vertical center
            ) {
                if (value.isEmpty()) {
                    Text(
                        placeholder,
                        fontSize = 11.sp,
                        color = Color(0xFF6A6A6A),
                        lineHeight = 11.sp
                    )
                }
                inner()
            }
        }
    )
}