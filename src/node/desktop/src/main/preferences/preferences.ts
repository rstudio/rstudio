import { UserDefaultTypes } from 'electron';
import { homedir } from 'os';
import FilePreferences from './file-preferences';
import MacPreferences from './mac-preferences';

export interface PlatformPreferences {
  getValue(
    key: string,
    type?: keyof UserDefaultTypes,
  ): string | number | boolean | unknown[] | Record<string, unknown> | undefined;
  setValue(key: string, type: keyof UserDefaultTypes, value: string): void;
}

const isMacOS = process.platform === 'darwin';

/**
 * MacOS stores periods as · in defaults. When adding a new key, ensure that the key is valid
 * on all platforms.
 */
export const preferenceKeys = {
  fontFixedWidth: isMacOS ? 'font·fixedWidth' : 'General.font.fixedWidth',
};

export let preferenceManager: PlatformPreferences;

switch (process.platform) {
  case 'darwin':
    preferenceManager = new MacPreferences();
    break;
  case 'win32':
    preferenceManager = new FilePreferences(`${homedir()}\\AppData\\Roaming\\RStudio\\desktop.ini`);
    break;
  case 'linux':
    preferenceManager = new FilePreferences(`${homedir()}/.config/RStudio/desktop.ini`);
    break;
  default:
    throw new Error('unsupported platform');
}
