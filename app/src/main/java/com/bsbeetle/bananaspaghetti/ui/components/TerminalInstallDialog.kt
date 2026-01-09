package com.bsbeetle.bananaspaghetti.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bsbeetle.bananaspaghetti.ui.theme.AppColors
import java.io.File

/**
 * Check if Ubuntu terminal is installed by looking for bash.
 * This uses the same logic as Tenebrio.kt
 */
fun isTerminalInstalled(filesDir: File): Boolean {
    val ubuntuBash = File(filesDir, "ubuntu/bin/bash")
    return ubuntuBash.exists()
}

/**
 * Dialog shown on first app launch when terminal is not installed.
 * Informs user about terminal requirements for LSP, Git, and other features.
 */
@Composable
fun TerminalInstallDialog(
    onInstallClick: () -> Unit,
    onDismiss: () -> Unit = {}
) {
    Dialog(
        onDismissRequest = { /* Prevent dismissal by clicking outside */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(AppColors.BgSecondary)
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Warning Icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(AppColors.AccentOrange.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = AppColors.AccentOrange,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Title
                Text(
                    text = "Terminal Setup Required",
                    color = AppColors.TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                // Description
                Text(
                    text = "Beetle requires a Linux terminal environment for full functionality. This includes:",
                    color = AppColors.TextSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                // Feature list
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(AppColors.BgPrimary)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FeatureItem("LSP", "Language Server Protocol for code intelligence")
                    FeatureItem("Git", "Full Git integration with SSH support")
                    FeatureItem("Build Tools", "Compilers, package managers, and more")
                    FeatureItem("Shell", "Interactive Ubuntu Linux terminal")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Install button
                Button(
                    onClick = onInstallClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.AccentBlue
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Install Terminal",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Note
                Text(
                    text = "This will download ~30MB and install Ubuntu Linux",
                    color = AppColors.TextTertiary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun FeatureItem(title: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .offset(y = 6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(AppColors.AccentBlue)
        )
        Column {
            Text(
                text = title,
                color = AppColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                color = AppColors.TextTertiary,
                fontSize = 12.sp
            )
        }
    }
}
