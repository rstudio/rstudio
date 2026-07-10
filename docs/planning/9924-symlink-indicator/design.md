# Design: Symlink visual indicator in the Files pane (#9924)

> **Status:** planning document. This file (and everything under
> `docs/planning/`) is a temporary artifact for design review and is removed in the
> implementation PR.

## Problem

The RStudio Files pane renders every entry with only its file-type icon and name. It
gives no indication that an entry is a **symbolic link**. Every other common tool the
user compares against does:

- `ls -l` shows `link -> target`
- macOS Finder overlays a small curved-arrow badge on the icon
- GitHub's file browser shows an arrow glyph
- Emacs `dired` shows the `lrwxrwxrwx ... link -> target` form

So a user cannot tell, from the Files pane alone, whether `R-CMD-check.yaml` is a real
file or a symlink to `check-full.yaml`. Issue #9924 asks for a visual indicator.

### Why now / relationship to #18191

PR #18191 ("Follow macOS Finder aliases in the Files pane") recently added end-to-end
plumbing so that clicking a macOS Finder alias *follows* it to its target
(`alias_target`). But it added **no visual indicator** — the alias still looks exactly
like a normal file. This work adds the indicator for both POSIX symlinks and Finder
aliases, completing that UX.

## Goals

1. Show a recognizable indicator on symlinked files and directories in the Files pane.
2. Cover macOS Finder aliases with the same indicator (they are "links" in the user's
   mental model even though they are not POSIX symlinks).
3. Do not regress Files pane listing performance.
4. Keep the indicator legible at the pane's small (~16px) icon size, including a
   non-visual affordance (tooltip / screen-reader text).

## Non-goals (explicitly out of scope)

- **Windows `.lnk` shortcut files.** There is no resolution infrastructure for them in
  the codebase, and they are not POSIX symlinks. Out of scope for this change.
- **Showing broken / dangling symlinks.** These are currently hidden by the listing
  filter (it requires the resolved target to exist). We keep that behavior — see
  Decision 2.

## Decisions

### Decision 1 — Indicator style: arrow-badge overlay (not a replacement icon)

We keep the normal file-type / folder icon and composite a **small shortcut-arrow badge**
in the lower-left corner of the icon.

**Why this over the alternatives:**

| Option | Verdict | Reason |
| --- | --- | --- |
| **Arrow badge overlay** (chosen) | ✅ | Matches macOS Finder and Windows conventions. Preserves file-type/folder information — critical because a symlink can point at *anything*, and a directory symlink must still read as a navigable folder. |
| Single generic "link" icon replacing the type icon | ❌ | Throws away file-type and directory-ness. You could no longer tell a dir-link from a script-link at a glance, and a directory symlink would stop looking navigable. |
| Separate status column (git-status style) | ❌ | Adds horizontal clutter to an already narrow pane, and a separate column is not an OS-familiar convention for "this is a link". |
| Text `-> target` appended to the name (`ls` style) | ❌ | Least glanceable; complicates the name column and its sorting. (We do reuse the `-> target` form, but in a *tooltip*, not the name.) |

### Decision 2 — Broken symlinks stay hidden

The listing loop in `SessionFilesListingMonitor::listFiles` filters entries on
`filePath.exists()`, and `exists()` follows the link — so a dangling **symlink** is
already excluded from the listing today. We **do not** change this filter. Rationale:
showing dangling links is a separate behavior change with its own edge cases (no size, no
mtime, uncertain type), and it expands scope beyond "add an indicator". A dangling
symlink remains absent from the pane, exactly as today.

Note the asymmetry with aliases: a macOS Finder alias is a **regular file**, so its own
`exists()` is true and it is *always* listed even when its target is broken/unresolvable.
Decision 3 therefore badges aliases from an alias-detection flag, not from successful
target resolution, so a broken alias is still marked (see below).

### Decision 3 — One indicator for symlinks *and* aliases (incl. broken aliases)

The frontend treats an entry as "link-like" when the backend reports `is_symlink`
**or** `is_alias`. Both get the same badge. From the user's point of view both "point
somewhere else", so a single indicator is the least surprising.

Crucially, `is_alias` is emitted whenever `isFinderAlias()` succeeds — **independent of
whether the target resolves**. The pre-existing `alias_target` field is only set when
resolution succeeds (it drives *navigation*), so keying the badge off `is_alias` (not
`alias_target`) ensures a broken/unresolvable alias is still badged, satisfying the
"cover Finder aliases" goal. `alias_target` remains the field that tells the client where
to navigate.

## Addressing the two stated concerns

### Concern A — no Files-pane performance regression

The listing path is already heavily stat-bound (roughly a dozen `stat`-family syscalls
per entry — see `implementation-plan.md` for the exact call chain). Detecting symlink
status adds **one** `is_symlink` call per entry (a `symlink_status` / `lstat`-equivalent
that does not follow the link), reusing the `FilePath` object the code already builds.
That is negligible relative to the existing per-entry cost. Resolving the symlink target
for the tooltip is done **only for entries that are actually symlinks**, so it costs
nothing on the common (non-link) path. macOS alias detection already runs per entry today
(added by #18191), so aliases add no new cost.

### Concern B — recognizable on a ~16px icon

The icon column is ~26px wide with a 16px icon and a 22.5px row — tight. Mitigations:

- **Corner placement + outline.** The badge sits in the lower-left corner (Finder/Windows
  convention) and carries a light outline/halo so it reads against both light and dark row
  backgrounds. Dark themes only *dim* icons (a brightness/saturation CSS filter), they do
  not invert them, so a single asset with an outline is expected to work in both; a
  dedicated dark asset is a fallback if contrast proves insufficient.
- **Non-visual affordances.** The badge carries alt/aria text ("symbolic link" / "alias"),
  and the **icon cell** gets a `title` tooltip of the form `name -> target` (or just the
  name when the target is unavailable, e.g. a broken alias). Hovering the icon reveals the
  relationship and screen readers announce it. The tooltip lives on the icon cell rather
  than the name cell because `LinkColumn` (the name column) hard-couples its `title` to
  its displayed text (`LinkColumn.java:104`, `title="{1}"` reuses the name string), so it
  cannot carry a distinct tooltip without a shared-class change — see the implementation
  plan.

## Risks / open points for review

- **New overlay pattern.** RStudio has no existing icon-overlay/badge mechanism (git
  status is rendered as a *separate* side-by-side image, not stacked). This introduces a
  small new compositing path in the icon renderer. It is opt-in: with no badge, rendering
  output is unchanged for every other icon consumer.
- **Badge legibility** ultimately needs a visual check on real light/dark themes; the
  asset may need tuning. Called out in the verification steps.
