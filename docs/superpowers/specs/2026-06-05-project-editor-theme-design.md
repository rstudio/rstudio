# Project-level editor theme

- **Issue:** [#2350](https://github.com/rstudio/rstudio/issues/2350)
- **Date:** 2026-06-05
- **Status:** Design approved; ready for implementation planning

## Goal

Let a project override the editor (Ace) color theme. Add an **Appearance** pane to
Project Options containing a single theme selector whose default entry, `(Default)`,
means "use the global setting." When set to anything else, that theme takes precedence
over the user's global `editor_theme` for that project only.

This is the first increment of a larger "project-level appearance settings" feature.
Only the **editor theme** is in scope here; the other controls in the global Appearance
pane (RStudio/UI theme, font face, font size, line height, help font size, text
rendering, zoom) are explicitly out of scope for this change.

## Scope

**In scope**
- A new `Appearance` pane in Project Options with one control: editor theme.
- Storage of the chosen theme name in the `.Rproj` file (shared/committed to VCS).
- Live application when changed in the dialog of the already-open project.
- Application on project open/switch (via the existing startup theme sync).
- Fallback to the user's global theme when the project's theme is not installed on
  the current machine, **without** altering the stored project setting.
- A companion fix to the global Appearance pane so an active project override cannot
  clobber the user's global `editor_theme`.

**Out of scope**
- All other appearance settings (UI theme, fonts, sizing, rendering, zoom).
- Per-user (`.Rproj.user`) storage — this setting is shared in `.Rproj`.

**Stretch goal (future fix, not this change)**
- A visual indicator in the Project Options Appearance pane when the stored theme is
  not installed on the current machine.

## Background / why this is the shape it is

The decision was made to store the setting as a real project setting in the `.Rproj`
file (shared across collaborators, committed to VCS), consistent with every other
Project Options pane. Two facts drive the design:

1. **The editor doesn't read the `editor_theme` pref directly.** It binds to the
   `userState().theme()` *object* (`AceThemes.java:69`). User **state has no project
   layer**. The bridge is the backend `syncThemePrefs()` (`SessionThemes.cpp:632`),
   which at session startup reads the *effective* `editor_theme` pref, resolves the
   name to a `{name,url,isDark}` object via `getAllThemes()`, and writes it into
   `userState().theme()` — which then drives the live binding.

2. **Opening/switching a project starts a fresh R session**, so `syncThemePrefs()`
   re-runs on every project open. Once the new `.Rproj` field is mirrored into the
   project pref layer for `editor_theme`, the cross-project case "just works" with no
   changes to the theme-application machinery.

The `.Rproj` field is added through the **sorted-fields** mechanism
(`RProjectFile.cpp`), not the frozen `legacyFields` set. This is precisely the path
that makes older RStudio versions *preserve* the unknown `EditorTheme` field on rewrite
instead of dropping it (the historical churn concern).

No `user-prefs-schema.json` change and no `generate-prefs.R` run are required:
`editor_theme` already exists as a pref, and the shared-prefs mirror in
`ProjectContext::uiPrefs()` is plain code, not the schema-driven `"local"` allowlist
(that allowlist governs the unrelated `.Rproj.user` per-user path, which we are not
using).

## Data flow

```
.Rproj  "EditorTheme: Cobalt"            (sorted field; preserved by older RStudio)
   │  read / write  (RProjectFile.cpp)
RProjectConfig.editorTheme  (C++)         "" == no override == (Default)
   │  ProjectContext::uiPrefs()  →  project pref layer:  editor_theme = "Cobalt"
   │  projectConfigJson() / writeProjectConfig()  ⇄  RProjectConfig.java (editor_theme)
   ▼
editor_theme pref, LAYER_PROJECT  (overrides LAYER_USER)
   │
   ├─ project OPEN/switch → backend syncThemePrefs() resolves effective editor_theme
   │                         (with missing-theme fallback) → userState.theme → editor
   └─ live save in dialog → client resolves name→AceTheme (with fallback)
                              → userState.theme().setGlobalValue() → AceThemes re-themes
```

Both application paths funnel through the existing `userState.theme` → `AceThemes`
binding. No new theme-application machinery is added.

## Sentinel / default convention

- `RProjectConfig.editorTheme` (C++) and `editor_theme` (RProjectConfig.java) use the
  **empty string** to mean "no override / `(Default)`".
- The `.Rproj` `EditorTheme` line is only written when the value is non-empty;
  otherwise the key is removed from the sorted-fields map.
- In `ProjectContext::uiPrefs()`, `editor_theme` is added to the project layer only
  when non-empty (so `(Default)` correctly falls through to the user layer).

## Detailed changes

### Backend (C++): `.Rproj` ⇄ project pref layer

1. **`src/cpp/core/include/core/r_util/RProjectFile.hpp`**
   - Add `std::string editorTheme;` to `RProjectConfig`; initialize to `""` in the
     constructor.

2. **`src/cpp/core/r_util/RProjectFile.cpp`**
   - **Read** (near the `FirstSortedExample` skeleton, ~line 1088): extract
     `EditorTheme` from `dcfFields` into `pConfig->editorTheme`, else
     `defaultConfig.editorTheme`. `EditorTheme` is *not* added to the frozen
     `legacyFields` set, so it is a sorted field.
   - **Write** (sorted-fields section, ~line 1414): if `!config.editorTheme.empty()`
     set `sortedFields["EditorTheme"] = config.editorTheme;` else
     `sortedFields.erase("EditorTheme");`.

3. **`src/cpp/session/projects/SessionProjectContext.cpp`** — `ProjectContext::uiPrefs()`
   (~line 1066, near the markdown block):
   ```cpp
   if (!config_.editorTheme.empty())
      uiPrefs[kEditorTheme] = config_.editorTheme;
   ```
   - Set `editorTheme = ""` in `ProjectContext::defaultConfig()`.

4. **`src/cpp/session/projects/SessionProjects.cpp`**
   - `projectConfigJson()` (~line 504): `configJson["editor_theme"] = config.editorTheme;`
   - `writeProjectConfig()` `readObject` call (~line 825): read `"editor_theme"` into
     `config.editorTheme`.

5. **`src/cpp/session/modules/SessionThemes.cpp`** — `syncThemePrefs()` (line 632):
   add the missing-theme fallback. Resolve the *effective* `editor_theme`; if its name
   is not present in `getAllThemes()`, fall back to the **user-layer** value
   (`prefs::userPrefs().readValue(kUserPrefsUserLayer, kEditorTheme)`) and resolve that;
   if that is also missing, leave the built-in default. Set `userState().theme()` to the
   resolved object. **Do not** modify the `editor_theme` pref or the `.Rproj` value — the
   project setting is preserved; only the *applied* theme falls back. Restructure the
   function so the target theme is computed and compared by name, rather than relying
   solely on the current `state != prefTheme` short-circuit.

### Frontend (GWT)

6. **`src/gwt/.../projects/model/RProjectConfig.java`**
   - Add `getEditorTheme()` / `setEditorTheme(String)` over `this.editor_theme`
     (mirror the `markdownWrap` accessor pattern).

7. **New: `src/gwt/.../projects/ui/prefs/ProjectAppearancePreferencesPane.java`**
   - Model on `ProjectRMarkdownPreferencesPane`. Inject `AceThemes`.
   - One theme `SelectWidget`; first entry `(Default)` with empty value.
   - Load the installed themes asynchronously via `AceThemes.getThemes(...)` and store
     the `HashMap<String, AceTheme>` (used both for the dropdown and to preserve a
     stored-but-uninstalled theme).
   - `initialize(options)`:
     - If `config.editorTheme` is empty → select `(Default)`.
     - If non-empty and present in the theme list → select it.
     - If non-empty but **not** installed → add it as an extra selectable item and
       select it, so OK does not silently clobber the stored value. (Stretch goal would
       annotate this item as unavailable.)
   - `onApply(options)`: `config.setEditorTheme(selectedValue)` (`""` for `(Default)`).
     Return an empty `RestartRequirement` (applied live; no restart needed).
   - `getName()` → "Appearance"; `getIcon()` → appearance icon;
     `wrapWithPanel("project_appearance_prefs")`.
   - Expose a small helper for the dialog's live-apply, e.g.
     `AceTheme resolveAppliedTheme(UserPrefs uiPrefs)` returning the selected theme if
     installed, otherwise the user-global theme (`editorTheme().getGlobalValue()`)
     looked up in the loaded theme list, or `null` if the list has not loaded yet.

8. **`src/gwt/.../projects/ui/prefs/ProjectPreferencesDialog.java`**
   - Add `public static final int APPEARANCE = 2;` **between** `EDITING (1)` and
     `R_MARKDOWN`; renumber `R_MARKDOWN`..`SHARING` (each +1). Insert the new pane into
     the `panes(...)` list between `editing` and `rMarkdown`.
   - Add the pane and dependencies to the constructor: `ProjectAppearancePreferencesPane
     appearance`, store `Provider<UserState> pUserState_` (currently injected but not
     stored), and keep a reference to the appearance pane.
   - In `doSaveChanges()`'s success callback, alongside the markdown block:
     ```java
     String projectTheme = config.getEditorTheme();          // "" == (Default)
     if (StringUtil.isNullOrEmpty(projectTheme))
        uiPrefs.editorTheme().removeProjectValue(true);
     else
        uiPrefs.editorTheme().setProjectValue(projectTheme);

     // live-apply (handles both (Default) and an uninstalled project theme)
     AceTheme applied = appearance_.resolveAppliedTheme(uiPrefs);
     if (applied != null)
        pUserState_.get().theme().setGlobalValue(applied);
     ```
     Rationale for keeping the pref sync + live-apply in `doSaveChanges` (matching the
     other panes): client pref/state mutation happens only after the server write
     succeeds.

### Global Appearance pane companion fix

9. **`src/gwt/.../workbench/prefs/views/AppearancePreferencesPane.java`**
   - Today the theme selector is seeded and the availability check is performed from
     `userState_.theme().getName()` (e.g. lines 565, 581). Once project overrides can
     mutate `userState.theme`, that no longer equals the user's global theme. Change the
     pane to seed/select and availability-check the theme from
     `userPrefs_.editorTheme().getGlobalValue()` (the user-layer name), looked up in the
     loaded theme list. `onApply` continues to write **both** `userState.theme` global
     value and `editor_theme` global value as today.
   - Effect: opening Global Options → Appearance while a project override is active shows
     and edits the user's *global* theme, and clicking OK can no longer overwrite it with
     the project theme.

## Edge cases and decisions

- **Cross-project open** — handled entirely by `syncThemePrefs()` at startup; no reload
  needed because each project open is a fresh session.
- **Live change in the open project** — applied immediately by setting `userState.theme`
  (the only hook the editor binds to); self-heals on next open because
  `syncThemePrefs()` re-derives `userState.theme` from the effective `editor_theme`.
- **Project theme not installed on this machine** — the applied theme falls back to the
  user's global theme (backend at open via `syncThemePrefs`; client at live-save via
  `resolveAppliedTheme`). The stored `.Rproj` `EditorTheme` and the project pref value
  are left unchanged, so the override is honored again on a machine that has the theme.
  The Project Options pane preserves the uninstalled name (adds it as a selected item)
  so editing other options and clicking OK does not erase it.
- **`(Default)` selected** — `removeProjectValue` drops the project override; the editor
  reverts to the user's global theme.
- **Theme list not yet loaded when OK is clicked** — `resolveAppliedTheme` returns
  `null`; the live-apply is skipped and the theme applies on the next project open via
  `syncThemePrefs` (mirrors the global pane's existing `themeList_ != null` guard).

## Testing

- **C++ (`rsession` / `core` scope):** `RProjectFile` round-trip test — write a config
  with `editorTheme` set, read it back, assert equality; and assert that an unknown
  sorted field present in the source `.Rproj` survives a write alongside `EditorTheme`
  (guards the churn-preservation behavior).
- **Playwright e2e (`e2e/rstudio/`):** open a project; set the project editor theme via
  the dialog and assert the active editor theme changes live; set it back to `(Default)`
  and assert it reverts to the global theme. Drive via the `window.rstudio` bridge
  (`prefs`, `project`, `commands`).
- **NEWS.md:** add an entry under `### New` referencing
  [#2350](https://github.com/rstudio/rstudio/issues/2350).

## Files touched (summary)

Backend: `RProjectFile.hpp`, `RProjectFile.cpp`, `SessionProjectContext.cpp`,
`SessionProjects.cpp`, `SessionThemes.cpp`.
Frontend: `RProjectConfig.java`, new `ProjectAppearancePreferencesPane.java`,
`ProjectPreferencesDialog.java`, `AppearancePreferencesPane.java`, project pane
resources/constants as needed.
Docs/tests: `NEWS.md`, C++ `RProjectFile` test, Playwright e2e test.
