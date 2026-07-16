# Files pane follows & badges Windows .lnk shortcuts

## Context

PR #18191 made the Files pane follow macOS Finder aliases (backend resolves them in `createFileSystemItem`, emitting `alias_target`; the GWT client navigates/opens the target). PR #18195 added a link badge + tooltip for symlinks and aliases (`is_symlink`/`symlink_target`/`is_alias` fields, `FileIcon.withLinkBadge`, `FilesList.decorateLinkIcon`). Both are merged into main.

This change implements the Windows counterpart: `.lnk` shell shortcuts. Today, clicking a `.lnk` in the Files pane errors (folder shortcut) or opens binary garbage (file shortcut) — closed-stale issue **#7327** describes exactly this. The symlink badging from #18195 already works on Windows; the new work is detecting/resolving `.lnk` files and badging them as "Shortcut".

**Key design decision:** the backend emits the resolved shortcut target in the *existing* `alias_target` field, so all merged client logic (navigation, open commands, dialogs, target-based file icons, columns dedupe) works unchanged. A new `is_shortcut` flag (not `is_alias`) drives the badge label, keeping platform knowledge server-side (no BrowseCap — unreliable in server mode).

**Branch:** `feature/windows-lnk-shortcuts`

## Verified facts

- rsession initializes COM (STA) eagerly on the main thread at startup: `src/cpp/session/SessionMain.cpp:2255-2259`. Listings and file-monitor callbacks (`checkForChanges` via `onBackgroundProcessing`, `SessionModuleContext.cpp:1036`) run on the main thread → no CoInitialize needed in the resolver. `rsession --run-tests` also runs gtest on the main thread after this init.
- Working IShellLink reference code exists in `src/cpp/session/modules/SessionGit.cpp:2815-2949` (`detectGitBinDirFromShortcut`), incl. a file-private `AutoRelease<T>` RAII wrapper (:2744). No CMake link changes needed (COM already used in this target).
- `FilePath` offers `getAbsolutePathW()` (:434), `getCanonicalPath()` (:443), `getExtensionLowerCase()` (:488, returns e.g. `".lnk"`), `isRegularFile()` (:715). On Windows, `FilePath` stringifies via `generic_wstring()` → backslashed paths from `GetPath` round-trip to forward-slashed, client-navigable paths automatically.
- `NOMINMAX`/`WIN32_LEAN_AND_MEAN` are global compile defs (`src/cpp/CMakeLists.txt:153,158`) — no windows.h macro collisions.
- Session test files are globbed unconditionally (`src/cpp/session/CMakeLists.txt:427-440`); `#ifdef _WIN32` gtest precedent: `src/cpp/core/system/Win32SystemTests.cpp`.

## Implementation

### 1. Backend resolver — NEW `src/cpp/session/SessionModuleContextWin32.cpp`

Mirrors `SessionModuleContext.mm` (the Apple companion). Two functions in `namespace rstudio::session::module_context`:

- `bool isWindowsShortcut(const FilePath&)` — `getExtensionLowerCase() == ".lnk" && isRegularFile()`. Cheap extension gate so COM work is only paid for actual `.lnk` files (excludes dirs named `*.lnk`).
- `Error resolveWindowsShortcut(const FilePath&, FilePath* pTargetPath)` — `CoCreateInstance(CLSID_ShellLink, …, IID_IShellLinkW)` → `QueryInterface(IID_IPersistFile)` → `Load(getAbsolutePathW().c_str(), STGM_READ)` → `GetPath(buf, MAX_PATH, nullptr, 0)`.
  - **No `IShellLink::Resolve()`** — it can search the disk / hit the network during a listing.
  - `GetPath` flags = `0` (not `SLGP_RAWPATH` which returns unexpanded `%VAR%`; not `SLGP_UNCPRIORITY`). Matches the `SessionGit.cpp:2873` precedent.
  - Success test is `hr == S_OK && buf non-empty` — `S_FALSE`/empty means a virtual (non-filesystem) shortcut → error.
  - **`GetPath` returns the STORED path even when the target was deleted** — resolution *succeeds* for broken shortcuts; broken detection is the caller's `exists()` check (comment this; it diverges from macOS where resolution itself fails).
  - Error shape mirrors `resolveFinderAlias`: `systemError(no_such_file_or_directory)` + `shortcut-path` and `hresult` properties.
- Duplicate the small `AutoRelease<T>` wrapper file-privately (like SessionGit.cpp); hoisting it to a shared header is out-of-scope churn.
- Includes: `<windows.h>`, `<objbase.h>`, `<shlobj.h>`, shared_core Error/FilePath.

### 2. Declarations — `src/cpp/session/include/session/SessionModuleContext.hpp`

Add an `#ifdef _WIN32` block right after the existing `#ifdef __APPLE__` block (ends line 137), same comment style: shortcuts are shell objects, not symlinks; stored target returned even if it no longer exists — callers detect broken shortcuts with `exists()`. No Windows headers needed in the hpp.

### 3. CMake — `src/cpp/session/CMakeLists.txt`

Append `SessionModuleContextWin32.cpp` to the existing `else()` (Windows) branch at lines 416-421.

### 4. Emit fields — `src/cpp/session/SessionModuleContext.cpp` `createFileSystemItem`

Add an `#ifdef _WIN32` block immediately after the `__APPLE__` block's `#endif` (line 1996), before `entry["dir"] = isDir;` — structurally identical to the alias block:

```cpp
if (!isDir && isWindowsShortcut(filePath))
{
   entry["is_shortcut"] = true;          // badge even when broken
   FilePath targetPath;
   Error error = resolveWindowsShortcut(filePath, &targetPath);
   if (error)
      LOG_DEBUG_MESSAGE(...);            // garbage/virtual .lnk = normal state
   else if (targetPath.exists())
   {
      entry["alias_target"] = createAliasedPath(targetPath);
      isDir = targetPath.isDirectory();
   }
}
```

Broken shortcut ⇒ `is_shortcut` only, no `alias_target`, `dir` false — same contract as a broken alias.

### 5. GWT client

- **`core/client/files/FileSystemItem.java`**: add native `isShortcut()` reading `this.is_shortcut`; extend `isLink()` to `isSymlink() || isAlias() || isShortcut()`; update comments on `getAliasTarget()`/`resolveAliasTarget()`/`getLinkTarget()` to mention Windows shortcuts (behavior unchanged — `alias_target` covers them).
- **`workbench/views/files/ui/FilesList.java`** `decorateLinkIcon` (:233-248): three-way label — symlink → `symbolicLinkBadgeLabel()`, shortcut → `shortcutBadgeLabel()`, else → `aliasBadgeLabel()`.
- **`workbench/views/files/FilesConstants.java`** + `FilesConstants_en.properties` (`shortcutBadgeLabel=Shortcut`) + `FilesConstants_fr.properties` (`shortcutBadgeLabel=Raccourci`).
- **Comment-only updates** where "macOS Finder alias" comments now also cover shortcuts: `FileTypeRegistry.java:703-707` (target-name icon lookup — load-bearing: `report.xlsx.lnk` gets the Excel icon), `Files.java` (~:356, ~:796), `FileDialog.java`, `OpenFileDialog.java`, `DirectoryContentsWidget.java`, `ChooseFolderDialog2.java`.
- No display-name change: rows keep the full `.lnk` filename (matches the pane's real-filename philosophy; the macOS PRs didn't rename either).
- No Commands.cmd.xml / prefs-schema changes.

### 6. Tests

**C++ — `src/cpp/session/SessionModuleContextTests.cpp`**, new `#ifdef _WIN32` section after `#endif // __APPLE__` (line 556), plus `<windows.h>/<objbase.h>/<shlobj.h>` includes next to the Apple include block (:26-31). `WindowsShortcutTest` fixture mirrors `FinderAliasTest`: temp dir canonicalized via `getCanonicalPath()` (8.3 short-name expansion, mirrors the mac `realpath` step), `createWindowsShortcut()` helper writing real `.lnk` via `IShellLinkW::SetPath` + `IPersistFile::Save`. Cases:
- Detection: shortcut to file/dir → true; uppercase `.LNK` → true; regular file/dir → false; directory named `folder.lnk` → false; nonexistent path → false.
- Resolution: file/dir targets resolve to equal absolute paths (dir also `isDirectory()`); garbage-content `.lnk` → `Load` error; **deleted target → resolve SUCCEEDS with stored path** (documented divergence from macOS).
- `createFileSystemItem` contract: dir shortcut → `dir` true + `is_shortcut` + `alias_target`; file shortcut; regular file → neither field; broken shortcut → `is_shortcut` only, `dir` false. Compare `alias_target` against `module_context::createAliasedPath(target)` (TEMP is under the user home on Windows, so values are `~`-aliased).

**GWT — `src/gwt/test/org/rstudio/core/client/files/FileSystemItemTests.java`**: extend the `createLinkEntry` JSNI fixture with an `isShortcut` param (update 6 existing call sites with `false`); add tests: shortcut-is-link + `getLinkTarget` from `alias_target`; broken shortcut is still link with null target; directory shortcut → `resolveAliasTarget()` is directory with target path; extend `testRegularFileIsNotLink` with `assertFalse(isShortcut())`.

**e2e — NEW `e2e/rstudio/utils/windows-shortcuts.ts` + `e2e/rstudio/tests/panes/files/files_shortcuts.test.ts`**: mirror `finder-aliases.ts` / `files_aliases.test.ts`. Fixture creates targets + `.lnk` files through the R console: R writes a `.ps1` (param target/link; `WScript.Shell` `CreateShortcut`/`Save`; prints `true` on `Test-Path` success) and runs `system2("powershell", c("-NoProfile","-NonInteractive","-ExecutionPolicy","Bypass","-File", ...))`, asserting a `SHORTCUT-SETUP-OK` sentinel. Shortcut names end in `.lnk` (rows show full filenames). Spec: `@windows_only @desktop_only` tags + `test.skip(process.platform !== 'win32', ...)`; mirror all four alias tests (dir-shortcut navigation, file-shortcut click opens target, open command on selection, columns dedupe of shortcut+target).

### 7. Release notes

- `NEWS.md` under `### New`: `- ([#7327](https://github.com/rstudio/rstudio/issues/7327)): On Windows, the Files pane now follows .lnk shortcuts: clicking a shortcut to a folder navigates to that folder, and clicking a shortcut to a file opens the file. Shortcuts are marked with a link indicator, like symbolic links and macOS Finder aliases.`
- `src/node/desktop/src/assets/whats-new/yellow-yarrow/index.html`: add a "Support Windows Shortcuts" bullet after the Finder-alias one.

## Verification

1. C++ build: `ninja` from `build/` (full build, per established workflow).
2. C++ tests: `build\src\cpp\rstudio-tests.bat --scope rsession` in a **fresh console** (not the agent shell); confirm `WindowsShortcutTest.*` pass.
3. GWT: `cd src/gwt && ant javac` (fast check) → `ant unittest` (FileSystemItemTests) → `ant draft` (runnable JS for e2e/manual).
4. e2e: run `files_shortcuts.test.ts` in desktop mode per the `rstudio-run-playwright-tests` skill.
5. Manual smoke in the built IDE: Explorer-created shortcuts to a folder, an `.R` file, a deleted target, and a Control Panel applet — verify navigate/open/badge ("Shortcut" alt-text, `name -> target` tooltip), broken/virtual shortcuts badged but inert.

Commit per-logical-change (backend, GWT, tests, e2e, notes may be grouped sensibly); roborev reviews auto-trigger per commit.

## Risks / gotchas

- Deleted-target resolution succeeds by design (stored path); don't "fix" the test that asserts this.
- 8.3 short names in `%TEMP%` on CI: fixture canonicalizes; fall back to `isEquivalentTo()` for live-target comparisons if a machine still mismatches.
- `is_shortcut` stays distinct from `is_alias`; verified all navigation/dialog/icon logic keys off `alias_target`/`resolveAliasTarget()` only — badging is the only flag consumer via `isLink()`.
- Perf: one lowercase-extension compare per non-dir entry; in-proc COM instantiation only for `.lnk` files — cheaper than the mac per-alias bookmark resolution.
