# Implementation plan: Symlink visual indicator (#9924)

> **Status:** planning document. Removed in the implementation PR. See `design.md` for
> the problem statement and locked decisions.

Line numbers below are anchors at time of writing; verify against the tree when
implementing.

## Layer 1 — Backend: emit the symlink flag (C++)

**File:** `src/cpp/session/SessionModuleContext.cpp`,
`createFileSystemItem(const FileInfo&)` (~lines 1925-1986).

This is the single funnel that serializes each file to JSON for the `list_files` RPC
(via `FilesListingMonitor::listFiles` →
`src/cpp/session/modules/SessionFilesListingMonitor.cpp:223`). The VCS decorator adds
`git_status`/`svn_status` to the same object right after, so per-file JSON augmentation
here is an established pattern.

Changes:

1. Construct the entry's `FilePath` **once** near the top and reuse it. Today it is built
   ad hoc for the `#ifdef __APPLE__` alias block (~:1946) and again for the `exists`
   field (~:1981).
2. Emit `entry["is_symlink"] = filePath.isSymlink();`.
   - `FilePath::isSymlink()` (`src/cpp/shared_core/FilePath.cpp:1435`) uses
     `boost::filesystem::is_symlink` (a `symlink_status`/`lstat`-equivalent — it does
     **not** follow the link, and returns true even for broken links).
3. (Optional, for the tooltip) when `is_symlink` is true, resolve and emit the target:
   `entry["symlink_target"] = createAliasedPath(filePath.resolveSymlink())`.
   - `FilePath::resolveSymlink()` (`src/cpp/shared_core/FilePath.cpp:1762`) wraps
     `boost::filesystem::read_symlink`. Runs only for actual symlinks (rare).

Deliberately **not** changed:

- The `exists()` filter at `SessionFilesListingMonitor.cpp:219` — broken links stay
  hidden (Decision 2).
- `FileInfo`. The `FileInfo(const FilePath&)` constructor intentionally does *not* read
  symlink status (`src/cpp/core/include/core/FileInfo.hpp:47-56`, with a warning about
  boost filesystem surprises). We query the `FilePath` directly in `createFileSystemItem`
  instead of threading a bool through `FileInfo`, so `FileInfo` equality and the file
  monitor are untouched.
- macOS Finder aliases already set `entry["alias_target"]` here; `is_symlink` stays
  `false` for them (they are regular files, per the `#ifdef __APPLE__` comment at ~:1940).

### Performance note

Per-entry syscalls today (all `boost::filesystem`, each an independent stat):
`exists()` + `fileListingFilter`'s `FileInfo` (`isDirectory`, `exists`, `getSize` up to
3, `getLastWriteTime`) + `createFileSystemItem`'s rebuilt `FileInfo` + `exists` +
(macOS) `isFinderAlias`. This change adds exactly one `is_symlink` call per entry and one
`read_symlink` **only** for entries that are symlinks. Negligible.

## Layer 2 — Frontend model (Java)

**File:** `src/gwt/src/org/rstudio/core/client/files/FileSystemItem.java` (near
`isDirectory()` :109 and `getAliasTarget()` :117).

```java
public final native boolean isSymlink() /*-{
   return !!this.is_symlink;
}-*/;

// single "link-like" predicate used by the UI: true POSIX symlink OR macOS alias
public final boolean isLink()
{
   return isSymlink() || getAliasTarget() != null;
}

// resolved symlink target for the tooltip (null when not a symlink)
public final native String getSymlinkTarget() /*-{
   return this.symlink_target || null;
}-*/;
```

## Layer 3 — Frontend rendering: the badge overlay

### 3a. New asset

Add `iconLinkBadge.png` (1x) and `iconLinkBadge_2x.png` (2x) to
`src/gwt/src/org/rstudio/studio/client/common/filetypes/`, and register in
`FileIconResources.java` following the existing `@Source("..._2x.png")` +
`new ImageResource2x(...)` convention. A small (~9-10px effective) curved shortcut arrow
with a light outline so it reads on both light and dark rows.

### 3b. Compositing (recommended approach)

There is no existing overlay/badge helper in the codebase (git status is rendered as a
separate side-by-side image, not stacked), so this adds a small, opt-in pattern.

- Extend `FileIcon` (`.../common/filetypes/FileIcon.java`) with an optional overlay badge
  (`ImageResource` + accessible description) and a builder, e.g. `withLinkBadge()`.
- Update `FileIconRenderer.render()`
  (`.../common/filetypes/FileIconRenderer.java:39-55`): when a badge is present, wrap the
  base icon HTML in a `position: relative` inline-block `<span>` and append an
  absolutely-positioned badge `<img>` (lower-left). When no badge is present (every other
  caller), emit exactly today's output — no behavior change. Handle **both** existing
  branches: the `ImageResourcePrototype.Bundle` sprite path (:42) and the plain `<img>`
  path (:48), by wrapping whichever base HTML is produced.
- In `FilesList.addIconColumn()` (`.../views/files/ui/FilesList.java:188-222`): after
  `fileTypeRegistry.getIconForFile(object)`, return `icon.withLinkBadge()` when
  `object.isLink()`. The parent ".." row (`FileIcon.PARENT_FOLDER_ICON`) never gets a
  badge.

**Fallback approach** (if extending `FileIcon`/`FileIconRenderer` proves too invasive):
make the icon column a Files-pane-local `Cell<FileSystemItem>` that renders the base icon
plus the badge, leaving `FileIconRenderer` untouched.

### 3c. CSS

Add wrapper + badge rules to `.../views/files/ui/FilesListDataGridStyle.css` (wrapper
`position: relative; display: inline-block`; badge `position: absolute; left/bottom: 0`
with fixed width/height). The existing dark-theme rule
`.rstudio-themes-dark .dataGridCell img { filter: brightness(90%) saturate(90%); }`
(:33-35) also dims the badge; the outline handles contrast. Only if a single asset is
insufficient, add `iconLinkBadgeDark_2x.png` and swap on `isDark` (established pattern,
e.g. `RSConnectDeploy.java`).

### 3d. Accessibility / discoverability

- Badge alt/aria text: "symbolic link" (or "alias" for aliases).
- Row tooltip: add a `title` on the name cell (`addNameColumn`, `FilesList.java:224`) of
  the form `name -> target`, from `getSymlinkTarget()` / `getAliasTarget()`.

## Tests

- **Frontend model (baseline):** extend
  `src/gwt/test/org/rstudio/core/client/files/FileSystemItemTests.java` (already tests the
  alias accessors) with cases that build a raw JS object with/without `is_symlink` and
  `alias_target` and assert `isSymlink()` / `isLink()` / `getSymlinkTarget()`.
  Run: `cd src/gwt && ant unittest`.
- **Backend:** if a session-level test harness makes it practical, assert
  `createFileSystemItem` sets `is_symlink` for a symlinked path; otherwise cover
  `FilePath::isSymlink()` behavior and rely on manual verification. Investigate existing
  `src/cpp/tests` coverage during implementation.
- **e2e (optional):** Playwright test creating a symlink in a temp dir and asserting the
  badge/aria appears. Optional because symlink creation on Windows CI needs privilege.

## Verification (end-to-end)

1. Build: `cd build && cmake --build . --target all`; then `cd src/gwt && ant draft`.
2. In a project dir create a file symlink (`ln -s target link`), a directory symlink, and
   (macOS) a Finder alias. Open the Files pane and confirm:
   - link/alias entries show the corner badge; plain files do not;
   - a directory symlink keeps the folder icon + badge and still navigates in;
   - hover tooltip shows `name -> target`; screen reader announces "symbolic link".
3. Regression: a broken symlink is still not listed (unchanged).
4. Performance sanity: list a large directory (thousands of entries) before/after; confirm
   no perceptible slowdown.

## NEWS.md

Add under `### New`:

```
- ([#9924](https://github.com/rstudio/rstudio/issues/9924)): The Files pane now shows a
  link indicator on symbolic links and macOS Finder aliases.
```

## Files touched (implementation PR)

- `src/cpp/session/SessionModuleContext.cpp` — emit `is_symlink` (+ optional target).
- `src/gwt/.../core/client/files/FileSystemItem.java` — `isSymlink()` / `isLink()` /
  `getSymlinkTarget()`.
- `src/gwt/.../common/filetypes/FileIcon.java`, `FileIconRenderer.java`,
  `FileIconResources.java` + new badge PNGs — the overlay.
- `src/gwt/.../views/files/ui/FilesList.java`, `FilesListDataGridStyle.css` — apply badge
  + tooltip.
- `src/gwt/test/.../files/FileSystemItemTests.java` — model tests.
- `NEWS.md` — changelog entry.
- Remove `docs/planning/9924-symlink-indicator/` in this same PR.
