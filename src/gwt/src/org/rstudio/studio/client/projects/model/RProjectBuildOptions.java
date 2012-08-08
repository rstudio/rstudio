/*
 * RProjectBuildOptions.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
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
   
   public native final boolean getCleanupAfterCheck() /*-{
      return this.cleanup_after_check;
   }-*/;

   public native final void setCleanupAfterCheck(boolean cleanup) /*-{
      this.cleanup_after_check = cleanup;
   }-*/;   

   public native final RProjectAutoRoxygenizeOptions getAutoRogyginizeOptions() /*-{
      return this.auto_roxygenize_options;
   }-*/;
   
   public native final void setAutoRoxyginizeOptions(
                               RProjectAutoRoxygenizeOptions options)  /*-{
      this.auto_roxygenize_options = options;
   }-*/;
}
