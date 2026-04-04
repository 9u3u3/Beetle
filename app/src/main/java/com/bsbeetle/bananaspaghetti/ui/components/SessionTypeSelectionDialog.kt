package com.bsbeetle.bananaspaghetti.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.bsbeetle.bananaspaghetti.R

@Composable
fun SessionTypeSelectionDialog(
    onDismiss: () -> Unit,
    onSelectEditor: () -> Unit,
    onSelectNotebook: () -> Unit,
    title: String = "Open in..."
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF252526),
            tonalElevation = 0.dp
        ) {
            Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp)) {

                // Title
                Text(
                    text = title,
                    color = Color(0xFFD4D4D4),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.3.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                HorizontalDivider(
                    color = Color(0xFF3C3C3C),
                    thickness = 0.5.dp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Option cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SessionTypeOption(
                        iconRes = R.drawable.article_icon,
                        label = "Editor",
                        description = "Code editing\nwith syntax",
                        modifier = Modifier.weight(1f),
                        onClick = { onSelectEditor(); onDismiss() }
                    )
                    SessionTypeOption(
                        iconRes = R.drawable.note_add_24dp_e3e3e3_fill0_wght400_grad0_opsz24,
                        label = "Notebook",
                        description = "Python cells\nwith execution",
                        modifier = Modifier.weight(1f),
                        onClick = { onSelectNotebook(); onDismiss() }
                    )
                }

                // Cancel row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "Cancel",
                            color = Color(0xFF9CDCFE),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionTypeOption(
    iconRes: Int,
    label: String,
    description: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF2D2D2D))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = Color(0xFF9CDCFE)),
                onClick = onClick
            )
            .padding(vertical = 18.dp, horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = label,
            tint = Color(0xFFCCCCCC),
            modifier = Modifier.size(36.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = label,
            color = Color(0xFFD4D4D4),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = description,
            color = Color(0xFF6A6A6A),
            fontSize = 10.sp,
            lineHeight = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}
