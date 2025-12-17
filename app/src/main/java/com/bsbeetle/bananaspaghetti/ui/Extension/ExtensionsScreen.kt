package com.bsbeetle.bananaspaghetti.ui.Extension

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.bsbeetle.bananaspaghetti.R

@Composable
fun ExtensionsScreen(navController: NavController, viewModel: ExtensionsViewModel = viewModel()) {
    val extensions by viewModel.extensions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    var selectedExtensionId by remember { mutableStateOf<Int?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    
    // Filter extensions based on search query
    val filteredExtensions = remember(extensions, searchQuery) {
        if (searchQuery.isBlank()) {
            extensions
        } else {
            extensions.filter { ext ->
                ext.name.contains(searchQuery, ignoreCase = true) ||
                ext.description.contains(searchQuery, ignoreCase = true) ||
                ext.slug.contains(searchQuery, ignoreCase = true) ||
                ext.author.contains(searchQuery, ignoreCase = true)
            }
        }
    }
    
    // Derive the selected extension from the live list to ensure updates are reflected
    val selectedExtension = remember(extensions, selectedExtensionId) {
        extensions.find { it.id == selectedExtensionId }
    }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    // Observe restart snackbar state
    val showRestartSnackbar by viewModel.showRestartSnackbar.collectAsState()

    // Handle back navigation - in portrait mode, go back to list first if detail is showing
    BackHandler {
        if (!isLandscape && selectedExtensionId != null) {
            selectedExtensionId = null
        } else {
            navController.popBackStack()
        }
    }

    // SVG Image Loader
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                add(SvgDecoder.Factory())
            }
            .build()
    }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
        }
    }
    
    // Show restart snackbar when extension is installed
    LaunchedEffect(showRestartSnackbar) {
        if (showRestartSnackbar) {
            snackbarHostState.showSnackbar(
                message = "Restart the app for extension to take effect",
                duration = SnackbarDuration.Long
            )
            viewModel.dismissRestartSnackbar()
        }
    }
    
    // Installation dialog state
    val showInstallDialog by viewModel.showInstallDialog.collectAsState()
    val installingExtensionName by viewModel.installingExtensionName.collectAsState()
    val installationOutput by viewModel.installationOutput.collectAsState()
    val installationComplete by viewModel.installationComplete.collectAsState()
    val installationError by viewModel.installationError.collectAsState()
    
    // Installation Progress Dialog
    if (showInstallDialog) {
        InstallationDialog(
            extensionName = installingExtensionName,
            outputLines = installationOutput,
            isComplete = installationComplete,
            isError = installationError,
            onDismiss = { viewModel.dismissInstallDialog() }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        if (isLandscape) {
            // LANDSCAPE LAYOUT - Sidebar + Details Panel
            Row(
                Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1E1E1E))
                    .padding(paddingValues)
            ) {
                // SIDEBAR
                Column(
                    modifier = Modifier
                        .width(300.dp)
                        .fillMaxHeight()
                        .background(Color(0xFF252526))
                ) {
                    Text(
                        text = "EXTENSIONS",
                        color = Color(0xFFBBBBBB),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(16.dp)
                    )

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search Extensions", color = Color(0xFF858585), fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF007ACC),
                            unfocusedBorderColor = Color(0xFF3C3C3C),
                            focusedContainerColor = Color(0xFF3C3C3C),
                            unfocusedContainerColor = Color(0xFF3C3C3C),
                            cursorColor = Color.White
                        ),
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                        shape = RoundedCornerShape(4.dp)
                    )

                    if (isLoading) {
                        Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color(0xFF007ACC), modifier = Modifier.size(24.dp))
                        }
                    } else {
                        ExtensionList(
                            extensions = filteredExtensions,
                            selectedExtensionId = selectedExtensionId,
                            imageLoader = imageLoader,
                            onExtensionClick = { selectedExtensionId = it.id }
                        )
                    }
                }

                // DETAILS PANEL
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .background(Color(0xFF1E1E1E))
                ) {
                    if (selectedExtension == null) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Image(
                                painter = painterResource(id = R.drawable.ascii_beetle),
                                contentDescription = null,
                                alpha = 0.1f,
                                modifier = Modifier.size(200.dp)
                            )
                        }
                    } else {
                        ExtensionDetails(
                            extension = selectedExtension,
                            imageLoader = imageLoader,
                            onInstall = { viewModel.installExtension(it) },
                            onUninstall = { viewModel.uninstallExtension(it) }
                        )
                    }
                }
            }
        } else {
            // PORTRAIT LAYOUT - Full screen list or detail
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1E1E1E))
                    .padding(paddingValues)
            ) {
                if (selectedExtension == null) {
                    // List View
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF252526))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Extensions",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search Extensions", color = Color(0xFF858585)) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF007ACC),
                            unfocusedBorderColor = Color(0xFF3C3C3C),
                            focusedContainerColor = Color(0xFF3C3C3C),
                            unfocusedContainerColor = Color(0xFF3C3C3C),
                            cursorColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    if (isLoading) {
                        Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color(0xFF007ACC), modifier = Modifier.size(32.dp))
                        }
                    } else {
                        ExtensionList(
                            extensions = filteredExtensions,
                            selectedExtensionId = null,
                            imageLoader = imageLoader,
                            onExtensionClick = { selectedExtensionId = it.id }
                        )
                    }
                } else {
                    // Detail View
                    // Header with back button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF252526))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { selectedExtensionId = null },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = selectedExtension.name,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Extension Details
                    PortraitExtensionDetails(
                        extension = selectedExtension,
                        imageLoader = imageLoader,
                        onInstall = { viewModel.installExtension(it) },
                        onUninstall = { viewModel.uninstallExtension(it) }
                    )
                }
            }
        }
    }
}

@Composable
fun SidebarExtensionItem(
    extension: ExtensionModel,
    isSelected: Boolean,
    imageLoader: ImageLoader,
    onClick: () -> Unit
) {
    val bg = if (isSelected) Color(0xFF37373D) else Color.Transparent

    Row(
        Modifier
            .fillMaxWidth()
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // Icon
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(extension.icon)
                .crossfade(true)
                .error(R.drawable.extension) // Fallback
                .placeholder(R.drawable.extension)
                .build(),
            imageLoader = imageLoader,
            contentDescription = null,
            modifier = Modifier
                .size(42.dp)
                .padding(top = 4.dp)
        )

        Spacer(Modifier.width(12.dp))

        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    extension.name,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Text(
                extension.description,
                color = Color(0xFFBBBBBB),
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 14.sp
            )
            
            Spacer(Modifier.height(4.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(extension.author, color = Color(0xFF9C9C9C), fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun ExtensionDetails(
    extension: ExtensionModel,
    imageLoader: ImageLoader,
    onInstall: (ExtensionModel) -> Unit,
    onUninstall: (ExtensionModel) -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(verticalAlignment = Alignment.Top) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(extension.icon)
                    .crossfade(true)
                    .error(R.drawable.extension)
                    .build(),
                imageLoader = imageLoader,
                contentDescription = null,
                modifier = Modifier.size(120.dp)
            )
            
            Spacer(Modifier.width(24.dp))
            
            Column {
                Text(extension.name, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(extension.slug, color = Color(0xFF666666), fontSize = 14.sp)
                    Spacer(Modifier.width(12.dp))
                    Text("v${extension.version}", color = Color(0xFFBBBBBB), fontSize = 14.sp)
                }
                
                Spacer(Modifier.height(8.dp))
                
                Text(extension.author, color = Color(0xFF4EC9B0), fontSize = 14.sp)
                
                Spacer(Modifier.height(16.dp))
                
                Text(extension.description, color = Color(0xFFCCCCCC), fontSize = 15.sp)
                
                Spacer(Modifier.height(20.dp))
                
                // Actions
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (extension.isInstalling) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color(0xFF007ACC),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Installing...", color = Color(0xFF007ACC), fontSize = 12.sp)
                    } else if (!extension.installed) {
                        Button(
                            onClick = { onInstall(extension) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007ACC)),
                            shape = RoundedCornerShape(2.dp)
                        ) {
                            Text("Install", color = Color.White)
                        }
                    } else {
                        Button(
                            onClick = { onUninstall(extension) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3C3C3C)),
                            shape = RoundedCornerShape(2.dp),
                            modifier = Modifier.width(100.dp)
                        ) {
                            Text("Uninstall", color = Color.White)
                        }
                        
                        Spacer(Modifier.width(8.dp))
                        
                        Button(
                            onClick = { /* TODO disable */ },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3C3C3C)),
                            shape = RoundedCornerShape(2.dp)
                        ) {
                            Text("Disable", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExtensionList(
    extensions: List<ExtensionModel>,
    selectedExtensionId: Int?,
    imageLoader: ImageLoader,
    onExtensionClick: (ExtensionModel) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
    ) {
        val installed = extensions.filter { it.installed }
        val marketplace = extensions.filter { !it.installed }

        if (installed.isNotEmpty()) {
            item {
                Text(
                    "INSTALLED",
                    color = Color(0xFFBBBBBB),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                )
            }
            items(installed) { ext ->
                SidebarExtensionItem(
                    extension = ext,
                    isSelected = ext.id == selectedExtensionId,
                    imageLoader = imageLoader,
                    onClick = { onExtensionClick(ext) }
                )
            }
        }

        if (marketplace.isNotEmpty()) {
            item {
                Text(
                    "MARKETPLACE",
                    color = Color(0xFFBBBBBB),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                )
            }
            items(marketplace) { ext ->
                SidebarExtensionItem(
                    extension = ext,
                    isSelected = ext.id == selectedExtensionId,
                    imageLoader = imageLoader,
                    onClick = { onExtensionClick(ext) }
                )
            }
        }
    }
}

@Composable
fun PortraitExtensionDetails(
    extension: ExtensionModel,
    imageLoader: ImageLoader,
    onInstall: (ExtensionModel) -> Unit,
    onUninstall: (ExtensionModel) -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Icon centered
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(extension.icon)
                    .crossfade(true)
                    .error(R.drawable.extension)
                    .build(),
                imageLoader = imageLoader,
                contentDescription = null,
                modifier = Modifier.size(80.dp)
            )
        }
        
        Spacer(Modifier.height(16.dp))
        
        // Name and version
        Text(
            extension.name,
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        
        Spacer(Modifier.height(4.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(extension.slug, color = Color(0xFF666666), fontSize = 13.sp)
            Spacer(Modifier.width(8.dp))
            Text("v${extension.version}", color = Color(0xFFBBBBBB), fontSize = 13.sp)
        }
        
        Spacer(Modifier.height(8.dp))
        
        Text(
            extension.author,
            color = Color(0xFF4EC9B0),
            fontSize = 14.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        
        Spacer(Modifier.height(20.dp))
        
        // Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            if (extension.isInstalling) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color(0xFF007ACC),
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(8.dp))
                Text("Installing...", color = Color(0xFF007ACC), fontSize = 14.sp)
            } else if (!extension.installed) {
                Button(
                    onClick = { onInstall(extension) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007ACC)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    Text("Install", color = Color.White, fontSize = 16.sp)
                }
            } else {
                Button(
                    onClick = { onUninstall(extension) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3C3C3C)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Uninstall", color = Color.White)
                }
                Spacer(Modifier.width(12.dp))
                Button(
                    onClick = { /* TODO disable */ },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3C3C3C)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Disable", color = Color.White)
                }
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        // Description
        Text(
            "Description",
            color = Color(0xFFBBBBBB),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            extension.description,
            color = Color(0xFFCCCCCC),
            fontSize = 15.sp,
            lineHeight = 22.sp
        )
    }
}