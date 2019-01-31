/*
 * SessionOpener.java
 *
 * Copyright (C) 2009-19 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.model;

import org.rstudio.studio.client.application.model.ActiveSession;

public class SessionOpener
{
   /**
    * Prepare to navigate to a session URL.
    * @param session session to load
    * @param editExistingLauncherParams if true, shows launcher settings dialog even if we already
    *                                   have launcher settings for this session; otherwise only
    *                                   prompts if we don't have any launcher settings
    * @param navigate action to take when session is ready to load
    */
   public void navigateToSession(ActiveSession session,
                                 boolean editExistingLauncherParams,
                                 Runnable navigate)
   {
      navigate.run();
   }
}
