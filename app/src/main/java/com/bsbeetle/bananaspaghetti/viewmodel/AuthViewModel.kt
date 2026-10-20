package com.bsbeetle.bananaspaghetti.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds the user's contact email locally.
 *
 * There is no access gate any more — the app opens straight to Home. The email is kept only so
 * the profile card and the issue reporter have something to show/attach; it is stored on-device
 * and never checked against a server.
 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPreferences =
        application.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    private val _userEmail = MutableStateFlow(sharedPreferences.getString("user_email", "") ?: "")
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()

    fun setUserEmail(email: String) {
        val trimmed = email.trim()
        _userEmail.value = trimmed
        sharedPreferences.edit().putString("user_email", trimmed).apply()
    }
}
