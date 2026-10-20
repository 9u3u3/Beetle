package com.bsbeetle.bananaspaghetti.utils.ai.agent

import kotlinx.serialization.json.JsonObject

/**
 * A tool the model can call. [schema] is a JSON Schema object for the input.
 *
 * Both providers take the same JSON Schema, so one definition serves Anthropic and Gemini.
 */
data class ToolSpec(val name: String, val description: String, val schema: JsonObject)

/**
 * One tool call from the model.
 *
 * [input] is null when the streamed arguments did not parse as a JSON object. The loop then sends
 * [rawInput] back as an error result instead of running the tool, so the model can try again.
 */
data class ToolCall(val id: String, val name: String, val input: JsonObject?, val rawInput: String)

/** The text that goes back to the model for one [ToolCall]. */
data class ToolOutcome(val content: String, val isError: Boolean = false)

/** Why a model turn ended. */
enum class StopKind {
    /** The model finished its answer. */
    DONE,
    /** The model wants the results of [AgentTurn.toolCalls]. */
    TOOL_USE,
    /** The turn hit the output token limit. */
    MAX_TOKENS
}

data class AgentTurn(val text: String, val toolCalls: List<ToolCall>, val stop: StopKind)

/**
 * A multi-turn conversation with tools, held in one provider's own wire format.
 *
 * The history must only grow. Anthropic ties thinking blocks to the exact earlier turns, and Gemini
 * ties thought signatures to its parts, so a provider stores each model turn exactly as it arrived
 * and never edits it.
 */
interface AgentConversation {

    fun addUserText(text: String)

    /**
     * Streams one model turn. [onText] receives each new piece of visible text. The finished turn
     * is added to the history before this returns.
     */
    suspend fun runTurn(onText: (String) -> Unit): AgentTurn

    /** Adds the results for every tool call of the last turn, in one message. */
    fun addToolResults(results: List<Pair<ToolCall, ToolOutcome>>)
}
