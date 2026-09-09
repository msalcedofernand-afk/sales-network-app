param(
  [string]$ProjectRef = "xceqwexdufdgnmctsxcg"
)

$ErrorActionPreference = "Stop"
if ([string]::IsNullOrWhiteSpace($env:SUPABASE_ACCESS_TOKEN)) { throw "Define SUPABASE_ACCESS_TOKEN antes de ejecutar este script." }
if ([string]::IsNullOrWhiteSpace($env:SUPABASE_DB_PASSWORD)) { throw "Define SUPABASE_DB_PASSWORD antes de ejecutar este script." }

npx supabase link --project-ref $ProjectRef --password $env:SUPABASE_DB_PASSWORD
npx supabase db push

$functions = Get-ChildItem "supabase/functions" -Directory | Where-Object { $_.Name -ne "_shared" }
foreach ($function in $functions) {
  npx supabase functions deploy $function.Name --project-ref $ProjectRef
}

Write-Host "Migraciones y Edge Functions desplegadas en $ProjectRef."
