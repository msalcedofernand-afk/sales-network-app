# update-version-json.ps1
# Auto-updates version.json files after each build
# Run after: .\gradlew.bat :app:assembleStableDebug or :app:assembleBetaDebug

param(
    [Parameter(Mandatory=$false)]
    [ValidateSet("stable", "beta")]
    [string]$Channel = "stable"
)

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot
$versionPropsFile = Join-Path $projectRoot "version.properties"
$webUpdatesDir = Join-Path $projectRoot "web\public\updates"

# Read current version from version.properties
if (-not (Test-Path $versionPropsFile)) {
    Write-Error "version.properties not found at: $versionPropsFile"
    exit 1
}

$versionProps = Get-Content $versionPropsFile | ForEach-Object {
    if ($_ -match "^(\w+)=(.+)$") {
        @{ Key = $Matches[1]; Value = $Matches[2].Trim() }
    }
} | Where-Object { $_ }

$versionCode = ($versionProps | Where-Object { $_.Key -eq "versionCode" }).Value
$versionName = ($versionProps | Where-Object { $_.Key -eq "versionName" }).Value

if (-not $versionCode -or -not $versionName) {
    Write-Error "Could not read versionCode or versionName from version.properties"
    exit 1
}

Write-Host "Current version: $versionName (code: $versionCode)" -ForegroundColor Cyan

# APK URL mapping
$apkUrls = @{
    "stable" = "https://raw.githubusercontent.com/msalcedofernand-afk/sales-network-app-releases/main/releases/sales-network-stable.apk"
    "beta" = "https://raw.githubusercontent.com/msalcedofernand-afk/sales-network-app-releases/beta/releases/sales-network-beta.apk"
}

# Release notes per channel
$releaseNotes = @{
    "stable" = "Versión estable actualizada. Mejoras de rendimiento y correcciones."
    "beta" = "Versión beta con cambios experimentales. Puede contener bugs."
}

# Update JSON file
$jsonFile = Join-Path $webUpdatesDir "$Channel.json"
$json = @{
    versionCode = [int]$versionCode
    versionName = $versionName
    channel = $Channel
    apkUrl = $apkUrls[$Channel]
    releaseNotes = $releaseNotes[$Channel]
    mandatory = $true
} | ConvertTo-Json -Depth 10

Set-Content -Path $jsonFile -Value $json -Encoding UTF8
Write-Host "Updated: $jsonFile" -ForegroundColor Green

# Also copy APK to releases folder if it exists
$apkSource = Join-Path $projectRoot "app\build\outputs\apk\$Channel\debug\app-$Channel-debug.apk"
$apkDest = Join-Path $projectRoot "releases\sales-network-$Channel.apk"

if (Test-Path $apkSource) {
    Copy-Item -Path $apkSource -Destination $apkDest -Force
    Write-Host "Copied APK: $apkDest" -ForegroundColor Green
} else {
    Write-Host "APK not found at: $apkSource (run build first)" -ForegroundColor Yellow
}

Write-Host "`nVersion manifest updated for $Channel channel." -ForegroundColor Cyan
