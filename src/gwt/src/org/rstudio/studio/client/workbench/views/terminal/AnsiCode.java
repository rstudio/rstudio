/*
 * AnsiEscapeCode.java
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

import java.util.Set;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.regex.Pattern;

/**
 * Helpers for working with ANSI Escape Codes in terminal and console.
 */
public class AnsiCode

{
   // ANSI command constants
   public static final String CSI = "\33[";
   public static final String SGR = "m";

   public static final int RESET = 0;
   public static final int RESET_FOREGROUND = 39;
   public static final int RESET_BACKGROUND = 49;

   public static final int BOLD = 1;
   public static final int BLURRED = 2;
   public static final int BOLD_BLURRED_OFF = 22;
   public static final String BOLD_STYLE = "xtermBold";
   public static final String BLURRED_STYLE = "NYI";

   public static final int ITALIC = 3;
   public static final int ITALIC_OFF = 23;
   public static final String ITALIC_STYLE = "NYI";

   public static final int UNDERLINE = 4;
   public static final int UNDERLINE_OFF = 24;
   public static final String UNDERLINE_STYLE = "xtermUnderline";

   public static final int BLINKSLOW = 5;
   public static final int BLINKFAST = 6;
   public static final int BLINK_OFF = 25;
   public static final String BLINK_STYLE = "xtermBlink";

   public static final int INVERSE = 7;
   public static final int INVERSE_OFF = 27;
   public static final String INVERSE_STYLE = "NYI";

   public static final int HIDDEN = 8;
   public static final int HIDDEN_OFF = 28;
   public static final String HIDDEN_STYLE = "NYI";

   public static final int STRIKETHROUGH = 9;
   public static final int STRIKETHROUGH_OFF = 29;
   public static final String STROKETHROUGH_STYLE = "NYI";

   public static final int FOREGROUND_MIN = 30;
   public static final int FOREGROUND_MAX = 37;

   public static final int FOREGROUND_SILVER = 90; // from crayon package
   public static final String FOREGROUND_SILVER_STYLE = "NYI";

   public static final int BACKGROUND_MIN = 40;
   public static final int BACKGROUND_MAX = 47;

   public static final int FOREGROUND_EXT = 38;
   public static final int BACKGROUND_EXT = 48;

   public static final String DEFAULTCOLORS = CSI + RESET + ";" + RESET + SGR;

   public static class ForeColor
   {
      public static final String BLACK        = CSI + RESET + ";30" + SGR;
      public static final String RED          = CSI + RESET + ";31" + SGR;
      public static final String GREEN        = CSI + RESET + ";32" + SGR;
      public static final String BROWN        = CSI + RESET + ";33" + SGR;
      public static final String BLUE         = CSI + RESET + ";34" + SGR;
      public static final String MAGENTA      = CSI + RESET + ";35" + SGR;
      public static final String CYAN         = CSI + RESET + ";36" + SGR;
      public static final String GRAY         = CSI + RESET + ";37" + SGR;
      public static final String DEFAULT_FORE = CSI + "39" + SGR;
   }
   
   public static class BackColor
   {
      public static final String BLACK        = CSI + RESET + ";40" + SGR;
      public static final String RED          = CSI + RESET + ";41" + SGR;
      public static final String GREEN        = CSI + RESET + ";42" + SGR;
      public static final String BROWN        = CSI + RESET + ";43" + SGR;
      public static final String BLUE         = CSI + RESET + ";44" + SGR;
      public static final String MAGENTA      = CSI + RESET + ";45" + SGR;
      public static final String CYAN         = CSI + RESET + ";46" + SGR;
      public static final String GRAY         = CSI + RESET + ";47" + SGR;
   }
   
   /**
    * Map an ANSI escape sequence to the appropriate css styles; only handles
    * colors and visual appearance covered by SGR codes; other sequences 
    * such as cursor movement are ignored.
    * @param code escape sequence
    * @clazzes list of current classes to modify
    * @return true if classes contains modified classes, false if caller should
    * reset all styles back to defaults
    */
   public static void classForCode(String code, Set<String> clazzes)
   {
      if (code == null || code.length() < 2)
         return; // unparsable sequence, ignore
      if (code.charAt(0) != '\033' && code.charAt(code.length() - 1) != 'm')
         return; // unsupported sequence, ignore
      if (code.length() == 2)
      {
         clazzes.clear(); // CSIm is equivalent to CSI0m, which is 'reset'
         return;
      }

      String[] tokens = code.substring(2, code.length() - 1).split(";");
      for (String token : tokens)
      {
         int codeVal = StringUtil.parseInt(token,  -1);
         if (codeVal == -1)
            continue;

         if (codeVal == RESET)
         {
            clazzes.clear();
         }
         else if (codeVal == BOLD)
         {
            clazzes.add(BOLD_STYLE);
         }
         else if (codeVal == BLURRED)
         {
            // NYI clazzes.add(BLURRED_STYLE);
         }
         else if (codeVal == BOLD_BLURRED_OFF)
         {
            clazzes.remove(BOLD_STYLE);
            // NYI clazzes.remove(BLURRED_STYLE);
         }
         else if (codeVal == ITALIC)
         {
            // NYI clazzes.add(ITALIC_STYLE);
         }
         else if (codeVal == ITALIC_OFF)
         {
            // NYI clazzes.remove(ITALIC_STYLE);
         }
         else if (codeVal == UNDERLINE)
         {
            clazzes.add(UNDERLINE_STYLE);
         }
         else if (codeVal == UNDERLINE_OFF)
         {
            clazzes.remove(UNDERLINE_STYLE);
         }
         else if (codeVal == BLINKSLOW || codeVal == BLINKFAST)
         {
            clazzes.add(BLINK_STYLE);
         }
         else if (codeVal == BLINK_OFF)
         {
            clazzes.remove(BLINK_STYLE);
         }
         else if (codeVal == INVERSE)
         {
            // NYI clazzes.add(INVERSE_STYLE);
         }
         else if (codeVal == INVERSE_OFF)
         {
            // NYI clazzes.remove(INVERSE_OFF);
         }
         else if (codeVal == HIDDEN)
         {
            // NYI clazzes.add(HIDDEN_STYLE);
         }
         else if (codeVal == HIDDEN_OFF)
         {
            // NYI clazzes.remove(HIDDEN_OFF);
         }
         else if (codeVal == STRIKETHROUGH)
         {
            // NYI clazzes.add(STRIKETHROUGH_STYLE);
         }
         else if (codeVal == STRIKETHROUGH_OFF)
         {
            // NYI clazzes.remove(STRIKETHROUGH_OFF);
         }
         else if (codeVal >= FOREGROUND_MIN && codeVal <= FOREGROUND_MAX)
         {
            clazzes.add("xtermColor" + Integer.toString(codeVal - FOREGROUND_MIN));
         }
         else if (codeVal >= BACKGROUND_MIN && codeVal <= BACKGROUND_MAX)
         {
            clazzes.add("xtermBgColor" + Integer.toString(codeVal - BACKGROUND_MIN));
         }
         else if (codeVal == RESET_FOREGROUND)
         {
            // TODO (gary)
         }
         else if (codeVal == RESET_BACKGROUND)
         {
            // TODO (gary)
         }
         else if (codeVal == FOREGROUND_EXT)
         {
            // TODO (gary)
         }
         else if (codeVal == BACKGROUND_EXT)
         {
            // TODO (gary)
         }
         else if (codeVal == FOREGROUND_SILVER)
         {
            // NYI clazzes.add(FOREGROUND_SILVER_STYLE);
         }
         else
         {
            // ignore all others
         }
      }
   }

   // Control characters handled by R console
   private static final String CONTROL_REGEX = "[\r\b\f\n]";

   // RegEx to match ansi escape codes copied from https://github.com/chalk/ansi-regex
   private static final String ANSI_REGEX = 
         "[\u001b\u009b][[()#;?]*(?:[0-9]{1,4}(?:;[0-9]{0,4})*)?[0-9A-PRZcf-nqry=><]";
   
   // Match both console control characters and ansi escape sequences
   public static final Pattern ESC_CONTROL_PATTERN =
         Pattern.create("(?:" + CONTROL_REGEX + ")|(?:" + ANSI_REGEX + ")");
}