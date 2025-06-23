/*
 * RSConnectAccount.java
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
package org.rstudio.studio.client.rsconnect.model;

import org.rstudio.studio.client.rsconnect.RSConnect;
import org.rstudio.studio.client.workbench.prefs.model.UserStateAccessor;

public class RSConnectAccount extends UserStateAccessor.PublishAccount
{
   protected RSConnectAccount() 
   {
   }
   
   static public final native RSConnectAccount create(String name, String server) /*-{
      return { 
         "name":   name, 
         "server": server 
      };
   }-*/;
   
   public final boolean isShinyAppsAccount()
   {
      return getServer().compareToIgnoreCase(
            RSConnect.SHINY_APPS_SERVICE_NAME) == 0;
   }

   public final boolean equals(RSConnectAccount other)
   {
      if (other == null)
         return false;
      else return getName()   == other.getName() && 
                  getServer() == other.getServer();
   }
}
