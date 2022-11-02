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
