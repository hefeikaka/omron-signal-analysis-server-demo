$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$runtimeHome = Split-Path -Parent $scriptDir
$samplePayload = Join-Path $runtimeHome 'data\sample-signal-ingest.json'

$health = Invoke-WebRequest -Uri 'http://127.0.0.1:9730/healthz' -UseBasicParsing
Write-Host "healthz => $($health.Content)"

$edge = Invoke-WebRequest -Uri 'http://127.0.0.1:9730/edge/' -UseBasicParsing
Write-Host "edge => HTTP $($edge.StatusCode)"

if (Test-Path $samplePayload) {
    $body = Get-Content $samplePayload -Raw
    $ingest = Invoke-WebRequest -Uri 'http://127.0.0.1:9730/api/signal/ingest' -Method POST -Body $body -ContentType 'application/json' -UseBasicParsing
    Write-Host "ingest => $($ingest.Content)"
} else {
    Write-Host "sample payload not found at $samplePayload"
}
