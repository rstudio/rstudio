import i18n, { TFunction } from 'i18next';
import Backend from 'i18next-xhr-backend';
import LanguageDetector from 'i18next-browser-languagedetector';
import { initReactI18next } from 'react-i18next';

export async function i18nInit(): Promise<TFunction> {
  t = await i18n
    .use(Backend)
    .use(LanguageDetector)
    .use(initReactI18next)
    .init({
      fallbackLng: 'en',
      debug: false,
      ns: ['translations', 'commands'],
      defaultNS: 'translations',
      backend: {
        loadPath: '/locales/en/{{ns}}.json',
      },
      keySeparator: false,
      interpolation: {
        escapeValue: false,
      },
    });

  return t;
}

export let t: TFunction;
