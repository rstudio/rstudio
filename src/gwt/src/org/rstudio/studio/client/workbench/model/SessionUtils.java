/*
 * SessionUtils.java
 *
 * Copyright (C) 2009-15 by RStudio, Inc.
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

import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;

public class SessionUtils
{
   public static boolean showPublishUi(Session session, UIPrefs prefs)
   {
      return session.getSessionInfo().getAllowRpubsPublish() &&
            prefs.showPublishUi().getValue();
   }
   
   public static boolean showShinyPublishUi(Session session, UIPrefs prefs)
   {
      return session.getSessionInfo().getRSConnectAvailable() &&
            showPublishUi(session, prefs);
   }
}
