package com.example.llmui.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    onNavigateToDaf: () -> Unit,
    onNavigateToExercises: () -> Unit,
    onNavigateToResults: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "FluencyCoach",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Trening mowy z DAF, ćwiczeniami i monitorowaniem postępów.",
            style = MaterialTheme.typography.bodyMedium
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Tryb DAF",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Załóż słuchawki przewodowe 🎧 i przejdź do ekranu DAF, aby dobrać opóźnienie i głośność feedbacku.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onNavigateToDaf,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Wejdź do DAF")
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HomeActionCard(
                title = "Ćwiczenia",
                subtitle = "Oddychanie, tempo mowy, czytanie i ekspresja.",
                buttonLabel = "Otwórz",
                modifier = Modifier.weight(1f),
                onClick = onNavigateToExercises
            )
            HomeActionCard(
                title = "Wyniki",
                subtitle = "Podgląd wyników i statystyk (w przygotowaniu).",
                buttonLabel = "Zobacz",
                modifier = Modifier.weight(1f),
                onClick = onNavigateToResults
            )
        }

        HomeActionCard(
            title = "Ustawienia i pomoc",
            subtitle = "Skonfiguruj audio (DSP) i przeczytaj krótkie FAQ.",
            buttonLabel = "Przejdź",
            onClick = { /* na razie tylko przez dolny pasek nawigacji */ }
        )

        Spacer(Modifier.height(8.dp))
        Text(
            text = "Tip: zaczynaj od krótkich sesji (3–5 minut) i rób przerwy. Bluetooth nie jest wspierany ze względu na dużą latencję.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun HomeActionCard(
    title: String,
    subtitle: String,
    buttonLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(4.dp))
            TextButton(
                onClick = onClick,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(buttonLabel)
            }
        }
    }
}
