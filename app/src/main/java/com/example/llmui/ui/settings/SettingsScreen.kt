package com.example.llmui.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    var showLatencyDialog by remember { mutableStateOf(false) }

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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Ustawienia DSP", style = MaterialTheme.typography.titleLarge)

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Sample rate: 48000 Hz")
                Text("Kanały: preferowane stereo, fallback do mono jeśli wymagane.")
                Text("Frames per burst: auto (zależne od urządzenia / AAudio).")
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Urządzenie audio", style = MaterialTheme.typography.titleMedium)
                Text("Preferowane: słuchawki przewodowe (jack / USB-C).")
                Text("Uwaga: Bluetooth nie jest wspierany (zbyt duża latencja).")
            }
        }

        Button(onClick = { showLatencyDialog = true }) {
            Text("Kalibruj latencję")
        }
    }
}
