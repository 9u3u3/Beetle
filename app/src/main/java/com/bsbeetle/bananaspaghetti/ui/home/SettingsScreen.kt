package com.bsbeetle.bananaspaghetti.ui.home

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.bsbeetle.bananaspaghetti.LocalKeyboardViewModel
import com.bsbeetle.bananaspaghetti.R
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.DemoSession
import com.bsbeetle.bananaspaghetti.viewmodel.ExpandViewModel
import com.bsbeetle.bananaspaghetti.viewmodel.KeyboardViewModel

@Composable
fun KeyboardSettings(
    navController: NavController
) {

    val keyboardViewModel: KeyboardViewModel = LocalKeyboardViewModel.current
    val uiState by keyboardViewModel.uiState.collectAsStateWithLifecycle()
    var indentValue by rememberSaveable { mutableStateOf("${uiState.indentValue}") }
    val backgroundBoxSize = 250.dp
    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()
    var tapped by rememberSaveable { mutableStateOf(false) }
    val context: Context = LocalContext.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .verticalScroll(verticalScrollState)
            .fillMaxWidth()

    ) {
        Row(
            modifier = Modifier
                .padding(25.dp)
                .horizontalScroll(horizontalScrollState),
            horizontalArrangement = Arrangement.spacedBy(80.dp)
        ) {
            Box(
                modifier = Modifier
                    .height(backgroundBoxSize)
                    .width(backgroundBoxSize)
                    .background(Color.DarkGray, shape = RoundedCornerShape(12.dp))
                    .border(
                        border = if (tapped) {
                            BorderStroke(2.dp, HighlightColor)
                        } else {
                            BorderStroke(0.dp, Color.Transparent)
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                    .shadow(2.dp, RoundedCornerShape(12.dp))
                    .padding(16.dp)
                    .clickable(onClick = { tapped = !tapped })
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                )
                {
                    Text(
                        "Text indent value",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    OutlinedTextField(
                        value = indentValue,
                        onValueChange = { newText ->
                            indentValue = newText
                            newText.toIntOrNull()?.let { num ->
                                keyboardViewModel.updateIndentSize(num)
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = Color.DarkGray,
                        ),
                        modifier = Modifier
                            .background(color = Color(0xFF2E2E2E))
                            .fillMaxWidth()
                    )
                }
            }
            Box(
                modifier = Modifier
                    .height(backgroundBoxSize)
                    .width(backgroundBoxSize)
                    .background(Color.DarkGray, shape = RoundedCornerShape(12.dp))
                    .border(BorderStroke(2.dp, HighlightColor), shape = RoundedCornerShape(12.dp))
                    .shadow(2.dp, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            )
            {
                Column(
                    modifier = Modifier
                        .padding(10.dp)
                        .height(backgroundBoxSize)
                        .width(backgroundBoxSize)
                        .background(color = Color.DarkGray)
                ) {
                    Text(
                        "Keyboard Visibility",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = uiState.isLeftKeyboardVisible, onCheckedChange = {
                            keyboardViewModel.toggleLeftKeyboardVisibility()
                        })
                        Text(
                            "Left Keyboard",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = uiState.isRightKeyboardVisible, onCheckedChange = {
                            keyboardViewModel.toggleRightKeyboardVisibility()
                        })
                        Text(
                            "Right Keyboard",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }

                }

            }
        }


        Box(
            modifier = Modifier
                .height(backgroundBoxSize)
                .width(backgroundBoxSize)
                .background(Color.DarkGray, shape = RoundedCornerShape(12.dp))
                .border(
                    BorderStroke(2.dp, HighlightColor),
                    shape = RoundedCornerShape(12.dp)
                )
                .shadow(2.dp, RoundedCornerShape(12.dp))
                .padding(16.dp)
                .clickable(onClick = { // Fix this demo thing later (not important for now)

                    DemoSession(context.applicationContext)
                    navController.navigate("demo")

                })
        ) {
            Text("Keyboard Sizing", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }

}

@Composable
fun SettingsScreen(
    navController: NavController
) {
    Row {ResponsiveScaffold(navController)}
}


@Composable
fun ResponsiveScaffold(
    navController: NavController
) {

    var selectedItem by remember { mutableStateOf<String?>("Keyboard") }

    Row(modifier = Modifier.fillMaxSize()) {
        val navItems = listOf(
            "Editor" to Icons.Default.Home,
            "Appearance" to Icons.Default.Settings,
            "Keybindings" to Icons.Default.Create,
            "Keyboard" to Icons.Default.Build
        )

        VerticalNavBar(
            items = navItems,
            onItemClick = { selected ->
                selectedItem = selected
            },
            selectedItem
        )

        // Main content area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            MainContent(selectedItem, navController)
        }
    }
}

val HighlightColor = Color(0xFF228edd)

@Composable
fun MainContent(
    selectedItem: String?,
    navController: NavController
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .paint(
                painterResource(R.drawable.mattmain),
                contentScale = ContentScale.Crop, alpha = 0.4f
            )
    ) {
        if (selectedItem == "Editor") EditorSettings()
        if (selectedItem == "Appearance") AppearenceSettings()
        if (selectedItem == "Keybindings") KeybindingsSettings()
        if (selectedItem == "Keyboard") KeyboardSettings(navController)
    }
}

@Composable
fun VerticalNavBar(
    items: List<Pair<String, ImageVector>>,
    onItemClick: (String) -> Unit,
    selectedItem: String?
) {

    Column(
        modifier = Modifier
            .width(200.dp)
            .fillMaxHeight()
            .paint(
                painterResource(id = R.drawable.matt),
                contentScale = ContentScale.Crop
            )
            .padding(vertical = 20.dp, horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Settings",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
        )

        items.forEach { (label, icon) ->
            val isSelected = label == selectedItem

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = if (isSelected) HighlightColor.copy(alpha = 0.15f) else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(
                        width = if (isSelected) 2.dp else 0.dp,
                        color = if (isSelected) HighlightColor else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .shadow(
                        elevation = if (isSelected) 16.dp else 0.dp,
                        shape = RoundedCornerShape(12.dp),
                        ambientColor = if (isSelected) HighlightColor else Color.Transparent,
                        spotColor = if (isSelected) HighlightColor else Color.Transparent
                    )
                    .clickable {
                        onItemClick(label)
                    }
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = label,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}


@Composable
fun KeybindingsSettings() {
}

@Composable
fun AppearenceSettings() {
}

@Composable
fun EditorSettings() {

}