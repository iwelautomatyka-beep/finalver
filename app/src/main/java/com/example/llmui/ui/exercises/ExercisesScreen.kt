package com.example.llmui.ui.exercises

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.llmui.data.db.AppDatabase
import com.example.llmui.data.db.ExerciseEntity
import com.example.llmui.data.db.ExerciseSessionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ExercisesScreen() {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val exerciseDao = remember { db.exerciseDao() }
    val sessionDao = remember { db.exerciseSessionDao() }
    val scope = rememberCoroutineScope()

    // Inicjalne wypełnienie bazy ćwiczeniami
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            if (exerciseDao.count() == 0) {
                exerciseDao.insertAll(defaultExercises)
            }
        }
    }

    val exercises by exerciseDao.getAll().collectAsState(initial = emptyList())
    val usageList by sessionDao.getExerciseUsage().collectAsState(initial = emptyList())
    val usageMap = remember(usageList) { usageList.associateBy { it.exerciseId } }
    val grouped = remember(exercises) { exercises.groupBy { it.category } }

    // Id aktualnie aktywnego ćwiczenia (ustawiane przy "Rozpocznij")
    var activeExerciseId by remember { mutableStateOf<Long?>(null) }

    // Ćwiczenie wybrane do podglądu w dużym oknie
    var selectedExercise by remember { mutableStateOf<ExerciseEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Ćwiczenia",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Kliknij „Szczegóły” albo pasek „Aktywne ćwiczenie”, żeby otworzyć obszerny opis z gotowymi tekstami i tipami dla prowadzącego.",
            style = MaterialTheme.typography.bodyMedium
        )

        // Pasek z informacją o aktywnym ćwiczeniu – klikalny => otwiera Szczegóły
        val activeExercise = activeExerciseId?.let { id ->
            exercises.firstOrNull { it.id == id }
        }

        activeExercise?.let { ex ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedExercise = ex },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Aktywne ćwiczenie",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = ex.title,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Dotknij, aby zobaczyć szczegóły tego ćwiczenia. Po zakończeniu zajrzyj do zakładki Wyniki.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            grouped.forEach { (category, list) ->
                item {
                    Text(
                        text = category,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                items(list, key = { it.id }) { ex ->
                    val stats = usageMap[ex.id]

                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(ex.title, style = MaterialTheme.typography.titleSmall)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Poziom: ${ex.level}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "Ok. ${ex.durationMinutes} min",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            val count = stats?.count ?: 0
                            val totalMin = (stats?.totalDurationSeconds ?: 0) / 60
                            Text(
                                text = "Wykonano: ${count}×, ~${totalMin} min łącznie",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )

                            // Skrót – żeby lista była lekka
                            Text(
                                text = shortSummary(ex.description),
                                style = MaterialTheme.typography.bodySmall
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = {
                                        // Ustawiamy aktywne ćwiczenie + zapis sesji do bazy
                                        activeExerciseId = ex.id
                                        scope.launch(Dispatchers.IO) {
                                            val now = System.currentTimeMillis()
                                            val durationSec = ex.durationMinutes * 60
                                            sessionDao.insert(
                                                ExerciseSessionEntity(
                                                    exerciseId = ex.id,
                                                    label = ex.title,
                                                    startTimeMillis = now,
                                                    durationSeconds = durationSec,
                                                    avgDelayMs = 0,
                                                    avgGain = 0f
                                                )
                                            )
                                        }
                                    }
                                ) {
                                    Text("Rozpocznij")
                                }

                                TextButton(
                                    onClick = {
                                        selectedExercise = ex
                                    }
                                ) {
                                    Text("Szczegóły")
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Duże okno z pełnym opisem i tipami
    selectedExercise?.let { ex ->
        ExerciseDetailDialog(
            exercise = ex,
            onDismiss = { selectedExercise = null }
        )
    }
}

/**
 * Prosty skrót opisu – bierzemy pierwsze 1–2 linie, żeby lista była czytelna.
 */
private fun shortSummary(description: String): String {
    val lines = description.lines().filter { it.isNotBlank() }
    return when {
        lines.isEmpty() -> ""
        lines.size == 1 -> lines.first()
        else -> lines.take(2).joinToString(" ")
    }
}

@Composable
private fun ExerciseDetailDialog(
    exercise: ExerciseEntity,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = exercise.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = exercise.category,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = exercise.description,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Zamknij")
            }
        }
    )
}

// Bardzo rozbudowane opisy z:
// - celem ćwiczenia,
// - kiedy używać,
// - krok po kroku (skrypt dla prowadzącego),
// - wieloma przykładami tekstów / mini-piosenek,
// - tipami i modyfikacjami.
private val defaultExercises: List<ExerciseEntity> = listOf(
    // ODDYCHANIE
    ExerciseEntity(
        title = "Oddychanie przeponowe 🌬️",
        category = "Oddychanie",
        level = 1,
        durationMinutes = 3,
        description = """
Cel ćwiczenia:
• Uspokojenie oddechu, obniżenie napięcia w ciele, przygotowanie do mówienia i ćwiczeń DAF.
• Uczenie dziecka, że oddech może „pomagać” w mówieniu, a nie przeszkadzać.

Kiedy używać:
• Na początku sesji, żeby „wejść” w spokojny tryb.
• W środku, gdy dziecko się nakręca / frustruje.
• Na koniec – jako wyciszenie.

Przebieg – skrypt dla prowadzącego:
1. „Usiądź wygodnie. Twoje stopy dotykają podłogi, plecy opierają się o oparcie.”
2. „Połóż jedną dłoń na brzuchu, a drugą na klatce piersiowej.”
3. „Spróbuj tak wciągać powietrze nosem, żeby bardziej unosił się brzuch niż klatka piersiowa.”
4. „Teraz wypuść powietrze ustami, jakbyś lekko zdmuchiwał świeczkę – spokojnie, bez wysiłku.”
5. „Powtórzymy to kilka razy, a potem dodamy do tego proste zdania.”

Proste zdania do czytania na wydechu:
• „Oddycham spokojnie i powoli.”
• „Mój brzuch unosi się jak balon.”
• „Z każdym wydechem rozluźniam ramiona.”
• „Mam czas, niczego nie muszę przyspieszać.”
• „Mój głos może być spokojny, tak jak mój oddech.”

Mini-opowiadania oddechowe – prowadzący może czytać, dziecko słucha i czasem dopowiada:
• „Wyobraź sobie, że w brzuchu masz małą latarenkę. Kiedy robisz wdech, latarenka świeci jaśniej. Kiedy robisz wydech, świeci trochę słabiej i całe ciało się rozluźnia.”
• „W brzuchu mieszka balon. Przy każdym wdechu balon rośnie, a przy wydechu powoli się opróżnia. Balon nigdy nie pęka, zawsze jest miękki i bezpieczny.”
• „Jesteś spokojnym żółwiem nad wodą. Wciągasz powietrze nosem, zatrzymujesz na krótką chwilę, a potem cicho wypuszczasz ustami. Żółw nigdzie się nie spieszy.”

Mini „piosenka oddechowa” (można nucić na dowolnej prostej melodii):
• „Wdeeech – brzuch jak ba-lon,
   wy-deeech – spokój ma dom.
   Wdeeech – świat jest tu,
   wy-deeech – ja mam czas, nie muszę już biec.”

Tipy dla osoby prowadzącej:
• Zwracaj uwagę na barki – jeśli idą w górę, wróć do prostego komunikatu: „Spróbuj, żeby unosił się brzuch, a barki zostawały spokojne.”
• Dziecko, które się wstydzi, może na początku ćwiczyć z rękami na brzuchu „pod stołem” lub pod kocem.
• Lepiej krócej i częściej, niż raz bardzo długo – 2–3 minuty, ale kilka razy w trakcie sesji.
• Dobrze działa odliczanie: „Wdech na 4, zatrzymanie na 1, wydech na 4.”
""".trimIndent()
    ),
    ExerciseEntity(
        title = "Oddech + sylaby 🫁",
        category = "Oddychanie",
        level = 2,
        durationMinutes = 4,
        description = """
Cel ćwiczenia:
• Łączenie spokojnego oddechu z prostą artykulacją.
• Oswajanie powtarzania sylab i prostych zdań bez pośpiechu.

Kiedy używać:
• Po krótkim bloku „czystego” oddychania przeponowego.
• Jako przygotowanie do ćwiczeń z DAF, gdy pracujecie nad rytmem i tempem.

Przebieg – skrypt dla prowadzącego:
1. „Najpierw zrobimy kilka spokojnych oddechów bez mówienia.”
2. „Teraz spróbujemy na jednym wydechu powiedzieć kilka sylab w równym rytmie.”
3. „Pokażę Ci przykład, a potem powtórzysz po mnie.”

Proponowane sekwencje sylab (mówione, a potem pół-śpiewane):
• „ma-ma-ma-ma – pauza – ma-ma-ma-ma”
• „pa-pa-pa-pa – pauza – pa-pa-pa-pa”
• „pa-ta-ka, pa-ta-ka, pa-ta-ka”
• „ba-da-ga, ba-da-ga, ba-da-ga”
• „la-ma-na, la-ma-na, la-ma-na”

Gotowe zdania do wykorzystania (można czytać z ekranu):
• „Mama ma małego misia o imieniu Mak.”
• „Tata tapetuje pokój bardzo powoli.”
• „Paweł powoli powtarza słowa po trenerze.”
• „Mały miś maszeruje małym krokiem po miękkim dywanie.”
• „Mówię tak spokojnie, jak idzie żółw po piasku.”

Wersje pół-śpiewane (bardzo proste motywy):
• „ma-ma-ma, po-wo-li tak – pa-pa-pa, spokojny takt”
• „pa-ta-ka, pa-ta-ka – mówimy w ryt-mie wol-ne-go kro-ka”

Tekst, który może czytać prowadzący jako „instrukcję na głos”:
• „Teraz wybierzemy dwie lub trzy ulubione sylaby. Na każdym wydechu powiemy je powoli, w równym rytmie, bez gonienia końców. Możesz sobie wyobrazić, że każde słowo jedzie małym wózkiem po torach – jeden wagonek, drugi, trzeci.”

Tipy dla osoby prowadzącej:
• Gdy tempo przyspiesza na końcu wydechu – skróćcie serię (np. trzy sylaby zamiast ośmiu).
• Jeśli dziecko zaczyna się bawić tempem (robi „wyścigi”) – wróć do komunikatu: „Spróbujmy mówić tak, jakby czas zwalniał.”
• Możesz jedną serię mówić Ty, a kolejną – dziecko, żeby miało przykład spokojnego tempa.
""".trimIndent()
    ),

    // TEMPO MOWY
    ExerciseEntity(
        title = "Mówienie jak „slow motion” 🐢",
        category = "Tempo mowy",
        level = 1,
        durationMinutes = 4,
        description = """
Cel ćwiczenia:
• Doświadczenie, jak brzmi głos, gdy naprawdę zwalniamy.
• Ćwiczenie miękkich początków zdań i wyraźnych końcówek.

Kiedy używać:
• Gdy dziecko mówi bardzo szybko, „wylewa” słowa.
• Na początku cyklu pracy nad tempem – żeby pokazać skrajnie wolną wersję.

Przebieg – skrypt dla prowadzącego:
1. „Zrobimy zabawę: mówimy tak, jakbyśmy byli w zwolnionym filmie.”
2. „Ja powiem jedno zdanie bardzo powoli, a Ty spróbujesz podobnie.”
3. „Najważniejsze jest spokojne rozpoczęcie i przerwa na końcu.”

Przykładowe zdania (do czytania z ekranu):
• „Dziś mam spokojny, dobry dzień.”
• „Mówię powoli i wyraźnie.”
• „Każde słowo ma swoje miejsce.”
• „Robię przerwy, gdy kończę myśl.”
• „Nie muszę się spieszyć z odpowiedzią.”

Bloki tematyczne (prowadzący może zadawać pytania):
• O poranku:
  – „Dziś rano obudziłem się i…”
  – „Moje śniadanie było…”
• O szkole:
  – „W szkole najbardziej podobało mi się, że…”
  – „Lubię, kiedy na lekcji…”
• O zabawie:
  – „Po południu bawiłem się w…”
  – „Najbardziej lubię bawić się w…”

Proste wersje pół-śpiewane:
• „Mó-wię po-wo-li, jak żół-wik na spa-cer-ze,
   sło-wa płyną ła-god-niut-ko, nic się tu nie śpie-szy.”
• „Mo-je sło-wa idą wol-no,
   mają miej-sce, mają czas.”

Tipy dla osoby prowadzącej:
• Nie wymagaj „poprawnego” śpiewu – to ma być zabawa, nie lekcja muzyki.
• Jeśli dziecko śmieje się z efektu „slow motion” – świetnie, śmiech rozładowuje napięcie.
• Pilnuj, aby pierwsze 2–3 wyrazy zdania były szczególnie wolne – tam zwykle pojawia się największe napięcie.
• Możesz nagrać krótką próbkę na wideo (za zgodą rodzica) i pokazać dziecku, jak wygląda jego „slow motion” – często to bardzo wzmacnia świadomość.
""".trimIndent()
    ),
    ExerciseEntity(
        title = "Liczenie w rytmie ⏱️",
        category = "Tempo mowy",
        level = 2,
        durationMinutes = 4,
        description = """
Cel ćwiczenia:
• Ćwiczenie równomiernego rytmu mowy.
• Odruch „pauza jest ok” – po każdej liczbie chwilka przerwy.

Kiedy używać:
• Gdy dziecko ma tendencję do „wypluwania” całych serii słów bez pauz.
• Jako rozgrzewka przed trudniejszymi tekstami.

Przebieg – skrypt dla prowadzącego:
1. „Ustalimy rytm – możesz lekko stukać palcem o stół albo klaskać cicho.”
2. „Przy każdym stuknięciu powiemy jedną liczbę i zrobimy krótką przerwę.”
3. „Najpierw zrobimy to razem, potem spróbujesz samodzielnie.”

Przykładowe sekwencje liczenia:
• „jeden… dwa… trzy… cztery…”
• „pięć… sześć… siedem… osiem… dziewięć… dziesięć…”
• Wstecz: „dziesięć… dziewięć… osiem… siedem… sześć… pięć…”

Liczenie z tekstem:
• „Jeden krok w przód, dwa kroki w tył,
   trzy i cztery – zwalniamy styl.”
• „Raz i dwa – spokojnie tak,
   trzy i cztery – oddycham w takt.”

Mini „piosenki liczące”:
• „Raz i dwa, raz i dwa,
   liczę wolno – tak się da.”
• „Trzy i cztery, pięć i sześć,
   każde słowo może brzmieć.”

Pomysły na prowadzenie dialogowe:
• Ty: „Powiem liczbę, a Ty powiesz spokojne zdanie – na przykład co dziś robiłeś.”
  – „Jeden…” – dziecko: „Dziś rano wstałem późno.”
  – „Dwa…” – dziecko: „Potem zjadłem śniadanie.”
• Można zamieniać się rolami: dziecko mówi liczbę, dorosły – zdanie.

Tipy dla osoby prowadzącej:
• Jeśli dziecko „ucieka do przodu”, zatrzymajcie się, wróćcie do liczenia tylko do pięciu.
• Naprzemienność (raz Ty, raz dziecko) odciąża i pokazuje wzór spokojnego rytmu.
• Można liczyć przy rysowaniu kroków na kartce – każda liczba = jeden mały, spokojny krok.
""".trimIndent()
    ),

    // CZYTANIE SYLAB / RYMÓW
    ExerciseEntity(
        title = "Czytanie sylab 🗣️",
        category = "Czytanie sylab / rymy",
        level = 1,
        durationMinutes = 5,
        description = """
Cel ćwiczenia:
• Ćwiczenie płynności przy prostym materiale językowym (sylaby, krótkie słowa).
• Oderwanie się od „treści” – skupienie na rytmie i miękkim starcie.

Kiedy używać:
• Na początku pracy nad płynnością, kiedy pełne zdania są jeszcze zbyt trudne.
• Jako „reset” po trudniejszym zadaniu – powrót do prostych sylab.

Przebieg – skrypt dla prowadzącego:
1. „Wybierzemy kilka rzędów sylab. Najpierw przeczytam ja, potem Ty.”
2. „Przed każdą serią zrobimy spokojny wdech, potem równy wydech z sylabami.”
3. „Nie liczy się szybkość, tylko spokojny rytm.”

Proponowane rzędy sylab:
• „pa-ta-ka, pa-ta-ka, pa-ta-ka”
• „ba-da-ga, ba-da-ga, ba-da-ga”
• „la-ma-na, la-ma-na, la-ma-na”
• „sa-za-sza, sa-za-sza”
• „ma-na-la, ma-na-la”

Zdania treningowe (łatwe do odczytania z ekranu):
• „Paweł pije powoli poranną herbatę.”
• „Tata tapetuje pokój bardzo spokojnie.”
• „Mały miś maszeruje małym krokiem.”
• „Szymon szuka szarego szalika na szafce.”

Pół-śpiewane sekwencje:
• „Pa-ta-ka, pa-ta-ka, idzie mowa w świat,
   pa-ta-ka, pa-ta-ka, każdy słyszy ją tak.”
• „La-ma-na, la-ma-na, la-ma-na – wolny krok,
   nasze słowa płyną miękko, nie goni ich tłok.”

Pomysł na grę:
• „Sylabowe domino” – dziecko wybiera trzy sylaby, Ty z nich układasz śmieszne słowo i zdanie:
  – sylaby: „pa, ta, ka” → słowo „pataka” → zdanie: „Pataka to tajemniczy ptak, który mówi powoli.”

Tipy dla osoby prowadzącej:
• Jeśli dziecko zaczyna bardziej „wykrzyczeć” sylaby – przypomnij, że ma być spokojnie i miękko, a nie głośno.
• Możecie nagrywać krótkie sekwencje i wspólnie słuchać – pytając: „Czy tempo było spokojne?”.
• Pozwalaj dziecku wymyślać własne rzędy – zwiększa to poczucie kontroli i zabawy.
""".trimIndent()
    ),
    ExerciseEntity(
        title = "Rymowanki z pauzami 🎵",
        category = "Czytanie sylab / rymy",
        level = 2,
        durationMinutes = 5,
        description = """
Cel ćwiczenia:
• Uczenie wyraźnych pauz między wersami.
• Trening rytmicznej, spokojnej mowy na znanym tekście.

Kiedy używać:
• Gdy dziecko lubi wierszyki / piosenki – wykorzystujemy coś, co jest przyjemne.
• Przed wejściem w dłuższe czytanki.

Przebieg – skrypt dla prowadzącego:
1. „Zaraz przeczytamy rymowankę. Po każdej linijce zrobimy pauzę i oddech.”
2. „Najpierw przeczytam ją ja, a Ty posłuchasz rytmu.”
3. „Potem będziemy czytać na zmianę – Ty jeden wers, ja drugi.”

Przykładowe rymowanki:

1)
• „Leci listek w dół powoli,
   dzieci patrzą, nikt się nie śpieszy.”
• „Listek krąży ponad ziemią,
   w końcu miękko w trawie leży.”

2)
• „Mały kotek idzie w dal,
   każdy kroczek stawia w dal.”
• „Nie chce biec, nie chce gnać,
   lubi wolno w świecie trwać.”

3)
• „Płynie rzeczka po kamieniach,
   cicho szumi w naszych snach.”
• „Nigdzie się nie musi spieszyć,
   ma swój rytm i własny czas.”

Wersja śpiewana (można nucić bez presji):
• „Le-ci lis-tek po-wo-li,
   ni-kto tu się nie śpie-szy.
   Dy-cham spo-ko-ooj-nie,
   mo-je sło-wa te-ż są cie-sze.”

Pomysły na własne rymowanki:
• O ulubionym zwierzaku: „Mały piesek lubi spać, kiedy ktoś ma wolny czas…”
• O przepływie dnia: „Rano słońce wolno wstaje, potem cicho znika w taje…”

Tipy dla osoby prowadzącej:
• Jeśli dziecko zna rymowankę na pamięć, będzie pewniejsze – to dobry start.
• Moduł „pauzy” możesz zaznaczać gestem dłoni – ręka w górę = mówimy, ręka w dół = pauza.
• Zachęcaj, aby patrzeć w jedno miejsce (np. punkt na ścianie), zamiast nerwowo rozglądać się.
""".trimIndent()
    ),

    // EKSPRESJA
    ExerciseEntity(
        title = "Mini-opowieści 🎧",
        category = "Ekspresja",
        level = 1,
        durationMinutes = 5,
        description = """
Cel ćwiczenia:
• Bezpieczne „pole do gadania” – dziecko może mówić o swoim dniu, marzeniach, ulubionych rzeczach.
• Ćwiczenie wolniejszych początków zdań, pauz między zdaniami i spokojnego tonu.

Kiedy używać:
• Gdy chcesz, żeby dziecko po prostu „pogadało”, ale nie w trybie przepytywania.
• Po technicznych ćwiczeniach (sylaby, liczenie) – jako bardziej naturalna mowa.

Przebieg – skrypt dla prowadzącego:
1. „Zrobimy krótką historię o Tobie. Nie musisz mówić wszystkiego, tylko tyle, ile chcesz.”
2. „Ja zadam pytanie, a Ty odpowiesz jednym spokojnym zdaniem, tak jak w slow motion.”
3. „Na końcu każdego zdania zrobimy pauzę – ja pokażę ręką, kiedy.”

Proponowane tematy:
• „Mój najlepszy dzień w szkole.”
• „Wymarzony dzień bez pośpiechu.”
• „Gdybym miał supermoc spowalniania czasu…”
• „Co lubię robić po południu, kiedy nie muszę się śpieszyć.”

Przykładowe pytania (do odczytania z ekranu):
• „Co lubisz najbardziej rano?”
• „Z kim najlepiej Ci się rozmawia?”
• „Kiedy Twoje słowa są spokojne – co wtedy robisz, jak się czujesz?”

Gotowe „startery” zdań:
• „Dziś rano obudziłem się i…”
• „Najbardziej lubię, kiedy…”
• „Wyobrażam sobie, że pewnego dnia…”
• „Kiedy mówię wolniej, czuję, że…”

Tipy dla osoby prowadzącej:
• Nie poprawiaj treści – to nie jest wypracowanie. Skup się na tempie i sposobie mówienia.
• Gdy pojawia się jąkanie, zachowaj spokój; możesz powiedzieć: „Zatrzymajmy się na chwilę, weźmy oddech i spróbujmy pierwsze słowo powiedzieć miękko.”
• Dziecko może chcieć rysować swoją historię w trakcie mówienia – to pomaga część uwagi przenieść na coś neutralnego.
""".trimIndent()
    ),
    ExerciseEntity(
        title = "Dialogi z rolą 🎭",
        category = "Ekspresja",
        level = 3,
        durationMinutes = 6,
        description = """
Cel ćwiczenia:
• Trening mówienia w sytuacjach zbliżonych do realnych (sklep, lekarz, szkoła).
• Ćwiczenie spokojnych początków wypowiedzi i prostych, jasnych komunikatów.

Kiedy używać:
• Gdy dziecko boi się odpowiadać przy tablicy, zamawiać coś w sklepie, rozmawiać z dorosłymi.
• Przy przygotowaniu do konkretnej sytuacji (np. wizyta u logopedy, lekarza, rozmowa w szkole).

Przebieg – skrypt dla prowadzącego:
1. „Wybierzemy scenkę, którą chcesz przećwiczyć: sklep, lekarz, plac zabaw, restauracja.”
2. „Najpierw ja będę osobą dorosłą, a Ty dzieckiem. Potem możemy się zamienić.”
3. „Najważniejsze jest spokojne pierwsze słowo i chwila pauzy po Twojej wypowiedzi.”

Przykładowy dialog „w sklepie”:
• Dziecko: „Dzień dobry, poproszę jedną bułkę i sok jabłkowy.”
• Sprzedawca (Ty): „Oczywiście, coś jeszcze?”
• Dziecko: „Nie, dziękuję, to wszystko.”
• Sprzedawca: „To będzie pięć złotych.”
• Dziecko: „Czy mogę zapłacić kartą?”
• Sprzedawca: „Tak, proszę bardzo.”
• Dziecko: „Dziękuję, do widzenia.”

Przykładowy dialog „u lekarza”:
• Dziecko: „Dzień dobry, boli mnie gardło od wczoraj.”
• Lekarz: „Kiedy boli najmocniej?”
• Dziecko: „Najbardziej boli, kiedy połykam.”
• Lekarz: „Co Ci pomaga?”
• Dziecko: „Ciepła herbata i gdy mało mówię.”

Przykładowy dialog „na placu zabaw”:
• Dziecko: „Czy możemy pobawić się razem na zjeżdżalni?”
• Inne dziecko (Ty): „Tak, jasne, chodźmy.”
• Dziecko: „Chciałbym pożyczyć Twoją piłkę.”
• Inne dziecko: „Tak, tylko oddaj mi ją za chwilę.”
• Dziecko: „Dziękuję, fajnie się z Tobą bawiło.”

Mini „piosenka dialogowa”:
• „Dzień dobry, mówię wolno – mam dla słów spokojne okno.
   Proszę, dzięki, przepraszam – te trzy słowa zawsze zgłaszam.”

Tipy dla osoby prowadzącej:
• Traktuj to jak zabawę w teatr – można użyć pluszaków, figurek, rysunków postaci.
• Zwracaj uwagę, czy dziecko nie zaczyna mówić zbyt szybko, gdy „wchodzi w rolę” – wtedy zatrzymaj scenkę, weźcie oddech i wznowicie od poprzedniego zdania.
• Możesz poprosić: „Najpierw powiedzmy całe zdanie szeptem bardzo powoli, a potem normalnym głosem – nadal powoli.”
• Dobrze działa krótkie podsumowanie na koniec: „Które zdanie dzisiaj powiedziało Ci się najspokojniej?”.
""".trimIndent()
    )
)
