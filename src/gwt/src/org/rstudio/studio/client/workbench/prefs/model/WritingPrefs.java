/*
 * WritingPrefs.java
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

public class WritingPrefs extends JavaScriptObject
{
   protected WritingPrefs() {}

   public static final native WritingPrefs create(boolean alwaysEnableConcordance,
                                                  boolean useTexi2Dvi, 
                                                  boolean cleanOutput,
                                                  boolean enableShellEscape) /*-{
      var prefs = new Object();
      prefs.always_enable_concordance = alwaysEnableConcordance;
      prefs.use_texi2dvi = useTexi2Dvi;
      prefs.clean_output = cleanOutput;
      prefs.enable_shell_escape = enableShellEscape;
      return prefs ;
   }-*/;

   public native final boolean getAlwaysEnableConcordance() /*-{
      return this.always_enable_concordance;
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
