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

import android.content.Context
import android.os.Handler
import androidx.annotation.OptIn
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.audio.AudioRendererEventListener
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.MediaCodecAudioRenderer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.Renderer

/**
 * Custom RenderersFactory that injects NativeAudioProcessor into ExoPlayer's
 * audio rendering pipeline for native effects processing.
 */
@OptIn(UnstableApi::class)
class NativeRenderersFactory(
    context: Context,
    private val audioEngine: AudioEngine
) : DefaultRenderersFactory(context) {

    private var nativeAudioProcessor: NativeAudioProcessor? = null

    init {
        setExtensionRendererMode(EXTENSION_RENDERER_MODE_PREFER)
    }

    override fun buildAudioSink(
        context: Context,
        enableFloatOutput: Boolean,
        enableAudioTrackPlaybackParams: Boolean
    ): AudioSink {
        // Create our native audio processor
        nativeAudioProcessor = NativeAudioProcessor(audioEngine)
        
        return DefaultAudioSink.Builder(context)
            .setEnableFloatOutput(enableFloatOutput)
            .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
            .setAudioProcessors(arrayOf(nativeAudioProcessor!!))
            .build()
    }

    fun getNativeAudioProcessor(): NativeAudioProcessor? = nativeAudioProcessor
}
