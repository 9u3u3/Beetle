package com.bsbeetle.bananaspaghetti.utils.ai

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.header
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import io.ktor.http.isSuccess
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// --- wire types (only the fields we send or act on) ---

@Serializable
private data class AnthropicRequest(
        val model: String,
        @SerialName("max_tokens") val maxTokens: Int,
        val system: List<SystemBlock>? = null,
        val messages: List<ApiMessage>,
        val stream: Boolean = true,
        /** `"default"` lets the API retry a declined request on a model it picks. Null omits it. */
        val fallbacks: String? = null
)

@Serializable
private data class SystemBlock(
        val text: String,
        val type: String = "text",
        @SerialName("cache_control") val cacheControl: CacheControl? = null
)

@Serializable private data class CacheControl(val type: String = "ephemeral")

@Serializable private data class ApiMessage(val role: String, val content: String)

@Serializable private data class AnthropicError(val error: ApiErrorDetail? = null)

@Serializable private data class ApiErrorDetail(val type: String? = null, val message: String? = null)

@Serializable
private data class StreamEvent(
        val type: String,
        val delta: StreamDelta? = null,
        val error: ApiErrorDetail? = null
)

@Serializable
private data class StreamDelta(
        val type: String? = null,
        val text: String? = null,
        @SerialName("stop_reason") val stopReason: String? = null
)

/**
 * Anthropic Messages API over raw HTTPS.
 *
 * Raw Ktor rather than the official SDK: the Java SDK is built for a server JVM and pulls a
 * dependency tree that would be heavy on a phone, while the request shape here is small enough that
 * hand-rolling it costs less than carrying that weight into the APK.
 */
object AnthropicProvider : AiProvider {

    private const val TAG = "AnthropicProvider"
    private const val URL = "https://api.anthropic.com/v1/messages"
    private const val VERSION = "2023-06-01"
    private const val DATA_PREFIX = "data:"
    private const val FALLBACK_BETA = "server-side-fallback-2026-07-01"

    /**
     * Models that accept `fallbacks: "default"`. Claude Haiku 5.5 has no server-side fallback, so
     * it is left out.
     */
    private val FALLBACK_MODELS = setOf("claude-opus-5-5", "claude-sonnet-5-5")

    private const val STREAM_REQUEST_TIMEOUT_MS = 30 * 60 * 1000L
    private const val STREAM_STALL_TIMEOUT_MS = 90_000L

    /**
     * Encodes the request body here, not through the client's ContentNegotiation. The API needs
     * fields that equal their Kotlin default (`"stream": true`, `"type": "text"`). A client Json
     * without `encodeDefaults` drops them, and the API then rejects the request.
     */
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
    }

    override val id = AiProviders.ANTHROPIC
    override val displayName = "Anthropic (Claude)"
    override val defaultModel = "claude-opus-5-5"
    override val keyHint = "sk-ant-…"
    override val consoleUrl = "https://console.anthropic.com/settings/keys"
    override val keyNote = "Pay-as-you-go. No free tier — the key needs billing set up."

    override val models =
            listOf(
                    AiModelOption(
                            "claude-opus-5-5",
                            "Claude Opus 5.5",
                            "Most capable · \$4/\$20 per Mtok"
                    ),
                    AiModelOption(
                            "claude-sonnet-5-5",
                            "Claude Sonnet 5.5",
                            "Balanced · \$2/\$10 per Mtok"
                    ),
                    AiModelOption(
                            "claude-haiku-5-5",
                            "Claude Haiku 5.5",
                            "Fastest · lowest cost · \$0.10/\$0.50 per Mtok"
                    )
            )

    override val supersededModels =
            mapOf(
                    "claude-opus-5" to "claude-opus-5-5",
                    "claude-opus-4-8" to "claude-opus-5-5",
                    "claude-opus-4-7" to "claude-opus-5-5",
                    "claude-opus-4-6" to "claude-opus-5-5",
                    "claude-sonnet-5" to "claude-sonnet-5-5",
                    "claude-sonnet-4-6" to "claude-sonnet-5-5",
                    "claude-sonnet-4-5" to "claude-sonnet-5-5",
                    "claude-haiku-4-5" to "claude-haiku-5-5"
            )

    override fun streamChat(
            client: HttpClient,
            apiKey: String,
            request: AiRequest
    ): Flow<String> =
            flow {
                        val body =
                                AnthropicRequest(
                                        model = request.model,
                                        maxTokens = request.maxTokens,
                                        system = buildSystem(request),
                                        messages =
                                                request.turns.map {
                                                    ApiMessage(
                                                            role =
                                                                    if (it.role == AiRole.USER) "user"
                                                                    else "assistant",
                                                            content = it.text
                                                    )
                                                },
                                        stream = true,
                                        fallbacks =
                                                if (request.model in FALLBACK_MODELS) "default"
                                                else null
                                )

                        client.preparePost(URL) {
                                    header("x-api-key", apiKey)
                                    header("anthropic-version", VERSION)
                                    if (body.fallbacks != null) {
                                        header("anthropic-beta", FALLBACK_BETA)
                                    }
                                    setBody(
                                            TextContent(
                                                    json.encodeToString(body),
                                                    ContentType.Application.Json
                                            )
                                    )
                                    timeout {
                                        requestTimeoutMillis = STREAM_REQUEST_TIMEOUT_MS
                                        connectTimeoutMillis = 30_000
                                        socketTimeoutMillis = STREAM_STALL_TIMEOUT_MS
                                    }
                                }
                                .execute { response ->
                                    if (!response.status.isSuccess()) {
                                        throw AiStreamException(
                                                describeError(
                                                        response.status.value,
                                                        response.bodyAsText(),
                                                        request.model
                                                )
                                        )
                                    }

                                    val channel = response.bodyAsChannel()
                                    var stopReason: String? = null

                                    while (true) {
                                        val line = channel.readUTF8Line() ?: break
                                        if (!line.startsWith(DATA_PREFIX)) continue

                                        val payload = line.removePrefix(DATA_PREFIX).trim()
                                        if (payload.isEmpty() || payload == "[DONE]") continue

                                        val event =
                                                runCatching {
                                                            json.decodeFromString<StreamEvent>(payload)
                                                        }
                                                        .getOrNull()
                                                        ?: continue

                                        when (event.type) {
                                            "content_block_delta" -> {
                                                val delta = event.delta
                                                if (delta?.type == "text_delta" &&
                                                                delta.text != null
                                                ) {
                                                    emit(delta.text)
                                                }
                                            }
                                            "message_delta" ->
                                                    stopReason = event.delta?.stopReason ?: stopReason
                                            "error" ->
                                                    throw AiStreamException(
                                                            event.error?.message
                                                                    ?: "The AI service reported an error."
                                                    )
                                        }
                                    }

                                    when (stopReason) {
                                        "refusal" ->
                                                throw AiStreamException(
                                                        "The model declined this request. Rephrasing it usually helps."
                                                )
                                        "max_tokens" ->
                                                Log.w(
                                                        TAG,
                                                        "Hit max_tokens (${request.maxTokens}); output truncated"
                                                )
                                    }
                                }
                    }
                    .flowOn(Dispatchers.IO)

    /**
     * Fixed instructions first, then the open file with a cache breakpoint on it.
     *
     * Caching is a prefix match, so the breakpoint on the file block covers the instructions too,
     * and the varying user question stays in `messages` after everything cached. Files below the
     * model's minimum cacheable prefix silently don't cache — expected, and
     * worth knowing before reading `usage` and concluding it's broken.
     */
    private fun buildSystem(request: AiRequest): List<SystemBlock> {
        val instructions = SystemBlock(request.systemInstructions)
        val content = request.fileContent
        if (content.isNullOrBlank()) return listOf(instructions)

        return listOf(
                instructions,
                SystemBlock(
                        text =
                                "The user's currently open file is `${request.fileName ?: "untitled"}`:\n" +
                                        "```\n$content\n```",
                        cacheControl = CacheControl()
                )
        )
    }

    private fun describeError(status: Int, raw: String, model: String): String {
        val detail =
                runCatching { json.decodeFromString<AnthropicError>(raw) }.getOrNull()?.error?.message

        return when (status) {
            401 -> "Invalid Anthropic API key. Check it in Settings → AI."
            403 -> "Your key doesn't have access to this model."
            404 -> "Unknown model: $model."
            429 -> "Rate limited by Anthropic — try again shortly."
            in 500..599 -> "Anthropic service error — try again."
            else -> detail ?: "Anthropic request failed ($status)."
        }
    }
}
