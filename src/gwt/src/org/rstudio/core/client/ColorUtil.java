/*
 * ColorUtil.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
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

   // Composite 'color' over an opaque 'background', honouring its alpha.
   public static RGBColor over(RGBColor color, RGBColor background)
   {
      double alpha = color.alpha();
      return new RGBColor(
            (int) Math.round(alpha * color.red() + (1 - alpha) * background.red()),
            (int) Math.round(alpha * color.green() + (1 - alpha) * background.green()),
            (int) Math.round(alpha * color.blue() + (1 - alpha) * background.blue()));
   }

   // sRGB <-> CIE L*a*b* (D65). Lab separates lightness (L*) from hue and
   // chroma (a*, b*), so a colour can be made lighter or darker without
   // changing what colour it is.
   public static double[] toLab(RGBColor color)
   {
      double r = linearize(color.red());
      double g = linearize(color.green());
      double b = linearize(color.blue());

      double x = (0.4124564 * r + 0.3575761 * g + 0.1804375 * b) / 0.95047;
      double y = (0.2126729 * r + 0.7151522 * g + 0.0721750 * b);
      double z = (0.0193339 * r + 0.1191920 * g + 0.9503041 * b) / 1.08883;

      double fx = labForward(x);
      double fy = labForward(y);
      double fz = labForward(z);

      return new double[] { 116 * fy - 16, 500 * (fx - fy), 200 * (fy - fz) };
   }

   public static RGBColor fromLab(double l, double a, double b)
   {
      double fy = (l + 16) / 116;
      double fx = fy + a / 500;
      double fz = fy - b / 200;

      double x = labInverse(fx) * 0.95047;
      double y = labInverse(fy);
      double z = labInverse(fz) * 1.08883;

      double r =  3.2404542 * x - 1.5371385 * y - 0.4985314 * z;
      double g = -0.9692660 * x + 1.8760108 * y + 0.0415560 * z;
      double bb = 0.0556434 * x - 0.2040259 * y + 1.0572252 * z;

      return new RGBColor(delinearize(r), delinearize(g), delinearize(bb));
   }

   // CIE76 colour difference; differences above about 10 are plainly visible.
   public static double deltaE(RGBColor x, RGBColor y)
   {
      double[] p = toLab(x);
      double[] q = toLab(y);

      double dl = p[0] - q[0];
      double da = p[1] - q[1];
      double db = p[2] - q[2];
      return Math.sqrt(dl * dl + da * da + db * db);
   }

   // Move 'color' in lightness, away from 'background' and toward
   // 'foreground', until it differs from the background by at least
   // 'minDeltaE'. Hue and chroma are kept, so a grey stays a grey and a blue
   // stays that blue; colours that already stand out are returned as-is. The
   // result never passes the lightness midway between background and
   // foreground, so text drawn over it stays legible in low-contrast themes.
   public static RGBColor separateFromBackground(RGBColor color,
                                                 RGBColor background,
                                                 RGBColor foreground,
                                                 double minDeltaE)
   {
      if (deltaE(color, background) >= minDeltaE)
         return color;

      double[] c = toLab(color);
      double[] bg = toLab(background);
      double[] fg = toLab(foreground);

      // the chroma difference contributes to deltaE already; lightness
      // supplies the rest
      double da = c[1] - bg[1];
      double db = c[2] - bg[2];
      double dl = Math.sqrt(Math.max(0, minDeltaE * minDeltaE - da * da - db * db));

      double midpoint = (bg[0] + fg[0]) / 2;
      double l = (fg[0] > bg[0])
            ? Math.max(c[0], Math.min(bg[0] + dl, midpoint))
            : Math.min(c[0], Math.max(bg[0] - dl, midpoint));

      return fromLab(clamp(l, 0, 100), c[1], c[2]);
   }

   private static double linearize(int channel)
   {
      double c = channel / 255.0;
      return (c <= 0.04045) ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
   }

   private static int delinearize(double value)
   {
      double v = clamp(value, 0, 1);
      double c = (v <= 0.0031308) ? 12.92 * v : 1.055 * Math.pow(v, 1 / 2.4) - 0.055;
      return (int) Math.round(255 * c);
   }

   private static double labForward(double t)
   {
      return (t > 0.008856) ? Math.pow(t, 1.0 / 3) : 7.787 * t + 16.0 / 116;
   }

   private static double labInverse(double t)
   {
      double cubed = t * t * t;
      return (cubed > 0.008856) ? cubed : (t - 16.0 / 116) / 7.787;
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

      public double alpha()
      {
         return alpha_;
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

      public RGBColor withAlpha(double alpha)
      {
         return new RGBColor(red_, green_, blue_, alpha);
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
            return fromRgbCssString(StringUtil.substring(cssString, rgbIdx).trim());

         int hashIdx = cssString.indexOf('#');
         if (hashIdx != -1)
            return fromHex(StringUtil.substring(cssString, hashIdx).trim());

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