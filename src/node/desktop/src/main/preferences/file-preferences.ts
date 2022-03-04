import { UserDefaultTypes } from 'electron';
import PropertiesReader from 'properties-reader';
import { PlatformPreferences } from './preferences';

class FilePreferences implements PlatformPreferences {
  private properties: PropertiesReader.Reader;

  constructor(file: string) {
    this.properties = PropertiesReader(file);
  }

  getValue(key: string, _type: keyof UserDefaultTypes) {
    return this.properties.get(key) ?? '';
  }

  setValue(_key: string, _type: keyof UserDefaultTypes, _value: keyof UserDefaultTypes): never {
    throw new Error('unimplemented');
  }
}

export default FilePreferences;
