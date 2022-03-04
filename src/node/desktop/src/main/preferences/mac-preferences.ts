import { systemPreferences, UserDefaultTypes } from 'electron';
import { PlatformPreferences } from './preferences';

class MacPreferences implements PlatformPreferences {
  getValue(key: string, type: keyof UserDefaultTypes) {
    return systemPreferences.getUserDefault(key, type);
  }

  setValue(_key: string, _type: keyof UserDefaultTypes, _value: keyof UserDefaultTypes): never {
    throw Error('unimplemented');
  }
}

export default MacPreferences;
