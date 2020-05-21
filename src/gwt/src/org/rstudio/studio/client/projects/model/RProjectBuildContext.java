/*
 * RProjectBuildContext.java
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
import com.google.gwt.core.client.JsArrayString;

public class RProjectBuildContext extends JavaScriptObject
{
   protected RProjectBuildContext()
   {
   }
   
   public native final boolean isRoxygen2Installed() /*-{
      return this.roxygen2_installed;
   }-*/;
   
   public native final boolean isDevtoolsInstalled() /*-{
      return this.devtools_installed;
   }-*/;
   
   public native final JsArrayString getWebsiteOutputFormats() /*-{
      return this.website_output_formats;
   }-*/;
   
   public final boolean isBookdownSite() 
   {
      JsArrayString formats = getWebsiteOutputFormats();
      for (int i = 0; i<formats.length(); i++)
         if (formats.get(i).startsWith("bookdown"))
            return true;
      
      return false;
   }
}
