/*
 * ShinyApplicationParams.java
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
package org.rstudio.studio.client.shiny.model;

import org.rstudio.core.client.js.JsObject;

import com.google.gwt.core.client.JavaScriptObject;

public class ShinyApplicationParams extends JavaScriptObject
{
   protected ShinyApplicationParams() {}
   
   public final static String STATE_STARTED = "started";
   public final static String STATE_STOPPING = "stopping";
   public final static String STATE_STOPPED = "stopped";
   public final static String STATE_RELOADING = "reloading";

   public final static String ID_FOREGROUND = "foreground";
   
   public native static ShinyApplicationParams create(String path,
                                                      String id,
                                                      String url,
                                                      String state) /*-{
      return {
         path: path,
         id: id,
         url: url,
         state: state, 
         viewer: 0
      };
   }-*/;
   
   public final native String getPath() /*-{
      return this.path;
   }-*/;

   public final native String getId() /*-{
      return this.id;
   }-*/;

   public final native String getUrl() /*-{
      return this.url;
   }-*/;
   
   public final native String getViewerType() /*-{
      return this.viewer;
   }-*/;

   public final native String getState() /*-{
      return this.state;
   }-*/;
   
   public final native JsObject getMeta() /*-{
      return this.meta;
   }-*/;

   public final native int getViewerOptions() /*-{
      return this.options;
   }-*/;
   
   public final native void setState(String state) /*-{
      this.state = state;
   }-*/;
   
   public final native void setViewerType(String viewerType) /*-{
      this.viewer = viewerType;
   }-*/;
}
