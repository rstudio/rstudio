const isLocalStorageItemSet = (key: string) => {
  if (typeof window !== "undefined") {
    const value = window.localStorage.getItem(key);

    return { isSet: value !== "" && !!value, value };
  }

  return { isSet: false, value: "" };
};

export const checkForNewLanguage = () => {
  return new Promise((resolve) => {
    const firstRunTime = new Date().getTime();
    const localeStorageItemKey = "LOCALE";
    const localeLastTimeSetStorageItemKey = "LAST_TIME";

    // This will check every 0.1 seconds if a new language has been set to the local storage
    const isThereNewLanguageInterval = setInterval(() => {
      // After 5 seconds, the default language will be set
      if (new Date().getTime() > firstRunTime + 5000) {
        clearInterval(isThereNewLanguageInterval);
        resolve("en");
      } else {
        const localeLastTimeData = isLocalStorageItemSet(localeLastTimeSetStorageItemKey);

        // If a new Language is set, the last time it was set
        // will be checked against the time this function has first ran
        if (localeLastTimeData.isSet) {
          const localeData = isLocalStorageItemSet(localeStorageItemKey);

          if (
            localeData.isSet &&
            firstRunTime <= parseInt("" + localeLastTimeData.value, 10) + 3000
          ) {
            clearInterval(isThereNewLanguageInterval);

            resolve("" + localeData.value);
          }
        }
      }
    }, 100);
  });
};

/**
 * Removes duplicate separators from a path, given a separator for search.
 *
 * @param {string} path
 * @param {string} [separator='/']
 * @return {*} 
 */
function normalizeSeparators(path: string, separator = '/') {
  return path.replace(/[\\/]+/g, separator);
}

/**
 * Removes duplicated separators from a path based on platform.
 *
 * @export
 * @param {string} _path
 * @return {*} 
 */
export function normalizeSeparatorsNative(_path: string) {
  /* using conditional to set the separator based on platform as `path` is not available here */
  const separator = process.platform === 'win32' ? '\\' : '/';
  return normalizeSeparators(_path, separator);
}
