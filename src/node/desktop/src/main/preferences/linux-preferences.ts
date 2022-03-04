import { UserDefaultTypes } from 'electron';
import { PlatformPreferences } from './preferences';

class LinuxPreferences implements PlatformPreferences {
  getValue(key: string, type: keyof UserDefaultTypes) {
    return '';
  }

  setValue(_key: string, _type: keyof UserDefaultTypes, _value: keyof UserDefaultTypes): never {
    throw Error('unimplemented');
  }
}

export default LinuxPreferences;
