package com.bsbeetle.bananaspaghetti.utils

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Checks the GitHub releases of the Beetle repository for a newer version.
 */
object UpdatesService {
    private const val LATEST_RELEASE_URL = "https://api.github.com/repos/9u3u3/Beetle/releases/latest"

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Returns the latest release when its version is newer than [currentVersion].
     * Returns null when the app is up to date or the check fails.
     */
    suspend fun fetchLatestUpdate(currentVersion: String): UpdateResponse? {
        return try {
            withContext(Dispatchers.IO) {
                val client = HttpClient(OkHttp)
                val response = try {
                    client.get(LATEST_RELEASE_URL) {
                        header("Accept", "application/vnd.github+json")
                    }
                } finally {
                    client.close()
                }

                if (response.status.value !in 200..299) return@withContext null
                val release = json.decodeFromString<GitHubRelease>(response.bodyAsText())
                if (!isNewer(release.tagName, currentVersion)) return@withContext null

                val version = release.tagName.removePrefix("v")
                UpdateResponse(
                    id = release.tagName.hashCode(),
                    title = "Beetle $version is available",
                    description = release.body?.let(::plainText).takeUnless { it.isNullOrEmpty() }
                        ?: "A new version of Beetle is available on GitHub.",
                    url = release.htmlUrl
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("UpdatesService", "Error fetching update: ${e.message}")
            null
        }
    }

    /** Removes the Markdown marks that the release notes use, because the app shows plain text. */
    fun plainText(markdown: String): String =
        markdown.lines().joinToString("\n") { line ->
            line.replace(Regex("^#+\\s*"), "")
                .replace(Regex("^(\\s*)[-*]\\s+"), "$1• ")
                .replace("**", "")
                .replace("`", "")
        }.trim()

    /** Compares dotted versions such as "v1.2.0" and "1.1.0", number by number. */
    fun isNewer(candidate: String, current: String): Boolean {
        fun parts(v: String) = v.trim().removePrefix("v").split('.', '-')
            .map { part -> part.takeWhile { it.isDigit() }.toIntOrNull() ?: 0 }
        val a = parts(candidate)
        val b = parts(current)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }
}

@Serializable
private data class GitHubRelease(
    @SerialName("tag_name") val tagName: String,
    @SerialName("html_url") val htmlUrl: String,
    val body: String? = null
)

data class UpdateResponse(
    val id: Int,
    val title: String,
    val description: String,
    val url: String
)
