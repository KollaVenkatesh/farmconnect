$logDir = Join-Path $PSScriptRoot 'runtime-logs'
New-Item -ItemType Directory -Force -Path $logDir | Out-Null
$pidFile = Join-Path $logDir 'supervisor.pid'
if (Test-Path -LiteralPath $pidFile) {
    $existingId = [int](Get-Content -LiteralPath $pidFile -Raw)
    if (Get-Process -Id $existingId -ErrorAction SilentlyContinue) {
        Write-Output "Site supervisor is already running (PID $existingId)."
        exit 0
    }
    Remove-Item -LiteralPath $pidFile -Force -ErrorAction SilentlyContinue
}
$backendDir = Join-Path (Split-Path -Parent $PSScriptRoot) 'farmconnect backend'
$jarPath = Join-Path $backendDir 'target\ecommerce-backend-0.0.1-SNAPSHOT.jar'
$latestSource = Get-ChildItem (Join-Path $backendDir 'src') -Recurse -File |
    Sort-Object LastWriteTimeUtc -Descending | Select-Object -First 1
if (-not (Test-Path -LiteralPath $jarPath) -or $latestSource.LastWriteTimeUtc -gt (Get-Item -LiteralPath $jarPath).LastWriteTimeUtc) {
    Write-Output 'Building the backend application...'
    & (Join-Path $backendDir 'mvnw.cmd') package -DskipTests
    if ($LASTEXITCODE -ne 0) { throw 'Backend build failed; the site was not started.' }
}
$runner = Join-Path $PSScriptRoot 'run-24h.ps1'
$arguments = "-NoProfile -ExecutionPolicy Bypass -File `"$runner`""
$process = Start-Process -FilePath powershell.exe -ArgumentList $arguments `
    -WindowStyle Hidden -PassThru `
    -RedirectStandardOutput (Join-Path $logDir 'supervisor-launch.out.log') `
    -RedirectStandardError (Join-Path $logDir 'supervisor-launch.err.log')
Write-Output "Started the 24 hour site supervisor (PID $($process.Id))."
