/**
 * Port of the element-id helpers in ElementIds.java, so Agent Loki can build
 * the same ids the GWT frontend assigns.
 *
 * Kept faithful rather than tidied: if these drift from the Java the tool
 * silently stops finding the elements it means to act on.
 */

/** ElementIds.ID_PREFIX. */
const ID_PREFIX = 'rstudio_';

/** ElementIds.COMMAND_ENTRY_PREFIX. */
const COMMAND_ENTRY_PREFIX = 'command_entry_';

/** CommandPalette.SCOPE_APP_COMMAND. The other scopes are never invoked. */
const SCOPE_APP_COMMAND = 'command';

/**
 * ElementIds.idSafeString: substitute CPP for the first "C++", replace every
 * non-alphanumeric with an underscore, collapse runs, trim the ends, lowercase.
 */
export function idSafeString(text: string): string {
  let out = text;
  if (out.includes('C++'))
    out = out.replace('C++', 'CPP');
  return out
    .replace(/[^a-zA-Z0-9]/g, '_')
    .replace(/_+/g, '_')
    .replace(/^_+/, '')
    .replace(/_+$/, '')
    .toLowerCase();
}

/**
 * The Command Palette row for an AppCommand.
 * CommandPaletteEntry.initialize builds it from the prefix, the scope, and the
 * id-safe command id; the visible text lives in the `_label` child.
 */
export function paletteEntryId(commandId: string): string {
  return `${ID_PREFIX}${COMMAND_ENTRY_PREFIX}${SCOPE_APP_COMMAND}_${idSafeString(commandId)}`;
}

/** The toolbar button for a command, when it has one. */
export function toolbarButtonId(commandId: string): string {
  return `${ID_PREFIX}tb_${idSafeString(commandId)}`;
}

/** The main-menu item for a command label (AppCommand gives each one this id). */
export function menuItemId(label: string): string {
  return `${ID_PREFIX}label_${idSafeString(label)}`;
}
