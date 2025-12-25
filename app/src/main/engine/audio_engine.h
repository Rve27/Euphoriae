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

#ifndef EUPHORIAE_AUDIO_ENGINE_H
#define EUPHORIAE_AUDIO_ENGINE_H

#include <array>
#include <atomic>
#include <cmath>

namespace euphoriae {

/**
 * AudioEngine - Native audio effects processor
 * 
 * Processes audio buffers from ExoPlayer with effects like
 * bass boost, virtualizer, and equalizer.
 */
class AudioEngine {
public:
    AudioEngine();
    ~AudioEngine() = default;

    // Process audio buffer in-place
    void processAudio(float* buffer, int32_t numFrames, int32_t channelCount);
    
    // Effects controls
    void setVolume(float volume);
    void setBassBoost(float strength);
    void setVirtualizer(float strength);
    void setEqualizerBand(int band, float gainDb);
    
    // Getters
    float getVolume() const { return mVolume.load(); }
    float getBassBoost() const { return mBassBoost.load(); }
    float getVirtualizer() const { return mVirtualizer.load(); }

private:
    void applyBassBoost(float* buffer, int32_t numFrames, int32_t channelCount);
    void applyVirtualizer(float* buffer, int32_t numFrames, int32_t channelCount);
    void applyEqualizer(float* buffer, int32_t numFrames, int32_t channelCount);
    void applyVolume(float* buffer, int32_t numSamples);

    // Effects parameters
    std::atomic<float> mVolume{1.0f};
    std::atomic<float> mBassBoost{0.0f};
    std::atomic<float> mVirtualizer{0.0f};
    
    static constexpr int kNumEqualizerBands = 10;
    std::array<std::atomic<float>, kNumEqualizerBands> mEqualizerBands{};
    
    // Bass boost filter state (per channel)
    float mBassState[2] = {0.0f, 0.0f};
    
    // Simple biquad filter coefficients for EQ bands
    struct BiquadState {
        float z1 = 0.0f;
        float z2 = 0.0f;
    };
    std::array<BiquadState, kNumEqualizerBands * 2> mEqStates{}; // stereo
};

} // namespace euphoriae

#endif // EUPHORIAE_AUDIO_ENGINE_H