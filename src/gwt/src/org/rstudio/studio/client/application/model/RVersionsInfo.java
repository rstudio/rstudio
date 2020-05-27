/*
 * RVersionsInfo.java
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
package org.rstudio.studio.client.application.model;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

public class RVersionsInfo extends JavaScriptObject
{
   protected RVersionsInfo() {}                    
   
   public final native String getRVersion() /*-{
      return this.r_version;
   }-*/;
   
   public final native String getRVersionLabel() /*-{
      return this.r_version_label;
   }-*/;

   public final native String getRVersionHome() /*-{
      return this.r_home_dir;
   }-*/;
   
   // settings below are null unless running with an overlay
   
   public final native String getDefaultRVersion() /*-{
      return this.default_r_version;
   }-*/;

   public final native String getDefaultRVersionHome() /*-{
      return this.default_r_version_home;
   }-*/;

   public final native String getDefaultRVersionLabel() /*-{
      return this.default_r_version_label;
   }-*/;
   
   public final native boolean getRestoreProjectRVersion() /*-{
      return this.restore_project_r_version;
   }-*/;
   
   public final boolean isMultiVersion()
   {
      JsArray<RVersionSpec> versions = getAvailableRVersions();
      return versions != null && versions.length() > 0;
   }
   
   public final native JsArray<RVersionSpec> getAvailableRVersions() /*-{
      return this.available_r_versions;
   }-*/;
   
}
