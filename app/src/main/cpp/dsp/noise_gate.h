#pragma once

#include "dsp_base.h"
#include <atomic>
#include <algorithm>
#include <cmath>

enum { NOISE_GATE_PARAM_THRESHOLD = 0, NOISE_GATE_PARAM_ATTENUATION = 1 };

class NoiseGateNode : public DspNode {
public:
    const char* name() const override { return "noise_gate"; }

    void setParam(int id, float v) override {
        if (id == NOISE_GATE_PARAM_THRESHOLD) {
            threshold_.store(std::clamp(v, 0.f, 0.2f));
        } else if (id == NOISE_GATE_PARAM_ATTENUATION) {
            attenuation_.store(std::clamp(v, 0.f, 1.f));
        }
    }

    void process(float* interleaved, int32_t frames) override {
        const int ch = ctx_.channels;
        const float th = threshold_.load();
        const float att = attenuation_.load();
        if (th <= 0.f) return;

        for (int f = 0; f < frames; ++f) {
            float peak = 0.0f;
            for (int c = 0; c < ch; ++c) {
                peak = std::max(peak, std::fabs(interleaved[f * ch + c]));
            }

            if (peak < th) {
                for (int c = 0; c < ch; ++c) {
                    interleaved[f * ch + c] *= att;
                }
            }
        }
    }

private:
    std::atomic<float> threshold_{0.0f};
    std::atomic<float> attenuation_{1.0f};
};
