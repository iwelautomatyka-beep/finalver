#pragma once
#include "logging.h"
#include "ring_buffer.h"
#include "engine_params.h"
#include "oboe/Oboe.h"
#include "../dsp/dsp_registry.h"
#include "../dsp/gain.h"
#include "../dsp/delay.h"
#include <memory>
#include <atomic>
#include <mutex>
#include <functional>
#include <cmath>
#include <algorithm>

class InputCallback : public oboe::AudioStreamCallback {
public:
    explicit InputCallback(RingBuffer& rb, std::atomic<float>& levelMeter) : mRing(rb), mLevelMeter(levelMeter) {}
    oboe::DataCallbackResult onAudioReady(oboe::AudioStream* stream, void* audioData, int32_t numFrames) override {
        float* in = static_cast<float*>(audioData);
        float peak = 0.0f;
        const int sampleCount = numFrames * stream->getChannelCount();
        for (int i = 0; i < sampleCount; ++i) {
            peak = std::max(peak, std::fabs(in[i]));
        }
        float smoothed = mLevelMeter.load() * 0.8f + peak * 0.2f;
        mLevelMeter.store(smoothed);
        mRing.write(in, static_cast<size_t>(numFrames * stream->getChannelCount()));
        return oboe::DataCallbackResult::Continue;
    }
private:
    RingBuffer& mRing;
    std::atomic<float>& mLevelMeter;
};

class OutputCallback : public oboe::AudioStreamCallback {
public:
    OutputCallback(RingBuffer& rb, class DspChain& chain, int channels)
        : mRing(rb), mChain(chain), mChannels(channels) {}

    oboe::DataCallbackResult onAudioReady(oboe::AudioStream*, void* audioData, int32_t numFrames) override {
        float* out = static_cast<float*>(audioData);
        mRing.read(out, static_cast<size_t>(numFrames * mChannels));
        mChain.process(out, numFrames);
        return oboe::DataCallbackResult::Continue;
    }
private:
    RingBuffer& mRing;
    DspChain& mChain;
    int mChannels;
};

class StreamErrorCallback : public oboe::AudioStreamErrorCallback {
public:
    explicit StreamErrorCallback(std::function<void()> onDisc) : onDisc_(std::move(onDisc)) {}
    void onErrorAfterClose(oboe::AudioStream*, oboe::Result error) override {
        if (error == oboe::Result::ErrorDisconnected && onDisc_) onDisc_();
    }
private:
    std::function<void()> onDisc_;
};

enum class EngineState { Stopped, Starting, Running, Stopping };

struct AudioEngineImpl {
    oboe::ManagedStream inputStream;
    oboe::ManagedStream outputStream;
    std::unique_ptr<InputCallback>  inputCb;
    std::unique_ptr<OutputCallback> outputCb;
    std::unique_ptr<StreamErrorCallback> inErrCb;
    std::unique_ptr<StreamErrorCallback> outErrCb;

    RingBuffer ring { 48000 * 8 };
    EngineParams params;

    DspRegistry registry;
    DspChain chain;
    DspContext dctx;

    std::atomic<EngineState> state { EngineState::Stopped };
    std::mutex startStopMutex;

    int32_t sampleRate = 48000;
    std::atomic<float> inputLevel { 0.0f };
    std::atomic<int32_t> preferredInputDeviceId { -1 };

    AudioEngineImpl();

    bool start();
    void stop();

    void clearChain();
    int  addNode(const char* type);
    void setParam(int index, int paramId, float value);
    float getInputLevel() const;
    void setPreferredInputDeviceId(int32_t deviceId);

private:
    void restartAsync();
};
