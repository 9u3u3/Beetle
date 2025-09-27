package com.bsbeetle.bananaspaghetti.utils

import com.bsbeetle.bananaspaghetti.R

fun getIconResourceId(
    fileName: String
) : Int {
    return when(fileName.substringAfterLast('.')) {
        "cpp" -> R.drawable.cplusplus_plain
        else -> R.drawable.file_type_text
    }
}