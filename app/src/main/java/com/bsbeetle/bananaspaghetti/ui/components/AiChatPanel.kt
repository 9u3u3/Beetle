package com.bsbeetle.bananaspaghetti.ui.components

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bsbeetle.bananaspaghetti.LocalAiChatViewModel
import com.bsbeetle.bananaspaghetti.LocalBufferViewModel
import com.bsbeetle.bananaspaghetti.LocalFileViewModel
import com.bsbeetle.bananaspaghetti.LocalKeyboardViewModel
import com.bsbeetle.bananaspaghetti.data.Ic
import com.bsbeetle.bananaspaghetti.ui.theme.AppColors
import com.bsbeetle.bananaspaghetti.ui.theme.IosevkaFont
import com.bsbeetle.bananaspaghetti.utils.ai.AiProviders
import com.bsbeetle.bananaspaghetti.utils.ai.LineDiff
import com.bsbeetle.bananaspaghetti.utils.ai.agent.DocPaths
import com.bsbeetle.bananaspaghetti.utils.preferences.AiPreferences
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.EditorSession
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.SessionList
import com.bsbeetle.bananaspaghetti.utils.webViewHandling.JsResult
import com.bsbeetle.bananaspaghetti.viewmodel.AiChatViewModel
import com.bsbeetle.bananaspaghetti.viewmodel.AiChatViewModel.Item
import com.bsbeetle.bananaspaghetti.viewmodel.AiChatViewModel.State
import java.io.File
import kotlinx.coroutines.delay
import org.json.JSONObject

/**
 * The AI agent, drawn in place of the editor.
 *
 * The editor WebView stays mounted under this panel, so a file the agent changes is copied into
 * the editor at once, and closing the panel shows the editor as it was.
 */
@Composable
fun AiChatPanel(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val aiVm = LocalAiChatViewModel.current
    val bufferVm = LocalBufferViewModel.current
    val fileVm = LocalFileViewModel.current

    val prefs = remember { AiPreferences(context) }
    var configured by remember { mutableStateOf(prefs.isConfigured()) }
    val isRunning by aiVm.isRunning.collectAsStateWithLifecycle()

    val openFileUri = bufferVm.currentFile.value?.uri ?: fileVm.selectedUri
    val openFilePath = DocPaths.toPath(openFileUri)
    val rootPath =
            DocPaths.toPath(fileVm.selectedFolderUri)
                    ?: openFilePath?.let { File(it).parent }
                    ?: File(context.filesDir, "ubuntu/home").path
    // Checked again on each resume: the user grants access in system settings and comes back.
    var resumeCount by remember { mutableStateOf(0) }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer =
                androidx.lifecycle.LifecycleEventObserver { _, event ->
                    if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) resumeCount++
                }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val needsFileAccess =
            remember(rootPath, resumeCount) {
                rootPath.startsWith("/storage/") &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                        !Environment.isExternalStorageManager()
            }

    // Copy each agent write into the editor that shows that file.
    LaunchedEffect(aiVm) {
        aiVm.fileWrites.collect { change ->
            val shownPath = DocPaths.toPath(bufferVm.currentFile.value?.uri)
            val editor = activeEditorSession()
            if (editor != null && shownPath == change.path) {
                replaceEditorText(editor, change.after)
                // The editor reports the change as an edit by the user. The disk already holds
                // this text, so mark the buffer clean again after that report.
                delay(400)
                bufferVm.currentFile.value?.uri?.toString()?.let { bufferVm.markAsClean(it) }
            }
        }
    }

    Column(
            modifier =
                    modifier.fillMaxSize()
                            // Opaque: the editor is mounted directly under this panel.
                            .background(AppColors.BgPrimary)
                            .imePadding()
    ) {
        PanelHeader(
                subtitle = "${aiVm.activeModelName()} · ${File(rootPath).name.ifEmpty { "/" }}",
                showClear = configured && aiVm.items.isNotEmpty(),
                onClear = { aiVm.clearConversation() },
                onClose = { aiVm.closePanel() }
        )
        HorizontalDivider(color = AppColors.Divider)

        when {
            !configured ->
                    ApiKeyEntry(prefs = prefs, onSaved = { configured = true }, modifier = Modifier.weight(1f))
            needsFileAccess -> FileAccessRequest(modifier = Modifier.weight(1f))
            else ->
                    ChatUi(
                            aiVm = aiVm,
                            isRunning = isRunning,
                            modifier = Modifier.weight(1f),
                            onSend = { text ->
                                readEditorText(openFilePath) { live ->
                                    aiVm.send(
                                            text,
                                            AiChatViewModel.Workspace(
                                                    rootPath = rootPath,
                                                    openFilePath = openFilePath,
                                                    openFileContent = live
                                            )
                                    )
                                }
                            }
                    )
        }
    }
}

@Composable
private fun PanelHeader(subtitle: String, showClear: Boolean, onClear: () -> Unit, onClose: () -> Unit) {
    Row(
            modifier =
                    Modifier.fillMaxWidth()
                            .background(AppColors.BgSecondary)
                            .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("AI agent", fontSize = 13.sp, color = AppColors.TextPrimary, maxLines = 1)
            Text(
                    subtitle,
                    fontSize = 10.sp,
                    color = AppColors.TextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
            )
        }
        if (showClear) {
            TextButton(onClick = onClear, contentPadding = PaddingValues(horizontal = 8.dp)) {
                Text("New chat", fontSize = 12.sp, color = AppColors.TextSecondary)
            }
        }
        Box(
                modifier = Modifier.clickable { onClose() }.padding(horizontal = 12.dp, vertical = 6.dp)
        ) { Text("✕", fontSize = 16.sp, color = AppColors.TextSecondary) }
    }
}

/** Shown when the project is in shared storage and Beetle cannot read it with file paths. */
@Composable
private fun FileAccessRequest(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(
            modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.Center
    ) {
        Text("Allow file access", fontSize = 15.sp, color = AppColors.TextPrimary)
        Spacer(Modifier.height(6.dp))
        Text(
                "The agent reads and changes your project files, and runs commands in the project " +
                        "folder. For a folder in phone storage, Android requires \"All files access\" for this.",
                fontSize = 12.sp,
                color = AppColors.TextSecondary
        )
        Spacer(Modifier.height(12.dp))
        Button(
                onClick = {
                    val intent =
                            Intent(
                                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                            )
                    runCatching { context.startActivity(intent) }
                            .onFailure {
                                context.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                            }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.AccentBlue)
        ) { Text("Open settings", color = Color.White) }
    }
}

/**
 * First-run key entry, shown in place of the chat until a provider has a key.
 *
 * Model choice stays in Settings → AI. A first-time user has no basis for that choice yet.
 */
@Composable
private fun ApiKeyEntry(prefs: AiPreferences, onSaved: () -> Unit, modifier: Modifier = Modifier) {
    var providerId by remember { mutableStateOf(prefs.providerId) }
    val provider = remember(providerId) { AiProviders.byId(providerId) }
    var key by remember(providerId) { mutableStateOf(prefs.apiKeyFor(providerId)) }

    Column(modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Connect your AI", fontSize = 16.sp, color = AppColors.TextPrimary)
        Spacer(Modifier.height(4.dp))
        Text(
                "Beetle uses your own API key. You pay the provider directly, and Beetle never " +
                        "sees your requests.",
                fontSize = 12.sp,
                color = AppColors.TextSecondary
        )

        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AiProviders.all.forEach { option ->
                OutlinedButton(
                        onClick = {
                            providerId = option.id
                            prefs.providerId = option.id
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        modifier = Modifier.weight(1f)
                ) {
                    Text(
                            (if (option.id == providerId) "● " else "○ ") + option.displayName,
                            color = AppColors.TextPrimary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        Text(provider.keyNote, fontSize = 11.sp, color = AppColors.TextTertiary)

        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
                value = key,
                onValueChange = { key = it },
                label = { Text("${provider.displayName} API key (${provider.keyHint})") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))
        Button(
                onClick = {
                    prefs.setApiKeyFor(providerId, key.trim())
                    if (prefs.isConfigured()) onSaved()
                },
                enabled = key.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
        ) { Text("Save and start") }

        Spacer(Modifier.height(8.dp))
        Text("Change the model in Settings → AI.", fontSize = 11.sp, color = AppColors.TextTertiary)
    }
}

@Composable
private fun ChatUi(
        aiVm: AiChatViewModel,
        isRunning: Boolean,
        modifier: Modifier = Modifier,
        onSend: (String) -> Unit
) {
    var input by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    val allowAll by aiVm.allowAllCommands.collectAsStateWithLifecycle()

    // Follow the stream. A delta grows the last item instead of adding one, so the key includes
    // the length of the last text.
    val last = aiVm.items.lastOrNull()
    val lastSize = (last as? Item.Assistant)?.text?.length ?: 0
    LaunchedEffect(aiVm.items.size, lastSize, last) {
        if (aiVm.items.isNotEmpty()) listState.scrollToItem(aiVm.items.lastIndex, Int.MAX_VALUE / 2)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (aiVm.items.isEmpty()) {
            EmptyState(modifier = Modifier.weight(1f))
        } else {
            LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(aiVm.items, key = { it.id }) { item ->
                    when (item) {
                        is Item.User -> UserBubble(item.text)
                        is Item.Assistant -> AssistantText(item.text)
                        is Item.Activity -> ActivityRow(item)
                        is Item.Edit -> EditCard(item, onUndo = { aiVm.undo(item) })
                        is Item.Command ->
                                CommandCard(
                                        item,
                                        onAllow = { aiVm.answerApproval(true) },
                                        onAllowAll = { aiVm.answerApproval(true, allowAll = true) },
                                        onDeny = { aiVm.answerApproval(false) }
                                )
                        is Item.Notice -> NoticeRow(item)
                    }
                }
                // Shown while the model thinks or a tool runs. Not while a command waits for the
                // user, because then nothing works until the user answers.
                val lastItem = aiVm.items.lastOrNull()
                val waitingForUser = aiVm.items.any { it is Item.Command && it.state == State.WAITING_APPROVAL }
                val streaming = lastItem is Item.Assistant && lastItem.text.isNotBlank()
                if (isRunning && !waitingForUser && !streaming) {
                    item(key = "working") { WorkingRow() }
                }
            }
        }

        HorizontalDivider(color = AppColors.Divider)
        if (allowAll) {
            Text(
                    "Commands run without asking in this chat.",
                    fontSize = 10.sp,
                    color = AppColors.AccentYellow,
                    modifier = Modifier.padding(start = 12.dp, top = 4.dp)
            )
        }
        Composer(
                input = input,
                onInputChange = { input = it },
                isRunning = isRunning,
                onStop = { aiVm.stop() },
                onSend = {
                    if (input.isNotBlank()) {
                        onSend(input.trim())
                        input = ""
                    }
                }
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
            modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.Center
    ) {
        Text("Ask the agent to work on this project", fontSize = 14.sp, color = AppColors.TextPrimary)
        Spacer(Modifier.height(8.dp))
        listOf(
                        "It reads and searches your files to understand the code.",
                        "It edits files. Each change shows as a diff with Undo.",
                        "It runs commands in Ubuntu after you allow them."
                )
                .forEach {
                    Text("•  $it", fontSize = 12.sp, color = AppColors.TextSecondary, lineHeight = 17.sp)
                }
        Spacer(Modifier.height(10.dp))
        Text(
                "Example: \"Add input checks to main.c, then build and run it.\"",
                fontSize = 11.sp,
                color = AppColors.TextTertiary
        )
    }
}

@Composable
private fun UserBubble(text: String) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        Text(
                text,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = AppColors.TextPrimary,
                modifier =
                        Modifier.padding(start = 40.dp)
                                .background(AppColors.AccentBlue.copy(alpha = 0.30f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun AssistantText(text: String) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        parseSegments(text).forEach { segment ->
            when (segment) {
                is ChatSegment.Prose ->
                        Text(
                                inlineMarkdown(segment.text),
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                color = AppColors.TextSecondary
                        )
                is ChatSegment.Code -> CodeBlock(segment)
            }
        }
    }
}

@Composable
private fun ActivityRow(item: Item.Activity) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StateMark(item.state)
            Spacer(Modifier.width(8.dp))
            Text(
                    item.label,
                    fontSize = 12.sp,
                    fontFamily = IosevkaFont,
                    color = AppColors.TextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
            )
        }
        item.detail?.let {
            Text(
                    it,
                    fontSize = 11.sp,
                    color = AppColors.AccentRed.copy(alpha = 0.85f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 22.dp, top = 2.dp)
            )
        }
    }
}

@Composable
private fun StateMark(state: State) {
    Box(modifier = Modifier.size(14.dp), contentAlignment = Alignment.Center) {
        when (state) {
            State.WAITING_APPROVAL -> Text("?", fontSize = 12.sp, color = AppColors.AccentYellow)
            State.RUNNING ->
                    CircularProgressIndicator(
                            modifier = Modifier.size(11.dp),
                            strokeWidth = 1.5.dp,
                            color = AppColors.AccentBlue
                    )
            State.DONE -> Text("✓", fontSize = 12.sp, color = AppColors.AccentGreen)
            State.FAILED -> Text("✕", fontSize = 12.sp, color = AppColors.AccentRed)
            State.DENIED -> Text("–", fontSize = 12.sp, color = AppColors.TextMuted)
        }
    }
}

/** A file change as a code review shows it: file, `+n −m`, then the changed lines. */
@Composable
private fun EditCard(item: Item.Edit, onUndo: () -> Unit) {
    var expanded by rememberSaveable(item.id) { mutableStateOf(true) }
    val created = item.change.before == null

    Column(
            modifier =
                    Modifier.fillMaxWidth()
                            .background(AppColors.BgSecondary, RoundedCornerShape(8.dp))
                            .border(1.dp, AppColors.Border, RoundedCornerShape(8.dp))
    ) {
        Row(
                modifier =
                        Modifier.fillMaxWidth()
                                .clickable { expanded = !expanded }
                                .padding(start = 10.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                    if (expanded) "▾" else "▸",
                    fontSize = 11.sp,
                    color = AppColors.TextTertiary,
                    modifier = Modifier.padding(end = 6.dp)
            )
            Text(
                    (if (created) "new  " else "") + item.change.relativePath,
                    fontSize = 12.sp,
                    fontFamily = IosevkaFont,
                    color = if (item.undone) AppColors.TextMuted else AppColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
            )
            Text("+${item.added}", fontSize = 12.sp, color = AppColors.StatusAdded)
            Spacer(Modifier.width(6.dp))
            Text("−${item.removed}", fontSize = 12.sp, color = AppColors.StatusDeleted)
            if (item.undone) {
                Text("undone", fontSize = 11.sp, color = AppColors.TextMuted, modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp))
            } else {
                TextButton(onClick = onUndo, contentPadding = PaddingValues(horizontal = 10.dp)) {
                    Text("Undo", fontSize = 12.sp, color = AppColors.AccentOrange)
                }
            }
        }

        if (expanded && item.preview.isNotEmpty() && !item.undone) {
            HorizontalDivider(color = AppColors.Divider)
            Column(
                    modifier =
                            Modifier.fillMaxWidth()
                                    .heightIn(max = 260.dp)
                                    .verticalScroll(rememberScrollState())
                                    .horizontalScroll(rememberScrollState())
                                    .padding(vertical = 4.dp)
            ) {
                item.preview.forEach { line ->
                    val (marker, color, tint) =
                            when (line.type) {
                                LineDiff.Type.ADD ->
                                        Triple("+", AppColors.StatusAdded, AppColors.StatusAdded.copy(alpha = 0.10f))
                                LineDiff.Type.REMOVE ->
                                        Triple("−", AppColors.StatusDeleted, AppColors.StatusDeleted.copy(alpha = 0.10f))
                                LineDiff.Type.CONTEXT -> Triple(" ", AppColors.TextTertiary, Color.Transparent)
                            }
                    Text(
                            "$marker ${line.text}",
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            fontFamily = IosevkaFont,
                            color = color,
                            softWrap = false,
                            modifier = Modifier.background(tint).padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CommandCard(item: Item.Command, onAllow: () -> Unit, onAllowAll: () -> Unit, onDeny: () -> Unit) {
    var expanded by rememberSaveable(item.id) { mutableStateOf(false) }
    val waiting = item.state == State.WAITING_APPROVAL

    Column(
            modifier =
                    Modifier.fillMaxWidth()
                            .background(Color(0xFF151515), RoundedCornerShape(8.dp))
                            .border(
                                    1.dp,
                                    if (waiting) AppColors.AccentYellow.copy(alpha = 0.6f) else AppColors.Border,
                                    RoundedCornerShape(8.dp)
                            )
    ) {
        Row(
                modifier =
                        Modifier.fillMaxWidth()
                                .clickable(enabled = item.output != null) { expanded = !expanded }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Top
        ) {
            Text("$ ", fontSize = 12.sp, fontFamily = IosevkaFont, color = AppColors.AccentGreen)
            Text(
                    item.command,
                    fontSize = 12.sp,
                    fontFamily = IosevkaFont,
                    color = AppColors.TextPrimary,
                    maxLines = if (expanded || waiting) 12 else 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(6.dp))
            if (item.state == State.DENIED) Text("Denied", fontSize = 11.sp, color = AppColors.TextMuted)
            else StateMark(item.state)
        }

        if (waiting) {
            Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 6.dp, end = 6.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                        onClick = onAllow,
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.AccentBlue),
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        modifier = Modifier.height(34.dp)
                ) { Text("Run", fontSize = 12.sp, color = Color.White) }
                TextButton(onClick = onAllowAll, contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Text("Always run", fontSize = 12.sp, color = AppColors.TextSecondary, maxLines = 1)
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDeny, contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Text("Deny", fontSize = 12.sp, color = AppColors.AccentRed)
                }
            }
        }

        if (item.output != null) {
            HorizontalDivider(color = AppColors.Divider)
            val lines = item.output.lines()
            val shown = if (expanded) item.output else lines.takeLast(4).joinToString("\n")
            Text(
                    shown,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    fontFamily = IosevkaFont,
                    color = if (item.state == State.FAILED) AppColors.AccentOrange else AppColors.TextTertiary,
                    softWrap = false,
                    modifier =
                            Modifier.fillMaxWidth()
                                    .heightIn(max = 300.dp)
                                    .verticalScroll(rememberScrollState())
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
            if (!expanded && lines.size > 4) {
                Text(
                        "Tap to show all ${lines.size} lines",
                        fontSize = 10.sp,
                        color = AppColors.TextMuted,
                        modifier = Modifier.padding(start = 10.dp, bottom = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun NoticeRow(item: Item.Notice) {
    Text(
            item.text,
            fontSize = 12.sp,
            color = if (item.isError) AppColors.AccentRed else AppColors.TextTertiary,
            modifier =
                    Modifier.fillMaxWidth()
                            .background(
                                    if (item.isError) AppColors.AccentRed.copy(alpha = 0.10f) else Color.Transparent,
                                    RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
private fun WorkingRow() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp, color = AppColors.AccentBlue)
        Spacer(Modifier.width(8.dp))
        Text("Working…", fontSize = 12.sp, color = AppColors.TextTertiary)
    }
}

@Composable
private fun CodeBlock(segment: ChatSegment.Code) {
    Column(
            modifier =
                    Modifier.fillMaxWidth()
                            .background(AppColors.BgSecondary, RoundedCornerShape(6.dp))
                            .border(1.dp, AppColors.BorderLight, RoundedCornerShape(6.dp))
    ) {
        segment.lang?.let {
            Text(it, fontSize = 10.sp, color = AppColors.TextMuted, modifier = Modifier.padding(start = 8.dp, top = 4.dp))
        }
        Text(
                segment.code,
                fontSize = 11.sp,
                fontFamily = IosevkaFont,
                lineHeight = 15.sp,
                color = AppColors.TextPrimary,
                softWrap = false,
                modifier =
                        Modifier.fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 8.dp, vertical = 6.dp)
        )
    }
}

/**
 * The input row. The field grows to four lines. Send and Stop sit at its right.
 */
@Composable
private fun Composer(
        input: String,
        onInputChange: (String) -> Unit,
        isRunning: Boolean,
        onStop: () -> Unit,
        onSend: () -> Unit
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val keyboardState by LocalKeyboardViewModel.current.uiState.collectAsStateWithLifecycle()
    // Portrait uses the system keyboard. Landscape uses Beetle's split keyboard unless the user
    // chose the system keyboard for landscape too.
    val useSystemKeyboard = !isLandscape || keyboardState.useLandscapeIme

    Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.Bottom
    ) {
        KeyboardRoutedInput(
                value = input,
                onValueChange = onInputChange,
                placeholder = "Ask the agent…",
                useSystemKeyboard = useSystemKeyboard,
                modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(6.dp))
        if (isRunning) {
            Button(
                    onClick = onStop,
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.BgTertiary),
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    modifier = Modifier.height(38.dp)
            ) { Text("Stop", fontSize = 12.sp, color = AppColors.AccentRed) }
        } else {
            Button(
                    onClick = onSend,
                    enabled = input.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.AccentBlue),
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    modifier = Modifier.height(38.dp)
            ) { Text("Send", fontSize = 12.sp, color = Color.White) }
        }
    }
}

/**
 * A native text field that works with both keyboards.
 *
 * With Beetle's split keyboard, the field publishes its InputConnection to [Ic], and the split
 * keyboard types into it. With the system keyboard, it is an ordinary EditText.
 */
@Composable
private fun KeyboardRoutedInput(
        value: String,
        onValueChange: (String) -> Unit,
        placeholder: String,
        useSystemKeyboard: Boolean,
        modifier: Modifier = Modifier
) {
    val editTextRef = remember { mutableStateOf<EditText?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            if (Ic.ic != null && editTextRef.value?.isFocused == true) Ic.ic = null
        }
    }

    Box(
            modifier =
                    modifier.heightIn(min = 38.dp)
                            .background(AppColors.BgInput, RoundedCornerShape(8.dp))
                            .border(1.dp, AppColors.Border, RoundedCornerShape(8.dp))
    ) {
        AndroidView(
                factory = { ctx ->
                    EditText(ctx).apply {
                        setTextColor(android.graphics.Color.WHITE)
                        textSize = 13f
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        val pad = (8 * resources.displayMetrics.density).toInt()
                        setPadding(pad + pad / 2, pad, pad, pad)
                        hint = placeholder
                        setHintTextColor(android.graphics.Color.parseColor("#7A7A7A"))
                        inputType =
                                EditorInfo.TYPE_CLASS_TEXT or
                                        EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE or
                                        EditorInfo.TYPE_TEXT_FLAG_CAP_SENTENCES
                        // In landscape a system IME can take the whole screen. Keep the panel visible.
                        imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI or EditorInfo.IME_FLAG_NO_FULLSCREEN
                        maxLines = 4
                        setText(value)
                        editTextRef.value = this

                        addTextChangedListener(
                                object : TextWatcher {
                                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                                        onValueChange(s?.toString() ?: "")
                                    }
                                    override fun afterTextChanged(s: Editable?) {}
                                }
                        )
                    }
                },
                update = { editText ->
                    editText.showSoftInputOnFocus = useSystemKeyboard
                    editText.setOnFocusChangeListener { view, hasFocus ->
                        if (useSystemKeyboard) {
                            if (hasFocus) {
                                view.post {
                                    val imm = view.context.getSystemService(InputMethodManager::class.java)
                                    imm?.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
                                }
                            }
                        } else if (hasFocus) {
                            view.post { Ic.ic = editText.onCreateInputConnection(EditorInfo()) }
                        } else {
                            Ic.ic = null
                        }
                    }
                    if (!useSystemKeyboard && editText.isFocused && Ic.ic == null) {
                        Ic.ic = editText.onCreateInputConnection(EditorInfo())
                    }
                    if (editText.text.toString() != value) {
                        editText.setText(value)
                        editText.setSelection(value.length)
                    }
                },
                modifier = Modifier.fillMaxWidth()
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Markdown-lite: fenced code, inline code and bold
// ---------------------------------------------------------------------------------------------

private sealed interface ChatSegment {
    data class Prose(val text: String) : ChatSegment

    data class Code(val lang: String?, val code: String) : ChatSegment
}

/** Splits text on ``` fences. An open fence at the end is code that is still streaming. */
private fun parseSegments(text: String): List<ChatSegment> {
    if (!text.contains("```")) {
        return if (text.isBlank()) emptyList() else listOf(ChatSegment.Prose(text.trim()))
    }
    val segments = mutableListOf<ChatSegment>()
    val buffer = StringBuilder()
    var inCode = false
    var lang: String? = null

    fun flush() {
        val content = buffer.toString().trim('\n')
        buffer.setLength(0)
        if (content.isBlank()) return
        segments += if (inCode) ChatSegment.Code(lang, content) else ChatSegment.Prose(content.trim())
    }

    text.lines().forEach { line ->
        if (line.trimStart().startsWith("```")) {
            flush()
            if (inCode) {
                inCode = false
                lang = null
            } else {
                inCode = true
                lang = line.trimStart().removePrefix("```").trim().ifBlank { null }
            }
        } else {
            buffer.append(line).append('\n')
        }
    }
    flush()
    return segments
}

/** Renders `code`, **bold**, "#" headings and "-" list items. Everything else stays as typed. */
private fun inlineMarkdown(text: String): AnnotatedString = buildAnnotatedString {
    text.lines().forEachIndexed { index, rawLine ->
        if (index > 0) append('\n')
        var line = rawLine
        val bullet = Regex("^(\\s*)[-*] ").find(line)
        if (bullet != null) line = bullet.groupValues[1] + "• " + line.substring(bullet.range.last + 1)
        val heading = line.trimStart().startsWith("#")
        if (heading) line = line.trimStart().trimStart('#').trimStart()
        if (heading) pushStyle(SpanStyle(fontWeight = FontWeight.Bold, color = AppColors.TextPrimary))
        var i = 0
        while (i < line.length) {
            when {
                line[i] == '`' && line.indexOf('`', i + 1) > i -> {
                    val end = line.indexOf('`', i + 1)
                    pushStyle(
                            SpanStyle(
                                    fontFamily = IosevkaFont,
                                    color = AppColors.AccentOrange,
                                    background = AppColors.BgTertiary
                            )
                    )
                    append(line, i + 1, end)
                    pop()
                    i = end + 1
                }
                line.startsWith("**", i) && line.indexOf("**", i + 2) > i -> {
                    val end = line.indexOf("**", i + 2)
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold, color = AppColors.TextPrimary))
                    append(line, i + 2, end)
                    pop()
                    i = end + 2
                }
                else -> {
                    append(line[i])
                    i++
                }
            }
        }
        if (heading) pop()
    }
}

// ---------------------------------------------------------------------------------------------
// Editor plumbing
// ---------------------------------------------------------------------------------------------

private fun activeEditorSession(): EditorSession? =
        SessionList.sessions.getOrNull(SessionList.activeSessionIndex ?: -1) as? EditorSession

/**
 * Reads the active editor's text, so the agent sees unsaved changes. Gives null when no editor
 * shows [path].
 */
private fun readEditorText(path: String?, onResult: (String?) -> Unit) {
    val view = activeEditorSession()?.view
    if (path == null || view == null) {
        onResult(null)
        return
    }
    view.evaluateJavascript("window.editor ? window.editor.state.doc.toString() : null") { result ->
        onResult(if (result == null || result == "null") null else JsResult.decodeString(result))
    }
}

/**
 * Replaces the editor text with [text]. Only the changed middle part is sent as a change, so the
 * cursor and the scroll position stay where they are when the edit is elsewhere.
 */
private fun replaceEditorText(session: EditorSession, text: String) {
    val quoted = JSONObject.quote(text)
    session.view?.evaluateJavascript(
            """
            (function(t){var e=window.editor;if(!e)return;var o=e.state.doc.toString();if(o===t)return;
            var a=0,m=Math.min(o.length,t.length);while(a<m&&o.charCodeAt(a)===t.charCodeAt(a))a++;
            var b=0;while(b<m-a&&o.charCodeAt(o.length-1-b)===t.charCodeAt(t.length-1-b))b++;
            e.dispatch({changes:{from:a,to:o.length-b,insert:t.slice(a,t.length-b)}});})($quoted)
            """.trimIndent(),
            null
    )
}
