# Electron-to-Playwright Migration Progress

Source: `rstudio-ide-automation/rstudio_server_pro/electron-tests/`
Target: `e2e/rstudio/tests/`

## Summary

| Metric | Count |
|--------|-------|
| Total electron test files | 32 |
| Total electron test methods | 115 |
| Files fully converted | 15 |
| Files partially converted | 0 |
| Files not started | 17 |

## Conversion Status

### Root-level tests (11 files, 57 methods)

| Electron Source | Methods | Playwright Target | Status | Notes |
|----------------|---------|-------------------|--------|-------|
| test_desktop_Citations.py | 17 | — | Not started | |
| test_desktop_Command_Palette.py | 2 | panes/misc/command-palette.test.ts (2) | Complete | |
| test_desktop_console.py | 11 | panes/console/console_pane.test.ts (8), panes/console/console_command_effects.test.ts (7), panes/console/execute_from_editor.test.ts (1) | Complete | Split by theme; added Find in Console coverage (3 new tests) and upgraded `help.start()` to verify help-pane contents |
| test_desktop_EnvironmentPane.py | 5 | — | Not started | |
| test_desktop_FindInFiles.py | 3 | panes/misc/find-in-files.test.ts (3) | Complete | |
| test_desktop_Package_Installation.py | 1 | — | Not started | |
| test_desktop_PlotsPane.py | 11 | — | Not started | |
| test_desktop_R.py | 1 | — | Not started | |
| test_desktop_R_Session_Restart.py | 1 | — | Not started | |
| test_desktop_terminal.py | 4 | — | Not started | |
| test_desktop_ViewerPane.py | 1 | panes/viewer/htmlwidgets.test.ts (1) | Complete | |

### EditorPane (10 files, 37 methods)

| Electron Source | Methods | Playwright Target | Status | Notes |
|----------------|---------|-------------------|--------|-------|
| test_desktop_CodeSuggestions.ts | — | panes/editor/code_suggestions.test.ts (5) | Complete | Already TypeScript in electron-tests |
| test_desktop_Copilot.py | 12 | panes/editor/copilot_ghost_text.test.ts (22) | Complete | Restructured with loops; test_enabling_copilot not included |
| test_desktop_Create_File_Types.py | 6 | panes/editor/create_file_types.test.ts (19) | Complete | Data-driven; 15 no-modal + 4 modal tests; includes Sweave creation. TODO: 3 WIP types from Selenium still need conversion (newRDocumentationDoc, newRPlumberDoc, newRPresentationDoc) — each requires extra dialog fields |
| test_desktop_DataViewer.py | 4 | panes/data-viewer/pagination-sorting.test.ts (4) | Complete | |
| test_desktop_Markdown.py | 2 | panes/editor/markdown.test.ts (2) | Complete | |
| test_desktop_Quarto.py | 1 | panes/editor/quarto.test.ts (1) | Complete | |
| test_desktop_RMarkdown.py | 7 | panes/editor/rmarkdown.test.ts (6) | Complete | |
| test_desktop_RNotebook.py | 1 | panes/editor/rnotebook.test.ts (1) | Complete | |
| test_desktop_Sweave.py | 2 | panes/editor/sweave.test.ts (2) | Complete | Creation test covered by create_file_types; added PDF compile test |
| test_desktop_SyntaxHighlighting.py | 1 | panes/editor/syntax-highlighting.test.ts (1) | Complete | |

### GlobalPrefs (10 files, 19 methods)

| Electron Source | Methods | Playwright Target | Status | Notes |
|----------------|---------|-------------------|--------|-------|
| test_desktop_GlobalPrefAccessibility.py | 6 | — | Not started | |
| test_desktop_GlobalPrefAppearance.py | 1 | — | Not started | |
| test_desktop_GlobalPrefCode.py | 2 | — | Not started | |
| test_desktop_GlobalPrefGeneral.py | 3 | — | Not started | |
| test_desktop_GlobalPrefPackages.py | 2 | — | Not started | |
| test_desktop_GlobalPrefPaneLayout.py | 1 | — | Not started | |
| test_desktop_GlobalPrefRMarkdown.py | 1 | — | Not started | |
| test_desktop_GlobalPrefSpelling.py | 1 | — | Not started | |
| test_desktop_GlobalPrefSweave.py | 1 | — | Not started | |
| test_desktop_GlobalPrefTerminal.py | 1 | — | Not started | |

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
