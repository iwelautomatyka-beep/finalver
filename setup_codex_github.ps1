param(
    [Parameter(Mandatory = $true)]
    [string]$RepoUrl,
    [string]$RemoteName = "origin",
    [string]$Branch = "work"
)

$ErrorActionPreference = "Stop"

if (-not (git rev-parse --is-inside-work-tree 2>$null)) {
    throw "ERROR: This is not a git repository."
}

try {
    git remote get-url $RemoteName | Out-Null
    git remote set-url $RemoteName $RepoUrl
    Write-Host "Updated remote '$RemoteName' -> $RepoUrl"
} catch {
    git remote add $RemoteName $RepoUrl
    Write-Host "Added remote '$RemoteName' -> $RepoUrl"
}

git fetch $RemoteName --prune

$hasLocalBranch = $false
try {
    git show-ref --verify --quiet "refs/heads/$Branch"
    if ($LASTEXITCODE -eq 0) { $hasLocalBranch = $true }
} catch {}

if ($hasLocalBranch) {
    git checkout $Branch
} else {
    git ls-remote --exit-code --heads $RemoteName $Branch | Out-Null
    if ($LASTEXITCODE -eq 0) {
        git checkout -B $Branch "$RemoteName/$Branch"
    } else {
        git checkout -B $Branch
    }
}

Write-Host ""
Write-Host "Running access diagnostics..."
& (Join-Path $PSScriptRoot "check_github_access.ps1") -RemoteName $RemoteName -Branch $Branch

Write-Host ""
Write-Host "Next step: authenticate once (PAT via credential manager) and push:"
Write-Host "  git push -u $RemoteName $Branch"
