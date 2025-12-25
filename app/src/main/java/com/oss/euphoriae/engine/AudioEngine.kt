/*
 * Copyright 2025 Euphoriae
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.oss.euphoriae.engine

import android.util.Log

/**
 * AudioEngine - Kotlin wrapper for native audio effects processor
 * 
 * Processes audio buffers with bass boost, virtualizer, and equalizer effects.
 */
class AudioEngine {

    companion object {
        private const val TAG = "AudioEngine"

        init {
            try {
                System.loadLibrary("audio_engine")
                Log.i(TAG, "Native audio engine library loaded")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Failed to load native library: ${e.message}")
            }
        }
    }

    private var isCreated = false

    /**
     * Initialize the native audio engine
     */
    fun create() {
        if (!isCreated) {
            nativeCreate()
            isCreated = true
            Log.i(TAG, "Audio engine created")
        }
    }

    /**
     * Destroy the native audio engine and release resources
     */
    fun destroy() {
        if (isCreated) {
            nativeDestroy()
            isCreated = false
            Log.i(TAG, "Audio engine destroyed")
        }
    }

    /**
     * Process audio buffer in-place with effects
     * @param buffer Float array of audio samples (interleaved if stereo)
     * @param numFrames Number of audio frames
     * @param channelCount Number of channels (1=mono, 2=stereo)
     */
    fun processAudio(buffer: FloatArray, numFrames: Int, channelCount: Int) {
        if (isCreated) {
            nativeProcessAudio(buffer, numFrames, channelCount)
        }
    }

    /**
     * Set master volume
     * @param volume Volume level from 0.0 to 2.0 (1.0 = unity gain)
     */
    fun setVolume(volume: Float) {
        if (isCreated) {
            nativeSetVolume(volume.coerceIn(0f, 2f))
        }
    }

    fun getVolume(): Float = if (isCreated) nativeGetVolume() else 1f

    /**
     * Set bass boost strength
     * @param strength Strength from 0.0 (off) to 1.0 (max)
     */
    fun setBassBoost(strength: Float) {
        if (isCreated) {
            nativeSetBassBoost(strength.coerceIn(0f, 1f))
        }
    }

    fun getBassBoost(): Float = if (isCreated) nativeGetBassBoost() else 0f

    /**
     * Set virtualizer strength (stereo widening)
     * @param strength Strength from 0.0 (off) to 1.0 (max)
     */
    fun setVirtualizer(strength: Float) {
        if (isCreated) {
            nativeSetVirtualizer(strength.coerceIn(0f, 1f))
        }
    }

    fun getVirtualizer(): Float = if (isCreated) nativeGetVirtualizer() else 0f

    /**
     * Set equalizer band gain
     * @param band Band index (0-9)
     * @param gainDb Gain in dB (-12 to +12)
     */
    fun setEqualizerBand(band: Int, gainDb: Float) {
        if (isCreated && band in 0..9) {
            nativeSetEqualizerBand(band, gainDb.coerceIn(-12f, 12f))
        }
    }

    // Native methods
    private external fun nativeCreate()
    private external fun nativeDestroy()
    private external fun nativeProcessAudio(buffer: FloatArray, numFrames: Int, channelCount: Int)
    private external fun nativeSetVolume(volume: Float)
    private external fun nativeSetBassBoost(strength: Float)
    private external fun nativeSetVirtualizer(strength: Float)
    private external fun nativeSetEqualizerBand(band: Int, gain: Float)
    private external fun nativeGetVolume(): Float
    private external fun nativeGetBassBoost(): Float
    private external fun nativeGetVirtualizer(): Float
}
