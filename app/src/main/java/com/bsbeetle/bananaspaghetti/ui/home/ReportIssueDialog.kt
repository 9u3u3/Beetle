package com.bsbeetle.bananaspaghetti.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bsbeetle.bananaspaghetti.ui.theme.KarmaticFont
import com.bsbeetle.bananaspaghetti.utils.diagnostics.GitHubIssues

/**
 * Report Issue dialog. It opens the bug report form of the GitHub repository in the browser,
 * with the text below and the device and app details filled in. The user attaches screenshots
 * and submits the issue on GitHub.
 */
@Composable
fun ReportIssueDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var steps by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedBorderColor = Color(0xFFFF4444),
        unfocusedBorderColor = Color(0xFF3C3C3C),
        focusedContainerColor = Color(0xFF2A2A2A),
        unfocusedContainerColor = Color(0xFF2A2A2A),
        cursorColor = Color.White
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Report Issue",
                        fontFamily = KarmaticFont,
                        fontSize = 24.sp,
                        color = Color(0xFFFF4444)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Beetle opens a new issue on GitHub with this text. " +
                        "Sign in to GitHub, add screenshots if you have them, and submit the issue.",
                    color = Color(0xFF9E9E9E),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                FieldLabel("Title")
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors,
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    placeholder = { Text("A short summary", color = Color(0xFF666666)) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                FieldLabel("What happened?")
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    colors = fieldColors,
                    shape = RoundedCornerShape(8.dp),
                    placeholder = { Text("What you saw and what you expected", color = Color(0xFF666666)) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                FieldLabel("Steps to reproduce")
                OutlinedTextField(
                    value = steps,
                    onValueChange = { steps = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    colors = fieldColors,
                    shape = RoundedCornerShape(8.dp),
                    placeholder = { Text("1. Open ...\n2. Tap ...", color = Color(0xFF666666)) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF2A2A2A), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Also sent: ${GitHubIssues.deviceInfo()}, Beetle ${GitHubIssues.appVersion()}",
                        color = Color(0xFFBBBBBB),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }

                if (error != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = error!!, color = Color(0xFFFF4444), fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (title.isBlank() || description.isBlank()) {
                            error = "Please enter a title and tell us what happened"
                            return@Button
                        }
                        val url = GitHubIssues.bugReportUrl(
                            title = title.trim(),
                            fields = mapOf(
                                "what-happened" to description.trim(),
                                "steps" to steps.trim(),
                                "device" to GitHubIssues.deviceInfo(),
                                "app-version" to GitHubIssues.appVersion()
                            )
                        )
                        if (GitHubIssues.open(context, url)) {
                            onDismiss()
                        } else {
                            error = "No browser found to open GitHub"
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4444)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Open on GitHub",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        color = Color(0xFFBBBBBB),
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium
    )
    Spacer(modifier = Modifier.height(6.dp))
}
