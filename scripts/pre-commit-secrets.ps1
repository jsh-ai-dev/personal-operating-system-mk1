param()

$ErrorActionPreference = "Stop"

$repoRoot = (& git rev-parse --show-toplevel 2>$null)
if (-not $repoRoot) {
    Write-Error "Cannot find git repository root."
    exit 1
}

Set-Location $repoRoot

$stagedFiles = @(& git diff --cached --name-only --diff-filter=ACMRT)

# .env 같은 로컬 시크릿 파일이 스테이징되면 즉시 차단
$blockedEnvFiles = $stagedFiles | Where-Object {
    ($_ -match '(^|/|\\)\.env(\..+)?$') -and ($_ -notmatch '(^|/|\\)\.env(\..+)?\.example$')
}

if ($blockedEnvFiles.Count -gt 0) {
    Write-Host "[pre-commit] Blocked: secret env file is staged." -ForegroundColor Red
    $blockedEnvFiles | ForEach-Object { Write-Host "  - $_" -ForegroundColor Yellow }
    Write-Host "Do not commit .env files. Unstage and try again." -ForegroundColor Yellow
    exit 1
}

$patchLines = @(& git --no-pager diff --cached -U0)
$addedLines = $patchLines | Where-Object { $_ -match '^\+' -and $_ -notmatch '^\+\+\+' }

# Detect env/config style keys only (reduces false positives in source code names like jwtSecret)
$secretPattern = '\b(?:[A-Z][A-Z0-9_]*(?:KEY|SECRET|TOKEN|PASSWORD)|POS_JWT_SECRET|GEMINI_API_KEY)\b\s*[:=]\s*[^\s]+'

$hits = @()
foreach ($line in $addedLines) {
    if ($line -match '^\+\s*(#|//)') {
        continue
    }
    if ($line -cmatch $secretPattern) {
        if ($line -match '[:=]\s*($|CHANGE_ME|pos$|pos-admin1234$)') {
            continue
        }
        $hits += $line
    }
}

if ($hits.Count -gt 0) {
    Write-Host "[pre-commit] Blocked: possible secret detected in staged changes." -ForegroundColor Red
    Write-Host "Review the added lines:" -ForegroundColor Yellow
    $hits | Select-Object -First 10 | ForEach-Object { Write-Host "  $_" -ForegroundColor Yellow }
    if ($hits.Count -gt 10) {
        Write-Host "  ...and $($hits.Count - 10) more" -ForegroundColor Yellow
    }
    Write-Host "Use placeholders in docs/templates and keep real values in .env only." -ForegroundColor Yellow
    exit 1
}

exit 0



