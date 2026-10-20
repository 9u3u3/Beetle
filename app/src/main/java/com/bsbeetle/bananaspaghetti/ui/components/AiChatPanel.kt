package com.bsbeetle.bananaspaghetti.ui.components

import android.net.Uri
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bsbeetle.bananaspaghetti.LocalAiChatViewModel
import com.bsbeetle.bananaspaghetti.LocalBufferViewModel
import com.bsbeetle.bananaspaghetti.LocalFileViewModel
import com.bsbeetle.bananaspaghetti.data.Ic
import com.bsbeetle.bananaspaghetti.ui.theme.AppColors
import com.bsbeetle.bananaspaghetti.utils.ai.AiProviders
import com.bsbeetle.bananaspaghetti.utils.ai.LineDiff
import com.bsbeetle.bananaspaghetti.utils.preferences.AiPreferences
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.EditorSession
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.SessionList
import com.bsbeetle.bananaspaghetti.viewmodel.AiChatViewModel
import org.json.JSONObject

/**
 * The AI assistant, rendered *in place of* the editor rather than over it.
 *
 * This replaced a [androidx.compose.material3.ModalBottomSheet]: on a landscape phone the sheet
 * left the chat about 150dp tall, so a single reply was clipped after three lines and the code in
 * it was unreadable — exactly the content the feature exists to produce. Taking the editor's own
 * slot gives the conversation the full pane, and the editor is one tap away in the top bar.
 *
 * The host keeps the editor WebView in the composition underneath, so toggling the panel is a
 * visibility change, not a teardown — editor scroll position, undo history and LSP state all
 * survive the round trip.
 */
@Composable
fun AiChatPanel(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val aiVm = LocalAiChatViewModel.current
    val bufferVm = LocalBufferViewModel.current

    val fileVm = LocalFileViewModel.current

    // BufferViewModel.currentFile is only set on some open paths, so relying on it alone reported
    // edits against "the buffer" even with a file plainly open. FileViewModel knows the path in the
    // cases it misses.
    val fileName =
            bufferVm.currentFile.value?.name
                    ?: fileVm.currentFilePath?.substringAfterLast('/')?.ifBlank { null }
                            ?: fileVm.currentRemotePath?.substringAfterLast('/')?.ifBlank { null }
    val filePath = bufferVm.currentFile.value?.uri?.toString() ?: fileVm.selectedUri?.toString()

    val prefs = remember { AiPreferences(context) }
    var configured by remember { mutableStateOf(prefs.isConfigured()) }

    val isLoading by aiVm.isLoading.collectAsStateWithLifecycle()
    val error by aiVm.error.collectAsStateWithLifecycle()
    val pendingEdit by aiVm.pendingEdit.collectAsStateWithLifecycle()
    val lastApplied by aiVm.lastApplied.collectAsStateWithLifecycle()

    // The editor the edit was asked about. A reply can take minutes, and the user can switch to
    // another editor in that time. The edit must go back to the editor it came from.
    var editTarget by remember { mutableStateOf<EditorSession?>(null) }

    // A finished edit goes straight into the buffer. The staging step still exists so the original
    // content is captured for undo, but the user never has to confirm to see their change.
    LaunchedEffect(pendingEdit) {
        pendingEdit?.let { edit ->
            applyEdit(context, bufferVm, edit, editTarget)
            aiVm.noteApplied(edit)
            aiVm.dismissPendingEdit()
        }
    }

    Column(
            modifier =
                    modifier.fillMaxSize()
                            // Opaque: the editor is still mounted directly beneath this.
                            .background(AppColors.BgPrimary)
                            .imePadding()
    ) {
        PanelHeader(
                title = fileName?.let { "AI · $it" } ?: "AI assistant",
                subtitle = aiVm.activeModelName(),
                showClear = configured,
                onClear = { aiVm.clearConversation() },
                onClose = { aiVm.closePanel() }
        )
        HorizontalDivider(color = AppColors.Divider)

        if (!configured) {
            ApiKeyEntry(
                    prefs = prefs,
                    onSaved = { configured = true },
                    modifier = Modifier.weight(1f)
            )
        } else {
            ChatUi(
                    aiVm = aiVm,
                    isLoading = isLoading,
                    error = error,
                    lastApplied = lastApplied,
                    fileName = fileName,
                    modifier = Modifier.weight(1f),
                    onUndo = {
                        lastApplied?.let { edit ->
                            writeToEditor(edit.originalContent, editTarget)
                            persist(context, bufferVm, edit.filePath, edit.originalContent)
                        }
                        aiVm.clearApplied()
                    },
                    onDismissApplied = { aiVm.clearApplied() },
                    onSend = { instruction, asEdit ->
                        if (asEdit) editTarget = activeEditorSession()
                        readActiveEditorContent { content ->
                            if (asEdit && content != null) {
                                // A never-saved buffer has no URI. That is not a reason to refuse
                                // the edit — stage it anyway and let the apply step write to the
                                // editor only, instead of silently downgrading to a chat reply.
                                aiVm.proposeEdit(
                                        instruction,
                                        filePath ?: "",
                                        fileName ?: "untitled",
                                        content
                                )
                            } else {
                                aiVm.sendChat(instruction, fileName, content)
                            }
                        }
                    },
                    onClearError = { aiVm.clearError() },
                    onStop = { aiVm.cancelActive() }
            )
        }
    }
}

@Composable
private fun PanelHeader(
        title: String,
        subtitle: String,
        showClear: Boolean,
        onClear: () -> Unit,
        onClose: () -> Unit
) {
    Row(
            modifier =
                    Modifier.fillMaxWidth()
                            .background(AppColors.BgSecondary)
                            .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 13.sp, color = AppColors.TextPrimary, maxLines = 1)
            // Which model is answering, so output can be attributed when comparing providers.
            Text(subtitle, fontSize = 10.sp, color = AppColors.TextMuted, maxLines = 1)
        }
        if (showClear) {
            TextButton(onClick = onClear) {
                Text("Clear", fontSize = 12.sp, color = AppColors.TextSecondary)
            }
        }
        Box(
                modifier = Modifier.clickable { onClose() }.padding(horizontal = 10.dp, vertical = 4.dp)
        ) { Text("✕", fontSize = 15.sp, color = AppColors.TextSecondary) }
    }
}

/**
 * First-run key entry, shown in place of the chat until a provider is configured.
 *
 * Deliberately thinner than Settings → AI: pick a provider, paste a key, start. Model choice lives
 * in Settings, because a first-time user has no basis for that decision yet and every extra control
 * here is one more thing between them and seeing the feature work.
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
                "Beetle uses your own API key and never stores a managed one, so you'll never get " +
                        "a surprise bill from us — you pay the provider directly.",
                fontSize = 12.sp,
                color = AppColors.TextSecondary
        )

        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            AiProviders.all.forEach { option ->
                OutlinedButton(
                        onClick = {
                            providerId = option.id
                            prefs.providerId = option.id
                        },
                        modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                            if (option.id == providerId) "● " else "○ ",
                            color = AppColors.TextPrimary
                    )
                    Text(option.displayName, color = AppColors.TextPrimary)
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        Text(provider.keyNote, fontSize = 11.sp, color = AppColors.TextMuted)

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
        ) { Text("Save & start") }

        Spacer(Modifier.height(8.dp))
        Text(
                "Change the model any time in Settings → AI.",
                fontSize = 11.sp,
                color = AppColors.TextMuted
        )
    }
}

@Composable
private fun ChatUi(
        aiVm: AiChatViewModel,
        isLoading: Boolean,
        error: String?,
        lastApplied: AiChatViewModel.PendingEdit?,
        fileName: String?,
        modifier: Modifier = Modifier,
        onUndo: () -> Unit,
        onDismissApplied: () -> Unit,
        onSend: (instruction: String, asEdit: Boolean) -> Unit,
        onClearError: () -> Unit,
        onStop: () -> Unit
) {
    var input by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Follow the stream: deltas grow the last message rather than adding one, so keying the scroll
    // on size alone would freeze partway through a long reply.
    val lastLength = aiVm.messages.lastOrNull()?.text?.length ?: 0
    LaunchedEffect(aiVm.messages.size, lastLength) {
        if (aiVm.messages.isNotEmpty()) listState.animateScrollToItem(aiVm.messages.size - 1)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (aiVm.messages.isEmpty()) {
            EmptyState(modifier = Modifier.weight(1f))
        } else {
            LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(aiVm.messages, key = { it.id }) { msg -> MessageBubble(msg = msg) }
            }
        }

        if (lastApplied != null) {
            AppliedBar(fileName = fileName, onUndo = onUndo, onDismiss = onDismissApplied)
        }

        if (error != null) {
            Row(
                    modifier =
                            Modifier.fillMaxWidth()
                                    .background(AppColors.AccentRed.copy(alpha = 0.12f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                        error,
                        color = AppColors.AccentRed,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onClearError) { Text("Dismiss", fontSize = 12.sp) }
            }
        }

        HorizontalDivider(color = AppColors.Divider)
        Composer(
                input = input,
                onInputChange = { input = it },
                isLoading = isLoading,
                onStop = onStop,
                onSend = { asEdit ->
                    if (input.isNotBlank()) {
                        onSend(input.trim(), asEdit)
                        input = ""
                    }
                }
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
            modifier = modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Describe a change", fontSize = 13.sp, color = AppColors.TextSecondary)
        Spacer(Modifier.height(4.dp))
        Text(
                "Say what to change and it is written into the open file.\n" +
                        "Undo is one tap if it gets it wrong.",
                fontSize = 10.sp,
                color = AppColors.TextMuted,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun MessageBubble(msg: AiChatViewModel.ChatUiMessage) {
    val isUser = msg.role == AiChatViewModel.ChatUiMessage.Role.USER

    if (isUser) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            Text(
                    msg.text,
                    fontSize = 12.sp,
                    color = AppColors.TextPrimary,
                    modifier =
                            Modifier.background(
                                            AppColors.AccentBlue.copy(alpha = 0.28f),
                                            RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
            )
        }
        return
    }

    msg.edit?.let { edit ->
        DiffCard(edit)
        return
    }

    // Assistant turns are split so fenced code renders as real code — monospace, horizontally
    // scrollable instead of wrapped, and directly applicable to the editor. Wrapping code in a
    // proportional font was the single biggest reason replies were unusable on a phone.
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        parseSegments(msg.text).forEach { segment ->
            when (segment) {
                is ChatSegment.Prose ->
                        Text(segment.text, fontSize = 12.sp, color = AppColors.TextSecondary)
                is ChatSegment.Code -> CodeBlock(segment = segment)
            }
        }
    }
}

/**
 * The result of an edit, in the shape a code review takes: file, `+n −m`, then the changed lines.
 *
 * Collapsible because the counts are usually the whole answer — the lines matter when they don't
 * look right. Unchanged runs are already elided by [LineDiff], so what shows is only what moved.
 */
@Composable
private fun DiffCard(edit: AiChatViewModel.EditSummary) {
    var expanded by rememberSaveable(edit.fileName, edit.added, edit.removed) {
        mutableStateOf(true)
    }

    Column(
            modifier =
                    Modifier.fillMaxWidth()
                            .background(AppColors.BgSecondary, RoundedCornerShape(6.dp))
                            .border(1.dp, AppColors.BorderLight, RoundedCornerShape(6.dp))
    ) {
        Row(
                modifier =
                        Modifier.fillMaxWidth()
                                .clickable { expanded = !expanded }
                                .padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                    if (expanded) "▾" else "▸",
                    fontSize = 9.sp,
                    color = AppColors.TextMuted,
                    modifier = Modifier.padding(end = 6.dp)
            )
            Text(
                    edit.fileName,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = AppColors.TextPrimary,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
            )
            Text("+${edit.added}", fontSize = 11.sp, color = AppColors.StatusAdded)
            Spacer(Modifier.width(6.dp))
            Text("−${edit.removed}", fontSize = 11.sp, color = AppColors.StatusDeleted)
        }

        if (expanded && edit.preview.isNotEmpty()) {
            HorizontalDivider(color = AppColors.Divider)
            Column(
                    modifier =
                            Modifier.fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(vertical = 4.dp)
            ) {
                edit.preview.forEach { line ->
                    val (marker, color, tint) =
                            when (line.type) {
                                LineDiff.Type.ADD ->
                                        Triple(
                                                "+",
                                                AppColors.StatusAdded,
                                                AppColors.StatusAdded.copy(alpha = 0.10f)
                                        )
                                LineDiff.Type.REMOVE ->
                                        Triple(
                                                "−",
                                                AppColors.StatusDeleted,
                                                AppColors.StatusDeleted.copy(alpha = 0.10f)
                                        )
                                LineDiff.Type.CONTEXT ->
                                        Triple(" ", AppColors.TextTertiary, Color.Transparent)
                            }
                    Text(
                            "$marker ${line.text}",
                            fontSize = 10.sp,
                            lineHeight = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            color = color,
                            softWrap = false,
                            modifier =
                                    Modifier.background(tint).padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
}

/**
 * Confirmation that an edit landed, with the only control that matters afterwards.
 *
 * Shown after the fact rather than before, because the edit is already visible in the editor
 * behind this panel — the question is no longer "do you want this?" but "keep it?".
 */
@Composable
private fun AppliedBar(fileName: String?, onUndo: () -> Unit, onDismiss: () -> Unit) {
    Row(
            modifier =
                    Modifier.fillMaxWidth()
                            .background(AppColors.AccentGreen.copy(alpha = 0.12f))
                            .padding(start = 10.dp, end = 2.dp, top = 2.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
                "Applied to ${fileName ?: "the buffer"}",
                fontSize = 11.sp,
                color = AppColors.AccentGreen,
                modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onUndo) {
            Text("Undo", fontSize = 11.sp, color = AppColors.AccentOrange)
        }
        Box(modifier = Modifier.clickable { onDismiss() }.padding(horizontal = 8.dp)) {
            Text("✕", fontSize = 12.sp, color = AppColors.TextMuted)
        }
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
        // No apply button: code from an edit is already in the file by the time it renders here.
        Text(
                segment.lang ?: "code",
                fontSize = 9.sp,
                color = AppColors.TextMuted,
                modifier = Modifier.padding(start = 8.dp, top = 4.dp)
        )
        Text(
                segment.code,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 14.sp,
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
 * Input row. One line, buttons inline — the panel is short in landscape and a stacked field plus a
 * button row was eating a third of it.
 *
 * One action, and it edits: this is an assistant attached to an open file, so "add error handling"
 * means do it. The reply is parsed and written into the buffer without asking — an Ask-vs-Edit
 * choice just made the user pick a mode before every message to get the only outcome they wanted.
 */
@Composable
private fun Composer(
        input: String,
        onInputChange: (String) -> Unit,
        isLoading: Boolean,
        onStop: () -> Unit,
        onSend: (asEdit: Boolean) -> Unit
) {
    Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
    ) {
        KeyboardRoutedInput(
                value = input,
                onValueChange = onInputChange,
                placeholder = "Ask, or describe an edit…",
                modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(6.dp))

        // While a response streams the useful control is "stop", not a spinner — the text arriving
        // on screen is already the progress indicator, and a long generation the user no longer
        // wants is otherwise unstoppable and still billable on their key.
        if (isLoading) {
            CircularProgressIndicator(
                    modifier = Modifier.height(14.dp).width(14.dp),
                    strokeWidth = 2.dp,
                    color = AppColors.AccentBlue
            )
            Spacer(Modifier.width(4.dp))
            TextButton(
                    onClick = onStop,
                    contentPadding =
                            androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
            ) { Text("Stop", fontSize = 11.sp, color = AppColors.AccentRed) }
        } else {
            Button(
                    onClick = { onSend(true) },
                    enabled = input.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.AccentBlue),
                    contentPadding =
                            androidx.compose.foundation.layout.PaddingValues(
                                    horizontal = 16.dp,
                                    vertical = 2.dp
                            ),
                    modifier = Modifier.height(30.dp)
            ) { Text("Send", fontSize = 11.sp, color = Color.White) }
        }
    }
}

/**
 * Text field wired to Beetle's own split keyboard instead of the system IME.
 *
 * The editor screen suppresses the soft keyboard, so a Compose `TextField` here would either
 * summon an IME the rest of the screen is built to avoid, or accept no input at all. A native
 * [EditText] with `showSoftInputOnFocus = false` publishes its [android.view.inputmethod.InputConnection]
 * to [Ic], which is the same channel [com.bsbeetle.bananaspaghetti.viewmodel.KeyboardViewModel]
 * already prefers over the WebView when something native holds focus — so the split keyboard types
 * here with no changes to the keyboard itself.
 *
 * Clearing [Ic.ic] on dispose matters: leaving a dead connection published would send every
 * subsequent keystroke to a detached view instead of back to the editor.
 */
@Composable
private fun KeyboardRoutedInput(
        value: String,
        onValueChange: (String) -> Unit,
        placeholder: String,
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
                    modifier.height(36.dp)
                            .background(AppColors.BgInput, RoundedCornerShape(6.dp))
                            .border(1.dp, AppColors.BorderLight, RoundedCornerShape(6.dp))
    ) {
        AndroidView(
                factory = { ctx ->
                    EditText(ctx).apply {
                        showSoftInputOnFocus = false // Beetle's own keyboard drives this field.
                        setTextColor(android.graphics.Color.WHITE)
                        textSize = 12f
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        setPadding(20, 8, 20, 8)
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
                                        onValueChange(s?.toString() ?: "")
                                    }
                                    override fun afterTextChanged(s: Editable?) {}
                                }
                        )

                        setOnFocusChangeListener { _, hasFocus ->
                            if (hasFocus) {
                                post { Ic.ic = onCreateInputConnection(EditorInfo()) }
                            } else {
                                Ic.ic = null
                            }
                        }
                        post { requestFocus() }
                    }
                },
                update = { editText ->
                    if (editText.text.toString() != value) {
                        val selectionStart = editText.selectionStart
                        editText.setText(value)
                        runCatching {
                                    editText.setSelection(
                                            kotlin.math.min(selectionStart, value.length)
                                    )
                                }
                                .onFailure { editText.setSelection(value.length) }
                    }
                },
                modifier = Modifier.fillMaxSize()
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Markdown-lite: just enough to tell code from prose
// ---------------------------------------------------------------------------------------------

private sealed interface ChatSegment {
    data class Prose(val text: String) : ChatSegment

    data class Code(val lang: String?, val code: String) : ChatSegment
}

/**
 * Splits an assistant turn on ``` fences.
 *
 * Runs on every recomposition of a streaming message, so it is a single linear pass with no regex.
 * An unterminated fence — the normal state mid-stream — is treated as code that simply has not
 * finished arriving, which is what makes code render as code while it streams rather than snapping
 * from prose to a block at the end.
 */
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
        segments +=
                if (inCode) ChatSegment.Code(lang, content)
                else ChatSegment.Prose(content.trim())
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

// ---------------------------------------------------------------------------------------------
// Editor <-> AI plumbing
// ---------------------------------------------------------------------------------------------

private const val TAG = "AiChatPanel"

private fun activeEditorSession(): EditorSession? =
        SessionList.sessions.getOrNull(SessionList.activeSessionIndex ?: -1) as? EditorSession

/** Read the live content of the active editor (async JS bridge), then deliver it to [onResult]. */
private fun readActiveEditorContent(onResult: (content: String?) -> Unit) {
    val session = activeEditorSession()
    if (session?.view != null) {
        session.view!!.evaluateJavascript("window.editor?.state?.doc?.toString() || ''") { result ->
            onResult(unescapeJsString(result))
        }
    } else {
        onResult(null)
    }
}

/** Write accepted content back into the editor and save it to disk. */
private fun applyEdit(
        context: android.content.Context,
        bufferVm: com.bsbeetle.bananaspaghetti.viewmodel.BufferViewModel,
        edit: AiChatViewModel.PendingEdit,
        target: EditorSession?
) {
    writeToEditor(edit.proposedContent, target)
    persist(context, bufferVm, edit.filePath, edit.proposedContent)
}

/** Writes into [target] only while that editor is still open. It never falls back to another one. */
private fun writeToEditor(content: String, target: EditorSession?) {
    val session = target?.takeIf { it in SessionList.sessions } ?: return
    val quoted = JSONObject.quote(content)
    session.view?.evaluateJavascript(
            "if (window.editor) { window.editor.dispatch({changes:{from:0,to:window.editor.state.doc.length,insert:$quoted}}); }",
            null
    )
}

/**
 * Persist to disk when we know where the buffer came from.
 *
 * A blank or unparseable path means an unsaved scratch buffer: the editor still gets the content,
 * and the user saves it wherever they like. Failing the whole apply in that case would make the
 * assistant useless on exactly the throwaway files people try it on first.
 */
private fun persist(
        context: android.content.Context,
        bufferVm: com.bsbeetle.bananaspaghetti.viewmodel.BufferViewModel,
        path: String?,
        content: String
) {
    if (path.isNullOrBlank()) return
    try {
        val uri = Uri.parse(path)
        context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
            out.write(content.toByteArray(Charsets.UTF_8))
            out.flush()
        }
        bufferVm.markAsClean(path)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to save AI edit to $path", e)
    }
}

/** evaluateJavascript returns a JSON-quoted string; turn it back into the real content. */
private fun unescapeJsString(result: String?): String? =
        com.bsbeetle.bananaspaghetti.utils.webViewHandling.JsResult.decodeString(result)
