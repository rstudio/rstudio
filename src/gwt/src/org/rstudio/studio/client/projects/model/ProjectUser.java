/*
 * ProjectUser.java
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
package org.rstudio.studio.client.projects.model;

import com.google.gwt.core.client.JavaScriptObject;

public class ProjectUser extends JavaScriptObject
{
   protected ProjectUser()
   {
   }
   
   public final native String getUsername() /*-{
      return this.username;
   }-*/;
   
   public final native String getSessionId() /*-{
      return this.session_id;
   }-*/;
   
   public final native String getClientId() /*-{
      return this.client_id;
   }-*/;
   
   public final native String getColor() /*-{
      return this.color;
   }-*/;
   
   public final native String getCurrentlyEditing() /*-{
      return this.currently_editing;
   }-*/;
   
   public final native int getLastSeen() /*-{
      return this.last_seen;
   }-*/;

   public final native int getTimeSinceLastSeen() /*-{
      return this.time_since_last_seen;
   }-*/;
}
