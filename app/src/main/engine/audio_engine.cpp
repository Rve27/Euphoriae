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

#include "audio_engine.h"
#include <android/log.h>
#include <algorithm>
#include <chrono>

#define LOG_TAG "EuphoriaeAudio"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

namespace euphoriae {

AudioEngine::AudioEngine() {
    LOGI("AudioEngine created");
}

void AudioEngine::processAudio(float* buffer, int32_t numFrames, int32_t channelCount) {
    if (buffer == nullptr || numFrames <= 0) return;
    
    // Start timing
    auto startTime = std::chrono::high_resolution_clock::now();
    
    // Apply effects in order
    float bassBoost = mBassBoost.load();
    if (bassBoost > 0.01f) {
        applyBassBoost(buffer, numFrames, channelCount);
    }
    
    float virtualizer = mVirtualizer.load();
    if (virtualizer > 0.01f && channelCount == 2) {
        applyVirtualizer(buffer, numFrames, channelCount);
    }
    
    // Apply equalizer
    applyEqualizer(buffer, numFrames, channelCount);
    
    // Apply volume last
    float volume = mVolume.load();
    if (std::abs(volume - 1.0f) > 0.001f) {
        applyVolume(buffer, numFrames * channelCount);
    }
    
    // End timing and calculate latency
    auto endTime = std::chrono::high_resolution_clock::now();
    auto duration = std::chrono::duration_cast<std::chrono::microseconds>(endTime - startTime);
    
    // Log every 100 buffers to avoid spam
    static int bufferCount = 0;
    bufferCount++;
    if (bufferCount % 100 == 0) {
        float latencyMs = duration.count() / 1000.0f;
        float bufferDurationMs = (numFrames * 1000.0f) / 48000.0f; // Assuming 48kHz
        LOGI("Processing latency: %.3f ms | Buffer: %.2f ms | Frames: %d | Channels: %d", 
             latencyMs, bufferDurationMs, numFrames, channelCount);
    }
}

void AudioEngine::setVolume(float volume) {
    mVolume.store(std::clamp(volume, 0.0f, 2.0f));
    LOGI("Volume set to: %.2f", volume);
}

void AudioEngine::setBassBoost(float strength) {
    mBassBoost.store(std::clamp(strength, 0.0f, 1.0f));
    LOGI("Bass boost set to: %.2f", strength);
}

void AudioEngine::setVirtualizer(float strength) {
    mVirtualizer.store(std::clamp(strength, 0.0f, 1.0f));
    LOGI("Virtualizer set to: %.2f", strength);
}

void AudioEngine::setEqualizerBand(int band, float gainDb) {
    if (band >= 0 && band < kNumEqualizerBands) {
        mEqualizerBands[band].store(std::clamp(gainDb, -12.0f, 12.0f));
        LOGI("EQ band %d set to: %.2f dB", band, gainDb);
    }
}

void AudioEngine::applyBassBoost(float* buffer, int32_t numFrames, int32_t channelCount) {
    float strength = mBassBoost.load();
    
    // Simple low-pass filter for bass emphasis
    // Cutoff around 150Hz at 44100Hz sample rate
    const float alpha = 0.15f + (strength * 0.15f);
    const float boost = 1.0f + (strength * 1.5f);
    
    for (int32_t i = 0; i < numFrames; i++) {
        for (int32_t ch = 0; ch < std::min(channelCount, 2); ch++) {
            int idx = i * channelCount + ch;
            float sample = buffer[idx];
            
            // Low-pass to extract bass
            mBassState[ch] = mBassState[ch] + alpha * (sample - mBassState[ch]);
            
            // Add boosted bass back
            buffer[idx] = sample + (mBassState[ch] * (boost - 1.0f));
            
            // Soft clip to prevent distortion
            buffer[idx] = std::tanh(buffer[idx]);
        }
    }
}

void AudioEngine::applyVirtualizer(float* buffer, int32_t numFrames, int32_t channelCount) {
    if (channelCount != 2) return;
    
    float strength = mVirtualizer.load();
    
    // Cross-channel mixing for stereo widening
    const float crossMix = strength * 0.4f;
    const float directGain = 1.0f + (strength * 0.2f);
    
    for (int32_t i = 0; i < numFrames; i++) {
        int idx = i * 2;
        float left = buffer[idx];
        float right = buffer[idx + 1];
        
        // Stereo widening by subtracting opposite channel
        buffer[idx] = (left * directGain) - (right * crossMix);
        buffer[idx + 1] = (right * directGain) - (left * crossMix);
    }
}

void AudioEngine::applyEqualizer(float* buffer, int32_t numFrames, int32_t channelCount) {
    // Check if any band has gain
    bool hasGain = false;
    for (int i = 0; i < kNumEqualizerBands; i++) {
        if (std::abs(mEqualizerBands[i].load()) > 0.1f) {
            hasGain = true;
            break;
        }
    }
    if (!hasGain) return;
    
    // Simplified EQ: apply gain based on frequency content estimation
    // This is a simplified approach - full parametric EQ would need proper biquad filters
    for (int32_t i = 0; i < numFrames * channelCount; i++) {
        float sample = buffer[i];
        
        // Apply overall EQ gain (simplified - averages all bands)
        float totalGain = 0.0f;
        for (int b = 0; b < kNumEqualizerBands; b++) {
            totalGain += mEqualizerBands[b].load();
        }
        totalGain = totalGain / kNumEqualizerBands;
        
        // Convert dB to linear gain
        float linearGain = std::pow(10.0f, totalGain / 20.0f);
        buffer[i] = sample * linearGain;
    }
}

void AudioEngine::applyVolume(float* buffer, int32_t numSamples) {
    float volume = mVolume.load();
    for (int32_t i = 0; i < numSamples; i++) {
        buffer[i] *= volume;
    }
}

} // namespace euphoriae