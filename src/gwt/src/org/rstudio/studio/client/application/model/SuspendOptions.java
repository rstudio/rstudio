/*
 * SuspendOptions.java
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
package org.rstudio.studio.client.application.model;

import com.google.gwt.core.client.JavaScriptObject;

public class SuspendOptions extends JavaScriptObject
{
   protected SuspendOptions()
   {  
   }
   
   public static native final SuspendOptions create(boolean saveMinimal,
                                                    boolean saveWorkspace) /*-{
      var options = new Object();
      options.save_minimal = saveMinimal;
      options.save_workspace = saveWorkspace;
      return options;
   }-*/;
   
   /*
    * Indidates that only a minimal amount of session state should be 
    * saved (e.g. working directory and up-arrow history). If this option
    * is true then the save_workspace option will be consulted to determine
    * whether the workspace should also be saved.
    */
   public native final boolean getSaveMinimal() /*-{
      return this.save_minimal;
   }-*/;

   public native final boolean getSaveWorkspace() /*-{
      return this.save_workspace;
   }-*/;
}
