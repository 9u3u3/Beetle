package com.bsbeetle.bananaspaghetti.ui.screens.sessions

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.bsbeetle.bananaspaghetti.ui.navigation.Screen
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.Session
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.BrowserSession
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.DemoSession
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.EditorSession
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.SessionList
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.SessionType
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.TerminalSession
import com.bsbeetle.bananaspaghetti.viewmodel.SessionViewModel
import com.bsbeetle.bananaspaghetti.LocalFileViewModel
import com.bsbeetle.bananaspaghetti.viewmodel.FileViewModel

//NO_PORTRAIT

@Composable
fun ZoomOut(
    navController: NavController,
    viewModel: SessionViewModel = viewModel()
) {
    val fileViewModel: FileViewModel = LocalFileViewModel.current
    val bufferViewModel: com.bsbeetle.bananaspaghetti.viewmodel.BufferViewModel = com.bsbeetle.bananaspaghetti.LocalBufferViewModel.current
    var showNewSessionDialog by remember { mutableStateOf(false) }

    // Capture snapshots when screen is shown
    LaunchedEffect(Unit) {
        viewModel.captureAllSnapshots()
    }

    BackHandler {
        if (SessionList.activeSessionIndex != null && SessionList.sessions.isNotEmpty()) {
            val activeSession = SessionList.sessions.getOrNull(SessionList.activeSessionIndex!!)
            if (activeSession != null) {
                navigateToSession(navController, activeSession)
            } else {
                // No valid active session, go Home
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Home.route) { inclusive = true }
                }
            }
        } else {
            // No sessions left, go Home
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Home.route) { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { /* Settings */ },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = { showNewSessionDialog = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Session",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Session Grid - 2 columns, larger cards
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(SessionList.sessions) { session ->
                    SessionCard(
                        session = session,
                        snapshot = viewModel.sessionSnapshots[session.id],
                        onClick = {
                            SessionList.activeSessionIndex = session.id
                            // Sync file tree with session's folder when switching to EditorSession
                            if (session is EditorSession) {
                                session.projectRoot?.let { projectRoot ->
                                    fileViewModel.selectedFolderUri = projectRoot.uri
                                }
                            }
                            navigateToSession(navController, session)
                        },
                        onClose = {
                            // Clear buffer if closing an editor session
                            if (session is EditorSession) {
                                bufferViewModel.clear()
                                fileViewModel.selectedUri = null
                                fileViewModel.currentFilePath = ""
                            }
                            session.destroy()
                            viewModel.removeSnapshot(session.id)
                        }
                    )
                }
            }
        }

        // New Session Dialog
        if (showNewSessionDialog) {
            NewSessionDialog(
                onDismiss = { showNewSessionDialog = false },
                onCreateSession = { sessionType, sessionName ->
                    // Create new session
                    val finalName = if (sessionName.isBlank()) sessionType.name.lowercase().replaceFirstChar { it.uppercase() } else sessionName
                    val appContext = navController.context.applicationContext
                    
                    // Clear buffer if creating a new editor session
                    if (sessionType == SessionType.EDITOR) {
                        bufferViewModel.clear()
                        fileViewModel.selectedUri = null
                        fileViewModel.currentFilePath = ""
                    }
                    
                    when (sessionType) {
                        SessionType.EDITOR -> EditorSession(appContext, finalName)
                        SessionType.BROWSER -> BrowserSession(appContext, name = finalName)
                        SessionType.TERMINAL -> TerminalSession(appContext, finalName)
                        SessionType.DEMO -> DemoSession(appContext, finalName)
                    }
                    showNewSessionDialog = false
                }
            )
        }
    }
}

@Composable
fun SessionCard(
    session: Session,
    snapshot: android.graphics.Bitmap?,
    onClick: () -> Unit,
    onClose: () -> Unit
) {
    val isActive = SessionList.activeSessionIndex == session.id

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF252525),
                        Color(0xFF151515)
                    )
                )
            )
            .border(
                width = if (isActive) 2.dp else 1.dp,
                brush = if (isActive) Brush.linearGradient(
                    colors = listOf(Color(0xFF4A90E2), Color(0xFF9013FE))
                ) else Brush.linearGradient(
                    colors = listOf(Color(0xFF333333), Color(0xFF222222))
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
    ) {
        // Session preview (WebView snapshot)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            if (snapshot != null) {
                Image(
                    bitmap = snapshot.asImageBitmap(),
                    contentDescription = "Session preview",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0D0D0D)),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Fallback when snapshot is not available
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0D0D0D), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = when (session.type) {
                                SessionType.EDITOR -> "📝"
                                SessionType.BROWSER -> "🌐"
                                SessionType.TERMINAL -> "💻"
                                SessionType.DEMO -> "🎨"
                            },
                            fontSize = 48.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = session.name,
                            color = Color(0xFFAAAAAA),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Close button with better visibility
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .size(32.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xCC000000))
                .clickable { onClose() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }

        // Session type label with icon
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xCC000000))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        when (session.type) {
                            SessionType.EDITOR -> Color(0xFF4A90E2)
                            SessionType.BROWSER -> Color(0xFF7B68EE)
                            SessionType.TERMINAL -> Color(0xFF50C878)
                            SessionType.DEMO -> Color(0xFFFFB347)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (session.type) {
                        SessionType.EDITOR -> "E"
                        SessionType.BROWSER -> "B"
                        SessionType.TERMINAL -> "T"
                        SessionType.DEMO -> "D"
                    },
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = session.name,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewSessionDialog(
    onDismiss: () -> Unit,
    onCreateSession: (SessionType, String) -> Unit
) {
    var selectedType by remember { mutableStateOf(SessionType.TERMINAL) }
    var sessionName by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x99000000))
            .alpha(0.7f)
            .clickable(enabled = false) { },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(400.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF2A2A2A))
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "New session",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // Session Type Dropdown
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedType.name.lowercase().replaceFirstChar { it.uppercase() },
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF555555),
                            unfocusedBorderColor = Color(0xFF3A3A3A),
                            focusedContainerColor = Color(0xFF1A1A1A),
                            unfocusedContainerColor = Color(0xFF1A1A1A),
                            focusedTrailingIconColor = Color.White,
                            unfocusedTrailingIconColor = Color(0xFF888888)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(Color(0xFF1A1A1A))
                    ) {
                        SessionType.values().forEach { type ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = type.name.lowercase().replaceFirstChar { it.uppercase() },
                                        color = Color.White
                                    )
                                },
                                onClick = {
                                    selectedType = type
                                    expanded = false
                                },
                                colors = MenuDefaults.itemColors(
                                    textColor = Color.White
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Session Name Input
                OutlinedTextField(
                    value = sessionName,
                    onValueChange = { sessionName = it },
                    placeholder = {
                        Text(
                            text = "Session name",
                            color = Color(0xFF666666)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF555555),
                        unfocusedBorderColor = Color(0xFF3A3A3A),
                        focusedContainerColor = Color(0xFF1A1A1A),
                        unfocusedContainerColor = Color(0xFF1A1A1A)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF3A3A3A)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            "Cancel",
                            color = Color.White,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    Button(
                        onClick = { onCreateSession(selectedType, sessionName) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF5E5CE6)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            "Done",
                            color = Color.White,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun navigateToSession(navController: NavController, session: Session) {
    when (session.type) {
        SessionType.EDITOR -> navController.navigate(Screen.OpenFolder.route) {
            popUpTo(Screen.ZoomOut.route) { inclusive = true }
        }
        SessionType.BROWSER -> navController.navigate(Screen.Browser.route) {
            popUpTo(Screen.ZoomOut.route) { inclusive = true }
        }
        SessionType.TERMINAL -> navController.navigate(Screen.Terminal.route) {
            popUpTo(Screen.ZoomOut.route) { inclusive = true }
        }
        SessionType.DEMO -> navController.navigate(Screen.Demo.route) {
            popUpTo(Screen.ZoomOut.route) { inclusive = true }
        }
    }
}