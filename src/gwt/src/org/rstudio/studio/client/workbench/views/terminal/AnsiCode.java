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
   public static final String INVERSE_FG_STYLE = "xtermInvertColor";
   public static final String INVERSE_BG_STYLE = "xtermInvertBgColor";

   public static final int HIDDEN = 8;
   public static final int HIDDEN_OFF = 28;
   public static final String HIDDEN_STYLE = "xtermHidden";

   public static final int STRIKETHROUGH = 9;
   public static final int STRIKETHROUGH_OFF = 29;
   public static final String STROKETHROUGH_STYLE = "NYI";

   public static final int FOREGROUND_MIN = 30;
   public static final int FOREGROUND_MAX = 37;
   public static final String FOREGROUND_STYLE = "xtermColor";

   public static final int BACKGROUND_MIN = 40;
   public static final int BACKGROUND_MAX = 47;
   public static final String BACKGROUND_STYLE = "xtermBgColor";
   
   public static final int FOREGROUND_INTENSE_MIN = 90;
   public static final int FOREGROUND_INTENSE_MAX = 97;
  
   public static final int BACKGROUND_INTENSE_MIN = 100;
   public static final int BACKGROUND_INTENSE_MAX = 107;

   public static final int FOREGROUND_EXT = 38;
   public static final int BACKGROUND_EXT = 48;

   public static final String DEFAULTCOLORS = CSI + RESET + ";" + RESET + SGR;
   
   private static final int DEFAULT_COLOR = -1;

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
   public void classForCode(String code, Set<String> clazzes)
   {
      if (code == null || code.length() < 2)
         return; // unrecognized sequence, ignore
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
            inverted_ = false;
            currentColor_ = DEFAULT_COLOR;
            currentBgColor_ = DEFAULT_COLOR;
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
            if (inverted_)
               return;
            resetForeground(clazzes);
            resetBackground(clazzes);
            int newFg = invertFgColor(currentBgColor_, clazzes, inverted_);
            int newBg = invertBgColor(currentColor_, clazzes, inverted_);
            currentColor_ = newFg;
            currentBgColor_ = newBg;
            inverted_ = true;
         }
         else if (codeVal == INVERSE_OFF)
         {
            if (!inverted_)
               return;
            resetForeground(clazzes);
            resetBackground(clazzes);
            int newFg = invertFgColor(currentBgColor_, clazzes, inverted_);
            int newBg = invertBgColor(currentColor_, clazzes, inverted_);
            currentColor_ = newFg;
            currentBgColor_ = newBg;
            inverted_ = false;
         }
         else if (codeVal == HIDDEN)
         {
            clazzes.add(HIDDEN_STYLE);
         }
         else if (codeVal == HIDDEN_OFF)
         {
            clazzes.remove(HIDDEN_OFF);
         }
         else if (codeVal == STRIKETHROUGH)
         {
            // NYI clazzes.add(STRIKETHROUGH_STYLE);
         }
         else if (codeVal == STRIKETHROUGH_OFF)
         {
            // NYI clazzes.remove(STRIKETHROUGH_OFF);
         }
         else if ((codeVal >= FOREGROUND_MIN && codeVal <= FOREGROUND_MAX) ||
                  (codeVal >= FOREGROUND_INTENSE_MIN && codeVal <= FOREGROUND_INTENSE_MAX))
         {
            currentColor_ = codeVal;
            resetForeground(clazzes);
            clazzes.add(clazzForColor(codeVal));
         }
         else if ((codeVal >= BACKGROUND_MIN && codeVal <= BACKGROUND_MAX) ||
               (codeVal >= BACKGROUND_INTENSE_MIN && codeVal <= BACKGROUND_INTENSE_MAX))
         {
            currentBgColor_ = codeVal;
            resetBackground(clazzes);
            clazzes.add(clazzForBgColor(codeVal));
         }
         else if (codeVal == RESET_FOREGROUND)
         {
            currentColor_ = DEFAULT_COLOR;
            resetForeground(clazzes);
         }
         else if (codeVal == RESET_BACKGROUND)
         {
            currentBgColor_ = DEFAULT_COLOR;
            resetBackground(clazzes);
         }
         else if (codeVal == FOREGROUND_EXT)
         {
            // TODO (gary)
         }
         else if (codeVal == BACKGROUND_EXT)
         {
            // TODO (gary)
         }
         else
         {
            // ignore all others
         }
      }
   }
   
   public static String clazzForColor(int color)
   {
      if (color >= FOREGROUND_MIN && color <= FOREGROUND_MAX)
     {
         return(FOREGROUND_STYLE + Integer.toString(color - FOREGROUND_MIN));
      }
      else
      {
         return(FOREGROUND_STYLE + Integer.toString(color + 8 - FOREGROUND_INTENSE_MIN));
      }
   }

   public static String clazzForBgColor(int color)
   {
      if (color >= BACKGROUND_MIN && color <= BACKGROUND_MAX)
      {
         return(BACKGROUND_STYLE + Integer.toString(color - BACKGROUND_MIN));
      }
      else
      {
         return(BACKGROUND_STYLE + Integer.toString(color + 8 - BACKGROUND_INTENSE_MIN));
      }
   }
   
   /**
    * Takes a background color, calculates inverse color as foreground color,
    * applies style, and returns new foreground color.
    * @param colorToInvert Background color to invert
    * @param clazzes clazzes to append to
    * @param inverted true if we are undoing a previous invert
    * @return new foreground color based on the supplied background color
    */
   public static int invertFgColor(int colorToInvert, Set<String> clazzes, boolean inverted)
   {
      if (colorToInvert == DEFAULT_COLOR)
      {
         if (!inverted)
            clazzes.add(INVERSE_FG_STYLE);
         return DEFAULT_COLOR;
      }
      else if (colorToInvert >= BACKGROUND_MIN && colorToInvert <= BACKGROUND_MAX)
      {
         int newFg = colorToInvert - (BACKGROUND_MIN - FOREGROUND_MIN);
         clazzes.add(FOREGROUND_STYLE + Integer.toString(newFg - FOREGROUND_MIN));
         return newFg;
      }
      else
      {
         int newFg = colorToInvert - (BACKGROUND_INTENSE_MIN - FOREGROUND_INTENSE_MIN);
         clazzes.add(FOREGROUND_STYLE + Integer.toString(newFg + 8 - FOREGROUND_INTENSE_MIN));
         return newFg;
      }
   }

   /**
    * Takes a foreground color, calculates inverse color as background color,
    * applies style, and returns new background color.
    * @param colorToInvert Foreground color to invert
    * @param clazzes classes to append to
    * @param inverted true if we are undoing a previous invert
    * @return new background color based on the supplied foreground color
    */
   public static int invertBgColor(int colorToInvert, Set<String> clazzes, boolean inverted)
   {
      if (colorToInvert == DEFAULT_COLOR)
      {
         if (!inverted)
            clazzes.add(INVERSE_BG_STYLE);
         return DEFAULT_COLOR;
      }
      else if (colorToInvert >= FOREGROUND_MIN && colorToInvert <= FOREGROUND_MAX)
      {
         int newBg = colorToInvert + (BACKGROUND_MIN - FOREGROUND_MIN);
         clazzes.add(BACKGROUND_STYLE + Integer.toString(newBg - BACKGROUND_MIN));
         return newBg;
      }
      else
      {
         int newBg = colorToInvert + (BACKGROUND_INTENSE_MIN - FOREGROUND_INTENSE_MIN);
         clazzes.add(BACKGROUND_STYLE + Integer.toString(newBg + 8 - BACKGROUND_INTENSE_MIN));
         return newBg;
      }
   }
    
   private static void resetForeground(Set<String> clazzes)
   {
      for (int i = FOREGROUND_MIN; i <= FOREGROUND_MAX; i++)
      {
         clazzes.remove(clazzForColor(i));
      }
      for (int i = FOREGROUND_INTENSE_MIN; i <= FOREGROUND_INTENSE_MAX; i++)
      {
         clazzes.remove(clazzForColor(i));
      }
      clazzes.remove(INVERSE_FG_STYLE);
   }

   private static void resetBackground(Set<String> clazzes)
   {
      for (int i = BACKGROUND_MIN; i <= BACKGROUND_MAX; i++)
      {
         clazzes.remove(clazzForBgColor(i));
      }
      for (int i = BACKGROUND_INTENSE_MIN; i <= BACKGROUND_INTENSE_MAX; i++)
      {
         clazzes.remove(clazzForBgColor(i));
      }
      clazzes.remove(INVERSE_BG_STYLE);
   }

   // Control characters handled by R console
   private static final String CONTROL_REGEX = "[\r\b\f\n]";

   // RegEx to match ansi escape codes copied from https://github.com/chalk/ansi-regex
   private static final String ANSI_REGEX = 
         "[\u001b\u009b][[()#;?]*(?:[0-9]{1,4}(?:;[0-9]{0,4})*)?[0-9A-PRZcf-nqry=><]";
   
   // Match both console control characters and ansi escape sequences
   public static final Pattern ESC_CONTROL_PATTERN =
         Pattern.create("(?:" + CONTROL_REGEX + ")|(?:" + ANSI_REGEX + ")");

   int currentColor_ = DEFAULT_COLOR;
   int currentBgColor_ = DEFAULT_COLOR;
   boolean inverted_ = false;
}