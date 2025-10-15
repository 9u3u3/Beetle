package com.bsbeetle.bananaspaghetti.ui.keyboard

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.bsbeetle.bananaspaghetti.model.Key
import com.bsbeetle.bananaspaghetti.model.StableKeyboardLayout
import com.bsbeetle.bananaspaghetti.viewmodel.KeyGesture

enum class KeyboardSide {
    LEFT, RIGHT
}

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun SplitKeyboardHalf(
    layout: StableKeyboardLayout,
    side: KeyboardSide,
    isCapsLockOn: Boolean,
    isShiftOn: Boolean,
    isCtrlOn: Boolean,
    onKeyEvent: (Key, KeyGesture) -> Unit,
    modifier: Modifier = Modifier
) {

    val configuration = LocalConfiguration.current
    val screenHeightDp = configuration.screenHeightDp.dp
    val keyboardHeight = screenHeightDp / 2f
    val numberOfRows = layout.rows.size
    val totalVerticalPadding = 16.dp
    val availableHeight = keyboardHeight - totalVerticalPadding
    val rowHeight = availableHeight / numberOfRows

    Column (
        modifier = modifier
            .width(IntrinsicSize.Max)
            .height(keyboardHeight)
            .padding(vertical = 4.dp, horizontal = 2.dp),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        layout.rows.forEach { rowOfKeys ->
            // Split keys into halves
            val splitIndex = (rowOfKeys.size + 1) / 2
            val keysToShow = when (side) {
                KeyboardSide.LEFT -> rowOfKeys.subList(0, splitIndex)
                KeyboardSide.RIGHT -> rowOfKeys.subList(splitIndex, rowOfKeys.size)
            }

            KeyboardHalfRow(
                keys = keysToShow,
                side = side,
                isCapsLockOn = isCapsLockOn,
                isShiftOn=isShiftOn,
                isCtrlOn,
                onKeyEvent = onKeyEvent,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(rowHeight)
            )
        }
    }
}

@Composable
fun KeyboardHalfRow(
    keys: List<Key>,
    side: KeyboardSide,
    isCapsLockOn: Boolean,
    isShiftOn: Boolean,
    isCtrlOn: Boolean,
    onKeyEvent: (Key, KeyGesture) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = when (side) {
            KeyboardSide.LEFT -> Arrangement.End
            KeyboardSide.RIGHT -> Arrangement.Start
        }
    ) {
        keys.forEach { key ->
            KeyboardKey(
                key = key,
                isShiftOn,
                isCapsLockOn,
                isCtrlOn,
                onKeyEvent = onKeyEvent,
            )
        }
    }
}