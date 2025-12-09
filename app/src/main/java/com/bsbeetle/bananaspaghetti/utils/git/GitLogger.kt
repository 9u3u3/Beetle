package com.bsbeetle.bananaspaghetti.utils.git

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object GitLogger {
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    fun log(command: String, type: LogType = LogType.INFO) {
        val entry = LogEntry(
            timestamp = dateFormat.format(Date()),
            message = command,
            type = type
        )
        // Keep last 100 logs
        val current = _logs.value.toMutableList()
        current.add(entry)
        if (current.size > 100) {
            current.removeAt(0)
        }
        _logs.value = current
    }

    fun clear() {
        _logs.value = emptyList()
    }
}

data class LogEntry(
    val timestamp: String,
    val message: String,
    val type: LogType
)

enum class LogType {
    INFO, COMMAND, ERROR, SUCCESS
}
