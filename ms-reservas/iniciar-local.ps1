param([switch]$PedirClave)
$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
$credentialFile = Join-Path $env:LOCALAPPDATA 'SanaEsencia\reservas-db.clixml'
if (-not $PedirClave -and (Test-Path -LiteralPath $credentialFile)) {
    $securePassword = Import-Clixml -LiteralPath $credentialFile
} else {
    $securePassword = Read-Host 'Contraseña del usuario técnico sana_reservas' -AsSecureString
}
$env:RESERVAS_DB_PASSWORD = [System.Net.NetworkCredential]::new('', $securePassword).Password
if (-not $env:JAVA_HOME -and (Test-Path -LiteralPath 'C:\Program Files\Java\jdk-17')) {
    $env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
}
try {
    & "$PSScriptRoot\mvnw.cmd" spring-boot:run
} finally {
    Remove-Item Env:RESERVAS_DB_PASSWORD -ErrorAction SilentlyContinue
}
