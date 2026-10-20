package com.bsbeetle.bananaspaghetti.utils.ai.agent

import com.bsbeetle.bananaspaghetti.utils.ai.AiStreamException
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
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

internal val agentJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
    explicitNulls = false
}

/**
 * Posts [body] and calls [onEvent] for each server-sent `data:` line that parses as a JSON object.
 *
 * A non-2xx response becomes an [AiStreamException] with the text from [describeError].
 */
internal suspend fun postSse(
        client: HttpClient,
        url: String,
        headers: Map<String, String>,
        body: JsonObject,
        describeError: (status: Int, raw: String) -> String,
        onEvent: (JsonObject) -> Unit
) =
        withContext(Dispatchers.IO) {
            client.preparePost(url) {
                        headers.forEach { (name, value) -> header(name, value) }
                        setBody(TextContent(agentJson.encodeToString(JsonObject.serializer(), body), ContentType.Application.Json))
                        timeout {
                            requestTimeoutMillis = 30 * 60 * 1000L
                            connectTimeoutMillis = 30_000
                            socketTimeoutMillis = 90_000
                        }
                    }
                    .execute { response ->
                        if (!response.status.isSuccess()) {
                            val status = response.status.value
                            throw AiStreamException(
                                    describeError(status, response.bodyAsText()),
                                    retryable = status == 408 || status == 429 || status >= 500
                            )
                        }
                        val channel = response.bodyAsChannel()
                        while (true) {
                            val line = channel.readUTF8Line() ?: break
                            if (!line.startsWith("data:")) continue
                            val payload = line.removePrefix("data:").trim()
                            if (payload.isEmpty() || payload == "[DONE]") continue
                            val event =
                                    runCatching { agentJson.parseToJsonElement(payload) as? JsonObject }
                                            .getOrNull()
                                            ?: continue
                            onEvent(event)
                        }
                    }
        }
