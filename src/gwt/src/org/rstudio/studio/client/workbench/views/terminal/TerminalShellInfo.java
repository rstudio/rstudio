/*
 * TerminalShellInfo.java
 *
 * Copyright (C) 2022 by Posit, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.terminal;

import com.google.gwt.core.client.GWT;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;

import com.google.gwt.core.client.JavaScriptObject;

public class TerminalShellInfo extends JavaScriptObject
{
   protected TerminalShellInfo() {}

   public final native String getShellType() /*-{
      return this.type;
   }-*/;

   public final native String getShellName() /*-{
      return this.name;
   }-*/;

   public static String getShellName(String shell)
   {
      switch (shell)
      {
      case UserPrefs.WINDOWS_TERMINAL_SHELL_DEFAULT:
         return constants_.defaultShellLabel();
      case UserPrefs.WINDOWS_TERMINAL_SHELL_WIN_GIT_BASH:
         return constants_.winGitBashShellLabel();
      case UserPrefs.WINDOWS_TERMINAL_SHELL_WIN_WSL_BASH:
         return constants_.winWslBashShellLabel();
      case UserPrefs.WINDOWS_TERMINAL_SHELL_WIN_CMD:
         return constants_.winCmdShellLabel();
      case UserPrefs.WINDOWS_TERMINAL_SHELL_WIN_PS:
         return constants_.winPsShellLabel();
      case UserPrefs.WINDOWS_TERMINAL_SHELL_PS_CORE:
         return constants_.psCoreShellLabel();
      case UserPrefs.POSIX_TERMINAL_SHELL_BASH:
         return constants_.bashShellLabel();
      case UserPrefs.POSIX_TERMINAL_SHELL_CUSTOM:
         return constants_.customShellLabel();
      case UserPrefs.POSIX_TERMINAL_SHELL_NONE:
         return constants_.nonShellLabel();
      case UserPrefs.POSIX_TERMINAL_SHELL_ZSH:
         return constants_.zshShellLabel();
      default:
         return constants_.unknownShellLabel();
      }
   }

   private static final TerminalConstants constants_ = GWT.create(TerminalConstants.class);
}
