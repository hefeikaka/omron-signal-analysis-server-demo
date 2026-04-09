$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$runtimeHome = Split-Path -Parent $scriptDir
$etcDir = Join-Path $runtimeHome "etc"
$libDir = Join-Path $runtimeHome "lib"
$logDir = Join-Path $runtimeHome "log"
$stdoutLog = Join-Path $logDir "demo-runtime.out.log"
$stderrLog = Join-Path $logDir "demo-runtime.err.log"
$jarPath = Get-ChildItem (Join-Path $libDir 'platform-runtime-*.jar') | Select-Object -First 1

if (-not $jarPath) {
    throw "platform-runtime jar was not found in $libDir"
}

$classpath = @($jarPath.FullName)
Get-ChildItem $libDir -Filter '*.jar' | Where-Object { $_.FullName -ne $jarPath.FullName } | ForEach-Object {
    $classpath += $_.FullName
}
$classpathArg = [string]::Join(';', $classpath)

if (-not (Test-Path $logDir)) {
    New-Item -ItemType Directory -Path $logDir | Out-Null
}

$existing = Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue | Where-Object { $_.LocalPort -eq 9730 } | Select-Object -First 1
if ($existing) {
    Write-Host "DEMO runtime is already listening on port 9730 (PID: $($existing.OwningProcess))"
    exit 0
}

$javaArgs = @(
    "-Dopen.runtime.home=$runtimeHome"
    "-Dopen.runtime.storage.mode=mongo"
    "-cp"
    $classpathArg
    "com.omron.oss.runtime.OpenRuntimeServer"
)

Write-Host "Starting DEMO runtime server in background"
Write-Host "runtimeHome: $runtimeHome"
Write-Host "etcDir: $etcDir"
Write-Host "stdoutLog: $stdoutLog"
Write-Host "stderrLog: $stderrLog"

$process = Start-Process -FilePath "java" `
    -ArgumentList $javaArgs `
    -WorkingDirectory $runtimeHome `
    -RedirectStandardOutput $stdoutLog `
    -RedirectStandardError $stderrLog `
    -PassThru

Start-Sleep -Seconds 2
$started = Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue | Where-Object { $_.LocalPort -eq 9730 } | Select-Object -First 1
if ($started) {
    Write-Host "DEMO runtime started on port 9730 (PID: $($started.OwningProcess))"
    exit 0
}

if (-not $process.HasExited) {
    Stop-Process -Id $process.Id -Force -ErrorAction SilentlyContinue
}

throw "DEMO runtime failed to start. Check $stdoutLog and $stderrLog"
