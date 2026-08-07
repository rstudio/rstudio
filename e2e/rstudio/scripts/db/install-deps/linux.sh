#!/usr/bin/env bash
#
# Install the database stack the Connections pane tests need (Linux).
#
# Called from .github/actions/os-e2e-deps when its install-database-stack input
# is set. It runs before that action installs R packages, and the order matters
# on more than one distro: the R `odbc` package needs the unixODBC headers to
# build, and Fedora has no PPM binaries so it compiles from source every time.
#
# One script, three package families, because that is the real unit of
# variation -- nine distro configs share three sets of package names. The same
# family switch on /etc/os-release that os-e2e-deps already uses twice
# internally (PPM repo selection, Playwright browser deps).
#
#   Debian/Ubuntu   unixodbc  odbc-postgresql  libsqliteodbc  [postgresql]
#   Fedora          unixODBC  unixODBC-devel  postgresql-odbc  sqliteodbc
#                   [postgresql-server]
#   RHEL/Rocky      unixODBC  unixODBC-devel  postgresql-odbc  sqlite-devel
#                   gcc make tar  [postgresql-server]
#                   plus sqliteodbc compiled from source, see below
#
# The PostgreSQL server is in brackets because it is installed only when its
# binaries are missing: the GitHub ubuntu-24.04 runner already ships
# PostgreSQL 16 (service disabled, which is all we need since the suite starts
# its own throwaway cluster), while the container engines start from a bare
# image and do need it.
#
# RHEL and Rocky have no packaged sqliteodbc -- not in the base repos and not
# in official EPEL either -- so it is compiled from the author's own tarball
# there (build_sqliteodbc below). That keeps all three families on the same two
# targets and the same driver version, which is what makes results across the
# nine distro configs comparable.
#
# Only CI runs this. A developer's machine is expected to have the stack
# installed by hand, and the suite deliberately does not mutate it.
#
# Idempotent, so re-running on a warm runner is cheap.

set -euo pipefail

if [ ! -r /etc/os-release ]; then
  echo "ERROR: no /etc/os-release; cannot determine the package family" >&2
  exit 1
fi
# shellcheck disable=SC1091
. /etc/os-release

# sudo is present on the bare runners and absent in most container images,
# where the setup steps already run as root. Resolve once rather than
# branching at every call.
if [ "$(id -u)" = "0" ]; then
  SUDO=""
elif command -v sudo >/dev/null 2>&1; then
  SUDO="sudo"
else
  echo "ERROR: not root and no sudo available; cannot install packages" >&2
  exit 1
fi

family=""
case "${ID:-}" in
  debian | ubuntu) family="debian" ;;
  fedora) family="fedora" ;;
  rhel | rocky | almalinux | centos) family="rhel" ;;
  *)
    if printf '%s' "${ID_LIKE:-}" | grep -q 'debian'; then
      family="debian"
    elif printf '%s' "${ID_LIKE:-}" | grep -q 'rhel\|fedora'; then
      family="rhel"
    fi
    ;;
esac
if [ -z "$family" ]; then
  echo "ERROR: unsupported distro ID='${ID:-}' ID_LIKE='${ID_LIKE:-}'" >&2
  exit 1
fi
echo "[db-deps] distro ${ID:-?} ${VERSION_ID:-?}, package family: $family"

# Does a PostgreSQL server already exist? Mirrors find_pgbin in
# scripts/db/postgres/linux.sh, so the two cannot disagree about what counts.
have_postgres() {
  if command -v initdb >/dev/null 2>&1; then
    return 0
  fi
  # Globs, not ls: an unmatched glob stays literal and the -x test fails for
  # it. Written as an explicit if so a failing test cannot trip set -e as the
  # last command in the loop body.
  local dir
  for dir in /usr/lib/postgresql/*/bin /usr/pgsql-*/bin; do
    if [ -x "$dir/initdb" ]; then
      return 0
    fi
  done
  return 1
}

# Build the SQLite ODBC driver from upstream source, for the RHEL family only.
#
# There is no packaged sqliteodbc for RHEL or Rocky: not in the base repos, and
# not in official EPEL either (checked 2026-08-05 -- Fedora Packages lists only
# Fedora branches, and the one EPEL 9 build is an unofficial COPR with nothing
# for EPEL 10). Building from the author's own tarball keeps this on official
# upstream code at the same version every other platform uses, rather than
# depending on one person's side repository, and every build input
# (unixODBC-devel, sqlite-devel, gcc, make) is in Rocky's own repos.
#
# Same source and version as the Windows installer this suite downloads, so
# there is no additional host to trust.
#
# --prefix=/usr and --libdir chosen deliberately: the default /usr/local/lib is
# not among the driverLibraries candidates in utils/db-targets.ts, and unixODBC
# would not find it there either. Installing into the distro library directory
# keeps one set of candidate paths working across the whole family.
SQLITEODBC_VERSION="0.99991"
SQLITEODBC_URL="https://ch-werner.hier-im-netz.de/sqliteodbc/sqliteodbc-${SQLITEODBC_VERSION}.tar.gz"

build_sqliteodbc() {
  if [ -f /usr/lib64/libsqlite3odbc.so ]; then
    echo "[db-deps] sqliteodbc already built"
    return 0
  fi
  echo "[db-deps] building sqliteodbc ${SQLITEODBC_VERSION} from source"
  local workdir
  workdir=$(mktemp -d)
  # The build tree is throwaway; remove it however this function exits.
  trap 'rm -rf "$workdir"' RETURN

  curl -fsSL --retry 3 --retry-delay 5 -o "$workdir/sqliteodbc.tar.gz" "$SQLITEODBC_URL"
  tar -xzf "$workdir/sqliteodbc.tar.gz" -C "$workdir"

  local src="$workdir/sqliteodbc-${SQLITEODBC_VERSION}"
  if [ ! -d "$src" ]; then
    echo "ERROR: unexpected archive layout; no $src" >&2
    return 1
  fi

  # --build is needed on arm64: this configure script's bundled config.guess
  # (dated 2003) doesn't recognize aarch64 and aborts with "cannot guess
  # build type" (seen on rocky-10-arm64). Passing --build skips that check;
  # its equally old config.sub still accepts an explicit aarch64 triplet
  # (verified locally), so nothing needs patching.
  #
  # --without-sqlite2 was dropped: it isn't a real option here (checked
  # --help). SQLite 2 support only activates via --with-sqlite=DIR, which
  # is never passed.
  #
  # CFLAGS demote GCC 14+'s default-to-error strictness back to the older
  # behavior. This ~2010s-era C code assigns/calls through old-style,
  # unprototyped function pointers (e.g. `int (*gpps)()` called with 6 real
  # arguments) and mismatched function-pointer types -- both are exactly the
  # class of thing GCC 14 turned from warnings into hard errors by default
  # (per its own release notes), which is what failed the build on Fedora 44
  # (Rocky's older GCC compiles the identical source with no flags at all, so
  # this is purely a toolchain-strictness gap, not a real code fix). -std=gnu17
  # opts back into the pre-C23 dialect these warnings are keyed to; the
  # individual -Wno-error= flags cover what -std alone does not demote.
  local cflags="-std=gnu17 -Wno-error=implicit-function-declaration"
  cflags="$cflags -Wno-error=implicit-int -Wno-error=incompatible-pointer-types"
  cflags="$cflags -Wno-error=int-conversion"
  (
    cd "$src"
    export CFLAGS="$cflags"
    ./configure --prefix=/usr --libdir=/usr/lib64 "--build=$(uname -m)-unknown-linux-gnu" >/dev/null
    make >/dev/null
  )
  $SUDO make -C "$src" install >/dev/null

  if [ ! -f /usr/lib64/libsqlite3odbc.so ]; then
    echo "ERROR: sqliteodbc build completed but /usr/lib64/libsqlite3odbc.so is absent" >&2
    return 1
  fi
  echo "[db-deps] sqliteodbc built and installed to /usr/lib64"
}

# Driver libraries the tests look for, per family. These must agree with the
# driverLibraries candidates in utils/db-targets.ts; a mismatch is exactly how
# a green run that tested nothing happens, which is what the check at the end
# exists to prevent. Populated per family below.
expect_postgres_driver=""
expect_sqlite_driver=""

case "$family" in
  debian)
    packages="unixodbc odbc-postgresql libsqliteodbc"
    have_postgres || packages="$packages postgresql"
    echo "[db-deps] apt-get install: $packages"
    $SUDO apt-get update
    # shellcheck disable=SC2086
    DEBIAN_FRONTEND=noninteractive $SUDO apt-get install -y $packages
    expect_postgres_driver="psqlodbcw.so"
    expect_sqlite_driver="libsqlite3odbc.so"
    ;;
  fedora)
    # unixODBC-devel because Fedora has no PPM binaries: the R odbc package
    # compiles from source and needs the headers, not just the runtime.
    #
    # TEMPORARY DIAGNOSTIC (2026-08-07): Fedora's own dnf-packaged sqliteodbc
    # (sqliteodbc-0.99991-8.fc44) crashes R with a fatal, uncatchable error
    # inside odbc::odbcListObjectTypes() -- confirmed via a breadcrumb probe
    # that isolated the exact call, on tests/panes/connections/, run
    # 31203160622. Building from the same upstream source RHEL/Rocky already
    # use (see build_sqliteodbc below) tests whether that is a defect in
    # Fedora's specific build, or in the upstream driver code regardless of
    # how it's compiled. Revert this to the plain `dnf install sqliteodbc`
    # once that question is answered, unless the source build turns out to be
    # the fix Fedora needs going forward.
    packages="unixODBC unixODBC-devel postgresql-odbc sqlite-devel gcc make tar"
    have_postgres || packages="$packages postgresql-server"
    echo "[db-deps] dnf install: $packages"
    # shellcheck disable=SC2086
    $SUDO dnf install -y $packages
    build_sqliteodbc
    expect_postgres_driver="psqlodbcw.so"
    expect_sqlite_driver="libsqlite3odbc.so"
    ;;
  rhel)
    # No packaged sqliteodbc here, so it is built from upstream source (see
    # build_sqliteodbc below). sqlite-devel and the toolchain are the build
    # inputs; everything else matches Fedora.
    packages="unixODBC unixODBC-devel postgresql-odbc sqlite-devel gcc make tar"
    have_postgres || packages="$packages postgresql-server"
    echo "[db-deps] dnf install: $packages"
    # shellcheck disable=SC2086
    $SUDO dnf install -y $packages
    build_sqliteodbc
    expect_postgres_driver="psqlodbcw.so"
    expect_sqlite_driver="libsqlite3odbc.so"
    ;;
esac

# Verify what this script set out to install, and fail here if it is absent.
# Deliberately not a search for "any driver": only the ones this family was
# supposed to provide, so an expected-absent SQLite driver on RHEL does not
# fail the run while a missing PostgreSQL driver does.
find_driver_lib() {
  local name=$1 dir
  for dir in /usr/lib/x86_64-linux-gnu/odbc /usr/lib/aarch64-linux-gnu/odbc \
             /usr/lib64 /usr/lib64/odbc /usr/lib /usr/lib/odbc; do
    if [ -f "$dir/$name" ]; then
      echo "$dir/$name"
      return 0
    fi
  done
  return 1
}

missing=()

if ! have_postgres; then
  missing+=("PostgreSQL server binaries (initdb)")
fi

if [ -n "$expect_postgres_driver" ]; then
  if lib=$(find_driver_lib "$expect_postgres_driver"); then
    echo "[db-deps] postgres driver: $lib"
  else
    missing+=("$expect_postgres_driver (PostgreSQL ODBC driver)")
  fi
fi

if [ -n "$expect_sqlite_driver" ]; then
  if lib=$(find_driver_lib "$expect_sqlite_driver"); then
    echo "[db-deps] sqlite driver: $lib"
  else
    missing+=("$expect_sqlite_driver (SQLite ODBC driver)")
  fi
fi

if [ ${#missing[@]} -gt 0 ]; then
  echo "ERROR: database stack incomplete after install; missing:" >&2
  printf '  %s\n' "${missing[@]}" >&2
  exit 1
fi

echo "[db-deps] Linux database stack ready ($family)"
