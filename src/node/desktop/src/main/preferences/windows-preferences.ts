import { UserDefaultTypes } from 'electron';
import { PlatformPreferences } from './preferences';
import PropertiesReader from 'properties-reader';
import { homedir } from 'os';

class WindowsPreferences implements PlatformPreferences {
  private static PREFERENCE_FILE = `${homedir()}\\AppData\\Roaming\\RStudio\\desktop.ini`;
  private properties: PropertiesReader.Reader;

  constructor() {
    this.properties = PropertiesReader(WindowsPreferences.PREFERENCE_FILE);
  }

  getValue(key: string, _type: keyof UserDefaultTypes) {
    return this.properties.get(key) ?? '';
  }

  setValue(_key: string, _type: keyof UserDefaultTypes, _value: keyof UserDefaultTypes): never {
    throw Error('unimplemented');
  }
}

export default WindowsPreferences;
