package com.bsbeetle.bananaspaghetti.ui.components

import android.annotation.SuppressLint
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bsbeetle.bananaspaghetti.R
import com.bsbeetle.bananaspaghetti.ui.theme.codaRegular

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun BottomBar(modifier: Modifier = Modifier, color: Color) {

    var targetHeight by remember { mutableStateOf(18.dp) }
    val logHeight by animateDpAsState(targetValue = targetHeight, label = "logHeightAnim")
    val density = LocalDensity.current
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

    Box(
        modifier = modifier.height(logHeight)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp)
                    .background(color)
                    .draggable(
                        orientation = Orientation.Vertical,
                        state = rememberDraggableState { delta ->
                            targetHeight = (targetHeight + with(density) { -delta.toDp() })
                                .coerceIn(18.dp, screenHeight)
                        }
                    )
            ) {
                Row(
                    modifier = Modifier.padding(start = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Line number and the column number
                    // Dummy line no. for now as there is no LSP integrated as of now
                    var lineNo: Int = 1
                    var colNo: Int = 4
                    Text(
                        text = "Ln $lineNo, Col $colNo",
                        fontFamily = codaRegular,
                        fontSize = 9.sp,
                        color = Color(0xFFFFFFFF)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // path to the objects
                    Image(
                        painter = painterResource(R.drawable.class_cube),
                        contentDescription = "Class",
                        modifier = Modifier.size(9.dp),
                        alignment = Alignment.Center
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "UserData",
                        fontFamily = codaRegular,
                        fontSize = 9.sp,
                        color = Color(0xFFFFFFFF)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Image(
                        painter = painterResource(R.drawable.chevron_right),
                        contentDescription = "Chevron",
                        modifier = Modifier.size(9.dp),
                        alignment = Alignment.Center
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Image(
                        painter = painterResource(R.drawable.data_object),
                        contentDescription = "Function",
                        modifier = Modifier.size(9.dp),
                        alignment = Alignment.Center
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "getUserName",
                        fontFamily = codaRegular,
                        fontSize = 9.sp,
                        color = Color(0xFFFFFFFF)
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    var warningCount: Int = 3
                    var errorCount: Int = 0

                    Image(
                        painter = painterResource(R.drawable.warning_icon),
                        contentDescription = "warning",
                        modifier = Modifier.size(9.dp),
                        alignment = Alignment.Center
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = warningCount.toString(),
                        fontFamily = codaRegular,
                        fontSize = 9.sp,
                        color = Color(0xFFFFFFFF)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Image(
                        painter = painterResource(R.drawable.error_icon),
                        contentDescription = "error",
                        modifier = Modifier.size(9.dp),
                        alignment = Alignment.Center
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = errorCount.toString(),
                        fontFamily = codaRegular,
                        fontSize = 9.sp,
                        color = Color(0xFFFFFFFF)
                    )

                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .background(Color(0xFF050505))
            ) {

                Text(
                    text = "Errors: 3\nWarnings: 9\nShow Fixes",
                    color = Color.White,
                    modifier = Modifier.padding(8.dp),
                    fontSize = 12.sp
                )
            }
        }
    }
}