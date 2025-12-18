package com.bsbeetle.bananaspaghetti.ui.components

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bsbeetle.bananaspaghetti.utils.webViewHandling.CustomWebView
import kotlinx.coroutines.delay

// Premium color palette
private val SearchBarBackground = Color(0xFF1E1E2E)
private val SearchInputBackground = Color(0xFF2D2D3D)
private val SearchInputBorder = Color(0xFF3D3D4D)
private val SearchInputBorderFocused = Color(0xFF7C3AED)
private val AccentPurple = Color(0xFF7C3AED)
private val AccentPurpleLight = Color(0xFFA78BFA)
private val TextPrimary = Color(0xFFE4E4E7)
private val TextSecondary = Color(0xFF71717A)
private val SuccessGreen = Color(0xFF22C55E)
private val WarningOrange = Color(0xFFF59E0B)

/** Premium VS Code-style search and replace bar for the editor */
@Composable
fun SearchReplaceBar(
        visible: Boolean,
        webView: CustomWebView?,
        onClose: () -> Unit,
        modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var searchQuery by remember { mutableStateOf("") }
    var replaceText by remember { mutableStateOf("") }
    var showReplace by remember { mutableStateOf(false) }
    var matchCase by remember { mutableStateOf(false) }
    var wholeWord by remember { mutableStateOf(false) }
    var useRegex by remember { mutableStateOf(false) }
    var matchCount by remember { mutableStateOf(0) }
    var currentMatch by remember { mutableStateOf(0) }
    var isSearchFocused by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }

    // Request focus when search bar becomes visible
    LaunchedEffect(visible) {
        if (visible) {
            delay(100)
            try {
                focusRequester.requestFocus()
                isSearchFocused = true
            } catch (e: Exception) {
                // Focus may fail if component not ready
            }
        } else {
            searchQuery = ""
            replaceText = ""
            matchCount = 0
            currentMatch = 0
            webView?.evaluateJavascript("if(window.clearSearch) window.clearSearch();", null)
        }
    }

    // Perform search when query changes
    LaunchedEffect(searchQuery, matchCase, wholeWord, useRegex) {
        if (searchQuery.isNotEmpty() && webView != null) {
            delay(150)
            val escapedQuery =
                    searchQuery
                            .replace("\\", "\\\\")
                            .replace("\"", "\\\"")
                            .replace("\n", "\\n")
                            .replace("\r", "\\r")
                            .replace("\t", "\\t")

            val jsCode =
                    """(function() {
                try {
                    if(window.searchInDocument) {
                        var result = window.searchInDocument("$escapedQuery", $matchCase, $wholeWord, $useRegex);
                        return JSON.stringify(result);
                    }
                    return '{"count":0,"current":0}';
                } catch(e) {
                    return '{"count":0,"current":0}';
                }
            })();""".trimIndent()

            webView.evaluateJavascript(jsCode) { result ->
                try {
                    val clean = result?.removeSurrounding("\"")?.replace("\\\"", "\"") ?: "{}"
                    if (clean.contains("count")) {
                        val countMatch = Regex("\"count\":(\\d+)").find(clean)
                        val currentMatch2 = Regex("\"current\":(\\d+)").find(clean)
                        matchCount = countMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
                        currentMatch = currentMatch2?.groupValues?.get(1)?.toIntOrNull() ?: 0
                    }
                } catch (e: Exception) {
                    android.util.Log.e("SearchBar", "Error parsing search result", e)
                }
            }
        } else if (searchQuery.isEmpty()) {
            matchCount = 0
            currentMatch = 0
        }
    }

    AnimatedVisibility(
            visible = visible,
            enter =
                    slideInVertically(initialOffsetY = { -it }, animationSpec = tween(200)) +
                            fadeIn(animationSpec = tween(200)),
            exit =
                    slideOutVertically(targetOffsetY = { -it }, animationSpec = tween(150)) +
                            fadeOut(animationSpec = tween(150)),
            modifier = modifier
    ) {
        Box(
                modifier =
                        Modifier.fillMaxWidth()
                                .shadow(
                                        8.dp,
                                        RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
                                )
                                .background(
                                        brush =
                                                Brush.verticalGradient(
                                                        colors =
                                                                listOf(
                                                                        SearchBarBackground,
                                                                        SearchBarBackground.copy(
                                                                                alpha = 0.98f
                                                                        )
                                                                )
                                                ),
                                        shape =
                                                RoundedCornerShape(
                                                        bottomStart = 12.dp,
                                                        bottomEnd = 12.dp
                                                )
                                )
                                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column {
                // Search Row
                Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                ) {
                    // Expand/collapse replace button
                    IconButton(
                            onClick = { showReplace = !showReplace },
                            modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                                imageVector =
                                        if (showReplace) Icons.Default.KeyboardArrowUp
                                        else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Toggle Replace",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    // Search input field
                    PremiumSearchInput(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = "Search",
                            isFocused = isSearchFocused,
                            onFocusChange = { isSearchFocused = it },
                            modifier = Modifier.weight(1f).focusRequester(focusRequester)
                    )

                    Spacer(Modifier.width(12.dp))

                    // Match count badge
                    MatchCountBadge(
                            current = currentMatch,
                            total = matchCount,
                            hasQuery = searchQuery.isNotEmpty()
                    )

                    Spacer(Modifier.width(8.dp))

                    // Navigation buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        NavigationButton(
                                icon = Icons.Default.KeyboardArrowUp,
                                contentDescription = "Previous match",
                                enabled = matchCount > 0,
                                onClick = {
                                    webView?.evaluateJavascript(
                                            "if(window.findPrevious) JSON.stringify(window.findPrevious());"
                                    ) { result ->
                                        parseMatchResult(result) { count, current ->
                                            matchCount = count
                                            currentMatch = current
                                        }
                                    }
                                }
                        )
                        NavigationButton(
                                icon = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Next match",
                                enabled = matchCount > 0,
                                onClick = {
                                    webView?.evaluateJavascript(
                                            "if(window.findNext) JSON.stringify(window.findNext());"
                                    ) { result ->
                                        parseMatchResult(result) { count, current ->
                                            matchCount = count
                                            currentMatch = current
                                        }
                                    }
                                }
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    // Option toggles
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        PremiumOptionToggle("Aa", "Match Case", matchCase) { matchCase = it }
                        PremiumOptionToggle("Ab", "Whole Word", wholeWord) { wholeWord = it }
                        PremiumOptionToggle(".*", "Regex", useRegex) { useRegex = it }
                    }

                    Spacer(Modifier.width(12.dp))

                    // Close button
                    IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                        Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close search",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Replace Row (collapsible)
                AnimatedVisibility(visible = showReplace) {
                    Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                    ) {
                        Spacer(Modifier.width(40.dp))

                        // Replace input
                        PremiumSearchInput(
                                value = replaceText,
                                onValueChange = { replaceText = it },
                                placeholder = "Replace with...",
                                isFocused = false,
                                onFocusChange = {},
                                modifier = Modifier.weight(1f)
                        )

                        Spacer(Modifier.width(12.dp))

                        // Replace button
                        PremiumActionButton(
                                text = "Replace",
                                onClick = {
                                    val escaped = replaceText.replace("'", "\\'")
                                    webView?.evaluateJavascript(
                                            "if(window.replaceInDocument) JSON.stringify(window.replaceInDocument('$escaped'));"
                                    ) { result ->
                                        parseMatchResult(result) { count, current ->
                                            matchCount = count
                                            currentMatch = current
                                        }
                                    }
                                },
                                enabled = matchCount > 0
                        )

                        Spacer(Modifier.width(8.dp))

                        // Replace All button
                        PremiumActionButton(
                                text = "All",
                                onClick = {
                                    val escaped = replaceText.replace("'", "\\'")
                                    webView?.evaluateJavascript(
                                            "if(window.replaceAllInDocument) window.replaceAllInDocument('$escaped');"
                                    ) { _ ->
                                        matchCount = 0
                                        currentMatch = 0
                                    }
                                },
                                enabled = matchCount > 0,
                                isPrimary = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumSearchInput(
        value: String,
        onValueChange: (String) -> Unit,
        placeholder: String,
        isFocused: Boolean,
        onFocusChange: (Boolean) -> Unit,
        modifier: Modifier = Modifier
) {
    val borderColor by
            animateColorAsState(
                    targetValue = if (isFocused) SearchInputBorderFocused else SearchInputBorder,
                    animationSpec = tween(150)
            )

    BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle =
                    TextStyle(
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal
                    ),
            cursorBrush = SolidColor(AccentPurple),
            modifier =
                    modifier.height(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SearchInputBackground)
                            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(text = placeholder, color = TextSecondary, fontSize = 14.sp)
                    }
                    innerTextField()
                }
            }
    )
}

@Composable
private fun MatchCountBadge(current: Int, total: Int, hasQuery: Boolean) {
    val backgroundColor =
            when {
                !hasQuery -> Color.Transparent
                total == 0 -> WarningOrange.copy(alpha = 0.15f)
                else -> SuccessGreen.copy(alpha = 0.15f)
            }
    val textColor =
            when {
                !hasQuery -> TextSecondary
                total == 0 -> WarningOrange
                else -> SuccessGreen
            }

    Box(
            modifier =
                    Modifier.clip(RoundedCornerShape(6.dp))
                            .background(backgroundColor)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
    ) {
        Text(
                text =
                        if (!hasQuery) "No query"
                        else if (total == 0) "No results" else "$current of $total",
                color = textColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun NavigationButton(
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        contentDescription: String,
        enabled: Boolean,
        onClick: () -> Unit
) {
    Box(
            modifier =
                    Modifier.size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (enabled) SearchInputBackground else Color.Transparent)
                            .clickable(enabled = enabled) { onClick() },
            contentAlignment = Alignment.Center
    ) {
        Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (enabled) TextPrimary else TextSecondary.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun PremiumOptionToggle(
        label: String,
        description: String,
        isActive: Boolean,
        onToggle: (Boolean) -> Unit
) {
    val backgroundColor by
            animateColorAsState(
                    targetValue =
                            if (isActive) AccentPurple.copy(alpha = 0.2f) else Color.Transparent,
                    animationSpec = tween(150)
            )
    val borderColor by
            animateColorAsState(
                    targetValue = if (isActive) AccentPurple else SearchInputBorder,
                    animationSpec = tween(150)
            )

    Box(
            modifier =
                    Modifier.size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(backgroundColor)
                            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
                            .clickable { onToggle(!isActive) },
            contentAlignment = Alignment.Center
    ) {
        Text(
                text = label,
                color = if (isActive) AccentPurpleLight else TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun PremiumActionButton(
        text: String,
        onClick: () -> Unit,
        enabled: Boolean = true,
        isPrimary: Boolean = false
) {
    val backgroundColor =
            when {
                !enabled -> SearchInputBackground.copy(alpha = 0.5f)
                isPrimary -> AccentPurple
                else -> SearchInputBackground
            }
    val textColor =
            when {
                !enabled -> TextSecondary.copy(alpha = 0.5f)
                isPrimary -> Color.White
                else -> TextPrimary
            }
    val borderColor = if (isPrimary && enabled) AccentPurple else SearchInputBorder

    Box(
            modifier =
                    Modifier.clip(RoundedCornerShape(6.dp))
                            .background(backgroundColor)
                            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
                            .clickable(enabled = enabled) { onClick() }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
    ) { Text(text = text, color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Medium) }
}

@Composable
private fun animateColorAsState(
        targetValue: Color,
        animationSpec: androidx.compose.animation.core.AnimationSpec<Color> = tween(300)
): State<Color> {
    return androidx.compose.animation.animateColorAsState(
            targetValue = targetValue,
            animationSpec = animationSpec
    )
}

private fun parseMatchResult(result: String?, callback: (count: Int, current: Int) -> Unit) {
    try {
        val clean = result?.removeSurrounding("\"")?.replace("\\\"", "\"") ?: "{}"
        val countMatch = Regex("\"count\":(\\d+)").find(clean)
        val currentMatch = Regex("\"current\":(\\d+)").find(clean)
        val count = countMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val current = currentMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
        callback(count, current)
    } catch (e: Exception) {
        android.util.Log.e("SearchBar", "Error parsing result", e)
    }
}
