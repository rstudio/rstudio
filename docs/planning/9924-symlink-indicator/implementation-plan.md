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
3. When `is_symlink` is true, resolve and emit the target (**mandatory**, not optional —
   the tooltip and end-to-end verification depend on it):
   `entry["symlink_target"] = createAliasedPath(filePath.resolveSymlink())`.
   - `FilePath::resolveSymlink()` (`src/cpp/shared_core/FilePath.cpp:1762`) wraps
     `boost::filesystem::read_symlink`. Runs only for actual symlinks (rare). If
     `resolveSymlink()` fails, omit `symlink_target`; the frontend tooltip falls back to
     the plain name (see Layer 3d).
4. **macOS aliases:** inside the existing `#ifdef __APPLE__` block (~:1944-1964), when
   `isFinderAlias(filePath)` is true, emit `entry["is_alias"] = true` **regardless of
   whether the target resolves**. Keep the existing `alias_target` emission exactly as-is
   (it stays gated on successful resolution and drives navigation). This is the fix for
   the broken-alias gap: a broken/unresolvable alias is a normal visible regular file, so
   badging must key off `is_alias`, not `alias_target`. No new syscall — `isFinderAlias`
   already runs here today.

Deliberately **not** changed:

- The `exists()` filter at `SessionFilesListingMonitor.cpp:219` — broken **symlinks** stay
  hidden (Decision 2). (Broken aliases are regular files and remain listed, now badged via
  `is_alias`.)
- `FileInfo`. The `FileInfo(const FilePath&)` constructor intentionally does *not* read
  symlink status (`src/cpp/core/include/core/FileInfo.hpp:47-56`, with a warning about
  boost filesystem surprises). We query the `FilePath` directly in `createFileSystemItem`
  instead of threading a bool through `FileInfo`, so `FileInfo` equality and the file
  monitor are untouched.
- `is_symlink` stays `false` for aliases (they are regular files, per the `#ifdef
  __APPLE__` comment at ~:1940); aliases are covered by `is_alias`.

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

// true for a macOS Finder alias, even one whose target could not be resolved
// (a broken alias has is_alias == true but alias_target == null)
public final native boolean isAlias() /*-{
   return !!this.is_alias;
}-*/;

// single "link-like" predicate used by the UI: POSIX symlink OR macOS alias
public final boolean isLink()
{
   return isSymlink() || isAlias();
}

// resolved symlink target for the tooltip (null when not a symlink or unresolved)
public final native String getSymlinkTarget() /*-{
   return this.symlink_target || null;
}-*/;

// best available "points to" target for the tooltip: symlink target or, for an
// alias, its resolved target; null when neither is available (e.g. broken alias)
public final String getLinkTarget()
{
   String target = getSymlinkTarget();
   return target != null ? target : getAliasTarget();
}
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
- Tooltip: add a `title` attribute to the **icon-cell wrapper `<span>`** introduced in 3b
  (in `FileIconRenderer`), of the form `name -> target` from `getLinkTarget()`; when the
  target is null (e.g. a broken alias), fall back to just the name. Passed alongside the
  badge when the icon column builds the decorated `FileIcon`.
- **Not on the name column.** `LinkColumn`'s cell template hard-codes
  `<div class="{0}" title="{1}">{1}</div>` (`LinkColumn.java:104`) — the `title` and the
  visible text are the *same* `{1}` value, and there is no title provider. Putting the
  `name -> target` string there would render it as visible text in the name column
  (the "text arrow in name" style we rejected in `design.md`). Reusing `LinkColumn` for a
  distinct tooltip would require adding a separate title provider to that shared class
  (also used by the Packages pane / `PackageLinkColumn`); the icon-cell tooltip avoids
  that and keeps all link-indicator logic in one place. If a name-column tooltip is later
  deemed necessary, extend `LinkColumn` with an optional title-provider template rather
  than overloading the value string.

## Tests

- **Frontend model (baseline):** extend
  `src/gwt/test/org/rstudio/core/client/files/FileSystemItemTests.java` (already tests the
  alias accessors) with cases that build a raw JS object and assert:
  `isSymlink()` / `getSymlinkTarget()` for a symlink; `isAlias()` for an alias
  **including a broken alias** (`is_alias` true, `alias_target` absent) still reporting
  `isLink()` true; `getLinkTarget()` preferring the symlink target and falling back to the
  alias target and to null. Run: `cd src/gwt && ant unittest`.
- **Backend:** if a session-level test harness makes it practical, assert
  `createFileSystemItem` sets `is_symlink` for a symlinked path; otherwise cover
  `FilePath::isSymlink()` behavior and rely on manual verification. Investigate existing
  `src/cpp/tests` coverage during implementation.
- **e2e (optional):** Playwright test creating a symlink in a temp dir and asserting the
  badge/aria appears. Optional because symlink creation on Windows CI needs privilege.

## Verification (end-to-end)

1. Build: `cd build && cmake --build . --target all`; then `cd src/gwt && ant draft`.
2. In a project dir create a file symlink (`ln -s target link`), a directory symlink, and
   (macOS) a Finder alias plus a **broken** Finder alias. Open the Files pane and confirm:
   - link/alias entries show the corner badge; plain files do not;
   - a directory symlink keeps the folder icon + badge and still navigates in;
   - the broken alias is still listed and badged (tooltip falls back to just the name);
   - hovering the icon shows `name -> target`; screen reader announces "symbolic link".
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

- `src/cpp/session/SessionModuleContext.cpp` — emit `is_symlink`, `symlink_target`, and
  `is_alias`.
- `src/gwt/.../core/client/files/FileSystemItem.java` — `isSymlink()` / `isAlias()` /
  `isLink()` / `getSymlinkTarget()` / `getLinkTarget()`.
- `src/gwt/.../common/filetypes/FileIcon.java`, `FileIconRenderer.java`,
  `FileIconResources.java` + new badge PNGs — the overlay.
- `src/gwt/.../views/files/ui/FilesList.java`, `FilesListDataGridStyle.css` — apply badge
  + tooltip.
- `src/gwt/test/.../files/FileSystemItemTests.java` — model tests.
- `NEWS.md` — changelog entry.
- Remove `docs/planning/9924-symlink-indicator/` in this same PR.
