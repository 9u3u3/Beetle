package com.bsbeetle.bananaspaghetti.utils.ai.agent

import com.bsbeetle.bananaspaghetti.utils.ai.AiStreamException
import com.bsbeetle.bananaspaghetti.utils.ai.GeminiProvider
import com.bsbeetle.bananaspaghetti.utils.ai.GeminiProvider.isTimeout
import io.ktor.client.HttpClient
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * A tool-use conversation with the Gemini API.
 *
 * Model parts are stored exactly as they arrived. Gemini 3 attaches a `thoughtSignature` to some
 * parts and rejects a function-calling turn that comes back without it.
 */
class GeminiAgent(
        private val client: HttpClient,
        private val apiKey: String,
        private val model: String,
        private val system: String,
        tools: List<ToolSpec>,
        private val maxTokens: Int = 32_000
) : AgentConversation {

    private val contents = mutableListOf<JsonObject>()

    private val toolsJson = buildJsonArray {
        add(
                buildJsonObject {
                    putJsonArray("functionDeclarations") {
                        tools.forEach { spec ->
                            add(
                                    buildJsonObject {
                                        put("name", spec.name)
                                        put("description", spec.description)
                                        put("parametersJsonSchema", spec.schema)
                                    }
                            )
                        }
                    }
                }
        )
    }

    override fun addUserText(text: String) {
        appendUserParts(listOf(buildJsonObject { put("text", text) }))
    }

    override fun addToolResults(results: List<Pair<ToolCall, ToolOutcome>>) {
        if (results.isEmpty()) return
        appendUserParts(
                results.map { (call, outcome) ->
                    buildJsonObject {
                        putJsonObject("functionResponse") {
                            put("name", call.name)
                            if (!call.id.startsWith(LOCAL_ID_PREFIX)) put("id", call.id)
                            putJsonObject("response") {
                                put(if (outcome.isError) "error" else "result", outcome.content.ifEmpty { "(no output)" })
                            }
                        }
                    }
                }
        )
    }

    private fun appendUserParts(parts: List<JsonObject>) {
        val last = contents.lastOrNull()
        if (last != null && (last["role"] as? JsonPrimitive)?.contentOrNull == "user") {
            val merged = buildJsonArray {
                (last["parts"] as? JsonArray)?.forEach { add(it) }
                parts.forEach { add(it) }
            }
            contents[contents.lastIndex] = content("user", merged)
        } else {
            contents += content("user", JsonArray(parts))
        }
    }

    override suspend fun runTurn(onText: (String) -> Unit): AgentTurn {
        val body = buildJsonObject {
            put("contents", JsonArray(contents.toList()))
            putJsonObject("systemInstruction") {
                putJsonArray("parts") { add(buildJsonObject { put("text", system) }) }
            }
            put("tools", toolsJson)
            putJsonObject("generationConfig") { put("maxOutputTokens", maxTokens) }
        }

        val parts = mutableListOf<JsonObject>()
        val calls = mutableListOf<ToolCall>()
        val text = StringBuilder()
        var finishReason: String? = null

        try {
            postSse(
                    client,
                    "${GeminiProvider.BASE_URL}/$model:streamGenerateContent?alt=sse",
                    mapOf("x-goog-api-key" to apiKey),
                    body,
                    describeError = { status, raw -> GeminiProvider.describeError(status, raw, model) }
            ) { chunk ->
                (chunk["promptFeedback"] as? JsonObject)?.str("blockReason")?.let {
                    throw AiStreamException("Gemini blocked this prompt ($it). Rephrasing it usually helps.")
                }
                val candidate = (chunk["candidates"] as? JsonArray)?.firstOrNull() as? JsonObject
                        ?: return@postSse
                candidate.str("finishReason")?.let { finishReason = it }
                val newParts = ((candidate["content"] as? JsonObject)?.get("parts") as? JsonArray).orEmpty()
                for (element in newParts) {
                    val part = element as? JsonObject ?: continue
                    parts += part
                    val isThought = (part["thought"] as? JsonPrimitive)?.booleanOrNull == true
                    val partText = part.str("text")
                    if (!isThought && !partText.isNullOrEmpty()) {
                        text.append(partText)
                        onText(partText)
                    }
                    val call = part["functionCall"] as? JsonObject
                    if (call != null) {
                        val name = call.str("name") ?: continue
                        val args = call["args"] as? JsonObject ?: JsonObject(emptyMap())
                        val id = call.str("id") ?: "$LOCAL_ID_PREFIX${calls.size}"
                        calls += ToolCall(id, name, args, args.toString())
                    }
                }
            }
        } catch (e: Exception) {
            if (e.isTimeout()) throw AiStreamException(GeminiProvider.describeTimeout(model))
            throw e
        }

        when (finishReason) {
            "SAFETY", "BLOCKED", "PROHIBITED_CONTENT", "SPII" ->
                    throw AiStreamException("Gemini stopped this response on a safety filter. Rephrasing it usually helps.")
            "RECITATION" ->
                    throw AiStreamException("Gemini stopped because the output matched training data too closely.")
            "MALFORMED_FUNCTION_CALL" ->
                    throw AiStreamException("Gemini sent a tool call it could not format. Try again, or pick another model.")
        }

        if (parts.isNotEmpty()) contents += content("model", JsonArray(parts))
        val stop =
                when {
                    finishReason == "MAX_TOKENS" -> StopKind.MAX_TOKENS
                    calls.isNotEmpty() -> StopKind.TOOL_USE
                    else -> StopKind.DONE
                }
        return AgentTurn(text.toString(), calls, stop)
    }

    private fun content(role: String, parts: JsonArray) = buildJsonObject {
        put("role", role)
        put("parts", parts)
    }

    private companion object {
        /** Older Gemini models send function calls without an ID. Such IDs never go back. */
        const val LOCAL_ID_PREFIX = "local-"
    }
}

private fun JsonObject.str(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull
