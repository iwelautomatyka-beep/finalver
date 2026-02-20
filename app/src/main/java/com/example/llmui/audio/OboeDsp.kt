package com.example.llmui.audio

import android.util.Log
import com.example.lowlatencymonitor.audio.AudioEngine

/**
 * DSP oparty o AudioEngine (Oboe):
 *  - DAF (delay) + gain,
 *  - FAF (pitch-shift) przez node "faf_pitch".
 */
object OboeDsp {

    // UWAGA: używamy tego samego TAG-a co C++ -> zobaczysz to w logach razem z "Started SR=..."
    private const val TAG = "AudioEngine"

    private const val GAIN_PARAM_GAIN = 0
    private const val NOISE_PARAM_THRESHOLD = 0
    private const val NOISE_PARAM_ATTENUATION = 1

    private const val DELAY_PARAM_TIME_MS = 0
    private const val DELAY_PARAM_FEEDBACK = 1
    private const val DELAY_PARAM_MIX = 2

    private const val FAF_PARAM_PITCH_RATIO = 0
    private const val FAF_PARAM_MIX = 1

    private var started = false

    private var gainNode = -1
    private var delayNode = -1
    private var fafNode = -1
    private var noiseGateNode = -1

    private var currentGain = 1.0f
    private var globalGain = 1.0f
    private var currentDelayMs = 0
    private var feedbackEnabled = false

    private var currentMicPreset: MicDspPreset = MicDspPreset.NEUTRAL

    private var fafPitchRatio = 1.0f
    private var fafMix = 0.0f
    private var noiseSuppressionEnabled = false
    private var preferredInputDeviceId = -1

    // ========== LIFECYCLE ==========

    fun start(): Boolean {
        if (started) {
            Log.i(TAG, "OboeDsp.start(): already started, gainNode=$gainNode delayNode=$delayNode fafNode=$fafNode")
            return true
        }

        return try {
            Log.i(TAG, "OboeDsp.start(): AudioEngine.start() + clearChain()")
            AudioEngine.setPreferredInputDeviceId(preferredInputDeviceId)
            AudioEngine.start()
            AudioEngine.clearChain()

            gainNode = AudioEngine.addNode("gain")
            noiseGateNode = AudioEngine.addNode("noise_gate")
            delayNode = AudioEngine.addNode("delay")
            fafNode   = AudioEngine.addNode("faf_pitch")

            Log.i(TAG, "OboeDsp.start(): chain created -> gainNode=$gainNode noiseGateNode=$noiseGateNode delayNode=$delayNode fafNode=$fafNode")

            if (gainNode >= 0) {
                applyCombinedGain()
            }

            if (noiseGateNode >= 0) {
                applyNoiseSuppression()
            }

            if (delayNode >= 0) {
                AudioEngine.setParam(delayNode, DELAY_PARAM_TIME_MS, currentDelayMs.toFloat())
                applyDelayMix()
            }

            if (fafNode >= 0) {
                AudioEngine.setParam(fafNode, FAF_PARAM_PITCH_RATIO, fafPitchRatio)
                AudioEngine.setParam(fafNode, FAF_PARAM_MIX, fafMix)
            } else {
                Log.e(TAG, "OboeDsp.start(): fafNode == -1, brak fabryki 'faf_pitch' w AudioEngineImpl")
            }

            started = true
            true
        } catch (t: Throwable) {
            Log.e(TAG, "OboeDsp.start(): exception: $t")
            started = false
            false
        }
    }

    fun stop() {
        if (!started) return
        try {
            Log.i(TAG, "OboeDsp.stop(): AudioEngine.stop()")
            AudioEngine.stop()
        } catch (_: Throwable) {
        } finally {
            started = false
            gainNode = -1
            noiseGateNode = -1
            delayNode = -1
            fafNode = -1
        }
    }

    // ========== DAF API ==========

    fun setDelayMs(value: Int) {
        val clampedUi = value.coerceIn(0, 300)
        currentDelayMs = clampedUi

        Log.i(TAG, "OboeDsp.setDelayMs($value) -> $clampedUi, delayNode=$delayNode")

        if (delayNode >= 0) {
            AudioEngine.setParam(delayNode, DELAY_PARAM_TIME_MS, currentDelayMs.toFloat())
            applyDelayMix()
        }
    }

    fun getMinDelayMs(): Int = 0

    fun getRingDelayMs(): Int = currentDelayMs

    fun setGain(g: Float) {
        val clamped = g.coerceIn(0f, 2f)
        currentGain = clamped
        Log.i(TAG, "OboeDsp.setGain($g) -> $clamped, gainNode=$gainNode")
        applyCombinedGain()
    }

    fun setGlobalGain(g: Float) {
        globalGain = g.coerceIn(0.5f, 2.0f)
        applyCombinedGain()
    }

    fun getGlobalGain(): Float = globalGain

    fun setNoiseSuppressionEnabled(enabled: Boolean) {
        noiseSuppressionEnabled = enabled
        applyNoiseSuppression()
    }

    fun isNoiseSuppressionEnabled(): Boolean = noiseSuppressionEnabled

    fun getMicInputLevel(): Float = AudioEngine.getInputLevel().coerceIn(0f, 1f)

    fun setPreferredInputDeviceId(deviceId: Int) {
        if (preferredInputDeviceId == deviceId) return
        preferredInputDeviceId = deviceId
        AudioEngine.setPreferredInputDeviceId(deviceId)
        if (started) {
            stop()
            start()
        }
    }

    fun getPreferredInputDeviceId(): Int = preferredInputDeviceId

    private fun applyCombinedGain() {
        val effective = (currentGain * globalGain).coerceIn(0f, 3f)
        if (gainNode >= 0) {
            AudioEngine.setParam(gainNode, GAIN_PARAM_GAIN, effective)
        }
    }

    fun getGain(): Float = currentGain

    fun setFeedbackMode(enabled: Boolean) {
        feedbackEnabled = enabled
        Log.i(TAG, "OboeDsp.setFeedbackMode($enabled)")
        applyDelayMix()
    }

    fun setTestToneEnabled(enabled: Boolean) {
        // brak generatora tonu – no-op
    }

    fun setMicPreset(preset: MicDspPreset) {
        currentMicPreset = preset
        Log.i(TAG, "OboeDsp.setMicPreset($preset)")
        applyMicPresetGain()
        applyDelayMix()
    }

    // ========== FAF API ==========

    /**
     * 1.0 = brak zmiany, <1 niżej, >1 wyżej (0.8..1.2)
     */
    fun setFafPitchRatio(ratio: Float) {
        val clamped = ratio.coerceIn(0.8f, 1.2f)
        fafPitchRatio = clamped
        Log.i(TAG, "OboeDsp.setFafPitchRatio($ratio) -> $clamped, fafNode=$fafNode")
        if (fafNode >= 0) {
            AudioEngine.setParam(fafNode, FAF_PARAM_PITCH_RATIO, clamped)
        }
    }

    /**
     * 0.0 = tylko normalny głos, 1.0 = tylko FAF.
     */
    fun setFafMix(mix: Float) {
        val clamped = mix.coerceIn(0f, 1f)
        fafMix = clamped
        Log.i(TAG, "OboeDsp.setFafMix($mix) -> $clamped, fafNode=$fafNode")
        if (fafNode >= 0) {
            AudioEngine.setParam(fafNode, FAF_PARAM_MIX, clamped)
        }
    }

    fun getFafPitchRatio(): Float = fafPitchRatio
    fun getFafMix(): Float = fafMix

    // ========== HELPERY ==========

    private fun applyMicPresetGain() {
        val targetGain = when (currentMicPreset) {
            MicDspPreset.NEUTRAL -> 1.0f
            MicDspPreset.SMOOTH  -> 0.8f
            MicDspPreset.DYNAMIC -> 1.3f
        }

        currentGain = targetGain
        Log.i(TAG, "OboeDsp.applyMicPresetGain() -> $targetGain, gainNode=$gainNode")
        applyCombinedGain()
    }

    private fun applyNoiseSuppression() {
        if (noiseGateNode < 0) return
        if (noiseSuppressionEnabled) {
            AudioEngine.setParam(noiseGateNode, NOISE_PARAM_THRESHOLD, 0.035f)
            AudioEngine.setParam(noiseGateNode, NOISE_PARAM_ATTENUATION, 0.10f)
        } else {
            AudioEngine.setParam(noiseGateNode, NOISE_PARAM_THRESHOLD, 0.0f)
            AudioEngine.setParam(noiseGateNode, NOISE_PARAM_ATTENUATION, 1.0f)
        }
    }

    private fun applyDelayMix() {
        if (delayNode < 0) {
            Log.i(TAG, "OboeDsp.applyDelayMix(): delayNode < 0 (brak)")
            return
        }

        if (!feedbackEnabled || currentDelayMs <= 0) {
            Log.i(TAG, "OboeDsp.applyDelayMix(): DAF OFF (feedback=false or delay<=0)")
            AudioEngine.setParam(delayNode, DELAY_PARAM_FEEDBACK, 0f)
            AudioEngine.setParam(delayNode, DELAY_PARAM_MIX, 0f)
            return
        }

        val effectiveMs = currentDelayMs.coerceIn(60, 220)
        Log.i(TAG, "OboeDsp.applyDelayMix(): effectiveMs=$effectiveMs, preset=$currentMicPreset")

        AudioEngine.setParam(delayNode, DELAY_PARAM_TIME_MS, effectiveMs.toFloat())

        val (fb, mix) = when (currentMicPreset) {
            MicDspPreset.NEUTRAL -> 0.0f to 0.60f
            MicDspPreset.SMOOTH  -> 0.0f to 0.45f
            MicDspPreset.DYNAMIC -> 0.0f to 0.80f
        }

        AudioEngine.setParam(delayNode, DELAY_PARAM_FEEDBACK, fb)
        AudioEngine.setParam(delayNode, DELAY_PARAM_MIX, mix)
    }
}
