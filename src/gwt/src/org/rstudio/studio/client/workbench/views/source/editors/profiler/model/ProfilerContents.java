/*
 * ProfilerContents.java
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
package org.rstudio.studio.client.workbench.views.source.editors.profiler.model;

import com.google.gwt.core.client.JavaScriptObject;

public class ProfilerContents extends JavaScriptObject
{
   protected ProfilerContents()
   {
   }
   
   public static final native ProfilerContents create(
      String path, 
      String htmlPath, 
      String htmlLocalPath, 
      boolean createProfile) /*-{
      var contents = new Object();

      contents.path = path;
      contents.htmlPath = htmlPath;
      contents.htmlLocalPath = htmlLocalPath;
      
      contents.isUserSaved = createProfile ? null : "saved";
      contents.createProfile = createProfile;
      return contents;
   }-*/;
   
   public final native String getPath() /*-{
      return this.path;
   }-*/;
   
   public final native String getHtmlPath() /*-{
      return this.htmlPath;
   }-*/;
   
   public final native boolean getCreateProfile() /*-{
      return this.createProfile;
   }-*/;
   
   public final native String getName() /*-{
      return this.name;
   }-*/;
   
   public final native String getHtmlLocalPath() /*-{
      return this.htmlLocalPath;
   }-*/;
   
   public final native boolean isUserSaved() /*-{
      return this.isUserSaved == "saved";
   }-*/;
}
