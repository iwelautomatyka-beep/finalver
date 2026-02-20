package com.example.llmui.ui.settings

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.llmui.audio.OboeDsp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private enum class MicSourceMode { INTERNAL, EXTERNAL }

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var showLatencyDialog by remember { mutableStateOf(false) }
    var globalGain by remember { mutableStateOf(OboeDsp.getGlobalGain()) }
    var noiseReduction by remember { mutableStateOf(OboeDsp.isNoiseSuppressionEnabled()) }
    var micSource by remember { mutableStateOf(MicSourceMode.INTERNAL) }
    var micLevel by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        OboeDsp.start()
        while (true) {
            micLevel = OboeDsp.getMicInputLevel()
            delay(60)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            // Nie zatrzymujemy silnika: DAF/FAF może działać po przejściu do innego ekranu.
        }
    }

    LaunchedEffect(globalGain) {
        OboeDsp.setGlobalGain(globalGain)
    }

    LaunchedEffect(noiseReduction) {
        OboeDsp.setNoiseSuppressionEnabled(noiseReduction)
    }

    LaunchedEffect(micSource) {
        when (micSource) {
            MicSourceMode.INTERNAL -> OboeDsp.setPreferredInputDeviceId(-1)
            MicSourceMode.EXTERNAL -> {
                val externalId = findPreferredExternalMicId(context)
                OboeDsp.setPreferredInputDeviceId(externalId)
            }
        }
    }

    if (showLatencyDialog) {
        AlertDialog(
            onDismissRequest = { showLatencyDialog = false },
            confirmButton = {
                TextButton(onClick = { showLatencyDialog = false }) {
                    Text("Zamknij")
                }
            },
            title = { Text("Kalibracja latencji") },
            text = {
                Text(
                    "Powiedz kilka krótkich słów do mikrofonu i obserwuj opóźnienie w słuchawkach. " +
                        "Zapisz komfortowe ustawienia opóźnienia w zakładce DAF."
                )
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Ustawienia DSP", style = MaterialTheme.typography.titleLarge)

        Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Globalny gain")
                Slider(
                    value = globalGain,
                    onValueChange = { globalGain = it },
                    valueRange = 0.5f..2.0f
                )
                Text("Aktualnie: ${String.format(\"%.2f\", globalGain)}×")
            }
        }

        Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Prosta redukcja szumu")
                    Switch(checked = noiseReduction, onCheckedChange = { noiseReduction = it })
                }
                Text("Tryb działa jak lekki noise gate: ciche tło jest tłumione.")
            }
        }

        Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Mikrofon")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { micSource = MicSourceMode.INTERNAL },
                        modifier = Modifier.weight(1f)
                    ) { Text("Wewnętrzny") }
                    Button(
                        onClick = { micSource = MicSourceMode.EXTERNAL },
                        modifier = Modifier.weight(1f)
                    ) { Text("Zewnętrzny") }
                }
                Text(
                    when (micSource) {
                        MicSourceMode.INTERNAL -> "Aktywny: mikrofon wewnętrzny telefonu"
                        MicSourceMode.EXTERNAL -> "Aktywny: preferowany mikrofon zewnętrzny (USB / headset)"
                    }
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Mic level bar")
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MicLevelBar(level = micLevel, modifier = Modifier.height(72.dp).width(14.dp))
                    Text("Poziom wejścia: ${(micLevel * 100).roundToInt()}%")
                }
            }
        }

        Button(onClick = { showLatencyDialog = true }) {
            Text("Kalibruj latencję")
        }
    }
}

private fun findPreferredExternalMicId(context: Context): Int {
    val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return -1
    val inputs = am.getDevices(AudioManager.GET_DEVICES_INPUTS)
    val preferred = inputs.firstOrNull {
        it.type == AudioDeviceInfo.TYPE_USB_DEVICE ||
            it.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
            it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET
    }
    return preferred?.id ?: -1
}

@Composable
private fun MicLevelBar(level: Float, modifier: Modifier = Modifier) {
    val norm = level.coerceIn(0f, 1f)
    val segments = 8

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
                norm < 0.4f -> MaterialTheme.colorScheme.primary
                norm < 0.75f -> MaterialTheme.colorScheme.secondary
                else -> MaterialTheme.colorScheme.error
            }
            Box(
                modifier = Modifier
                    .width(14.dp)
                    .height(7.dp)
                    .background(color = color, shape = RoundedCornerShape(3.dp))
            )
        }
    }
}
