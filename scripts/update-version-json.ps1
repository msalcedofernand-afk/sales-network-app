param(
    [Parameter(Mandatory = $true)] [ValidateSet("stable", "beta")] [string]$Channel,
    [Parameter(Mandatory = $true)] [string]$ApkPath,
    [Parameter(Mandatory = $true)] [string]$OutputPath,
    [int]$VersionCode = 0,
    [string]$VersionName = "",
    [int]$MinSupportedVersionCode = 0,
    [string]$ApkName = "",
    [string[]]$ReleaseNotes = @("Mejoras de seguridad y estabilidad."),
    [string]$ReleasedAt = (Get-Date).ToUniversalTime().ToString("o")
)

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot
$propertiesPath = Join-Path $projectRoot "version.properties"
$resolvedApk = (Resolve-Path -LiteralPath $ApkPath).Path
$props = @{}
Get-Content -LiteralPath $propertiesPath | ForEach-Object {
    if ($_ -match '^\s*([^#][^=]*)=(.*)$') { $props[$Matches[1].Trim()] = $Matches[2].Trim() }
}

$versionCode = if ($VersionCode -gt 0) { $VersionCode } else { [int]$props.versionCode }
$versionName = if ($VersionName) { $VersionName } else { [string]$props.versionName }
if ($Channel -eq "beta" -and -not $PSBoundParameters.ContainsKey("VersionName")) { $versionName = "$versionName-beta.$($props.betaNumber)" }
if ($MinSupportedVersionCode -le 0) { $MinSupportedVersionCode = $versionCode }

$expectedPackage = if ($Channel -eq "beta") { "com.salesnetwork.avon.app.beta" } else { "com.salesnetwork.avon.app" }
$apkName = if ($ApkName) { $ApkName } elseif ($Channel -eq "beta") { "sales-network-beta.apk" } else { "sales-network-stable.apk" }
$branch = if ($Channel -eq "beta") { "beta" } else { "main" }
$apkUrl = "https://raw.githubusercontent.com/msalcedofernand-afk/sales-network-app-releases/$branch/releases/$apkName"
$sha256 = (Get-FileHash -LiteralPath $resolvedApk -Algorithm SHA256).Hash.ToLowerInvariant()

$apkanalyzer = Get-Command apkanalyzer -ErrorAction SilentlyContinue
if ($apkanalyzer) {
    $actualPackage = (& $apkanalyzer.Source manifest application-id $resolvedApk).Trim()
    $actualCode = [int](& $apkanalyzer.Source manifest version-code $resolvedApk).Trim()
    $actualName = (& $apkanalyzer.Source manifest version-name $resolvedApk).Trim()
    if ($actualPackage -ne $expectedPackage) { throw "Package inesperado: $actualPackage (esperado $expectedPackage)" }
    if ($actualCode -ne $versionCode) { throw "versionCode del APK: $actualCode (esperado $versionCode)" }
    if ($actualName -ne $versionName) { throw "versionName del APK: $actualName (esperado $versionName)" }
}

$manifest = [ordered]@{
    schemaVersion = 1; versionCode = $versionCode; versionName = $versionName; channel = $Channel
    apkUrl = $apkUrl; sha256 = $sha256; minSupportedVersionCode = $MinSupportedVersionCode
    releasedAt = $ReleasedAt; releaseNotes = @($ReleaseNotes)
}
$parent = Split-Path -Parent $OutputPath
if ($parent) { New-Item -ItemType Directory -Force -Path $parent | Out-Null }
$manifest | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath $OutputPath -Encoding utf8NoBOM
Write-Host "Manifiesto $Channel generado desde $resolvedApk -> $OutputPath"
