# ============================================================
# Script de ejecución JMeter (PowerShell) — ms-loadfiles-neg
# Uso:
#   .\scripts\run-tests.ps1
#   $env:AMBIENTE_PIPE = "qa"; .\scripts\run-tests.ps1
#   $env:AMBIENTE_PIPE = "prod"; $env:PERFIL = "smoke"; .\scripts\run-tests.ps1
# ============================================================
param(
    [string]$Ambiente = $env:AMBIENTE_PIPE,
    [string]$Perfil   = $env:PERFIL
)

if (-not $Ambiente) { $Ambiente = "dev" }
if (-not $Perfil)   { $Perfil   = "master" }

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$BaseDir   = Split-Path -Parent $ScriptDir
$Timestamp = Get-Date -Format "yyyyMMdd-HHmmss"

# ── Validar ambiente ──
if ($Ambiente -notin @("dev","qa","prod")) {
    Write-Error "AMBIENTE_PIPE debe ser: dev, qa, prod"
    exit 1
}

# ── Seleccionar plan JMX ──
$PlanFile = switch ($Perfil) {
    "master" { Join-Path $BaseDir "plans\ms-loadfiles-master.jmx" }
    "smoke"  { Join-Path $BaseDir "plans\smoke\ms-loadfiles-smoke.jmx" }
    "load"   { Join-Path $BaseDir "plans\load\ms-loadfiles-load.jmx" }
    "stress" { Join-Path $BaseDir "plans\stress\ms-loadfiles-stress.jmx" }
    default  { Write-Error "PERFIL debe ser: master, smoke, load, stress"; exit 1 }
}

$PropsFile  = Join-Path $BaseDir "config\$Ambiente.properties"
$ResultsDir = Join-Path $BaseDir "results"
$ReportsDir = Join-Path $BaseDir "reports\run-$Ambiente-$Perfil-$Timestamp"
$ResultFile = Join-Path $ResultsDir "result-$Ambiente-$Perfil-$Timestamp.jtl"
$LogFile    = Join-Path $ResultsDir "jmeter-$Ambiente-$Perfil-$Timestamp.log"

# ── Validaciones ──
if (-not (Test-Path $PlanFile)) {
    Write-Error "Plan JMX no encontrado: $PlanFile"
    exit 1
}
if (-not (Test-Path $PropsFile)) {
    Write-Error "Properties no encontrado: $PropsFile"
    exit 1
}
if (-not (Get-Command jmeter -ErrorAction SilentlyContinue)) {
    Write-Error "jmeter no encontrado en PATH. Instalar Apache JMeter 5.6+"
    exit 1
}

# ── Crear directorios de salida ──
New-Item -ItemType Directory -Force -Path $ResultsDir | Out-Null
New-Item -ItemType Directory -Force -Path $ReportsDir | Out-Null

Write-Host "╔══════════════════════════════════════════════════════╗"
Write-Host "║    ms-loadfiles-neg — JMeter Performance Suite      ║"
Write-Host "╠══════════════════════════════════════════════════════╣"
Write-Host "║  Ambiente : $Ambiente"
Write-Host "║  Perfil   : $Perfil"
Write-Host "║  Plan     : $(Split-Path -Leaf $PlanFile)"
Write-Host "║  Timestamp: $Timestamp"
Write-Host "╚══════════════════════════════════════════════════════╝"

# ── Ejecutar JMeter ──
$jmeterArgs = @(
    "-n",
    "-t", $PlanFile,
    "-p", $PropsFile,
    "-JAMBIENTE_PIPE=$Ambiente",
    "-l", $ResultFile,
    "-e",
    "-o", $ReportsDir,
    "-j", $LogFile
)

& jmeter @jmeterArgs
$ExitCode = $LASTEXITCODE

Write-Host ""
Write-Host "══════════════════════════════════════════════"
if ($ExitCode -eq 0) {
    Write-Host "OK Ejecucion completada"
} else {
    Write-Host "ERROR Ejecucion con errores (exit code: $ExitCode)"
}
Write-Host "  Resultados : $ResultFile"
Write-Host "  Reporte    : $ReportsDir\index.html"
Write-Host "══════════════════════════════════════════════"

exit $ExitCode