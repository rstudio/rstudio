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
import org.rstudio.studio.client.workbench.prefs.model.UserPrefsAccessor;

/**
 * Utility class for Posit AI (PAI) feature availability checks.
 */
public class PaiUtil
{
   /**
    * Returns true if the Posit AI feature is enabled.
    *
    * @param sessionInfo The session info containing admin settings
    * @param userPrefs The user preferences
    * @return true if PAI is enabled, false otherwise
    */
   public static boolean isPaiEnabled(SessionInfo sessionInfo, UserPrefs userPrefs)
   {
      return sessionInfo.getPositAssistantEnabled();
   }

   /**
    * Returns true if the user has selected Posit AI as their assistant.
    * Use this to gate features that should only be active when PAI is selected.
    *
    * @param userPrefs The user preferences
    * @return true if user has selected Posit AI, false otherwise
    */
   public static boolean isPaiSelected(UserPrefs userPrefs)
   {
      return userPrefs.assistant().getGlobalValue()
            .equals(UserPrefsAccessor.ASSISTANT_POSIT);
   }
}
