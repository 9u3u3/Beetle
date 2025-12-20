package com.bsbeetle.bananaspaghetti.ui.browser

import android.content.res.Configuration
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.SessionList
import com.bsbeetle.bananaspaghetti.utils.webViewHandling.CustomWebView
import com.bsbeetle.bananaspaghetti.viewmodel.DevWebViewModel
import com.bsbeetle.bananaspaghetti.viewmodel.ViewportMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevBrowserScreen(
        modifier: Modifier = Modifier,
        webView: CustomWebView,
        viewModel: DevWebViewModel
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Local state for the text field to prevent typing lag, synced with VM
    var urlInput by rememberSaveable { mutableStateOf("") }
    var showDevTools by rememberSaveable { mutableStateOf(false) }

    // Initialize ViewModel with WebView
    LaunchedEffect(webView) {
        viewModel.setDevWebView(webView)
        if (webView.url.isNullOrBlank()) {
            viewModel.loadUrl("https://www.google.com")
        }
    }

    // Sync URL input with state (only when page finishes loading or changes externally)
    LaunchedEffect(uiState.url) {
        if (urlInput != uiState.url && !uiState.isLoading) {
            urlInput = uiState.url
        }
    }

    Column(
            modifier =
                    modifier.fillMaxSize()
                            .background(if (isLandscape) Color.Transparent else Color.Black)
    ) {
        // Conditional Top Bar based on Orientation
        if (isLandscape) {
            LandscapeTopBar(
                    urlInput = urlInput,
                    onUrlChange = { urlInput = it },
                    canGoBack = uiState.canGoBack,
                    canGoForward = uiState.canGoForward,
                    showDevTools = showDevTools,
                    viewportMode = uiState.viewportMode,
                    onBack = { viewModel.goBack() },
                    onForward = { viewModel.goForward() },
                    onRefresh = { viewModel.reload() },
                    onNavigate = { viewModel.loadUrl(urlInput) },
                    onToggleDevTools = {
                        showDevTools = !showDevTools
                        viewModel.toggleDevTools(showDevTools)
                    },
                    onToggleViewport = { viewModel.toggleViewportMode() }
            )
        } else {
            PortraitTopBar(
                    urlInput = urlInput,
                    onUrlChange = { urlInput = it },
                    canGoBack = uiState.canGoBack,
                    canGoForward = uiState.canGoForward,
                    showDevTools = showDevTools,
                    viewportMode = uiState.viewportMode,
                    onBack = { viewModel.goBack() },
                    onForward = { viewModel.goForward() },
                    onRefresh = { viewModel.reload() },
                    onNavigate = { viewModel.loadUrl(urlInput) },
                    onToggleDevTools = {
                        showDevTools = !showDevTools
                        viewModel.toggleDevTools(showDevTools)
                    },
                    onToggleViewport = { viewModel.toggleViewportMode() }
            )
        }

        // Progress indicator
        AnimatedVisibility(
                visible = uiState.isLoading,
                enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
        ) {
            LinearProgressIndicator(
                    progress = { uiState.loadProgress / 100f },
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = if (isLandscape) Color(0xFF007ACC) else Color(0xFF6366F1),
                    trackColor = Color(0xFF1A1A1A)
            )
        }

        // WebView
        AndroidView(
                modifier = Modifier.fillMaxWidth().weight(1f).background(Color.White),
                factory = { 
                    // Remove from existing parent if any (prevents crash)
                    (webView.parent as? android.view.ViewGroup)?.removeView(webView)
                    webView 
                },
                update = { viewModel.updateNavigation() }
        )
    }
}

@Composable
private fun LandscapeTopBar(
        urlInput: String,
        onUrlChange: (String) -> Unit,
        canGoBack: Boolean,
        canGoForward: Boolean,
        showDevTools: Boolean,
        viewportMode: ViewportMode,
        onBack: () -> Unit,
        onForward: () -> Unit,
        onRefresh: () -> Unit,
        onNavigate: () -> Unit,
        onToggleDevTools: () -> Unit,
        onToggleViewport: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth().height(38.dp).background(Color(0xFF1B1B1B))) {
        Row(
                modifier = Modifier.fillMaxWidth().height(38.dp).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CompactIconButton(onClick = onBack, enabled = canGoBack, icon = Icons.Default.ArrowBack)
            CompactIconButton(
                    onClick = onForward,
                    enabled = canGoForward,
                    icon = Icons.Default.ArrowForward
            )
            CompactIconButton(onClick = onRefresh, enabled = true, icon = Icons.Default.Refresh)

            // Using the specialized native input for custom keyboard support
            CompactTextFieldAlternative(
                    value = urlInput,
                    onValueChange = onUrlChange,
                    placeholder = "Search",
                    modifier = Modifier.weight(1f).height(28.dp),
                    onEnterPressed = { onNavigate() }
            )

            CompactIconButton(onClick = onNavigate, enabled = true, icon = Icons.Default.Search)

            // Viewport toggle button (mobile/desktop)
            CompactIconButton(
                    onClick = onToggleViewport,
                    enabled = true,
                    icon =
                            if (viewportMode == ViewportMode.MOBILE) Icons.Default.PhoneAndroid
                            else Icons.Default.Computer,
                    isActive = viewportMode == ViewportMode.DESKTOP
            )

            CompactIconButton(
                    onClick = onToggleDevTools,
                    enabled = true,
                    icon = Icons.Default.Build,
                    isActive = showDevTools
            )
        }
    }
}

@Composable
private fun PortraitTopBar(
        urlInput: String,
        onUrlChange: (String) -> Unit,
        canGoBack: Boolean,
        canGoForward: Boolean,
        showDevTools: Boolean,
        viewportMode: ViewportMode,
        onBack: () -> Unit,
        onForward: () -> Unit,
        onRefresh: () -> Unit,
        onNavigate: () -> Unit,
        onToggleDevTools: () -> Unit,
        onToggleViewport: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth().height(42.dp).background(Color(0xFF1B1B1B))) {
        Row(
                modifier = Modifier.fillMaxWidth().height(42.dp).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CompactIconButton(onClick = onBack, enabled = canGoBack, icon = Icons.Default.ArrowBack)
            CompactIconButton(
                    onClick = onForward,
                    enabled = canGoForward,
                    icon = Icons.Default.ArrowForward
            )
            CompactIconButton(onClick = onRefresh, enabled = true, icon = Icons.Default.Refresh)

            // Compact text field matching landscape style
            CompactTextFieldAlternative(
                    value = urlInput,
                    onValueChange = onUrlChange,
                    placeholder = "Search",
                    modifier = Modifier.weight(1f).height(28.dp),
                    onEnterPressed = { onNavigate() }
            )

            CompactIconButton(onClick = onNavigate, enabled = true, icon = Icons.Default.Search)

            // Viewport toggle button (mobile/desktop)
            CompactIconButton(
                    onClick = onToggleViewport,
                    enabled = true,
                    icon =
                            if (viewportMode == ViewportMode.MOBILE) Icons.Default.PhoneAndroid
                            else Icons.Default.Computer,
                    isActive = viewportMode == ViewportMode.DESKTOP
            )

            CompactIconButton(
                    onClick = onToggleDevTools,
                    enabled = true,
                    icon = Icons.Default.Build,
                    isActive = showDevTools
            )
        }
    }
}

@Composable
private fun CompactIconButton(
        onClick: () -> Unit,
        enabled: Boolean,
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        isActive: Boolean = false
) {
    val iconColor =
            when {
                isActive -> Color(0xFF007ACC)
                enabled -> Color(0xFFCCCCCC)
                else -> Color(0xFF6A6A6A)
            }

    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(20.dp)) {
        Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(16.dp)
        ) // Adjusted size to fit compact bar
    }
}

@Composable
private fun PortraitIconButton(
        onClick: () -> Unit,
        enabled: Boolean,
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        isActive: Boolean = false
) {
    val backgroundColor =
            when {
                isActive -> Color(0xFF6366F1)
                enabled -> Color(0xFF1A1A1A)
                else -> Color(0xFF0F0F0F)
            }

    val iconColor =
            when {
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
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
        }
    }
}

/** Native EditText wrapper to support the custom virtual keyboard input connection. */
@Composable
fun CompactTextFieldAlternative(
        value: String,
        onValueChange: (String) -> Unit,
        placeholder: String,
        modifier: Modifier = Modifier,
        onEnterPressed: () -> Unit
) {
    val editTextRef = remember { mutableStateOf<EditText?>(null) }
    // Safe access to session
    val activeSession =
            if (SessionList.sessions.isNotEmpty() && SessionList.activeSessionIndex != null) {
                SessionList.sessions.getOrNull(SessionList.activeSessionIndex!!)
            } else null

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
                    setSingleLine(true)

                    editTextRef.value = this

                    addTextChangedListener(
                            object : TextWatcher {
                                override fun beforeTextChanged(
                                        s: CharSequence?,
                                        start: Int,
                                        count: Int,
                                        after: Int
                                ) {}

                                override fun onTextChanged(
                                        s: CharSequence?,
                                        start: Int,
                                        before: Int,
                                        count: Int
                                ) {
                                    val txt = s?.toString() ?: ""

                                    // Check for newline from custom keyboard
                                    if (txt.endsWith("\n")) {
                                        val cleanedUrl = txt.trim()
                                        onValueChange(cleanedUrl) // Update state first
                                        onEnterPressed() // Trigger navigation
                                    } else {
                                        onValueChange(txt)
                                    }
                                }

                                override fun afterTextChanged(s: Editable?) {}
                            }
                    )

                    setOnFocusChangeListener { _, hasFocus ->
                        if (hasFocus && activeSession != null) {
                            post {
                                // Link input connection to the active session for the virtual
                                // keyboard
                                activeSession._inputConnection =
                                        onCreateInputConnection(EditorInfo())
                            }
                        }
                    }
                }
            },
            update = { editText ->
                if (editText.text.toString() != value) {
                    // Prevent cursor jumping if text is same
                    val selectionStart = editText.selectionStart
                    editText.setText(value)
                    try {
                        editText.setSelection(Math.min(selectionStart, value.length))
                    } catch (e: Exception) {
                        editText.setSelection(value.length)
                    }
                }

                // Re-establish connection if lost
                if (editText.isFocused &&
                                activeSession != null &&
                                activeSession._inputConnection == null
                ) {
                    editText.post {
                        activeSession._inputConnection =
                                editText.onCreateInputConnection(EditorInfo())
                    }
                }
            },
            modifier =
                    modifier.background(Color(0xFF2D2D30), RoundedCornerShape(4.dp)).fillMaxHeight()
    )
}
