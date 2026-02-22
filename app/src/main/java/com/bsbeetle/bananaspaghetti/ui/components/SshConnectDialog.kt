package com.bsbeetle.bananaspaghetti.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bsbeetle.bananaspaghetti.utils.ssh.SshConnectionParams
import com.bsbeetle.bananaspaghetti.viewmodel.SshViewModel

@Composable
fun SshConnectDialog(
    onDismiss: () -> Unit,
    onConnected: () -> Unit,
    sshViewModel: SshViewModel
) {
    val context = LocalContext.current
    val connecting by sshViewModel.connecting.collectAsStateWithLifecycle()
    val connected by sshViewModel.connected.collectAsStateWithLifecycle()
    val error by sshViewModel.error.collectAsStateWithLifecycle()

    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("22") }
    var username by remember { mutableStateOf("") }
    var authType by remember { mutableStateOf(SshConnectionParams.AuthType.APP_SSH_KEYS) }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(connected) {
        if (connected) {
            onConnected()
        }
    }

    Dialog(onDismissRequest = {
        sshViewModel.clearError()
        onDismiss()
    }) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF252526),
            tonalElevation = 0.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {

                Text(
                    text = "Connect to Remote",
                    color = Color(0xFFD4D4D4),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.3.sp
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFF3C3C3C), thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(16.dp))

                SshField(
                    label = "Host",
                    value = host,
                    onValueChange = { host = it },
                    placeholder = "192.168.1.100"
                )

                Spacer(modifier = Modifier.height(10.dp))

                SshField(
                    label = "Port",
                    value = port,
                    onValueChange = { port = it },
                    placeholder = "22",
                    keyboardType = KeyboardType.Number
                )

                Spacer(modifier = Modifier.height(10.dp))

                SshField(
                    label = "Username",
                    value = username,
                    onValueChange = { username = it },
                    placeholder = "user"
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "AUTHENTICATION",
                    color = Color(0xFF858585),
                    fontSize = 10.sp,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                SshAuthOption(
                    label = "Use app SSH keys",
                    description = "Keys from Settings → SSH Keys",
                    selected = authType == SshConnectionParams.AuthType.APP_SSH_KEYS,
                    onClick = { authType = SshConnectionParams.AuthType.APP_SSH_KEYS }
                )

                Spacer(modifier = Modifier.height(6.dp))

                SshAuthOption(
                    label = "Password",
                    description = "Authenticate with password",
                    selected = authType == SshConnectionParams.AuthType.PASSWORD,
                    onClick = { authType = SshConnectionParams.AuthType.PASSWORD }
                )

                if (authType == SshConnectionParams.AuthType.PASSWORD) {
                    Spacer(modifier = Modifier.height(10.dp))
                    SshField(
                        label = "Password",
                        value = password,
                        onValueChange = { password = it },
                        placeholder = "••••••••",
                        keyboardType = KeyboardType.Password,
                        visualTransformation = PasswordVisualTransformation()
                    )
                }

                error?.let { err ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = err,
                        color = Color(0xFFFF6B6B),
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = {
                        sshViewModel.clearError()
                        onDismiss()
                    }) {
                        Text("Cancel", color = Color(0xFF888888), fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            val portNum = port.toIntOrNull() ?: 22
                            sshViewModel.connect(
                                SshConnectionParams(
                                    host = host.trim(),
                                    port = portNum,
                                    username = username.trim(),
                                    authType = authType,
                                    password = password
                                ),
                                context
                            )
                        },
                        enabled = host.isNotBlank() && username.isNotBlank() && !connecting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF007ACC),
                            disabledContainerColor = Color(0xFF3C3C3C)
                        )
                    ) {
                        if (connecting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text("Connect", fontSize = 13.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun SshField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    Column {
        Text(
            text = label.uppercase(),
            color = Color(0xFF858585),
            fontSize = 10.sp,
            letterSpacing = 0.8.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = Color(0xFF555555), fontSize = 13.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = visualTransformation,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color(0xFFE8E8E8),
                unfocusedTextColor = Color(0xFFCCCCCC),
                focusedBorderColor = Color(0xFF007ACC),
                unfocusedBorderColor = Color(0xFF3C3C3C),
                cursorColor = Color(0xFF007ACC),
                focusedContainerColor = Color(0xFF2D2D2D),
                unfocusedContainerColor = Color(0xFF252526)
            ),
            shape = RoundedCornerShape(6.dp)
        )
    }
}

@Composable
private fun SshAuthOption(
    label: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .border(
                width = if (selected) 1.dp else 0.5.dp,
                color = if (selected) Color(0xFF007ACC) else Color(0xFF3C3C3C),
                shape = RoundedCornerShape(6.dp)
            )
            .background(if (selected) Color(0xFF1C3A52) else Color(0xFF2D2D2D))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            modifier = Modifier.size(18.dp),
            colors = RadioButtonDefaults.colors(
                selectedColor = Color(0xFF007ACC),
                unselectedColor = Color(0xFF555555)
            )
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(text = label, color = Color(0xFFD4D4D4), fontSize = 13.sp)
            Text(text = description, color = Color(0xFF858585), fontSize = 10.sp)
        }
    }
}
