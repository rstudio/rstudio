const isLocalStorageItemSet = (key: string) => {
  const value = window.localStorage.getItem(key);

  return { isSet: value !== '' && !!value, value };
};

export const checkForNewLanguage = () => {
  return new Promise((resolve, reject) => {
    const now = new Date().getTime();
    const localeStorageItemKey = 'LOCALE';
    const localeLastTimeSetStorageItemKey = 'LAST_TIME';

    const isThereNewLanguageInterval = setInterval(() => {
      const localeLastTimeData = isLocalStorageItemSet(localeLastTimeSetStorageItemKey);

      if (localeLastTimeData.isSet) {
        const localeData = isLocalStorageItemSet(localeStorageItemKey);

        if (localeData.isSet && now > parseInt('' + localeLastTimeData.value, 10)) {
          clearInterval(isThereNewLanguageInterval);
          const newLanguage = localeData.value;

          resolve('' + newLanguage);
        }
      }
    }, 100);

    if (new Date().getTime() > now + 30000) {
      clearInterval(isThereNewLanguageInterval);

      resolve('en');
    }
  });
};
