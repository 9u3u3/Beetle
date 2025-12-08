package com.bsbeetle.bananaspaghetti.model


// Defines the type of action a key performs
enum class KeyType {
    CHARACTER,
    ACTION_SHIFT,
    ACTION_BACKSPACE,
    ACTION_SPACE,
    ACTION_ENTER,
    ACTION_TOGGLE,

    ACTION_TAB,
    ACTION_CTRL,
    ACTION_ALT,
    ACTION_ESCAPE,
    ACTION_LEFT,
    ACTION_RIGHT,
    ACTION_UP,
    ACTION_DOWN,
    ACTION_HOME,
    ACTION_END,
    ACTION_PGUP,
    ACTION_PGDN
}
// Represents a single key on the keyboard
data class Key(
    val text: String,
    val shiftText: String? = null,
    val weight: Float = 1f, // Relative width of the key
    val type: KeyType = KeyType.CHARACTER
)