/*
 * CompilePdfPrefs.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.prefs.model;

import com.google.gwt.core.client.JavaScriptObject;

public class CompilePdfPrefs extends JavaScriptObject
{
   protected CompilePdfPrefs() {}

   public static final native CompilePdfPrefs create(boolean useTexi2Dvi, 
                                                     boolean cleanOutput,
                                                     boolean enableShellEscape) /*-{
      var prefs = new Object();
      prefs.use_texi2dvi = useTexi2Dvi;
      prefs.clean_output = cleanOutput;
      prefs.enable_shell_escape = enableShellEscape;
      return prefs ;
   }-*/;
   
   public native final boolean getUseTexi2Dvi() /*-{
      return this.use_texi2dvi;
   }-*/;
   
   public native final boolean getCleanOutput() /*-{
      return this.clean_output;
   }-*/;
   
   public native final boolean getEnableShellEscape() /*-{
      return this.enable_shell_escape;
   }-*/;
   
}
