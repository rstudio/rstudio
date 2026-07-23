# Universal `diagnostics` and `rpostback` binaries Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship `bin/diagnostics` and `bin/rpostback` as universal (x86_64 + arm64) Mach-O binaries in the macOS RStudio.app so Apple Silicon no longer needs Rosetta for them.

**Architecture:** The arm64 build already produces both binaries; the x86_64 primary build's install step (`prepare-package.cmake`) fuses each with its arm64 counterpart via `lipo`. The merge is gated on an explicit `RSTUDIO_UNIVERSAL_BUILD` flag from `make-package` (set only when both architectures are built), so single-architecture builds skip it. Each fat binary's per-slice load commands point at the matching `Frameworks` (x86_64) / `Frameworks/arm64` (arm64) bundled OpenSSL.

**Tech Stack:** CMake install scripts, Bash, macOS `lipo` / `install_name_tool` / `codesign`.

**Design doc:** `docs/specs/2026-07-23-universal-diagnostics-rpostback-design.md`

## Global Constraints

- macOS packaging only; no C++/GWT/TS source changes and no runtime selection logic.
- Do not change the existing `rsession-arm64` / `node-arm64` merge (existence-gated `configure_file` copy) — out of scope.
- Match existing style: CMake install scripts use 3-space indentation and the local `echo()` helper (not `message()`) for progress output.
- `package/osx/scripts/node_modules/` is gitignored; never stage it. Only `package.json` / `package-lock.json` are committed for the `@electron/universal` bump.
- Commit messages: imperative mood, <=72-char subject. Use "Addresses #12572" (not "Fixes"), per repo git conventions.
- This plan file and the design doc under `docs/specs/` are removed before push/PR.

---

### Task 1: Universal `diagnostics` / `rpostback` packaging

**Files:**
- Modify: `package/osx/make-package` (after line 261; and the x86_64 configure block around line 343)
- Modify: `package/osx/CMakeLists.txt` (after the `RSESSION_ARM64_PATH` block, lines 28-32)
- Modify: `package/osx/cmake/prepare-package.cmake` (append after line 145)
- Modify: `NEWS.md` (add to the `### Fixed` section, after line 42)

**Interfaces:**
- Produces: CMake cache variable `RSTUDIO_UNIVERSAL_BUILD` (`"0"` or `"1"`), consumed via `@RSTUDIO_UNIVERSAL_BUILD@` in the configured `prepare-package.cmake`.
- Produces: CMake cache variables `DIAGNOSTICS_ARM64_PATH`, `RPOSTBACK_ARM64_PATH`, consumed via `@DIAGNOSTICS_ARM64_PATH@` / `@RPOSTBACK_ARM64_PATH@`.
- Consumes (existing): `RSESSION_BINARY_DIR`, `FIX_LIBRARY_PATHS_SCRIPT_PATH`, `echo()`, `CMAKE_INSTALL_PREFIX` — all already defined at the top of `prepare-package.cmake`.

- [ ] **Step 1: Compute the universal-build flag in `make-package`**

Insert immediately after line 261 (`case "${arch}" in *x86_64*) build_x86_64=1 ;; esac`):

```sh

# a universal build produces both architectures; only then may the x86_64
# primary build merge the arm64 slices of diagnostics/rpostback. lipo cannot
# combine two same-architecture inputs, so single-arch builds must skip it.
universal_build=0
if [ -n "${build_x86_64}" ] && [ -n "${build_arm64}" ]; then
   universal_build=1
fi
```

- [ ] **Step 2: Pass the flag on the x86_64 configure**

In the `arch -x86_64 "${CMAKE}"` configure invocation, add a line immediately after `-DRSTUDIO_PACKAGE_BUILD=1 \` (line 343). Keep the trailing backslash alignment of the surrounding block:

```sh
      -DRSTUDIO_UNIVERSAL_BUILD=${universal_build}          \
```

Do NOT add it to the arm64 configure blocks (lines ~393 and ~409) — only the x86_64 primary build runs `prepare-package.cmake`.

- [ ] **Step 3: Verify `make-package` still parses**

Run: `bash -n package/osx/make-package && echo OK`
Expected: `OK`

Run: `grep -n "RSTUDIO_UNIVERSAL_BUILD" package/osx/make-package`
Expected: two lines — the `universal_build=1` assignment and the single `-DRSTUDIO_UNIVERSAL_BUILD=${universal_build}` on the x86_64 configure.

- [ ] **Step 4: Expose the arm64 build paths in `package/osx/CMakeLists.txt`**

Insert after the existing `RSESSION_ARM64_PATH` `set(...)` (immediately after line 32, before the `# developer-id code signing` comment):

```cmake

# help install script find arm64 diagnostics / rpostback for universal binaries
set(
   DIAGNOSTICS_ARM64_PATH
   "${CMAKE_CURRENT_SOURCE_DIR}/build-arm64/src/cpp/diagnostics/diagnostics"
   CACHE INTERNAL "")

set(
   RPOSTBACK_ARM64_PATH
   "${CMAKE_CURRENT_SOURCE_DIR}/build-arm64/src/cpp/session/postback/rpostback"
   CACHE INTERNAL "")
```

- [ ] **Step 5: Append the universal-merge block to `prepare-package.cmake`**

Append at the end of the file (after line 145):

```cmake

# Combine the x86_64 and arm64 builds of diagnostics and rpostback into
# universal binaries. This runs only for universal builds: the primary build is
# x86_64 (so the binaries already installed under bin/ are the x86_64 slices)
# and a separate arm64 build has been produced. lipo cannot merge two inputs of
# the same architecture, so single-architecture builds must not reach here --
# hence gating on RSTUDIO_UNIVERSAL_BUILD, not on file existence alone.
if("@RSTUDIO_UNIVERSAL_BUILD@" STREQUAL "1")

   set(LIPO_STAGING_DIR "${CMAKE_INSTALL_PREFIX}/.arm64-lipo-staging")
   file(MAKE_DIRECTORY "${LIPO_STAGING_DIR}")

   foreach(TOOL diagnostics rpostback)

      if("${TOOL}" STREQUAL "diagnostics")
         set(ARM64_SOURCE "@DIAGNOSTICS_ARM64_PATH@")
      else()
         set(ARM64_SOURCE "@RPOSTBACK_ARM64_PATH@")
      endif()

      set(X64_BINARY "${RSESSION_BINARY_DIR}/${TOOL}")

      if(EXISTS "${ARM64_SOURCE}" AND EXISTS "${X64_BINARY}")

         echo("Creating universal '${TOOL}' binary")

         # stage the arm64 build and point it at the arm64 Frameworks directory
         file(COPY "${ARM64_SOURCE}" DESTINATION "${LIPO_STAGING_DIR}")
         execute_process(
            COMMAND
               "${FIX_LIBRARY_PATHS_SCRIPT_PATH}"
               "${LIPO_STAGING_DIR}"
               "@executable_path/../Frameworks/arm64"
               "${TOOL}")

         # fuse the (already path-fixed) x86_64 slice with the arm64 slice
         execute_process(
            COMMAND
               lipo -create
                  "${X64_BINARY}"
                  "${LIPO_STAGING_DIR}/${TOOL}"
               -output "${X64_BINARY}.universal"
            RESULT_VARIABLE LIPO_RESULT)

         if(NOT LIPO_RESULT EQUAL 0)
            message(FATAL_ERROR "lipo failed for '${TOOL}' (exit ${LIPO_RESULT})")
         endif()

         file(RENAME "${X64_BINARY}.universal" "${X64_BINARY}")

      else()

         echo("Skipping universal '${TOOL}': arm64 build not found at '${ARM64_SOURCE}'")

      endif()

   endforeach()

   # remove staging artifacts so they are not packaged or signed
   file(REMOVE_RECURSE "${LIPO_STAGING_DIR}")

endif()
```

Notes for the implementer:
- The x86_64 `bin/diagnostics` and `bin/rpostback` already had their OpenSSL load commands rewritten to `@executable_path/../Frameworks` by the existing fix at lines 133-145, which runs before this block. This block only fixes the arm64 slice (to `.../Frameworks/arm64`) before merging, so each slice resolves its own bundled OpenSSL.
- `codesign-package.sh` runs after this script and recursively re-signs the bundle, so the freshly `lipo`'d (signature-stripped) binaries are re-signed automatically. No change to that script.

- [ ] **Step 6: Add the NEWS.md entry**

Add to the end of the `### Fixed` section (after line 42):

```markdown
- ([#12572](https://github.com/rstudio/rstudio/issues/12572)): The macOS `diagnostics` and `rpostback` helper binaries are now universal (Apple Silicon + Intel), so features that use them no longer require Rosetta 2 on Apple Silicon Macs.
```

- [ ] **Step 7: Sanity-check the edits**

Run: `git diff --stat package/osx/make-package package/osx/CMakeLists.txt package/osx/cmake/prepare-package.cmake NEWS.md`
Expected: four files changed, additions only in the expected regions.

Run: `grep -c "@RSTUDIO_UNIVERSAL_BUILD@\|@DIAGNOSTICS_ARM64_PATH@\|@RPOSTBACK_ARM64_PATH@" package/osx/cmake/prepare-package.cmake`
Expected: `3`

- [ ] **Step 8: Commit**

```bash
git add package/osx/make-package package/osx/CMakeLists.txt \
        package/osx/cmake/prepare-package.cmake NEWS.md
git commit -m "Build universal diagnostics/rpostback on macOS"
```

---

### Task 2: `@electron/universal` version bump

**Files:**
- Modify (already staged in the working tree): `package/osx/scripts/package.json`, `package/osx/scripts/package-lock.json`

**Interfaces:** none (build-time packaging dependency only).

- [ ] **Step 1: Confirm the bump and that node_modules is not staged**

Run: `git diff --cached --stat; git status --short package/osx/scripts/`
Expected: only `package.json` and `package-lock.json` under `package/osx/scripts/` are modified; `node_modules/` does not appear (it is gitignored).

Run: `grep '"@electron/universal"' package/osx/scripts/package.json`
Expected: `"@electron/universal": "3.0.6"`

- [ ] **Step 2: Commit**

```bash
git add package/osx/scripts/package.json package/osx/scripts/package-lock.json
git commit -m "Update @electron/universal to 3.0.6"
```

---

### Task 3: Build verification (macOS)

This is the meaningful integration gate. The static checks above catch syntax mistakes; correctness is confirmed by packaging on macOS. No commit — verification only.

The build installs the merged bundle at `package/osx/install/RStudio.app`; its
binaries live under `Contents/Resources/app/bin/`. Below, `BIN` refers to
`package/osx/install/RStudio.app/Contents/Resources/app/bin`.

**Interfaces:** none.

- [ ] **Step 1: Universal build**

Run: `cd package/osx && ./make-package --arch=x86_64,arm64` (a normal packaging build; a full DMG/signing run is not required — an install into `install/RStudio.app` is enough to inspect `bin/`).

Then inspect the produced app bundle's `bin/`:

Run:
```bash
file "${BIN}/diagnostics" "${BIN}/rpostback"
```
Expected: each reports `Mach-O universal binary with 2 architectures: [x86_64 ...] [arm64 ...]`.

- [ ] **Step 2: Per-slice OpenSSL paths are correct**

Run:
```bash
otool -L -arch arm64  "${BIN}/diagnostics" | grep Frameworks
otool -L -arch x86_64 "${BIN}/diagnostics" | grep Frameworks
```
Expected: arm64 slice references `@executable_path/../Frameworks/arm64/libssl.3.dylib` (and `libcrypto`); x86_64 slice references `@executable_path/../Frameworks/libssl.3.dylib`. Repeat for `rpostback`.

- [ ] **Step 3: Signatures valid**

Run: `codesign -vvv --strict "${BIN}/diagnostics" "${BIN}/rpostback"`
Expected: `valid on disk` / `satisfies its Designated Requirement` (no errors). Confirm no leftover `package/osx/install/RStudio.app/../.arm64-lipo-staging` directory exists beside the `.app`.

- [ ] **Step 4: Single-architecture builds skip the merge cleanly**

Run (arm64-only): `cd package/osx && ./make-package --arch=arm64 --clean`
Expected: packaging succeeds (no `lipo` "have the same architectures" error); `file bin/diagnostics` / `bin/rpostback` report a single `arm64` architecture.

Run (x86_64-only): `cd package/osx && ./make-package --arch=x86_64 --clean`
Expected: packaging succeeds; `file bin/diagnostics` / `bin/rpostback` report a single `x86_64` architecture with no arm64 slice.

- [ ] **Step 5: Functional smoke test (Apple Silicon, arm64 R)**

Launch the universal build on an Apple Silicon Mac with an arm64 R:
- No Rosetta prompt at startup.
- Help > run a diagnostics report succeeds.
- A git-over-SSH operation (which invokes `rpostback`) succeeds.
