package com.bsbeetle.bananaspaghetti.directGit.ui

import android.util.Log
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

// Uses GitTheme for colors (same package)

@Composable
fun SshSetupDialog(viewModel: GitViewModel, onDismiss: () -> Unit, onComplete: () -> Unit) {
    // Use Ubuntu SSH Key Manager for ED25519 keys
    val ubuntuSshKeyManager = viewModel.getUbuntuSshKeyManager()
    val ubuntuGitHelper = viewModel.getUbuntuGitHelper()
    val oldSshKeyManager = viewModel.getSshKeyManager() // Old RSA manager

    var username by remember { mutableStateOf("") }
    var publicKey by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var errorMessage: String? by remember { mutableStateOf(null) }
    var hasOldKeys by remember { mutableStateOf(false) }
    var hasNewKeys by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    val uiState by viewModel.uiState.collectAsState()

    // Check for existing keys when dialog opens
    LaunchedEffect(Unit) {
        hasOldKeys = oldSshKeyManager.hasKeys()
        hasNewKeys = ubuntuSshKeyManager.hasKeys()

        Log.d("SshSetupDialog", "🔍 Checking existing keys:")
        Log.d("SshSetupDialog", "  - Old RSA keys exist: $hasOldKeys")
        Log.d("SshSetupDialog", "  - New ED25519 keys exist: $hasNewKeys")

        if (hasNewKeys) {
            val key = ubuntuSshKeyManager.getPublicKey()
            Log.d("SshSetupDialog", "  - ED25519 public key: ${key?.take(50)}...")
            publicKey = key ?: ""
        } else if (hasOldKeys) {
            val key = oldSshKeyManager.getPublicKey()
            Log.d("SshSetupDialog", "  - RSA public key: ${key?.take(50)}...")
            Log.w("SshSetupDialog", "⚠️ WARNING: Old RSA keys found! User should delete these.")
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
                modifier = Modifier.width(600.dp).heightIn(max = 700.dp),
                shape = RoundedCornerShape(12.dp),
                color = GitTheme.BgDark,
                border = BorderStroke(1.dp, GitTheme.BorderColor)
        ) {
            Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
                // Header
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                            "SSH Key Setup (ED25519)",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = GitTheme.TextPrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, null, tint = GitTheme.TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Instructions
                Text(
                        "Set up ED25519 SSH authentication for Git operations",
                        fontSize = 14.sp,
                        color = GitTheme.TextSecondary
                )

                // Warning if old keys exist
                if (hasOldKeys) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0x33F48771),
                            shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                    Icons.Rounded.Warning,
                                    null,
                                    tint = Color(0xFFF48771),
                                    modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                    "Old RSA keys detected! Delete them to generate new ED25519 keys.",
                                    fontSize = 12.sp,
                                    color = Color(0xFFF48771)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Delete old keys button (if they exist)
                if (hasOldKeys || hasNewKeys) {
                    Button(
                            onClick = {
                                isDeleting = true
                                errorMessage = null

                                CoroutineScope(Dispatchers.IO).launch {
                                    try {
                                        Log.d("SshSetupDialog", "🗑️ Deleting all keys...")
                                        oldSshKeyManager.deleteKeys()
                                        ubuntuSshKeyManager.deleteKeys()

                                        withContext(Dispatchers.Main) {
                                            hasOldKeys = false
                                            hasNewKeys = false
                                            publicKey = ""
                                            isDeleting = false
                                            Log.d("SshSetupDialog", "✅ All keys deleted")
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            errorMessage = "Failed to delete keys: ${e.message}"
                                            isDeleting = false
                                        }
                                    }
                                }
                            },
                            enabled = !isDeleting && !isGenerating,
                            modifier = Modifier.fillMaxWidth(),
                            colors =
                                    ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFF48771),
                                            contentColor = Color.White
                                    )
                    ) {
                        if (isDeleting) {
                            CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Deleting...")
                        } else {
                            Icon(Icons.Rounded.Delete, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Delete All Keys")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Generate Keys Button
                Button(
                        onClick = {
                            isGenerating = true
                            errorMessage = null

                            Log.d("SshSetupDialog", "🔑 Generating ED25519 keys...")

                            // Run key generation on background thread
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    // Generate ED25519 keys using Ubuntu
                                    val success = ubuntuSshKeyManager.generateKeyPair()

                                    withContext(Dispatchers.Main) {
                                        if (success) {
                                            publicKey = ubuntuSshKeyManager.getPublicKey() ?: ""
                                            hasNewKeys = true

                                            Log.d("SshSetupDialog", "✅ ED25519 keys generated!")
                                            Log.d(
                                                    "SshSetupDialog",
                                                    "Public key: ${publicKey.take(50)}..."
                                            )

                                            // Auto-setup keys in Ubuntu
                                            CoroutineScope(Dispatchers.IO).launch {
                                                val privateKey =
                                                        ubuntuSshKeyManager.getPrivateKeyPEM()
                                                if (privateKey != null && publicKey.isNotEmpty()) {
                                                    ubuntuGitHelper.setupSshKeys(
                                                            privateKey,
                                                            publicKey
                                                    )
                                                }
                                            }

                                            errorMessage = null
                                        } else {
                                            errorMessage = "Failed to generate ED25519 SSH keys"
                                            Log.e("SshSetupDialog", "❌ Failed to generate keys")
                                        }
                                        isGenerating = false
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        errorMessage = "Error: ${e.message}"
                                        isGenerating = false
                                        Log.e("SshSetupDialog", "❌ Error generating keys", e)
                                    }
                                }
                            }
                        },
                        enabled = !isGenerating && !isDeleting,
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                                ButtonDefaults.buttonColors(
                                        containerColor = GitTheme.AccentBlue,
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
                        Text("Generate ED25519 SSH Keys")
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
                            color = GitTheme.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    SelectionContainer {
                        Box(
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .background(GitTheme.BgCard, RoundedCornerShape(6.dp))
                                                .border(1.dp, GitTheme.BorderColor, RoundedCornerShape(6.dp))
                                                .padding(12.dp)
                        ) {
                            Text(
                                    publicKey,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = GitTheme.AccentGreen,
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
                                            containerColor = GitTheme.BgHover,
                                            contentColor = GitTheme.TextPrimary
                                    ),
                            border = BorderStroke(1.dp, GitTheme.BorderColor)
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
                            color = GitTheme.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = GitTheme.AccentBlue.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, GitTheme.AccentBlue.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                    "1. Go to GitHub → Settings → SSH and GPG keys",
                                    fontSize = 12.sp,
                                    color = GitTheme.TextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("2. Click 'New SSH key'", fontSize = 12.sp, color = GitTheme.TextSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                    "3. Paste the public key above",
                                    fontSize = 12.sp,
                                    color = GitTheme.TextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("4. Click 'Add SSH key'", fontSize = 12.sp, color = GitTheme.TextSecondary)
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
                                            containerColor = GitTheme.AccentGreen,
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
