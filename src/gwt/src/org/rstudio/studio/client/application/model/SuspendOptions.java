/*
 * SuspendOptions.java
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

public class SuspendOptions extends JavaScriptObject
{
   protected SuspendOptions()
   {  
   }
   
   public static final SuspendOptions createSaveAll(boolean excludePackages) 
   {
      return create(false, false, excludePackages);
   }
   
   public static final SuspendOptions createSaveMinimal(boolean saveWorkspace) 
   {
      return create(true, saveWorkspace, false);
   }
   
   private static native final SuspendOptions create(boolean saveMinimal,
                                                    boolean saveWorkspace,
                                                    boolean excludePackages) /*-{
      var options = new Object();
      options.save_minimal = saveMinimal;
      options.save_workspace = saveWorkspace;
      options.exclude_packages = excludePackages;
      return options;
   }-*/;
   
   /*
    * Indicates that only a minimal amount of session state should be
    * saved (e.g. working directory and up-arrow history). 
    * 
    * If this option is true then the save_workspace option will be 
    * consulted to determine whether the workspace should also be saved.
    * 
    * If this option is false then the exclude_packages option will be
    * consulted to determine whether to exclude packages
    */
   public native final boolean getSaveMinimal() /*-{
      return this.save_minimal;
   }-*/;

   /*
    * This option is only consulted if save_minimal is true
    */
   public native final boolean getSaveWorkspace() /*-{
      return this.save_workspace;
   }-*/;
   
   /*
    * This option is only consulted if save_minimal is false
    */
   public native final boolean getExcludePackages() /*-{
      return this.exclude_packages;
   }-*/;
   
}
