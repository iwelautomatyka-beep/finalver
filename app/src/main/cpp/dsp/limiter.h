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

