package com.bsbeetle.bananaspaghetti.ui.home

import androidx.compose.foundation.layout.Box
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.bsbeetle.bananaspaghetti.ui.cmeditor.Editor
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.bsbeetle.bananaspaghetti.data.KeyboardLayout
import com.bsbeetle.bananaspaghetti.ui.components.BottomBar
import com.bsbeetle.bananaspaghetti.ui.keyboard.KeyboardSide
import com.bsbeetle.bananaspaghetti.ui.keyboard.SplitKeyboardHalf
import com.bsbeetle.bananaspaghetti.data.TopBarButtons
import com.bsbeetle.bananaspaghetti.ui.components.TopBar
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
    keyboardViewModel: KeyboardViewModel,
    sliderViewModel: SliderViewModel,
    navController: NavController
) {

    val uiState by keyboardViewModel.uiState.collectAsStateWithLifecycle()
    val sliderValue by sliderViewModel.sliderValue.collectAsStateWithLifecycle()


    val context = LocalContext.current

    // for selecting the layout.
    val layout = if (uiState.toggleKeyboard) {
        KeyboardLayout.developerLayout
    } else {
        KeyboardLayout.qwertyLayout
    }

    // Error log panel at bottom
    val density = LocalDensity.current
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    var targetHeight by remember { mutableStateOf(17.dp) } // default height

    // Animated height
    val logHeight by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = spring(  // spring = bouncy, you can also use tween for linear
            dampingRatio = 0.8f,
            stiffness = 50f
        ),
        label = "logHeightAnim"
    )
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


    Box {
        ConstraintLayout(modifier = Modifier.fillMaxSize()) {
            val (topLeftBarRef, topMiddleBarRef, topRightBarRef, topLeftRef, topRightRef, leftKeyboardRef, codeEditor, rightKeyboardRef, errorLogRef,bottomBarRef) = createRefs()

            // Top Left box
            Box(
                modifier = Modifier
                    .constrainAs(topLeftRef) {
                        top.linkTo(topLeftBarRef.bottom)
                        bottom.linkTo(leftKeyboardRef.top)
                        start.linkTo(parent.start)
                        end.linkTo(codeEditor.start)
                        width = Dimension.fillToConstraints
                        height = Dimension.fillToConstraints
                    }
                    .background(color = Color.DarkGray)

            ) {}

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
            Box(
                modifier = Modifier
                    .constrainAs(topRightRef) {
                        top.linkTo(topRightBarRef.bottom)
                        bottom.linkTo(rightKeyboardRef.top)
                        start.linkTo(codeEditor.start)
                        end.linkTo(parent.end)
                        width = Dimension.fillToConstraints
                        height = Dimension.fillToConstraints
                    }
                    .background(color = Color.DarkGray)
            ) {}

            if (uiState.isLeftKeyboardVisible) {
                // Left Keyboard - positioned at bottom left
                // Key is helped to listen to any changes that occur and recompose the UI accordingly
                key(sliderValue) {
                    SplitKeyboardHalf(
                        layout = layout,
                        side = KeyboardSide.LEFT,
                        isCapsLockOn = uiState.isCapsLockOn,
                        isShiftOn = uiState.isShiftOn,
                        onKeyEvent = keyboardViewModel::onKeyEvent,
                        modifier = Modifier.constrainAs(leftKeyboardRef) {
                            start.linkTo(parent.start)
                            bottom.linkTo(parent.bottom)
                            width =
                                Dimension.percent(sliderValue / 100f) // 20% width for left keyboard
                        }
                    )
                }
            }

            // Code Editor - positioned in the center, full height
            Editor(
                modifier = Modifier
                    .constrainAs(codeEditor) {
                        top.linkTo(topMiddleBarRef.bottom)  // Now linked to TopBar
                        bottom.linkTo(parent.bottom)
                        //                        start.linkTo(leftKeyboardRef.end)
                        //                        end.linkTo(rightKeyboardRef.start)
                        // Dynamically set the start constraint
                        if (uiState.isLeftKeyboardVisible) {
                            start.linkTo(leftKeyboardRef.end)
                        } else {
                            start.linkTo(parent.start)
                        }

                        // Dynamically set the end constraint
                        if (uiState.isRightKeyboardVisible) {
                            end.linkTo(rightKeyboardRef.start)
                        } else {
                            end.linkTo(parent.end)
                        }
                        width = Dimension.fillToConstraints // Takes remaining 60% width
                        height = Dimension.fillToConstraints
                    },
                keyboardViewModel,
                webView = webView

            )

            // Right Keyboard - positioned at bottom right
            // Key is helped to listen to any changes that occur and recompose the UI accordingly

            if (uiState.isRightKeyboardVisible) {
                key(sliderValue) {
                    SplitKeyboardHalf(
                        layout = layout,
                        side = KeyboardSide.RIGHT,
                        isCapsLockOn = uiState.isCapsLockOn,
                        isShiftOn = uiState.isShiftOn,
                        onKeyEvent = keyboardViewModel::onKeyEvent,
                        modifier = Modifier.constrainAs(rightKeyboardRef) {
                            end.linkTo(parent.end)
                            bottom.linkTo(parent.bottom)
                            width =
                                Dimension.percent(sliderValue / 100f) // 20% width for right keyboard
                        }
                    )
                }
            }
// ========= Error log panel =========
            Box(
                modifier = Modifier
                    .height(logHeight)
                    .background(Color.DarkGray)
                    .constrainAs(errorLogRef) {
                        bottom.linkTo(codeEditor.bottom)   // stick to editor’s bottom edge
                        start.linkTo(codeEditor.start)
                        end.linkTo(codeEditor.end)
                        width = Dimension.fillToConstraints
                    }
            ) {
                Column {
                    // Drag handle (only dragging)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(16.dp)
                            .background(Color.Green)
                            .draggable(
                                orientation = Orientation.Vertical,
                                state = rememberDraggableState { delta ->
                                    targetHeight = (targetHeight + with(density) { -delta.toDp() })
                                        .coerceIn(17.dp, screenHeight) // min height = 30.dp
                                }
                            )
                    )

                    // Scrollable error log content
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
//                            .weight(1f) // take all remaining space
                            .verticalScroll(rememberScrollState())
                            .padding(8.dp)
                    ) {
                        Text(
                            "For interactions that involve moving an element along a single axis, such as a slider or a side\n" +
                                    "panel, Compose provides the Modifier.draggable. This modifier is designed specifically for\n" +
                                    "single-orientation (horizontal or vertical) dragging.4\n" +
                                    "It is crucial to understand that draggable only detects the gesture and reports the distance\n" +
                                    "dragged (the delta) during each movement event. It does not move the composable on its\n" +
                                    "own. The developer is responsible for capturing this delta, updating a state variable (typically\n" +
                                    "an offset), and then using another modifier, such as Modifier.offset, to visually translate the\n" +
                                    "component on the screen.For interactions that involve moving an element along a single axis, such as a slider or a side\n" +
                                    "panel, Compose provides the Modifier.draggable. This modifier is designed specifically for\n" +
                                    "single-orientation (horizontal or vertical) dragging.4\n" +
                                    "It is crucial to understand that draggable only detects the gesture and reports the distance\n" +
                                    "dragged (the delta) during each movement event. It does not move the composable on its\n" +
                                    "own. The developer is responsible for capturing this delta, updating a state variable (typically\n" +
                                    "an offset), and then using another modifier, such as Modifier.offset, to visually translate the\n" +
                                    "component on the screen.For interactions that involve moving an element along a single axis, such as a slider or a side\n" +
                                    "panel, Compose provides the Modifier.draggable. This modifier is designed specifically for\n" +
                                    "single-orientation (horizontal or vertical) dragging.4\n" +
                                    "It is crucial to understand that draggable only detects the gesture and reports the distance\n" +
                                    "dragged (the delta) during each movement event. It does not move the composable on its\n" +
                                    "own. The developer is responsible for capturing this delta, updating a state variable (typically\n" +
                                    "an offset), and then using another modifier, such as Modifier.offset, to visually translate the\n" +
                                    "component on the screen.",
                            color = Color.White
                        )
                    }
                }
            }


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



}
