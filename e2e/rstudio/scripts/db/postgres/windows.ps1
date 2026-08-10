<#
Throwaway PostgreSQL server for the Connections pane tests (Windows).

Contract (identical for every engine/OS script under scripts/db/):
  start    <dataDir>   initialize and start a server on PW_DBP_PORT with role
                       PW_DBP_USER / password PW_DBP_PASSWORD and database
                       PW_DBP_DATABASE owned by that role
  stop     <dataDir>   stop the server; the caller deletes the files
  sessions <dataDir>   print the number of client connections to
                       PW_DBP_DATABASE, excluding the probe's own

The four PW_DBP_* variables are supplied by the dispatcher
(utils/db-provision.ts) from the target descriptor, the single source of
truth. Schemas, tables, and rows are NOT created here: the tests seed their
own objects through DBI, so this script stays engine-setup only.

Differences from the macOS sibling, all Windows-specific:

  * No unix_socket_directories setting. Windows has no Unix sockets, so the
    server is TCP-only without having to ask.
  * The server is started through pg_ctl, never postgres.exe directly.
    postgres.exe refuses to run under an account with administrator
    privileges, which is exactly what a CI runner user has; pg_ctl exists
    partly to handle that, creating a restricted token for the child.
  * Binaries are found via PATH, then PGBIN, then any versioned install under
    Program Files. On the GitHub windows-2025 image PGBIN is already set and a
    full PostgreSQL 17 server is present (stopped and disabled), so nothing
    needs installing -- this script starts its own throwaway cluster and
    leaves that service alone.
#>

param(
    [Parameter(Mandatory = $true, Position = 0)]
    [ValidateSet('start', 'stop', 'sessions')]
    [string] $Action,

    [Parameter(Mandatory = $true, Position = 1)]
    [string] $DataDir
)

$ErrorActionPreference = 'Stop'

$PgData = Join-Path $DataDir 'pgdata'
$PgLog = Join-Path $DataDir 'pg.log'

function Get-PgBin {
    $onPath = Get-Command initdb.exe -ErrorAction SilentlyContinue
    if ($onPath) { return (Split-Path -Parent $onPath.Source) }

    if ($env:PGBIN -and (Test-Path (Join-Path $env:PGBIN 'initdb.exe'))) {
        return $env:PGBIN
    }

    # Built by string concatenation and probed through .NET rather than
    # Join-Path / Test-Path: both of those resolve the drive and throw when it
    # does not exist, whereas Directory.Exists simply returns false. That keeps
    # this a plain lookup with no exception control flow, and leaves the throw
    # below as the single place this function reports failure.
    $programFiles = if ($env:ProgramFiles) { $env:ProgramFiles } else { 'C:\Program Files' }
    $root = "$programFiles\PostgreSQL"
    if ([System.IO.Directory]::Exists($root)) {
        # Highest version first, so a machine with several installs uses the
        # newest. Names are version numbers ("17"), hence the numeric sort;
        # anything unparseable sorts last rather than throwing.
        $dirs = Get-ChildItem -Path $root -Directory -ErrorAction SilentlyContinue |
            Sort-Object -Property @{ Expression = {
                if ($_.Name -match '^(\d+)') { [int]$Matches[1] } else { 0 }
            } } -Descending
        foreach ($dir in $dirs) {
            $bin = Join-Path $dir.FullName 'bin'
            if (Test-Path (Join-Path $bin 'initdb.exe')) { return $bin }
        }
    }

    throw 'no PostgreSQL binaries found (set PGBIN, or choco install postgresql)'
}

# $ErrorActionPreference does not apply to native executables: they report
# failure through an exit code, which PowerShell is happy to ignore. Every
# call below goes through this so a failure stops the script the way `set -e`
# would in the macOS sibling.
function Invoke-Native {
    param(
        [string] $Exe,
        [string[]] $Arguments,
        [string] $What,
        [switch] $Quiet
    )
    if ($Quiet) {
        & $Exe @Arguments | Out-Null
    } else {
        & $Exe @Arguments
    }
    if ($LASTEXITCODE -ne 0) {
        throw "$What failed (exit $LASTEXITCODE)"
    }
}

$PgBin = Get-PgBin
$Initdb = Join-Path $PgBin 'initdb.exe'
$PgCtl = Join-Path $PgBin 'pg_ctl.exe'
$Psql = Join-Path $PgBin 'psql.exe'
$PgIsReady = Join-Path $PgBin 'pg_isready.exe'

switch ($Action) {
    'stop' {
        if (Test-Path $PgData) {
            Invoke-Native -Exe $PgCtl -Arguments @('-D', $PgData, '-m', 'fast', '-w', 'stop') -What 'pg_ctl stop'
        }
        exit 0
    }

    'sessions' {
        # Backends still attached to the test database, minus this probe's own.
        # The caller runs this after the IDE has been shut down, so anything
        # left is a session the tests orphaned -- e.g. an R restart while a DBI
        # connection was still open.
        if (-not (Test-Path $PgData)) {
            Write-Output 0
            exit 0
        }
        if (-not $env:PW_DBP_PASSWORD) { throw 'PW_DBP_PASSWORD not set' }
        if (-not $env:PW_DBP_PORT) { throw 'PW_DBP_PORT not set' }
        if (-not $env:PW_DBP_DATABASE) { throw 'PW_DBP_DATABASE not set' }

        $env:PGPASSWORD = $env:PW_DBP_PASSWORD
        $sql = @"
SELECT count(*) FROM pg_stat_activity
 WHERE datname = '$($env:PW_DBP_DATABASE)'
   AND pid <> pg_backend_pid();
"@
        $out = & $Psql -At -h 127.0.0.1 -p $env:PW_DBP_PORT -U postgres -d postgres -c $sql
        if ($LASTEXITCODE -ne 0) { exit 1 }
        # Only the count reaches stdout: the caller parses this as an integer
        # and treats anything else as "could not answer".
        Write-Output ($out | Select-Object -Last 1).Trim()
        exit 0
    }
}

# start

foreach ($name in 'PW_DBP_PORT', 'PW_DBP_DATABASE', 'PW_DBP_USER', 'PW_DBP_PASSWORD') {
    if (-not (Get-Item "env:$name" -ErrorAction SilentlyContinue).Value) {
        throw "$name not set"
    }
}

$port = $env:PW_DBP_PORT
$database = $env:PW_DBP_DATABASE
$user = $env:PW_DBP_USER
$password = $env:PW_DBP_PASSWORD

if (Test-Path $PgData) {
    throw "$PgData already exists; refusing to reinitialize"
}
New-Item -ItemType Directory -Path $DataDir -Force | Out-Null

# All connections are TCP and require a password (scram). The superuser
# password is only used by this script's own psql calls below.
#
# Written with an explicit LF and no byte-order mark: initdb reads the first
# line of this file verbatim, so PowerShell's default CRLF (and, in 5.1, a BOM
# on utf8) would land inside the password.
$superPw = Join-Path $DataDir '.superpw'
$noBom = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText($superPw, "$password`n", $noBom)
try {
    Invoke-Native -Exe $Initdb -What 'initdb' -Quiet -Arguments @(
        '-D', $PgData,
        '-U', 'postgres',
        '--auth-host=scram-sha-256',
        "--pwfile=$superPw",
        '--encoding=UTF8',
        '--locale=C'
    )
} finally {
    Remove-Item $superPw -Force -ErrorAction SilentlyContinue
}

$conf = @"

port = $port
listen_addresses = '127.0.0.1'
# Throwaway data: favor speed over durability.
fsync = off
full_page_writes = off
"@
[System.IO.File]::AppendAllText((Join-Path $PgData 'postgresql.conf'), $conf, $noBom)

$env:PGHOST = '127.0.0.1'
$env:PGPORT = $port
$env:PGPASSWORD = $password

# pg_ctl -w probes readiness over a real connection, and drops the elevated
# token for the server child (see the header note about postgres.exe).
& $PgCtl -D $PgData -l $PgLog -w -t 60 start
if ($LASTEXITCODE -ne 0) {
    # Write-Output, not Write-Error: under $ErrorActionPreference = 'Stop'
    # (set at the top of this script), Write-Error is a terminating error, so
    # the header line would abort the script before the log dump below it
    # ever ran -- the one piece of evidence explaining why postgres failed
    # would never reach the log. Confirmed by reproducing it directly.
    # exit 1 below is what makes this failure real; these lines are just the
    # message.
    Write-Output 'ERROR: postgres failed to start; log follows:'
    if (Test-Path $PgLog) { Get-Content $PgLog | Write-Output }
    exit 1
}

$psqlArgs = @('-v', 'ON_ERROR_STOP=1', '-h', '127.0.0.1', '-p', $port, '-U', 'postgres', '-d', 'postgres', '-q')
Invoke-Native -Exe $Psql -What 'create role' -Quiet `
    -Arguments ($psqlArgs + @('-c', "CREATE ROLE $user LOGIN PASSWORD '$password';"))
Invoke-Native -Exe $Psql -What 'create database' -Quiet `
    -Arguments ($psqlArgs + @('-c', "CREATE DATABASE $database OWNER $user;"))

Invoke-Native -Exe $PgIsReady -What 'pg_isready' -Quiet -Arguments @('-h', '127.0.0.1', '-p', $port)
Write-Output "postgres ready on 127.0.0.1:$port (data: $PgData)"
exit 0
