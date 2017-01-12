/*
 * AnsiEscapeCode.java
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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

/**
 * Helpers for generating Ansi Escape Code strings for output to a terminal.
 */
public class AnsiCode
{
   // Ansi command constants
   public static final String CSI = "\33[";
   public static final String SGR = "m";
   
   public static final String DEFAULTCOLORS = CSI + "0;0" + SGR;

   public static class ForeColor
   {
      public static final String BLACK        = CSI + "0;30" + SGR;
      public static final String RED          = CSI + "0;31" + SGR;
      public static final String GREEN        = CSI + "0;32" + SGR;
      public static final String BROWN        = CSI + "0;33" + SGR;
      public static final String BLUE         = CSI + "0;34" + SGR;
      public static final String MAGENTA      = CSI + "0;35" + SGR;
      public static final String CYAN         = CSI + "0;36" + SGR;
      public static final String GRAY         = CSI + "0;37" + SGR;
      public static final String DARKGRAY     = CSI + "1;30" + SGR;
      public static final String LIGHTRED     = CSI + "1;31" + SGR;
      public static final String LIGHTGREEN   = CSI + "1;32" + SGR;
      public static final String YELLOW       = CSI + "1;33" + SGR;
      public static final String LIGHTBLUE    = CSI + "1;34" + SGR;
      public static final String LIGHTMAGENTA = CSI + "1;35" + SGR;
      public static final String LIGHTCYAN    = CSI + "1:36" + SGR;
      public static final String WHITE        = CSI + "1;37" + SGR;
   }
   
   public static class BackColor
   {
      public static final String BLACK        = CSI + "0;40" + SGR;
      public static final String RED          = CSI + "0;41" + SGR;
      public static final String GREEN        = CSI + "0;42" + SGR;
      public static final String BROWN        = CSI + "0;43" + SGR;
      public static final String BLUE         = CSI + "0;44" + SGR;
      public static final String MAGENTA      = CSI + "0;45" + SGR;
      public static final String CYAN         = CSI + "0;46" + SGR;
      public static final String GRAY         = CSI + "0;47" + SGR;
      public static final String DARKGRAY     = CSI + "1;40" + SGR;
      public static final String LIGHTRED     = CSI + "1;41" + SGR;
      public static final String LIGHTGREEN   = CSI + "1;42" + SGR;
      public static final String YELLOW       = CSI + "1;43" + SGR;
      public static final String LIGHTBLUE    = CSI + "1;44" + SGR;
      public static final String LIGHTMAGENTA = CSI + "1;45" + SGR;
      public static final String LIGHTCYAN    = CSI + "1:46" + SGR;
      public static final String WHITE        = CSI + "1;47" + SGR;
   }
}
