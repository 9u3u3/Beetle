package com.bsbeetle.bananaspaghetti.utils.preferences

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages editor preferences using SharedPreferences. Handles theme, indentation, display options,
 * and other editor settings.
 */
class EditorPreferences(context: Context) {
    private val prefs: SharedPreferences =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "editor_preferences"

        // Theme
        private const val KEY_THEME = "editor_theme"
        private const val DEFAULT_THEME = "oneDark"

        // Indentation
        private const val KEY_TAB_SIZE = "tab_size"
        private const val KEY_INDENT_WITH_TABS = "indent_with_tabs"

        // Display
        private const val KEY_WORD_WRAP = "word_wrap"
        private const val KEY_LINE_NUMBERS = "line_numbers"
        private const val KEY_RELATIVE_LINE_NUMBERS = "relative_line_numbers"
        private const val KEY_INDENT_GUIDES = "indent_guides"

        // Editing
        private const val KEY_BRACKET_MATCHING = "bracket_matching"
        private const val KEY_AUTO_CLOSE_BRACKETS = "auto_close_brackets"
        private const val KEY_SCROLL_PAST_END = "scroll_past_end"

        // Cursor
        private const val KEY_CURSOR_BLINK_RATE = "cursor_blink_rate"

        // Auto Save
        private const val KEY_AUTO_SAVE = "auto_save"
        private const val KEY_AUTO_SAVE_DELAY = "auto_save_delay"
    }

    // Theme
    var theme: String
        get() = prefs.getString(KEY_THEME, DEFAULT_THEME) ?: DEFAULT_THEME
        set(value) = prefs.edit().putString(KEY_THEME, value).apply()

    // Indentation
    var tabSize: Int
        get() = prefs.getInt(KEY_TAB_SIZE, 4)
        set(value) = prefs.edit().putInt(KEY_TAB_SIZE, value).apply()

    var indentWithTabs: Boolean
        get() = prefs.getBoolean(KEY_INDENT_WITH_TABS, false)
        set(value) = prefs.edit().putBoolean(KEY_INDENT_WITH_TABS, value).apply()

    // Display
    var wordWrap: Boolean
        get() = prefs.getBoolean(KEY_WORD_WRAP, true)
        set(value) = prefs.edit().putBoolean(KEY_WORD_WRAP, value).apply()

    var lineNumbers: Boolean
        get() = prefs.getBoolean(KEY_LINE_NUMBERS, true)
        set(value) = prefs.edit().putBoolean(KEY_LINE_NUMBERS, value).apply()

    var relativeLineNumbers: Boolean
        get() = prefs.getBoolean(KEY_RELATIVE_LINE_NUMBERS, false)
        set(value) = prefs.edit().putBoolean(KEY_RELATIVE_LINE_NUMBERS, value).apply()

    var indentGuides: Boolean
        get() = prefs.getBoolean(KEY_INDENT_GUIDES, false)
        set(value) = prefs.edit().putBoolean(KEY_INDENT_GUIDES, value).apply()

    // Editing
    var bracketMatching: Boolean
        get() = prefs.getBoolean(KEY_BRACKET_MATCHING, true)
        set(value) = prefs.edit().putBoolean(KEY_BRACKET_MATCHING, value).apply()

    var autoCloseBrackets: Boolean
        get() = prefs.getBoolean(KEY_AUTO_CLOSE_BRACKETS, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_CLOSE_BRACKETS, value).apply()

    var scrollPastEnd: Boolean
        get() = prefs.getBoolean(KEY_SCROLL_PAST_END, true)
        set(value) = prefs.edit().putBoolean(KEY_SCROLL_PAST_END, value).apply()

    // Cursor
    var cursorBlinkRate: Int
        get() = prefs.getInt(KEY_CURSOR_BLINK_RATE, 530)
        set(value) = prefs.edit().putInt(KEY_CURSOR_BLINK_RATE, value).apply()

    // Auto Save
    var autoSave: Boolean
        get() = prefs.getBoolean(KEY_AUTO_SAVE, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_SAVE, value).apply()

    var autoSaveDelay: Int
        get() = prefs.getInt(KEY_AUTO_SAVE_DELAY, 5)
        set(value) = prefs.edit().putInt(KEY_AUTO_SAVE_DELAY, value).apply()

    /** Available themes with display names */
    val availableThemes =
            listOf(
                    "oneDark" to "One Dark",
                    "dark" to "VS Code Dark",
                    "monokai" to "Monokai",
                    "github" to "GitHub Dark",
                    "dracula" to "Dracula",
                    "solarizedDark" to "Solarized Dark",
                    "nord" to "Nord"
            )

    /** Get display name for a theme ID */
    fun getThemeDisplayName(themeId: String): String {
        return availableThemes.find { it.first == themeId }?.second ?: themeId
    }
}
