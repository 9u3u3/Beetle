package com.bsbeetle.bananaspaghetti.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bsbeetle.bananaspaghetti.utils.ssh.SshConnectionManager
import com.bsbeetle.bananaspaghetti.utils.ssh.SshConnectionParams
import com.bsbeetle.bananaspaghetti.utils.ssh.SshFileSystem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SshViewModel : ViewModel() {

    companion object {
        private const val TAG = "SshViewModel"
    }

    var connection: com.jcraft.jsch.Session? = null
        private set
    var fileSystem: SshFileSystem? = null
        private set
    var connectionParams: SshConnectionParams? = null
        private set

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    private val _connecting = MutableStateFlow(false)
    val connecting: StateFlow<Boolean> = _connecting.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /** True when the SSH session dropped mid-use (e.g. server timeout, network loss). */
    private val _sessionDown = MutableStateFlow(false)
    val sessionDown: StateFlow<Boolean> = _sessionDown.asStateFlow()

    /** Called by SshFileSystem when it detects the session is no longer alive. */
    fun markSessionDown() {
        if (!_sessionDown.value) {
            _sessionDown.value = true
            _connected.value = false
            Log.w(TAG, "SSH session detected as down — reconnect required")
        }
    }

    fun connect(params: SshConnectionParams, context: Context) {
        if (_connecting.value) return  // already in-flight, skip duplicate call
        viewModelScope.launch(Dispatchers.IO) {
            _connecting.value = true
            _error.value = null
            _sessionDown.value = false   // clear disconnected banner on fresh attempt
            try {
                val manager = SshConnectionManager(context)
                val session = manager.connect(params)
                connection = session
                fileSystem = SshFileSystem(session) { markSessionDown() }
                connectionParams = params
                _connected.value = true
                Log.d(TAG, "Connected to ${params.host}")
            } catch (e: Exception) {
                Log.e(TAG, "Connection failed", e)
                _error.value = e.message ?: "Connection failed"
            } finally {
                _connecting.value = false
            }
        }
    }

    fun disconnect() {
        fileSystem?.disconnect()
        connection?.disconnect()
        connection = null
        fileSystem = null
        connectionParams = null
        _connected.value = false
        _sessionDown.value = false
        Log.d(TAG, "Disconnected")
    }

    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        disconnect()
    }
}
