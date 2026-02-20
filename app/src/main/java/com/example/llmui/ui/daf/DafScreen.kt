package com.example.llmui.ui.daf

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.llmui.audio.OboeDsp
import kotlin.math.roundToInt

// Proste presety – działają tylko na UI (ustawiają delay + gain)
private enum class DafPreset {
    GENTLE, STANDARD, STRONG
}

@Composable
fun DafScreen() {
    val context = LocalContext.current

    var dafEnabled by rememberSaveable { mutableStateOf(false) }
    var delayMs by rememberSaveable { mutableStateOf(120f) }    // 0–300, realnie 60–220 w silniku
    var gain by rememberSaveable { mutableStateOf(1.1f) }       // 0.4–2.0
    var currentPreset by rememberSaveable { mutableStateOf<DafPreset?>(null) }

    // LED „mic level” – uproszczony: zależy od wzmocnienia i tego, czy DAF jest włączony
    val ledLevel = if (dafEnabled) (gain / 2f).coerceIn(0f, 1f) else 0f

    val ledStatusText = when {
        !dafEnabled -> "DAF wyłączony"
        ledLevel < 0.25f -> "Delikatny odsłuch"
        ledLevel < 0.6f -> "Komfortowy poziom"
        else -> "Uwaga: może być głośno"
    }

    // Sterowanie natywnym silnikiem DAF
    LaunchedEffect(dafEnabled) {
        try {
            if (dafEnabled) {
                OboeDsp.start()
            } else {
                // wyłączamy efekt i silnik
                OboeDsp.setFeedbackMode(false)
                OboeDsp.setDelayMs(0)
                OboeDsp.setGain(1.0f)
                OboeDsp.stop()
            }
        } catch (_: Throwable) {
        }
    }

    LaunchedEffect(delayMs, gain, dafEnabled) {
        if (!dafEnabled) return@LaunchedEffect
        try {
            val d = delayMs.roundToInt()
            OboeDsp.setDelayMs(d)
            OboeDsp.setGain(gain)
            OboeDsp.setFeedbackMode(true)
        } catch (_: Throwable) {
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // NAGŁÓWEK + MIC LEVEL
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f, fill = true),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Tryb DAF – Delayed Auditory Feedback",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Podłącz PRZEWODOWE słuchawki do telefonu. " +
                                "Dziecko słyszy swój głos z opóźnieniem – to pomaga „zawiesić” jąkanie.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Column(
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .width(72.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "MIC",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    MicLevelBar(
                        level = ledLevel,
                        modifier = Modifier
                            .height(52.dp)
                            .width(14.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = ledStatusText,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // BLOK DAF – WŁĄCZNIK + OPÓŹNIENIE + GAIN + PRESETY
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // 1. Główny włącznik DAF z blokadą słuchawek
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f, fill = true),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "DAF – opóźniony odsłuch",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Działa tylko na słuchawkach przewodowych. " +
                                    "Bez słuchawek nie włączysz trybu (ochrona słuchu).",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(
                        checked = dafEnabled,
                        onCheckedChange = { checked ->
                            if (checked) {
                                if (!areHeadphonesConnected(context)) {
                                    Toast
                                        .makeText(
                                            context,
                                            "Podłącz słuchawki przewodowe, żeby włączyć DAF.",
                                            Toast.LENGTH_LONG
                                        )
                                        .show()
                                } else {
                                    dafEnabled = true
                                }
                            } else {
                                dafEnabled = false
                                currentPreset = null
                            }
                        }
                    )
                }

                Divider()

                // 2. Opóźnienie
                Text(
                    text = "Opóźnienie DAF (ms)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Najczęściej użyteczny zakres to 60–200 ms. " +
                            "W okolicach 60–80 ms DAF działa bardzo intensywnie terapeutycznie.",
                    style = MaterialTheme.typography.bodySmall
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Slider(
                        value = delayMs,
                        onValueChange = {
                            delayMs = it
                            currentPreset = null   // ręczne kręcenie = wychodzimy z presetów
                        },
                        valueRange = 0f..300f,
                        steps = 300 / 5 - 1,
                        modifier = Modifier.weight(1f, fill = true),
                        enabled = dafEnabled,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${delayMs.roundToInt()} ms",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Divider()

                // 3. Głośność / gain
                Text(
                    text = "Głośność / wzmocnienie DAF",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Dopasuj tak, żeby głos dziecka był wyraźny, ale bez dyskomfortu i przesterów.",
                    style = MaterialTheme.typography.bodySmall
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Slider(
                        value = gain,
                        onValueChange = {
                            gain = it
                            currentPreset = null
                        },
                        valueRange = 0.4f..2.0f,
                        steps = 8,
                        modifier = Modifier.weight(1f, fill = true),
                        enabled = dafEnabled,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = String.format("%.1f×", gain),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Divider()

                // 4. Presety DAF
                Text(
                    text = "Presety DAF",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Gotowe kombinacje opóźnienia i głośności. " +
                            "Działają tylko gdy DAF jest włączony.",
                    style = MaterialTheme.typography.bodySmall
                )

                DafPresetButton(
                    label = "Łagodny (ok. 70 ms)",
                    description = "Miękki efekt, dobry na start.",
                    selected = currentPreset == DafPreset.GENTLE,
                    enabled = dafEnabled,
                    onClick = {
                        if (!dafEnabled) {
                            Toast.makeText(
                                context,
                                "Najpierw włącz DAF przełącznikiem powyżej.",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            delayMs = 70f
                            gain = 0.9f
                            currentPreset = DafPreset.GENTLE
                        }
                    }
                )

                DafPresetButton(
                    label = "Standard (ok. 120 ms)",
                    description = "Mocny efekt terapeutyczny, codzienne ćwiczenia.",
                    selected = currentPreset == DafPreset.STANDARD,
                    enabled = dafEnabled,
                    onClick = {
                        if (!dafEnabled) {
                            Toast.makeText(
                                context,
                                "Najpierw włącz DAF przełącznikiem powyżej.",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            delayMs = 120f
                            gain = 1.1f
                            currentPreset = DafPreset.STANDARD
                        }
                    }
                )

                DafPresetButton(
                    label = "Mocny (ok. 170 ms)",
                    description = "Najsilniejsze „zawieszenie” jąkania, krótkie sesje.",
                    selected = currentPreset == DafPreset.STRONG,
                    enabled = dafEnabled,
                    onClick = {
                        if (!dafEnabled) {
                            Toast.makeText(
                                context,
                                "Najpierw włącz DAF przełącznikiem powyżej.",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            delayMs = 170f
                            gain = 1.3f
                            currentPreset = DafPreset.STRONG
                        }
                    }
                )
            }
        }

        // Krótki box z tipami
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Jak używać DAF?",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "• Zacznij od presetów: Standard lub Łagodny.\n" +
                            "• Ćwiczenia rób krótko, ale regularnie (kilka minut, kilka razy dziennie).\n" +
                            "• Jeśli dziecko się męczy – zmniejsz głośność albo wyłącz DAF na chwilę.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/**
 * Sprawdza, czy są podłączone przewodowe słuchawki / headset.
 */
private fun areHeadphonesConnected(context: Context): Boolean {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
    val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
    return devices.any { info ->
        info.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                info.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES
    }
}

/**
 * Prosty pionowy LED bar – kilka segmentów „świeci” w zależności od level 0..1.
 */
@Composable
private fun MicLevelBar(
    level: Float,
    modifier: Modifier = Modifier
) {
    val norm = level.coerceIn(0f, 1f)
    val segments = 6

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        for (i in (segments - 1) downTo 0) {
            val threshold = (i + 1).toFloat() / segments.toFloat()
            val isActive = norm >= threshold

            val color = when {
                !isActive -> MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                norm < 0.25f -> MaterialTheme.colorScheme.secondary
                norm < 0.6f -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.error
            }

            Box(
                modifier = Modifier
                    .width(14.dp)
                    .height(6.dp)
                    .background(
                        color = color,
                        shape = RoundedCornerShape(3.dp)
                    )
            )
        }
    }
}

/**
 * Jeden przycisk presetu – pełna szerokość, opis + zaznaczenie aktywnego.
 */
@Composable
private fun DafPresetButton(
    label: String,
    description: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        colors = if (selected) {
            ButtonDefaults.buttonColors()
        } else {
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (selected) "$label  ✓" else label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
