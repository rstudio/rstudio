/*
 * RProjectBuildOptions.java
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

public class RProjectBuildOptions extends JavaScriptObject
{
   protected RProjectBuildOptions()
   {
   }
   
   public native static final RProjectBuildOptions createEmpty() /*-{
      var options = new Object();
      return options;
   }-*/;
   
   public native final String getMakefileArgs() /*-{
      return this.makefile_args;
   }-*/;
   
   public native final void setMakefileArgs(String makefileArgs) /*-{
      this.makefile_args = makefileArgs;
   }-*/;   
   
   public native final boolean getPreviewWebsite() /*-{
      return this.preview_website;
   }-*/;
   
   public native final void setPreviewWebsite(boolean preview) /*-{
      this.preview_website = preview;
   }-*/;
   
   public native final boolean getLivePreviewWebsite() /*-{
      return this.live_preview_website;
   }-*/;

   public native final void setLivePreviewWebsite(boolean preview) /*-{
      this.live_preview_website = preview;
   }-*/;
   
   public native final String getWebsiteOutputFormat() /*-{
      return this.website_output_format;
   }-*/;

   public native final void setWebsiteOutputFormat(String format) /*-{
      this.website_output_format = format;
   }-*/;   
  
   public native final RProjectAutoRoxygenizeOptions getAutoRogyginizeOptions() /*-{
      return this.auto_roxygenize_options;
   }-*/;
   
   public native final void setAutoRoxyginizeOptions(
                               RProjectAutoRoxygenizeOptions options)  /*-{
      this.auto_roxygenize_options = options;
   }-*/;
}
