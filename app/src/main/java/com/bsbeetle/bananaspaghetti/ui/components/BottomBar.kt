package com.bsbeetle.bananaspaghetti.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bsbeetle.bananaspaghetti.R
import com.bsbeetle.bananaspaghetti.directGit.viewmodel.GitViewModel
import com.bsbeetle.bananaspaghetti.ui.theme.codaRegular
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.SessionList
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.TerminalSession
import com.bsbeetle.bananaspaghetti.viewmodel.DiagnosticItem
import com.bsbeetle.bananaspaghetti.viewmodel.LspViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class BottomPanelTab {
        PROBLEMS,
        TERMINAL,
        GIT_CONSOLE
}

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun BottomBar(
        modifier: Modifier = Modifier,
        color: Color,
        lspViewModel: LspViewModel = viewModel(),
        gitViewModel: GitViewModel = viewModel(),
        editorViewModel: com.bsbeetle.bananaspaghetti.viewmodel.EditorViewModel? = null,
        expandToHalf: Boolean = false,
        onExpandHandled: () -> Unit = {}
) {

        var targetHeight by remember { mutableStateOf(18.dp) }
        val logHeight by animateDpAsState(targetValue = targetHeight, label = "logHeightAnim")
        val density = LocalDensity.current
        val screenHeight = LocalConfiguration.current.screenHeightDp.dp
        val context = LocalContext.current

        val lspStatus by lspViewModel.lspStatus.collectAsStateWithLifecycle()

        // Tab state
        var selectedTab by remember { mutableStateOf(BottomPanelTab.PROBLEMS) }
        
        // Handle external request to expand to half screen and switch to terminal
        androidx.compose.runtime.LaunchedEffect(expandToHalf) {
                if (expandToHalf) {
                        targetHeight = screenHeight * 0.5f
                        selectedTab = BottomPanelTab.TERMINAL
                        onExpandHandled()
                }
        }

        // Get current editor session and use its linked terminals
        // Use fallback logic: first try activeSessionIndex, then find any EditorSession
        val currentEditorSession = remember(SessionList.activeSessionIndex, SessionList.sessions.size) {
                val sessionAtIndex = SessionList.sessions.getOrNull(SessionList.activeSessionIndex ?: -1) as?
                        com.bsbeetle.bananaspaghetti.utils.sessionHandling.EditorSession
                // Fallback: find any EditorSession if activeSessionIndex doesn't point to one
                sessionAtIndex ?: SessionList.sessions.filterIsInstance<com.bsbeetle.bananaspaghetti.utils.sessionHandling.EditorSession>().lastOrNull()
        }

        // Terminal sessions for bottom panel - use EditorSession's linked terminals if available
        // This ensures terminals persist when navigating away and back
        val panelTerminals =
                currentEditorSession?.linkedTerminals
                        ?: remember { mutableStateListOf<TerminalSession>() }
        var activeTerminalIndex by remember { mutableIntStateOf(0) }

        // Coroutine scope for async terminal creation
        val scope = rememberCoroutineScope()

        // Update editor bottom padding when bottom bar height changes
        // This allows scrolling past the last line when bottom bar covers it
        androidx.compose.runtime.LaunchedEffect(logHeight) {
                val paddingPx = with(density) { logHeight.toPx().toInt() }
                Log.d(
                        "BottomBar",
                        "Bottom bar height changed: ${logHeight}, paddingPx: $paddingPx, webView: ${editorViewModel?.webView}"
                )
                editorViewModel?.webView?.let { webView ->
                        val js =
                                "if(window.setBottomPadding) { window.setBottomPadding($paddingPx); console.log('[Editor] setBottomPadding called with $paddingPx'); } else { console.log('[Editor] setBottomPadding not available yet'); }"
                        webView.evaluateJavascript(js, null)
                        Log.d("BottomBar", "Executed JS for bottom padding: $paddingPx")
                }
                        ?: Log.w("BottomBar", "webView is null, cannot set bottom padding")
        }

        Box(modifier = modifier.height(logHeight)) {
                Column {
                        // Status bar header - 18dp (UNCHANGED)
                        Box(
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .height(18.dp)
                                                .background(color)
                                                .draggable(
                                                        orientation = Orientation.Vertical,
                                                        state =
                                                                rememberDraggableState { delta ->
                                                                        targetHeight =
                                                                                (targetHeight +
                                                                                                with(
                                                                                                        density
                                                                                                ) {
                                                                                                        -delta.toDp()
                                                                                                })
                                                                                        .coerceIn(
                                                                                                18.dp,
                                                                                                screenHeight
                                                                                        )
                                                                }
                                                )
                        ) {
                                Row(
                                        modifier = Modifier.padding(start = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                ) {
                                        Text(
                                                text =
                                                        "Ln ${lspStatus.line}, Col ${lspStatus.column}",
                                                fontFamily = codaRegular,
                                                fontSize = 9.sp,
                                                color = Color(0xFFFFFFFF)
                                        )

                                        Spacer(modifier = Modifier.width(8.dp))

                                        if (lspStatus.breadcrumbs.isNotEmpty()) {
                                                lspStatus.breadcrumbs.forEachIndexed { index, item
                                                        ->
                                                        val icon =
                                                                when (item.kind) {
                                                                        5, 23 ->
                                                                                R.drawable
                                                                                        .class_cube // Class, Struct
                                                                        6, 12 ->
                                                                                R.drawable
                                                                                        .data_object // Method, Function
                                                                        else ->
                                                                                R.drawable
                                                                                        .data_object
                                                                }

                                                        Image(
                                                                painter = painterResource(icon),
                                                                contentDescription = "Symbol",
                                                                modifier = Modifier.size(9.dp),
                                                                alignment = Alignment.Center
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                                text = item.name,
                                                                fontFamily = codaRegular,
                                                                fontSize = 9.sp,
                                                                color = Color(0xFFFFFFFF)
                                                        )

                                                        if (index < lspStatus.breadcrumbs.size - 1
                                                        ) {
                                                                Spacer(
                                                                        modifier =
                                                                                Modifier.width(4.dp)
                                                                )
                                                                Image(
                                                                        painter =
                                                                                painterResource(
                                                                                        R.drawable
                                                                                                .chevron_right
                                                                                ),
                                                                        contentDescription = ">",
                                                                        modifier =
                                                                                Modifier.size(9.dp),
                                                                        alignment = Alignment.Center
                                                                )
                                                                Spacer(
                                                                        modifier =
                                                                                Modifier.width(4.dp)
                                                                )
                                                        }
                                                }
                                        } else if (lspStatus.currentSymbol.isNotEmpty()) {
                                                // Fallback to old single symbol if no breadcrumbs
                                                // (e.g. older LSP)
                                                Image(
                                                        painter =
                                                                painterResource(
                                                                        R.drawable.class_cube
                                                                ),
                                                        contentDescription = "Symbol",
                                                        modifier = Modifier.size(9.dp),
                                                        alignment = Alignment.Center
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                        text = lspStatus.currentSymbol,
                                                        fontFamily = codaRegular,
                                                        fontSize = 9.sp,
                                                        color = Color(0xFFFFFFFF)
                                                )
                                        }

                                        Spacer(modifier = Modifier.weight(1f))

                                        // Center Swipe Up Icon
                                        Image(
                                                painter = painterResource(R.drawable.swipe_up),
                                                contentDescription = "Swipe Up",
                                                modifier = Modifier.size(12.dp),
                                                alignment = Alignment.Center
                                        )

                                        Spacer(modifier = Modifier.weight(1f))

                                        Image(
                                                painter = painterResource(R.drawable.warning_icon),
                                                contentDescription = "warning",
                                                modifier = Modifier.size(9.dp),
                                                alignment = Alignment.Center
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                                text = lspStatus.warningCount.toString(),
                                                fontFamily = codaRegular,
                                                fontSize = 9.sp,
                                                color = Color(0xFFFFFFFF)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Image(
                                                painter = painterResource(R.drawable.error_icon),
                                                contentDescription = "error",
                                                modifier = Modifier.size(9.dp),
                                                alignment = Alignment.Center
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                                text = lspStatus.errorCount.toString(),
                                                fontFamily = codaRegular,
                                                fontSize = 9.sp,
                                                color = Color(0xFFFFFFFF)
                                        )
                                }
                        }

                        // Tab bar - 24dp
                        Row(
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .height(24.dp)
                                                .background(Color(0xFF1E1E1E)),
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                // Problems tab
                                PanelTab(
                                        title = "Problems",
                                        count = lspStatus.diagnostics.size,
                                        isSelected = selectedTab == BottomPanelTab.PROBLEMS,
                                        onClick = { selectedTab = BottomPanelTab.PROBLEMS }
                                )

                                // Terminal tab
                                PanelTab(
                                        title = "Terminal",
                                        count = panelTerminals.size,
                                        isSelected = selectedTab == BottomPanelTab.TERMINAL,
                                        onClick = { selectedTab = BottomPanelTab.TERMINAL }
                                )

                                // Git Console tab
                                PanelTab(
                                        title = "Git Console",
                                        count =
                                                gitViewModel.uiState.collectAsState()
                                                        .value
                                                        .terminalHistory
                                                        .size,
                                        isSelected = selectedTab == BottomPanelTab.GIT_CONSOLE,
                                        onClick = { selectedTab = BottomPanelTab.GIT_CONSOLE }
                                )

                                Spacer(modifier = Modifier.weight(1f))

                                // Add terminal button (only show when on Terminal tab)
                                if (selectedTab == BottomPanelTab.TERMINAL) {
                                        val fileViewModel =
                                                com.bsbeetle.bananaspaghetti.LocalFileViewModel
                                                        .current
                                        Text(
                                                text = "+",
                                                color = Color(0xFFCCCCCC),
                                                fontSize = 16.sp,
                                                modifier =
                                                        Modifier.padding(horizontal = 8.dp)
                                                                .clickable {
                                                                        // Get the opened folder's
                                                                        // actual path
                                                                        val folderPath =
                                                                                fileViewModel
                                                                                        .selectedFolderUri
                                                                                        ?.let { uri
                                                                                                ->
                                                                                                extractPathFromUri(
                                                                                                        context,
                                                                                                        uri
                                                                                                )
                                                                                        }

                                                                        // Fallback to file's parent
                                                                        // directory if no folder is
                                                                        // open
                                                                        val currentPath =
                                                                                folderPath
                                                                                        ?: fileViewModel
                                                                                                .selectedUri
                                                                                                ?.let {
                                                                                                        uri
                                                                                                        ->
                                                                                                        extractPathFromUri(
                                                                                                                        context,
                                                                                                                        uri
                                                                                                                )
                                                                                                                ?.substringBeforeLast(
                                                                                                                        "/"
                                                                                                                )
                                                                                                }
                                                                                                ?: (SessionList
                                                                                                        .sessions
                                                                                                        .getOrNull(
                                                                                                                SessionList
                                                                                                                        .activeSessionIndex
                                                                                                                        ?: -1
                                                                                                        ) as?
                                                                                                        com.bsbeetle.bananaspaghetti.utils.sessionHandling.EditorSession)
                                                                                                ?.projectRoot
                                                                                                ?.uri
                                                                                                ?.let {
                                                                                                        uri
                                                                                                        ->
                                                                                                        extractPathFromUri(
                                                                                                                context,
                                                                                                                uri
                                                                                                        )
                                                                                                }

                                                                        val newTerminal =
                                                                                TerminalSession(
                                                                                        context.applicationContext,
                                                                                        "bash ${panelTerminals.size + 1}",
                                                                                        initialDir =
                                                                                                currentPath
                                                                                )
                                                                        // Add to list but DON'T
                                                                        // switch to it yet
                                                                        // This lets it load in the
                                                                        // "background" while
                                                                        // current view
                                                                        // stays
                                                                        val newIndex =
                                                                                panelTerminals.size
                                                                        panelTerminals.add(
                                                                                newTerminal
                                                                        )

                                                                        // Wait for terminal to be
                                                                        // ready, then switch to it
                                                                        scope.launch {
                                                                                newTerminal
                                                                                        .getOrInitView()
                                                                                newTerminal.isReady
                                                                                        .first {
                                                                                                ready
                                                                                                ->
                                                                                                ready
                                                                                        }
                                                                                // Now safe to
                                                                                // switch - terminal
                                                                                // is fully loaded
                                                                                activeTerminalIndex =
                                                                                        newIndex
                                                                        }
                                                                }
                                        )
                                }

                                // Clear button for Git Console tab
                                if (selectedTab == BottomPanelTab.GIT_CONSOLE) {
                                        Text(
                                                text = "Clear",
                                                color = Color(0xFFCCCCCC),
                                                fontSize = 11.sp,
                                                modifier =
                                                        Modifier.padding(horizontal = 8.dp)
                                                                .clickable {
                                                                        gitViewModel.clearMessages()
                                                                }
                                        )
                                }
                        }

                        // Content area
                        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF050505))) {
                                when (selectedTab) {
                                        BottomPanelTab.PROBLEMS -> {
                                                ProblemsContent(
                                                        diagnostics = lspStatus.diagnostics,
                                                        onDiagnosticClick = { diag ->
                                                                // TODO: Navigate to line in editor
                                                        }
                                                )
                                        }
                                        BottomPanelTab.TERMINAL -> {
                                                TerminalContent(
                                                        terminals = panelTerminals,
                                                        activeIndex = activeTerminalIndex,
                                                        onTabClick = { index ->
                                                                activeTerminalIndex = index
                                                        },
                                                        onClose = { index ->
                                                                if (index < panelTerminals.size) {
                                                                        panelTerminals[index]
                                                                                .destroy()
                                                                        panelTerminals.removeAt(
                                                                                index
                                                                        )
                                                                        if (activeTerminalIndex >=
                                                                                        panelTerminals
                                                                                                .size
                                                                        ) {
                                                                                activeTerminalIndex =
                                                                                        (panelTerminals
                                                                                                        .size -
                                                                                                        1)
                                                                                                .coerceAtLeast(
                                                                                                        0
                                                                                                )
                                                                        }
                                                                }
                                                        },
                                                        context = context
                                                )
                                        }
                                        BottomPanelTab.GIT_CONSOLE -> {
                                                // Git Console - show terminal command history
                                                val gitUiState by
                                                        gitViewModel.uiState
                                                                .collectAsStateWithLifecycle()
                                                GitConsoleContent(
                                                        terminalHistory =
                                                                gitUiState.terminalHistory,
                                                        onClear = { gitViewModel.clearMessages() }
                                                )
                                        }
                                }
                        }
                }
        }
}

@Composable
private fun PanelTab(title: String, count: Int, isSelected: Boolean, onClick: () -> Unit) {
        val bgColor = if (isSelected) Color(0xFF050505) else Color.Transparent
        val textColor = if (isSelected) Color.White else Color(0xFF888888)
        val bottomBorder = if (isSelected) Color(0xFF007ACC) else Color.Transparent

        Column(
                modifier =
                        Modifier.clickable(onClick = onClick)
                                .background(bgColor)
                                .padding(horizontal = 12.dp)
                                .fillMaxHeight(),
                verticalArrangement = Arrangement.Center
        ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                                text = title,
                                color = textColor,
                                fontSize = 11.sp,
                                fontWeight =
                                        if (isSelected) FontWeight.Medium else FontWeight.Normal
                        )
                        if (count > 0) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                        text = "($count)",
                                        color = textColor.copy(alpha = 0.7f),
                                        fontSize = 10.sp
                                )
                        }
                }
                // Bottom accent line
                Box(modifier = Modifier.width(40.dp).height(2.dp).background(bottomBorder))
        }
}

@Composable
private fun ProblemsContent(
        diagnostics: List<DiagnosticItem>,
        onDiagnosticClick: (DiagnosticItem) -> Unit
) {
        Column(
                modifier =
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(8.dp)
        ) {
                if (diagnostics.isEmpty()) {
                        Text(text = "No problems", color = Color(0xFF50fa7b), fontSize = 12.sp)
                } else {
                        diagnostics.forEach { diag ->
                                val iconColor =
                                        if (diag.severity == "error") Color(0xFFff5555)
                                        else Color(0xFFffb86c)
                                val icon = if (diag.severity == "error") "✗" else "⚠"

                                Row(
                                        modifier =
                                                Modifier.fillMaxWidth()
                                                        .clickable { onDiagnosticClick(diag) }
                                                        .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.Top
                                ) {
                                        Text(text = icon, color = iconColor, fontSize = 11.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                                Text(
                                                        text = diag.message,
                                                        color = Color.White,
                                                        fontSize = 11.sp
                                                )
                                                Text(
                                                        text =
                                                                "Line ${diag.line}, Column ${diag.column}",
                                                        color = Color(0xFF888888),
                                                        fontSize = 9.sp
                                                )
                                        }
                                }
                        }
                }
        }
}

@Composable
private fun GitConsoleContent(terminalHistory: List<String>, onClear: () -> Unit) {
        Column(
                modifier =
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(8.dp)
        ) {
                if (terminalHistory.isEmpty()) {
                        Text(
                                text = "No git commands executed yet",
                                color = Color(0xFF888888),
                                fontSize = 12.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                } else {
                        terminalHistory.forEach { line ->
                                val (color, displayText) =
                                        when {
                                                line.startsWith("\$ ") ->
                                                        Color(0xFF61AFEF) to
                                                                line // Command - cyan/blue
                                                line.startsWith("✅") ->
                                                        Color(0xFF98C379) to line // Success - green
                                                line.startsWith("❌") ->
                                                        Color(0xFFE06C75) to line // Error - red
                                                line.startsWith("📦") ||
                                                        line.startsWith("🔑") ||
                                                        line.startsWith("🔧") ->
                                                        Color(0xFFE5C07B) to
                                                                line // Info/setup - yellow
                                                line.startsWith("⏳") ->
                                                        Color(0xFF888888) to line // Waiting - gray
                                                else ->
                                                        Color(0xFFABB2BF) to
                                                                line // Default output - light gray
                                        }

                                Text(
                                        text = displayText,
                                        color = color,
                                        fontSize = 11.sp,
                                        fontFamily =
                                                androidx.compose.ui.text.font.FontFamily.Monospace,
                                        modifier = Modifier.padding(vertical = 1.dp)
                                )
                        }
                }
        }
}

@Composable
private fun TerminalContent(
        terminals: List<TerminalSession>,
        activeIndex: Int,
        onTabClick: (Int) -> Unit,
        onClose: (Int) -> Unit,
        context: Context
) {
        Column(modifier = Modifier.fillMaxSize()) {
                // Terminal tabs row - horizontally scrollable
                if (terminals.isNotEmpty()) {
                        Row(
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .height(26.dp)
                                                .background(Color(0xFF252526))
                                                .horizontalScroll(rememberScrollState()),
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                terminals.forEachIndexed { index, terminal ->
                                        val isActive = index == activeIndex
                                        Row(
                                                modifier =
                                                        Modifier.clickable { onTabClick(index) }
                                                                .background(
                                                                        if (isActive)
                                                                                Color(0xFF1E1E1E)
                                                                        else Color.Transparent
                                                                )
                                                                .padding(
                                                                        horizontal = 10.dp,
                                                                        vertical = 5.dp
                                                                ),
                                                verticalAlignment = Alignment.CenterVertically
                                        ) {
                                                // Terminal icon
                                                Image(
                                                        painter =
                                                                painterResource(
                                                                        R.drawable
                                                                                .terminal_24dp_e3e3e3_fill0_wght400_grad0_opsz24
                                                                ),
                                                        contentDescription = "Terminal",
                                                        modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                        text = terminal.name,
                                                        color =
                                                                if (isActive) Color.White
                                                                else Color(0xFFAAAAAA),
                                                        fontSize = 11.sp
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                // Close button
                                                Box(
                                                        modifier =
                                                                Modifier.size(16.dp)
                                                                        .background(
                                                                                color =
                                                                                        Color.Transparent,
                                                                                shape =
                                                                                        androidx.compose
                                                                                                .foundation
                                                                                                .shape
                                                                                                .CircleShape
                                                                        )
                                                                        .clickable {
                                                                                onClose(index)
                                                                        },
                                                        contentAlignment = Alignment.Center
                                                ) {
                                                        Text(
                                                                text = "×",
                                                                color = Color(0xFF888888),
                                                                fontSize = 14.sp
                                                        )
                                                }
                                        }
                                        // Separator between tabs
                                        if (index < terminals.size - 1) {
                                                Box(
                                                        modifier =
                                                                Modifier.width(1.dp)
                                                                        .height(16.dp)
                                                                        .background(
                                                                                Color(0xFF3C3C3C)
                                                                        )
                                                )
                                        }
                                }
                        }
                }

                // Terminal WebView - use key() to force recreation for each terminal
                if (terminals.isNotEmpty() && activeIndex < terminals.size) {
                        val activeTerminal = terminals[activeIndex]
                        val isTerminalReady by activeTerminal.isReady.collectAsStateWithLifecycle()

                        // Key by terminal ID to ensure proper recreation
                        key(activeTerminal.id) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                        // Loading overlay - shown FIRST, under the WebView layer
                                        // but visible when
                                        // WebView is transparent
                                        if (!isTerminalReady) {
                                                Box(
                                                        modifier =
                                                                Modifier.fillMaxSize()
                                                                        .background(
                                                                                Color(0xFF1E1E1E)
                                                                        ),
                                                        contentAlignment = Alignment.Center
                                                ) {
                                                        Column(
                                                                horizontalAlignment =
                                                                        Alignment
                                                                                .CenterHorizontally,
                                                                verticalArrangement =
                                                                        Arrangement.Center
                                                        ) {
                                                                androidx.compose.material3
                                                                        .CircularProgressIndicator(
                                                                                modifier =
                                                                                        Modifier.size(
                                                                                                32.dp
                                                                                        ),
                                                                                color =
                                                                                        Color(
                                                                                                0xFF007ACC
                                                                                        ),
                                                                                strokeWidth = 2.dp
                                                                        )
                                                                Spacer(
                                                                        modifier =
                                                                                Modifier.height(
                                                                                        12.dp
                                                                                )
                                                                )
                                                                Text(
                                                                        text =
                                                                                "Initializing terminal...",
                                                                        color = Color(0xFFAAAAAA),
                                                                        fontSize = 12.sp
                                                                )
                                                        }
                                                }
                                        }

                                        // WebView - invisible until ready (alpha = 0 prevents any
                                        // flash)
                                        AndroidView(
                                                factory = { ctx -> activeTerminal.getOrInitView() },
                                                modifier =
                                                        Modifier.fillMaxSize()
                                                                .alpha(
                                                                        if (isTerminalReady) 1f
                                                                        else 0f
                                                                )
                                                                .clickable {
                                                                        // Set as active session
                                                                        // when clicked
                                                                        SessionList
                                                                                .activeSessionIndex =
                                                                                SessionList.sessions
                                                                                        .indexOf(
                                                                                                activeTerminal
                                                                                        )
                                                                },
                                                update = { webView ->
                                                        // Make native WebView visible when ready
                                                        if (isTerminalReady) {
                                                                webView.visibility =
                                                                        android.view.View.VISIBLE
                                                                webView.alpha = 1f
                                                        }
                                                }
                                        )
                                }
                        }
                } else {
                        // Empty state - prompt to create terminal
                        Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                        ) {
                                Text(
                                        text = "Click + to open a terminal",
                                        color = Color(0xFF888888),
                                        fontSize = 12.sp
                                )
                        }
                }
        }
}

/** Convert a content URI to a real file path. Works with SAF (Storage Access Framework) URIs. */
private fun extractPathFromUri(context: Context, uri: android.net.Uri): String? {
        // If it's already a file path
        if (uri.scheme == "file") {
                return uri.path
        }

        // For content URIs, try to extract the path from the document ID
        if (uri.scheme == "content") {
                val docId =
                        try {
                                android.provider.DocumentsContract.getDocumentId(uri)
                        } catch (e: Exception) {
                                try {
                                        android.provider.DocumentsContract.getTreeDocumentId(uri)
                                } catch (e2: Exception) {
                                        null
                                }
                        }

                if (docId != null) {
                        val split = docId.split(":")
                        if (split.size >= 2) {
                                val type = split[0]
                                val path = split.subList(1, split.size).joinToString(":")
                                return when {
                                        type == "primary" -> "/storage/emulated/0/$path"
                                        type == "raw" -> path
                                        type.startsWith("/") -> docId
                                        else -> "/storage/$type/$path"
                                }
                        } else if (docId.startsWith("/")) {
                                return docId
                        }
                }

                uri.path?.let { path ->
                        val primaryMatch = Regex("primary:(.+)").find(path)
                        if (primaryMatch != null) {
                                return "/storage/emulated/0/${primaryMatch.groupValues[1]}"
                        }
                }
        }

        return null
}
