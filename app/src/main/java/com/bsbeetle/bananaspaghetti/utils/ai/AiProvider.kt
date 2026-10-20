package com.bsbeetle.bananaspaghetti.utils.ai

import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.Flow

/** Which side of the conversation a turn came from. Providers map this to their own vocabulary. */
enum class AiRole {
    USER,
    ASSISTANT
}

data class AiTurn(val role: AiRole, val text: String)

/** A model the user can pick, with a cost hint shown in Settings. */
data class AiModelOption(val id: String, val displayName: String, val hint: String)

/**
 * One request to a provider.
 *
 * [fileName] / [fileContent] are passed through rather than pre-baked into [systemInstructions] so
 * each provider can decide how to attach the open file. That matters for cost: Anthropic wants an
 * explicit `cache_control` breakpoint on the file block, while Gemini caches long prefixes
 * implicitly and needs nothing. Flattening the file into one string here would make the Anthropic
 * cache breakpoint impossible to place.
 */
data class AiRequest(
        val model: String,
        val systemInstructions: String,
        val fileName: String? = null,
        val fileContent: String? = null,
        val turns: List<AiTurn>,
        val maxTokens: Int
)

/** Raised for failures that already carry a message fit to show the user. */
class AiStreamException(message: String) : Exception(message)

/**
 * A BYOK chat backend.
 *
 * Beetle never holds a managed key for any provider — the user brings their own and pays the vendor
 * directly, which is what makes "you will never get a surprise charge from us" true rather than
 * aspirational.
 *
 * Implementations must stream. On a phone the alternative is a long blank wait followed by
 * all-or-nothing, and a large-output request is the shape most likely to hit an HTTP timeout on
 * patchy mobile data.
 */
interface AiProvider {

    /** Stable identifier persisted in preferences. Never show this to the user. */
    val id: String

    val displayName: String

    /** Models offered in the picker, most capable first. */
    val models: List<AiModelOption>

    val defaultModel: String

    /** Placeholder for the key field, so the user can tell they're pasting the right kind of key. */
    val keyHint: String

    /** Where the user gets a key. Shown in Settings. */
    val consoleUrl: String

    /** One-line note shown under the key field — the thing worth knowing before signing up. */
    val keyNote: String

    /**
     * Old model IDs mapped to their current equivalent. Applied on read so a user who never opens
     * Settings still moves forward instead of sitting on an ID that will eventually 404.
     */
    val supersededModels: Map<String, String>
        get() = emptyMap()

    /**
     * Streams assistant text as it is generated. Emits incremental chunks, not the running total.
     *
     * Implementations should throw [AiStreamException] for anything the user can act on (bad key,
     * unknown model, rate limit, content refusal) and let genuine transport failures propagate.
     */
    fun streamChat(client: HttpClient, apiKey: String, request: AiRequest): Flow<String>
}

/** The providers Beetle supports, in the order they appear in Settings. */
object AiProviders {

    const val ANTHROPIC = "anthropic"
    const val GEMINI = "gemini"

    val all: List<AiProvider> = listOf(AnthropicProvider, GeminiProvider)

    fun byId(id: String): AiProvider = all.firstOrNull { it.id == id } ?: AnthropicProvider
}
