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

    fun connect(params: SshConnectionParams, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _connecting.value = true
            _error.value = null
            try {
                val manager = SshConnectionManager(context)
                val session = manager.connect(params)
                connection = session
                fileSystem = SshFileSystem(session)
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
