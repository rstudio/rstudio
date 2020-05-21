/*
 * RProjectOptions.java
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

import org.rstudio.studio.client.packrat.model.PackratContext;
import org.rstudio.studio.client.workbench.projects.RenvContext;

import com.google.gwt.core.client.JavaScriptObject;

public class RProjectOptions extends JavaScriptObject
{
   protected RProjectOptions()
   {
   }
   
   public static final RProjectOptions createEmpty()
   {
      return create(RProjectConfig.createEmpty(), 
                    RProjectVcsOptions.createEmpty(),
                    RProjectBuildOptions.createEmpty(),
                    RProjectPackratOptions.createEmpty(),
                    RProjectRenvOptions.createEmpty());
   }
   
   public native static final RProjectOptions create(
                                    RProjectConfig config,
                                    RProjectVcsOptions vcsOptions,
                                    RProjectBuildOptions buildOptions,
                                    RProjectPackratOptions packratOptions,
                                    RProjectRenvOptions renvOptions)
   /*-{
      var options = new Object();
      options.config = config;
      options.vcs_options = vcsOptions;
      options.vcs_options_default = new Object();
      options.build_options = buildOptions;
      options.packrat_options = packratOptions;
      options.renv_options = renvOptions;
      return options;
   }-*/;
   
   public native final RProjectConfig getConfig() /*-{
      return this.config;
   }-*/;
   
   public native final RProjectVcsOptions getVcsOptions() /*-{
      return this.vcs_options;
   }-*/;
   
   public native final RProjectBuildOptions getBuildOptions() /*-{
      return this.build_options;
   }-*/;
   
   public native final RProjectPackratOptions getPackratOptions() /*-{
      return this.packrat_options;
   }-*/;
   
   public native final RProjectRenvOptions getRenvOptions() /*-{
      return this.renv_options;
   }-*/;

   public native final RProjectVcsContext getVcsContext() /*-{
      return this.vcs_context;
   }-*/;
   
   public native final RProjectBuildContext getBuildContext() /*-{
      return this.build_context;
   }-*/;
   
   public native final PackratContext getPackratContext() /*-{
      return this.packrat_context;
   }-*/;
   
   public native final RenvContext getRenvContext() /*-{
      return this.renv_context;
   }-*/;
   
   
   
}
