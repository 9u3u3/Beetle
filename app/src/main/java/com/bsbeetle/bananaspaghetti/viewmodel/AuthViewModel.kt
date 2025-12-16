package com.bsbeetle.bananaspaghetti.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class AuthRequest(val email: String)

@Serializable
data class AuthResponse(val authorized: Boolean, val error: String? = null)

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPreferences = application.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    private val _isAuthorized = MutableStateFlow(sharedPreferences.getBoolean("is_authorized", false))
    val isAuthorized: StateFlow<Boolean> = _isAuthorized.asStateFlow()

    private val _userEmail = MutableStateFlow(sharedPreferences.getString("user_email", "") ?: "")
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                isLenient = true
            })
        }
    }

    fun checkWaitlist(email: String) {
        if (email.isBlank()) {
            _authError.value = "Email is required"
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _authError.value = null
            try {
                val url = "https://beetle-backend.vercel.app/api/auth/check-waitlist"
                val response: AuthResponse = client.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(AuthRequest(email))
                }.body()

                if (response.authorized) {
                    _isAuthorized.value = true
                    _userEmail.value = email
                    saveAuthStatus(true, email)
                } else {
                    _isAuthorized.value = false
                    _authError.value = "Access Denied"
                }

            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error checking waitlist", e)
                _authError.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun saveAuthStatus(authorized: Boolean, email: String) {
        sharedPreferences.edit()
            .putBoolean("is_authorized", authorized)
            .putString("user_email", email)
            .apply()
    }
    
    fun logout() {
        _isAuthorized.value = false
        _userEmail.value = ""
        saveAuthStatus(false, "")
    }
}
