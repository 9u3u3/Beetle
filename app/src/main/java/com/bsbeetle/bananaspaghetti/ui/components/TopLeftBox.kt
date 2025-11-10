package com.bsbeetle.bananaspaghetti.ui.components

import android.content.res.Configuration
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.bsbeetle.bananaspaghetti.LocalBufferViewModel
import com.bsbeetle.bananaspaghetti.LocalFileViewModel
import com.bsbeetle.bananaspaghetti.utils.Tree
import com.bsbeetle.bananaspaghetti.viewmodel.BufferViewModel
import com.bsbeetle.bananaspaghetti.viewmodel.FileViewModel

@Composable
fun TopLeftBox(
    modifier: Modifier = Modifier,
    webView: WebView
) {

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    ConstraintLayout(modifier = modifier.background(Color(0xFF252525))) {
        val (leftBar, rightSpace) = createRefs()

        // Left bar
        Box(
            modifier = Modifier
                .constrainAs(leftBar) {
                    start.linkTo(parent.start)
                    top.linkTo(parent.top)
                    width = Dimension.value(25.dp)
                }
                .fillMaxHeight()
                .background(Color(0xFF212121))
        ) {  }

        // Right Space
        Tree(
            modifier = Modifier
                .constrainAs(rightSpace) {
                 start.linkTo(leftBar.end)
                 end.linkTo(parent.end)
                 top.linkTo(parent.top)
                 width = Dimension.fillToConstraints
                }
                .fillMaxHeight(),
            webView
        )
    }
}