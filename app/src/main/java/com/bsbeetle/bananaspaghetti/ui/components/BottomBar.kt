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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bsbeetle.bananaspaghetti.R
import com.bsbeetle.bananaspaghetti.ui.theme.codaRegular
import com.bsbeetle.bananaspaghetti.viewmodel.LspViewModel

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun BottomBar(modifier: Modifier = Modifier, color: Color, lspViewModel: LspViewModel = viewModel()) {

    var targetHeight by remember { mutableStateOf(18.dp) }
    val logHeight by animateDpAsState(targetValue = targetHeight, label = "logHeightAnim")
    val density = LocalDensity.current
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    
    val lspStatus by lspViewModel.lspStatus.collectAsStateWithLifecycle()

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
                    Text(
                        text = "Ln ${lspStatus.line}, Col ${lspStatus.column}",
                        fontFamily = codaRegular,
                        fontSize = 9.sp,
                        color = Color(0xFFFFFFFF)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    if (lspStatus.currentSymbol.isNotEmpty()) {
                        // path to the objects
                        Image(
                            painter = painterResource(R.drawable.class_cube), // Placeholder icon logic could be improved
                            contentDescription = "Symbol",
                            modifier = Modifier.size(9.dp),
                            alignment = Alignment.Center
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = lspStatus.currentSymbol,
                            fontFamily = codaRegular,
                            fontSize = 9.sp,
                            color = Color(0xFFFFFFFF)
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Image(
                        painter = painterResource(R.drawable.warning_icon),
                        contentDescription = "warning",
                        modifier = Modifier.size(9.dp),
                        alignment = Alignment.Center
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = lspStatus.warningCount.toString(),
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
                        text = lspStatus.errorCount.toString(),
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
                    text = "Errors: ${lspStatus.errorCount}\nWarnings: ${lspStatus.warningCount}\nShow Fixes",
                    color = Color.White,
                    modifier = Modifier.padding(8.dp),
                    fontSize = 12.sp
                )
            }
        }
    }
}