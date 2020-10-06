/*
 * AnsiEscapeCode.java
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

package org.rstudio.core.client;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.rstudio.core.client.regex.Pattern;

/**
 * Helpers for working with ANSI Escape Codes in terminal and console.
 */
public class AnsiCode
{
   // ANSI command constants
   public static final String CSI = "\33[";
   public static final String SGR = "m";

   // Move Cursor Horizontal Absolute
   public static final String CHA = "G";

   // Erase in Line
   public static final String EL = "K";

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
   public static final String ITALIC_STYLE = "xtermItalic";

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
   public static final String STRIKETHROUGH_STYLE = "xtermStrike";

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
   public static final int EXT_BY_INDEX = 5;
   public static final int EXT_BY_RGB = 2;

   public static final int DEFAULT_FONT = 10;
   public static final int FONT_ONE = 11;
   public static final int FONT_TWO = 12;
   public static final int FONT_THREE = 13;
   public static final int FONT_FOUR = 14;
   public static final int FONT_FIVE = 15;
   public static final int FONT_SIX = 16;
   public static final int FONT_SEVEN = 17;
   public static final int FONT_EIGHT = 18;

   // Font-nine is used by RStudio to reduce spacing between lines
   public static final int FONT_NINE = 19;
   public static final String FONT_NINE_STYLE = "xtermFont9";

   public static final String DEFAULTCOLORS = CSI + RESET + ";" + RESET + SGR;

   public static class AnsiClazzes
   {
      // span-level css classes
      public String inlineClazzes = null;

      // block-level css classes
      public String blockClazzes = null;
   }

   public static class ForeColorNum
   {
      public static final int BLACK = 30;
      public static final int RED = 31;
      public static final int GREEN = 32;
      public static final int YELLOW = 33;
      public static final int BLUE = 34;
      public static final int MAGENTA = 35;
      public static final int CYAN = 36;
      public static final int WHITE = 37;
   }

   public static class BackColorNum
   {
      public static final int BLACK = 40;
      public static final int RED = 41;
      public static final int GREEN = 42;
      public static final int YELLOW = 43;
      public static final int BLUE = 44;
      public static final int MAGENTA = 45;
      public static final int CYAN = 46;
      public static final int WHITE = 47;
   }

   public static class ForeColor
   {
      public static final String BLACK        = CSI + RESET + ";30" + SGR;
      public static final String RED          = CSI + RESET + ";31" + SGR;
      public static final String GREEN        = CSI + RESET + ";32" + SGR;
      public static final String YELLOW       = CSI + RESET + ";33" + SGR;
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
      public static final String DEFAULT_BACK = CSI + RESET + ";49" + SGR;
   }

   private static class Color
   {
      public static final int DEFAULT_COLOR = -1;

      public Color()
      {
         extended_ = false;
         code_ = DEFAULT_COLOR;
      }

      public Color(boolean extended, int code)
      {
         extended_ = extended;
         code_ = code;
      }

      public int code() { return code_; }

      public void setCode(int code)
      {
         extended_ = false;
         code_ = code;
      }

      public void setExtended(int code)
      {
         extended_ = true;
         code_ = code;
      }

      public void reset()
      {
         extended_ = false;
         code_ = DEFAULT_COLOR;
      }

      public boolean defaultColor()
      {
         return code_ == DEFAULT_COLOR;
      }

      public static boolean isNormalFgColorCode(int code)
      {
         return (code >= FOREGROUND_MIN && code <= FOREGROUND_MAX);
      }

      public static boolean isIntenseFgColorCode(int code)
      {
         return (code >= FOREGROUND_INTENSE_MIN && code <= FOREGROUND_INTENSE_MAX);
      }

      public static boolean isFgColorCode(int code)
      {
         return isNormalFgColorCode(code) || isIntenseFgColorCode(code);
      }

      public static boolean isNormalBgColorCode(int code)
      {
         return (code >= BACKGROUND_MIN && code <= BACKGROUND_MAX);
      }

      public static boolean isIntenseBgColorCode(int code)
      {
         return (code >= BACKGROUND_INTENSE_MIN && code <= BACKGROUND_INTENSE_MAX);
      }

      public static boolean isBgColorCode(int code)
      {
         return isNormalBgColorCode(code) || isIntenseBgColorCode(code);
      }

      public static String clazzForColorIndex(int index, boolean background)
      {
         return((background ? BACKGROUND_STYLE : FOREGROUND_STYLE) +
               Integer.toString(index));
      }

      /**
       * Convert a non-extended foreground color to equivalent background color
       * @param fg foreground value
       * @return background value
       */
      public static int fgToBgColor(int fg)
      {
         if (isNormalFgColorCode(fg))
            return fg + (BACKGROUND_MIN - FOREGROUND_MIN);
         else if (isIntenseFgColorCode(fg))
            return fg + (BACKGROUND_INTENSE_MIN - FOREGROUND_INTENSE_MIN);
         else
            return fg;
      }

      /**
       * Convert a non-extended background color to equivalent foreground color
       * @param bg background value
       * @return foreground value
       */
      public static int bgToFgColor(int bg)
      {
         if (isNormalBgColorCode(bg))
            return bg - (BACKGROUND_MIN - FOREGROUND_MIN);
         else if (isIntenseBgColorCode(bg))
            return bg - (BACKGROUND_INTENSE_MIN - FOREGROUND_INTENSE_MIN);
         else
            return bg;
      }

      public boolean isExtended() { return extended_; }

      private boolean extended_;
      private int code_;
   }

   public AnsiCode()
   {
   }

   /**
    * Map an ANSI escape sequence to the appropriate css styles; only handles
    * colors and visual appearance covered by SGR codes; other sequences
    * such as cursor movement are ignored.
    * @param code escape sequence
    * @return AnsiClazzes, containing both span-level and block-level styles
    */
   public AnsiClazzes processCode(String code)
   {
      if (code == null || code.length() < 2)
         return null;
      if (code.charAt(0) != '\033' && code.charAt(code.length() - 1) != 'm')
         return null;
      if (code.length() == 2)
      {
         clazzes_.clear(); // CSIm is equivalent to CSI0m, which is 'reset'
         blockClazzes_.clear();
         return null;
      }

      int extendedColor = 0;
      boolean extendedMarkerSeen = false;
      boolean extendedRGBMarkerSeen = false;
      int extendedRGBColorsSeen = 0;

      String[] tokens = code.substring(2, code.length() - 1).split(";");
      for (String token : tokens)
      {
         int codeVal = StringUtil.parseInt(token,  -1);
         if (codeVal == -1)
            continue;

         if (extendedColor > 0)
         {
            if (!extendedMarkerSeen && !extendedRGBMarkerSeen)
            {
               if (codeVal == EXT_BY_INDEX)
               {
                  extendedMarkerSeen = true;
                  continue;
               }
               else if (codeVal == EXT_BY_RGB)
               {
                  extendedRGBMarkerSeen = true;
                  extendedRGBColorsSeen = 0;
               }
               else
               {
                  // unknown extended color format; hard to recover so
                  // just reset back to defaults and return
                  clazzes_.clear();
                  blockClazzes_.clear();
                  return null;
               }
            }
            else
            {
               // We don't support colors specified via RGB, but parse the
               // sequence then ignore it in case there are supported
               // sequences after it
               if (extendedRGBMarkerSeen)
               {
                  extendedRGBColorsSeen++;
                  if (extendedRGBColorsSeen == 3 /*red, green, blue*/)
                  {
                     extendedColor = 0;
                     extendedRGBMarkerSeen = false;
                     extendedRGBColorsSeen = 0;
                  }
               }
               else
               {
                  if ((!inverted_ && extendedColor == FOREGROUND_EXT) || (inverted_ && (extendedColor == BACKGROUND_EXT)))
                  {
                     if (codeVal >= 0 && codeVal <= 255)
                     {
                        currentColor_.setExtended(codeVal);
                        resetForeground();
                        clazzes_.add(Color.clazzForColorIndex(codeVal, false /*background*/));
                     }
                  }
                  else
                  {
                     if (codeVal >= 0 && codeVal <= 255)
                     {
                        currentBgColor_.setExtended(codeVal);
                        resetBackground();
                        clazzes_.add(Color.clazzForColorIndex(codeVal, true /*background*/));
                     }
                  }
                  extendedColor = 0;
                  extendedMarkerSeen = false;
               }
            }
         }
         else if (codeVal == RESET)
         {
            inverted_ = false;
            currentColor_.reset();
            currentBgColor_.reset();
            clazzes_.clear();
            blockClazzes_.clear();
         }
         else if (codeVal == BOLD)
         {
            clazzes_.add(BOLD_STYLE);
         }
         else if (codeVal == BLURRED)
         {
            // NYI clazzes_.add(BLURRED_STYLE);
         }
         else if (codeVal == BOLD_BLURRED_OFF)
         {
            clazzes_.remove(BOLD_STYLE);
            // NYI clazzes_.remove(BLURRED_STYLE);
         }
         else if (codeVal == ITALIC)
         {
            clazzes_.add(ITALIC_STYLE);
         }
         else if (codeVal == ITALIC_OFF)
         {
            clazzes_.remove(ITALIC_STYLE);
         }
         else if (codeVal == UNDERLINE)
         {
            clazzes_.add(UNDERLINE_STYLE);
         }
         else if (codeVal == UNDERLINE_OFF)
         {
            clazzes_.remove(UNDERLINE_STYLE);
         }
         else if (codeVal == BLINKSLOW || codeVal == BLINKFAST)
         {
            clazzes_.add(BLINK_STYLE);
         }
         else if (codeVal == BLINK_OFF)
         {
            clazzes_.remove(BLINK_STYLE);
         }
         else if (codeVal == INVERSE)
         {
            if (!inverted_)
            {
               resetForeground();
               resetBackground();
               Color newFg = invertFgColor();
               Color newBg = invertBgColor();
               currentColor_ = newFg;
               currentBgColor_ = newBg;
               inverted_ = true;
            }
         }
         else if (codeVal == INVERSE_OFF)
         {
            if (inverted_)
            {
               resetForeground();
               resetBackground();
               Color newFg = invertFgColor();
               Color newBg = invertBgColor();
               currentColor_ = newFg;
               currentBgColor_ = newBg;
               inverted_ = false;
            }
         }
         else if (codeVal == HIDDEN)
         {
            clazzes_.add(HIDDEN_STYLE);
         }
         else if (codeVal == HIDDEN_OFF)
         {
            clazzes_.remove(HIDDEN_STYLE);
         }
         else if (codeVal == STRIKETHROUGH)
         {
            clazzes_.add(STRIKETHROUGH_STYLE);
         }
         else if (codeVal == STRIKETHROUGH_OFF)
         {
            clazzes_.remove(STRIKETHROUGH_STYLE);
         }
         else if (Color.isFgColorCode(codeVal))
         {
            if (!inverted_)
               setForegroundColor(codeVal);
            else
               setBackgroundColor(Color.fgToBgColor(codeVal));
         }
         else if (Color.isBgColorCode(codeVal))
         {
            if (!inverted_)
               setBackgroundColor(codeVal);
            else
               setForegroundColor(Color.bgToFgColor(codeVal));
         }
         else if (codeVal == RESET_FOREGROUND)
         {
            if (!inverted_)
            {
               currentColor_.reset();
               resetForeground();
            }
            else
            {
               currentBgColor_.reset();
               resetBackground();
               clazzes_.add(INVERSE_BG_STYLE);
            }
         }
         else if (codeVal == RESET_BACKGROUND)
         {
            if (!inverted_)
            {
               currentBgColor_.reset();
               resetBackground();
            }
            else
            {
               currentColor_.reset();
               resetForeground();
               clazzes_.add(INVERSE_FG_STYLE);
            }
         }
         else if (codeVal == FOREGROUND_EXT)
         {
           extendedColor = codeVal;
           extendedMarkerSeen = false;
         }
         else if (codeVal == BACKGROUND_EXT)
         {
           extendedColor = codeVal;
           extendedMarkerSeen = false;
         }
         else if (codeVal == FONT_NINE)
         {
            blockClazzes_.add(FONT_NINE_STYLE);
         }
         else if (codeVal == DEFAULT_FONT ||
               (codeVal >= FONT_ONE && codeVal <= FONT_EIGHT))
         {
            blockClazzes_.remove(FONT_NINE_STYLE);
         }
         else
         {
            // ignore all others
         }
      }
      return getStyles();
   }

   public static String clazzForColor(int color)
   {
      int index = ForeColorNum.WHITE;

      if (color >= FOREGROUND_MIN && color <= FOREGROUND_MAX)
      {
         index = color - FOREGROUND_MIN;
      }
      else if (color >= FOREGROUND_INTENSE_MIN && color <= FOREGROUND_INTENSE_MAX)
      {
         index = color + 8 - FOREGROUND_INTENSE_MIN;
      }
      return Color.clazzForColorIndex(index, false /*background*/);
   }

   public static String clazzForBgColor(int color)
   {
      int index = BackColorNum.BLACK;

      if (color >= BACKGROUND_MIN && color <= BACKGROUND_MAX)
      {
         index = color - BACKGROUND_MIN;
      }
      else if (color >= BACKGROUND_INTENSE_MIN && color <= BACKGROUND_INTENSE_MAX)
      {
         index = color + 8 - BACKGROUND_INTENSE_MIN;
      }
      return Color.clazzForColorIndex(index,  true /*background*/);
   }

   private void setForegroundColor(int codeVal)
   {
      currentColor_.setCode(codeVal);
      resetForeground();
      clazzes_.add(clazzForColor(codeVal));
   }

   private void setBackgroundColor(int codeVal)
   {
      currentBgColor_ = new Color(false, codeVal);
      resetBackground();
      clazzes_.add(clazzForBgColor(codeVal));
   }

   private AnsiClazzes getStyles()
   {
      AnsiClazzes styles = new AnsiClazzes();

      if (!clazzes_.isEmpty())
      {
         StringBuilder buildClazzes = new StringBuilder();
         Iterator<String> itr = clazzes_.iterator();
         while (itr.hasNext())
         {
            if (buildClazzes.length() > 0)
               buildClazzes.append(" ");
            buildClazzes.append(itr.next());
         }
         styles.inlineClazzes = buildClazzes.toString();
      }

      if (!blockClazzes_.isEmpty())
      {
         // block styles (line-height via font9)
         StringBuilder buildClazzes = new StringBuilder();
         Iterator<String> itr = blockClazzes_.iterator();
         while (itr.hasNext())
         {
            if (buildClazzes.length() > 0)
               buildClazzes.append(" ");
            buildClazzes.append(itr.next());
         }
         styles.blockClazzes = buildClazzes.toString();
      }

      return styles;
   }

   /**
    * Calculates inverse foreground color based on current background color,
    * applies style, and returns new foreground color.
    * @return new foreground color based on the background color
    */
   private Color invertFgColor()
   {
      if (currentBgColor_.defaultColor())
      {
         if (!inverted_)
            clazzes_.add(INVERSE_FG_STYLE);
         return new Color();
      }
      else if (currentBgColor_.isExtended())
      {
         clazzes_.add(Color.clazzForColorIndex(currentBgColor_.code(), false /*background*/));
         return new Color(true /*extended*/, currentBgColor_.code());
      }
      else if (Color.isNormalBgColorCode(currentBgColor_.code()))
      {
         int newFg = currentBgColor_.code() - (BACKGROUND_MIN - FOREGROUND_MIN);
         clazzes_.add(FOREGROUND_STYLE + (newFg - FOREGROUND_MIN));
         return new Color(false /*extended*/, newFg);
      }
      else
      {
         int newFg = currentBgColor_.code() - (BACKGROUND_INTENSE_MIN - FOREGROUND_INTENSE_MIN);
         clazzes_.add(FOREGROUND_STYLE + (newFg + 8 - FOREGROUND_INTENSE_MIN));
         return new Color(false /*extended*/, newFg);
      }
   }

   /**
    * Calculates inverse background color based on current foreground color,
    * applies style, and returns new background color.
    * @return new background color based on the foreground color
    */
   private Color invertBgColor()
   {
      if (currentColor_.defaultColor())
      {
         if (!inverted_)
            clazzes_.add(INVERSE_BG_STYLE);
         return new Color();
      }
      else if (currentColor_.isExtended())
      {
         clazzes_.add(Color.clazzForColorIndex(currentColor_.code(), true /*background*/));
         return new Color(true /*extended*/, currentColor_.code());
      }
      else if (currentColor_.code() >= FOREGROUND_MIN && currentColor_.code() <= FOREGROUND_MAX)
      {
         int newBg = currentColor_.code() + (BACKGROUND_MIN - FOREGROUND_MIN);
         clazzes_.add(BACKGROUND_STYLE + Integer.toString(newBg - BACKGROUND_MIN));
         return new Color(false /*extended*/, newBg);
      }
      else
      {
         int newBg = currentColor_.code() + (BACKGROUND_INTENSE_MIN - FOREGROUND_INTENSE_MIN);
         clazzes_.add(BACKGROUND_STYLE + Integer.toString(newBg + 8 - BACKGROUND_INTENSE_MIN));
         return new Color(false /*extended*/, newBg);
      }
   }

   private void resetForeground()
   {
      for (int i = 0; i < 256; i++)
      {
         clazzes_.remove(Color.clazzForColorIndex(i, false /*background*/));
      }
      clazzes_.remove(INVERSE_FG_STYLE);
   }

   private void resetBackground()
   {
      for (int i = 0; i < 256; i++)
      {
         clazzes_.remove(Color.clazzForColorIndex(i, true /*background*/));
      }
      clazzes_.remove(INVERSE_BG_STYLE);
   }

   public static String prettyPrint(String input)
   {
      // not efficient but only intended for debug/unit testing
      return input.replace("\u001b", "<ESC>")
                  .replace("\7", "<BEL>")
                  .replace("\177", "<DEL>")
                  .replace("\r", "<CR>")
                  .replace("\n", "<LF>")
                  .replace("\f", "<FF>")
                  .replace("\b", "<BS>")
                  .replace("\t", "<TAB>");
   }

   public static String prettyPrintNonCRLF(String input)
   {
      // not efficient but only intended for debug/unit testing
      return input.replace("\u001b", "<ESC>")
                  .replace("\7", "<BEL>")
                  .replace("\177", "<DEL>")
                  .replace("\b", "<BS>");
   }


   // Control characters handled by R console, plus leading character of
   // ANSI escape sequences
   public static final String CONTROL_REGEX = "[\r\b\f\n\u001b\u009b]";

   // RegEx to match ANSI escape codes copied from https://github.com/chalk/ansi-regex
   public static final String ANSI_REGEX =
         "[\u001b\u009b][[()#;?]*(?:[0-9]{1,4}(?:;[0-9]{0,4})*)?[0-9A-PRZcf-nqry=><@]";

   // Match ANSI escape sequences
   public static final Pattern ANSI_ESCAPE_PATTERN = Pattern.create(ANSI_REGEX);

   // Match control characters and start of ANSI sequences
   public static final Pattern CONTROL_PATTERN = Pattern.create(CONTROL_REGEX);

   // RegEx to match complete SGR codes (colors, fonts, appearance)
   public static final String SGR_REGEX =
         "[\u001b\u009b]\\[(?:[0-9]{1,4}(?:;[0-9]{0,4})*)?[m]";

   // Match ANSI SGR escape sequences
   public static final Pattern SGR_ESCAPE_PATTERN = Pattern.create(SGR_REGEX);

   // RegEx to match partial SGR codes (don't have final "m" yet)
   public static final String SGR_PARTIAL_REGEX =
         "[\u001b\u009b]\\[(?:[0-9]{1,4}(?:;[0-9]{0,4})*)?[0-9]";

   // Match partial potential ANSI SGR escape sequences
   public static final Pattern SGR_PARTIAL_ESCAPE_PATTERN = Pattern.create(SGR_PARTIAL_REGEX);

   private Color currentColor_ = new Color();
   private Color currentBgColor_ = new Color();
   private boolean inverted_ = false;

   private final Set<String> clazzes_ = new LinkedHashSet<>();
   private final Set<String> blockClazzes_ = new LinkedHashSet<>();
}
