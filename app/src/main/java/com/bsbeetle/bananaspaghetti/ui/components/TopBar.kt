package com.bsbeetle.bananaspaghetti.ui.components

import android.content.Context
import android.content.res.Configuration
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bsbeetle.bananaspaghetti.LocalBufferViewModel
import com.bsbeetle.bananaspaghetti.LocalEditorViewModel
import com.bsbeetle.bananaspaghetti.data.TopBarButtons
import com.bsbeetle.bananaspaghetti.utils.loadContentWhenReady
import com.bsbeetle.bananaspaghetti.viewmodel.BufferViewModel
import com.bsbeetle.bananaspaghetti.viewmodel.EditorViewModel
@Composable
fun TopBar(
    modifier: Modifier = Modifier,
    color: Color,
    buttons: List<TopBarButtons>? = null,
    context: Context? = null,
) {
    val bufferViewModel: BufferViewModel = LocalBufferViewModel.current
    val editorViewModel: EditorViewModel = LocalEditorViewModel.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val webView = editorViewModel.webView

    Box(
        modifier = modifier
            .background(color)
            .fillMaxWidth()
            .height(if (isLandscape) 15.dp else 25.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (buttons != null) {
                Row(horizontalArrangement = Arrangement.Center) {
                    buttons.forEach { button ->
                        Box(
                            modifier = Modifier
                                .clickable { button.onClick() }
                                .background(Color.Transparent)
                                .fillMaxHeight()
                                .width(50.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // ✅ Show icon if available
                                button.icon?.let { icon ->
                                    androidx.compose.material3.Icon(
                                        imageVector = icon,
                                        contentDescription = button.title ?: "",
                                        tint = Color.White,
                                        modifier = Modifier.height(16.dp)
                                    )
                                }

                                // ✅ Show image if available
                                button.imageRes?.let { imageRes ->
                                    androidx.compose.foundation.Image(
                                        painter = androidx.compose.ui.res.painterResource(id = imageRes),
                                        contentDescription = button.title ?: "",
                                        modifier = Modifier.height(16.dp)
                                    )
                                }


                                // ✅ Show title if available
                                button.title?.let {
                                    Text(
                                        text = it,
                                        color = Color.White,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        VerticalDivider(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(1.dp),
                            color = Color(0xFF8D8D8D)
                        )
                    }
                }
            } else bufferViewModel?.bufferLine?.size?.let {
                if (it > 0) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().fillMaxHeight()
                    ) {
                        items(bufferViewModel.bufferLine.entries.toList()) { entry ->
                            BufferCard(
                                modifier = Modifier.background(Color.DarkGray),
                                context = context!!,
                                entry.key,
                                entry.value,
                                bufferViewModel.filesOpenedList,
                                webView!!
                            ) {
                                bufferViewModel.bufferLine.remove(entry.key)
                                bufferViewModel.filesOpenedList.removeAll { it == entry.key }

                                loadContentWhenReady(
                                    webView,
                                    if (bufferViewModel.bufferLine.isNotEmpty())
                                        bufferViewModel.bufferLine[bufferViewModel.filesOpenedList.last()]!!
                                    else ""
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
