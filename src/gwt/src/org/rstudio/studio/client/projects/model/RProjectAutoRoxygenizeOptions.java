/*
 * RProjectAutoRoxygenizeOptions.java
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

public class RProjectAutoRoxygenizeOptions extends JavaScriptObject
{
   protected RProjectAutoRoxygenizeOptions()
   {
   }
   
   public native static final RProjectAutoRoxygenizeOptions create(
                                            boolean runOnCheck,
                                            boolean runOnBuilds,
                                            boolean runOnBuildAndReload) /*-{
      var options = new Object();
      options.run_on_check = runOnCheck;
      options.run_on_package_builds = runOnBuilds;
      options.run_on_build_and_reload = runOnBuildAndReload;
      return options;
   }-*/;
   
   public native final boolean getRunOnCheck() /*-{
      return this.run_on_check;
   }-*/;

   public native final void setRunOnCheck(boolean runOnCheck) /*-{
      this.run_on_check = runOnCheck;
   }-*/;   

   public native final boolean getRunOnPackageBuilds() /*-{
      return this.run_on_package_builds;
   }-*/;

   public native final void setRunOnPackageBuilds(boolean runOnBuilds) /*-{
      this.run_on_package_builds = runOnBuilds;
   }-*/; 

   public native final boolean getRunOnBuildAndReload() /*-{
      return this.run_on_build_and_reload;
   }-*/;

   public native final void setRunOnBuildAndReload(boolean runOnBuildAndReload) /*-{
      this.run_on_build_and_reload = runOnBuildAndReload;
   }-*/;  
}
