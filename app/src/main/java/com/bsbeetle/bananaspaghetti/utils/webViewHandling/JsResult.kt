package com.bsbeetle.bananaspaghetti.utils.webViewHandling

/**
 * Decodes the value that `WebView.evaluateJavascript` gives to its callback.
 *
 * The WebView encodes the JS result as JSON. A string result arrives as a quoted JSON string, and
 * an `undefined` or `null` result arrives as `null`. The decoder reads the escapes in one pass.
 * A chain of `replace` calls cannot do this correctly: source code that contains `"\n"` or `\\`
 * comes back with its backslashes changed.
 *
 * Returns null when the result is missing, `null`, or not a JSON string.
 */
object JsResult {

    fun decodeString(result: String?): String? {
        if (result == null) return null
        val raw = result.trim()
        if (raw.length < 2 || raw.first() != '"' || raw.last() != '"') return null

        val out = StringBuilder(raw.length)
        var i = 1
        val end = raw.length - 1
        while (i < end) {
            val c = raw[i]
            if (c != '\\' || i + 1 >= end) {
                out.append(c)
                i++
                continue
            }
            when (val next = raw[i + 1]) {
                '"' -> out.append('"')
                '\\' -> out.append('\\')
                '/' -> out.append('/')
                'b' -> out.append('\b')
                'f' -> out.append('\u000C')
                'n' -> out.append('\n')
                'r' -> out.append('\r')
                't' -> out.append('\t')
                'u' -> {
                    val hex = if (i + 6 <= end) raw.substring(i + 2, i + 6) else ""
                    val code = hex.toIntOrNull(16)
                    if (hex.length == 4 && code != null) {
                        out.append(code.toChar())
                        i += 6
                        continue
                    }
                    out.append('\\').append(next)
                }
                else -> out.append('\\').append(next)
            }
            i += 2
        }
        return out.toString()
    }
}
