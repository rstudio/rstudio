/*
 * ThemeColorExtractor.java
 *
 * Copyright (C) 2025 by Posit Software, PBC
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
package org.rstudio.core.client.theme;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.Visibility;

import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.ColorUtil.RGBColor;

/**
 * Utility class for extracting theme colors from the current RStudio theme.
 * Follows the pattern established in ApplicationThemes.onComputeThemeColors().
 *
 * This extracts colors by creating hidden DOM samplers with theme classes,
 * reading their computed styles, and caching the results.
 */
public class ThemeColorExtractor
{
   // Cache to avoid repeated DOM sampling
   private static String cachedThemeName_ = null;
   private static Map<String, String> cachedColors_ = null;

   /**
    * Extract extended CSS variables for theming iframes.
    * Returns a map of CSS variable names to color values.
    * Results are cached by theme name for performance.
    * This includes both essential and extended colors for complete theme matching.
    *
    * @return Map of CSS variable names (e.g. "--rstudio-editor-background") to color values
    */
   public static Map<String, String> extractEssentialColors()
   {
      // Get current theme name for cache key
      String currentThemeName = getCurrentThemeName();

      // Check cache
      if (cachedThemeName_ != null &&
          cachedThemeName_.equals(currentThemeName) &&
          cachedColors_ != null)
      {
         // Return copy of cached map for safety
         return new HashMap<>(cachedColors_);
      }

      // Extract fresh colors (now includes extended palette)
      Map<String, String> colors = doExtractExtendedColors();

      // Cache results
      cachedThemeName_ = currentThemeName;
      cachedColors_ = new HashMap<>(colors);

      return colors;
   }

   /**
    * Clear the color cache. Call this when theme changes.
    */
   public static void clearCache()
   {
      cachedThemeName_ = null;
      cachedColors_ = null;
   }

   /**
    * Internal method that performs the actual color extraction.
    * Creates hidden DOM samplers, reads computed styles, then cleans up.
    * This extracts both essential and extended colors for complete theming.
    */
   private static Map<String, String> doExtractExtendedColors()
   {
      Map<String, String> colors = new HashMap<>();

      try
      {
         Document doc = Document.get();

         // Create hidden sampler element with ace_editor classes
         // This matches the pattern in ApplicationThemes.onComputeThemeColors()
         Element sampler = doc.createDivElement();
         sampler.addClassName("ace_editor");
         sampler.addClassName("ace_content");

         // Hide the sampler
         Style samplerStyle = sampler.getStyle();
         samplerStyle.setVisibility(Visibility.HIDDEN);
         samplerStyle.setPosition(Position.ABSOLUTE);

         // Append to body temporarily to get computed styles
         doc.getBody().appendChild(sampler);

         // Read computed styles from editor
         Style computed = DomUtils.getComputedStyles(sampler);

         // Extract background and foreground colors
         String background = computed.getBackgroundColor();
         String foreground = computed.getColor();

         colors.put("--rstudio-editor-background", background);
         colors.put("--rstudio-editor-foreground", foreground);

         // Clean up sampler
         sampler.removeFromParent();

         // Detect if theme is dark based on background luminance
         boolean isDark = isBackgroundDark(background);

         // Essential colors
         colors.put("--rstudio-focusBorder", deriveFocusBorderColor(foreground, isDark));
         colors.put("--rstudio-button-foreground", foreground);
         colors.put("--rstudio-panel-border", deriveBorderColor(background, foreground, isDark));

         // Extended colors for widgets and popovers
         String widgetBackground = deriveWidgetBackground(background, isDark);
         colors.put("--rstudio-editorWidget-background", widgetBackground);
         colors.put("--rstudio-editorWidget-foreground", foreground);
         colors.put("--rstudio-editorWidget-border", deriveBorderColor(widgetBackground, foreground, isDark));

         // Secondary button colors (less prominent than primary)
         colors.put("--rstudio-button-secondaryBackground", deriveSecondaryButtonBackground(background, isDark));
         colors.put("--rstudio-button-secondaryForeground", foreground);

         // Muted/disabled colors
         colors.put("--rstudio-textBlockQuote-background", deriveMutedBackground(background, isDark));
         colors.put("--rstudio-disabledForeground", deriveDisabledForeground(foreground, isDark));

         // Interactive element colors
         colors.put("--rstudio-list-hoverBackground", deriveHoverBackground(background, isDark));
         colors.put("--rstudio-list-activeSelectionForeground", foreground);

         // Error/destructive color
         colors.put("--rstudio-errorForeground", deriveErrorColor(isDark));

         // Primary button colors (distinct from focus border)
         colors.put("--rstudio-primaryButton-background", derivePrimaryButtonBackground(isDark));
         colors.put("--rstudio-primaryButton-foreground", derivePrimaryButtonForeground(isDark));

         // Selection/message background color
         colors.put("--rstudio-selectionBackground", deriveSelectionBackground(background, isDark));

         // Syntax highlighting colors for code blocks in chat UI
         // Core syntax tokens
         colors.put("--rstudio-syntax-keyword", extractSyntaxColor("ace_keyword", foreground));
         colors.put("--rstudio-syntax-string", extractSyntaxColor("ace_string", foreground));
         colors.put("--rstudio-syntax-comment", extractSyntaxColor("ace_comment", foreground));
         colors.put("--rstudio-syntax-number", extractSyntaxColorMultiClass(
               new String[]{"ace_constant", "ace_numeric"}, foreground));
         colors.put("--rstudio-syntax-function", extractSyntaxColorMultiClass(
               new String[]{"ace_support", "ace_function"}, foreground));
         colors.put("--rstudio-syntax-operator", extractSyntaxColorMultiClass(
               new String[]{"ace_keyword", "ace_operator"}, foreground));

         // Extended syntax tokens
         colors.put("--rstudio-syntax-variable", extractSyntaxColor("ace_variable", foreground));
         colors.put("--rstudio-syntax-punctuation", extractSyntaxColor("ace_punctuation", foreground));
         colors.put("--rstudio-syntax-constant", extractSyntaxColorMultiClass(
               new String[]{"ace_constant", "ace_language"}, foreground));
         colors.put("--rstudio-syntax-class-name", extractSyntaxColorMultiClass(
               new String[]{"ace_support", "ace_class"}, foreground));
         colors.put("--rstudio-syntax-tag", extractSyntaxColorMultiClass(
               new String[]{"ace_meta", "ace_tag"}, foreground));
         colors.put("--rstudio-syntax-attr-name", extractSyntaxColorMultiClass(
               new String[]{"ace_entity", "ace_other", "ace_attribute-name"}, foreground));

      }
      catch (Exception e)
      {
         Debug.logException(e);

         // Fall back to safe defaults if extraction fails
         colors.put("--rstudio-editor-background", "#ffffff");
         colors.put("--rstudio-editor-foreground", "#000000");
         colors.put("--rstudio-focusBorder", "#4d9de0");
         colors.put("--rstudio-button-foreground", "#000000");
         colors.put("--rstudio-panel-border", "#d0d0d0");

         // Extended fallbacks
         colors.put("--rstudio-editorWidget-background", "#f5f5f5");
         colors.put("--rstudio-editorWidget-foreground", "#000000");
         colors.put("--rstudio-editorWidget-border", "#d0d0d0");
         colors.put("--rstudio-button-secondaryBackground", "#e8e8e8");
         colors.put("--rstudio-button-secondaryForeground", "#000000");
         colors.put("--rstudio-textBlockQuote-background", "#f5f5f5");
         colors.put("--rstudio-disabledForeground", "#888888");
         colors.put("--rstudio-list-hoverBackground", "#e8e8e8");
         colors.put("--rstudio-list-activeSelectionForeground", "#000000");
         colors.put("--rstudio-errorForeground", "#dc3545");
         colors.put("--rstudio-primaryButton-background", "#4d9de0");
         colors.put("--rstudio-primaryButton-foreground", "#ffffff");
         colors.put("--rstudio-selectionBackground", "#daeffe");

         // Syntax highlighting fallbacks (VS Code light theme colors)
         colors.put("--rstudio-syntax-keyword", "#0000ff");
         colors.put("--rstudio-syntax-string", "#a31515");
         colors.put("--rstudio-syntax-comment", "#008000");
         colors.put("--rstudio-syntax-number", "#098658");
         colors.put("--rstudio-syntax-function", "#795e26");
         colors.put("--rstudio-syntax-operator", "#000000");
         colors.put("--rstudio-syntax-variable", "#001080");
         colors.put("--rstudio-syntax-punctuation", "#000000");
         colors.put("--rstudio-syntax-constant", "#0000ff");
         colors.put("--rstudio-syntax-class-name", "#267f99");
         colors.put("--rstudio-syntax-tag", "#800000");
         colors.put("--rstudio-syntax-attr-name", "#ff0000");
      }

      return colors;
   }

   /**
    * Determine if the background color is dark based on luminance.
    * Uses the same algorithm as RGBColor.isDark().
    */
   private static boolean isBackgroundDark(String cssColor)
   {
      try
      {
         RGBColor color = RGBColor.fromCss(cssColor);
         if (color != null)
         {
            return color.isDark();
         }
      }
      catch (Exception e)
      {
         // If parsing fails, assume light theme
      }

      return false;
   }

   /**
    * Derive a focus border color appropriate for the theme.
    * For dark themes, use a lighter blue. For light themes, use a darker blue.
    */
   private static String deriveFocusBorderColor(String foreground, boolean isDark)
   {
      if (isDark)
      {
         // Light blue for dark themes
         return "#75b5e7";
      }
      else
      {
         // Darker blue for light themes
         return "#4d9de0";
      }
   }

   /**
    * Derive a border color by mixing background and foreground.
    * Creates a subtle border that works in both light and dark themes.
    */
   private static String deriveBorderColor(String background, String foreground, boolean isDark)
   {
      try
      {
         RGBColor bg = RGBColor.fromCss(background);
         RGBColor fg = RGBColor.fromCss(foreground);

         if (bg != null && fg != null)
         {
            // Mix 85% background with 15% foreground for subtle border
            int r = (int)(bg.red() * 0.85 + fg.red() * 0.15);
            int g = (int)(bg.green() * 0.85 + fg.green() * 0.15);
            int b = (int)(bg.blue() * 0.85 + fg.blue() * 0.15);

            return "rgb(" + r + ", " + g + ", " + b + ")";
         }
      }
      catch (Exception e)
      {
         Debug.logException(e);
      }

      // Fallback borders
      return isDark ? "#3a3a3a" : "#d0d0d0";
   }

   /**
    * Derive a widget/popover background that's slightly different from main background.
    * For dark themes, slightly lighter. For light themes, slightly darker.
    */
   private static String deriveWidgetBackground(String background, boolean isDark)
   {
      try
      {
         RGBColor bg = RGBColor.fromCss(background);
         if (bg != null)
         {
            int adjustment = isDark ? 10 : -10;
            int r = Math.max(0, Math.min(255, bg.red() + adjustment));
            int g = Math.max(0, Math.min(255, bg.green() + adjustment));
            int b = Math.max(0, Math.min(255, bg.blue() + adjustment));
            return "rgb(" + r + ", " + g + ", " + b + ")";
         }
      }
      catch (Exception e)
      {
         Debug.logException(e);
      }
      return isDark ? "#2a2a2a" : "#f5f5f5";
   }

   /**
    * Derive a secondary button background (less prominent than primary).
    */
   private static String deriveSecondaryButtonBackground(String background, boolean isDark)
   {
      try
      {
         RGBColor bg = RGBColor.fromCss(background);
         if (bg != null)
         {
            int adjustment = isDark ? 15 : -15;
            int r = Math.max(0, Math.min(255, bg.red() + adjustment));
            int g = Math.max(0, Math.min(255, bg.green() + adjustment));
            int b = Math.max(0, Math.min(255, bg.blue() + adjustment));
            return "rgb(" + r + ", " + g + ", " + b + ")";
         }
      }
      catch (Exception e)
      {
         Debug.logException(e);
      }
      return isDark ? "#3a3a3a" : "#e8e8e8";
   }

   /**
    * Derive a muted background for blockquotes, etc.
    */
   private static String deriveMutedBackground(String background, boolean isDark)
   {
      try
      {
         RGBColor bg = RGBColor.fromCss(background);
         if (bg != null)
         {
            int adjustment = isDark ? 12 : -12;
            int r = Math.max(0, Math.min(255, bg.red() + adjustment));
            int g = Math.max(0, Math.min(255, bg.green() + adjustment));
            int b = Math.max(0, Math.min(255, bg.blue() + adjustment));
            return "rgb(" + r + ", " + g + ", " + b + ")";
         }
      }
      catch (Exception e)
      {
         Debug.logException(e);
      }
      return isDark ? "#2d2d2d" : "#f5f5f5";
   }

   /**
    * Derive a disabled/muted foreground color.
    */
   private static String deriveDisabledForeground(String foreground, boolean isDark)
   {
      try
      {
         RGBColor fg = RGBColor.fromCss(foreground);
         if (fg != null)
         {
            // Reduce opacity/brightness
            int adjustment = isDark ? -40 : 40;
            int r = Math.max(0, Math.min(255, fg.red() + adjustment));
            int g = Math.max(0, Math.min(255, fg.green() + adjustment));
            int b = Math.max(0, Math.min(255, fg.blue() + adjustment));
            return "rgb(" + r + ", " + g + ", " + b + ")";
         }
      }
      catch (Exception e)
      {
         Debug.logException(e);
      }
      return isDark ? "#888888" : "#666666";
   }

   /**
    * Derive a hover background for list items.
    */
   private static String deriveHoverBackground(String background, boolean isDark)
   {
      try
      {
         RGBColor bg = RGBColor.fromCss(background);
         if (bg != null)
         {
            int adjustment = isDark ? 20 : -20;
            int r = Math.max(0, Math.min(255, bg.red() + adjustment));
            int g = Math.max(0, Math.min(255, bg.green() + adjustment));
            int b = Math.max(0, Math.min(255, bg.blue() + adjustment));
            return "rgb(" + r + ", " + g + ", " + b + ")";
         }
      }
      catch (Exception e)
      {
         Debug.logException(e);
      }
      return isDark ? "#3a3a3a" : "#e8e8e8";
   }

   /**
    * Derive an error/destructive color appropriate for the theme.
    */
   private static String deriveErrorColor(boolean isDark)
   {
      // Use standard red tones that work well in both themes
      return isDark ? "#f48771" : "#dc3545";
   }

   /**
    * Derive a primary button background color appropriate for the theme.
    * Uses RStudio's blue accent color, adapted for light and dark themes.
    */
   private static String derivePrimaryButtonBackground(boolean isDark)
   {
      if (isDark)
      {
         // Slightly lighter blue for dark themes (better contrast)
         return "#5a9fe5";
      }
      else
      {
         // Medium blue for light themes (RStudio's standard blue)
         return "#4d9de0";
      }
   }

   /**
    * Derive a primary button foreground color.
    * Always white for maximum contrast with blue background.
    */
   private static String derivePrimaryButtonForeground(boolean isDark)
   {
      // White text works well on both blue backgrounds
      return "#ffffff";
   }

   /**
    * Derive a selection background color appropriate for the theme.
    * Used for chat message backgrounds, text selections, etc.
    * Should be distinct from main background but still subtle.
    */
   private static String deriveSelectionBackground(String background, boolean isDark)
   {
      try
      {
         RGBColor bg = RGBColor.fromCss(background);
         if (bg != null)
         {
            if (isDark)
            {
               // For dark themes, make selection lighter than background
               // Increase RGB values by 40 for better contrast on very dark backgrounds
               int r = Math.max(0, Math.min(255, bg.red() + 40));
               int g = Math.max(0, Math.min(255, bg.green() + 40));
               int b = Math.max(0, Math.min(255, bg.blue() + 40));
               return "rgb(" + r + ", " + g + ", " + b + ")";
            }
            else
            {
               // For light themes, make selection slightly darker than background
               // Decrease RGB values by 15 for subtle highlighting
               int r = Math.max(0, Math.min(255, bg.red() - 15));
               int g = Math.max(0, Math.min(255, bg.green() - 15));
               int b = Math.max(0, Math.min(255, bg.blue() - 15));
               return "rgb(" + r + ", " + g + ", " + b + ")";
            }
         }
      }
      catch (Exception e)
      {
         Debug.logException(e);
      }

      // Fallback colors aligned with CSS defaults
      return isDark ? "#47789e" : "#daeffe";
   }

   /**
    * Get the current theme name for cache keying.
    * We use a simple hash of background/foreground colors as the theme identifier
    * to avoid dependencies on AceThemes or other theme management code.
    */
   private static String getCurrentThemeName()
   {
      try
      {
         // Sample current colors to create a theme fingerprint
         Document doc = Document.get();
         Element sampler = doc.createDivElement();
         sampler.addClassName("ace_editor");

         Style samplerStyle = sampler.getStyle();
         samplerStyle.setVisibility(Visibility.HIDDEN);
         samplerStyle.setPosition(Position.ABSOLUTE);

         doc.getBody().appendChild(sampler);

         Style computed = DomUtils.getComputedStyles(sampler);
         String bg = computed.getBackgroundColor();
         String fg = computed.getColor();

         sampler.removeFromParent();

         // Use colors as theme identifier
         return bg + "|" + fg;
      }
      catch (Exception e)
      {
         Debug.logException(e);
         return "default";
      }
   }

   /**
    * Extract a syntax highlighting color from a single Ace CSS class.
    * Returns the foreground color as fallback if extraction fails.
    *
    * @param className The Ace CSS class (e.g., "ace_keyword")
    * @param fallback Fallback color if extraction fails
    * @return The extracted color value
    */
   private static String extractSyntaxColor(String className, String fallback)
   {
      try
      {
         String color = DomUtils.extractCssValue(className, "color");
         if (color != null && !color.isEmpty())
         {
            return color;
         }
      }
      catch (Exception e)
      {
         Debug.logException(e);
      }
      return fallback;
   }

   /**
    * Extract a syntax highlighting color from multiple Ace CSS classes.
    * The classes are applied to a single element (compound selector).
    * This matches Ace theme CSS like ".ace_constant.ace_numeric { color: ... }"
    * Returns the foreground color as fallback if extraction fails.
    *
    * @param classNames Array of Ace CSS classes (e.g., {"ace_constant", "ace_numeric"})
    * @param fallback Fallback color if extraction fails
    * @return The extracted color value
    */
   private static String extractSyntaxColorMultiClass(String[] classNames, String fallback)
   {
      try
      {
         // Join class names with spaces to create a compound selector
         // e.g., "ace_constant ace_numeric" creates <div class="ace_constant ace_numeric">
         // which matches CSS like .ace_constant.ace_numeric { color: ... }
         String classNamesStr = String.join(" ", classNames);
         String color = DomUtils.extractCssValue(classNamesStr, "color");
         if (color != null && !color.isEmpty())
         {
            return color;
         }
      }
      catch (Exception e)
      {
         Debug.logException(e);
      }
      return fallback;
   }
}
