$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$runtimeHome = Split-Path -Parent $scriptDir
$etcDir = Join-Path $runtimeHome "etc"

function Read-PropertyFile {
    param([string]$Path)
    $result = @{}
    foreach ($line in Get-Content $Path) {
        $trimmed = $line.Trim()
        if (-not $trimmed -or $trimmed.StartsWith("#")) {
            continue
        }

        $parts = $trimmed.Split("=", 2)
        if ($parts.Length -eq 2) {
            $result[$parts[0].Trim()] = $parts[1].Trim()
        }
    }
    return $result
}

function Resolve-Property {
    param(
        [hashtable]$Properties,
        [string]$Name,
        [string]$DefaultValue
    )

    if ($Properties.ContainsKey($Name) -and $Properties[$Name]) {
        return $Properties[$Name]
    }

    return $DefaultValue
}

function Resolve-Placeholders {
    param(
        [string]$Value,
        [hashtable]$Properties
    )

    $resolved = $Value
    foreach ($entry in $Properties.GetEnumerator()) {
        $token = '${' + $entry.Key + '}'
        $resolved = $resolved.Replace($token, [string]$entry.Value)
    }

    return $resolved
}

$mongoCfg = Read-PropertyFile (Join-Path $etcDir "com.omron.gc.cm.mongodb-signal.cfg")
$systemCfg = Read-PropertyFile (Join-Path $etcDir "custom.system.properties")
$indexCfg = Read-PropertyFile (Join-Path $etcDir "custom.index.properties")
$mongoDatabase = Resolve-Placeholders (Resolve-Property $mongoCfg "database" "DB_SingalAys") $systemCfg
$mongoHost = Resolve-Placeholders (Resolve-Property $mongoCfg "url" "127.0.0.1") $systemCfg
$mongoPort = Resolve-Placeholders (Resolve-Property $mongoCfg "port" "27017") $systemCfg
$mongoInit = Join-Path $scriptDir "mongo-init.js"
$indexCatalog = Get-Content (Join-Path $scriptDir "index-catalog.json") -Raw | ConvertFrom-Json
$indexUrl = Resolve-Placeholders (Resolve-Property $indexCfg "karaf.indexing.url" "http://127.0.0.1:9200") $indexCfg
$indexUrl = Resolve-Placeholders $indexUrl $systemCfg

Write-Host "MongoDB target => $mongoHost`:$mongoPort / $mongoDatabase"
if (Get-Command mongosh -ErrorAction SilentlyContinue) {
    $env:OPEN_MONGO_DATABASE = $mongoDatabase
    & mongosh --host $mongoHost --port $mongoPort $mongoDatabase $mongoInit
    Remove-Item Env:\OPEN_MONGO_DATABASE -ErrorAction SilentlyContinue
} elseif (Get-Command mongo -ErrorAction SilentlyContinue) {
    & mongo "$mongoHost`:$mongoPort/$mongoDatabase" $mongoInit
} else {
    Write-Host "mongosh/mongo was not found. Run the following command manually:"
    Write-Host "mongosh --host $mongoHost --port $mongoPort $mongoDatabase $mongoInit"
}

$mappingBody = @'
{
  "settings": {
    "number_of_shards": 1,
    "number_of_replicas": 0
  },
  "mappings": {
    "dynamic": true,
    "properties": {
      "machineId": { "type": "keyword" },
      "collectedAt": { "type": "date", "format": "epoch_millis" },
      "samplingFrequency": { "type": "integer" },
      "values": { "type": "object", "enabled": true }
    }
  }
}
'@

Write-Host "Elasticsearch target => $indexUrl"
foreach ($indexName in $indexCatalog.indices) {
    $target = "$indexUrl/$indexName"
    if (Get-Command curl.exe -ErrorAction SilentlyContinue) {
        $statusCode = & curl.exe -s -o NUL -w "%{http_code}" -X PUT $target -H "Content-Type: application/json" -d $mappingBody
        if ($statusCode -eq "200" -or $statusCode -eq "201") {
            Write-Host "Created or updated $indexName"
        } elseif ($statusCode -eq "400" -or $statusCode -eq "409") {
            Write-Host "Skipped existing $indexName"
        } else {
            throw "Failed to initialize $indexName, HTTP $statusCode"
        }
    } else {
        Write-Host "curl.exe was not found. Run this manually:"
        Write-Host "curl -X PUT $target -H `"`"Content-Type: application/json`"`" -d `"`"$mappingBody`"`""
    }
}
