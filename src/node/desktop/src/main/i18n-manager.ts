/* eslint-disable @typescript-eslint/no-implicit-any-catch */
/*
 * i18n-manager.ts
 *
 * Copyright (C) 2022 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

import i18next from 'i18next';
import * as en from '../assets/locales/en.json';
import * as fr from '../assets/locales/fr.json';

// Initialize the translation resources, as required for a document.
const initI18n = () => {
  i18next
    .init({
      resources: {
        en: { translation: en },
        fr: { translation: fr },
      },
      fallbackLng: 'en',
    })
    .catch((err) => {
      console.log('Error when initializing i18next: ');
      console.log(err);
    });
};

const changeLanguage = async (language = 'en') => {
  return i18next.changeLanguage(language).catch((err) => {
    console.log('Something went wrong changing language to ' + language, err);
  });
};

/**
 * Localize a document
 *
 * Find document elements containing translatable text,
 * that is, elements with the 'data-18n-id' attribute set,
 * and replace the contents of those elements with the appropriate
 * translated equivalent (if any).
 *
 * @param document The document, whose elements will be translated.
 * @param scope The scope in which translations are defined.
 */
function localize(document: Document, scope: string): void {
  try {
    localizeImpl(document, scope);
  } catch (err) {
    const prefix = i18next.t('i18nManager.errorLocalizingDocument');
    console.log(`${prefix}: ${err}`);
  }
}

function localizeImpl(document: Document, scope: string): void {
  // find translatable elements, and translate them
  const elements = document.querySelectorAll('[data-i18n]');
  elements.forEach((el) => {
    const id = el.getAttribute('data-i18n') as string;
    const fullId = id.indexOf('.') === -1 ? `${scope}.${id}` : id;
    if (i18next.exists(fullId)) {
      el.innerHTML = i18next.t(fullId);
    }
  });
}

export { initI18n, localize, changeLanguage };
