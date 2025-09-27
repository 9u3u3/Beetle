package com.bsbeetle.bananaspaghetti.ui.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bsbeetle.bananaspaghetti.data.TopBarButtons
import com.bsbeetle.bananaspaghetti.utils.EditorWebView
import com.bsbeetle.bananaspaghetti.utils.loadContentWhenReady
import com.bsbeetle.bananaspaghetti.viewmodel.BufferViewModel

@Composable
fun TopBar(
    modifier: Modifier = Modifier,
    color: Color,
    buttons: List<TopBarButtons>? = null,
    bufferViewModel: BufferViewModel? = null,
    context: Context? = null
) {

    Box(
        modifier = modifier
            .background(color)
            .fillMaxWidth()
            .height(15.dp),
    ) {
        // Here there will be a row of buttons
        Row(
            // modifier = Modifier.padding(start = 30.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {

            // If Buttons are there
            // This is for the left top bar and the right top bar
            // As only those bar will contain the the buttons
            if(buttons != null) {
                // Display buttons with their functionalities
                Row (
                    horizontalArrangement = Arrangement.Center
                ) {
                    buttons.forEach { button ->
                        Box(
                            modifier = Modifier
                                .clickable{button.onClick()},
                        ) {
                            Text(
                                text = button.title,
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }
                        VerticalDivider(
                            modifier = Modifier.fillMaxHeight()
                                .width(1.dp),
                            color = Color(0xFF8D8D8D)
                        )
                    }
                }
            } else bufferViewModel?.bufferLine?.size?.let {
                if(it > 0) { // for the middle bar, i.e. the buffer bar

                    LazyRow (
                        modifier = Modifier.fillMaxWidth().fillMaxHeight()
                    ) {
                        items(bufferViewModel.bufferLine.entries.toList()) { entry ->
                            BufferCard(
                                modifier = Modifier.background(Color.DarkGray),
                                context = context!!,
                                entry.key,
                                entry.value,
                                bufferViewModel.filesOpenedList
                            ) {
                                bufferViewModel.bufferLine.remove(entry.key)
                                bufferViewModel.filesOpenedList.removeAll { it == entry.key }

                                loadContentWhenReady(
                                    EditorWebView.getWebview(context!!),
                                    if(bufferViewModel.bufferLine.isNotEmpty()) bufferViewModel.bufferLine[bufferViewModel.filesOpenedList.last()]!! else ""
                                )

                            }
                        }
                    }
                }
            }
        }
    }
}