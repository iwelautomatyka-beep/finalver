package com.example.llmui.ui.faf

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import kotlin.math.pow
import kotlin.math.roundToInt

private enum class FafPreset {
    LIGHT, MEDIUM, STRONG
}

@Composable
fun FafScreen() {
    val context = LocalContext.current

    var fafEnabled by rememberSaveable { mutableStateOf(false) }
    // zakres -6 .. +6 półtonów, domyślnie lekko w dół
    var pitchSemitones by rememberSaveable { mutableStateOf(-4f) }
    // 0..1 miks efektu
    var mix by rememberSaveable { mutableStateOf(0.7f) }
    var currentPreset by rememberSaveable { mutableStateOf<FafPreset?>(FafPreset.MEDIUM) }

    // start/stop silnika i bazowy FAF
    LaunchedEffect(fafEnabled) {
        if (fafEnabled) {
            val started = OboeDsp.start()
            if (!started) {
                Toast.makeText(context, "Nie udało się uruchomić silnika audio (FAF).", Toast.LENGTH_SHORT).show()
                return@LaunchedEffect
            }
            val ratio = semitonesToRatio(pitchSemitones).coerceIn(0.8f, 1.2f)
            OboeDsp.setFafPitchRatio(ratio)
            OboeDsp.setFafMix(mix)
        } else {
            // wyłącz sam efekt FAF, ale nie zatrzymuj całego silnika
            OboeDsp.setFafMix(0f)
        }
    }

    // reagowanie na suwaki gdy FAF włączony
    LaunchedEffect(pitchSemitones, mix, fafEnabled) {
        if (!fafEnabled) return@LaunchedEffect
        val ratio = semitonesToRatio(pitchSemitones).coerceIn(0.8f, 1.2f)
        OboeDsp.setFafPitchRatio(ratio)
        OboeDsp.setFafMix(mix)
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // NAGŁÓWEK
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
                        text = "Tryb FAF – Frequency Altered Feedback",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Dziecko słyszy swój głos ze zmienioną wysokością. " +
                                "To może dodatkowo „odklejać” jąkanie i bawić się głosem.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Najpierw włącz DAF i sprawdź, że działa stabilnie. " +
                                "FAF jest dodatkiem – mocny efekt, używaj go krótko.",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Light
                    )
                }
            }

            // BLOK FAF – włącznik + pitch + mix + presety
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

                    // 1. Włącznik FAF
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
                                text = "FAF – zmiana wysokości głosu",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Działa na tym samym torze co DAF. " +
                                        "Na początek używaj razem z DAF i tylko przez chwilę.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Switch(
                            checked = fafEnabled,
                            onCheckedChange = { checked ->
                                fafEnabled = checked
                                if (!checked) {
                                    currentPreset = null
                                }
                            }
                        )
                    }

                    Divider()

                    // 2. Wysokość głosu (pitch)
                    Text(
                        text = "Zmiana wysokości głosu (półtony)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Ujemne wartości = głos niższy, dodatnie = wyższy. " +
                                "Najczęściej używa się lekkiego obniżenia (−2..−5).",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Slider(
                            value = pitchSemitones,
                            onValueChange = {
                                pitchSemitones = it
                                currentPreset = null
                            },
                            valueRange = -6f..6f,
                            steps = 11,
                            modifier = Modifier.weight(1f, fill = true),
                            enabled = fafEnabled,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        val semi = pitchSemitones.roundToInt()
                        val txt = when {
                            semi > 0  -> "+${semi}"
                            semi == 0 -> "0"
                            else      -> "$semi"
                        }
                        Text(
                            text = "$txt st",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Divider()

                    // 3. Miks efektu
                    Text(
                        text = "Miks FAF (ile przesterowanego głosu)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "0% = tylko normalny głos, 100% = tylko przesterowany. " +
                                "Na start spróbuj 60–80%.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Slider(
                            value = mix,
                            onValueChange = {
                                mix = it
                                currentPreset = null
                            },
                            valueRange = 0f..1f,
                            steps = 9,
                            modifier = Modifier.weight(1f, fill = true),
                            enabled = fafEnabled,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${(mix * 100).roundToInt()} %",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Divider()

                    // 4. Presety FAF
                    Text(
                        text = "Presety FAF",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Gotowe kombinacje wysokości i miksu. " +
                                "Działają tylko, gdy FAF jest włączony.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    FafPresetButton(
                        label = "Delikatny (ok. −2 st / 50%)",
                        description = "Subtelna zmiana, dobry na start.",
                        selected = currentPreset == FafPreset.LIGHT,
                        enabled = fafEnabled,
                        onClick = {
                            if (!fafEnabled) {
                                Toast.makeText(
                                    context,
                                    "Najpierw włącz FAF przełącznikiem powyżej.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                pitchSemitones = -2f
                                mix = 0.5f
                                currentPreset = FafPreset.LIGHT
                            }
                        }
                    )

                    FafPresetButton(
                        label = "Standard (ok. −4 st / 70%)",
                        description = "Wyraźna zmiana, dobrze słyszalny efekt.",
                        selected = currentPreset == FafPreset.MEDIUM,
                        enabled = fafEnabled,
                        onClick = {
                            if (!fafEnabled) {
                                Toast.makeText(
                                    context,
                                    "Najpierw włącz FAF przełącznikiem powyżej.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                pitchSemitones = -4f
                                mix = 0.7f
                                currentPreset = FafPreset.MEDIUM
                            }
                        }
                    )

                    FafPresetButton(
                        label = "Mocny (ok. −5 st / 90%)",
                        description = "Bardzo wyraźny efekt, krótkie sesje.",
                        selected = currentPreset == FafPreset.STRONG,
                        enabled = fafEnabled,
                        onClick = {
                            if (!fafEnabled) {
                                Toast.makeText(
                                    context,
                                    "Najpierw włącz FAF przełącznikiem powyżej.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                pitchSemitones = -5f
                                mix = 0.9f
                                currentPreset = FafPreset.STRONG
                            }
                        }
                    )
                }
            }

            // TIPY
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
                        text = "Jak używać FAF?",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "• Najpierw upewnij się, że DAF działa i jest wygodny.\n" +
                                "• FAF traktuj jak „specjalny efekt” – krótko, dla zabawy i przełamania schematu.\n" +
                                "• Jeśli dziecko się męczy albo rozprasza – wróć do samego DAF.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

private fun semitonesToRatio(semitones: Float): Float {
    // 2^(semitones/12)
    return 2f.pow(semitones / 12f)
}

@Composable
private fun FafPresetButton(
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
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Start
            )
        }
    }
}
