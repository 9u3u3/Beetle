package com.bsbeetle.bananaspaghetti.directGit.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bsbeetle.bananaspaghetti.directGit.ui.GitTheme

/**
 * Dialog for authentication (username/token).
 */
@Composable
fun AuthDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Boolean) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }
    var save by remember { mutableStateOf(true) }

    StyledDialog(title = "Authentication Required", onDismiss = onDismiss) {
        Text("Enter your GitHub credentials to continue.", color = GitTheme.TextSecondary, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
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
            value = token,
            onValueChange = { token = it },
            label = { Text("Personal Access Token") },
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = save,
                onCheckedChange = { save = it },
                colors = CheckboxDefaults.colors(checkedColor = GitTheme.AccentBlue)
            )
            Text("Save credentials securely", color = GitTheme.TextSecondary, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { onConfirm(username, token, save) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = GitTheme.AccentBlue),
            shape = RoundedCornerShape(6.dp)
        ) { Text("Authenticate") }
    }
}

/**
 * Dialog for remote URL settings.
 */
@Composable
fun RemoteDialog(
    currentUrl: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var url by remember { mutableStateOf(currentUrl) }
    StyledDialog(title = "Remote Settings", onDismiss = onDismiss) {
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("Remote URL") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GitTheme.AccentBlue,
                unfocusedBorderColor = GitTheme.BorderColor,
                focusedLabelColor = GitTheme.AccentBlue,
                unfocusedLabelColor = GitTheme.TextTertiary
            )
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { onConfirm(url) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = GitTheme.AccentBlue),
            shape = RoundedCornerShape(6.dp)
        ) { Text("Save Remote") }
    }
}
