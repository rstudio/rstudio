#!/usr/bin/env bash
#
# Throwaway MySQL server for the Connections pane tests (macOS).
#
# Contract (identical for every engine/OS script under scripts/db/):
#   start    <dataDir>   initialize and start a server on PW_DBP_PORT with role
#                        PW_DBP_USER / password PW_DBP_PASSWORD and database
#                        PW_DBP_DATABASE granted to that role
#   stop     <dataDir>   stop the server; the caller deletes the files
#   sessions <dataDir>   print the number of client connections to
#                        PW_DBP_DATABASE, excluding the probe's own
#
# The four PW_DBP_* environment variables are supplied by the dispatcher
# (utils/db-provision.ts) from the target descriptor. Tests seed their own
# tables through DBI.
#
# Everything under test uses TCP. mysqld cannot run without a Unix socket
# and the sandbox path exceeds the socket-path length limit, so the socket
# lands at a short fixed /tmp path keyed by port (one server per port, so
# no collisions).

set -euo pipefail

usage() { echo "usage: $0 start|stop|sessions <dataDir>" >&2; exit 2; }

[ $# -eq 2 ] || usage
ACTION=$1
DATADIR=$2

MYSQLDATA="$DATADIR/mysqldata"
ERRLOG="$DATADIR/mysql.err.log"

find_mysql_bin() {
  # Prefer PATH; fall back to the Homebrew mysql keg (both prefixes).
  if command -v mysqld >/dev/null 2>&1; then
    dirname "$(command -v mysqld)"
    return
  fi
  local prefix
  for prefix in /opt/homebrew /usr/local; do
    if [ -x "$prefix/opt/mysql/bin/mysqld" ]; then
      echo "$prefix/opt/mysql/bin"
      return
    fi
  done
  echo "ERROR: no MySQL binaries found (brew install mysql)" >&2
  exit 1
}

BIN=$(find_mysql_bin)

case "$ACTION" in
  stop)
    if [ -d "$MYSQLDATA" ] && [ -n "${PW_DBP_PORT:-}" ]; then
      "$BIN/mysqladmin" --protocol=tcp -h 127.0.0.1 -P "$PW_DBP_PORT" -u root shutdown
      # mysqladmin returns once the server ACCEPTS the shutdown, not once it
      # has exited. The caller deletes this directory immediately after, so
      # returning early leaves mysqld still writing into it and the delete
      # fails with ENOTEMPTY -- stranding the whole sandbox on disk. Wait for
      # the pid file to disappear, which mysqld removes on clean exit.
      for _ in $(seq 1 60); do
        if ! ls "$MYSQLDATA"/*.pid >/dev/null 2>&1; then
          exit 0
        fi
        sleep 0.5
      done
      echo "WARNING: mysqld still running 30s after shutdown; data dir may not delete" >&2
    fi
    exit 0
    ;;
  sessions)
    # Threads still attached to the test database, minus this probe's own.
    # The caller runs this after the IDE has been shut down, so anything
    # left is a session the tests orphaned -- e.g. an R restart while a DBI
    # connection was still open.
    if [ ! -d "$MYSQLDATA" ] || [ -z "${PW_DBP_PORT:-}" ]; then
      echo 0
      exit 0
    fi
    "$BIN/mysql" --protocol=tcp -h 127.0.0.1 -P "$PW_DBP_PORT" -u root -N -B \
      -e "SELECT COUNT(*) FROM information_schema.processlist
          WHERE db = '${PW_DBP_DATABASE:?PW_DBP_DATABASE not set}'
            AND id <> CONNECTION_ID();"
    exit 0
    ;;
  start)
    ;;
  *)
    usage
    ;;
esac

: "${PW_DBP_PORT:?PW_DBP_PORT not set}"
: "${PW_DBP_DATABASE:?PW_DBP_DATABASE not set}"
: "${PW_DBP_USER:?PW_DBP_USER not set}"
: "${PW_DBP_PASSWORD:?PW_DBP_PASSWORD not set}"

SOCKET="/tmp/pwtest-mysql-$PW_DBP_PORT.sock"

if [ -d "$MYSQLDATA" ]; then
  echo "ERROR: $MYSQLDATA already exists; refusing to reinitialize" >&2
  exit 1
fi
mkdir -p "$DATADIR"

# --initialize-insecure leaves root@localhost passwordless; that account is
# only used by this script's own bootstrap below and by stop. The role the
# tests authenticate with gets a real password (caching_sha2, the default).
"$BIN/mysqld" --initialize-insecure --datadir="$MYSQLDATA" --log-error="$ERRLOG"

if ! "$BIN/mysqld" --datadir="$MYSQLDATA" --port="$PW_DBP_PORT" \
  --bind-address=127.0.0.1 --socket="$SOCKET" --log-error="$ERRLOG" --daemonize; then
  echo "ERROR: mysqld failed to start; log follows:" >&2
  cat "$ERRLOG" >&2
  exit 1
fi

CLIENT=("$BIN/mysql" --protocol=tcp -h 127.0.0.1 -P "$PW_DBP_PORT" -u root)
"${CLIENT[@]}" -e "CREATE USER '$PW_DBP_USER'@'%' IDENTIFIED BY '$PW_DBP_PASSWORD';"
"${CLIENT[@]}" -e "CREATE DATABASE $PW_DBP_DATABASE;"
"${CLIENT[@]}" -e "GRANT ALL PRIVILEGES ON $PW_DBP_DATABASE.* TO '$PW_DBP_USER'@'%';"

echo "mysql ready on 127.0.0.1:$PW_DBP_PORT (data: $MYSQLDATA)"
