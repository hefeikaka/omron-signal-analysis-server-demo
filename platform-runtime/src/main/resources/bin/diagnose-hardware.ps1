param(
    [string]$MongoHost = "127.0.0.1",
    [int]$MongoPort = 27017,
    [string]$MongoDatabase = "db_signal",
    [string]$CollectionName = "rawdata_signal_curve",
    [string]$ComPort = "COM7",
    [int[]]$BaudRates = @(9600, 19200, 38400, 57600, 115200)
)

$ErrorActionPreference = "Stop"

function Write-Section {
    param([string]$Title)
    Write-Host ""
    Write-Host "=== $Title ==="
}

function Find-Mongosh {
    $candidates = @(
        (Get-Command mongosh -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Source -ErrorAction SilentlyContinue),
        "C:\Users\$env:USERNAME\AppData\Local\Programs\mongosh\mongosh.exe",
        "C:\Program Files\MongoDB\mongosh\bin\mongosh.exe"
    ) | Where-Object { $_ }

    foreach ($candidate in $candidates) {
        if (Test-Path $candidate) {
            return $candidate
        }
    }
    throw "mongosh not found. Install MongoDB Shell first."
}

function Invoke-MongoJson {
    param(
        [string]$MongoshPath,
        [string]$Script
    )

    & $MongoshPath --quiet --eval $Script "mongodb://$MongoHost`:$MongoPort/admin"
}

function Test-SerialPortRead {
    param(
        [string]$PortName,
        [int[]]$Rates
    )

    $results = @()
    foreach ($baud in $Rates) {
        $port = $null
        try {
            $port = New-Object System.IO.Ports.SerialPort $PortName, $baud, "None", 8, "One"
            $port.ReadTimeout = 1000
            $port.Open()
            $buffer = New-Object byte[] 256
            $bytesRead = 0
            $samples = New-Object System.Collections.Generic.List[string]
            $deadline = (Get-Date).AddSeconds(3)
            while ((Get-Date) -lt $deadline) {
                try {
                    $count = $port.Read($buffer, 0, $buffer.Length)
                    if ($count -gt 0) {
                        $bytesRead += $count
                        $samples.Add([System.BitConverter]::ToString($buffer, 0, [Math]::Min($count, 24)))
                    }
                } catch [System.TimeoutException] {
                }
            }

            $results += [PSCustomObject]@{
                Baud      = $baud
                Opened    = $true
                BytesRead = $bytesRead
                Sample    = ($samples | Select-Object -First 3) -join " | "
            }
        } catch {
            $results += [PSCustomObject]@{
                Baud      = $baud
                Opened    = $false
                BytesRead = 0
                Sample    = $_.Exception.Message
            }
        } finally {
            if ($port -and $port.IsOpen) {
                $port.Close()
            }
        }
    }

    return $results
}

$mongosh = Find-Mongosh

Write-Section "Runtime"
$rc2Status = & "D:\server\omronATC-signal-analysis-server-1.0.0\omron-signal-analysis-server-1.0.0-RC2\bin\status.bat" 2>$null
$openStatusScript = "D:\server\omronATC-signal-analysis-server-1.0.0\omron-signal-analysis-server-open\platform-runtime\target\omron-signal-analysis-server-open-runtime\bin\status-open.ps1"
$openStatus = if (Test-Path $openStatusScript) { & $openStatusScript 2>$null } else { "Open runtime status script not found" }
Write-Host $rc2Status
Write-Host $openStatus

Write-Section "Mongo"
$mongoScript = "const c = db.getSiblingDB('$MongoDatabase').getCollection('$CollectionName');" +
    "const now = new Date();" +
    "const oneMinuteAgo = new Date(now.getTime() - 60 * 1000);" +
    "const fiveMinutesAgo = new Date(now.getTime() - 5 * 60 * 1000);" +
    "const latest = c.find({}, {_id:0,time:1,subjectCode:1,factor:1,sequenceNumber:1}).sort({time:-1}).limit(1).toArray();" +
    "print(EJSON.stringify({now: now,total: c.countDocuments(),last1m: c.countDocuments({time: {`$gte: oneMinuteAgo}}),last5m: c.countDocuments({time: {`$gte: fiveMinutesAgo}}),latest: latest}));"
$mongoResult = Invoke-MongoJson -MongoshPath $mongosh -Script $mongoScript
Write-Host $mongoResult

Write-Section "Serial Ports"
Get-CimInstance Win32_SerialPort |
    Select-Object DeviceID, Name, Description, PNPDeviceID |
    Format-Table -AutoSize

Write-Section "Port Probe"
$probeResults = Test-SerialPortRead -PortName $ComPort -Rates $BaudRates
$probeResults | Format-Table -AutoSize

Write-Section "Diagnosis"
$mongoData = $mongoResult | ConvertFrom-Json
$latestTime = $null
if ($mongoData.latest -and $mongoData.latest.Count -gt 0) {
    $latestTime = Get-Date ($mongoData.latest[0].time.'$date')
}

if ($mongoData.last1m -gt 0 -or $mongoData.last5m -gt 0) {
    Write-Host "RC2 is receiving recent signal data. Hardware path appears alive."
} else {
    Write-Host "No recent signal data reached MongoDB."
    if ($latestTime) {
        Write-Host ("Latest stored signal time: " + $latestTime.ToString("yyyy-MM-dd HH:mm:ss zzz"))
    }
}

$anyPortData = $probeResults | Where-Object { $_.Opened -and $_.BytesRead -gt 0 }
if ($anyPortData) {
    Write-Host "Serial port returned raw bytes. Hardware is emitting data at at least one tested baud rate."
} else {
    Write-Host "Serial port could be opened, but no bytes were observed at the tested baud rates."
}
