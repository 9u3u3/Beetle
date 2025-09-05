package com.bsbeetle.bananaspaghetti.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bsbeetle.bananaspaghetti.data.KeyboardLayout
import com.bsbeetle.bananaspaghetti.ui.keyboard.SplitKeyboard
import com.bsbeetle.bananaspaghetti.viewmodel.KeyboardViewModel


@Composable
fun NewFileScreen(
    viewModel: KeyboardViewModel = viewModel()
){

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // for selecting the layout.
    val layout = if (uiState.toggleKeyboard) {
        KeyboardLayout.developerLayout
    } else {
        KeyboardLayout.qwertyLayout
    }

        ConstraintLayout(modifier = Modifier.fillMaxSize()) {
            val (codeEditor, keyboardRef) = createRefs()

            //Code Editor spans full height and middle 60% width
            Box(
                modifier = Modifier
                    .constrainAs(codeEditor) {
                        // This is to have full height from top to bottom
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)

                        // This tells to fill the 60% width from start to end
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        width = Dimension.percent(0.6f)

                        // Fill the total available height on the screen
                        height = Dimension.fillToConstraints

                        centerHorizontallyTo(parent) // Center the 60% width element
                    }
                    .background(color = Color.Black)
                    .verticalScroll(rememberScrollState())
                    .padding(5.dp)
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