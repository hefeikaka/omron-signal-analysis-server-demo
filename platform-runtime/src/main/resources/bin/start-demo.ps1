$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
& (Join-Path $scriptDir "start-open.ps1")
