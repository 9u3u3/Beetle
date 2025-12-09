package com.bsbeetle.bananaspaghetti.ui.Extension

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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bsbeetle.bananaspaghetti.R

@Composable
fun ExtensionsScreen() {
    var selectedExtension by remember { mutableStateOf<ExtensionModel?>(null) }

    Row(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E))
    ) {

        // SIDEBAR
        Column(
            modifier = Modifier
                .width(300.dp)
                .fillMaxHeight()
                .background(Color(0xFF252526))
        ) {
            // Sidebar Header
            Text(
                text = "EXTENSIONS",
                color = Color(0xFFBBBBBB),
                fontSize = 11.sp,
                modifier = Modifier.padding(16.dp)
            )

            // Search Bar (Dummy)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .background(Color(0xFF3C3C3C), RoundedCornerShape(2.dp))
                    .padding(8.dp)
            ) {
                Text("Search Extensions in Marketplace", color = Color(0xFF858585), fontSize = 12.sp)
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 12.dp)
            ) {
                items(ExtensionsRepo.extensions) { ext ->
                    SidebarExtensionItem(
                        extension = ext,
                        isSelected = ext.id == selectedExtension?.id,
                        onClick = { selectedExtension = ext }
                    )
                }
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
                ExtensionDetails(extension = selectedExtension!!)
            }
        }
    }
}

@Composable
fun SidebarExtensionItem(
    extension: ExtensionModel,
    isSelected: Boolean,
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
        Image(
            painter = painterResource(id = extension.iconResId),
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
                if (extension.installed) {
                    Spacer(Modifier.width(8.dp))
                    Text("Installed", color = Color(0xFF89D185), fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
fun ExtensionDetails(extension: ExtensionModel) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(verticalAlignment = Alignment.Top) {
            Image(
                painter = painterResource(id = extension.iconResId),
                contentDescription = null,
                modifier = Modifier.size(120.dp)
            )
            
            Spacer(Modifier.width(24.dp))
            
            Column {
                Text(extension.name, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(extension.id, color = Color(0xFF666666), fontSize = 14.sp)
                    Spacer(Modifier.width(12.dp))
                    Text("v${extension.version}", color = Color(0xFFBBBBBB), fontSize = 14.sp)
                }
                
                Spacer(Modifier.height(8.dp))
                
                Text(extension.author, color = Color(0xFF4EC9B0), fontSize = 14.sp)
                
                Spacer(Modifier.height(12.dp))
                
                // Stats
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⬇ ${extension.downloads}", color = Color(0xFFCCCCCC), fontSize = 12.sp)
                    Spacer(Modifier.width(16.dp))
                    Text("★ ${extension.rating}", color = Color(0xFFCCCCCC), fontSize = 12.sp)
                }
                
                Spacer(Modifier.height(16.dp))
                
                Text(extension.description, color = Color(0xFFCCCCCC), fontSize = 15.sp)
                
                Spacer(Modifier.height(20.dp))
                
                // Actions
                Row {
                    if (!extension.installed) {
                        Button(
                            onClick = { /* TODO install */ },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007ACC)),
                            shape = RoundedCornerShape(2.dp)
                        ) {
                            Text("Install", color = Color.White)
                        }
                    } else {
                        Button(
                            onClick = { /* TODO uninstall */ },
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
        
        Spacer(Modifier.height(32.dp))
        
        // Tabs
        Row {
            Text("DETAILS", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 24.dp))
            Text("FEATURES", color = Color(0xFF888888), fontSize = 13.sp, modifier = Modifier.padding(end = 24.dp))
            Text("CHANGELOG", color = Color(0xFF888888), fontSize = 13.sp)
        }
        
        HorizontalDivider(Modifier.padding(top = 8.dp, bottom = 24.dp), color = Color(0xFF333333))
        
        // Content (Readme)
        Text(
            text = """
                # ${extension.name}
                
                ${extension.description}
                
                ## Features
                - IntelliSense
                - Debugging
                - Code Navigation
                - Refactoring
                
                ## Requirements
                - VS Code 1.50.0 or higher
                
                ## Known Issues
                None.
                
                ## Release Notes
                ### ${extension.version}
                - Initial release
            """.trimIndent(),
            color = Color(0xFFCCCCCC),
            fontSize = 14.sp,
            lineHeight = 22.sp
        )
    }
}