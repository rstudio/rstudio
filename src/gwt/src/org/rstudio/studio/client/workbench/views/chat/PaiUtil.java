/*
 * PaiUtil.java
 *
 * Copyright (C) 2026 by Posit Software, PBC
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
package org.rstudio.studio.client.workbench.views.chat;

import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;

/**
 * Utility class for Posit AI (PAI) feature availability checks.
 */
public class PaiUtil
{
   /**
    * Returns true if the Posit AI feature is enabled. This requires both:
    * 1. The allow-pai admin option (always true in open-source, configurable in Pro)
    * 2. The pai user preference (temporary, will be removed when feature is ready)
    *
    * @param sessionInfo The session info containing admin settings
    * @param userPrefs The user preferences
    * @return true if PAI is enabled, false otherwise
    */
   public static boolean isPaiEnabled(SessionInfo sessionInfo, UserPrefs userPrefs)
   {
      return sessionInfo.getAllowPai() && userPrefs.pai().getGlobalValue();
   }
}
