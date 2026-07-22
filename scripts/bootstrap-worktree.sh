#!/usr/bin/env bash
#
# Bootstrap a git worktree so the JS/TS test suites "just work".
#
# `git worktree add` only materializes tracked files. node_modules is
# gitignored, and a couple of generated files are gitignored too, so a fresh
# .worktrees/<name> checkout starts without them -- tsx/ts-node test runs and
# even `tsc` then fail with "Cannot find module" until deps are installed.
#
# When the worktree's package-lock.json matches the main checkout's, the main
# checkout's node_modules already satisfies it, so we copy-on-write CLONE it
# (APFS clonefile via `cp -c`, or reflink on Linux) instead of running
# `npm ci`. A clone is near-instant, needs no network, and -- crucially for
# src/node/desktop -- skips electron-rebuild and the failing generate.ts
# postinstall. We clone rather than symlink because tsx/ts-node/electron-mocha
# resolve files via realpath: a symlinked node_modules resolves back into the
# main checkout and its loader hook can't intercept the worktree's .ts files. A
# clone's files have their own worktree realpath, so resolution is correct. We
# clone rather than hardlink because clones are copy-on-write -- a write in the
# worktree can't corrupt the parent's node_modules through a shared inode.
#
# Falls back to `npm ci` when the lockfiles differ (the branch changed deps) or
# the main checkout has no node_modules to clone.
#
# Platforms: the clone mechanism is gated by OS -- clonefile on macOS (`cp -c`),
# reflink on Linux (`cp --reflink=auto`). On filesystems without copy-on-write
# (ext4, NTFS) the clone transparently degrades to a full recursive copy:
# correct and still offline, just not instant. Cloning is always from THIS
# machine's main checkout, so the compiled native modules match the platform.
# Run under bash (Git Bash or WSL on Windows).
#
# Alongside the desktop deps, this also materializes a "dev shim" build root
# (<worktree>/build-dev-shim) for running the desktop-dev Playwright suite from
# the worktree without a C++ build. The Electron dev launcher (findComponents
# in src/node/desktop/src/main/utils.ts) honors RSTUDIO_CPP_BUILD_OUTPUT and
# needs <root>/session/rsession.exe plus <root>/conf/rdesktop-dev.conf; the
# shim links session/ from the main checkout's build and rewrites the conf's
# www paths to THIS worktree's GWT output. `npm run test:desktop-dev` detects
# the shim automatically when the worktree has no configured build/ (see
# resolveCppBuildOutput in e2e/rstudio/scripts/dev-common.ts); set
# RSTUDIO_CPP_BUILD_OUTPUT yourself only to override it. Do NOT point
# RSTUDIO_CPP_BUILD_OUTPUT at the main checkout's build/src/cpp directly: its
# conf serves the main checkout's src/gwt/www -- your worktree changes would
# silently not be exercised, and after `ant desktop` / `ant devmode` that
# output is a Super Dev Mode stub that redirects to a code server on
# localhost:9876, so without that code server running RStudio dies at startup
# with "code server not available" and automation times out waiting for
# window.rstudio.ready.
#
# Usage:
#   scripts/bootstrap-worktree.sh [worktree-dir] [--skip-desktop] \
#       [--with-build-dir[=<dir>]]
#
#   worktree-dir   defaults to the current directory; may be any path inside
#                  the target worktree (it is normalized to the repo top).
#   --skip-desktop skip src/node/desktop (its deps are large; skip when you
#                  only need the e2e/rstudio suite). Also skips the dev shim.
#   --with-build-dir[=<dir>]
#                  also configure a CMake build directory for the worktree
#                  (default <worktree>/build; <dir> may be absolute or relative
#                  to the worktree). This configures FRESH -- it never copies
#                  the main checkout's build/, which bakes absolute paths (the
#                  cache's source dir, ninja depfiles, generated dev confs, and
#                  debug/__FILE__ strings in objects), so a copied tree would
#                  build and serve the wrong checkout. A fresh configure writes
#                  correct paths; the cross-checkout CCACHE_BASEDIR setup in
#                  cmake/globals.cmake then lets the compile reuse objects
#                  another checkout already built, keeping it cheap.

set -euo pipefail

SKIP_DESKTOP=0
WANT_BUILD_DIR=0
BUILD_DIR_ARG=""
TARGET=""
for arg in "$@"; do
  case "$arg" in
    --skip-desktop) SKIP_DESKTOP=1 ;;
    --with-build-dir) WANT_BUILD_DIR=1 ;;
    --with-build-dir=*) WANT_BUILD_DIR=1; BUILD_DIR_ARG="${arg#*=}" ;;
    *) TARGET="$arg" ;;
  esac
done

# Normalize to the repo top of the requested (or current) worktree so the
# relative package paths below resolve no matter where this is invoked from.
WORKTREE="$(git -C "${TARGET:-$PWD}" rev-parse --show-toplevel)"

# The primary worktree's .git is a real directory; --git-common-dir points at
# it, so its parent is the main checkout. We clone node_modules and copy
# generated-but-gitignored files from there.
COMMON_DIR="$(git -C "$WORKTREE" rev-parse --path-format=absolute --git-common-dir)"
MAIN_CHECKOUT="$(dirname "$COMMON_DIR")"

echo "[bootstrap] worktree:      $WORKTREE"
echo "[bootstrap] main checkout: $MAIN_CHECKOUT"

# Copy-on-write clone a directory tree (clonefile on macOS/APFS, reflink on
# Linux), falling back to a plain recursive copy where COW is unsupported. The
# destination must not already exist; we clear a partial tree before falling
# back so a failed clone can't nest into the retry.
clone_tree() {
  local src="$1" dst="$2"
  rm -rf "$dst"
  if [ "$(uname -s)" = "Darwin" ]; then
    cp -Rc "$src" "$dst" 2>/dev/null && return 0
  else
    cp -R --reflink=auto "$src" "$dst" 2>/dev/null && return 0
  fi
  rm -rf "$dst"
  cp -R "$src" "$dst"
}

# Locate the pinned node toolchain's bin directory (version from
# dependencies/tools/rstudio-tools.sh): this worktree, then the main checkout,
# then the global tools location -- mirroring pinnedNodeBinDir in
# e2e/rstudio/scripts/dev-common.ts. Prints nothing when not found.
pinned_node_bin() {
  local version
  version="$(sed -n 's/^export RSTUDIO_NODE_VERSION="\(.*\)"$/\1/p' \
    "$WORKTREE/dependencies/tools/rstudio-tools.sh" 2>/dev/null | head -1)"
  if [ -z "$version" ]; then
    return
  fi

  # the '-arm64' dir suffix is a macOS-only convention in this repo
  local suffix=""
  if [ "$(uname -s)" = "Darwin" ] && [ "$(uname -m)" = "arm64" ]; then
    suffix="-arm64"
  fi

  local root
  for root in "$WORKTREE" "$MAIN_CHECKOUT" /opt/rstudio-tools; do
    local bin="$root/dependencies/common/node/$version$suffix/bin"
    if [ -x "$bin/node" ]; then
      echo "$bin"
      return
    fi
  done
}

# True when main's installed node_modules is current relative to main's own
# lockfile. npm records what it installed in node_modules/.package-lock.json;
# if any package version there disagrees with package-lock.json (or a
# non-optional package is missing entirely), the tree is stale -- typically
# npm ci was not rerun in main after a dependency bump -- and cloning it
# would propagate a broken tree that npm ci in the worktree would not.
main_tree_current() {
  local main="$1"
  [ -f "$main/node_modules/.package-lock.json" ] || return 1
  node -e '
    const fs = require("fs");
    const lock = JSON.parse(fs.readFileSync(process.argv[1], "utf8"));
    const installed = JSON.parse(fs.readFileSync(process.argv[2], "utf8"));
    for (const [name, meta] of Object.entries(lock.packages ?? {})) {
      if (!name) continue; // root entry
      const inst = installed.packages?.[name];
      // optional (often platform-specific) packages may be legitimately
      // absent from the installed tree
      if (!inst) {
        if (!meta.optional) process.exit(1);
        continue;
      }
      if (meta.version && inst.version !== meta.version) process.exit(1);
    }
  ' "$main/package-lock.json" "$main/node_modules/.package-lock.json"
}

# Make sure a package dir has node_modules: clone from main when the lockfiles
# match and main's installed tree is current, otherwise npm ci.
ensure_deps() {
  local pkg="$1"
  local wt="$WORKTREE/$pkg"
  local main="$MAIN_CHECKOUT/$pkg"

  if [ ! -f "$wt/package-lock.json" ]; then
    echo "[bootstrap] skip $pkg (no package-lock.json)"
    return
  fi
  if [ -d "$wt/node_modules" ]; then
    echo "[bootstrap] $pkg already has node_modules; leaving as-is"
    return
  fi

  if [ "$WORKTREE" != "$MAIN_CHECKOUT" ] \
     && [ -d "$main/node_modules" ] \
     && cmp -s "$wt/package-lock.json" "$main/package-lock.json"; then
    if main_tree_current "$main"; then
      echo "[bootstrap] cloning $pkg/node_modules from main (lockfiles match)"
      if clone_tree "$main/node_modules" "$wt/node_modules"; then
        return
      fi
      echo "[bootstrap] clone failed; falling back to npm ci"
    else
      echo "[bootstrap] main's $pkg/node_modules is stale relative to its lockfile (rerun npm ci there); falling back to npm ci"
    fi
  fi

  # use the repo's pinned node for npm ci -- a mismatched system node can
  # break the desktop postinstall (native rebuilds, generate.ts)
  local node_bin
  node_bin="$(pinned_node_bin)"
  if [ -n "$node_bin" ]; then
    echo "[bootstrap] npm ci in $pkg (using pinned node from $node_bin)"
    ( cd "$wt" && PATH="$node_bin:$PATH" npm ci )
  else
    echo "[bootstrap] npm ci in $pkg (pinned node not found; using system node)"
    ( cd "$wt" && npm ci )
  fi
}

# Copy a generated, gitignored file from the main checkout when the worktree is
# missing it (and we are not already in the main checkout). Cloning
# node_modules sidesteps the desktop generate.ts postinstall, but this stub
# lives outside node_modules, so copy it explicitly.
copy_generated() {
  local rel="$1"
  if [ "$WORKTREE" = "$MAIN_CHECKOUT" ] || [ -f "$WORKTREE/$rel" ]; then
    return
  fi
  if [ ! -f "$MAIN_CHECKOUT/$rel" ]; then
    echo "[bootstrap] WARNING: $rel missing from main checkout too; build it there first"
    return
  fi
  echo "[bootstrap] copying generated $rel"
  mkdir -p "$(dirname "$WORKTREE/$rel")"
  cp "$MAIN_CHECKOUT/$rel" "$WORKTREE/$rel"
}

# Materialize the desktop-dev shim build root (see header). Best-effort: when
# the main checkout has no built rsession/conf yet, warn and move on -- the
# rest of the bootstrap is still useful. The conf is regenerated on every run
# (the main checkout's conf may have changed); the session link is only
# created when missing.
make_dev_shim() {
  local dir="$WORKTREE/build-dev-shim"
  local main_cpp="$MAIN_CHECKOUT/build/src/cpp"

  if [ "$WORKTREE" = "$MAIN_CHECKOUT" ]; then
    return
  fi
  if [ ! -f "$main_cpp/conf/rdesktop-dev.conf" ] || [ ! -d "$main_cpp/session" ]; then
    echo "[bootstrap] WARNING: no built session/conf under $main_cpp; build the main checkout first -- skipping dev shim"
    return
  fi

  mkdir -p "$dir/conf"

  # session/ must hold the main checkout's built rsession. A symlink is fine
  # everywhere but Windows, where Git Bash's `ln -s` silently degrades to a
  # copy -- use a real NTFS junction there instead. PowerShell rather than
  # `cmd /c mklink`: cmd's /c quote-stripping and MSYS path conversion both
  # mangle the mklink arguments.
  if [ ! -e "$dir/session" ]; then
    case "$(uname -s)" in
      MINGW*|MSYS*|CYGWIN*)
        powershell.exe -NoProfile -Command \
          "New-Item -ItemType Junction -Path '$(cygpath -w "$dir/session")' -Target '$(cygpath -w "$main_cpp/session")' | Out-Null"
        ;;
      *)
        ln -s "$main_cpp/session" "$dir/session"
        ;;
    esac
  fi

  # Rewrite the conf's www paths to THIS worktree's GWT output; everything
  # else (rpostback, staged R modules, dependency paths) stays pointed at the
  # main checkout's build. The conf wants forward slashes even on Windows.
  local wt_fwd="$WORKTREE"
  if command -v cygpath >/dev/null 2>&1; then
    wt_fwd="$(cygpath -m "$WORKTREE")"
  fi
  sed \
    -e "s|^www-local-path=.*|www-local-path=$wt_fwd/src/gwt/www|" \
    -e "s|^www-symbol-maps-path=.*|www-symbol-maps-path=$wt_fwd/src/gwt/extras/rstudio/symbolMaps|" \
    "$main_cpp/conf/rdesktop-dev.conf" > "$dir/conf/rdesktop-dev.conf"

  echo "[bootstrap] dev shim ready: $dir"
  if [ ! -f "$WORKTREE/src/gwt/www/rstudio/rstudio.nocache.js" ]; then
    echo "[bootstrap] NOTE: worktree GWT output missing; build it first: (cd src/gwt && ant draft)"
  fi
  echo "[bootstrap] run desktop-dev e2e from e2e/rstudio with:"
  echo "[bootstrap]   npm run test:desktop-dev -- <spec>"
  echo "[bootstrap] (the wrapper detects this shim when the worktree has no configured build/)"
}

# Configure a CMake build directory for the worktree. Always a fresh configure
# (cmake -S <worktree> -B <dir>), never a copy of the main checkout's build/ --
# a CMake build tree bakes absolute paths into the cache, ninja depfiles, the
# generated dev confs (www-local-path), and the compiled objects themselves, so
# a copied tree would build and serve the main checkout rather than this one. A
# fresh configure writes correct paths; the cross-checkout CCACHE_BASEDIR setup
# in cmake/globals.cmake makes the subsequent compile reuse cached objects, so
# this stays cheap despite not copying.
configure_build() {
  local dir="$1"

  # Resolve a relative path against the worktree root so the build dir lands
  # inside the worktree no matter where this script was invoked from.
  case "$dir" in
    /*) ;;
    *) dir="$WORKTREE/$dir" ;;
  esac

  if ! command -v cmake >/dev/null 2>&1; then
    echo "[bootstrap] WARNING: cmake not found on PATH; skipping --with-build-dir"
    return
  fi

  echo "[bootstrap] configuring CMake build dir: $dir"
  cmake -S "$WORKTREE" -B "$dir" -DCMAKE_EXPORT_COMPILE_COMMANDS=1

  echo "[bootstrap] build dir configured; compile with: cmake --build \"$dir\""
}

# The e2e/rstudio Playwright suite -- the common case.
ensure_deps "e2e/rstudio"

# The Electron desktop suite. Cloning skips its electron-rebuild and the
# failing generate.ts postinstall; we still copy the type stub that postinstall
# would have generated (it lives outside node_modules).
if [ "$SKIP_DESKTOP" -eq 0 ]; then
  ensure_deps "src/node/desktop"
  copy_generated "src/node/desktop/src/types/user-state-schema.d.ts"
  make_dev_shim
else
  echo "[bootstrap] skipping src/node/desktop (--skip-desktop)"
fi

# Optional CMake build-dir configure (default <worktree>/build when no path
# was given).
if [ "$WANT_BUILD_DIR" -eq 1 ]; then
  configure_build "${BUILD_DIR_ARG:-build}"
fi

echo "[bootstrap] done"
