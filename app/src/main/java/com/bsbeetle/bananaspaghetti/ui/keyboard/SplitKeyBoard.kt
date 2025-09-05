package com.bsbeetle.bananaspaghetti.ui.keyboard


import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bsbeetle.bananaspaghetti.model.Key
import com.bsbeetle.bananaspaghetti.model.KeyType
import com.bsbeetle.bananaspaghetti.viewmodel.KeyboardViewModel


@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun SplitKeyboard(
    layout: List<List<Key>>,
    isCapsLockOn: Boolean,
    modifier: Modifier = Modifier,
    viewModel: KeyboardViewModel
) {
    val configuration = LocalConfiguration.current
    val screenHeightDp = configuration.screenHeightDp.dp
    val keyboardHeight = screenHeightDp / 2f

    val numberOfRows = layout.size
    val totalVerticalPadding = 16.dp
    val availableHeight = keyboardHeight - totalVerticalPadding
    val rowHeight = availableHeight / numberOfRows


    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(keyboardHeight)
            .padding(vertical = 4.dp, horizontal = 2.dp),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        layout.forEach { rowOfKeys ->
            ConstraintLayout(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(rowHeight)
            ) {
                val (leftRef, rightRef) = createRefs()

                // Split keys into halves
                val splitIndex = (rowOfKeys.size + 1) / 2
                val leftHalf = rowOfKeys.subList(0, splitIndex)
                val rightHalf = rowOfKeys.subList(splitIndex, rowOfKeys.size)

                // Left Keyboard
                LeftKeyboard(
                    keys = leftHalf,
                    isCapsLockOn = isCapsLockOn,
                    viewModel = viewModel,
                    modifier = Modifier.constrainAs(leftRef) {
                        start.linkTo(parent.start)
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        width = Dimension.percent(0.2f)
                    }
                )

                // Right Keyboard
                RightKeyboard(
                    keys = rightHalf,
                    isCapsLockOn = isCapsLockOn,
                    viewModel = viewModel,
                    modifier = Modifier.constrainAs(rightRef) {
                        end.linkTo(parent.end)
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        width = Dimension.percent(0.2f)
                    }
                )
            }
        }
    }
}

@Composable
fun LeftKeyboard(
    keys: List<Key>,
    isCapsLockOn: Boolean,
    viewModel: KeyboardViewModel,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.End
    ) {
        keys.forEach { key ->
            KeyboardKey(
                key,
                isCapsLockOn,
                onKeyPress = { text ->
                    when (key.type) {
                        KeyType.ACTION_TOGGLE -> viewModel.toggleKeyboardVisibility()
                        KeyType.ACTION_BACKSPACE -> viewModel.onBackspace()
                        KeyType.ACTION_ENTER -> viewModel.onKeyPress('\n')
                        KeyType.ACTION_SPACE -> viewModel.onKeyPress(' ')
                        KeyType.ACTION_SHIFT -> viewModel.onToggleCapsLock()
                        else -> viewModel.onKeyPress(text.first())
                    }
                },
                modifier.background(
                    shape = RectangleShape,
                    color = Color.Black
                )
            )
        }
    }
}

@Composable
fun RightKeyboard(
    keys: List<Key>,
    isCapsLockOn: Boolean,
    viewModel: KeyboardViewModel,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Start
    ) {
        keys.forEach { key ->
            KeyboardKey(
                key,
                isCapsLockOn,
                onKeyPress = { text ->
                    when (key.type) {
                        KeyType.ACTION_TOGGLE -> viewModel.toggleKeyboardVisibility()
                        KeyType.ACTION_BACKSPACE -> viewModel.onBackspace()
                        KeyType.ACTION_ENTER -> viewModel.onEnter()
                        KeyType.ACTION_SPACE -> viewModel.onKeyPress(' ')
                        KeyType.ACTION_SHIFT -> viewModel.onToggleCapsLock()
                        else -> viewModel.onKeyPress(text.first())
                    }
                },
                modifier.background(
                    shape = RoundedCornerShape(50.dp),
                    color = Color.Black
                )
            )
        }
    }
}
