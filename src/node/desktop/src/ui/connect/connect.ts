/* eslint-disable indent */
/* eslint-disable max-len */
/*
 * connect.ts
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

/* eslint-disable @typescript-eslint/no-implicit-any-catch */
import { initI18n } from '../../main/i18n-manager';
import i18next from 'i18next';

const loadPageLocalization = () => {
  initI18n();

  window.addEventListener('load', () => {
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
      'retry_url',
      'warning',
      'rLogo',
    ].map((id) => 'i18n-' + id);

    try {
      document.title = i18next.t('uiFolder.cannotConnectToR');

      i18nIds.forEach((id) => {
        const reducedId = id.replace('i18n-', '');
        const element = document.getElementById(id) as HTMLElement;

        switch (reducedId) {
          case 'retry_url':
            (document.getElementById('retry_url') as HTMLElement).innerHTML = i18next.t('uiFolder.retry');
            break;
          case 'ifYouveCustomizedRSessionCreation':
            (element.innerHTML = i18next.t('uiFolder.' + reducedId)),
              { rProfileFileExtension: '<samp>~/.Rprofile</samp>),' };
            break;
          default:
            element.innerHTML = i18next.t('uiFolder.' + reducedId);
            break;
        }
      });
    } catch (err) {
      console.log('Error occurred when loading i18n: ', err);
    }
  });
};

loadPageLocalization();
