#pragma once

#include "audio_engine.h"
#include <vector>
#include <cstdint>
#include <algorithm>

class FafPitchNode : public DspNode {
public:
    const char* name() const override { return "faf_pitch"; }

public:
    enum ParamId {
        PARAM_PITCH_RATIO = 0, // 1.0 = brak zmiany
        PARAM_MIX         = 1  // 0.0 = dry, 1.0 = tylko FAF
    };

    FafPitchNode() = default;
    ~FafPitchNode() override = default;

    void prepare(const DspContext& ctx) override;
    void setParam(int id, float value) override;
    void process(float* buffer, int32_t numFrames) override;

private:
    int   sampleRate = 48000;
    int   channels   = 1;

    float pitchRatio = 1.0f;
    float mix        = 0.0f;

    std::vector<float> ring;
    int32_t ringFrames = 0;
    int32_t ringWrite  = 0;
    float   ringRead   = 0.0f;

    void allocateRing();
};

