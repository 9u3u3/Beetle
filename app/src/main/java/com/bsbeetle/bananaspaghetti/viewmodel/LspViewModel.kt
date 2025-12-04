package com.bsbeetle.bananaspaghetti.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class LspStatus(
    val line: Int = 1,
    val column: Int = 1,
    val errorCount: Int = 0,
    val warningCount: Int = 0,
    val currentSymbol: String = "",
    val currentSymbolType: String = "" // e.g., "function", "class", etc.
)

class LspViewModel : ViewModel() {
    private val _lspStatus = MutableStateFlow(LspStatus())
    val lspStatus: StateFlow<LspStatus> = _lspStatus.asStateFlow()

    fun updateCursorPosition(line: Int, column: Int) {
        _lspStatus.update { it.copy(line = line, column = column) }
    }

    fun updateDiagnostics(errors: Int, warnings: Int) {
        _lspStatus.update { it.copy(errorCount = errors, warningCount = warnings) }
    }
    
    fun updateSymbol(name: String, type: String) {
        _lspStatus.update { it.copy(currentSymbol = name, currentSymbolType = type) }
    }
}
