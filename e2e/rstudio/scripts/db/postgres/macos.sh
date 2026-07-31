#!/usr/bin/env bash
#
# Throwaway PostgreSQL server for the Connections pane tests (macOS).
#
# Contract (identical for every engine/OS script under scripts/db/):
#   start <dataDir>   initialize and start a server on PW_DBP_PORT with role
#                     PW_DBP_USER / password PW_DBP_PASSWORD and database
#                     PW_DBP_DATABASE owned by that role
#   stop  <dataDir>   stop the server; the caller deletes the files
#
# The four PW_DBP_* environment variables are supplied by the dispatcher
# (utils/db-provision.ts) from the target descriptor, the single source of
# truth. Schemas, tables, and rows are NOT created here: the tests seed
# their own objects through DBI, so this script stays engine-setup only.
#
# Uses the PostgreSQL binaries from Homebrew (any installed version). The
# server is TCP-only: Unix-domain sockets are disabled because the sandbox
# path routinely exceeds the 103-byte socket-path limit, and TCP is the path
# the ODBC driver takes anyway.

set -euo pipefail

usage() { echo "usage: $0 start|stop <dataDir>" >&2; exit 2; }

[ $# -eq 2 ] || usage
ACTION=$1
DATADIR=$2

PGDATA="$DATADIR/pgdata"
PGLOG="$DATADIR/pg.log"

find_pgbin() {
  # Prefer PATH; fall back to any Homebrew postgresql@N keg (both prefixes).
  if command -v initdb >/dev/null 2>&1; then
    dirname "$(command -v initdb)"
    return
  fi
  local prefix dir
  for prefix in /opt/homebrew /usr/local; do
    for dir in "$prefix"/opt/postgresql@*/bin; do
      if [ -x "$dir/initdb" ]; then
        echo "$dir"
        return
      fi
    done
  done
  echo "ERROR: no PostgreSQL binaries found (brew install postgresql@14)" >&2
  exit 1
}

PGBIN=$(find_pgbin)

case "$ACTION" in
  stop)
    if [ -d "$PGDATA" ]; then
      "$PGBIN/pg_ctl" -D "$PGDATA" -m fast -w stop
    fi
    exit 0
    ;;
  start)
    ;;
  *)
    usage
    ;;
esac

# The connection parameters are only needed to create the cluster; stop gets
# by on the data directory alone.
: "${PW_DBP_PORT:?PW_DBP_PORT not set}"
: "${PW_DBP_DATABASE:?PW_DBP_DATABASE not set}"
: "${PW_DBP_USER:?PW_DBP_USER not set}"
: "${PW_DBP_PASSWORD:?PW_DBP_PASSWORD not set}"

if [ -d "$PGDATA" ]; then
  echo "ERROR: $PGDATA already exists; refusing to reinitialize" >&2
  exit 1
fi
mkdir -p "$DATADIR"

# All connections are TCP and require a password (scram). The superuser
# password is only used by this script's own psql calls below.
SUPERPW="$DATADIR/.superpw"
printf '%s\n' "$PW_DBP_PASSWORD" > "$SUPERPW"
"$PGBIN/initdb" -D "$PGDATA" -U postgres \
  --auth-host=scram-sha-256 --pwfile="$SUPERPW" \
  --encoding=UTF8 --locale=C > "$PGLOG" 2>&1
rm -f "$SUPERPW"

{
  echo "port = $PW_DBP_PORT"
  echo "listen_addresses = '127.0.0.1'"
  echo "unix_socket_directories = ''"
  # Throwaway data: favor speed over durability.
  echo "fsync = off"
  echo "full_page_writes = off"
} >> "$PGDATA/postgresql.conf"

# pg_ctl -w probes readiness over a connection; with no socket it must be
# told to probe TCP.
export PGHOST=127.0.0.1
export PGPORT="$PW_DBP_PORT"
export PGPASSWORD="$PW_DBP_PASSWORD"

if ! "$PGBIN/pg_ctl" -D "$PGDATA" -l "$PGLOG" -w -t 60 start >> "$PGLOG" 2>&1; then
  echo "ERROR: postgres failed to start; log follows:" >&2
  cat "$PGLOG" >&2
  exit 1
fi

PSQL=("$PGBIN/psql" -v ON_ERROR_STOP=1 -h 127.0.0.1 -p "$PW_DBP_PORT" -U postgres -d postgres -q)
"${PSQL[@]}" -c "CREATE ROLE $PW_DBP_USER LOGIN PASSWORD '$PW_DBP_PASSWORD';"
"${PSQL[@]}" -c "CREATE DATABASE $PW_DBP_DATABASE OWNER $PW_DBP_USER;"

"$PGBIN/pg_isready" -h 127.0.0.1 -p "$PW_DBP_PORT"
echo "postgres ready on 127.0.0.1:$PW_DBP_PORT (data: $PGDATA)"
