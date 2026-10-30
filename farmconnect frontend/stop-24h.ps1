$logDir = Join-Path $PSScriptRoot 'runtime-logs'
$pidFile = Join-Path $logDir 'supervisor.pid'
if (-not (Test-Path -LiteralPath $pidFile)) {
    Write-Output 'The 24 hour site supervisor is not running.'
    exit 0
}
$supervisorId = [int](Get-Content -LiteralPath $pidFile -Raw)
Stop-Process -Id $supervisorId -Force -ErrorAction SilentlyContinue
foreach ($name in 'frontend', 'backend') {
    $servicePidFile = Join-Path $logDir "$name.pid"
    if (Test-Path -LiteralPath $servicePidFile) {
        $serviceId = [int](Get-Content -LiteralPath $servicePidFile -Raw)
        & taskkill.exe /PID $serviceId /T /F 2>$null | Out-Null
        Remove-Item -LiteralPath $servicePidFile -Force -ErrorAction SilentlyContinue
    }
}
Remove-Item -LiteralPath $pidFile -Force -ErrorAction SilentlyContinue
Write-Output 'Stopped the site supervisor and its frontend/backend processes.'
