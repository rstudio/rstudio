/*
 * RSConnectServerInfo.java
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
package org.rstudio.studio.client.rsconnect.model;

import com.google.gwt.core.client.JavaScriptObject;

public class RSConnectServerInfo extends JavaScriptObject
{
   protected RSConnectServerInfo()
   {
   }
   
   public final native boolean isValid() /*-{
      return this.valid;
   }-*/;

   public final native String getMessage() /*-{
      return this.message || "";
   }-*/;

   public final native String getVersion() /*-{
      return this.version || "";
   }-*/;

   public final native String getAbout() /*-{
      return this.about || "";
   }-*/;
   
   public final native String getUrl() /*-{
      return this.url || "";
   }-*/;

   public final native String getName() /*-{
      return this.name || "";
   }-*/;
   
   public final String getInfoString() 
   {
      return "Server: " + getName() + " (" + getUrl() + ")\n" + 
             "Version: " + getVersion() + "\n" + 
             "About: " + getAbout() + "\n";
   }
}
