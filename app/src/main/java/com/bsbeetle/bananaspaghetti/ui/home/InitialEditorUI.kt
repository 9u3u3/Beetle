package com.bsbeetle.bananaspaghetti.ui.home

import androidx.compose.foundation.layout.Box
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.bsbeetle.bananaspaghetti.ui.cmeditor.Editor
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.bsbeetle.bananaspaghetti.data.KeyboardLayout
import com.bsbeetle.bananaspaghetti.ui.components.BottomBar
import com.bsbeetle.bananaspaghetti.ui.keyboard.KeyboardSide
import com.bsbeetle.bananaspaghetti.ui.keyboard.SplitKeyboardHalf
import com.bsbeetle.bananaspaghetti.data.TopBarButtons
import com.bsbeetle.bananaspaghetti.ui.components.TopBar
import com.bsbeetle.bananaspaghetti.ui.keyboard.SmoothSlider
import com.bsbeetle.bananaspaghetti.utils.loadContentWhenReady
import com.bsbeetle.bananaspaghetti.utils.readFromFile
import com.bsbeetle.bananaspaghetti.viewmodel.FileViewModel
import com.bsbeetle.bananaspaghetti.viewmodel.KeyboardViewModel
import com.bsbeetle.bananaspaghetti.viewmodel.SliderViewModel
import java.io.FileNotFoundException
import java.io.OutputStreamWriter


@Composable
fun InitialEditorUI (
    fileViewModel: FileViewModel,
    viewModel: KeyboardViewModel,
    sliderViewModel: SliderViewModel,
    navController: NavController
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sliderValue by sliderViewModel.sliderValue.collectAsStateWithLifecycle()


    val context = LocalContext.current

    // for selecting the layout.
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
    val buttonsForLeftTopBar = listOf(
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

    val buttonsForRightTopBar=listOf(TopBarButtons("Settings",onClick = {
        navController.navigate("settings")
    }))


    ConstraintLayout(modifier = Modifier.fillMaxSize()) {
            val (topLeftBarRef,topMiddleBarRef,topRightBarRef,topLeftRef, topRightRef, leftKeyboardRef, codeEditor, rightKeyboardRef, bottomBarRef) = createRefs()

            // Top Left box
            Box(modifier = Modifier.constrainAs(topLeftRef) {
                top.linkTo(topLeftBarRef.bottom)
                bottom.linkTo(leftKeyboardRef.top)
                start.linkTo(parent.start)
                end.linkTo(codeEditor.start)
                width = Dimension.fillToConstraints
                height = Dimension.fillToConstraints
            }
                .background(color = Color.DarkGray)

            ) {

                SmoothSlider(sliderViewModel = sliderViewModel)

            }

            // TopBars

        //Left Top Bar
            TopBar(
                modifier = Modifier
                    .constrainAs(topLeftBarRef) {
                        top.linkTo(parent.top)
                        start.linkTo(parent.start)
                        end.linkTo(topMiddleBarRef.start)
                        width = Dimension.percent(0.2f)
                        height = Dimension.wrapContent
                    },
//                color = Color(0xFF2F2F2F),
                Color.Red,
                buttonsForLeftTopBar
            )

        //Middle Top Bar
        TopBar(
            modifier = Modifier
                .constrainAs(topMiddleBarRef) {
                    top.linkTo(parent.top)
                    start.linkTo(topLeftBarRef.end)
                    end.linkTo(topRightBarRef.start)
                    width = Dimension.percent(0.6f)
                    height = Dimension.wrapContent
                },
//                color = Color(0xFF2F2F2F),
            color = Color.Yellow,
            buttonsForLeftTopBar
        )

        //Right Top Bar
        TopBar(
            modifier = Modifier
                .constrainAs(topRightBarRef) {
                    top.linkTo(parent.top)
                    start.linkTo(topMiddleBarRef.end)
                    end.linkTo(parent.end)
                    width = Dimension.percent(0.2f)
                    height = Dimension.wrapContent
                },
//                color = Color(0xFF2F2F2F),
            color = Color.Green,
            buttonsForRightTopBar
        )

            // Top Right box
            Box(modifier = Modifier.constrainAs(topRightRef) {
                top.linkTo(topRightBarRef.bottom)
                bottom.linkTo(rightKeyboardRef.top)
                start.linkTo(codeEditor.start)
                end.linkTo(parent.end)
                width = Dimension.fillToConstraints
                height = Dimension.fillToConstraints
            }
                .background(color = Color.DarkGray)
            ) {}
            // Left Keyboard - positioned at bottom left
            SplitKeyboardHalf(
                layout = layout,
                side = KeyboardSide.LEFT,
                isCapsLockOn = uiState.isCapsLockOn,
                isShiftOn = uiState.isShiftOn,
                viewModel = viewModel,
                modifier = Modifier.constrainAs(leftKeyboardRef) {
                    start.linkTo(parent.start)
                    bottom.linkTo(parent.bottom)
                    width = Dimension.percent(sliderValue/100f) // 20% width for left keyboard
                }
            )

            // Code Editor - positioned in the center, full height
            Editor(
                modifier = Modifier
                    .constrainAs(codeEditor) {
                        top.linkTo(topMiddleBarRef.bottom)  // Now linked to TopBar
                        bottom.linkTo(parent.bottom)
                        start.linkTo(leftKeyboardRef.end)
                        end.linkTo(rightKeyboardRef.start)
                        width = Dimension.fillToConstraints // Takes remaining 60% width
                        height = Dimension.fillToConstraints
                    },
                viewModel,
                webView = webView

            )

            // Right Keyboard - positioned at bottom right
            SplitKeyboardHalf(
                layout = layout,
                side = KeyboardSide.RIGHT,
                isCapsLockOn = uiState.isCapsLockOn,
                isShiftOn = uiState.isShiftOn,
                viewModel = viewModel,
                modifier = Modifier.constrainAs(rightKeyboardRef) {
                    end.linkTo(parent.end)
                    bottom.linkTo(parent.bottom)
                    width = Dimension.percent(sliderValue/100f) // 20% width for right keyboard
                }
            )


            BottomBar(
                modifier = Modifier
                    .height(16.dp)
                    .constrainAs(bottomBarRef) {
                        bottom.linkTo(codeEditor.bottom)   // stick to editor’s bottom edge
                        start.linkTo(codeEditor.start)     // align with editor
                        end.linkTo(codeEditor.end)
                        width = Dimension.fillToConstraints
                    },
                color = Color(0xFF2D57A8)

            )
        }

}
