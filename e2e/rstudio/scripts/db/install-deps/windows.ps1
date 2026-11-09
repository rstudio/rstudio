<#
Install the database stack the Connections pane tests need (Windows).

Called from .github/actions/os-e2e-deps when its install-database-stack input
is set.

What gets installed, and what does not:

  psqlodbc     PostgreSQL ODBC driver. Not present on any runner image; the
               only ODBC drivers those ship are Microsoft's SQL Server ones.
               Installed from the x64 MSI on the project's GitHub release,
               not Chocolatey: the community feed fails intermittently (406
               and 503 responses in CI, 2026-11), and choco can exit 0 after
               installing nothing. The x64 MSI alone registers every driver
               name utils/db-targets.ts describes.
  sqliteodbc   SQLite ODBC driver. SQLite needs no server: the driver opens a
               database file directly, so this is the whole requirement for
               that target. Downloaded from the author's site and installed
               silently.
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

# Download a pinned installer, retrying transient network failures. A checksum
# mismatch is not retried: a pinned file that changed upstream won't change
# back. Messages go through Write-Host so they never become function output.
function Save-VerifiedDownload {
    param(
        [Parameter(Mandatory = $true)] [string] $Url,
        [Parameter(Mandatory = $true)] [string] $OutFile,
        [Parameter(Mandatory = $true)] [string] $Sha256
    )

    $delays = @(10, 30, 60)

    # Progress rendering makes Invoke-WebRequest dramatically slower in CI.
    # Assigned here it is local to this function, so nothing to restore.
    $ProgressPreference = 'SilentlyContinue'

    for ($attempt = 0; ; $attempt++) {
        try {
            # -TimeoutSec bounds the wait for the response headers. Under
            # Windows PowerShell 5.1 a stall partway through the body is cut
            # off by .NET's 300s ReadWriteTimeout instead. Either way a stalled
            # connection becomes a retry rather than hanging the step until the
            # job times out.
            Invoke-WebRequest -Uri $Url -OutFile $OutFile -UseBasicParsing -TimeoutSec 120
            break
        } catch {
            # A 4xx other than 408/429 is the server's final answer (the
            # pinned asset is gone, or access is denied), so waiting won't
            # change it. No response at all (DNS, timeout) casts to 0 and is
            # retried.
            $status = [int]$_.Exception.Response.StatusCode
            $permanent = $status -ge 400 -and $status -lt 500 -and $status -notin @(408, 429)
            if ($permanent -or $attempt -ge $delays.Count) {
                throw
            }
            $delay = $delays[$attempt]
            Write-Host "::warning::download of $Url failed ($($_.Exception.Message)); retrying in ${delay}s"
            Start-Sleep -Seconds $delay
        }
    }

    $actual = (Get-FileHash -Path $OutFile -Algorithm SHA256).Hash
    if ($actual -ne $Sha256) {
        Write-Host "ERROR: $(Split-Path $OutFile -Leaf) checksum mismatch:"
        Write-Host "  expected $Sha256"
        Write-Host "  got      $actual"
        exit 1
    }
}

# psqlODBC. To update, change the release tag and take the new
# psqlodbc_x64.msi digest from the release's asset list (`gh api
# repos/postgresql-interfaces/psqlodbc/releases`); use the plain build, not
# the -mimalloc one.
$psqlOdbcUrl = 'https://github.com/postgresql-interfaces/psqlodbc/releases/download/REL-18_00_0005/psqlodbc_x64.msi'
$psqlOdbcSha256 = '56345B91E41302271A7ECFC4F4819F49B0FE9FC061BD2B0D72338D81D30597A9'

# Keyed on the driver the tests select (windowsInstalledDriverPattern in
# utils/db-targets.ts).
if (Test-Path -LiteralPath 'HKLM:\SOFTWARE\ODBC\ODBCINST.INI\PostgreSQL Unicode(x64)') {
    Write-Output '[db-deps] psqlodbc already installed'
} else {
    $psqlOdbcMsi = Join-Path $env:TEMP 'psqlodbc_x64.msi'
    $psqlOdbcLog = Join-Path $env:TEMP 'psqlodbc_x64-install.log'

    Write-Output "[db-deps] downloading psqlodbc from $psqlOdbcUrl"
    Save-VerifiedDownload -Url $psqlOdbcUrl -OutFile $psqlOdbcMsi -Sha256 $psqlOdbcSha256

    Write-Output '[db-deps] installing psqlodbc (silent)'
    $msiArgs = @('/i', "`"$psqlOdbcMsi`"", '/qn', '/norestart', '/l*v', "`"$psqlOdbcLog`"")
    $proc = Start-Process -FilePath msiexec.exe -ArgumentList $msiArgs -Wait -PassThru
    # 3010 is success with a reboot requested; ODBC registration doesn't need one.
    if ($proc.ExitCode -notin @(0, 3010)) {
        # A verbose MSI log ends with a property dump, so its tail says
        # nothing about the failure. The first 'Return value 3' is the action
        # that failed, and the lines before it carry the error. The log itself
        # dies with the runner, so this excerpt is all that survives.
        $failure = Get-Content -Path $psqlOdbcLog -ErrorAction SilentlyContinue |
            Select-String -Pattern 'Return value 3' -Context 30, 2 |
            Select-Object -First 1
        if ($failure) {
            @($failure.Context.PreContext) + $failure.Line + @($failure.Context.PostContext)
        } else {
            # No failing action (1618, 1619, 1620, ...): the exit code is the
            # story, but show what little the log has.
            Get-Content -Path $psqlOdbcLog -Tail 20 -ErrorAction SilentlyContinue
        }
        throw "psqlodbc MSI install exited $($proc.ExitCode); log: $psqlOdbcLog"
    }
    Remove-Item $psqlOdbcMsi -Force -ErrorAction SilentlyContinue
}

# SQLite ODBC driver: the author's 64-bit NSIS installer, run unattended. `/S`
# is NSIS's standard silent switch; the author's page documents the installer
# as NSIS but does not document the switch, so the verification block below is
# what actually confirms the install worked.
$sqliteOdbcUrl = 'https://ch-werner.hier-im-netz.de/sqliteodbc/sqliteodbc_w64.exe'
$sqliteOdbcExe = Join-Path $env:TEMP 'sqliteodbc_w64.exe'
# There is no versioned filename for this file upstream (unlike the Linux
# tarball's sqliteodbc-0.99991.tar.gz), so this hash IS the version pin:
# computed from a fresh download at the time this was written, re-verify and
# update together if the author ever ships a new build under the same name.
$sqliteOdbcSha256 = 'A4804E4F54F42C721DF1323C5FCAC101A8C7A577E7F20979227324CEAB572D51'

Write-Output "[db-deps] downloading SQLite ODBC driver from $sqliteOdbcUrl"
Save-VerifiedDownload -Url $sqliteOdbcUrl -OutFile $sqliteOdbcExe -Sha256 $sqliteOdbcSha256

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
    # The 64-bit name the tests select (windowsInstalledDriverPattern in
    # utils/db-targets.ts); '^PostgreSQL Unicode' alone would also accept
    # the suffixless alias.
    'PostgreSQL' = '^PostgreSQL Unicode\('
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
    # Write-Output, not Write-Error: under $ErrorActionPreference = 'Stop'
    # (set at the top of this script), Write-Error is a terminating error, so
    # the FIRST call would abort the script right there and the actual
    # $missing list below it would never print -- only the header line would
    # ever reach the log. Confirmed by reproducing it directly. exit 1 below
    # is what makes this failure real; these lines are just the message.
    Write-Output 'ERROR: database stack incomplete after install; missing:'
    $missing | ForEach-Object { Write-Output "  $_" }
    exit 1
}

Write-Output '[db-deps] Windows database stack ready'
exit 0
