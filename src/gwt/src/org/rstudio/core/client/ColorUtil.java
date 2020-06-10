/*
 * ColorUtil.java
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

import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;

public class ColorUtil
{
   private static double clamp(double value, double low, double high)
   {
      if (value < low)
         return low;
      else if (value > high)
         return high;
      return value;
   }
   
   private static int clamp(int value, int low, int high)
   {
      return (int) clamp((double) value, (double) low, (double) high);
   }
   
   private static final native double mix(double left, double right, double mix, double factor)
   /*-{
      if (factor === 1) {
         return ((mix * left) + ((1 - mix) * right));
      }
      
      var lhs = mix * Math.pow(left, factor);
      var rhs = (1 - mix) * Math.pow(right, factor);
      
      return Math.pow(lhs + rhs, 1 / factor);
   }-*/;
   
   private static final int mix(int left, int right, double mix, double factor)
   {
      return (int) mix((double) left, (double) right, mix, factor);
   }
   
   public static class RGBColor
   {
      public RGBColor(int red, int green, int blue, double alpha)
      {
         red_ = clamp(red, 0, 255);
         green_ = clamp(green, 0, 255);
         blue_ = clamp(blue, 0, 255);
         alpha_ = clamp(alpha, 0, 1);
      }
      
      public RGBColor(int red, int green, int blue)
      {
         this(red, green, blue, 1);
      }
      
      private RGBColor()
      {
         this(0, 0, 0, 1);
      }
      
      public int red()
      {
         return red_;
      }
      
      public int green()
      {
         return green_;
      }
      
      public int blue()
      {
         return blue_;
      }
      
      public RGBColor mixedWith(RGBColor other, double ratio, double mode)
      {
         ratio = clamp(ratio, 0, 1);
         return new RGBColor(
               mix(red_, other.red_, ratio, mode),
               mix(green_, other.green_, ratio, mode),
               mix(blue_, other.blue_, ratio, mode),
               mix(alpha_, other.alpha_, ratio, 1));
      }
      
      public static RGBColor fromHex(String hexString)
      {
         // validate
         Match match = RE_HEX.match(hexString, 0);
         if (match == null)
            return new RGBColor();
         
         return new RGBColor(
               hexToInt(match.getGroup(1)),
               hexToInt(match.getGroup(2)),
               hexToInt(match.getGroup(3)));
      }
      
      private static RGBColor fromRgbCssString(String cssString)
      {
         Match match = null;
         Pattern pattern = null;
         
         if (cssString.startsWith("rgba"))
            pattern = RE_RGBA;
         else if (cssString.startsWith("rgb"))
            pattern = RE_RGB;
         
         if (pattern == null)
         {
            Debug.logToConsole("Non-conformant RGB color string: '" + cssString + "'");
            return new RGBColor();
         }
         
         match = pattern.match(cssString, 0);
         if (match == null)
         {
            Debug.logToConsole("Failed to match RGB string: '" + cssString + "'");
            return new RGBColor();
         }
         
         return new RGBColor(
               Integer.parseInt(match.getGroup(1)),
               Integer.parseInt(match.getGroup(2)),
               Integer.parseInt(match.getGroup(3)),
               match.hasGroup(4) ? Double.parseDouble(match.getGroup(4)) : 1);
         
      }
      
      public static RGBColor fromCss(String cssString)
      {
         // NOTE: We 'scrape' this from a CSS field so we allow other
         // content that could be lying around.
         int rgbIdx = cssString.indexOf("rgb");
         if (rgbIdx != -1)
            return fromRgbCssString(cssString.substring(rgbIdx).trim());
         
         int hashIdx = cssString.indexOf('#');
         if (hashIdx != -1)
            return fromHex(cssString.substring(hashIdx).trim());
        
         Debug.logToConsole("Failed to parse CSS '" + cssString + "'");
         return new RGBColor();
      }
      
      private static int hexToInt(String hexValue)
      {
         int result = 0;
         try {
            result = Integer.parseInt(hexValue, 16);
         } finally {}
         
         return result;
      }
      
      public String asHex()
      {
         return "#" +
            toTwoDigitHex(red_) +
            toTwoDigitHex(green_) +
            toTwoDigitHex(blue_);
      }
      
      public String asRgb()
      {
         if (alpha_ == 1)
            return "rgb(" + red_ + ", " + green_ + ", " + blue_ + ")";
         else
            return "rgba(" + red_ + ", " + green_ + ", " + blue_ + ", " + alpha_ + ")";
      }
      
      public boolean isDark()
      {
         double sum =
               Math.pow(red_, 2) +
               Math.pow(green_, 2) +
               Math.pow(blue_, 2);
         
         return sum < MID_GREY;
      }
      
      private static native final String toTwoDigitHex(int value) /*-{
         var result = value.toString(16);
         if (result.length === 1)
            return "0" + result;
         return result.substring(result.length - 2);
      }-*/;
      
      private final int red_;
      private final int green_;
      private final int blue_;
      private final double alpha_;
      
      public static final RGBColor WHITE = new RGBColor(255, 255, 255);
      public static final RGBColor BLACK = new RGBColor(0, 0, 0);
      
      private static final double MID_GREY =
            Math.pow(127, 2) +
            Math.pow(127, 2) +
            Math.pow(127, 2);
      
      private static final String HEX_CAPTURE_PATTERN = "([0-9a-fA-F]{2})";
      private static final Pattern RE_HEX = Pattern.create(
            "^\\s*#" + StringUtil.repeat(HEX_CAPTURE_PATTERN, 3) + "\\s*$");
      
      private static final String RGB_ENTRY_CAPTURE_PATTERN = "\\s*(.*?)\\s*";
      
      private static final Pattern RE_RGB = Pattern.create(
            "^\\s*rgb\\(" +
            RGB_ENTRY_CAPTURE_PATTERN + "," + // red
            RGB_ENTRY_CAPTURE_PATTERN + "," + // green
            RGB_ENTRY_CAPTURE_PATTERN +       // blue
            "\\)\\s*$", "");
      
      private static final Pattern RE_RGBA = Pattern.create(
            "^\\s*rgba\\(" +
            RGB_ENTRY_CAPTURE_PATTERN + "," + // red
            RGB_ENTRY_CAPTURE_PATTERN + "," + // green
            RGB_ENTRY_CAPTURE_PATTERN + "," + // blue
            RGB_ENTRY_CAPTURE_PATTERN +       // alpha
            "\\)\\s*$", "");
            
   }

}
