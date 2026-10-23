package com.bsbeetle.bananaspaghetti.directGit.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bsbeetle.bananaspaghetti.directGit.ui.GitTheme
import java.io.File

/**
 * Dialog for cloning a Git repository.
 */
@Composable
fun CloneDialog(
    onDismiss: () -> Unit,
    onClone: (String, String) -> Unit
) {
    val context = LocalContext.current
    var url by remember { mutableStateOf("") }
    var targetPath by remember {
        mutableStateOf(File(context.filesDir, "ubuntu/home/").absolutePath + "/")
    }

    StyledDialog(title = "Clone Repository", onDismiss = onDismiss) {
        Text(
            "Clone a Git repository using SSH authentication.",
            color = GitTheme.TextSecondary,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = url,
            onValueChange = { newUrl ->
                url = newUrl
                if (newUrl.isNotBlank()) {
                    val repoName = newUrl.substringAfterLast("/").removeSuffix(".git")
                    if (repoName.isNotBlank()) {
                        targetPath = File(context.filesDir, "ubuntu/home/$repoName").absolutePath
                    }
                }
            },
            label = { Text("Repository URL") },
            placeholder = { Text("git@github.com:user/repo.git") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GitTheme.AccentBlue,
                unfocusedBorderColor = GitTheme.BorderColor,
                focusedLabelColor = GitTheme.AccentBlue,
                unfocusedLabelColor = GitTheme.TextTertiary
            )
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = targetPath,
            onValueChange = { targetPath = it },
            label = { Text("Clone To (Full Path)") },
            placeholder = { Text(File(context.filesDir, "ubuntu/home/my-project").absolutePath) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GitTheme.AccentBlue,
                unfocusedBorderColor = GitTheme.BorderColor,
                focusedLabelColor = GitTheme.AccentBlue,
                unfocusedLabelColor = GitTheme.TextTertiary
            )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Tip: Use internal storage for better compatibility with the Ubuntu terminal",
            color = GitTheme.TextTertiary,
            fontSize = 10.sp,
            fontStyle = FontStyle.Italic
        )

        Spacer(modifier = Modifier.height(16.dp))

        // SSH Info
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(GitTheme.AccentBlue.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Info, null, tint = GitTheme.AccentBlue, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Using SSH authentication - ensure your SSH keys are configured",
                color = GitTheme.TextSecondary,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onClone(url, targetPath) },
            enabled = url.isNotBlank() && targetPath.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = GitTheme.AccentBlue),
            shape = RoundedCornerShape(6.dp)
        ) {
            Icon(Icons.Rounded.CloudDownload, null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Clone Repository")
        }
    }
}
