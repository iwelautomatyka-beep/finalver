package com.example.llmui.ui.results

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.llmui.data.db.AppDatabase
import com.example.llmui.data.db.ExerciseEntity
import com.example.llmui.data.db.ExerciseSessionEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ResultsScreen() {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val exerciseDao = remember { db.exerciseDao() }
    val sessionDao = remember { db.exerciseSessionDao() }

    val sessions by sessionDao.getRecent(limit = 30).collectAsState(initial = emptyList())
    val exercises by exerciseDao.getAll().collectAsState(initial = emptyList())
    val exerciseMap = remember(exercises) { exercises.associateBy { it.id } }

    val totalSeconds = sessions.sumOf { it.durationSeconds }
    val totalMinutes = totalSeconds / 60
    val sessionCount = sessions.size
    val avgDelay = if (sessionCount > 0) sessions.sumOf { it.avgDelayMs } / sessionCount else 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Wyniki",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Podsumowanie",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text("Sesje łącznie: $sessionCount")
                Text("Czas łączny: ${totalMinutes} min")
                Text("Średnie opóźnienie DAF: ${avgDelay} ms")
                Text(
                    text = "Sesje z ćwiczeń i z zakładki DAF pojawiają się tutaj jako lista, aby łatwo śledzić postęp.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(sessions, key = { it.id }) { s ->
                val ex: ExerciseEntity? = s.exerciseId?.let { exerciseMap[it] }
                val title = ex?.title ?: (s.label ?: "Sesja DAF")

                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formatDateTime(s.startTimeMillis),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "≈ ${s.durationSeconds / 60} min",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (s.avgDelayMs > 0 || s.avgGain > 0f) {
                            Text(
                                text = "DAF: delay ${s.avgDelayMs} ms, gain ${"%.1f".format(s.avgGain)}x",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

private fun formatDateTime(millis: Long): String {
    val df = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    return df.format(Date(millis))
}
