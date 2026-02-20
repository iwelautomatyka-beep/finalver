$ErrorActionPreference = "Stop"

# Katalog projektu = folder, w którym leży ten skrypt
$projectRoot = Split-Path -Parent $PSCommandPath
Set-Location $projectRoot

Write-Host "== FluencyCoach: DAF presets =="

$dafPath = "app\src\main\java\com\example\llmui\ui\daf\DafScreen.kt"

if (-not (Test-Path $dafPath)) {
    throw "Nie znalazłem pliku $dafPath – upewnij się, że skrypt leży w katalogu głównym projektu."
}

# Backup
$backupPath = "$dafPath.bak_presets_{0}" -f (Get-Date -Format "yyyyMMdd_HHmmss")
Copy-Item $dafPath $backupPath -Force
Write-Host "Backup DafScreen.kt -> $backupPath"

# Wczytaj plik
$daf = Get-Content $dafPath -Raw

# 1) Enum z presetami (jeśli jeszcze nie ma)
if ($daf -notmatch 'enum class DafPreset') {
    Write-Host "Dodaję enum DafPreset..."
    $enumBlock = @'
enum class DafPreset { GENTLE, STANDARD, STRONG, CUSTOM }

'@

    $daf = $daf -replace 'import kotlin.math.roundToInt\s*', "import kotlin.math.roundToInt`r`n`r`n$enumBlock"
}

# 2) Stan currentPreset w DafScreen (na początku funkcji)
if ($daf -notmatch 'currentPreset') {
    Write-Host "Dodaję stan currentPreset do DafScreen..."
    $daf = $daf -replace 'fun DafScreen\(\)\s*\{', "fun DafScreen() {`r`n    var currentPreset by rememberSaveable { mutableStateOf(DafPreset.GENTLE) }`r`n"
}

# 3) Wstawienie w UI wiersza z presetami pod opisem zakresu terapeutycznego
if ($daf -notmatch 'DafPresetButton') {
    Write-Host "Dodaję UI z presetami DAF..."

    $pattern = 'Text\(\s*text = "Typowy zakres terapeutyczny: 70–200 ms\.[^"]*"\s*,\s*[\r\n\s]*style = MaterialTheme\.typography\.bodySmall\s*\)'

    $insertBlock = @'
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DafPresetButton(
                        label = "Łagodny 65 ms",
                        selected = currentPreset == DafPreset.GENTLE,
                        onClick = {
                            currentPreset = DafPreset.GENTLE
                            delayMs = 65f
                            gain = 1.05f
                        }
                    )
                    DafPresetButton(
                        label = "Standard 90 ms",
                        selected = currentPreset == DafPreset.STANDARD,
                        onClick = {
                            currentPreset = DafPreset.STANDARD
                            delayMs = 90f
                            gain = 1.1f
                        }
                    )
                    DafPresetButton(
                        label = "Mocny 130 ms",
                        selected = currentPreset == DafPreset.STRONG,
                        onClick = {
                            currentPreset = DafPreset.STRONG
                            delayMs = 130f
                            gain = 1.05f
                        }
                    )
                }
'@

    $newDaf = $daf -replace $pattern, ("`$0`r`n$insertBlock")

    if ($newDaf -eq $daf) {
        Write-Warning "Nie udało się znaleźć bloku z tekstem o zakresie terapeutycznym – UI presetów NIE zostało wstrzyknięte."
    } else {
        $daf = $newDaf
    }
}

# 4) Helper-komponent DafPresetButton na końcu pliku
if ($daf -notmatch 'fun DafPresetButton') {
    Write-Host "Dodaję helper DafPresetButton..."

    $helper = @'

@Composable
private fun DafPresetButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = if (selected) {
        ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    } else {
        ButtonDefaults.buttonColors()
    }

    Button(
        onClick = onClick,
        modifier = Modifier.weight(1f),
        colors = colors
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center
        )
    }
}
'@

    $daf = $daf.TrimEnd() + "`r`n`r`n" + $helper
}

# Zapis z powrotem
Set-Content $dafPath $daf -Encoding UTF8
Write-Host "Zapisano zmodyfikowany DafScreen.kt"

# 5) Build + install + start
Write-Host "== Buduję APK =="
.\gradlew.bat :app:assembleDebug

Write-Host "== Instaluję na urządzeniu =="
adb install -r app\build\outputs\apk\debug\app-debug.apk

Write-Host "== Startuję apkę =="
adb shell am start -n com.example.llmui/.MainActivity

Write-Host "Gotowe. Sprawdź zakładkę DAF – powinny być 3 presety: Łagodny, Standard, Mocny."
