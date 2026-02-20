# FluencyCoach — LLM UI Couch — PL + Oboe (v2) — szkielet z lepszym layoutem

Ten projekt to świeży szablon aplikacji Android (Kotlin + Jetpack Compose + Material3)
pod trening mowy z DAF. Audio/DSP jest na razie stubem w C++ (bez realnego przetwarzania),
ale interfejs JNI, pliki CMake i UI są gotowe.

## Wymagania

- Android Studio z JDK 17 (JBR)
- Android Gradle Plugin ~8.2.x
- NDK zbliżone do 25.1.8937393
- minSdk 26, target/compileSdk 34

## Import projektu

1. Rozpakuj ZIP np. do:
   `C:\Users\Janek\Desktop\FluencyCoach_work\FluencyCoach`
2. W Android Studio: *File → Open...* i wskaż katalog `FluencyCoach`.
3. Poczekaj na pełną synchronizację Gradle.

## Budowanie z PowerShell

W katalogu projektu:

```powershell
cd .\FluencyCoach

.\gradlew.bat clean :app:assembleDebug
```

(Jeśli wrappera brak, uruchom build z poziomu Android Studio.)

APK znajdziesz w:
`app\build\outputs\apk\debug\app-debug.apk`

## Instalacja i uruchomienie (adb)

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb shell pm grant com.example.llmui android.permission.RECORD_AUDIO
adb shell am start -n com.example.llmui/com.example.llmui.MainActivity
```

Logcat (tagi audio):

```powershell
adb logcat -s OboeAudio AudioTrack AudioRecord AAudio OboeDsp-JNI
```

## Co jest zrobione

- Dolny pasek nawigacji (Start / DAF / Ćwiczenia / Ustawienia / Wyniki).
- Ekran **Start**:
  - Karta DAF z CTA.
  - Karty: Ćwiczenia / Wyniki / Ustawienia i pomoc.
- Ekran **DAF**:
  - Slider opóźnienia 0–300 ms.
  - Slider gain 0.0–2.0.
  - Przełączniki Feedback / Test tone.
  - Sekcja „Ćwiczenia DAF” (PL + emoji).
- Ekran **Ćwiczenia**: kategorie, opisy, czas, poziom, CTA „Rozpocznij”.
- Ekran **Ustawienia**: sample rate 48000, stereo z fallback, info o BT.
- Ekran **Wyniki**: prosty dashboard z przykładowymi danymi.
- Room: zdefiniowane encje (Exercise, ExerciseSession, Progress) + DAO + AppDatabase.
- C++: `daf_engine` + `jni_bridge` + `CMakeLists` (stub, tylko loguje).

## TODO (następne kroki)

- Podpięcie prawdziwego silnika Oboe/AAudio w `daf_engine.cpp`.
- Zasilanie Room realnymi danymi sesji (DAF + ćwiczenia).
- Prosty moduł DSP: RMS, pauzy, heurystyczne „zająknięcia”.
- Wyświetlenie prawdziwych statystyk w zakładce „Wyniki”.
- Opcjonalnie: DataStore dla globalnych ustawień DAF.
