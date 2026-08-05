<#
Install the database stack the Connections pane tests need (Windows).

Called from .github/actions/os-e2e-deps when its install-database-stack input
is set.

What gets installed, and what does not:

  psqlodbc     PostgreSQL ODBC driver, from Chocolatey. Not present on any
               runner image; the only ODBC drivers those ship are Microsoft's
               SQL Server ones.
  sqliteodbc   SQLite ODBC driver. SQLite needs no server: the driver opens a
               database file directly, so this is the whole requirement for
               that target. There is NO Chocolatey package for it (checked
               2026-08-05: `psqlodbc` exists, `sqliteodbc` does not), so it is
               downloaded from the author's site and installed silently.
  PostgreSQL   NOT installed. The windows-2025 image already carries a full
               PostgreSQL 17 server (stopped and disabled) with PGBIN set, and
               scripts/db/postgres/windows.ps1 starts its own throwaway
               cluster from those binaries. Installing a second copy would
               cost minutes for nothing.
  unixODBC     Not applicable. Windows has a driver manager built into the OS,
               so unlike macOS and Linux there is nothing to install for it,
               and the R odbc package links against the system one.

MySQL is currently deactivated in utils/db-targets.ts. Re-enabling it here
means adding a MySQL *server* install (the image ships only the client), which
is the reason it was parked.

Only CI runs this. A developer's machine is expected to have the drivers
installed by hand, and the suite deliberately does not mutate it. A missing
driver is not fatal: the target goes unregistered and the connections specs
skip with a reason naming it.

Idempotent, so re-running on a warm runner is cheap.
#>

$ErrorActionPreference = 'Stop'

if (-not (Get-Command choco.exe -ErrorAction SilentlyContinue)) {
    throw 'choco is not available; it is preinstalled on GitHub Windows runners'
}

$packages = @('psqlodbc')

# Set on every runner, but defaulted rather than trusted: Join-Path throws on a
# null first argument, which would turn a missing variable into a confusing
# parameter-binding error instead of a plain install.
$chocoRoot = if ($env:ChocolateyInstall) { $env:ChocolateyInstall } else { 'C:\ProgramData\chocolatey' }

foreach ($package in $packages) {
    # `choco list --local-only` is the pre-2.0 spelling; 2.x made `list` local
    # by default and removed the switch, so query the lib directory instead --
    # stable across both.
    $installed = Test-Path (Join-Path $chocoRoot "lib\$package")
    if ($installed) {
        Write-Output "[db-deps] $package already installed"
        continue
    }
    Write-Output "[db-deps] installing $package"
    & choco.exe install $package --yes --no-progress --limit-output
    if ($LASTEXITCODE -ne 0) {
        throw "choco install $package failed (exit $LASTEXITCODE)"
    }
}

# SQLite ODBC driver: no Chocolatey package exists, so fetch the author's
# 64-bit NSIS installer and run it unattended. `/S` is NSIS's standard silent
# switch; the author's page documents the installer as NSIS but does not
# document the switch, so the verification block below is what actually
# confirms the install worked.
$sqliteOdbcUrl = 'https://ch-werner.hier-im-netz.de/sqliteodbc/sqliteodbc_w64.exe'
$sqliteOdbcExe = Join-Path $env:TEMP 'sqliteodbc_w64.exe'
Write-Output "[db-deps] downloading SQLite ODBC driver from $sqliteOdbcUrl"
# Progress rendering makes Invoke-WebRequest dramatically slower in CI.
$prevProgress = $ProgressPreference
$ProgressPreference = 'SilentlyContinue'
try {
    Invoke-WebRequest -Uri $sqliteOdbcUrl -OutFile $sqliteOdbcExe -UseBasicParsing
} finally {
    $ProgressPreference = $prevProgress
}

Write-Output '[db-deps] installing SQLite ODBC driver (silent)'
$proc = Start-Process -FilePath $sqliteOdbcExe -ArgumentList '/S' -Wait -PassThru
if ($proc.ExitCode -ne 0) {
    throw "sqliteodbc installer exited $($proc.ExitCode)"
}
Remove-Item $sqliteOdbcExe -Force -ErrorAction SilentlyContinue

# Verify the install produced drivers the tests can actually find, and fail
# here if it did not. Without this a renamed or relocated package would
# surface much later as a suite that skips every connections test and reports
# green, which is the failure mode this step exists to prevent.
#
# The check mirrors how utils/connections.ts locates an installed driver on
# Windows: read the names the vendors' own installers registered under the
# machine-wide ODBCINST.INI, rather than guessing at version-numbered paths
# like psqlODBC\1600\bin.
$odbcInst = 'HKLM:\SOFTWARE\ODBC\ODBCINST.INI\ODBC Drivers'
if (-not (Test-Path $odbcInst)) {
    throw "no ODBC driver registry key at $odbcInst; nothing registered a driver"
}
$registered = (Get-Item $odbcInst).GetValueNames()
Write-Output "[db-deps] registered ODBC drivers: $($registered -join ', ')"

$expected = @{
    'PostgreSQL' = '^PostgreSQL Unicode'
    'SQLite'     = '^SQLite3'
}
$missing = @()
foreach ($label in $expected.Keys) {
    if (-not ($registered | Where-Object { $_ -match $expected[$label] })) {
        $missing += "$label (no registered driver matching '$($expected[$label])')"
    }
}

# PGBIN is what the provisioning script relies on for the preinstalled server.
if (-not ($env:PGBIN -and (Test-Path (Join-Path $env:PGBIN 'initdb.exe')))) {
    # Probed through .NET for the same reason as Get-PgBin in
    # scripts/db/postgres/windows.ps1: Directory.Exists returns false for a
    # drive that does not exist, where Join-Path and Test-Path would throw.
    $programFiles = if ($env:ProgramFiles) { $env:ProgramFiles } else { 'C:\Program Files' }
    $fallback = "$programFiles\PostgreSQL"
    $found = $false
    if ([System.IO.Directory]::Exists($fallback)) {
        $found = [bool](Get-ChildItem $fallback -Directory -ErrorAction SilentlyContinue |
            Where-Object { Test-Path (Join-Path $_.FullName 'bin\initdb.exe') })
    }
    if (-not $found) {
        $missing += 'initdb.exe (no PostgreSQL server via PGBIN or Program Files)'
    }
}

if ($missing.Count -gt 0) {
    Write-Error 'database stack incomplete after install; missing:'
    $missing | ForEach-Object { Write-Error "  $_" }
    exit 1
}

Write-Output '[db-deps] Windows database stack ready'
exit 0
