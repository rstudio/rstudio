/*
 * RSConnectApplicationInfo.java
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

public class RSConnectApplicationInfo extends JavaScriptObject
{
   protected RSConnectApplicationInfo() 
   {
   }
   
   public final native double getId() /*-{
      return this.id;
   }-*/;
   
   public final native String getName() /*-{
      return this.name;
   }-*/;
   
   public final native String getTitle() /*-{
      return this.title;
   }-*/;

   public final native String getUrl() /*-{
      return this.url;
   }-*/;
   
   public final native String getConfigUrl() /*-{
      return this.config_url;
   }-*/;

   public final native String getStatus() /*-{
      return this.status;
   }-*/;

   public final native String getCreatedTime() /*-{
      return this.created_time;
   }-*/;

   public final native String getUpdatedTime() /*-{
      return this.updated_time;
   }-*/;
}
