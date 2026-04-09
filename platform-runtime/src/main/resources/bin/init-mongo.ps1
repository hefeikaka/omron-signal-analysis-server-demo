$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$runtimeHome = Split-Path -Parent $scriptDir
$mongoScript = Join-Path $runtimeHome "data\\init\\mongo\\init-db.js"

if (-not (Test-Path $mongoScript)) {
    throw "Mongo init script not found: $mongoScript"
}

if (-not (Get-Command mongosh -ErrorAction SilentlyContinue)) {
    throw "mongosh is not installed or not on PATH"
}

Write-Host "Initializing MongoDB with $mongoScript"
& mongosh --file $mongoScript
