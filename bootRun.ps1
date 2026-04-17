# .env loading script for bootRun
# Usage: .\bootRun.ps1 [-SkipInfra] [-InfraOnly]
param(
    [switch]$SkipInfra,
    [switch]$InfraOnly
)

$ErrorActionPreference = 'Stop'

$rootPath = $PSScriptRoot
$envFile = Join-Path $rootPath ".env"

if (-not $SkipInfra) {
    $docker = Get-Command docker -ErrorAction SilentlyContinue
    if (-not $docker) {
        Write-Error "docker 명령을 찾을 수 없습니다. Docker Desktop 실행 여부를 확인하거나 -SkipInfra 옵션을 사용하세요."
        exit 1
    }

    Write-Host "Starting infrastructure with docker compose (postgres, redis)..."
    & docker compose -f (Join-Path $rootPath "compose.yaml") up -d postgres redis
    if ($LASTEXITCODE -ne 0) {
        Write-Error "docker compose up -d postgres redis 실행에 실패했습니다."
        exit $LASTEXITCODE
    }
    Write-Host ""
}

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
Write-Host "Effective AI settings:"
Write-Host "  POS_AI_PROVIDER=$env:POS_AI_PROVIDER"
Write-Host "  GEMINI_FLASH_MODEL=$env:GEMINI_FLASH_MODEL"
Write-Host "  GEMINI_PRO_MODEL=$env:GEMINI_PRO_MODEL"
Write-Host ""

if ($InfraOnly) {
    Write-Host "Infrastructure is ready. App start skipped (-InfraOnly)."
    exit 0
}

Write-Host "Starting bootRun..."
& (Join-Path $rootPath "gradlew.bat") bootRun
