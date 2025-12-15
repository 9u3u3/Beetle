package com.bsbeetle.bananaspaghetti.directGit.ui

import androidx.compose.ui.graphics.Color

/**
 * Git UI Theme Colors
 *
 * Centralized color definitions for the Git tab UI. Using a consistent color scheme makes the UI
 * look professional and makes it easy to update colors in one place.
 *
 * Color Scheme:
 * - Dark backgrounds for VS Code-like appearance
 * - Blue accents for primary actions
 * - Green for success/additions
 * - Yellow for modifications
 * - Red for deletions/errors
 *
 * Usage:
 * ```kotlin
 * Text(
 *     "Branch: main",
 *     color = GitTheme.AccentBlue
 * )
 * ```
 */
object GitTheme {

    // ========================================================================
    // Background Colors
    // ========================================================================

    /** Main dark background color (VS Code dark theme) */
    val BgDark = Color(0xFF1E1E1E)

    /** Card/surface background (slightly lighter than main bg) */
    val BgCard = Color(0xFF252526)

    /** Hover state background */
    val BgHover = Color(0xFF2D2D2D)

    /** Selected item background */
    val BgSelected = Color(0xFF37373D)

    // ========================================================================
    // Accent Colors
    // ========================================================================

    /** Primary accent color (VS Code blue) */
    val AccentBlue = Color(0xFF007ACC)

    /** Success/addition color (green) */
    val AccentGreen = Color(0xFF73C991)

    /** Warning/modification color (yellow) */
    val AccentYellow = Color(0xFFE2C08D)

    /** Error/deletion color (red) */
    val AccentRed = Color(0xFFF48771)

    /** Warning accent color (orange) */
    val AccentOrange = Color(0xFFFF8C00)

    // ========================================================================
    // Text Colors
    // ========================================================================

    /** Primary text color (high contrast) */
    val TextPrimary = Color(0xFFE1E1E1)

    /** Secondary text color (medium contrast) */
    val TextSecondary = Color(0xFFAAAAAA)

    /** Tertiary text color (low contrast, for hints) */
    val TextTertiary = Color(0xFF666666)

    // ========================================================================
    // Border Colors
    // ========================================================================

    /** Standard border color */
    val BorderColor = Color(0xFF333333)

    /** Focused border color */
    val BorderFocused = AccentBlue

    // ========================================================================
    // Status Colors
    // ========================================================================

    /** Untracked file status */
    val StatusUntracked = AccentGreen

    /** Modified file status */
    val StatusModified = AccentYellow

    /** Staged file status */
    val StatusStaged = AccentBlue

    /** Deleted file status */
    val StatusDeleted = AccentRed

    // ========================================================================
    // Helper Functions
    // ========================================================================

    /**
     * Gets the appropriate color for a Git file status.
     *
     * @param status Git status code ("U", "M", "S", "D")
     * @return Color for the status
     *
     * Example:
     * ```kotlin
     * val color = GitTheme.getStatusColor("M")
     * // Returns AccentYellow for modified files
     * ```
     */
    fun getStatusColor(status: String): Color {
        return when (status) {
            "U" -> StatusUntracked // Untracked
            "M" -> StatusModified // Modified
            "S" -> StatusStaged // Staged
            "D" -> StatusDeleted // Deleted
            else -> AccentBlue // Default
        }
    }

    /**
     * Gets a semi-transparent version of a status color for backgrounds.
     *
     * @param status Git status code
     * @param alpha Transparency (0.0 to 1.0)
     * @return Color with applied transparency
     *
     * Example:
     * ```kotlin
     * val bgColor = GitTheme.getStatusBackgroundColor("M", 0.2f)
     * // Returns yellow with 20% opacity
     * ```
     */
    fun getStatusBackgroundColor(status: String, alpha: Float = 0.2f): Color {
        return getStatusColor(status).copy(alpha = alpha)
    }
}
