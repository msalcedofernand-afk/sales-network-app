param(
    [Parameter(Mandatory = $true)] [ValidatePattern('^\d+\.\d+\.\d+$')] [string]$VersionName,
    [Parameter(Mandatory = $true)] [ValidateRange(1, 2147483647)] [int]$VersionCode,
    [ValidateRange(1, 9999)] [int]$BetaNumber = 1,
    [Parameter(Mandatory = $true)] [string[]]$ReleaseNotes
)

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot
$propertiesPath = Join-Path $projectRoot "version.properties"
@(
    "# Fuente unica de version. Solo scripts/prepare-release.ps1 puede modificarla."
    "versionCode=$VersionCode"
    "versionName=$VersionName"
    "betaNumber=$BetaNumber"
) | Set-Content -LiteralPath $propertiesPath -Encoding utf8NoBOM

$placeholderSha = "0" * 64
foreach ($channel in @("stable", "beta")) {
    $branch = if ($channel -eq "beta") { "beta" } else { "main" }
    $displayVersion = if ($channel -eq "beta") { "$VersionName-beta.$BetaNumber" } else { $VersionName }
    $template = [ordered]@{
        schemaVersion = 1; versionCode = $VersionCode; versionName = $displayVersion; channel = $channel
        apkUrl = "https://raw.githubusercontent.com/msalcedofernand-afk/sales-network-app-releases/$branch/releases/sales-network-$channel.apk"
        sha256 = $placeholderSha; minSupportedVersionCode = $VersionCode; releasedAt = $null
        releaseNotes = @($ReleaseNotes)
    }
    foreach ($relativePath in @("updates/$channel.json", "web/public/updates/$channel.json")) {
        $path = Join-Path $projectRoot $relativePath
        $template | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath $path -Encoding utf8NoBOM
    }
}
Write-Host "Preparada $VersionName (code=$VersionCode, beta=$BetaNumber)."
Write-Host "Los SHA y releasedAt se completan después de compilar el APK firmado."

