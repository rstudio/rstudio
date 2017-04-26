/*
 * TerminalShellInfo.java
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
package org.rstudio.studio.client.workbench.views.terminal;

import com.google.gwt.core.client.JavaScriptObject;

public class TerminalShellInfo extends JavaScriptObject
{
   // Keep these enums synced with enum TerminalShellType on server

   // Open the default shell type
   public static final int SHELL_DEFAULT = 0;

   // -- Start Windows-only
   public static final int SHELL_GITBASH = 1;
   public static final int SHELL_WSLBASH = 2; // Windows Services for Linux

   public static final int SHELL_CMD32 = 3;
   public static final int SHELL_CMD64 = 4;

   public static final int SHELL_PS32 = 5; // Powershell
   public static final int SHELL_PS64 = 6;
   // -- End Windows-only	
   
   public static final int SHELL_POSIX_BASH = 7;

   protected TerminalShellInfo() {}

   public final native int getShellType() /*-{
      return this.type;
   }-*/;

   public final native String getShellName() /*-{
      return this.name;
   }-*/; 
   
   public static String getShellName(int shell)
   {
      switch (shell)
      {
      case SHELL_DEFAULT:
         return "Default";
      case SHELL_GITBASH:
         return "Git Bash";
      case SHELL_WSLBASH:
         return "WSL";
      case SHELL_CMD32:
         return "Command Prompt (32-bit)";
      case SHELL_CMD64:
         return "Command Prompt (64-bit)";
      case SHELL_PS32:
         return "PowerShell (32-bit)";
      case SHELL_PS64:
         return "PowerShell (64-bit)";
      case SHELL_POSIX_BASH:
         return "Bash";
      default:
         return "Unknown";
      }
   }
   
}
