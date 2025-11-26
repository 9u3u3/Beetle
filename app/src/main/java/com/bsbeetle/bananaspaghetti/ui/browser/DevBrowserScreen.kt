package com.bsbeetle.bananaspaghetti.ui.browser

import android.content.res.Configuration
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bsbeetle.bananaspaghetti.utils.webViewHandling.CustomWebView
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.SessionList
import com.bsbeetle.bananaspaghetti.viewmodel.DevWebViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevBrowserScreen(
    modifier: Modifier = Modifier,
    webView: CustomWebView,
    viewModel: DevWebViewModel // Add ViewModel parameter
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var urlInput by rememberSaveable { mutableStateOf("") }
    var showDevTools by rememberSaveable { mutableStateOf(false) }

    // Initialize ViewModel with WebView
    LaunchedEffect(webView) {
        viewModel.setDevWebView(webView)
        if (webView.url.isNullOrBlank()) {
            viewModel.loadUrl("https://www.google.com")
        }
    }

    // Sync URL input with state
    LaunchedEffect(uiState.url) {
        if (urlInput != uiState.url) {
            urlInput = uiState.url
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(if (isLandscape) Color.Transparent else Color.Black)
    ) {
        // Conditional Top Bar
        if (isLandscape) {
            LandscapeTopBar(
                urlInput = urlInput,
                onUrlChange = { urlInput = it },
                canGoBack = uiState.canGoBack,
                canGoForward = uiState.canGoForward,
                showDevTools = showDevTools,
                onBack = { viewModel.goBack() },
                onForward = { viewModel.goForward() },
                onRefresh = { viewModel.reload() },
                onNavigate = { viewModel.loadUrl(urlInput) },
                onToggleDevTools = {
                    showDevTools = !showDevTools
                    viewModel.toggleDevTools(showDevTools)
                }
            )
        } else {
            PortraitTopBar(
                urlInput = urlInput,
                onUrlChange = { urlInput = it },
                canGoBack = uiState.canGoBack,
                canGoForward = uiState.canGoForward,
                showDevTools = showDevTools,
                onBack = { viewModel.goBack() },
                onForward = { viewModel.goForward() },
                onRefresh = { viewModel.reload() },
                onNavigate = { viewModel.loadUrl(urlInput) },
                onToggleDevTools = {
                    showDevTools = !showDevTools
                    viewModel.toggleDevTools(showDevTools)
                }
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = if (isLandscape) Color(0xFF007ACC) else Color(0xFF6366F1),
                trackColor = Color(0xFF1A1A1A)
            )
        }

        // WebView
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.White),
            factory = { webView },
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
    onBack: () -> Unit,
    onForward: () -> Unit,
    onRefresh: () -> Unit,
    onNavigate: () -> Unit,
    onToggleDevTools: () -> Unit
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
            CompactIconButton(onClick = onBack, enabled = canGoBack, icon = Icons.Default.ArrowBack)
            CompactIconButton(onClick = onForward, enabled = canGoForward, icon = Icons.Default.ArrowForward)
            CompactIconButton(onClick = onRefresh, enabled = true, icon = Icons.Default.Refresh)

            CompactTextFieldAlternative(
                value = urlInput,
                onValueChange = onUrlChange,
                placeholder = "Search or enter URL",
                modifier = Modifier.weight(1f).height(18.dp),
                onEnterPressed = { onNavigate() }
            )

            CompactIconButton(onClick = onNavigate, enabled = true, icon = Icons.Default.Search)
            CompactIconButton(onClick = onToggleDevTools, enabled = true, icon = Icons.Default.Build, isActive = showDevTools)
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
    onBack: () -> Unit,
    onForward: () -> Unit,
    onRefresh: () -> Unit,
    onNavigate: () -> Unit,
    onToggleDevTools: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0A0A0A))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PortraitIconButton(onClick = onBack, enabled = canGoBack, icon = Icons.Default.ArrowBack)
        PortraitIconButton(onClick = onForward, enabled = canGoForward, icon = Icons.Default.ArrowForward)
        PortraitIconButton(onClick = onRefresh, enabled = true, icon = Icons.Default.Refresh)

        OutlinedTextField(
            value = urlInput,
            onValueChange = onUrlChange,
            modifier = Modifier.weight(1f).heightIn(min = 52.dp),
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
            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, fontWeight = FontWeight.Normal),
            placeholder = { Text("Search or enter URL", color = Color(0xFF666666), fontSize = 14.sp) },
            trailingIcon = {
                Box(modifier = Modifier.size(28.dp).padding(end = 4.dp), contentAlignment = Alignment.Center) {
                    IconButton(onClick = onNavigate, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.ArrowForward, null, tint = Color(0xFFBBBBBB), modifier = Modifier.size(16.dp))
                    }
                }
            }
        )

        PortraitIconButton(onClick = onToggleDevTools, enabled = true, icon = Icons.Default.Build, isActive = showDevTools)
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

    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(20.dp)) {
        Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(12.dp))
    }
}

@Composable
private fun PortraitIconButton(
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

    Surface(onClick = onClick, enabled = enabled, modifier = Modifier.size(40.dp), shape = RoundedCornerShape(10.dp), color = backgroundColor) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun CompactTextFieldAlternative(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onEnterPressed: () -> Unit
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
                        if (txt.endsWith("\n")) {
                            onEnterPressed()
                        }
                    }
                    override fun afterTextChanged(s: Editable?) {}
                })

                setOnFocusChangeListener { _, hasFocus ->
                    if (hasFocus) {
                        post {
                            activeSession._inputConnection = onCreateInputConnection(EditorInfo())
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
                    activeSession._inputConnection = editText.onCreateInputConnection(EditorInfo())
                }
            }
        },
        modifier = modifier.background(Color(0xFF2D2D30), RoundedCornerShape(4.dp)).fillMaxHeight()
    )
}