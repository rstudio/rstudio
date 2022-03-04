import { UserDefaultTypes } from 'electron';
import MacPreferences from './mac-preferences';

export interface PlatformPreferences {
  getValue(
    key: string,
    type?: keyof UserDefaultTypes,
  ): string | number | boolean | unknown[] | Record<string, unknown> | undefined;
  setValue(key: string, type: keyof UserDefaultTypes, value: string): void;
}

class PreferenceManager implements PlatformPreferences {
  private platformPreferences: PlatformPreferences;

  constructor() {
    switch (process.platform) {
      case 'darwin':
        this.platformPreferences = new MacPreferences();
        break;
      default:
        throw Error('unsupported platform');
    }
  }

  getValue(
    key: string,
    type?: keyof UserDefaultTypes,
  ): string | number | boolean | unknown[] | Record<string, unknown> | undefined {
    return this.platformPreferences.getValue(key, type);
  }

  setValue(key: string, type: keyof UserDefaultTypes, value: string): void {
    this.platformPreferences.setValue(key, type, value);
  }
}

export default PreferenceManager;
