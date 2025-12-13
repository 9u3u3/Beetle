package com.bsbeetle.bananaspaghetti.data

import com.bsbeetle.bananaspaghetti.model.Key
import com.bsbeetle.bananaspaghetti.model.KeyType
import com.bsbeetle.bananaspaghetti.model.StableKeyboardLayout

object KeyboardLayout {

    // Base weights
    private const val NORMAL_KEY = 1f
    private const val SHIFT_KEY = NORMAL_KEY
    private const val SPACE_KEY = NORMAL_KEY * 3
    private const val ENTER_KEY = NORMAL_KEY * 1.5f
    private const val CTRL_KEY = NORMAL_KEY * 1.2f

    // Page 0: Alphabet layout (ABC)
    val qwertyLayout = StableKeyboardLayout(
        listOf(
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
                Key("m", "M", NORMAL_KEY), Key(text = ",", shiftText = ",", weight = NORMAL_KEY), Key(text = ".", shiftText = ".", weight = NORMAL_KEY)
            ),
            // Row 5: Removed CTRL, added two toggle keys
            listOf(
                Key("!#1", type = KeyType.ACTION_TOGGLE, weight = NORMAL_KEY),      // Next: symbols
                Key("⌨", type = KeyType.ACTION_TOGGLE_BACK, weight = NORMAL_KEY),   // Prev: navigation
                Key("Space", type = KeyType.ACTION_SPACE, weight = SPACE_KEY),
                Key("tab", "\t", type = KeyType.ACTION_TAB),

                Key("Esc", "Esc", NORMAL_KEY, type = KeyType.ACTION_ESCAPE),
                Key(":", ":", NORMAL_KEY),
                Key("↵", type = KeyType.ACTION_ENTER, weight = ENTER_KEY),
                Key("⌫", type = KeyType.ACTION_BACKSPACE, weight = NORMAL_KEY),
            )
        )
    )

    // Page 1: Special characters layout (!#1)
    val editorLayout = StableKeyboardLayout(
        listOf(
            // Row 1: PC Standard Shift-Row (Muscle Memory: 1=!, 2=@, etc.)
            listOf(
                Key("!", "!", NORMAL_KEY), Key("@", "@", NORMAL_KEY), Key("#", "#", NORMAL_KEY),
                Key("$", "$", NORMAL_KEY), Key("%", "%", NORMAL_KEY), Key("^", "^", NORMAL_KEY),
                Key("&", "&", NORMAL_KEY), Key("*", "*", NORMAL_KEY), Key("(", "(", NORMAL_KEY),
                Key(")", ")", NORMAL_KEY),
            ),

            // Row 2: High Frequency Coding
            listOf(
                Key("_", "_", NORMAL_KEY), Key("-", "-", NORMAL_KEY), Key("+", "+", NORMAL_KEY),
                Key("=", "=", NORMAL_KEY), Key("\"", "\"", NORMAL_KEY), Key("'", "'", NORMAL_KEY),
                Key(":", ":", NORMAL_KEY), Key(";", ";", NORMAL_KEY), Key("{", "{", NORMAL_KEY),
                Key("}", "}", NORMAL_KEY),
            ),

            // Row 3: Arrays, Generics, Pathing & Logic
            listOf(
                Key("~", "~", NORMAL_KEY), Key("`", "`", NORMAL_KEY), Key("\\", "\\", NORMAL_KEY),
                Key("|", "|", NORMAL_KEY), Key("[", "[", NORMAL_KEY), Key("]", "]", NORMAL_KEY),
                Key("<", "<", NORMAL_KEY), Key(">", ">", NORMAL_KEY), Key("/", "/", NORMAL_KEY),
                Key("?", "?", NORMAL_KEY),
            ),

            // Row 4: Replaced && with Ctrl
            listOf(
                Key("tab", "\t", type = KeyType.ACTION_TAB, weight = SHIFT_KEY),
                Key("Ctrl", type = KeyType.ACTION_CTRL, weight = NORMAL_KEY), // Moved Ctrl here
                Key("||", "||", NORMAL_KEY),
                Key("==", "==", NORMAL_KEY), Key("!=", "!=", NORMAL_KEY),
                Key("->", "->", NORMAL_KEY), Key("=>", "=>", NORMAL_KEY),
                Key("£", "£", NORMAL_KEY), Key("€", "€", NORMAL_KEY),
                Key(".", ".", NORMAL_KEY)
            ),

            // Row 5: Two toggle keys, no CTRL (moved to row 4)
            listOf(
                Key("⌨", type = KeyType.ACTION_TOGGLE, weight = NORMAL_KEY),       // Next: navigation
                Key("ABC", type = KeyType.ACTION_TOGGLE_BACK, weight = NORMAL_KEY), // Prev: alphabet
                Key("Space", type = KeyType.ACTION_SPACE, weight = SPACE_KEY),
                Key("Esc", "Esc", type = KeyType.ACTION_ESCAPE),

                Key("←", "←", type = KeyType.ACTION_LEFT, weight = NORMAL_KEY),
                Key("→", "→", type = KeyType.ACTION_RIGHT, weight = NORMAL_KEY),
                Key("↵", type = KeyType.ACTION_ENTER, weight = ENTER_KEY),
                Key("⌫", type = KeyType.ACTION_BACKSPACE, weight = NORMAL_KEY),
            )
        )
    )

    // Page 2: Navigation/Terminal layout (⌨)
    val terminalLayout = StableKeyboardLayout(
        listOf(
            // Row 1: Numbers and Symbols
            listOf(
                Key("1", "!", NORMAL_KEY), Key("2", "@", NORMAL_KEY), Key("3", "#", NORMAL_KEY),
                Key("4", "$", NORMAL_KEY), Key("5", "%", NORMAL_KEY), Key("6", "^", NORMAL_KEY),
                Key("7", "&", NORMAL_KEY), Key("8", "*", NORMAL_KEY), Key("9", "(", NORMAL_KEY),
                Key("0", ")", NORMAL_KEY),
            ),

            // Row 2: Navigation and Control
            listOf(
                Key("Esc", "Esc", type = KeyType.ACTION_ESCAPE, weight = NORMAL_KEY),
                Key("/", "/", NORMAL_KEY), Key("-", "-", NORMAL_KEY),
                Key("Home", type = KeyType.ACTION_HOME, weight = NORMAL_KEY),
                Key("Up", type = KeyType.ACTION_UP, weight = NORMAL_KEY),
                Key("End", type = KeyType.ACTION_END, weight = NORMAL_KEY),
                Key("PgUp", type = KeyType.ACTION_PGUP, weight = NORMAL_KEY),
                Key("`", "`", NORMAL_KEY), Key("~", "~", NORMAL_KEY),
                Key("|", "|", NORMAL_KEY)
            ),

            // Row 3: Ctrl, Alt, Arrows
            listOf(
                Key("Tab", "\t", type = KeyType.ACTION_TAB, weight = NORMAL_KEY),
                Key("Ctrl", type = KeyType.ACTION_CTRL, weight = NORMAL_KEY),
                Key("Alt", type = KeyType.ACTION_ALT, weight = NORMAL_KEY),
                Key("Left", type = KeyType.ACTION_LEFT, weight = NORMAL_KEY),
                Key("Down", type = KeyType.ACTION_DOWN, weight = NORMAL_KEY),
                Key("Right", type = KeyType.ACTION_RIGHT, weight = NORMAL_KEY),
                Key("PgDn", type = KeyType.ACTION_PGDN, weight = NORMAL_KEY),
                Key("[", "[", NORMAL_KEY), Key("]", "]", NORMAL_KEY),
                Key("\\", "\\", NORMAL_KEY)
            ),

            // Row 4: Two toggle keys, removed redundant Ctrl
            listOf(
                Key("ABC", type = KeyType.ACTION_TOGGLE, weight = NORMAL_KEY),      // Next: alphabet
                Key("!#1", type = KeyType.ACTION_TOGGLE_BACK, weight = NORMAL_KEY), // Prev: symbols
                Key("Space", type = KeyType.ACTION_SPACE, weight = SPACE_KEY),
                Key("Esc", "Esc", type = KeyType.ACTION_ESCAPE),

                Key("←", "←", type = KeyType.ACTION_LEFT, weight = NORMAL_KEY),
                Key("→", "→", type = KeyType.ACTION_RIGHT, weight = NORMAL_KEY),
                Key("↵", type = KeyType.ACTION_ENTER, weight = ENTER_KEY),
                Key("⌫", type = KeyType.ACTION_BACKSPACE, weight = NORMAL_KEY),
            )
        )
    )
}