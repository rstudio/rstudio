/**
 * (macOS only)
 *
 * Clean the clipboard following a copy operation.
 * This function is used primarily to ensure that the content on the
 * clipboard is UTF-8 encoded, rather than UTF-16 encoded.
 *
 * @param stripHtml Should HTML placed on the clipboard be removed?
 */
export declare function cleanClipboard(stripHtml: boolean): void;

/**
 * (Windows only)
 *
 * Convert a file path into a Windows short path name if possible.
 */
export declare function shortPathName(path: string): string;

/**
 * (Windows only)
 *
 * Detect if the CTRL key is currently being held down.
 */
export declare function isCtrlKeyDown(): boolean;

/**
 * (Windows only)
 *
 * Return the path for the current user's My Documents directory.
 */
export declare function currentCSIDLPersonalHomePath(): string;

/**
 * (Windows only)
 *
 * Return the path for the default My Documents directory and force creation if needed.
 */
export declare function defaultCSIDLPersonalHomePath(): string;

/**
 * (Windows only)
 *
 * Finds R installations on the system by enumerating the registry.
 */
export declare function searchRegistryForInstallationsOfR(): string[];

/**
 * (Windows only)
 *
 * Find the default version of R.
 *
 * @param registryVersionKey The registry version key -- typically 'R' or 'R64'.
 */
export declare function searchRegistryForDefaultInstallationOfR(registryVersionKey: string): string;

/**
 * (Windows only)
 *
 * Open a file using the default application registered for that file.
 *
 * @param path The path to an existing file.
 */
export declare function openExternal(path: string): void;

/**
 * (Windows only)
 *
 * List monospace fonts available on the system.
 *
 */
export declare function win32ListMonospaceFonts(): string[];

/**
 * (Windows only)
 *
 * Watch for native dialogs created by the given process, raising each new
 * dialog above our windows. Replaces any watch registered by a previous
 * call. Must be called from the Electron main thread (events are delivered
 * via its message loop); failure to install the watch is silent (logged
 * only when RS_LOG_LEVEL=debug).
 *
 * @param pid The process to watch. A pid of 0 stops any existing watch
 *   without registering a new one.
 */
export declare function win32WatchSessionDialogs(pid: number): void;

/**
 * (Windows only)
 *
 * Stop watching for session dialogs. Safe to call when no watch is active.
 */
export declare function win32StopWatchingSessionDialogs(): void;

/**
 * (macOS only)
 *
 * List all fonts available on the system in a single pass.
 * Returns an object with monospace and proportional font family names.
 */
export declare function macOSListFonts(): {
  monospace: string[];
  proportional: string[];
};
