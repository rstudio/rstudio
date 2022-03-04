import { systemPreferences, UserDefaultTypes } from 'electron';
import { PlatformPreferences } from './preferences';

/**
 * MacOS specific implementation
 *
 * The preferences are stored in defauls, which is MacOS specific.
 */
class MacPreferences implements PlatformPreferences {
  getValue(key: string, type: keyof UserDefaultTypes) {
    return systemPreferences.getUserDefault(key, type);
  }

  setValue(_key: string, _type: keyof Electron.UserDefaultTypes, _value: string): never {
    throw new Error('unimplemented');
  }
}

export default MacPreferences;
