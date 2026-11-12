/*
 * connect.ts
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

import { changeLanguage, initI18n } from '../../main/i18n-manager';
import i18next from 'i18next';

const loadPageLocalization = () => {
  initI18n();

  const updateLabels = () => {
    const i18nIds = [
      'cannotConnectToR',
      'rstudioCantEstablishAConnectionToR',
      'theRSessionIsTakingAnUnusuallyLongTimeToStart',
      'rstudioIsUnableToCommunicateWithR',
      'pleaseTryTheFollowing',
      'theRSessionIsTakingAnUnusuallyLongTimeToStart',
      'ifYouveCustomizedRSessionCreation',
      'ifYouAreUsingAFirewallOrAntivirus',
      'runRguiRAppOrRInATerminalToEnsure',
      'furtherTroubleshootingHelpCanBeFound',
      'troubleshootingRstudioStartup',
      'warning',
      'rLogo',
    ].map((id) => 'i18n-' + id);

    try {
      document.title = i18next.t('uiFolder.cannotConnectToR');

      i18nIds.forEach((id) => {
        const reducedId = id.replace('i18n-', '');
        const elements = document.querySelectorAll(`[id=${id}]`);

        elements.forEach((element) => {
          switch (reducedId) {
            case 'ifYouveCustomizedRSessionCreation':
              element.innerHTML = i18next.t('uiFolder.' + reducedId, {
                rProfileFileExtension: '<samp>~/.Rprofile</samp>',
              });
              break;
            default:
              element.innerHTML = i18next.t('uiFolder.' + reducedId);
              break;
          }
        });
      });

      // the Retry link keeps the id that connect.html's script uses to set its URL
      (document.getElementById('retry_url') as HTMLElement).textContent = i18next.t('uiFolder.retry');
    } catch (err) {
      console.log('Error occurred when loading i18n: ', err);
    }
  };

  window.addEventListener('load', () => {
    void changeLanguage(window.desktopLanguage).then(updateLabels);
  });
};

loadPageLocalization();
