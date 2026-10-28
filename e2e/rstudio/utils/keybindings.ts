// Keybindings live in <config home>/keybindings/. The editor bindings are in
// editor_bindings.json, but the Keyboard Shortcuts dialog's Reset button also
// rewrites the app and addin bindings, so all three are handled. Desktop and
// spawned-server workers run with a sandboxed RSTUDIO_CONFIG_HOME, but an
// external server shares its config with the developer, so any pre-existing
// files are set aside and restored through the R session.

const BINDINGS_DIR = [
  'file.path(Sys.getenv("RSTUDIO_CONFIG_HOME",',
  '  unset = file.path(Sys.getenv("XDG_CONFIG_HOME", unset = "~/.config"), "rstudio")),',
  '  "keybindings")',
].join(' ');

const BINDINGS_FILES =
  `file.path(${BINDINGS_DIR}, c("editor_bindings.json", "rstudio_bindings.json", "addins.json"))`;

/** R code: move any existing keybindings files aside. Run via executeInConsole(). */
export const BACKUP_BINDINGS = [
  `for (f in ${BINDINGS_FILES})`,
  'if (file.exists(f)) file.rename(f, paste0(f, ".e2e-backup"))',
].join(' ');

/** R code: drop whatever the test wrote and put the originals back. */
export const RESTORE_BINDINGS = [
  `for (f in ${BINDINGS_FILES}) {`,
  'unlink(f);',
  'if (file.exists(paste0(f, ".e2e-backup"))) file.rename(paste0(f, ".e2e-backup"), f)',
  '}',
].join(' ');

/**
 * R code: write custom app command bindings (command id -> shortcut, e.g.
 * "Ctrl+Alt+Shift+K") to rstudio_bindings.json. Run BACKUP_BINDINGS first.
 */
export function writeAppBindings(bindings: Record<string, string>): string {
  const json = JSON.stringify(bindings).replace(/\\/g, '\\\\').replace(/'/g, "\\'");
  return [
    `local({ d <- ${BINDINGS_DIR};`,
    'dir.create(d, recursive = TRUE, showWarnings = FALSE);',
    `writeLines('${json}', file.path(d, "rstudio_bindings.json")) })`,
  ].join(' ');
}
