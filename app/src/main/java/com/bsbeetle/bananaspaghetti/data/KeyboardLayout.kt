package com.bsbeetle.bananaspaghetti.data

import com.bsbeetle.bananaspaghetti.model.Key
import com.bsbeetle.bananaspaghetti.model.KeyType
import com.bsbeetle.bananaspaghetti.model.StableKeyboardLayout


object KeyboardLayout {

    // Base weights
    private const val NORMAL_KEY = 1f
    private const val SHIFT_KEY = NORMAL_KEY
    private const val SPACE_KEY = NORMAL_KEY*3
    private const val ENTER_KEY = NORMAL_KEY*1.5f
    private const val CTRL_KEY = NORMAL_KEY*1.2f

    val qwertyLayout = StableKeyboardLayout( listOf(
        // Row 1
        listOf(
            Key("1", "!", NORMAL_KEY), Key("2", "@", NORMAL_KEY), Key("3", "#", NORMAL_KEY),
            Key("4", "$", NORMAL_KEY), Key("5", "%", NORMAL_KEY), Key("6", "^", NORMAL_KEY),
            Key("7", "&", NORMAL_KEY), Key("8", "*", NORMAL_KEY), Key("9", "(", NORMAL_KEY),
            Key("0", ")", NORMAL_KEY)
        ),
        // Row 2
        listOf(
            Key("q", "Q", NORMAL_KEY), Key("w", "W", NORMAL_KEY), Key("e", "E", NORMAL_KEY),
            Key("r", "R", NORMAL_KEY), Key("t", "T", NORMAL_KEY), Key("y", "Y", NORMAL_KEY),
            Key("u", "U", NORMAL_KEY), Key("i", "I", NORMAL_KEY), Key("o", "O", NORMAL_KEY),
            Key("p", "P", NORMAL_KEY)
        ),
        // Row 3
        listOf(
            Key("a", "A", NORMAL_KEY), Key("s", "S", NORMAL_KEY), Key("d", "D", NORMAL_KEY),
            Key("f", "F", NORMAL_KEY), Key("g", "G", NORMAL_KEY), Key("h", "H", NORMAL_KEY),
            Key("j", "J", NORMAL_KEY), Key("k", "K", NORMAL_KEY), Key("l", "L", NORMAL_KEY),
            Key(text = ";", shiftText = ":", weight = NORMAL_KEY)
        ),
        // Row 4
        listOf(
            Key("⇧", shiftText = "⇪", type = KeyType.ACTION_SHIFT, weight = SHIFT_KEY),
            Key("z", "Z", NORMAL_KEY), Key("x", "X", NORMAL_KEY), Key("c", "C", NORMAL_KEY),
            Key("v", "V", NORMAL_KEY), Key("b", "B", NORMAL_KEY), Key("n", "N", NORMAL_KEY),
            Key("m", "M", NORMAL_KEY),Key(text = ",", shiftText = ",", weight = NORMAL_KEY),Key(text = ".", shiftText = ".", weight = NORMAL_KEY)
        ),
        //Row 5
        listOf(
            Key("123?", type = KeyType.ACTION_TOGGLE, weight = NORMAL_KEY),
            Key("Ctrl", type = KeyType.ACTION_CTRL, weight = CTRL_KEY),
            Key("Space", type = KeyType.ACTION_SPACE, weight = SPACE_KEY),
            Key("tab", "\t", type = KeyType.ACTION_TAB, ),


            Key("Esc", "Esc", NORMAL_KEY, type = KeyType.ACTION_ESCAPE),
            Key(":", ":", NORMAL_KEY),
            Key("↵", type = KeyType.ACTION_ENTER, weight = ENTER_KEY),
            Key("⌫", type = KeyType.ACTION_BACKSPACE, weight = NORMAL_KEY),
        )
    ))


    val developerLayout = StableKeyboardLayout(listOf(
        //Row 1
        listOf(
            Key("{", "{", NORMAL_KEY), Key("}", "}", NORMAL_KEY), Key("(", "(", NORMAL_KEY),
            Key(")", ")", NORMAL_KEY), Key("[", "[", NORMAL_KEY), Key("]", "]", NORMAL_KEY),
            Key("<", "<", NORMAL_KEY), Key(">", ">", NORMAL_KEY), Key(";", ";", NORMAL_KEY),
            Key(":", ":", NORMAL_KEY),
        ),

        //Row 2
        listOf(
            Key(".", ".", NORMAL_KEY), Key(",", ",", NORMAL_KEY), Key("\"", "\"", NORMAL_KEY),
            Key("'", "'", NORMAL_KEY), Key("`", "`", NORMAL_KEY), Key("\\\\", "\\\\", NORMAL_KEY),
            Key("/", "/", NORMAL_KEY), Key("|", "|", NORMAL_KEY), Key("&", "&", NORMAL_KEY),
            Key("=", "=", NORMAL_KEY),
        ),

        // Row 3
        listOf(
            Key("+", "+", NORMAL_KEY), Key("-", "-", NORMAL_KEY), Key("*", "*", NORMAL_KEY),
            Key("%", "%", NORMAL_KEY), Key("!", "!", NORMAL_KEY), Key("?", "?", NORMAL_KEY),
            Key("~", "~", NORMAL_KEY), Key("^", "^", NORMAL_KEY), Key("#", "#", NORMAL_KEY),
            Key("$", "$", NORMAL_KEY),
        ),

        // Row 4
        listOf(
            Key("@", "@", NORMAL_KEY), Key(":", ":", NORMAL_KEY), Key("_", "_", NORMAL_KEY),
            Key("?", "?", NORMAL_KEY), Key("&&", "&&", NORMAL_KEY), Key("tab", "\t", type = KeyType.ACTION_TAB, ), // tab/indent
            Key("nL", "\n", NORMAL_KEY), Key("=>", "=>", NORMAL_KEY),Key("→", "→", NORMAL_KEY),
            Key("*", "*", NORMAL_KEY)
        ),

        // Row 5
        listOf(
            Key("\uD83D\uDD24", type = KeyType.ACTION_TOGGLE, weight = NORMAL_KEY),  Key("Ctrl", type = KeyType.ACTION_CTRL, weight = CTRL_KEY),  Key("Space", type = KeyType.ACTION_SPACE, weight = NORMAL_KEY*3),

            Key("Esc", "Esc", NORMAL_KEY, type = KeyType.ACTION_ESCAPE),Key("→", "→", NORMAL_KEY), Key("*", "*", NORMAL_KEY)
        )
    ))
}