package com.oss.euphoriae.data.remote

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit

/**
 * Service for fetching lyrics from LRCLIB API
 * https://lrclib.net/
 */
class LrclibService {
    
    private val client = HttpClient(OkHttp) {
        engine {
            config {
                connectTimeout(30, TimeUnit.SECONDS)
                readTimeout(30, TimeUnit.SECONDS)
                writeTimeout(30, TimeUnit.SECONDS)
            }
        }
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }
    
    companion object {
        private const val TAG = "LrclibService"
        private const val BASE_URL = "https://lrclib.net/api"
    }
    
    /**
     * Fetch lyrics by track name and artist
     * Returns null if not found or error occurs
     */
    suspend fun getLyrics(trackName: String, artistName: String): LrclibResponse? {
        return try {
            android.util.Log.d(TAG, "Fetching: track='$trackName', artist='$artistName'")
            
            val response = client.get("$BASE_URL/get") {
                parameter("track_name", trackName)
                parameter("artist_name", artistName)
            }
            
            android.util.Log.d(TAG, "Response status: ${response.status}")
            
            if (response.status == HttpStatusCode.OK) {
                val result = response.body<LrclibResponse>()
                android.util.Log.d(TAG, "Got lyrics: synced=${result.syncedLyrics != null}, plain=${result.plainLyrics != null}")
                result
            } else {
                android.util.Log.w(TAG, "Non-OK response: ${response.status}")
                null
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to fetch lyrics", e)
            null
        }
    }
    
    /**
     * Search for lyrics - tries with full info first, then falls back to simpler query
     */
    suspend fun searchLyrics(
        trackName: String,
        artistName: String,
        albumName: String? = null,
        duration: Long? = null
    ): LrclibResponse? {
        // First try: simple query with just track and artist (most reliable)
        val simpleResult = getLyrics(trackName, artistName)
        if (simpleResult != null) {
            return simpleResult
        }
        
        // Second try: with duration for better matching
        if (duration != null && duration > 0) {
            try {
                android.util.Log.d(TAG, "Trying with duration: ${duration / 1000}s")
                
                val response = client.get("$BASE_URL/get") {
                    parameter("track_name", trackName)
                    parameter("artist_name", artistName)
                    parameter("duration", duration / 1000)
                }
                
                if (response.status == HttpStatusCode.OK) {
                    return response.body<LrclibResponse>()
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Duration search failed", e)
            }
        }
        
        return null
    }
    
    fun close() {
        client.close()
    }
}

