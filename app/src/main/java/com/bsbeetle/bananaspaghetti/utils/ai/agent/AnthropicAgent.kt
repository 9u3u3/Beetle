package com.bsbeetle.bananaspaghetti.utils.ai.agent

import com.bsbeetle.bananaspaghetti.utils.ai.AiStreamException
import com.bsbeetle.bananaspaghetti.utils.ai.AnthropicProvider
import io.ktor.client.HttpClient
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/**
 * A tool-use conversation with the Anthropic Messages API.
 *
 * The history is kept as raw JSON. Each assistant turn goes back exactly as it arrived, thinking
 * blocks and signatures included. Claude 5.5 models tie thinking blocks to the earlier turns, so an
 * edited history makes the API drop them and also misses the prompt cache.
 */
class AnthropicAgent(
        private val client: HttpClient,
        private val apiKey: String,
        private val model: String,
        system: String,
        tools: List<ToolSpec>,
        private val maxTokens: Int = 32_000
) : AgentConversation {

    private val messages = mutableListOf<JsonObject>()

    private val systemBlocks = buildJsonArray {
        add(
                buildJsonObject {
                    put("type", "text")
                    put("text", system)
                    putJsonObject("cache_control") { put("type", "ephemeral") }
                }
        )
    }

    private val toolsJson = buildJsonArray {
        tools.forEach { spec ->
            add(
                    buildJsonObject {
                        put("name", spec.name)
                        put("description", spec.description)
                        put("input_schema", spec.schema)
                        // Large file bodies stream as the model writes them. The input is then
                        // not checked by the API, so TurnAssembler parses it strictly.
                        put("eager_input_streaming", true)
                    }
            )
        }
    }

    private val useFallbacks = model in AnthropicProvider.FALLBACK_MODELS

    override fun addUserText(text: String) {
        appendUserBlocks(listOf(textBlock(text)))
    }

    override fun addToolResults(results: List<Pair<ToolCall, ToolOutcome>>) {
        if (results.isEmpty()) return
        appendUserBlocks(
                results.map { (call, outcome) ->
                    buildJsonObject {
                        put("type", "tool_result")
                        put("tool_use_id", call.id)
                        put("content", outcome.content.ifEmpty { "(no output)" })
                        if (outcome.isError) put("is_error", true)
                    }
                }
        )
    }

    /**
     * Appends to the last user message when the history already ends with one. That happens after
     * a failed turn, and the API wants user and assistant turns to alternate.
     */
    private fun appendUserBlocks(blocks: List<JsonObject>) {
        val last = messages.lastOrNull()
        if (last != null && last["role"]?.jsonPrimitive?.contentOrNull == "user") {
            val merged = buildJsonArray {
                (last["content"] as? JsonArray)?.forEach { add(it) }
                blocks.forEach { add(it) }
            }
            messages[messages.lastIndex] = message("user", merged)
        } else {
            messages += message("user", JsonArray(blocks))
        }
    }

    override suspend fun runTurn(onText: (String) -> Unit): AgentTurn {
        val body = buildJsonObject {
            put("model", model)
            put("max_tokens", maxTokens)
            put("system", systemBlocks)
            put("messages", JsonArray(messages.toList()))
            put("tools", toolsJson)
            put("stream", true)
            // Automatic caching moves the breakpoint to the end of the history on every request.
            putJsonObject("cache_control") { put("type", "ephemeral") }
            if (useFallbacks) put("fallbacks", "default")
        }
        val headers = buildMap {
            put("x-api-key", apiKey)
            put("anthropic-version", AnthropicProvider.VERSION)
            if (useFallbacks) put("anthropic-beta", AnthropicProvider.FALLBACK_BETA)
        }

        val assembler = TurnAssembler(onText)
        postSse(
                client,
                AnthropicProvider.URL,
                headers,
                body,
                describeError = { status, raw -> AnthropicProvider.describeError(status, raw, model) },
                onEvent = assembler::accept
        )

        val result = assembler.finish()
        if (result.stopReason == "refusal") {
            throw AiStreamException("The model declined this request. Rephrasing it usually helps.")
        }
        if (result.content.isNotEmpty()) messages += message("assistant", JsonArray(result.content))

        val stop =
                when {
                    result.stopReason == "max_tokens" -> StopKind.MAX_TOKENS
                    result.toolCalls.isNotEmpty() -> StopKind.TOOL_USE
                    else -> StopKind.DONE
                }
        return AgentTurn(result.text, result.toolCalls, stop)
    }

    private fun message(role: String, content: JsonArray) = buildJsonObject {
        put("role", role)
        put("content", content)
    }

    private fun textBlock(text: String) = buildJsonObject {
        put("type", "text")
        put("text", text)
    }

    /**
     * Builds the content blocks of one streamed message from its server-sent events.
     *
     * It keeps every block field it does not know, so unknown block types go back unchanged.
     */
    class TurnAssembler(private val onText: (String) -> Unit = {}) {

        class Result(
                val content: List<JsonObject>,
                val toolCalls: List<ToolCall>,
                val text: String,
                val stopReason: String?
        )

        private class Block(val start: JsonObject) {
            val text = StringBuilder()
            val thinking = StringBuilder()
            var signature: String? = null
            val partialJson = StringBuilder()
        }

        private val blocks = sortedMapOf<Int, Block>()
        private var stopReason: String? = null

        fun accept(event: JsonObject) {
            when (event.str("type")) {
                "content_block_start" -> {
                    val index = event.int("index") ?: return
                    val start = event["content_block"] as? JsonObject ?: return
                    blocks[index] = Block(start)
                    // A text block can start with text in it.
                    val initial = start.str("text")
                    if (start.str("type") == "text" && !initial.isNullOrEmpty()) {
                        blocks[index]!!.text.append(initial)
                        onText(initial)
                    }
                }
                "content_block_delta" -> {
                    val block = blocks[event.int("index") ?: return] ?: return
                    val delta = event["delta"] as? JsonObject ?: return
                    when (delta.str("type")) {
                        "text_delta" ->
                                delta.str("text")?.let {
                                    block.text.append(it)
                                    onText(it)
                                }
                        "thinking_delta" -> delta.str("thinking")?.let { block.thinking.append(it) }
                        "signature_delta" -> block.signature = delta.str("signature")
                        "input_json_delta" -> delta.str("partial_json")?.let { block.partialJson.append(it) }
                    }
                }
                "message_delta" ->
                        (event["delta"] as? JsonObject)?.str("stop_reason")?.let { stopReason = it }
                "error" -> {
                    val error = event["error"] as? JsonObject
                    val type = error?.str("type")
                    throw AiStreamException(
                            error?.str("message") ?: "The AI service reported an error.",
                            retryable = type == "overloaded_error" || type == "api_error" || type == "rate_limit_error"
                    )
                }
            }
        }

        fun finish(): Result {
            val ordered = blocks.values.toList()
            // After a fallback inside the turn, the API wants the blocks of the declined model
            // left out: thinking, redacted thinking and tool calls before the last fallback block.
            val boundary = ordered.indexOfLast { it.start.str("type") == "fallback" }

            val content = mutableListOf<JsonObject>()
            val calls = mutableListOf<ToolCall>()
            val text = StringBuilder()
            ordered.forEachIndexed { i, block ->
                val type = block.start.str("type")
                if (i < boundary && type in DROP_BEFORE_FALLBACK) return@forEachIndexed
                when (type) {
                    "text" -> {
                        if (block.text.isEmpty()) return@forEachIndexed
                        text.append(block.text)
                        content += block.start.with("text", JsonPrimitive(block.text.toString()))
                    }
                    "thinking" -> {
                        var out = block.start.with("thinking", JsonPrimitive(block.thinking.toString()))
                        block.signature?.let { out = out.with("signature", JsonPrimitive(it)) }
                        content += out
                    }
                    "tool_use" -> {
                        val raw = block.partialJson.toString()
                        val parsed =
                                if (raw.isBlank()) JsonObject(emptyMap())
                                else runCatching { agentJson.parseToJsonElement(raw).jsonObject }.getOrNull()
                        val id = block.start.str("id") ?: return@forEachIndexed
                        val name = block.start.str("name") ?: return@forEachIndexed
                        // The history must hold a JSON object as input, even when the stream
                        // gave invalid JSON. The tool result then reports the raw text.
                        content += block.start.with("input", parsed ?: JsonObject(emptyMap()))
                        calls += ToolCall(id, name, parsed, raw)
                    }
                    else -> content += block.start
                }
            }
            return Result(content, calls, text.toString(), stopReason)
        }

        private companion object {
            val DROP_BEFORE_FALLBACK = setOf("thinking", "redacted_thinking", "tool_use")
        }
    }
}

private fun JsonObject.str(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull

private fun JsonObject.int(key: String): Int? = (this[key] as? JsonPrimitive)?.contentOrNull?.toIntOrNull()

private fun JsonObject.with(key: String, value: JsonElement) = JsonObject(this + (key to value))
