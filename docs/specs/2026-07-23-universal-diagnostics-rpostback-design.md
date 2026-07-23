# Universal `diagnostics` and `rpostback` binaries (macOS)

- Issue: https://github.com/rstudio/rstudio/issues/12572
- Date: 2026-07-23
- Status: Approved design

> Note: this document is process scaffolding for the change. It lives under
> `docs/specs/` (not gitignored) so it is reviewed, but it is removed before
> the change is pushed or a PR is opened.

## Problem

RStudio.app on macOS is shipped as a Universal application, but two
RStudio-authored native binaries under `Contents/Resources/app/bin/` are still
x86_64-only, with no arm64 counterpart:

- `diagnostics`
- `rpostback`

On Apple Silicon this forces those features through Rosetta 2, and newer macOS
Tahoe builds now surface a Rosetta prompt at startup when Intel-only binaries
are present. Tahoe (macOS 26) is the last release to support Intel Macs, and
Rosetta 2 is slated for removal in macOS 28 (2027), so the Intel-only binaries
need to carry a native arm64 slice.

### Findings (confirmed on 2026.08.0-daily+118)

A full-bundle `file` scan confirms `diagnostics` and `rpostback` are the only
RStudio-authored native binaries lacking an arm64 counterpart. Everything else
is either a fat/universal binary (main `RStudio` executable, Electron
framework, `desktop.node`/`dock.node`/`unix_dgram.node`) or ships both
architectures for runtime selection (`rsession`/`rsession-arm64`,
`node`/`node-arm64`, `Frameworks` vs `Frameworks/arm64`, quarto `x86_64` vs
`aarch64`, copilot `darwin/x64` vs `darwin/arm64`).

## Why universal (fat) binaries, not the dual-binary pattern

`rsession` uses the dual-binary + `/usr/bin/arch` approach because it `dlopen`s
`libR` and must match the architecture of the user's R installation. `otool -L`
confirms neither `diagnostics` nor `rpostback` links `libR`; they depend only on
system libraries plus the bundled OpenSSL
(`@executable_path/../Frameworks/libssl.3.dylib`, `libcrypto.3.dylib`). They
have no R-arch constraint and simply need to run natively on either
architecture. A single fat binary satisfies that with no runtime selection
logic, which is why the issue itself calls this "the cleanest" option.

`@electron/universal`'s `makeUniversalApp` only merges the two Electron desktop
`.app` bundles (`desktop-build-x86_64` and `desktop-build-arm64`); it never
touches `Contents/Resources/app/bin/` (the arm64 Electron build does not even
contain a `bin/` directory). The C++ binaries are installed separately by CMake
and stitched together in `prepare-package.cmake` -- which is why `rsession-arm64`
is copied in by hand there. The `@electron/universal` 3.0.4 -> 3.0.6 bump rides
along as maintenance but is not the mechanism for this fix.

## Design

The arm64 build already produces both binaries (`make-package` runs the arm64
build with `--target all`); they are simply not merged into the packaged app.

### Gating: merge only in a universal build

`lipo -create` rejects two inputs of the same architecture, so the merge must
run **only** when the primary build is x86_64 and a *separate* arm64 build was
produced in the same run. File existence alone is not a safe signal:

- In an **arm64-only** build the primary build is arm64 and
  `build-arm64/.../diagnostics` is that same arm64 binary, so
  `lipo -create arm64 arm64` would error.
- In an **x86_64-only incremental** build, `make-package` retains a stale
  `build-arm64` from a previous universal build (it is only removed by
  `--clean`), so an existence check would wrongly merge stale arm64 code.

The fix is an explicit universal-build signal from `make-package`, mirroring the
reviewer guidance: gate on configuration, not file presence.

### 1. `package/osx/make-package`

The primary (x86_64) configure knows whether an arm64 build is also happening
(`build_x86_64` and `build_arm64` are both computed before the x86_64 build).
Pass an explicit flag on that configure:

```sh
universal_build=0
if [ -n "${build_x86_64}" ] && [ -n "${build_arm64}" ]; then
   universal_build=1
fi
```

Add `-DRSTUDIO_UNIVERSAL_BUILD=${universal_build}` to the `arch -x86_64
"${CMAKE}"` configure invocation (the x86_64 primary build that owns the
install/package targets and runs `prepare-package.cmake`).

### 2. `package/osx/CMakeLists.txt`

Expose the arm64 build outputs, mirroring the existing `RSESSION_ARM64_PATH`:

```cmake
set(DIAGNOSTICS_ARM64_PATH
   "${CMAKE_CURRENT_SOURCE_DIR}/build-arm64/src/cpp/diagnostics/diagnostics"
   CACHE INTERNAL "")
set(RPOSTBACK_ARM64_PATH
   "${CMAKE_CURRENT_SOURCE_DIR}/build-arm64/src/cpp/session/postback/rpostback"
   CACHE INTERNAL "")
```

`RSTUDIO_UNIVERSAL_BUILD` arrives as a `-D` cache variable, so it is available
for `@RSTUDIO_UNIVERSAL_BUILD@` substitution in the configured
`prepare-package.cmake` (via `configure_and_install` -> `configure_file(@ONLY)`).
When absent it substitutes to the empty string.

### 3. `package/osx/cmake/prepare-package.cmake`

A new block at the end of the file, after the existing x86_64 library-path fix
(current lines 133-145) so `bin/diagnostics` and `bin/rpostback` already point
at `@executable_path/../Frameworks`. The whole block is guarded by
`if("@RSTUDIO_UNIVERSAL_BUILD@" STREQUAL "1")`. For each tool: a universal build
promises both slices, so a missing x86_64 or arm64 input is a `FATAL_ERROR`
naming the missing path (fail fast rather than silently ship a thin binary that
still needs Rosetta). Otherwise:

1. Copy the arm64 binary into a throwaway staging dir
   (`${CMAKE_INSTALL_PREFIX}/.arm64-lipo-staging`).
2. Run `fix-library-paths.sh <staging> "@executable_path/../Frameworks/arm64"
   <tool>` -- the same fixer used for `rsession-arm64` -- to rewrite its bundled
   OpenSSL references to the arm64 Frameworks directory.
3. `lipo -create <bin/tool (x64, fixed)> <staging/tool (arm64, fixed)> -output
   <temp>` and move the fat result over `bin/tool`.
4. Remove the staging dir.

Each fat binary's x86_64 slice keeps its `../Frameworks/...` load commands and
its arm64 slice keeps `../Frameworks/arm64/...`, so dyld resolves the correct
bundled OpenSSL per architecture at runtime.

### Codesigning

No change to `codesign-package.sh`. It runs after `prepare-package.cmake`, and
`codesign-directory` recursively signs everything under the bundle, including
`bin/diagnostics` and `bin/rpostback`. `lipo` strips signatures; the recursive
re-sign restores them. Only `rsession`/`rsession-arm64` need special
entitlements, so the default-entitlement signing already covers these two.

## Edge cases

Gating on `RSTUDIO_UNIVERSAL_BUILD` gives correct behavior across all three
build modes:

- **Universal** (`--arch=x86_64,arm64`): flag is `1`; primary build is x86_64,
  arm64 outputs exist -> `lipo` produces fat binaries. (`file` reports two
  architectures.)
- **arm64-only** (`--arch=arm64`): flag is `0` -> block skipped; the primary
  arm64 build's `bin/diagnostics` and `bin/rpostback` ship thin arm64. Avoids
  the `lipo` same-architecture error (finding 1).
- **x86_64-only** (`--arch=x86_64`): flag is `0` -> block skipped, even when a
  stale `build-arm64` from an earlier universal build is present. The binaries
  ship thin x86_64, exactly as today (finding 2).

The existing `rsession-arm64` / `node-arm64` merge in the same file keeps its
`if(EXISTS "@RSESSION_ARM64_PATH@")` gating unchanged. It uses `configure_file`
(copy), which tolerates a same-architecture source, so it does not hit the
`lipo` failure that motivates the stricter gate here; retrofitting that
pre-existing block onto the new flag is out of scope for this bugfix.

## Verification

Build-time packaging change; verify against build artifacts:

- **Universal build** (`--arch=x86_64,arm64`):
  - `file bin/diagnostics` and `file bin/rpostback` report
    "Mach-O universal binary with 2 architectures [x86_64] [arm64]".
  - `otool -L -arch arm64 bin/diagnostics` shows OpenSSL refs under
    `../Frameworks/arm64`; `-arch x86_64` shows `../Frameworks`.
  - `codesign -vvv --strict` passes on both binaries (already part of the
    packaging script's final validation step).
  - On Apple Silicon with an arm64 R: no Rosetta prompt at startup; a
    diagnostics report runs, and a git-over-ssh operation (which invokes
    `rpostback`) works.
- **arm64-only build** (`--arch=arm64`): packaging succeeds (no `lipo`
  same-architecture error); `file bin/diagnostics` / `bin/rpostback` report a
  single arm64 architecture.
- **x86_64-only build** (`--arch=x86_64`) with a stale `build-arm64` present:
  packaging succeeds and `file bin/diagnostics` / `bin/rpostback` report a
  single x86_64 architecture (no arm64 slice leaks in).

No C++/GWT/desktop unit-test surface -- this is build-time packaging only.

## Out of scope

- `rsession`/`rsession-arm64` behavior (unchanged; correctly arch-selected).
- Any change to `makeUniversalApp` usage beyond the version bump already staged.
