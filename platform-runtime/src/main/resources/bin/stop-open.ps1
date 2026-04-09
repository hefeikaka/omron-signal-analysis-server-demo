$ErrorActionPreference = "Stop"

$port = 9730
$connections = Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue | Where-Object { $_.LocalPort -eq $port }

if (-not $connections) {
    Write-Host "DEMO runtime is not running on port $port"
    exit 0
}

$connections | Select-Object -ExpandProperty OwningProcess -Unique | ForEach-Object {
    Stop-Process -Id $_ -Force
    Write-Host "Stopped process $_ listening on port $port"
}
