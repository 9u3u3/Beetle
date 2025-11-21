package com.bsbeetle.bananaspaghetti.data

import kotlinx.serialization.Serializable

// Serializable data classes for the json parser to properly parse the request made by client

@Serializable
data class LspMessage(
    val jsonrpc: String = "2.0",
    val id: Int? = null,
    val method: String,
    val params: kotlinx.serialization.json.JsonElement? = null,
    val result: kotlinx.serialization.json.JsonElement? = null,
    val error: LspError? = null
)

@Serializable
data class LspError(
    val code: Int,
    val message: String,
    val data: kotlinx.serialization.json.JsonElement? = null
)

// Common LSP types
@Serializable
data class TextDocumentIdentifier(val uri: String)

@Serializable
data class VersionedTextDocumentIdentifier(val uri: String, val version: Long)

@Serializable
data class Position(val line: Int, val character: Int)

@Serializable
data class Range(val start: Position, val end: Position)

@Serializable
data class TextDocumentItem(
    val uri: String,
    val languageId: String,
    val version: Int,
    val text: String
)

@Serializable
data class ContentChangeEvent(val text: String)

@Serializable
data class Diagnostic(
    val range: Range,
    val severity: Int? = null,
    val message: String,
    val source: String? = null
)

@Serializable
data class PublishDiagnosticsParams(
    val uri: String,
    val diagnostics: List<Diagnostic>
)

@Serializable
data class CompletionContext(val triggerKind: Int)

@Serializable
data class TextDocumentPositionParams(
    val textDocument: TextDocumentIdentifier,
    val position: Position,
    val context: CompletionContext? = null
)

@Serializable
data class DidChangeParams(
    val textDocument: VersionedTextDocumentIdentifier,
    val contentChanges: List<ContentChangeEvent>
)

@Serializable
data class InitializeParams(
    val processId: Int?,
    val rootUri: String?,
    val capabilities: Map<String, kotlinx.serialization.json.JsonElement>
)