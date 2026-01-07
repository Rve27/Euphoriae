package com.oss.euphoriae.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response model from LRCLIB API
 * Endpoint: GET https://lrclib.net/api/get?track_name=X&artist_name=Y
 */
@Serializable
data class LrclibResponse(
    val id: Int? = null,
    val trackName: String? = null,
    val artistName: String? = null,
    val albumName: String? = null,
    val duration: Double? = null,
    val instrumental: Boolean = false,
    val plainLyrics: String? = null,
    val syncedLyrics: String? = null
)
