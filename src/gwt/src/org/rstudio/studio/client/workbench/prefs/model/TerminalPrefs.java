/*
 * TerminalPrefs.java
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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

package org.rstudio.studio.client.workbench.prefs.model;

import com.google.gwt.core.client.JavaScriptObject;

public class TerminalPrefs extends JavaScriptObject
{
   protected TerminalPrefs() {}

   public static final native TerminalPrefs create(int defaultShell, 
                                                   String customShellPath,
                                                   String customShellOptions) /*-{
      var prefs = new Object();
      prefs.default_shell = defaultShell;
      prefs.shell_exe_path = customShellPath;
      prefs.shell_exe_options = customShellOptions;
      return prefs;
   }-*/;

   public native final int getDefaultTerminalShellValue() /*-{
      return this.default_shell;
   }-*/;

   public native final String getCustomTerminalShellPath() /*-{
      return this.shell_exe_path;
   }-*/;

   public native final String getCustomTerminalShellOptions() /*-{
      return this.shell_exe_options;
   }-*/;
}
