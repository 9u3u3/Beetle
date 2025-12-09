package com.bsbeetle.bananaspaghetti.directGit.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.bsbeetle.bananaspaghetti.directGit.viewmodel.GitViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Theme colors (matching GitTab)
private val BgDark = Color(0xFF1E1E1E)
private val BgCard = Color(0xFF252526)
private val BgHover = Color(0xFF2D2D2D)
private val AccentBlue = Color(0xFF007ACC)
private val AccentGreen = Color(0xFF73C991)
private val TextPrimary = Color(0xFFE1E1E1)
private val TextSecondary = Color(0xFFAAAAAA)
private val TextTertiary = Color(0xFF666666)
private val BorderColor = Color(0xFF333333)

@Composable
fun SshSetupDialog(viewModel: GitViewModel, onDismiss: () -> Unit, onComplete: () -> Unit) {
    val sshKeyManager = viewModel.getSshKeyManager()
    var username by remember { mutableStateOf("") }
    var publicKey by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    var errorMessage: String? by remember { mutableStateOf(null) }
    val clipboardManager = LocalClipboardManager.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
                modifier = Modifier.width(600.dp).heightIn(max = 700.dp),
                shape = RoundedCornerShape(12.dp),
                color = BgDark,
                border = BorderStroke(1.dp, BorderColor)
        ) {
            Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
                // Header
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                            "SSH Key Setup",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, null, tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Instructions
                Text(
                        "Set up SSH authentication for Git operations",
                        fontSize = 14.sp,
                        color = TextSecondary
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Step 1: GitHub Username
                Text(
                        "Step 1: Enter your GitHub username",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("GitHub Username") },
                        placeholder = { Text("your-username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                                OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AccentBlue,
                                        unfocusedBorderColor = BorderColor,
                                        focusedLabelColor = AccentBlue,
                                        unfocusedLabelColor = TextTertiary,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                )
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Step 2: Generate Keys
                Text(
                        "Step 2: Generate SSH keys",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                        onClick = {
                            if (username.isBlank()) {
                                errorMessage = "Please enter your GitHub username first"
                                return@Button
                            }
                            isGenerating = true
                            errorMessage = null

                            // Run key generation on background thread to avoid ANR
                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)
                                    .launch {
                                        try {
                                            sshKeyManager.saveUsername(username)
                                            val success = sshKeyManager.generateKeyPair()

                                            kotlinx.coroutines.withContext(
                                                    kotlinx.coroutines.Dispatchers.Main
                                            ) {
                                                if (success) {
                                                    publicKey = sshKeyManager.getPublicKey() ?: ""
                                                    errorMessage = null
                                                } else {
                                                    errorMessage = "Failed to generate SSH keys"
                                                }
                                                isGenerating = false
                                            }
                                        } catch (e: Exception) {
                                            kotlinx.coroutines.withContext(
                                                    kotlinx.coroutines.Dispatchers.Main
                                            ) {
                                                errorMessage = "Error: ${e.message}"
                                                isGenerating = false
                                            }
                                        }
                                    }
                        },
                        enabled = !isGenerating && username.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                                ButtonDefaults.buttonColors(
                                        containerColor = AccentBlue,
                                        contentColor = Color.White
                                )
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generating...")
                    } else {
                        Icon(Icons.Rounded.Key, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generate SSH Keys")
                    }
                }

                // Error message
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(errorMessage!!, fontSize = 12.sp, color = Color(0xFFF48771))
                }

                // Public key display
                if (publicKey.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                            "Step 3: Copy your public key",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    SelectionContainer {
                        Box(
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .background(BgCard, RoundedCornerShape(6.dp))
                                                .border(1.dp, BorderColor, RoundedCornerShape(6.dp))
                                                .padding(12.dp)
                        ) {
                            Text(
                                    publicKey,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = AccentGreen,
                                    lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                            onClick = { clipboardManager.setText(AnnotatedString(publicKey)) },
                            modifier = Modifier.fillMaxWidth(),
                            colors =
                                    ButtonDefaults.buttonColors(
                                            containerColor = BgHover,
                                            contentColor = TextPrimary
                                    ),
                            border = BorderStroke(1.dp, BorderColor)
                    ) {
                        Icon(Icons.Rounded.ContentCopy, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Copy to Clipboard")
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Step 4: Add to GitHub
                    Text(
                            "Step 4: Add to GitHub",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = AccentBlue.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, AccentBlue.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                    "1. Go to GitHub → Settings → SSH and GPG keys",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("2. Click 'New SSH key'", fontSize = 12.sp, color = TextSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                    "3. Paste the public key above",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("4. Click 'Add SSH key'", fontSize = 12.sp, color = TextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Done button
                    Button(
                            onClick = {
                                onComplete()
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors =
                                    ButtonDefaults.buttonColors(
                                            containerColor = AccentGreen,
                                            contentColor = Color.White
                                    )
                    ) {
                        Icon(Icons.Rounded.CheckCircle, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Done - I've added the key to GitHub")
                    }
                }
            }
        }
    }
}
