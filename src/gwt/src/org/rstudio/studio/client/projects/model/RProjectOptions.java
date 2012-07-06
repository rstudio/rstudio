/*
 * RProjectOptions.java
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

public class RProjectOptions extends JavaScriptObject
{
   protected RProjectOptions()
   {
   }
   
   public static final RProjectOptions createEmpty()
   {
      return create(RProjectConfig.createEmpty(), 
                    RProjectVcsOptions.createEmpty(),
                    RProjectBuildOptions.createEmpty());
   }
   
   public native static final RProjectOptions create(
                                    RProjectConfig config,
                                    RProjectVcsOptions vcsOptions,
                                    RProjectBuildOptions buildOptions) /*-{
      var options = new Object();
      options.config = config;
      options.vcs_options = vcsOptions;
      options.vcs_options_default = new Object();
      options.build_options = buildOptions;
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

   public native final RProjectVcsContext getVcsContext() /*-{
      return this.vcs_context;
   }-*/;
}
