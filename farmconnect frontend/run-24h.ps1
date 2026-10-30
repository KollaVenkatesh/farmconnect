$ErrorActionPreference = 'Stop'
$frontendDir = $PSScriptRoot
$backendDir = Join-Path (Split-Path -Parent $PSScriptRoot) 'farmconnect backend'
$logDir = Join-Path $PSScriptRoot 'runtime-logs'
$jarPath = Join-Path $backendDir 'target\ecommerce-backend-0.0.1-SNAPSHOT.jar'
$vitePath = Join-Path $frontendDir 'node_modules\vite\bin\vite.js'
$nodePath = (Get-Command node.exe).Source
$javaPath = (Get-Command java.exe).Source
New-Item -ItemType Directory -Force -Path $logDir | Out-Null

Add-Type -TypeDefinition @"
using System.Runtime.InteropServices;
public static class KeepAwake {
    [DllImport("kernel32.dll")]
    public static extern uint SetThreadExecutionState(uint flags);
}
"@
[void][KeepAwake]::SetThreadExecutionState([uint32]::Parse('2147483649'))

$pidFile = Join-Path $logDir 'supervisor.pid'
Set-Content -LiteralPath $pidFile -Value $PID

function Test-LocalMongo {
    $client = [System.Net.Sockets.TcpClient]::new()
    try {
        $task = $client.ConnectAsync('127.0.0.1', 27017)
        return $task.Wait(500) -and $client.Connected
    } catch {
        return $false
    } finally {
        $client.Dispose()
    }
}

function Start-ManagedService([string]$Name, [string]$FilePath, [string]$Directory, [string[]]$Arguments) {
    $stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
    $stdout = Join-Path $logDir "$Name-$stamp.out.log"
    $stderr = Join-Path $logDir "$Name-$stamp.err.log"
    $process = Start-Process -FilePath $FilePath -ArgumentList $Arguments `
        -WorkingDirectory $Directory -RedirectStandardOutput $stdout `
        -RedirectStandardError $stderr -WindowStyle Hidden -PassThru
    Set-Content -LiteralPath (Join-Path $logDir "$Name.pid") -Value $process.Id
    Add-Content -LiteralPath (Join-Path $logDir 'supervisor.log') `
        -Value "$(Get-Date -Format o) started $Name pid=$($process.Id)"
    return $process
}

function Stop-ManagedService($Process) {
    if ($null -ne $Process -and -not $Process.HasExited) {
        & taskkill.exe /PID $Process.Id /T /F 2>$null | Out-Null
    }
}

$frontend = $null
$backend = $null
$mongoWasAvailable = $null
try {
    while ($true) {
        $mongoAvailable = (-not [string]::IsNullOrWhiteSpace($env:MONGODB_URI)) -or (Test-LocalMongo)
        if ($null -eq $mongoWasAvailable -or $mongoAvailable -ne $mongoWasAvailable -or $null -eq $backend -or $backend.HasExited) {
            Stop-ManagedService $backend
            $indexSetting = if ($mongoAvailable) { 'true' } else { 'false' }
            $backendArguments = @('-jar', "`"$jarPath`"", "--spring.data.mongodb.auto-index-creation=$indexSetting")
            $backend = Start-ManagedService 'backend' $javaPath $backendDir $backendArguments
            $mongoWasAvailable = $mongoAvailable
        }
        if ($null -eq $frontend -or $frontend.HasExited) {
            $frontendArguments = @("`"$vitePath`"", '--configLoader', 'native', '--host', '0.0.0.0')
            $frontend = Start-ManagedService 'frontend' $nodePath $frontendDir $frontendArguments
        }
        Start-Sleep -Seconds 5
    }
} finally {
    Stop-ManagedService $frontend
    Stop-ManagedService $backend
    Remove-Item -LiteralPath $pidFile -Force -ErrorAction SilentlyContinue
    Remove-Item -LiteralPath (Join-Path $logDir 'frontend.pid'), (Join-Path $logDir 'backend.pid') -Force -ErrorAction SilentlyContinue
    [void][KeepAwake]::SetThreadExecutionState([uint32]::Parse('2147483648'))
}
