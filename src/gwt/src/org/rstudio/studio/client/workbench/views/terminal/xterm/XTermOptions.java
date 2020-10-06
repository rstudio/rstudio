/*
 * XTermOptions.java
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
package org.rstudio.studio.client.workbench.views.terminal.xterm;

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

/**
 * An object containing start up options for the terminal (ITerminalOptions).
 * https://github.com/xtermjs/xterm.js/blob/4.7.0/typings/xterm.d.ts
 */
@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
public class XTermOptions
{
   /**
    * Whether to allow the use of proposed API. When false, any usage of APIs
    * marked as experimental/proposed will throw an error. This defaults to
    * true currently, but will change to false in v5.0.
    */
   public boolean allowProposedApi;

   /**
    * Whether background should support non-opaque color. It must be set before
    * executing the `Terminal.open()` method and can't be changed later without
    * executing it again. Note that enabling this can negatively impact
    * performance.
    */
   public boolean allowTransparency;

   /**
    * A data uri of the sound to use for the bell when `bellStyle = 'sound'`.
    */
   public String bellSound;

   /**
    * The type of the bell notification the terminal will use.
    * 'none' | 'sound'
    */
   public String bellStyle;

   /**
    * When enabled the cursor will be set to the beginning of the next line
    * with every new line. This is equivalent to sending '\r\n' for each '\n'.
    * Normally the termios settings of the underlying PTY deals with the
    * translation of '\n' to '\r\n' and this setting should not be used. If you
    * deal with data from a non-PTY related source, this settings might be
    * useful.
    */
   public boolean convertEol;

   /**
    * The number of columns in the terminal.
    */
   public int cols;

   /**
    * Whether the cursor blinks.
    */
   public boolean cursorBlink;

   /**
    * The style of the cursor.
    * 'block' | 'underline' | 'bar'
    */
   public String cursorStyle;

   /**
    * The width of the cursor in CSS pixels when `cursorStyle` is set to 'bar'.
    */
   public int cursorWidth;

   /**
    * Whether input should be disabled.
    */
   public boolean disableStdin;

   /**
    * Whether to draw bold text in bright colors. The default is true.
    */
   public boolean drawBoldTextInBrightColors;

   /**
    * The modifier key hold to multiply scroll speed.
    * 'alt' | 'ctrl' | 'shift' | undefined
    */
   public String fastScrollModifier;

   /**
    * The scroll speed multiplier used for fast scrolling.
    */
   public double fastScrollSensitivity;

   /**
    * The font size used to render text.
    */
   public double fontSize;

   /**
    * The font family used to render text.
    */
   public String fontFamily;

   /**
    * The font weight used to render non-bold text.
    * 'normal' | 'bold' | '100' | '200' | '300' | '400' | '500' | '600' | '700' | '800' | '900'
    */
   public String fontWeight;

   /**
    * The font weight used to render bold text.
    * 'normal' | 'bold' | '100' | '200' | '300' | '400' | '500' | '600' | '700' | '800' | '900'
    */
   public String fontWeightBold;

   /**
    * The spacing in whole pixels between characters.
    */
   public int letterSpacing;

   /**
    * The line height used to render text.
    */
   public double lineHeight;

   /**
    * What log level to use, this will log for all levels below and including
    * what is set:
    *
    * 1. debug
    * 2. info (default)
    * 3. warn
    * 4. error
    * 5. off
    */
   public String logLevel;

   /**
    * Whether to treat option as the meta key.
    */
   public boolean macOptionIsMeta;

   /**
    * Whether holding a modifier key will force normal selection behavior,
    * regardless of whether the terminal is in mouse events mode. This will
    * also prevent mouse events from being emitted by the terminal. For
    * example, this allows you to use xterm.js' regular selection inside tmux
    * with mouse mode enabled.
    */
   public boolean macOptionClickForcesSelection;

   /**
    * The minimum contrast ratio for text in the terminal, setting this will
    * change the foreground color dynamically depending on whether the contrast
    * ratio is met. Example values:
    *
    * - 1: The default, do nothing.
    * - 4.5: Minimum for WCAG AA compliance.
    * - 7: Minimum for WCAG AAA compliance.
    * - 21: White on black or black on white.
    */
   public double minimumContrastRatio;

   /**
    * The type of renderer to use, this allows using the fallback DOM renderer
    * when canvas is too slow for the environment. The following features do
    * not work when the DOM renderer is used:
    *
    * - Letter spacing
    * - Cursor blink
    *
    * 'dom' | 'canvas'
    */
   public String rendererType;

   /**
    * Whether to select the word under the cursor on right click, this is
    * standard behavior in a lot of macOS applications.
    */
   public boolean rightClickSelectsWord;

   /**
    * The number of rows in the terminal.
    */
   public int rows;

   /**
    * Whether screen reader support is enabled. When on this will expose
    * supporting elements in the DOM to support NVDA on Windows and VoiceOver
    * on macOS.
    */
   public boolean screenReaderMode;

   /**
    * The amount of scrollback in the terminal. Scrollback is the amount of
    * rows that are retained when lines are scrolled beyond the initial
    * viewport.
    */
   public int scrollback;

   /**
    * The scrolling speed multiplier used for adjusting normal scrolling speed.
    */
   public double scrollSensitivity;

   /**
    * The size of tab stops in the terminal.
    */
   public double tabStopWidth;

   /**
    * The color theme of the terminal.
    */
   public XTermTheme theme;

   /**
    * Whether "Windows mode" is enabled. Because Windows backends winpty and
    * conpty operate by doing line wrapping on their side, xterm.js does not
    * have access to wrapped lines. When Windows mode is enabled the following
    * changes will be in effect:
    *
    * - Reflow is disabled.
    * - Lines are assumed to be wrapped if the last character of the line is
    *   not whitespace.
    */
   public boolean windowsMode;

   /**
    * A string containing all characters that are considered word separated by the
    * double click to select work logic.
    */
   public String wordSeparator;

   /*
    * Enable various window manipulation and report features.
    * All features are disabled by default for security reasons.
    */
   XTermWindowOptions windowOptions;

   @JsOverlay public static XTermOptions create(
         String bellStyle,
         boolean cursorBlink,
         boolean screenReaderMode,
         String rendererType,
         boolean windowsMode,
         XTermTheme theme,
         String fontFamily,
         double fontSize,
         double lineHeight)
   {
      XTermOptions options = new XTermOptions();
      options.bellStyle = bellStyle;
      options.cursorBlink = cursorBlink;
      options.screenReaderMode = screenReaderMode;
      options.rendererType = rendererType;
      options.windowsMode = windowsMode;
      options.theme = theme;
      options.fontFamily = fontFamily;
      options.fontSize = fontSize;
      options.lineHeight = lineHeight;
      return options;
   }
}
