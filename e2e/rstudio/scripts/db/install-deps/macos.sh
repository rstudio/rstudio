#!/usr/bin/env bash
#
# Install the database stack the Connections pane tests need (macOS).
#
# Called from .github/actions/os-e2e-deps when its install-database-stack
# input is set. It runs before that action installs R packages, and the order
# matters: the R `odbc` package (listed in required-packages.txt) needs
# unixODBC present, so installing it afterwards gives a confusing package
# build failure instead of a clear missing-driver skip.
#
# What each piece is for:
#   unixodbc        the ODBC driver manager. Both the sandbox-local ODBC
#                   configuration the suite builds (ODBCSYSINI) and the R
#                   odbc package depend on it.
#   psqlodbc        PostgreSQL ODBC driver, installs psqlodbcw.so
#   sqliteodbc      SQLite ODBC driver, installs libsqlite3odbc.dylib
#   postgresql@17   the server that scripts/db/postgres/macos.sh starts
#
# SQLite needs no server at all: its driver opens a file directly, so the
# driver is the whole requirement for that target.
#
# Only CI runs this. A developer's machine is expected to have the stack
# installed by hand, and the suite deliberately does not mutate it. A missing
# driver is not fatal anywhere: resolveDriverLibrary returns null, the target
# goes unregistered, and the connections specs skip with a reason that names
# the problem.
#
# Idempotent, so re-running on a warm runner is cheap.

set -euo pipefail

# Auto-update turns a 30-second bottle install into several minutes of
# unrelated formula churn, and cleanup discards bottles a later run could
# reuse. Neither helps a single-purpose CI install.
export HOMEBREW_NO_AUTO_UPDATE=1
export HOMEBREW_NO_INSTALL_CLEANUP=1
export HOMEBREW_NO_ENV_HINTS=1

# MySQL is currently deactivated in utils/db-targets.ts (dropped from
# ALL_DB_TARGETS, descriptor kept). Its formulae stay listed but commented so
# re-enabling here and there is one symmetric change in each file.
FORMULAE=(
  unixodbc
  psqlodbc
  sqliteodbc
  postgresql@17
  # mysql
  # mariadb-connector-odbc
)

for formula in "${FORMULAE[@]}"; do
  if brew list --versions "$formula" >/dev/null 2>&1; then
    echo "[db-deps] $formula already installed"
  else
    echo "[db-deps] installing $formula"
    brew install "$formula"
  fi
done

# Verify the install produced what the tests actually look for, and fail here
# if it did not. Without this a renamed formula or a relocated library would
# surface much later as a suite that skips every connections test and reports
# green, which is the failure mode this whole step exists to prevent.
#
# The library paths must agree with the driverLibraries candidates in
# utils/db-targets.ts. brew --prefix covers both /opt/homebrew (Apple
# Silicon) and /usr/local (the Intel runner).
PREFIX=$(brew --prefix)
missing=()

for lib in "$PREFIX/lib/psqlodbcw.so" "$PREFIX/lib/libsqlite3odbc.dylib"; do
  [ -f "$lib" ] || missing+=("$lib")
done

# find_pgbin in scripts/db/postgres/macos.sh checks PATH first, then any
# postgresql@N keg. postgresql@17 is keg-only, so on a clean runner it is the
# keg path that resolves; probe it the same way rather than assuming PATH.
pg_found=false
if command -v initdb >/dev/null 2>&1; then
  pg_found=true
else
  for bindir in "$PREFIX"/opt/postgresql@*/bin; do
    if [ -x "$bindir/initdb" ]; then
      pg_found=true
      break
    fi
  done
fi
if [ "$pg_found" = false ]; then
  missing+=("initdb (no PostgreSQL server binaries on PATH or in a keg)")
fi

if [ ${#missing[@]} -gt 0 ]; then
  echo "ERROR: database stack incomplete after install; missing:" >&2
  printf '  %s\n' "${missing[@]}" >&2
  exit 1
fi

echo "[db-deps] macOS database stack ready (prefix: $PREFIX)"
