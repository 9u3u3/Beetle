package com.bsbeetle.bananaspaghetti.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Centralized app colors for consistent theming across the app.
 * Use these instead of hardcoding Color(0xFF...) values.
 */
object AppColors {
    // Backgrounds
    val BgPrimary = Color(0xFF1E1E1E)      // Main background
    val BgSecondary = Color(0xFF252526)    // Sidebar/card background
    val BgTertiary = Color(0xFF2D2D2D)     // Elevated cards
    val BgHover = Color(0xFF37373D)        // Hover states
    val BgInput = Color(0xFF3C3C3C)        // Input fields
    
    // Accents
    val AccentBlue = Color(0xFF007ACC)     // Primary accent
    val AccentGreen = Color(0xFF4EC9B0)    // Success/online
    val AccentOrange = Color(0xFFCE9178)   // Warnings
    val AccentRed = Color(0xFFF44747)      // Errors/destructive
    val AccentPurple = Color(0xFFC586C0)   // Special highlights
    val AccentYellow = Color(0xFFDCDCAA)   // Warnings/attention
    
    // Text
    val TextPrimary = Color(0xFFFFFFFF)    // Primary text
    val TextSecondary = Color(0xFFBBBBBB)  // Secondary text
    val TextTertiary = Color(0xFF9E9E9E)   // Muted text
    val TextMuted = Color(0xFF666666)      // Very muted/placeholder
    val TextDisabled = Color(0xFF585858)   // Disabled text
    
    // Borders & Dividers
    val Border = Color(0xFF404040)         // Standard border
    val BorderLight = Color(0xFF3C3C3C)    // Lighter border
    val Divider = Color(0xFF333333)        // Dividers
    
    // Status Colors
    val StatusAdded = Color(0xFF50fa7b)    // Git added
    val StatusModified = Color(0xFFf1fa8c) // Git modified  
    val StatusDeleted = Color(0xFFff5555)  // Git deleted
    
    // Transparency
    val Overlay = Color(0x80000000)        // 50% black overlay
    val OverlayLight = Color(0x40000000)   // 25% black overlay
}
