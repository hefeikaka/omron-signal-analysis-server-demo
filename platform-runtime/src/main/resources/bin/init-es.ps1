$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$runtimeHome = Split-Path -Parent $scriptDir
$payloadDir = Join-Path $runtimeHome "data\\init\\es"
$mappingFile = Join-Path $payloadDir "signal-analysischannel-process-v1.0.mapping.json"
$indexUrl = "http://127.0.0.1:9200/signal-analysischannel-process-v1.0"

if (-not (Test-Path $mappingFile)) {
    throw "ES mapping file not found: $mappingFile"
}

Write-Host "Creating or updating Elasticsearch index at $indexUrl"
Invoke-WebRequest -Uri $indexUrl -Method PUT -InFile $mappingFile -ContentType "application/json" -UseBasicParsing | Out-Null
Write-Host "Elasticsearch initialization completed"
