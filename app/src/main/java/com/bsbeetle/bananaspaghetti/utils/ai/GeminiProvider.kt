package com.bsbeetle.bananaspaghetti.utils.ai

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRequestTimeoutException
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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// --- wire types (only the fields we send or act on) ---

@Serializable private data class Part(val text: String)

@Serializable private data class Content(val parts: List<Part>, val role: String? = null)

@Serializable
private data class GeminiRequest(
        val contents: List<Content>,
        val systemInstruction: Content? = null,
        val generationConfig: GenerationConfig? = null
)

@Serializable private data class GenerationConfig(val maxOutputTokens: Int)

@Serializable
private data class GeminiChunk(
        val candidates: List<Candidate> = emptyList(),
        val promptFeedback: PromptFeedback? = null
)

@Serializable
private data class Candidate(val content: Content? = null, val finishReason: String? = null)

@Serializable private data class PromptFeedback(val blockReason: String? = null)

@Serializable private data class GeminiErrorEnvelope(val error: GeminiErrorDetail? = null)

@Serializable
private data class GeminiErrorDetail(
        val code: Int? = null,
        val message: String? = null,
        val status: String? = null
)

/**
 * Google Gemini API over raw HTTPS, using `:streamGenerateContent?alt=sse`.
 *
 * Two differences from Anthropic worth knowing when reading this file:
 *
 * 1. **Role vocabulary.** Gemini calls the assistant side `model`, not `assistant`, and rejects
 *    anything else — so [AiRole] is mapped rather than passed through.
 * 2. **Caching is implicit.** There is no per-block cache breakpoint to set. Gemini caches long
 *    repeated prefixes on its own, so the open file is simply prepended to the system instruction
 *    and the win comes for free. That is why this provider has nothing resembling Anthropic's
 *    `cache_control` handling — its absence is deliberate, not an omission.
 *
 * Gemini also has a **free tier**, which makes it the lower-friction way for a new user to try the
 * AI features without setting up billing first.
 */
object GeminiProvider : AiProvider {

    private const val TAG = "GeminiProvider"
    internal const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
    private const val DATA_PREFIX = "data:"

    private const val STREAM_REQUEST_TIMEOUT_MS = 30 * 60 * 1000L
    internal const val STREAM_STALL_TIMEOUT_MS = 90_000L

    /** Encodes the request body here so the client's Json settings cannot change the wire format. */
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
    }

    override val id = AiProviders.GEMINI
    override val displayName = "Google (Gemini)"
    override val defaultModel = "gemini-3.5-flash-lite"
    override val keyHint = "AIza…"
    override val consoleUrl = "https://aistudio.google.com/apikey"
    override val keyNote = "Has a free tier — you can try this without setting up billing."

    override val models =
            listOf(
                    AiModelOption(
                            "gemini-3.7-flash",
                            "Gemini 3.7 Flash",
                            "Best for coding · \$0.75/\$3.75 per Mtok"
                    ),
                    AiModelOption(
                            "gemini-3.1-pro-preview",
                            "Gemini 3.1 Pro",
                            "Deepest reasoning · \$2/\$12 per Mtok"
                    ),
                    AiModelOption(
                            "gemini-3.5-flash-lite",
                            "Gemini 3.5 Flash-Lite",
                            "Fastest · \$0.30/\$2.50 per Mtok"
                    ),
                    AiModelOption(
                            "gemini-3.1-flash-lite",
                            "Gemini 3.1 Flash-Lite",
                            "Lowest cost · \$0.25/\$1.50 per Mtok"
                    )
            )

    override val supersededModels =
            mapOf(
                    // Shut down by Google — anyone still pointed at these gets moved forward
                    // rather than discovering it as a 404 mid-edit.
                    "gemini-2.0-flash" to "gemini-3.7-flash",
                    "gemini-2.0-flash-lite" to "gemini-3.5-flash-lite",
                    "gemini-3-pro-preview" to "gemini-3.1-pro-preview",
                    "gemini-2.5-flash" to "gemini-3.7-flash",
                    "gemini-2.5-flash-lite" to "gemini-3.5-flash-lite"
            )

    override fun streamChat(
            client: HttpClient,
            apiKey: String,
            request: AiRequest
    ): Flow<String> =
            flow {
                        val body =
                                GeminiRequest(
                                        contents =
                                                request.turns.map {
                                                    Content(
                                                            parts = listOf(Part(it.text)),
                                                            // Gemini's assistant role is "model".
                                                            role =
                                                                    if (it.role == AiRole.USER) "user"
                                                                    else "model"
                                                    )
                                                },
                                        systemInstruction =
                                                Content(parts = listOf(Part(buildSystemText(request)))),
                                        generationConfig =
                                                GenerationConfig(maxOutputTokens = request.maxTokens)
                                )

                        val url = "$BASE_URL/${request.model}:streamGenerateContent?alt=sse"

                        client.preparePost(url) {
                                    header("x-goog-api-key", apiKey)
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
                                    var finishReason: String? = null

                                    while (true) {
                                        val line = channel.readUTF8Line() ?: break
                                        if (!line.startsWith(DATA_PREFIX)) continue

                                        val payload = line.removePrefix(DATA_PREFIX).trim()
                                        if (payload.isEmpty() || payload == "[DONE]") continue

                                        val chunk =
                                                runCatching {
                                                            json.decodeFromString<GeminiChunk>(payload)
                                                        }
                                                        .getOrNull()
                                                        ?: continue

                                        chunk.promptFeedback?.blockReason?.let {
                                            throw AiStreamException(
                                                    "Gemini blocked this prompt ($it). Rephrasing it usually helps."
                                            )
                                        }

                                        val candidate = chunk.candidates.firstOrNull()
                                        candidate?.finishReason?.let { finishReason = it }

                                        candidate?.content?.parts?.forEach { part ->
                                            if (part.text.isNotEmpty()) emit(part.text)
                                        }
                                    }

                                    when (finishReason) {
                                        "SAFETY", "BLOCKED" ->
                                                throw AiStreamException(
                                                        "Gemini stopped this response on a safety filter. Rephrasing it usually helps."
                                                )
                                        "RECITATION" ->
                                                throw AiStreamException(
                                                        "Gemini stopped because the output matched training data too closely."
                                                )
                                        "MAX_TOKENS" ->
                                                Log.w(
                                                        TAG,
                                                        "Hit maxOutputTokens (${request.maxTokens}); output truncated"
                                                )
                                    }
                                }
                    }
                    .catch { cause ->
                        // A model the key cannot generate with (free-tier `limit: 0`) does not
                        // 429 here — Google accepts the connection and never writes a byte, so
                        // the only signal is our own stall timeout. Raw "Socket timeout has
                        // expired [url=…]" tells the user nothing, so name the likely cause.
                        if (cause.isTimeout()) {
                            throw AiStreamException(describeTimeout(request.model))
                        }
                        throw cause
                    }
                    .flowOn(Dispatchers.IO)

    internal fun Throwable.isTimeout(): Boolean =
            this is HttpRequestTimeoutException ||
                    this is java.net.SocketTimeoutException ||
                    this is java.io.InterruptedIOException

    internal fun describeTimeout(model: String) =
            "$model never responded (waited ${STREAM_STALL_TIMEOUT_MS / 1000}s for the first " +
                    "byte). Google stalls instead of erroring when a key has no quota for a " +
                    "model, so this usually means your key can't generate with $model — free " +
                    "keys can't use the Pro and top Flash tiers. Pick another model in " +
                    "Settings → AI, or enable billing on your key."


    /**
     * Gemini takes one system instruction, so the open file is appended to it rather than carried
     * as a separate cacheable block. Prefix caching is implicit on Google's side, so keeping the
     * stable instructions first and the file after them is still the right order.
     */
    private fun buildSystemText(request: AiRequest): String = buildString {
        append(request.systemInstructions)
        val content = request.fileContent
        if (!content.isNullOrBlank()) {
            append("\n\nThe user's currently open file is `${request.fileName ?: "untitled"}`:\n")
            append("```\n")
            append(content)
            append("\n```")
        }
    }

    internal fun describeError(status: Int, raw: String, model: String): String {
        val detail =
                runCatching { json.decodeFromString<GeminiErrorEnvelope>(raw) }
                        .getOrNull()
                        ?.error
                        ?.message

        return when (status) {
            400 ->
                    // Gemini returns 400 for a malformed key as well as a malformed request, so the
                    // message is the only way to tell the user which one they're looking at.
                    detail?.let { "Gemini rejected the request: $it" }
                            ?: "Gemini rejected the request. Check your API key in Settings → AI."
            401, 403 -> "Invalid Gemini API key, or it lacks access to this model."
            404 -> "Unknown model: $model. Pick another in Settings → AI."
            429 -> "Rate limited by Gemini — the free tier has per-minute limits. Try again shortly."
            in 500..599 -> "Gemini service error — try again."
            else -> detail ?: "Gemini request failed ($status)."
        }
    }
}
