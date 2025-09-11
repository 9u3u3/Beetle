package com.bsbeetle.bananaspaghetti.ui.home

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.bsbeetle.bananaspaghetti.ui.cmeditor.Editor
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bsbeetle.bananaspaghetti.data.KeyboardLayout
import com.bsbeetle.bananaspaghetti.ui.keyboard.SplitKeyboard
import com.bsbeetle.bananaspaghetti.viewmodel.FileViewModel
import com.bsbeetle.bananaspaghetti.viewmodel.KeyboardViewModel

@Composable
fun InitialEditorUI (
    fileViewModel: FileViewModel,
    viewModel: KeyboardViewModel
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Selecting the keyboard layout
    val layout = if (uiState.toggleKeyboard) {
        KeyboardLayout.developerLayout
    } else {
        KeyboardLayout.qwertyLayout
    }

    ConstraintLayout(modifier = Modifier.fillMaxSize()) {
        val (codeEditor, keyboardRef) = createRefs()

        // WebView Editor field spans full height and middle 60% width
        Editor(
        //Code Editor spans full height and middle 60% width
            modifier = Modifier
                .constrainAs(codeEditor) {
                    // This is to have full height from top to bottom
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    width = Dimension.percent(0.6f)
                    // Fill the total available height on the screen
                    height = Dimension.fillToConstraints
                    centerHorizontallyTo(parent) // Center the 60% width element
                },
            viewModel,
            fileViewModel
        )

        // Split keyboard Constraint layout(overlaying)
        SplitKeyboard(
            layout = layout,
            isCapsLockOn = uiState.isCapsLockOn,
            viewModel = viewModel,
            modifier = Modifier.constrainAs(keyboardRef) {
                // Keyboard positioned at the bottom
                bottom.linkTo(parent.bottom)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                width = Dimension.fillToConstraints // Fills available width
                // The keyboard will naturally overlay on top of the text field
            }
        )
    }
}
