$ErrorActionPreference = "Stop"

$port = 9730
$connection = Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue | Where-Object { $_.LocalPort -eq $port } | Select-Object -First 1

if ($connection) {
    Write-Host "DEMO runtime is listening on port $port (PID: $($connection.OwningProcess))"
    exit 0
}

Write-Host "DEMO runtime is not listening on port $port"
exit 1
