package com.example.llmui.audio

/**
 * Presety DSP mikrofonu – krok 1 (tylko mapowanie na parametry,
 * bez dodatkowego C++ DSP).
 */
enum class MicDspPreset(
    val uiLabel: String,
    val uiDescription: String
) {
    NEUTRAL(
        uiLabel = "Neutralny",
        uiDescription = "Najbardziej naturalne brzmienie – delikatne działanie DAF."
    ),
    SMOOTH(
        uiLabel = "Wygładzony",
        uiDescription = "Lekko wygładza głos i trochę mocniej miesza efekt DAF."
    ),
    DYNAMIC(
        uiLabel = "Dynamiczny",
        uiDescription = "Nieco mocniejszy efekt DAF i subiektywnie głośniejsze brzmienie."
    )
}
