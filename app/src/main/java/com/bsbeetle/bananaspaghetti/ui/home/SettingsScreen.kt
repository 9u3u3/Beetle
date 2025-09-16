package com.bsbeetle.bananaspaghetti.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.bsbeetle.bananaspaghetti.viewmodel.KeyboardViewModel

@Composable
fun SettingsScreen(
    keyboardViewModel: KeyboardViewModel,
    navController: NavController

) {
    val uiState by keyboardViewModel.uiState.collectAsStateWithLifecycle()

    var indentValue by remember { mutableStateOf("${uiState.indentValue}") }

    Column (
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.DarkGray)
    ){
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = Color.DarkGray)
                .padding(16.dp)

        ) {
            Column {
                OutlinedTextField(
                    value = indentValue,
                    onValueChange = { newText ->
                        indentValue = newText
                        newText.toIntOrNull()?.let { num ->
                            keyboardViewModel.updateIndentSize(num)
                        }
                    },
                    label = { Text("Indent size") },
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color.DarkGray,
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.LightGray
                    )
                )
            }
        }
            Box {
                Column(
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 5.dp)
                ) {
                    Text("Keyboard Visibility")
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ){
                        Checkbox(checked = uiState.isLeftKeyboardVisible, onCheckedChange = {
                            keyboardViewModel.toggleLeftKeyboardVisibility()
                        })
                        Text("Left Keyboard")
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ){
                        Checkbox(checked = uiState.isRightKeyboardVisible, onCheckedChange = {
                            keyboardViewModel.toggleRightKeyboardVisibility()
                        })
                        Text("Right Keyboard")
                    }
                }
            }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = {
                    navController.navigate("demo")
                })
        ){
           Text("Keyboard Sizing")
        }
        }

}