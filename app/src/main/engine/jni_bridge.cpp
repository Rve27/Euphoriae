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

#include <jni.h>
#include "audio_engine.h"
#include <memory>
#include <android/log.h>

#define LOG_TAG "EuphoriaeAudio"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

static std::unique_ptr<euphoriae::AudioEngine> sEngine;

extern "C" {

JNIEXPORT void JNICALL
Java_com_oss_euphoriae_engine_AudioEngine_nativeCreate(JNIEnv *env, jobject thiz) {
    if (!sEngine) {
        sEngine = std::make_unique<euphoriae::AudioEngine>();
        LOGI("Native AudioEngine instance created");
    }
}

JNIEXPORT void JNICALL
Java_com_oss_euphoriae_engine_AudioEngine_nativeDestroy(JNIEnv *env, jobject thiz) {
    sEngine.reset();
    LOGI("Native AudioEngine instance destroyed");
}

JNIEXPORT void JNICALL
Java_com_oss_euphoriae_engine_AudioEngine_nativeProcessAudio(
        JNIEnv *env, 
        jobject thiz, 
        jfloatArray audioBuffer, 
        jint numFrames, 
        jint channelCount) {
    if (!sEngine || audioBuffer == nullptr) return;
    
    jfloat* buffer = env->GetFloatArrayElements(audioBuffer, nullptr);
    if (buffer == nullptr) return;
    
    sEngine->processAudio(buffer, numFrames, channelCount);
    
    // Copy back the modified buffer
    env->ReleaseFloatArrayElements(audioBuffer, buffer, 0);
}

JNIEXPORT void JNICALL
Java_com_oss_euphoriae_engine_AudioEngine_nativeSetVolume(JNIEnv *env, jobject thiz, jfloat volume) {
    if (sEngine) {
        sEngine->setVolume(volume);
    }
}

JNIEXPORT void JNICALL
Java_com_oss_euphoriae_engine_AudioEngine_nativeSetBassBoost(JNIEnv *env, jobject thiz, jfloat strength) {
    if (sEngine) {
        sEngine->setBassBoost(strength);
    }
}

JNIEXPORT void JNICALL
Java_com_oss_euphoriae_engine_AudioEngine_nativeSetVirtualizer(JNIEnv *env, jobject thiz, jfloat strength) {
    if (sEngine) {
        sEngine->setVirtualizer(strength);
    }
}

JNIEXPORT void JNICALL
Java_com_oss_euphoriae_engine_AudioEngine_nativeSetEqualizerBand(JNIEnv *env, jobject thiz, jint band, jfloat gain) {
    if (sEngine) {
        sEngine->setEqualizerBand(band, gain);
    }
}

JNIEXPORT jfloat JNICALL
Java_com_oss_euphoriae_engine_AudioEngine_nativeGetVolume(JNIEnv *env, jobject thiz) {
    return sEngine ? sEngine->getVolume() : 1.0f;
}

JNIEXPORT jfloat JNICALL
Java_com_oss_euphoriae_engine_AudioEngine_nativeGetBassBoost(JNIEnv *env, jobject thiz) {
    return sEngine ? sEngine->getBassBoost() : 0.0f;
}

JNIEXPORT jfloat JNICALL
Java_com_oss_euphoriae_engine_AudioEngine_nativeGetVirtualizer(JNIEnv *env, jobject thiz) {
    return sEngine ? sEngine->getVirtualizer() : 0.0f;
}

} // extern "C"
