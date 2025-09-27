package com.bsbeetle.bananaspaghetti.ui.components

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import com.bsbeetle.bananaspaghetti.R
import com.bsbeetle.bananaspaghetti.utils.EditorWebView
import com.bsbeetle.bananaspaghetti.utils.getFileName
import com.bsbeetle.bananaspaghetti.utils.getIconResourceId
import com.bsbeetle.bananaspaghetti.utils.loadContentWhenReady

@Composable
fun BufferCard(
    modifier: Modifier = Modifier,
    context: Context,
    file: DocumentFile,
    content: String = "",
    filesOpenedList: MutableList<DocumentFile>,
    onClickDelete: () -> Unit
) {

    val fileName: String = getFileName(context, file.uri)
    val webView = EditorWebView.getWebview(context)

    Box(
      modifier = modifier.clickable {
          loadContentWhenReady(webView, content)
          filesOpenedList += file
      }
    ) {
        Row (
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Icon
            Icon(
                painter = painterResource(id = getIconResourceId(fileName)),
                contentDescription = "file icon",
                modifier = Modifier.size(7.dp)
            )

            Spacer(modifier = Modifier.width(3.dp))

            // FileName
            Text(
                text = fileName,
                color = Color.White,
                fontSize = 8.sp
            )

            Spacer(modifier = Modifier.width(3.dp))

            // Close
            Icon(
                painter = painterResource(id = R.drawable.icon_close),
                contentDescription = "close file icon",
                modifier = Modifier.size(7.dp).clickable{
                    onClickDelete()
                }
            )

            VerticalDivider(
                modifier = Modifier.fillMaxHeight().width(2.dp),
                color = Color(0xFF0082DB)
            )

        }
    }
}