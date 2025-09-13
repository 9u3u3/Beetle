package com.bsbeetle.bananaspaghetti.ui.home

import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.bsbeetle.bananaspaghetti.ui.cmeditor.Editor
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bsbeetle.bananaspaghetti.data.KeyboardLayout
import com.bsbeetle.bananaspaghetti.data.TopBarButtons
import com.bsbeetle.bananaspaghetti.ui.components.BottomBar
import com.bsbeetle.bananaspaghetti.ui.components.TopBar
import com.bsbeetle.bananaspaghetti.ui.keyboard.SplitKeyboard
import com.bsbeetle.bananaspaghetti.utils.loadContentWhenReady
import com.bsbeetle.bananaspaghetti.utils.readFromFile
import com.bsbeetle.bananaspaghetti.viewmodel.FileViewModel
import com.bsbeetle.bananaspaghetti.viewmodel.KeyboardViewModel
import java.io.FileNotFoundException
import java.io.OutputStreamWriter

@Composable
fun InitialEditorUI (
    fileViewModel: FileViewModel,
    viewModel: KeyboardViewModel
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Selecting the keyboard layout
    val layout = if (uiState.toggleKeyboard) {
        KeyboardLayout.developerLayout
    } else {
        KeyboardLayout.qwertyLayout
    }

    val webView = remember {
        WebView(context).apply {

            WebView.setWebContentsDebuggingEnabled(true)

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
                cacheMode = WebSettings.LOAD_NO_CACHE
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                useWideViewPort = true
                loadWithOverviewMode = true
                setSupportZoom(false)
            }

            // Load the content only when the page is loaded
            webViewClient = object : WebViewClient() { // Creates an anonymous subclass of webView
                //... client
                // Handles callback related to the lifecycle of the webView's page loading
                override fun onPageFinished(view: WebView?, url: String?) { // called when webView
                    //... finishes loading the page
                    // view -> webView instance, url -> url of the loaded page
                    super.onPageFinished(view, url)
                    fileViewModel.selectedUri?.let { uri -> // check is selectedUri is not null,
                        //... if not null then executes the below code
                        val fileContent = readFromFile(context, uri)
                        loadContentWhenReady(view, fileContent) // loads the code to the
                        //... to the webView
                    }
                }
            }

            loadUrl("file:///android_asset/editor/index.html")
        }
    }

    val buttons = listOf(
        TopBarButtons("Save") {
            // Get the code from code mirror, override that with the opened file
            webView.evaluateJavascript("window.code_") { result ->
                val newCode = result.removeSurrounding("\"")
                    .replace("\\n", "\n")
                    .replace("\\u003C", "<")
                    .replace("\\u003E", ">")
                    .replace("\\\"", "\"")

                // get the content resolver
                val resolver = context.contentResolver
                // open the output
                fileViewModel.selectedUri?.let { uri -> // This block only runs when selectedUri is
                    //... not null
                    val out = resolver.openOutputStream(uri, "wt") // truncate mode
                        ?: throw FileNotFoundException("Unable to open output stream for $uri")

                    OutputStreamWriter(out, Charsets.UTF_8).use { writer ->
                        writer.write(newCode)
                        writer.flush()
                    }
                }
            }
        }
    )

    ConstraintLayout(modifier = Modifier.fillMaxSize()) {
        val (topBarRef, codeEditor, keyboardRef, bottomBarRef) = createRefs()

        // TopBar Constraint
        TopBar(
            modifier = Modifier
                .constrainAs(topBarRef) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    width = Dimension.fillToConstraints
                    height = Dimension.wrapContent
                },
            color = Color(0xFF2F2F2F),
            buttons
        )

        // WebView Editor field
        Editor(
            modifier = Modifier
                .constrainAs(codeEditor) {
                    top.linkTo(topBarRef.bottom)  // Now linked to TopBar
                    bottom.linkTo(bottomBarRef.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    width = Dimension.percent(0.6f)
                    height = Dimension.fillToConstraints
                    centerHorizontallyTo(parent)
                },
            viewModel = viewModel,
            webView = webView
        )

        // Split keyboard (overlaying)
        SplitKeyboard(
            layout = layout,
            isCapsLockOn = uiState.isCapsLockOn,
            viewModel = viewModel,
            modifier = Modifier.constrainAs(keyboardRef) {
                bottom.linkTo(bottomBarRef.top)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                width = Dimension.fillToConstraints
            }
        )

        // Bottom Bar
        BottomBar(
            modifier = Modifier
                .constrainAs(bottomBarRef) {
                    bottom.linkTo(parent.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    width = Dimension.fillToConstraints
                },
            color = Color(0xFF2D57A8)
        )
    }

}
