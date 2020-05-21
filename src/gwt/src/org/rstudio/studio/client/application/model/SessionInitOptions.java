/*
 * SessionInitOptions.java
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

public class SessionInitOptions extends JavaScriptObject
{
   public final static String RESTORE_WORKSPACE_OPTION = "restore_workspace";
   public final static int RESTORE_WORKSPACE_NO      = 0;
   public final static int RESTORE_WORKSPACE_YES     = 1;
   public final static int RESTORE_WORKSPACE_DEFAULT = 2;

   public final static String RUN_RPROFILE_OPTION = "run_rprofile";
   public final static int RUN_RPROFILE_NO      = 0;
   public final static int RUN_RPROFILE_YES     = 1;
   public final static int RUN_RPROFILE_DEFAULT = 2;
   
   protected SessionInitOptions()
   {  
   }
   
   public native final void setRestoreWorkspace(int value) /*-{
      this.restore_workspace = value;
   }-*/;
   
   public native final void setRunRprofile(int value) /*-{
      this.run_rprofile = value;
   }-*/;
   
   public native final int restoreWorkspace() /*-{
      return this.restore_workspace;
   }-*/;
   
   public native final int runRprofile() /*-{
      return this.run_rprofile;
   }-*/;
   
   public native static final SessionInitOptions create(
      int restoreWorkspace, int runRprofile) /*-{
      return {
         restore_workspace: restoreWorkspace,
         run_rprofile:      runRprofile
      };
   }-*/;
}
