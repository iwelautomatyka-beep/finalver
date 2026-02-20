cd "C:\Users\Janek\Desktop\FluencyCoach"

# Backup current files
Copy-Item "app\src\main\cpp\core\audio_engine.h" "app\src\main\cpp\core\audio_engine.h.bak" -Force
Copy-Item "app\src\main\cpp\core\audio_engine.cpp" "app\src\main\cpp\core\audio_engine.cpp.bak" -Force
Copy-Item "app\src\main\java\com\example\llmui\audio\OboeDsp.kt" "app\src\main\java\com\example\llmui\audio\OboeDsp.kt.bak" -Force

# 1) audio_engine.h
@'
#pragma once
#include "logging.h"
#include "ring_buffer.h"
#include "engine_params.h"
#include "oboe/Oboe.h"
#include "../dsp/dsp_registry.h"
#include "../dsp/gain.h"
#include "../dsp/delay.h"
#include "../dsp/limiter.h"
#include <memory>
#include <atomic>
#include <mutex>
#include <functional>

class InputCallback : public oboe::AudioStreamCallback {
public:
    explicit InputCallback(RingBuffer& rb) : mRing(rb) {}
    oboe::DataCallbackResult onAudioReady(oboe::AudioStream* stream, void* audioData, int32_t numFrames) override {
        float* in = static_cast<float*>(audioData);
        mRing.write(in, numFrames * stream->getChannelCount());
        return oboe::DataCallbackResult::Continue;
    }
private:
    RingBuffer& mRing;
};

class OutputCallback : public oboe::AudioStreamCallback {
public:
    using ProcessFn = std::function<void(float*, int32_t, int32_t)>;

    OutputCallback(RingBuffer& rb, ProcessFn fn) : mRing(rb), mProcess(std::move(fn)) {}

    oboe::DataCallbackResult onAudioReady(
        oboe::AudioStream* stream,
        void* audioData,
        int32_t numFrames
    ) override {
        float* out = static_cast<float*>(audioData);
        const int32_t channels = stream->getChannelCount();
        const int32_t count = numFrames * channels;

        tempBuffer.resize(count);
        int32_t read = mRing.read(tempBuffer.data(), count);
        if (read < count) {
            std::fill(tempBuffer.begin() + read, tempBuffer.end(), 0.0f);
        }

        if (mProcess) {
            mProcess(tempBuffer.data(), numFrames, channels);
        } else {
            std::copy(tempBuffer.begin(), tempBuffer.end(), out);
        }

        std::copy(tempBuffer.begin(), tempBuffer.end(), out);
        return oboe::DataCallbackResult::Continue;
    }

private:
    RingBuffer& mRing;
    ProcessFn mProcess;
    std::vector<float> tempBuffer;
};

class AudioEngineImpl;

class AudioEngine {
public:
    static AudioEngine& instance();

    bool start();
    void stop();
    void restartAsync();

    int addNode(const std::string& name);
    void clearChain();
    void setParam(int nodeId, int paramId, float value);

private:
    AudioEngine();
    ~AudioEngine();

    std::unique_ptr<AudioEngineImpl> impl;
};

'@ | Set-Content "app\src\main\cpp\core\audio_engine.h" -Encoding UTF8

# 2) audio_engine.cpp
@'
#include "audio_engine.h"
#include <thread>
#include <chrono>

AudioEngineImpl::AudioEngineImpl() {
    registry.registerFactory("gain", []{ return std::make_unique<GainNode>(); });
    registry.registerFactory("delay", []{ return std::make_unique<DelayNode>(); });
    registry.registerFactory("limiter", []{ return std::make_unique<LimiterNode>(); });
}

void AudioEngineImpl::restartAsync() {
    std::thread([this]{
        stop();
        std::this_thread::sleep_for(std::chrono::milliseconds(100));
        start();
    }).detach();
}

bool AudioEngineImpl::start() {
    std::lock_guard<std::mutex> lock(mutex);
    if (running) return true;

    params.sampleRate = 48000;
    params.channels = 2;
    params.framesPerBurst = 192;

    inputBuffer = std::make_unique<RingBuffer>(params.sampleRate * params.channels);
    outputBuffer = std::make_unique<RingBuffer>(params.sampleRate * params.channels);

    inputCallback = std::make_unique<InputCallback>(*inputBuffer);
    outputCallback = std::make_unique<OutputCallback>(
        *outputBuffer,
        [this](float* data, int32_t frames, int32_t channels) {
            DspContext ctx;
            ctx.sampleRate = params.sampleRate;
            ctx.channels = channels;
            registry.processChain(chain, data, frames, ctx);
        }
    );

    oboe::AudioStreamBuilder inputBuilder;
    inputBuilder.setDirection(oboe::Direction::Input)
        ->setSharingMode(oboe::SharingMode::Exclusive)
        ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
        ->setFormat(oboe::AudioFormat::Float)
        ->setChannelCount(params.channels)
        ->setSampleRate(params.sampleRate)
        ->setCallback(inputCallback.get());

    if (inputBuilder.openStream(inputStream) != oboe::Result::OK) {
        return false;
    }

    params.framesPerBurst = inputStream->getFramesPerBurst();

    oboe::AudioStreamBuilder outputBuilder;
    outputBuilder.setDirection(oboe::Direction::Output)
        ->setSharingMode(oboe::SharingMode::Exclusive)
        ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
        ->setFormat(oboe::AudioFormat::Float)
        ->setChannelCount(params.channels)
        ->setSampleRate(params.sampleRate)
        ->setCallback(outputCallback.get());

    if (outputBuilder.openStream(outputStream) != oboe::Result::OK) {
        inputStream->close();
        inputStream = nullptr;
        return false;
    }

    inputStream->requestStart();
    outputStream->requestStart();
    running = true;
    return true;
}

void AudioEngineImpl::stop() {
    std::lock_guard<std::mutex> lock(mutex);
    if (!running) return;

    if (inputStream) {
        inputStream->requestStop();
        inputStream->close();
        inputStream = nullptr;
    }

    if (outputStream) {
        outputStream->requestStop();
        outputStream->close();
        outputStream = nullptr;
    }

    running = false;
}

int AudioEngineImpl::addNode(const std::string& name) {
    std::lock_guard<std::mutex> lock(mutex);
    return registry.addNode(name);
}

void AudioEngineImpl::clearChain() {
    std::lock_guard<std::mutex> lock(mutex);
    registry.clearChain();
}

void AudioEngineImpl::setParam(int nodeId, int paramId, float value) {
    std::lock_guard<std::mutex> lock(mutex);
    registry.setParam(nodeId, paramId, value);
}

AudioEngine& AudioEngine::instance() {
    static AudioEngine engine;
    return engine;
}

AudioEngine::AudioEngine() : impl(std::make_unique<AudioEngineImpl>()) {}

AudioEngine::~AudioEngine() = default;

bool AudioEngine::start() {
    return impl->start();
}

void AudioEngine::stop() {
    impl->stop();
}

void AudioEngine::restartAsync() {
    impl->restartAsync();
}

int AudioEngine::addNode(const std::string& name) {
    return impl->addNode(name);
}

void AudioEngine::clearChain() {
    impl->clearChain();
}

void AudioEngine::setParam(int nodeId, int paramId, float value) {
    impl->setParam(nodeId, paramId, value);
}

'@ | Set-Content "app\src\main\cpp\core\audio_engine.cpp" -Encoding UTF8

# 3) limiter.h (new file)
@'
#pragma once
#include "dsp_base.h"
#include <atomic>
#include <algorithm>
#include <cmath>

enum { LIMITER_PARAM_THRESHOLD = 0, LIMITER_PARAM_RATIO = 1 };

class LimiterNode : public DspNode {
public:
    const char* name() const override { return "limiter"; }

    void setParam(int id, float value) override {
        if (id == LIMITER_PARAM_THRESHOLD) {
            float v = std::clamp(value, 0.1f, 1.2f);
            threshold_.store(v);
        } else if (id == LIMITER_PARAM_RATIO) {
            float v = value < 1.0f ? 1.0f : value;
            ratio_.store(v);
        }
    }

    void process(float* interleaved, int32_t frames) override {
        const int ch = ctx_.channels;
        if (ch <= 0) return;

        const float thr = threshold_.load();
        const float r   = ratio_.load();
        const float invRatio = (r <= 1.0f) ? 1.0f : (1.0f / r);

        const int count = frames * ch;
        for (int i = 0; i < count; ++i) {
            float x = interleaved[i];
            float a = std::fabs(x);
            if (a > thr) {
                float sign = x >= 0.0f ? 1.0f : -1.0f;
                float over = a - thr;
                float compressed = thr + over * invRatio;
                x = sign * compressed;
            }

            if (x > 1.0f) x = 1.0f;
            else if (x < -1.0f) x = -1.0f;

            interleaved[i] = x;
        }
    }

private:
    std::atomic<float> threshold_{0.9f};
    std::atomic<float> ratio_{4.0f};
};

'@ | Set-Content "app\src\main\cpp\dsp\limiter.h" -Encoding UTF8

# 4) OboeDsp.kt – wersja z limiterem na koncu lancucha
@'
package com.example.llmui.audio

import com.example.lowlatencymonitor.audio.AudioEngine

/**
 * DAF oparty o AudioEngine (Oboe) z LowLatencyMonitor.
 *
 * Założenia:
 *  - 0 ms  = DAF wyłączony (mix=0, fb=0),
 *  - 60–220 ms = użyteczny zakres DAF, bez feedbacku (fb=0), mix ~0.6,
 *  - brak limitera i brak dodatkowego boostera głośności – tak jak w wersji,
 *    którą oceniałeś jako „bardzo dobrą”.
 */
object OboeDsp {

    private const val GAIN_PARAM_GAIN = 0

    private const val DELAY_PARAM_TIME_MS = 0
    private const val DELAY_PARAM_FEEDBACK = 1
    private const val DELAY_PARAM_MIX = 2

    private var started = false
    private var gainNode = -1
    private var delayNode = -1
    private var limiterNode = -1

    private var currentGain = 1.0f
    private var currentDelayMs = 0
    private var feedbackEnabled = false

    fun start(): Boolean {
        if (started) return true
        return try {
            AudioEngine.start()
            AudioEngine.clearChain()

            // GAIN
            gainNode = AudioEngine.addNode("gain")
            if (gainNode >= 0) {
                AudioEngine.setParam(gainNode, GAIN_PARAM_GAIN, currentGain)
            }

            // DELAY (DAF)
            delayNode = AudioEngine.addNode("delay")
            if (delayNode >= 0) {
                AudioEngine.setParam(
                    delayNode,
                    DELAY_PARAM_TIME_MS,
                    currentDelayMs.toFloat()
                )
                applyDelayMix()
            }

            // LIMITER - safety at the end of chain
            limiterNode = AudioEngine.addNode("limiter")

            started = true
            true
        } catch (_: Throwable) {
            started = false
            false
        }
    }

    fun stop() {
        if (!started) return
        try {
            AudioEngine.stop()
        } catch (_: Throwable) {
        } finally {
            started = false
        }
    }

    fun setDelayMs(value: Int) {
        val clampedUi = value.coerceIn(0, 300)
        currentDelayMs = clampedUi
        if (!started || delayNode < 0) return

        try {
            AudioEngine.setParam(
                delayNode,
                DELAY_PARAM_TIME_MS,
                currentDelayMs.toFloat()
            )
            applyDelayMix()
        } catch (_: Throwable) {
        }
    }

    fun setGain(g: Float) {
        val clamped = g.coerceIn(0.4f, 2.0f)
        currentGain = clamped
        if (!started || gainNode < 0) return
        try {
            AudioEngine.setParam(gainNode, GAIN_PARAM_GAIN, currentGain)
        } catch (_: Throwable) {
        }
    }

    fun setFeedbackMode(enabled: Boolean) {
        feedbackEnabled = enabled
        applyDelayMix()
    }

    fun setTestToneEnabled(enabled: Boolean) {
        // nadal brak osobnego generatora tonu w tym obiekcie –
        // używasz istniejącego test tone z poprzedniej wersji, jeśli jest potrzebny.
    }

    private fun applyDelayMix() {
        if (!started || delayNode < 0) return

        val mix = when {
            currentDelayMs <= 0 -> 0f
            currentDelayMs < 70 -> 0.3f
            currentDelayMs < 140 -> 0.6f
            currentDelayMs < 220 -> 0.8f
            else -> 1.0f
        }

        val fb = if (feedbackEnabled && currentDelayMs > 0) 0.25f else 0f

        try {
            AudioEngine.setParam(delayNode, DELAY_PARAM_MIX, mix)
            AudioEngine.setParam(delayNode, DELAY_PARAM_FEEDBACK, fb)
        } catch (_: Throwable) {
        }
    }

    fun getMinDelayMs(): Int = 0
    fun getRingDelayMs(): Int = currentDelayMs
    fun getGain(): Float = currentGain
}
'@ | Set-Content "app\src\main\java\com\example\llmui\audio\OboeDsp.kt" -Encoding UTF8
