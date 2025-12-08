package com.bsbeetle.bananaspaghetti.utils.sessionHandling

import androidx.compose.runtime.mutableStateListOf

object SessionList {
    val sessions = mutableStateListOf<Session>()
    var activeSessionIndex: Int? = null
}