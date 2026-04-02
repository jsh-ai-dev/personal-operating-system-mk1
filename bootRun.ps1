# .env loading script for bootRun
# Usage: .\bootRun.ps1
$envFile = Join-Path $PSScriptRoot ".env"
if (-not (Test-Path $envFile)) {
    Write-Error ".env file not found. Please copy .env.example first:"
    Write-Host "  Copy-Item .env.example .env"
    exit 1
}
Get-Content $envFile | Where-Object {
    $_ -notmatch '^\s*#' -and $_ -match '='
} | ForEach-Object {
    $parts = $_ -split '=', 2
    $key   = $parts[0].Trim()
    $value = $parts[1].Trim()
    [System.Environment]::SetEnvironmentVariable($key, $value, 'Process')
    Write-Host "  [env] $key loaded"
}
Write-Host ""
Write-Host "Starting bootRun..."
.\gradlew.bat bootRun