package com.bsbeetle.bananaspaghetti.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun BottomBar(modifier: Modifier = Modifier, color: Color) {
    Box(
        modifier = modifier
            .background(color)
            .fillMaxWidth()
    ) {
        // Here there will be a row of buttons
        Row(
            modifier = Modifier.padding(start = 30.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            // Display the information
        }
    }
}