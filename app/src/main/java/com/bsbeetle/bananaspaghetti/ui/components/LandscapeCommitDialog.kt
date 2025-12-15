package com.bsbeetle.bananaspaghetti.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

// Theme colors
private val BgDark = Color(0xFF1E1E1E)
private val BgCard = Color(0xFF252526)
private val AccentBlue = Color(0xFF007ACC)
private val TextPrimary = Color(0xFFE1E1E1)
private val TextSecondary = Color(0xFFAAAAAA)
private val TextTertiary = Color(0xFF666666)
private val BorderColor = Color(0xFF333333)

@Composable
fun LandscapeCommitDialog(
    isCommitAndPush: Boolean = false,
    selectedFilesCount: Int,
    onDismiss: () -> Unit,
    onCommit: (message: String) -> Unit,
    onKeyboardInput: (String) -> Unit = {}
) {
    var commitMessage by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val view = LocalView.current

    // Suppress system IME
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.hide()
    }

    // Keep IME hidden
    DisposableEffect(view) {
        onDispose { }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .background(BgCard, RoundedCornerShape(12.dp))
                .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            // Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (isCommitAndPush) "Commit & Push" else "Commit",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onDismiss) {
                    Icon(
                        Icons.Rounded.Close,
                        null,
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Selected files count
            Text(
                "$selectedFilesCount file(s) selected",
                color = TextSecondary,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Commit message input
            BasicTextField(
                value = commitMessage,
                onValueChange = { 
                    commitMessage = it
                    // Note: In a real implementation, this would route through custom keyboard
                },
                textStyle = TextStyle(
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace
                ),
                cursorBrush = SolidColor(AccentBlue),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .focusRequester(focusRequester)
                    .background(BgDark, RoundedCornerShape(6.dp))
                    .border(1.dp, BorderColor, RoundedCornerShape(6.dp))
                    .padding(10.dp),
                decorationBox = { innerTextField ->
                    Box {
                        if (commitMessage.isEmpty()) {
                            Text(
                                "Enter commit message...",
                                color = TextTertiary,
                                fontSize = 14.sp
                            )
                        }
                        innerTextField()
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Cancel Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(44.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BgDark,
                        contentColor = TextSecondary
                    ),
                    border = BorderStroke(1.dp, BorderColor),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("Cancel", fontSize = 13.sp)
                }

                // Commit Button
                Button(
                    onClick = {
                        if (commitMessage.isNotBlank()) {
                            onCommit(commitMessage)
                        }
                    },
                    modifier = Modifier.weight(1f).height(44.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentBlue,
                        disabledContainerColor = AccentBlue.copy(alpha = 0.5f)
                    ),
                    enabled = commitMessage.isNotBlank(),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(
                        if (isCommitAndPush) Icons.Rounded.CloudUpload else Icons.Rounded.Check,
                        null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (isCommitAndPush) "Commit & Push" else "Commit",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
