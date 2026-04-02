param(
    [Parameter(Position = 0)]
    [string]$Sql,

    [string]$File,

    [switch]$Help
)

$projectRoot = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $projectRoot ".env"

function Show-Usage {
    Write-Host "Usage:"
    Write-Host '  .\scripts\db-query.ps1 -Sql "select now();"'
    Write-Host '  .\scripts\db-query.ps1 -File ".\sql\check-notes.sql"'
    Write-Host '  .\scripts\db-query.ps1 "select id, title from notes;"'
    Write-Host ""
    Write-Host "Examples:"
    Write-Host '  .\scripts\db-query.ps1 -Sql "select count(*) as note_count from notes;"'
    Write-Host '  .\scripts\db-query.ps1 "select note_id, tag from note_tags order by note_id, tag;"'
    Write-Host '  .\scripts\db-query.ps1 -File ".\sql\maintenance\vacuum-check.sql"'
}

if ($Help) {
    Show-Usage
    exit 0
}

if (-not [string]::IsNullOrWhiteSpace($Sql) -and -not [string]::IsNullOrWhiteSpace($File)) {
    Write-Error "-Sql 과 -File 은 동시에 사용할 수 없습니다."
    Show-Usage
    exit 1
}

if ([string]::IsNullOrWhiteSpace($Sql) -and [string]::IsNullOrWhiteSpace($File)) {
    Show-Usage
    exit 0
}

# Load .env values into process env so DB name/user can be reused.
if (Test-Path $envFile) {
    Get-Content $envFile | Where-Object {
        $_ -notmatch '^\s*#' -and $_ -match '='
    } | ForEach-Object {
        $parts = $_ -split '=', 2
        $key = $parts[0].Trim()
        $value = $parts[1].Trim()
        [System.Environment]::SetEnvironmentVariable($key, $value, 'Process')
    }
}

$dbName = $env:POS_DB_NAME
if ([string]::IsNullOrWhiteSpace($dbName)) {
    $dbName = "personal_operating_system"
}

$dbUser = $env:POS_DB_USERNAME
if ([string]::IsNullOrWhiteSpace($dbUser)) {
    $dbUser = "pos"
}

Set-Location $projectRoot

# Requires postgres service container from compose.yaml to be running.
if (-not [string]::IsNullOrWhiteSpace($File)) {
    $resolvedFile = Resolve-Path -Path $File -ErrorAction SilentlyContinue
    if ($null -eq $resolvedFile) {
        Write-Error "SQL 파일을 찾을 수 없습니다: $File"
        exit 1
    }

    # 컨테이너 내부에서는 호스트 파일 경로를 직접 읽을 수 없으므로,
    # 파일 내용을 읽어 SQL 문자열로 전달합니다.
    $sqlFromFile = Get-Content -Path $resolvedFile.Path -Raw
    docker compose exec postgres psql -U $dbUser -d $dbName -c $sqlFromFile
    exit $LASTEXITCODE
}

docker compose exec postgres psql -U $dbUser -d $dbName -c $Sql




