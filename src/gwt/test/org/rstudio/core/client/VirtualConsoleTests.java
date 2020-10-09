/*
 * VirtualConsoleTests.java
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

import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.PreElement;
import com.google.gwt.junit.client.GWTTestCase;

import junit.framework.Assert;

// Note on test coverage: the VirtualConsole has two side effects; it operates on a
// DOM, if supplied, and also maintains an internal string with the output results.
// The "consolify" tests are testing the internal string only, and most of the other
// tests are only testing the DOM. Ideally these should all be extended to do both.

public class VirtualConsoleTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   private static class FakePrefs implements VirtualConsole.Preferences
   {
      @Override
      public int truncateLongLinesInConsoleHistory()
      {
         return truncateLines_;
      }

      @Override
      public String consoleAnsiMode()
      {
         return ansiMode_;
      }

      @Override
      public boolean screenReaderEnabled()
      {
         return screenReaderEnabled_;
      }

      @Override
      public boolean limitConsoleVisible()
      {
         return limitConsoleVisible_;
      }

      public boolean limitConsoleVisible_ = false;
      public int truncateLines_ = 1000;
      public String ansiMode_ = UserPrefs.ANSI_CONSOLE_MODE_ON;
      public boolean screenReaderEnabled_ = false;
   }

   private static String consolify(String text)
   {
      VirtualConsole console = new VirtualConsole(null, new FakePrefs());
      console.submit(text);
      return console.toString();
   }

   private VirtualConsole getVC(Element ele)
   {
      return new VirtualConsole(ele, new FakePrefs());
   }

   private static String setCsiCode(int code)
   {
      return setCsiCode(String.valueOf(code));
   }

   private static String setCsiCode(String code)
   {
      return AnsiCode.CSI + code + AnsiCode.SGR;
   }

   private static String setForegroundIndex(int color)
   {
      return setCsiCode(AnsiCode.FOREGROUND_EXT + ";" + AnsiCode.EXT_BY_INDEX + ";" + color);
   }

   private static String setBackgroundIndex(int color)
   {
      return setCsiCode(AnsiCode.BACKGROUND_EXT + ";" + AnsiCode.EXT_BY_INDEX + ";" + color);
   }

   private static String inverseOn()
   {
      return setCsiCode(AnsiCode.INVERSE);
   }

   private static String inverseOff()
   {
      return setCsiCode(AnsiCode.INVERSE_OFF);
   }

   // ---- tests start here ----

   public void testSimpleText()
   {
      String simple = consolify("foo");
      Assert.assertEquals("foo", simple);
   }

   public void testEmbeddedBackspace()
   {
      String backspace = consolify("bool\bk");
      Assert.assertEquals("book", backspace);
   }

   public void testTrailingBackspace()
   {
      String backspace = consolify("bool\bk");
      Assert.assertEquals("book", backspace);
   }

   public void testCarriageReturn()
   {
      String cr = consolify("hello\rj");
      Assert.assertEquals("jello", cr);
   }

   public void testNewlineCarriageReturn()
   {
      String cr = consolify("L1\nL2\rL3");
      Assert.assertEquals("L1\nL3", cr);
   }

   public void testSimpleColor()
   {
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = getVC(ele);
      vc.submit("Error", "error");
      Assert.assertEquals(
            "<span class=\"error\">Error</span>",
            ele.getInnerHTML());
   }

   public void testTwoColors()
   {
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = getVC(ele);
      vc.submit("Output 1", "one");
      vc.submit("Output 2", "two");
      Assert.assertEquals(
            "<span class=\"one\">Output 1</span>" +
            "<span class=\"two\">Output 2</span>",
            ele.getInnerHTML());
   }

   public void testColorOverwrite()
   {
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = getVC(ele);
      vc.submit("XXXX\r", "X");
      vc.submit("YY", "Y");
      Assert.assertEquals(
            "<span class=\"Y\">YY</span>" +
            "<span class=\"X\">XX</span>",
            ele.getInnerHTML());
   }

   public void testColorSplit()
   {
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = getVC(ele);
      vc.submit("123456");
      vc.submit("\b\b\b\bXX", "X");
      Assert.assertEquals(
            "<span>12</span>" +
            "<span class=\"X\">XX</span>" +
            "<span>56</span>",
            ele.getInnerHTML());
   }

   public void testColorOverlap()
   {
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = getVC(ele);
      vc.submit("123", "A");
      vc.submit("456", "B");
      vc.submit("\b\b\b\bXX", "X");
      Assert.assertEquals(
            "<span class=\"A\">12</span>" +
            "<span class=\"X\">XX</span>" +
            "<span class=\"B\">56</span>",
            ele.getInnerHTML());
   }

   public void testFormFeed()
   {
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = getVC(ele);
      vc.submit("Sample1\n");
      vc.submit("Sample2\n");
      vc.submit("Sample3\f");
      vc.submit("Sample4");
      Assert.assertEquals("<span>Sample4</span>", ele.getInnerHTML());
   }

   public void testCarriageReturnWithStyleChange()
   {
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = getVC(ele);

      vc.submit("World", "a");

      // replace first character of line with green 'X', then change
      // second character to red 'Y'
      vc.submit("\r\033[32mX\033[31mY\033[0m", "a");
      Assert.assertEquals(
            "<span class=\"a xtermColor2\">X</span>" +
            "<span class=\"a xtermColor1\">Y</span>" +
            "<span class=\"a\">rld</span>",
            ele.getInnerHTML());
   }

   public void testCarriageReturnWithStyleChange2()
   {
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = getVC(ele);

      vc.submit("Hello\nWorld", "a");

      // replace first character of current line with green 'X', then change
      // second character to red 'Y'
      vc.submit("\r\033[32mX\033[31mY\033[0m", "a");
      Assert.assertEquals(
            "<span class=\"a\">Hello\n</span>" +
            "<span class=\"a xtermColor2\">X</span>" +
            "<span class=\"a xtermColor1\">Y</span>" +
            "<span class=\"a\">rld</span>",
            ele.getInnerHTML());
   }

   public void testCarriageReturnWithStyleChange3()
   {
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = getVC(ele);

      vc.submit("Hello\nWorld", "a");

      vc.submit("\r\033[32m123\033[31m45\033[0m", "a");
      Assert.assertEquals(
            "<span class=\"a\">Hello\n</span>" +
            "<span class=\"a xtermColor2\">123</span>" +
            "<span class=\"a\"></span>" +
            "<span class=\"a xtermColor1\">45</span>",
            ele.getInnerHTML());
   }

   public void testAnsiColorStyleHelper()
   {
      Assert.assertEquals("xtermColor0",
            AnsiCode.clazzForColor(AnsiCode.FOREGROUND_MIN));
      Assert.assertEquals("xtermColor7",
            AnsiCode.clazzForColor(AnsiCode.FOREGROUND_MAX));
      Assert.assertEquals("xtermBgColor0",
            AnsiCode.clazzForBgColor(AnsiCode.BACKGROUND_MIN));
      Assert.assertEquals("xtermBgColor7",
            AnsiCode.clazzForBgColor(AnsiCode.BACKGROUND_MAX));
   }

   public void testAnsiIntenseColorStyleHelper()
   {
      Assert.assertEquals("xtermColor8",
            AnsiCode.clazzForColor(AnsiCode.FOREGROUND_INTENSE_MIN));
      Assert.assertEquals("xtermColor15",
            AnsiCode.clazzForColor(AnsiCode.FOREGROUND_INTENSE_MAX));
      Assert.assertEquals("xtermBgColor8",
            AnsiCode.clazzForBgColor(AnsiCode.BACKGROUND_INTENSE_MIN));
      Assert.assertEquals("xtermBgColor15",
            AnsiCode.clazzForBgColor(AnsiCode.BACKGROUND_INTENSE_MAX));
   }

   public void testMinAnsiForegroundColor()
   {
      int color = AnsiCode.FOREGROUND_MIN;
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = getVC(ele);
      vc.submit(setCsiCode(color) + "Hello");
      String expected ="<span class=\"xtermColor0\">Hello</span>";
      Assert.assertEquals(expected, ele.getInnerHTML());
   }

   public void testMaxAnsiForegroundColor()
   {
      int color = AnsiCode.FOREGROUND_MAX;
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = getVC(ele);
      vc.submit(setCsiCode(color) + "Hello World");
      String expected ="<span class=\"xtermColor7\">Hello World</span>";
      Assert.assertEquals(expected, ele.getInnerHTML());
   }

   public void testMinAnsiBgColor()
   {
      int color = AnsiCode.BACKGROUND_MIN;
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = getVC(ele);
      vc.submit(setCsiCode(color) + "pretty");
      String expected ="<span class=\"" +
            AnsiCode.clazzForBgColor(color) + "\">pretty</span>";
      Assert.assertEquals(expected, ele.getInnerHTML());
   }

   public void testMaxAnsiBgColor()
   {
      int color = AnsiCode.BACKGROUND_MAX;
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = getVC(ele);
      vc.submit(setCsiCode(color) + "colors");
      String expected ="<span class=\"" +
            AnsiCode.clazzForBgColor(color) + "\">colors</span>";
      Assert.assertEquals(expected, ele.getInnerHTML());
   }

   public void testResetAnsiForegroundColor()
   {
      int color = AnsiCode.ForeColorNum.RED;
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = getVC(ele);
      vc.submit(
            setCsiCode(color) + "Hello " +
            setCsiCode(AnsiCode.RESET_FOREGROUND) + "World");
      String expected =
            "<span class=\"" +
            AnsiCode.clazzForColor(color) + "\">Hello </span>" +
            "<span>World</span>";
      Assert.assertEquals(expected, ele.getInnerHTML());
   }

   public void testResetAnsiBgColor()
   {
      int color = AnsiCode.BackColorNum.GREEN;
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = getVC(ele);
      vc.submit(
            setCsiCode(color) + "Sunny " +
            setCsiCode(AnsiCode.RESET_BACKGROUND) + "Days");
      String expected = "<span class=\"" + AnsiCode.clazzForBgColor(color) +
            "\">Sunny </span><span>Days</span>";
      Assert.assertEquals(expected, ele.getInnerHTML());
   }

   public void testAnsiForegroundAndBgColor()
   {
      int color = AnsiCode.ForeColorNum.MAGENTA;
      int bgColor = AnsiCode.BackColorNum.GREEN;
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = getVC(ele);
      vc.submit(setCsiCode(color) + setCsiCode(bgColor) + "Hello");
      String expected ="<span class=\"" +
            AnsiCode.clazzForColor(color) + " " +
            AnsiCode.clazzForBgColor(bgColor) + "\">Hello</span>";
      Assert.assertEquals(expected, ele.getInnerHTML());
   }

   public void testResetAnsiColors()
   {
      int color = AnsiCode.ForeColorNum.YELLOW;
      int bgColor = AnsiCode.BackColorNum.CYAN;
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = getVC(ele);
      vc.submit(
            setCsiCode(color) +
            setCsiCode(bgColor) + "Colorful " +
            setCsiCode(AnsiCode.RESET) + "Bland");
      String expected ="<span class=\"" +
            AnsiCode.clazzForColor(color) + " " +
            AnsiCode.clazzForBgColor(bgColor) +
            "\">Colorful </span><span>Bland</span>";
      Assert.assertEquals(expected, ele.getInnerHTML());
   }

   public void testMinAnsiIntenseForegroundColor()
   {
      int color = AnsiCode.FOREGROUND_INTENSE_MIN;
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = getVC(ele);
      vc.submit(setCsiCode(color) + "Hello");
      String expected ="<span class=\"" +
            AnsiCode.clazzForColor(color) +
            "\">Hello</span>";
      Assert.assertEquals(expected, ele.getInnerHTML());
   }

   public void testMaxAnsiIntenseForegroundColor()
   {
      int color = AnsiCode.FOREGROUND_INTENSE_MAX;
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = getVC(ele);
      vc.submit(setCsiCode(color) + "Hello World");
      String expected ="<span class=\"" +
            AnsiCode.clazzForColor(color) + "\">Hello World</span>";
      Assert.assertEquals(expected, ele.getInnerHTML());
   }

   public void testMinAnsiIntenseBgColor()
   {
      int color = AnsiCode.BACKGROUND_INTENSE_MIN;
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = getVC(ele);
      vc.submit(setCsiCode(color) + "pretty");
      String expected ="<span class=\"" +
            AnsiCode.clazzForBgColor(color) + "\">pretty</span>";
      Assert.assertEquals(expected, ele.getInnerHTML());
   }

   public void testMaxAnsiIntenseBgColor()
   {
      int color = AnsiCode.BACKGROUND_INTENSE_MAX;
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = getVC(ele);
      vc.submit(AnsiCode.CSI + color + AnsiCode.SGR + "colors");
      String expected ="<span class=\"" +
            AnsiCode.clazzForBgColor(color) + "\">colors</span>";
      Assert.assertEquals(expected, ele.getInnerHTML());
   }

   public void testResetAnsiIntenseForegroundColor()
   {
      int color = AnsiCode.FOREGROUND_INTENSE_MIN + 1;
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = getVC(ele);
      vc.submit(
            AnsiCode.CSI + color + AnsiCode.SGR + "Hello " +
            AnsiCode.CSI + AnsiCode.RESET_FOREGROUND + AnsiCode.SGR + "World");
      String expected =
            "<span class=\"" +
            AnsiCode.clazzForColor(color) + "\">Hello </span>" +
            "<span>World</span>";
      Assert.assertEquals(expected, ele.getInnerHTML());
   }

   public void testResetAnsiIntenseBgColor()
   {
      int color = AnsiCode.BACKGROUND_INTENSE_MIN + 1;
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = getVC(ele);
      vc.submit(
            AnsiCode.CSI + color + AnsiCode.SGR + "Sunny " +
            AnsiCode.CSI + AnsiCode.RESET_BACKGROUND + AnsiCode.SGR + "Days");
      String expected = "<span class=\"" + AnsiCode.clazzForBgColor(color) +
            "\">Sunny </span><span>Days</span>";
      Assert.assertEquals(expected, ele.getInnerHTML());
   }

   public void testAnsiInvertDefaultColors()
   {
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = getVC(ele);
      vc.submit(inverseOn() + "Sunny Days");
      String expected = "<span class=\"xtermInvertColor xtermInvertBgColor\"" +
            ">Sunny Days</span>";
      Assert.assertEquals(expected, ele.getInnerHTML());
   }

   public void testAnsiInvertRevertDefaultColors()
   {
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = getVC(ele);
      vc.submit(
            inverseOn() + "Sunny " +
            inverseOff() + "Days");
      String expected = "<span class=\"xtermInvertColor xtermInvertBgColor\"" +
            ">Sunny </span><span>Days</span>";
      Assert.assertEquals(expected, ele.getInnerHTML());
   }

   public void testAnsiInvertCustomBgColor()
   {
      int bgColor = AnsiCode.BackColorNum.GREEN;
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = getVC(ele);
      vc.submit(
            AnsiCode.CSI + bgColor + AnsiCode.SGR +
            inverseOn() + "Sunny Days");
      String expected = "<span class=\"xtermColor2 xtermInvertBgColor\"" +
            ">Sunny Days</span>";
      Assert.assertEquals(expected, ele.getInnerHTML());
   }

   public void testAnsiInvertCustomFgColor()
   {
      int fgColor = AnsiCode.ForeColorNum.BLUE;
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = getVC(ele);
      vc.submit(
            AnsiCode.CSI + fgColor + AnsiCode.SGR +
            inverseOn() + "Sunny Days");
      String expected = "<span class=\"xtermInvertColor xtermBgColor4\"" +
            ">Sunny Days</span>";
      Assert.assertEquals(expected, ele.getInnerHTML());
   }

   public void testAnsiForeground256Color()
   {
      int color = 120;
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = getVC(ele);
      vc.submit(setForegroundIndex(color) + "Hello World");
      String expected ="<span class=\"xtermColor120\">Hello World</span>";
      Assert.assertEquals(expected, ele.getInnerHTML());
   }

   public void testAnsiBackground256Color()
   {
      int color = 213;
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = getVC(ele);
      vc.submit(setBackgroundIndex(color) + "Hello World");
      String expected ="<span class=\"xtermBgColor213\">Hello World</span>";
      Assert.assertEquals(expected, ele.getInnerHTML());
   }

   public void testAnsi256Color()
   {
      int color = 65;
      int bgColor = 252;
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = getVC(ele);
      vc.submit(setForegroundIndex(color) + setBackgroundIndex(bgColor) + "Hello World");
      String expected ="<span class=\"xtermColor65 xtermBgColor252\">Hello World</span>";
      Assert.assertEquals(expected, ele.getInnerHTML());
   }

   public void testAnsiInvert256Color()
   {
      int color = 61;
      int bgColor = 129;
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = getVC(ele);
      vc.submit(setForegroundIndex(color) + setBackgroundIndex(bgColor) +
            inverseOn() + "Hello World");
      String expected ="<span class=\"xtermColor129 xtermBgColor61\">Hello World</span>";
      Assert.assertEquals(expected, ele.getInnerHTML());
   }

   public void testAnsiInvert256FgColor()
   {
      int color = 77;
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = getVC(ele);
      vc.submit(AnsiCode.CSI + AnsiCode.FOREGROUND_EXT + ";" +
            AnsiCode.EXT_BY_INDEX + ";" + color + AnsiCode.SGR +
            inverseOn() + "Hello World");
      String expected ="<span class=\"xtermInvertColor xtermBgColor77\">Hello World</span>";
      Assert.assertEquals(expected, ele.getInnerHTML());
   }

   public void testAnsiInvert256BgColor()
   {
      int bgColor = 234;
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = getVC(ele);
      vc.submit(AnsiCode.CSI + AnsiCode.BACKGROUND_EXT + ";" +
            AnsiCode.EXT_BY_INDEX + ";" + bgColor + AnsiCode.SGR +
            inverseOn() + "Hello World");
      String expected ="<span class=\"xtermColor234 xtermInvertBgColor\">Hello World</span>";
      Assert.assertEquals(expected, ele.getInnerHTML());
   }

   public void testAnsiComplexSequence1()
   {
      int fgColor = AnsiCode.ForeColorNum.GREEN;
      int bgColor = 230;
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = getVC(ele);
      vc.submit(
         AnsiCode.CSI + fgColor + ";" +
         AnsiCode.BACKGROUND_EXT + ";" + AnsiCode.EXT_BY_INDEX + ";" + bgColor + ";" +
         AnsiCode.BOLD + ";" + AnsiCode.UNDERLINE + ";" + AnsiCode.BOLD_BLURRED_OFF + ";" +
         AnsiCode.INVERSE_OFF + AnsiCode.SGR + "Hello World");
      String expected ="<span class=\"xtermColor2 xtermBgColor230 xtermUnderline\">Hello World</span>";
      Assert.assertEquals(expected, ele.getInnerHTML());
   }

   public void testAnsiComplexSequence2()
   {
      int fgColor = AnsiCode.ForeColorNum.GREEN;
      int bgColor = 230;
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = getVC(ele);
      vc.submit(
         AnsiCode.CSI + fgColor + ";" +
         AnsiCode.BACKGROUND_EXT + ";" + AnsiCode.EXT_BY_INDEX + ";" + bgColor + ";" +
         AnsiCode.BOLD + ";" + AnsiCode.UNDERLINE + ";" + AnsiCode.BOLD_BLURRED_OFF + ";" +
         AnsiCode.INVERSE_OFF + AnsiCode.SGR + "Hello World",
         "existingStyle moreExisting");
      String expected ="<span class=\"existingStyle moreExisting xtermColor2 xtermBgColor230 xtermUnderline\">Hello World</span>";
      Assert.assertEquals(expected, ele.getInnerHTML());
   }

   public void testAnsiUnsupportedRGBExtendedColorScheme()
   {
      // don't currently support specifying color via RGB, which is
      // ESC[38;2;r;g;bm or ESC[48;2;r;g;bm
      // in those cases want to ignore the RGB sequence and carry on
      // processing following codes
      int red = 123;
      int green = 231;
      int blue = 121;
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = getVC(ele);
      vc.submit(
         AnsiCode.CSI + AnsiCode.FOREGROUND_EXT + ";" + AnsiCode.EXT_BY_RGB + ";" +
         red + ";" + green + ";" + blue + ";" + AnsiCode.BOLD + AnsiCode.SGR +
         "Hello World");
      String expected ="<span class=\"xtermBold\">Hello World</span>";
      Assert.assertEquals(expected, ele.getInnerHTML());
   }

   public void testAnsiMultiUnsupportedRGBExtendedColorScheme()
   {
      int red = 123;
      int green = 231;
      int blue = 121;
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = getVC(ele);
      vc.submit(
         AnsiCode.CSI + AnsiCode.BOLD + ";" +
         AnsiCode.FOREGROUND_EXT + ";" + AnsiCode.EXT_BY_RGB + ";" +
         red + ";" + green + ";" + blue + ";" +
         AnsiCode.UNDERLINE + ";" +
         AnsiCode.BACKGROUND_EXT + ";" + AnsiCode.EXT_BY_RGB + ";" +
         red + ";" + green + ";" + blue + ";" +
         AnsiCode.FOREGROUND_EXT + ";" + AnsiCode.EXT_BY_INDEX + ";" + 255 +
         AnsiCode.SGR +
         "Hello World");
      String expected ="<span class=\"xtermBold xtermUnderline xtermColor255\">Hello World</span>";
      Assert.assertEquals(expected, ele.getInnerHTML());
   }

   public void testAnsiCodeSplitAcrossSubmits()
   {
      int color = AnsiCode.ForeColorNum.MAGENTA;
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = getVC(ele);
      vc.submit(AnsiCode.CSI + color);
      vc.submit(AnsiCode.SGR + "Hello");
      String expected ="<span class=\"" +
            AnsiCode.clazzForColor(color) + "\">Hello</span>";
      Assert.assertEquals(expected, ele.getInnerHTML());
   }

   public void testAnsiCodeSplitAcrossSubmitsWithParentStyle()
   {
      int color = AnsiCode.ForeColorNum.MAGENTA;
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = getVC(ele);
      vc.submit(AnsiCode.CSI + color, "myStyle");
      vc.submit(AnsiCode.SGR + "Hello", "myStyle");
      String expected ="<span class=\"myStyle " +
            AnsiCode.clazzForColor(color) + "\">Hello</span>";
      Assert.assertEquals(expected, ele.getInnerHTML());
   }

   public void testMultipleAnsiCodesSplitsAcrossSubmits()
   {
      int color = AnsiCode.ForeColorNum.MAGENTA;
      int bgColor = AnsiCode.BackColorNum.GREEN;
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = getVC(ele);
      vc.submit(AnsiCode.CSI + color);
      vc.submit(AnsiCode.SGR + AnsiCode.CSI);
      vc.submit(Integer.toString(bgColor));
      vc.submit(AnsiCode.SGR + "Hello");
      String expected ="<span class=\"" +
            AnsiCode.clazzForColor(color) + " " +
            AnsiCode.clazzForBgColor(bgColor) + "\">Hello</span>";
      Assert.assertEquals(expected, ele.getInnerHTML());
   }

   public void testAnsiCodeAtEndOfSubmitCall()
   {
      int color = AnsiCode.ForeColorNum.MAGENTA;
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = getVC(ele);
      vc.submit(AnsiCode.CSI + color);
      vc.submit(AnsiCode.SGR);
      vc.submit("Hello");
      String expected ="<span class=\"" +
            AnsiCode.clazzForColor(color) + "\">Hello</span>";
      Assert.assertEquals(expected, ele.getInnerHTML());
   }

   public void testSplitAnsiCodeFollowedByMoreTextSubmits()
   {
      int color = AnsiCode.ForeColorNum.MAGENTA;
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = getVC(ele);
      vc.submit(AnsiCode.CSI + color);
      vc.submit(AnsiCode.SGR);
      vc.submit("Hello");
      vc.submit(" World");
      String expected ="<span class=\"" +
            AnsiCode.clazzForColor(color) + "\">Hello World</span>";
      Assert.assertEquals(expected, ele.getInnerHTML());
   }

   public void testAnsiCodeFollowedByMoreTextSubmits()
   {
      int color = AnsiCode.ForeColorNum.RED;
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = getVC(ele);
      vc.submit(AnsiCode.CSI + color + AnsiCode.SGR, "someClazz");
      vc.submit("Hello World", "someClazz");
      String expected ="<span class=\"someClazz " +
            AnsiCode.clazzForColor(color) + "\">Hello World</span>";
      Assert.assertEquals(expected, ele.getInnerHTML());
   }

   public void testAnsiComplexSequence1WithStripping()
   {
      int fgColor = AnsiCode.ForeColorNum.GREEN;
      int bgColor = 230;
      PreElement ele = Document.get().createPreElement();
      FakePrefs prefs = new FakePrefs();
      prefs.ansiMode_ = UserPrefs.ANSI_CONSOLE_MODE_STRIP;
      VirtualConsole vc = new VirtualConsole(ele, prefs);
      vc.submit(
         AnsiCode.CSI + fgColor + ";" +
         AnsiCode.BACKGROUND_EXT + ";" + AnsiCode.EXT_BY_INDEX + ";" + bgColor + ";" +
         AnsiCode.BOLD + ";" + AnsiCode.UNDERLINE + ";" + AnsiCode.BOLD_BLURRED_OFF + ";" +
         AnsiCode.INVERSE_OFF + AnsiCode.SGR + "Hello World", "myOriginalStyle");
      String expected ="<span class=\"myOriginalStyle\">Hello World</span>";
      Assert.assertEquals(expected, ele.getInnerHTML());
   }

   public void testAnsiComplexSequence1WithPassThrough()
   {
      int fgColor = AnsiCode.ForeColorNum.GREEN;
      int bgColor = 230;
      PreElement ele = Document.get().createPreElement();
      FakePrefs prefs = new FakePrefs();
      prefs.ansiMode_ = UserPrefs.ANSI_CONSOLE_MODE_OFF;
      VirtualConsole vc = new VirtualConsole(ele, prefs);
      vc.submit(
         AnsiCode.CSI + fgColor + ";" +
         AnsiCode.BACKGROUND_EXT + ";" + AnsiCode.EXT_BY_INDEX + ";" + bgColor + ";" +
         AnsiCode.BOLD + ";" + AnsiCode.UNDERLINE + ";" + AnsiCode.BOLD_BLURRED_OFF + ";" +
         AnsiCode.INVERSE_OFF + AnsiCode.SGR + "Hello World", "myOriginalStyle");
      String expected ="<span class=\"myOriginalStyle\"><ESC>[32;48;5;230;1;4;22;27mHello World</span>";
      Assert.assertEquals(expected, AnsiCode.prettyPrint(ele.getInnerHTML()));
   }

   public void testMultipleAnsiCodesSplitsAcrossSubmitsWithStripping()
   {
      int color = AnsiCode.ForeColorNum.MAGENTA;
      int bgColor = AnsiCode.BackColorNum.GREEN;
      PreElement ele = Document.get().createPreElement();
      FakePrefs prefs = new FakePrefs();
      prefs.ansiMode_ = UserPrefs.ANSI_CONSOLE_MODE_STRIP;
      VirtualConsole vc = new VirtualConsole(ele, prefs);
      vc.submit(AnsiCode.CSI + color);
      vc.submit(AnsiCode.SGR + AnsiCode.CSI);
      vc.submit(Integer.toString(bgColor));
      vc.submit(AnsiCode.SGR + "Hello");
      String expected ="<span>Hello</span>";
      Assert.assertEquals(expected, ele.getInnerHTML());
   }

   public void testAnsiCodeSplitAcrossSubmitsWithParentStyleWithPassthrough()
   {
      int color = AnsiCode.ForeColorNum.MAGENTA;
      PreElement ele = Document.get().createPreElement();
      FakePrefs prefs = new FakePrefs();
      prefs.ansiMode_ = UserPrefs.ANSI_CONSOLE_MODE_OFF;
      VirtualConsole vc = new VirtualConsole(ele, prefs);
      vc.submit(AnsiCode.CSI + color, "myStyle");
      vc.submit(AnsiCode.SGR + "Hello", "myStyle");
      String expected ="<span class=\"myStyle\"><ESC>[35mHello</span>";
      Assert.assertEquals(expected, AnsiCode.prettyPrint(ele.getInnerHTML()));
   }

   public void testMultipleCarriageReturn()
   {
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = getVC(ele);
      vc.submit("hello world\r           \rgoodbye");
      Assert.assertEquals("<span>goodbye    </span>", ele.getInnerHTML());
      Assert.assertEquals("goodbye    ", vc.toString());
   }

   public void testNonDestructiveBackspace()
   {
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = getVC(ele);
      vc.submit("hello world\b\b\b\b\b");
      Assert.assertEquals("<span>hello world</span>", ele.getInnerHTML());
      Assert.assertEquals("hello world", vc.toString());
   }

   public void testSingleSupportedANSICodes()
   {
      // https://github.com/rstudio/rstudio/issues/2248
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = getVC(ele);
      vc.submit(AnsiCode.CSI + "?25lBuilding sites \342\200\246 " +
                AnsiCode.CSI + "?25h\r" + AnsiCode.CSI + "[K");
      Assert.assertEquals("<span>Building sites \342\200\246 </span>", ele.getInnerHTML());
      Assert.assertEquals("Building sites \342\200\246 ", vc.toString());
   }

   public void testMultipleUnknownANSICodes()
   {
      // https://github.com/rstudio/rstudio/issues/2248
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = getVC(ele);
      vc.submit("We are " + AnsiCode.CSI + "?25lbuilding sites \342\200\246" +
                AnsiCode.CSI + "?25h\r" + AnsiCode.CSI + "[K");
      Assert.assertEquals("<span>We are building sites \342\200\246</span>", ele.getInnerHTML());
      Assert.assertEquals("We are building sites \342\200\246", vc.toString());
   }

   public void testMultiCarriageReturnsWithoutColorsMultiSubmits()
   {
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = getVC(ele);
      vc.submit("123");
      vc.submit("x \r");
      vc.submit("456");
      vc.submit("x \r");
      vc.submit("789");
      Assert.assertEquals("<span>789x </span>", ele.getInnerHTML());
      Assert.assertEquals("789x ", vc.toString());
   }

   public void testMultiCarriageReturnWithoutColors()
   {
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = getVC(ele);
      vc.submit("123" + "x \r" + "456" + "x \r" + "789");
      Assert.assertEquals("<span>789x </span>", ele.getInnerHTML());
      Assert.assertEquals("789x ", vc.toString());
   }

   public void testMultiCarriageReturnWithColors()
   {
      // https://github.com/rstudio/rstudio/issues/2387
      // cat(c(crayon::red("123"), "x \r", crayon::red("456"), "x \r", crayon::red("789\n")), sep = "")
      String red = AnsiCode.CSI + AnsiCode.ForeColorNum.RED + AnsiCode.SGR;
      String reset = AnsiCode.CSI + AnsiCode.RESET_FOREGROUND + AnsiCode.SGR;

      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = getVC(ele);
      vc.submit(red + "123" + reset + "x \r" +
                red + "456" + reset + "x \r" +
                red + "789");

      String expected =
            "<span class=\"" + AnsiCode.clazzForColor(AnsiCode.ForeColorNum.RED) +
            "\">789</span><span>x </span>";

      Assert.assertEquals(expected, ele.getInnerHTML());
      Assert.assertEquals("789x ", vc.toString());
   }


   public void testCRNoColorsIssue2665Part1()
   {
      // https://github.com/rstudio/rstudio/issues/2665
      // this checks the output without colors involved
      // cat("✔ xxx", "yyy", "xxx")
      // cat("\r")
      // cat("✔xxx", "yyy", "zzz")
      // cat("\n")

      // test writing over a previously written line (via \r) with
      // no colors involved, the second line shorter than the first,
      // ending with a \n
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = getVC(ele);
      vc.submit("✔ xxx yyy xxx");
      vc.submit("\r");
      vc.submit("✔xxx yyy zzz");
      vc.submit("\n");

      String expected = "<span>✔xxx yyy zzzx\n</span>";
      Assert.assertEquals(expected, ele.getInnerHTML());
      Assert.assertEquals("✔xxx yyy zzzx\n", vc.toString());
   }

   public void testCRNoColorsIssue2665Part2()
   {
      // https://github.com/rstudio/rstudio/issues/2665
      // this checks the output without colors involved
      // cat("✔ xxx", "yyy", "xxx")
      // cat("\r")
      // cat("✔ xxx", "yyy", "zzz")
      // cat("\n")

      // test writing over a previously written line (via \r) with
      // no colors involved, and the second line the same length as
      // the first, ending with a \n
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = getVC(ele);
      vc.submit("✔ xxx yyy xxx");
      vc.submit("\r");
      vc.submit("✔ xxx yyy zzz");
      vc.submit("\n");

      String expected = "<span>✔ xxx yyy zzz\n</span>";
      Assert.assertEquals(expected, ele.getInnerHTML());
      Assert.assertEquals("✔ xxx yyy zzz\n", vc.toString());
   }

   public void testCRColorsIssue2665Part1()
   {
      // https://github.com/rstudio/rstudio/issues/2665
      // cat("✔ xxx", crayon::blue("yyy"), "xxx")
      // cat("\r")
      // cat("✔xxx", crayon::red("yyy"), "zzz")
      // cat("\n")

      // test writing over a previously written line (via \r) with
      // colors involved, and the second line shorter than the first line,
      // ending with \n
      String red = AnsiCode.CSI + AnsiCode.ForeColorNum.RED + AnsiCode.SGR;
      String redCode = "\"" + AnsiCode.clazzForColor(AnsiCode.ForeColorNum.RED) + "\"";
      String blue = AnsiCode.CSI + AnsiCode.ForeColorNum.BLUE + AnsiCode.SGR;
      String blueCode = "\"" + AnsiCode.clazzForColor(AnsiCode.ForeColorNum.BLUE) + "\"";
      String reset = AnsiCode.CSI + AnsiCode.RESET_FOREGROUND + AnsiCode.SGR;

      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = getVC(ele);
      vc.submit("✔ xxx " + blue + "yyy" + reset + " xxx");
      vc.submit("\r");
      vc.submit("✔xxx " + red + "yyy" + reset + " zzz");
      vc.submit("\n");

      String expected =
            "<span>✔xxx </span>" +
            "<span class=" + redCode + ">yyy</span>" +
            "<span class=" + blueCode + "></span>" +
            "<span></span><span> zzzx\n</span>";
      Assert.assertEquals(expected, ele.getInnerHTML());
      Assert.assertEquals("✔xxx yyy zzzx\n", vc.toString());
   }

   public void testProgressBar4777Part1()
   {
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = getVC(ele);

      vc.submit("=>---------- 1.11 kB/s");
      vc.submit("\r");
      vc.submit("==>--------- 1.12 kB/s");
      vc.submit("\r");
      vc.submit("==>--------- 1.13 kB/s");
      vc.submit("\r");
      vc.submit("===>-------- 1.14 kB/s");
      vc.submit("\r");
      vc.submit("====>------- 1.15 kB/s");
      vc.submit("\r");
      vc.submit("====>-------- 1.2 kB/s");
      vc.submit("\r");
      vc.submit("======>----- 1.21 kB/s");
      vc.submit("\r");
      vc.submit("=======>---- 1.22 kB/s");
      vc.submit("\r");
      vc.submit("========>--- 1.23 kB/s");
      vc.submit("\r");
      vc.submit("=========>--- 1.3 kB/s");
      vc.submit("\r");
      vc.submit("==========>- 1.32 kB/s");
      vc.submit("\r");
      String lastLine = "===========> 1.33 kB/s";
      vc.submit(lastLine);

      String expected = "<span>===========&gt; 1.33 kB/s</span>";
      Assert.assertEquals(expected, ele.getInnerHTML());
      Assert.assertEquals(lastLine, vc.toString());
   }

   public void testProgressBar4777Part2()
   {
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = getVC(ele);

      String color = "\033[32m";
      String restore = "\033[39m";

      vc.submit("=>---------- " + color + "1.11 kB/s " + restore);
      vc.submit("\r");
      vc.submit("==>--------- " + color + "1.12 kB/s " + restore);
      vc.submit("\r");
      vc.submit("==>--------- " + color + "1.13 kB/s " + restore);
      vc.submit("\r");
      vc.submit("===>-------- " + color + "1.14 kB/s " + restore);
      vc.submit("\r");
      vc.submit("====>------- " + color + "1.15 kB/s " + restore);
      vc.submit("\r");
      vc.submit("====>------- " + color + "1.20 kB/s " + restore);
      vc.submit("\r");
      vc.submit("======>----- " + color + "1.21 kB/s " + restore);
      vc.submit("\r");
      vc.submit("=======>---- " + color + "1.22 kB/s " + restore);
      vc.submit("\r");
      vc.submit("========>--- " + color + "1.23 kB/s " + restore);
      vc.submit("\r");
      vc.submit("=========>-- " + color + "1.30 kB/s " + restore);
      vc.submit("\r");
      vc.submit("==========>- " + color + "1.32 kB/s " + restore);
      vc.submit("\r");
      vc.submit("===========> " + color + "1.33 kB/s " + restore);

      Assert.assertTrue(true);
      String expected = "<span>===========&gt; </span><span class=\"xtermColor2\">1.33 kB/s </span>";
      Assert.assertEquals(expected, ele.getInnerHTML());
      Assert.assertEquals("===========> 1.33 kB/s " , vc.toString());
   }

   public void testProgressBar4777Part3()
   {
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = getVC(ele);

      String color = "\033[32m";
      String restore = "\033[39m";

      vc.submit("aa " + color + "11 " + restore);
      vc.submit("\r");
      vc.submit("bbb " + color + "2 " + restore);
      vc.submit("\r");
      vc.submit("cc " + color + "33 " + restore);

      String expected = "<span>cc </span><span class=\"xtermColor2\"></span><span class=\"xtermColor2\">33 </span>";
      Assert.assertEquals(expected, ele.getInnerHTML());
      Assert.assertEquals("cc 33 ", vc.toString());
   }

   public void testScreenReaderOffNoTextNotCaptured()
   {
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = getVC(ele);
      vc.submit("Hello World", "someclass", false, true);
      Assert.assertEquals(vc.getNewText(), "");
   }

   public void testScreenReaderOnTextCaptured()
   {
      PreElement ele = Document.get().createPreElement();
      FakePrefs prefs = new FakePrefs();
      prefs.screenReaderEnabled_ = true;
      VirtualConsole vc = new VirtualConsole(ele, prefs);
      String text = "Hello World\nHow are you?";
      vc.submit(text, "someclass", false, true);
      String newText = vc.getNewText();
      Assert.assertEquals(newText, text);
   }

   public void testBackground255Carryover6092()
   {
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = getVC(ele);

      String testInput =
            setForegroundIndex(232) + setBackgroundIndex(255) + "one" +
            setCsiCode(AnsiCode.RESET_BACKGROUND) + setCsiCode(AnsiCode.RESET_FOREGROUND) + " two";

      vc.submit(testInput);
      String expected = "<span class=\"xtermColor232 xtermBgColor255\">one</span><span> two</span>";
      Assert.assertEquals(expected, ele.getInnerHTML());
      Assert.assertEquals("one two", vc.toString());
   }

   public void testForeground255Carryover6092()
   {
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = getVC(ele);

      String testInput =
            setForegroundIndex(255) + setBackgroundIndex(232) + "one" +
                  setCsiCode(AnsiCode.RESET_BACKGROUND) + setCsiCode(AnsiCode.RESET_FOREGROUND) + " two";

      vc.submit(testInput);
      String expected = "<span class=\"xtermColor255 xtermBgColor232\">one</span><span> two</span>";
      Assert.assertEquals(expected, ele.getInnerHTML());
      Assert.assertEquals("one two", vc.toString());
   }

   public void testInverseFgHandling()
   {
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = getVC(ele);

      String testInput = inverseOn() + setCsiCode(AnsiCode.ForeColorNum.RED) +
         "This is visible in both " + setCsiCode(AnsiCode.RESET_FOREGROUND) +
         "This is visible only in the terminal" + inverseOff();

      vc.submit(testInput);
      String expected = "<span class=\"xtermInvertColor xtermBgColor1\">This is visible in both </span>" +
                        "<span class=\"xtermInvertColor xtermInvertBgColor\">This is visible only in the terminal</span>";
      Assert.assertEquals(expected, ele.getInnerHTML());
      Assert.assertEquals("This is visible in both This is visible only in the terminal", vc.toString());
   }

   public void testInverseBgHandling()
   {
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = getVC(ele);

      String testInput = inverseOn() + setCsiCode(103 /*bright yellow background*/) +
         "Part One " + setCsiCode(AnsiCode.RESET_BACKGROUND) +
         "Other Part" + inverseOff();

      vc.submit(testInput);
      String expected = "<span class=\"xtermInvertBgColor xtermColor11\">Part One </span>" +
         "<span class=\"xtermInvertBgColor xtermInvertColor\">Other Part</span>";
      Assert.assertEquals(expected, ele.getInnerHTML());
      Assert.assertEquals("Part One Other Part", vc.toString());
   }

   public void testInverseFgBgHandling()
   {
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = getVC(ele);

      String testInput = inverseOn() + setCsiCode(103 /*bright yellow background*/) + setCsiCode(AnsiCode.ForeColorNum.RED) +
         "Part One " + setCsiCode(AnsiCode.RESET_BACKGROUND) + setCsiCode(AnsiCode.RESET_FOREGROUND) +
         "Other Part" + inverseOff();

      vc.submit(testInput);
      String expected = "<span class=\"xtermColor11 xtermBgColor1\">Part One </span>" +
         "<span class=\"xtermInvertColor xtermInvertBgColor\">Other Part</span>";
      Assert.assertEquals(expected, ele.getInnerHTML());
      Assert.assertEquals("Part One Other Part", vc.toString());
   }

   public void testInverse256FgHandling()
   {
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = getVC(ele);

      String testInput = inverseOn() + setForegroundIndex(196 /*red*/) +
         "Inverted with red background " + setCsiCode(AnsiCode.RESET_FOREGROUND) +
         "Inverted with default background" + inverseOff();

      vc.submit(testInput);
      String expected = "<span class=\"xtermInvertColor xtermBgColor196\">Inverted with red background </span>" +
         "<span class=\"xtermInvertColor xtermInvertBgColor\">Inverted with default background</span>";
      Assert.assertEquals(expected, ele.getInnerHTML());
      Assert.assertEquals("Inverted with red background Inverted with default background", vc.toString());
   }

   public void testInverse256FgBgHandling()
   {
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = getVC(ele);

      String testInput = inverseOn() + setForegroundIndex(196 /*red*/) + setBackgroundIndex(228 /*yellow*/) +
         "Inverted with red background yellow foreground " + setCsiCode(AnsiCode.RESET_FOREGROUND) +
         setCsiCode(AnsiCode.RESET_BACKGROUND) + "Inverted with default colors" + inverseOff();

      vc.submit(testInput);
      String expected = "<span class=\"xtermBgColor196 xtermColor228\">Inverted with red background yellow foreground </span>" +
         "<span class=\"xtermInvertBgColor xtermInvertColor\">Inverted with default colors</span>";
      Assert.assertEquals(expected, ele.getInnerHTML());
      Assert.assertEquals("Inverted with red background yellow foreground Inverted with default colors", vc.toString());
   }

   public void testCarriageReturnPartialEndOverwrite()
   {
      // Case where a span is partially overwritten after a carriage-return
      // and thus must be moved to a new position; it was being moved to an incorrect location
      // causing output to be lost.
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = getVC(ele);

      String testInput = "⠹ [ \033[32mPASS\033[39m x632 \033[31mFAIL\033[39m x16 \033[35mWARN\033[39m x4" +
         "\r" +
         "\033[32m✓\033[39m |   4       | reporter-zzz\033[36m [\033[0m";

      vc.submit(testInput);
      String expected = "<span class=\"xtermColor2\">✓</span><span> |   4       | reporter-zzz</span>" +
         "<span></span><span class=\"xtermColor6\"> [</span>";
      Assert.assertEquals(expected, ele.getInnerHTML());
      Assert.assertEquals("✓ |   4       | reporter-zzz [", vc.toString());
   }
}
