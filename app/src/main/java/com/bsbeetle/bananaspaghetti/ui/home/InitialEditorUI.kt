package com.bsbeetle.bananaspaghetti.ui.home


import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.bsbeetle.bananaspaghetti.ui.cmeditor.Editor
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bsbeetle.bananaspaghetti.data.KeyboardLayout
import com.bsbeetle.bananaspaghetti.ui.components.BottomBar
import com.bsbeetle.bananaspaghetti.ui.keyboard.KeyboardSide
import com.bsbeetle.bananaspaghetti.ui.keyboard.SplitKeyboardHalf
import com.bsbeetle.bananaspaghetti.viewmodel.KeyboardViewModel



@Composable
fun InitialEditorUI(
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
        val (topLeftRef,topRightRef,leftKeyboardRef, codeEditor, rightKeyboardRef,bottomBarRef) = createRefs()

        // Top Left box
        Box(modifier = Modifier.constrainAs(topLeftRef){
            top.linkTo(parent.top)
            bottom.linkTo(leftKeyboardRef.top)
            start.linkTo(parent.start)
            end.linkTo(codeEditor.start)
            width= Dimension.fillToConstraints
            height = Dimension.fillToConstraints
        }
        ){}

        // Top Right box
        Box(modifier = Modifier.constrainAs(topRightRef){
            top.linkTo(parent.top)
            bottom.linkTo(rightKeyboardRef.top)
            start.linkTo(codeEditor.start)
            end.linkTo(parent.end)
            width= Dimension.fillToConstraints
            height = Dimension.fillToConstraints
        }
        ){}
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
                width = Dimension.percent(0.2f) // 20% width for left keyboard
            }
        )

        // Code Editor - positioned in the center, full height
        Editor(
            modifier = Modifier
                .constrainAs(codeEditor) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    start.linkTo(leftKeyboardRef.end)
                    end.linkTo(rightKeyboardRef.start)
                    width = Dimension.fillToConstraints // Takes remaining 60% width
                    height = Dimension.fillToConstraints
                },
            viewModel
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
                width = Dimension.percent(0.2f) // 20% width for right keyboard
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
                }
        )
    }
}
