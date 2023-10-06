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
 * (macOS only)
 *
 * Write the current clipboard image to a local file, and update the Pasteboard
 * with a reference to that local content on disk. This works around an apparent
 * Electron bug with copy + pasting images on the Pasteboard that lack an associated URL.
 */
export declare function writeClipboardImage(): void;

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
