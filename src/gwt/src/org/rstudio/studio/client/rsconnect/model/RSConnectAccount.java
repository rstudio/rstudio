/*
 * RSConnectAccount.java
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
package org.rstudio.studio.client.rsconnect.model;

import org.rstudio.studio.client.rsconnect.RSConnect;

import com.google.gwt.core.client.JavaScriptObject;

public class RSConnectAccount extends JavaScriptObject
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
   
   public final native String getName() /*-{
      return this.name || "";
   }-*/;

   public final native String getServer() /*-{
      return this.server || "";
   }-*/;
   
   public final boolean isCloudAccount()
   {
      return getServer().compareToIgnoreCase(
            RSConnect.CLOUD_SERVICE_NAME) == 0;
   }

   public final boolean equals(RSConnectAccount other)
   {
      if (other == null)
         return false;
      else return getName().equals(other.getName()) && 
             getServer().equals(other.getServer());
   }
}