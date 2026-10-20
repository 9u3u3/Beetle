package com.bsbeetle.bananaspaghetti.utils

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Service for fetching updates from the API
 */
object UpdatesService {
    private const val BASE_URL = "https://beetle-backend.vercel.app"
    
    private val json = Json { ignoreUnknownKeys = true }
    
    /**
     * Fetches the latest update from the API
     * Returns UpdateResponse or null if failed
     */
    suspend fun fetchLatestUpdate(): UpdateResponse? {
        return try {
            withContext(Dispatchers.IO) {
                val client = HttpClient(OkHttp)
                val response = client.get("$BASE_URL/api/updates/latest")
                client.close()
                
                if (response.status.value in 200..299) {
                    val body = response.bodyAsText()
                    json.decodeFromString<ApiResponse>(body).data
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("UpdatesService", "Error fetching update: ${e.message}")
            null
        }
    }
}

@Serializable
data class ApiResponse(
    val success: Boolean,
    val data: UpdateResponse? = null,
    val message: String? = null
)

@Serializable
data class UpdateResponse(
    val id: Int,
    val created_at: String? = null,
    val title: String,
    val description: String
)
