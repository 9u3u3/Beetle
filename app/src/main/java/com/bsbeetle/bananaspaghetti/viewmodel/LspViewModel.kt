package com.bsbeetle.bananaspaghetti.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class DiagnosticItem(
    val message: String,
    val line: Int,
    val column: Int,
    val severity: String // "error" or "warning"
)

data class BreadcrumbItem(
    val name: String,
    val kind: Int,
    val detail: String = ""
)

data class LspStatus(
    val line: Int = 1,
    val column: Int = 1,
    val errorCount: Int = 0,
    val warningCount: Int = 0,
    val currentSymbol: String = "",
    val currentSymbolType: String = "",
    val diagnostics: List<DiagnosticItem> = emptyList(),
    val breadcrumbs: List<BreadcrumbItem> = emptyList(),
    val isLspStarting: Boolean = false
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
    
    fun updateDiagnosticsList(diagnostics: List<DiagnosticItem>) {
        _lspStatus.update { it.copy(diagnostics = diagnostics) }
    }
    
    fun updateBreadcrumbs(items: List<BreadcrumbItem>) {
        _lspStatus.update { it.copy(breadcrumbs = items) }
    }

    fun setLspStarting(starting: Boolean) {
        _lspStatus.update { it.copy(isLspStarting = starting) }
    }
}
