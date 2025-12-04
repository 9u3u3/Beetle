package com.bsbeetle.bananaspaghetti.ui.browser

import android.text.Editable
import android.widget.EditText
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.bsbeetle.bananaspaghetti.utils.webViewHandling.CustomWebView
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.SessionList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevBrowserScreen(
    modifier: Modifier = Modifier,
    webView: CustomWebView
) {

    val homeUrl = "https://www.google.com"
    var urlInput by rememberSaveable { mutableStateOf("") }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var loadProgress by remember { mutableIntStateOf(0) }
    var showDevTools by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (webView.url.isNullOrBlank()) {
            webView.loadUrl(homeUrl)
        }
    }

    val updateNavigation: () -> Unit = {
        canGoBack = webView.canGoBack()
        canGoForward = webView.canGoForward()
        urlInput = webView.url ?: ""
    }

    DisposableEffect(webView) {
        val client = object : android.webkit.WebViewClient() {
            override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                isLoading = false
                updateNavigation()
            }

            override fun onPageStarted(view: android.webkit.WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                isLoading = true
                url?.let { urlInput = it }
            }

            override fun onRenderProcessGone(view: android.webkit.WebView?, detail: android.webkit.RenderProcessGoneDetail?): Boolean {
                android.util.Log.d("DevBrowserUI", "WebView Renderer Crashed")
                return true
            }
        }

        val chrome = object : android.webkit.WebChromeClient() {
            override fun onProgressChanged(view: android.webkit.WebView?, newProgress: Int) {
                loadProgress = newProgress
                isLoading = newProgress < 100
            }
        }

        webView.webViewClient = client
        webView.webChromeClient = chrome
        onDispose {}
    }

    val toggleDevTools = {
        showDevTools = !showDevTools
        webView.evaluateJavascript(
            if (showDevTools) "eruda.show();" else "eruda.hide();",
            null
        )
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
                .background(Color(0xFF1B1B1B))
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {

                CompactIconButton(
                    onClick = {
                        webView.goBack()
                        updateNavigation()
                    },
                    enabled = canGoBack,
                    icon = Icons.Default.ArrowBack
                )

                CompactIconButton(
                    onClick = {
                        webView.goForward()
                        updateNavigation()
                    },
                    enabled = canGoForward,
                    icon = Icons.Default.ArrowForward
                )

                CompactIconButton(
                    onClick = {
                        webView.reload()
                    },
                    enabled = true,
                    icon = Icons.Default.Refresh
                )

                CompactTextFieldAlternative(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    placeholder = "Search or enter URL",
                    modifier = Modifier
                        .weight(1f)
                        .height(18.dp),
                    onEnterPressed = { url -> webView.loadUrl(url) }
                )

                CompactIconButton(
                    onClick = {
                        webView.loadUrl(urlInput)
                    },
                    enabled = true,
                    icon = Icons.Default.Search
                )

                CompactIconButton(
                    onClick = toggleDevTools,
                    enabled = true,
                    icon = Icons.Default.Build,
                    isActive = showDevTools
                )
            }
        }

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

        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.White),

            factory = { webView },

            update = {
                updateNavigation()
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
fun CompactTextFieldAlternative(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onEnterPressed: (String) -> Unit
) {
    val editTextRef = remember { mutableStateOf<EditText?>(null) }
    val activeSession = SessionList.sessions[SessionList.activeSessionIndex!!]

    AndroidView(
        factory = { ctx ->
            EditText(ctx).apply {
                showSoftInputOnFocus = false
                setTextColor(android.graphics.Color.WHITE)
                textSize = 11f
                setBackgroundColor(android.graphics.Color.parseColor("#2D2D30"))
                setPadding(16, 0, 16, 0)
                hint = placeholder
                setHintTextColor(android.graphics.Color.parseColor("#6A6A6A"))
                setText(value)

                editTextRef.value = this

                addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        val txt = s?.toString() ?: ""
                        onValueChange(txt)

                        // 🔥 Detect Enter from custom keyboard
                        if (txt.endsWith("\n")) {
                            val cleanedUrl = txt.trim()
                            onEnterPressed(cleanedUrl)
                        }
                    }
                    override fun afterTextChanged(s: Editable?) {}
                })

                setOnFocusChangeListener { _, hasFocus ->
                    if (hasFocus) {
                        post {
                            activeSession._inputConnection =
                                onCreateInputConnection(EditorInfo())
                        }
                    }
                }
            }
        },

        update = { editText ->
            if (editText.text.toString() != value) {
                editText.setText(value)
                editText.setSelection(editText.text.length)
            }
            if (editText.isFocused && activeSession._inputConnection == null) {
                editText.post {
                    activeSession._inputConnection =
                        editText.onCreateInputConnection(EditorInfo())
                }
            }
        },

        modifier = modifier
            .background(Color(0xFF2D2D30), RoundedCornerShape(4.dp))
            .fillMaxHeight()
    )
}