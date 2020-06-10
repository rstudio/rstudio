/*
 * SessionUtils.java
 *
 * Copyright (C) 2020 by RStudio, PBC
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

import org.rstudio.studio.client.workbench.prefs.model.UserState;

public class SessionUtils
{
   public static boolean showPublishUi(Session session, UserState state)
   {
      return session.getSessionInfo().getAllowPublish() &&
            state.showPublishUi().getValue();
   }
   
   // Whether to show UI that publishes content to an external service. Note
   // that the server takes care of ensuring that this is false if showPublishUi
   // is false, so it's unnecessary to check both values.
   public static boolean showExternalPublishUi(Session session, UserState state)
   {
      return session.getSessionInfo().getAllowExternalPublish() &&
            state.showPublishUi().getValue();
   }
}
