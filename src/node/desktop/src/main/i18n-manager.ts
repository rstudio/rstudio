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

const initI18n = () => {
  i18next
    .init({
      resources: {
        en: { translation: en },
      },
      fallbackLng: 'en',
    })
    .catch((err) => {
      console.log('Error when initializing i18next: ');
      console.log(err);
    });
};

export { initI18n };
