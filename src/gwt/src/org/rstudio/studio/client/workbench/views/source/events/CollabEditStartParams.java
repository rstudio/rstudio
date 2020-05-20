/*
 * CollabEditStartParams.java
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

package org.rstudio.studio.client.workbench.views.source.events;

import com.google.gwt.core.client.JavaScriptObject;

public class CollabEditStartParams extends JavaScriptObject
{
   protected CollabEditStartParams() 
   {
   }
   
   public final native String getUrl() /*-{
      return this.url;
   }-*/;
   
   public final native String getPath() /*-{
      return this.path;
   }-*/;
   
   public final native String getId() /*-{
      return this.id;
   }-*/;
   
   public final native String cursorColor() /*-{
      return this.cursor_color;
   }-*/;
   
   public final native boolean isMaster() /*-{
      return this.master;
   }-*/;
   
   public final native boolean isRejoining() /*-{
      return this.rejoining;
   }-*/;
   
   public final native String getHostNode() /*-{
      return this.host_node;
   }-*/;
}
