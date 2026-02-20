param(
    [string]$FluencyRoot = "C:\Users\Janek\Desktop\FluencyCoach",
    [string]$MonitorRoot = "C:\Users\Janek\Desktop\dzialajace\LowLatencyMonitor_v2_stable\LowLatencyMonitor"
)

function Log {
    param([string]$msg)
    Write-Host "[$(Get-Date -Format HH:mm:ss)] $msg"
}

Log "Starting audio/DSP file collection (wet/dry, limiter, mic level, latency)..."

$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$desktop   = [Environment]::GetFolderPath("Desktop")
$outRoot   = Join-Path $desktop "FC_Audio_Extract_$timestamp"
$fcOut     = Join-Path $outRoot "FluencyCoach"
$monOut    = Join-Path $outRoot "LowLatencyMonitor"

New-Item -ItemType Directory -Path $fcOut -Force  | Out-Null
New-Item -ItemType Directory -Path $monOut -Force | Out-Null

Log "Output root: $outRoot"

function Copy-KnownPaths {
    param(
        [string]$root,
        [string]$destRoot,
        [string]$label
    )

    if (-not (Test-Path $root)) {
        Log "Skip $label, root does not exist: $root"
        return
    }

    Log "Copying known paths from $label..."

    $paths = @(
        "app\src\main\cpp",
        "app\src\main\c++",
        "app\src\main\jni",
        "app\src\main\java\com\example\llmui\audio",
        "app\src\main\java\com\example\llmui\ui\daf",
        "app\CMakeLists.txt",
        "CMakeLists.txt",
        "app\build.gradle",
        "app\build.gradle.kts",
        "build.gradle",
        "build.gradle.kts"
    )

    foreach ($rel in $paths) {
        $src = Join-Path $root $rel
        if (Test-Path $src) {
            $dest    = Join-Path $destRoot $rel
            $destDir = Split-Path $dest

            if (-not (Test-Path $destDir)) {
                New-Item -ItemType Directory -Path $destDir -Force | Out-Null
            }

            if ((Get-Item $src).PSIsContainer) {
                Log "  Copy dir: $rel"
                Copy-Item $src -Destination $dest -Recurse -Force
            } else {
                Log "  Copy file: $rel"
                Copy-Item $src -Destination $dest -Force
            }
        }
    }
}

function Copy-MatchingFiles {
    param(
        [string]$root,
        [string]$destRoot,
        [string]$label
    )

    if (-not (Test-Path $root)) {
        Log "Skip $label for pattern search, root does not exist: $root"
        return
    }

    Log "Searching $label for DSP/audio patterns..."

    $patterns = @(
        "oboe::",
        "AAudio",
        "AudioStream",
        "Limiter",
        "limiter",
        "compressor",
        "RMS",
        "rms",
        "peak",
        "inputLevel",
        "micLevel"
    )

    $sourceDirs = @(
        "app\src\main\cpp",
        "app\src\main\c++",
        "app\src\main\jni",
        "app\src\main\java"
    )

    $seen = @{}

    foreach ($relDir in $sourceDirs) {
        $dir = Join-Path $root $relDir
        if (-not (Test-Path $dir)) { continue }

        $files = Get-ChildItem -Path $dir -Recurse -File -Include *.cpp,*.c,*.h,*.hpp,*.kt,*.java -ErrorAction SilentlyContinue

        foreach ($pat in $patterns) {
            Log "  Pattern $pat in $relDir"
            try {
                $matches = $files | Select-String -Pattern $pat -SimpleMatch -ErrorAction SilentlyContinue
            } catch {
                $matches = @()
            }

            foreach ($m in $matches) {
                $srcPath = $m.Path
                if ($seen.ContainsKey($srcPath)) { continue }
                $seen[$srcPath] = $true

                $relPath  = $srcPath.Substring($root.Length).TrimStart('\','/')
                $destPath = Join-Path $destRoot $relPath
                $destDir  = Split-Path $destPath

                if (-not (Test-Path $destDir)) {
                    New-Item -ItemType Directory -Path $destDir -Force | Out-Null
                }

                Copy-Item $srcPath $destPath -Force
                Log "    + $relPath"
            }
        }
    }
}

Copy-KnownPaths    -root $FluencyRoot -destRoot $fcOut  -label "FluencyCoach"
Copy-KnownPaths    -root $MonitorRoot -destRoot $monOut -label "LowLatencyMonitor"

Copy-MatchingFiles -root $FluencyRoot -destRoot $fcOut  -label "FluencyCoach"
Copy-MatchingFiles -root $MonitorRoot -destRoot $monOut -label "LowLatencyMonitor"

Log "Creating ZIP..."

$zipPath = "$outRoot.zip"
if (Test-Path $zipPath) {
    Remove-Item $zipPath -Force
}

Compress-Archive -Path "$outRoot\*" -DestinationPath $zipPath

Log "Done."
Log "Folder: $outRoot"
Log "ZIP:    $zipPath"
Log "Mozesz teraz wyslac ZIP albo listing plikow na czat."
