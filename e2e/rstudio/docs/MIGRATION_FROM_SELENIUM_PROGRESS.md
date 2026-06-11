# Electron-to-Playwright Migration Progress

Source: `rstudio-ide-automation/rstudio_server_pro/electron-tests/`
Target: `e2e/rstudio/tests/`

## Summary

| Metric | Count |
|--------|-------|
| Total electron test files | 32 |
| Total electron test methods | 115 |
| Files fully converted | 30 |
| Files partially converted | 0 |
| Files not started | 6 |

## Conversion Status

### Root-level tests (11 files, 57 methods)

| Electron Source | Methods | Playwright Target | Status | Notes |
|----------------|---------|-------------------|--------|-------|
| test_desktop_Citations.py | 17 | panes/editor/citations.test.ts (to be scrapped) | Failed miserably (restart from scratch) | First attempt failed miserably: only 6 of 18 tests passing, a mid-session refactor regressed a 9-passing state, and Quarto never reaches visual mode. Scrapping the file and redoing the migration from scratch. |
| test_desktop_Command_Palette.py | 2 | panes/misc/command-palette.test.ts (2) | Complete | Open/filter/run (Escape closes; "script" keeps R + SQL script, drops text file; Enter runs the front-loaded New R Script), and the inline native-pipe pref toggle (verified by inserting "\|>"). Both cross-mode. |
| test_desktop_console.py | 11 | panes/console/console_pane.test.ts (8), panes/console/console_command_effects.test.ts (7), panes/console/execute_from_editor.test.ts (1) | Complete | Split by theme; added Find in Console coverage (3 new tests) and upgraded `help.start()` to verify help-pane contents |
| test_desktop_EnvironmentPane.py | 5 | panes/environment/environment_pane.test.ts (5) | Complete | Toolbar elements, the import-dataset/object-view/environment-list dropdowns, and memory-pie growth + usage-report modal. No Desktop-only assumptions; the Selenium maximize/restore workaround was dropped. |
| test_desktop_FindInFiles.py | 3 | panes/misc/find_in_files_replace.test.ts (3) | Complete | Folded 2 new cross-mode tests into the existing spec: the search-options dialog (case/whole-word/regex), and a multi-file search (3 files, term in 2 of 3) with the search/replace toolbar toggle. The third method (Replace All) was already covered there. |
| test_desktop_Package_Installation.py | 1 | panes/console/package_installation.test.ts (1) | Complete | |
| test_desktop_PlotsPane.py | 11 | panes/plots/plots_pane.test.ts (11) | Complete | All 11 tests including the 3 that were commented out in the desktop source (export dropdown, save as image, save as PDF). Two-step save flow (OK + GWT file chooser) with R file-exists verification. Zoom and resize tests are @desktop_only. |
| test_desktop_R.py | 1 | panes/editor/r_execution.test.ts (1) | Complete | |
| test_desktop_R_Session_Restart.py | 1 | panes/console/r_session_restart.test.ts (1) | Complete | |
| test_desktop_terminal.py | 4 | panes/terminal/terminal.test.ts (4) | Complete | Added to existing terminal spec: toolbar next/previous buttons, R --version output, file create and ls, Shift+Backspace character deletion (@desktop_only). Uses rstudioapi::terminalBuffer for all output assertions. |
| test_desktop_ViewerPane.py | 1 | panes/viewer/htmlwidgets.test.ts (1) | Complete | plotly htmlwidget auto-displays in the Viewer pane; asserts the text-trace label renders in the iframe |

### EditorPane (10 files, 37 methods)

| Electron Source | Methods | Playwright Target | Status | Notes |
|----------------|---------|-------------------|--------|-------|
| test_desktop_CodeSuggestions.ts | — | panes/editor/code_suggestions.test.ts (5) | Complete | Already TypeScript in electron-tests |
| test_desktop_Copilot.py | 12 | panes/editor/copilot_ghost_text.test.ts (22) | Complete | Restructured with loops; test_enabling_copilot not included |
| test_desktop_Create_File_Types.py | 6 | panes/editor/create_file_types.test.ts (19) | Not started | Data-driven; 15 no-modal + 4 modal tests; includes Sweave creation. TODO: 3 WIP types from Selenium still need conversion (newRDocumentationDoc, newRPlumberDoc, newRPresentationDoc) — each requires extra dialog fields |
| test_desktop_DataViewer.py | 4 | panes/data-viewer/pagination-sorting.test.ts (4) | Complete | |
| test_desktop_Markdown.py | 2 | panes/editor/markdown.test.ts (2) | Complete | newMarkdownDoc creation, plus previewHTML to the Viewer pane with rendered-text assertions |
| test_desktop_Quarto.py | 1 | panes/editor/quarto.test.ts (1) | Complete | |
| test_desktop_RMarkdown.py | 7 | panes/editor/rmarkdown.test.ts (6) | Complete | |
| test_desktop_RNotebook.py | 1 | panes/editor/rnotebook.test.ts (1) | Complete | Preview displays the .nb.html sidecar written by the editor save hook, so the doc is saved before previewing; preview click retried for the first-click-does-nothing quirk |
| test_desktop_Sweave.py | 2 | panes/editor/sweave.test.ts (2) | Complete | Creation test covered by create_file_types; added PDF compile test |
| test_desktop_SyntaxHighlighting.py | 1 | panes/editor/syntax-highlighting.test.ts (1) | Complete | |

### GlobalPrefs (10 files, 19 methods)

| Electron Source | Methods | Playwright Target | Status | Notes |
|----------------|---------|-------------------|--------|-------|
| test_desktop_GlobalPrefAccessibility.py | 6 | preferences/global_prefs_accessibility.test.ts (6) | Complete | Keyboard a11y of the Global Options dialog: initial focus, Arrow-key section navigation, and Tab/Shift+Tab focus wrapping (including wrap-to-current-section after changing panes). Cross-mode. Fixed two latent Selenium assertion bugs (`assert focused, x` -> proper focus checks) and added the `console_options` tab selector. |
| test_desktop_GlobalPrefAppearance.py | 1 | preferences/global_prefs_panels.test.ts (1) | Complete | |
| test_desktop_GlobalPrefCode.py | 2 | preferences/global_prefs_panels.test.ts (2) | Complete | |
| test_desktop_GlobalPrefGeneral.py | 3 | preferences/global_prefs_panels.test.ts (1) | Complete | 2 WIP methods were commented out in the Python source; not migrated |
| test_desktop_GlobalPrefPackages.py | 2 | preferences/global_prefs_panels.test.ts (1) | Complete | 1 method was commented out in the Python source; not migrated |
| test_desktop_GlobalPrefPaneLayout.py | 1 | preferences/global_prefs_panels.test.ts (1) | Complete | |
| test_desktop_GlobalPrefRMarkdown.py | 1 | preferences/global_prefs_panels.test.ts (1) | Complete | |
| test_desktop_GlobalPrefSpelling.py | 1 | preferences/global_prefs_panels.test.ts (1) | Complete | |
| test_desktop_GlobalPrefSweave.py | 1 | preferences/global_prefs_panels.test.ts (1) | Complete | |
| test_desktop_GlobalPrefTerminal.py | 1 | preferences/global_prefs_panels.test.ts (1) | Complete | Also includes 2 new tests (Python panel, Assistant panel) with no Selenium equivalent |

### Licensing (4 files, 9 methods)

| Electron Source | Methods | Playwright Target | Status | Notes |
|----------------|---------|-------------------|--------|-------|
| test_desktop_activate_license.py | 3 | — | Not started | |
| test_desktop_license.py | 2 | — | Not started | |
| test_desktop_License_Diagnostics.py | 2 | — | Not started | |
| test_desktop_remove_license.py | 2 | — | Not started | |

### Projects (1 file, 5 methods)

| Electron Source | Methods | Playwright Target | Status | Notes |
|----------------|---------|-------------------|--------|-------|
| test_desktop_Create_Projects.py | 5 | projects/create_projects.test.ts (5) | Complete | |

## Fixme Tests

| Playwright File | Test Name | Blocker | Notes |
|----------------|-----------|---------|-------|
| — | — | — | None tracked yet |
