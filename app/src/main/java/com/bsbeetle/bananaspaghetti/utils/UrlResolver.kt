package com.bsbeetle.bananaspaghetti.utils

import java.net.URLEncoder

/** Turns what the user typed in the browser's address bar into a URL to load. */
object UrlResolver {

    private const val SEARCH_URL = "https://www.google.com/search?q="
    private val SCHEME = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*:(//|[^0-9])")
    private val IPV4 = Regex("^\\d{1,3}(\\.\\d{1,3}){3}$")

    fun resolve(raw: String): String {
        val input = raw.trim()
        if (input.isEmpty()) return "about:blank"
        if (SCHEME.containsMatchIn(input)) return input

        val host = input.substringBefore('/').substringBefore('?').substringBefore('#')
                .substringBefore(':')
        return when {
            input.any { it.isWhitespace() } -> search(input)
            isLocalHost(host) -> "http://$input"
            host.contains('.') || host.equals("localhost", true) -> "https://$input"
            else -> search(input)
        }
    }

    /** Dev servers on this device or the local network speak plain http. */
    private fun isLocalHost(host: String): Boolean {
        if (host.equals("localhost", ignoreCase = true) || host.endsWith(".local", true)) return true
        if (!IPV4.matches(host)) return false
        val parts = host.split('.').map { it.toInt() }
        return when {
            parts[0] == 127 || parts[0] == 10 || parts[0] == 0 -> true
            parts[0] == 192 && parts[1] == 168 -> true
            parts[0] == 172 && parts[1] in 16..31 -> true
            else -> false
        }
    }

    private fun search(query: String) = SEARCH_URL + URLEncoder.encode(query, "UTF-8")
}
