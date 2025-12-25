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

#define LOG_TAG "EuphoriaeAudio"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

namespace euphoriae {

AudioEngine::AudioEngine() {
    LOGI("AudioEngine created");
}

void AudioEngine::processAudio(float* buffer, int32_t numFrames, int32_t channelCount) {
    if (buffer == nullptr || numFrames <= 0) return;
    
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
}

void AudioEngine::setVolume(float volume) {
    mVolume.store(std::clamp(volume, 0.0f, 2.0f));
    LOGD("Volume set to: %.2f", volume);
}

void AudioEngine::setBassBoost(float strength) {
    mBassBoost.store(std::clamp(strength, 0.0f, 1.0f));
    LOGD("Bass boost set to: %.2f", strength);
}

void AudioEngine::setVirtualizer(float strength) {
    mVirtualizer.store(std::clamp(strength, 0.0f, 1.0f));
    LOGD("Virtualizer set to: %.2f", strength);
}

void AudioEngine::setEqualizerBand(int band, float gainDb) {
    if (band >= 0 && band < kNumEqualizerBands) {
        mEqualizerBands[band].store(std::clamp(gainDb, -12.0f, 12.0f));
        LOGD("EQ band %d set to: %.2f dB", band, gainDb);
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