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
 * AudioEngine - Kotlin wrapper for native DSP audio processor
 * 
 * This is a SINGLETON to ensure the same instance is used for both
 * audio processing in MusicPlaybackService and UI control in EqualizerScreen.
 */
class AudioEngine private constructor() {

    companion object {
        private const val TAG = "AudioEngine"
        
        @Volatile
        private var INSTANCE: AudioEngine? = null
        
        fun getInstance(): AudioEngine {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AudioEngine().also { 
                    INSTANCE = it
                    Log.i(TAG, "AudioEngine singleton created")
                }
            }
        }

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

    // ================== Lifecycle ==================

    fun create() {
        if (!isCreated) {
            nativeCreate()
            isCreated = true
            Log.i(TAG, "Audio engine created")
        }
    }

    fun destroy() {
        if (isCreated) {
            nativeDestroy()
            isCreated = false
            Log.i(TAG, "Audio engine destroyed")
        }
    }

    fun processAudio(buffer: FloatArray, numFrames: Int, channelCount: Int) {
        if (isCreated) {
            nativeProcessAudio(buffer, numFrames, channelCount)
        }
    }

    // ================== Basic Effects ==================

    fun setVolume(volume: Float) {
        if (isCreated) nativeSetVolume(volume.coerceIn(0f, 2f))
    }

    fun getVolume(): Float = if (isCreated) nativeGetVolume() else 1f

    fun setBassBoost(strength: Float) {
        if (isCreated) nativeSetBassBoost(strength.coerceIn(0f, 1f))
    }

    fun getBassBoost(): Float = if (isCreated) nativeGetBassBoost() else 0f

    fun setVirtualizer(strength: Float) {
        if (isCreated) nativeSetVirtualizer(strength.coerceIn(0f, 1f))
    }

    fun getVirtualizer(): Float = if (isCreated) nativeGetVirtualizer() else 0f

    fun setEqualizerBand(band: Int, gainDb: Float) {
        if (isCreated && band in 0..9) {
            nativeSetEqualizerBand(band, gainDb.coerceIn(-12f, 12f))
        }
    }

    // ================== Dynamic Processing ==================

    fun setCompressor(strength: Float) {
        if (isCreated) nativeSetCompressor(strength.coerceIn(0f, 1f))
    }

    fun getCompressor(): Float = if (isCreated) nativeGetCompressor() else 0f

    fun setLimiter(ceiling: Float) {
        if (isCreated) nativeSetLimiter(ceiling.coerceIn(0.5f, 1f))
    }

    fun setVolumeLeveler(level: Float) {
        if (isCreated) nativeSetVolumeLeveler(level.coerceIn(0f, 1f))
    }

    // ================== Surround / Spatial ==================

    fun setSurround3D(depth: Float) {
        if (isCreated) nativeSetSurround3D(depth.coerceIn(0f, 1f))
    }

    fun setRoomSize(size: Float) {
        if (isCreated) nativeSetRoomSize(size.coerceIn(0f, 1f))
    }

    fun setSurroundLevel(level: Float) {
        if (isCreated) nativeSetSurroundLevel(level.coerceIn(0f, 1f))
    }

    fun setHeadphoneSurround(enabled: Boolean) {
        if (isCreated) nativeSetHeadphoneSurround(enabled)
    }

    /**
     * Set headphone type for optimization
     * @param type 0=Generic, 1=InEar, 2=OverEar, 3=OpenBack, 4=Studio
     */
    fun setHeadphoneType(type: Int) {
        if (isCreated) nativeSetHeadphoneType(type.coerceIn(0, 4))
    }

    // ================== Audio Enhancement ==================

    fun setClarity(level: Float) {
        if (isCreated) nativeSetClarity(level.coerceIn(0f, 1f))
    }

    fun getClarity(): Float = if (isCreated) nativeGetClarity() else 0f

    fun setTubeWarmth(warmth: Float) {
        if (isCreated) nativeSetTubeWarmth(warmth.coerceIn(0f, 1f))
    }

    fun getTubeWarmth(): Float = if (isCreated) nativeGetTubeWarmth() else 0f

    fun setSpectrumExtension(level: Float) {
        if (isCreated) nativeSetSpectrumExtension(level.coerceIn(0f, 1f))
    }

    fun setTrebleBoost(level: Float) {
        if (isCreated) nativeSetTrebleBoost(level.coerceIn(0f, 1f))
    }

    // ================== Stereo ==================

    fun setStereoBalance(balance: Float) {
        if (isCreated) nativeSetStereoBalance(balance.coerceIn(-1f, 1f))
    }

    fun setChannelSeparation(separation: Float) {
        if (isCreated) nativeSetChannelSeparation(separation.coerceIn(0f, 1f))
    }

    // ================== Native Methods ==================

    // Core
    private external fun nativeCreate()
    private external fun nativeDestroy()
    private external fun nativeProcessAudio(buffer: FloatArray, numFrames: Int, channelCount: Int)

    // Basic effects
    private external fun nativeSetVolume(volume: Float)
    private external fun nativeSetBassBoost(strength: Float)
    private external fun nativeSetVirtualizer(strength: Float)
    private external fun nativeSetEqualizerBand(band: Int, gain: Float)
    private external fun nativeGetVolume(): Float
    private external fun nativeGetBassBoost(): Float
    private external fun nativeGetVirtualizer(): Float

    // Advanced effects
    private external fun nativeSetCompressor(strength: Float)
    private external fun nativeSetLimiter(ceiling: Float)
    private external fun nativeSetSurround3D(depth: Float)
    private external fun nativeSetRoomSize(size: Float)
    private external fun nativeSetClarity(level: Float)
    private external fun nativeSetTubeWarmth(warmth: Float)
    private external fun nativeSetSpectrumExtension(level: Float)
    private external fun nativeSetTrebleBoost(level: Float)
    private external fun nativeSetVolumeLeveler(level: Float)
    private external fun nativeSetStereoBalance(balance: Float)
    private external fun nativeSetChannelSeparation(separation: Float)
    private external fun nativeGetCompressor(): Float
    private external fun nativeGetClarity(): Float
    private external fun nativeGetTubeWarmth(): Float
    private external fun nativeSetSurroundLevel(level: Float)
    private external fun nativeSetHeadphoneSurround(enabled: Boolean)
    private external fun nativeSetHeadphoneType(type: Int)
    private external fun nativeSetReverb(preset: Int, wetMix: Float)
    private external fun nativeGetReverbPreset(): Int

    // ================== Reverb ==================

    /**
     * Set reverb effect
     * @param preset 0=None, 1=SmallRoom, 2=MediumRoom, 3=LargeRoom, 4=MediumHall, 5=LargeHall, 6=Plate
     * @param wetMix Wet/dry mix 0.0 to 1.0
     */
    fun setReverb(preset: Int, wetMix: Float = 0.5f) {
        if (isCreated) nativeSetReverb(preset.coerceIn(0, 6), wetMix.coerceIn(0f, 1f))
    }

    fun getReverbPreset(): Int = if (isCreated) nativeGetReverbPreset() else 0

    // ================== Tempo/Pitch ==================

    /**
     * Set playback tempo (time stretch)
     * @param tempo 0.5 to 2.0 (1.0 = normal speed)
     */
    fun setTempo(tempo: Float) {
        if (isCreated) nativeSetTempo(tempo.coerceIn(0.5f, 2.0f))
    }

    fun getTempo(): Float = if (isCreated) nativeGetTempo() else 1.0f

    /**
     * Set pitch shift
     * @param semitones -12 to +12 semitones
     */
    fun setPitch(semitones: Float) {
        if (isCreated) nativeSetPitch(semitones.coerceIn(-12f, 12f))
    }

    fun getPitch(): Float = if (isCreated) nativeGetPitch() else 0f

    private external fun nativeSetTempo(tempo: Float)
    private external fun nativeSetPitch(semitones: Float)
    private external fun nativeGetTempo(): Float
    private external fun nativeGetPitch(): Float
}
