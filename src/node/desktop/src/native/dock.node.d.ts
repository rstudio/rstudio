/**
 * (macOS only)
 *
 * Sets the label on the dock icon.
 *
 * @param label the new label, an empty string clears the label
 */
export declare function setDockLabel(label: string): void;

/**
 * (macOS only)
 *
 * Move a window behind every other window on screen, including those of
 * other applications, without activating or hiding it.
 *
 * @param handle the window's BrowserWindow.getNativeWindowHandle()
 * @returns whether the window was reordered; always false off macOS
 */
export declare function orderWindowBack(handle: Buffer): boolean;
