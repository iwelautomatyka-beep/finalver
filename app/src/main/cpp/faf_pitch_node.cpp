#include "faf_pitch_node.h"
#include <cmath>
#include <algorithm>

static float clampf(float v, float lo, float hi) {
    return std::max(lo, std::min(hi, v));
}

void FafPitchNode::prepare(const DspContext& ctx) {
    sampleRate = ctx.sampleRate;
    channels   = ctx.channels;
    allocateRing();

    ringWrite = 0;
    ringRead  = 0.0f;
}

void FafPitchNode::allocateRing() {
    const float maxSeconds = 0.4f;
    ringFrames = static_cast<int32_t>(sampleRate * maxSeconds);
    if (ringFrames < 1) ringFrames = 1;

    ring.assign(static_cast<size_t>(ringFrames * channels), 0.0f);
}

void FafPitchNode::setParam(int id, float value) {
    switch (id) {
        case PARAM_PITCH_RATIO:
            pitchRatio = clampf(value, 0.8f, 1.2f);
            break;
        case PARAM_MIX:
            mix = clampf(value, 0.0f, 1.0f);
            break;
        default:
            break;
    }
}

void FafPitchNode::process(float* buffer, int32_t numFrames) {
    const int32_t numChannels = channels;

    if (!buffer || numChannels <= 0) return;
    if (ringFrames <= 0 || ring.empty()) return;

    // zawsze karmimy ring
    for (int32_t i = 0; i < numFrames; ++i) {
        const int32_t baseRingIdx = (ringWrite * numChannels) % (ringFrames * numChannels);
        const int32_t baseBufIdx  = i * numChannels;

        for (int c = 0; c < numChannels; ++c) {
            ring[baseRingIdx + c] = buffer[baseBufIdx + c];
        }

        ringWrite = (ringWrite + 1) % ringFrames;
    }

    if (mix <= 0.001f || pitchRatio == 1.0f) {
        return;
    }

    for (int32_t i = 0; i < numFrames; ++i) {
        const int32_t inIdx = i * numChannels;

        int32_t readFrame0 = static_cast<int32_t>(ringRead);
        float   frac       = ringRead - static_cast<float>(readFrame0);

        if (readFrame0 >= ringFrames) {
            readFrame0 %= ringFrames;
        }
        int32_t readFrame1 = readFrame0 + 1;
        if (readFrame1 >= ringFrames) readFrame1 -= ringFrames;

        const int32_t ringBase0 = (readFrame0 * numChannels) % (ringFrames * numChannels);
        const int32_t ringBase1 = (readFrame1 * numChannels) % (ringFrames * numChannels);

        for (int c = 0; c < numChannels; ++c) {
            float s0 = ring[ringBase0 + c];
            float s1 = ring[ringBase1 + c];
            float shifted = s0 + (s1 - s0) * frac;

            float dry = buffer[inIdx + c];
            float wet = shifted;

            buffer[inIdx + c] = dry * (1.0f - mix) + wet * mix;
        }

        ringRead += pitchRatio;
        if (ringRead >= static_cast<float>(ringFrames)) {
            ringRead -= static_cast<float>(ringFrames);
        }
    }
}

