/*
 * VirtualConsoleTests.java
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
package org.rstudio.studio.client;

import org.rstudio.core.client.AnsiCode;
import org.rstudio.core.client.VirtualConsole;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.PreElement;
import com.google.gwt.junit.client.GWTTestCase;

import junit.framework.Assert;

public class VirtualConsoleTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   public void testSimpleText()
   {
      String simple = VirtualConsole.consolify("foo");
      Assert.assertEquals("foo", simple);
   }
   
   public void testBackspace()
   {
      String backspace = VirtualConsole.consolify("bool\bk");
      Assert.assertEquals("book", backspace);
   }
   
   public void testCarriageReturn()
   {
      String cr = VirtualConsole.consolify("hello\rj");
      Assert.assertEquals("jello", cr);
   }
   
   public void testNewlineCarrigeReturn()
   {
      String cr = VirtualConsole.consolify("L1\nL2\rL3");
      Assert.assertEquals("L1\nL3", cr);
   }

   public void testSimpleColor()
   {
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = new VirtualConsole(ele);
      vc.submit("Error", "error");
      Assert.assertEquals(
            "<span class=\"error\">Error</span>", 
            ele.getInnerHTML());
   }

   public void testTwoColors()
   {
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = new VirtualConsole(ele);
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
      VirtualConsole vc = new VirtualConsole(ele);
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
      VirtualConsole vc = new VirtualConsole(ele);
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
      VirtualConsole vc = new VirtualConsole(ele);
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
      VirtualConsole vc = new VirtualConsole(ele);
      vc.submit("Sample1\n");
      vc.submit("Sample2\n");
      vc.submit("Sample3\f");
      vc.submit("Sample4");
      Assert.assertEquals("<span>Sample4</span>", ele.getInnerHTML());
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
      VirtualConsole vc = new VirtualConsole(ele);
      vc.submit(AnsiCode.CSI + color + AnsiCode.SGR + "Hello");
      String expected ="<span class=\"xtermColor0\">Hello</span>"; 
      Assert.assertEquals(expected, ele.getInnerHTML());
   }

   public void testMaxAnsiForegroundColor()
   {
      int color = AnsiCode.FOREGROUND_MAX;
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = new VirtualConsole(ele);
      vc.submit(AnsiCode.CSI + color + AnsiCode.SGR + "Hello World");
      String expected ="<span class=\"xtermColor7\">Hello World</span>";
      Assert.assertEquals(expected, ele.getInnerHTML());
   }
  
   public void testMinAnsiBgColor()
   {
      int color = AnsiCode.BACKGROUND_MIN;
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = new VirtualConsole(ele);
      vc.submit(AnsiCode.CSI + color + AnsiCode.SGR + "pretty");
      String expected ="<span class=\"" + 
            AnsiCode.clazzForBgColor(color) + "\">pretty</span>"; 
      Assert.assertEquals(expected, ele.getInnerHTML());
   }

   public void testMaxAnsiBgColor()
   {
      int color = AnsiCode.BACKGROUND_MAX;
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = new VirtualConsole(ele);
      vc.submit(AnsiCode.CSI + color + AnsiCode.SGR + "colors");
      String expected ="<span class=\"" + 
            AnsiCode.clazzForBgColor(color) + "\">colors</span>"; 
      Assert.assertEquals(expected, ele.getInnerHTML());
   }

   public void testResetAnsiForegroundColor()
   {
      int color = AnsiCode.ForeColorNum.RED;
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = new VirtualConsole(ele);
      vc.submit(
            AnsiCode.CSI + color + AnsiCode.SGR + "Hello " +
            AnsiCode.CSI + AnsiCode.RESET_FOREGROUND + AnsiCode.SGR + "World");
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
      VirtualConsole vc = new VirtualConsole(ele);
      vc.submit(
            AnsiCode.CSI + color + AnsiCode.SGR + "Sunny " +
            AnsiCode.CSI + AnsiCode.RESET_BACKGROUND + AnsiCode.SGR + "Days");
      String expected = "<span class=\"" + AnsiCode.clazzForBgColor(color) + 
            "\">Sunny </span><span>Days</span>"; 
      Assert.assertEquals(expected, ele.getInnerHTML());
   }

   public void testAnsiForegroundAndBgColor()
   {
      int color = AnsiCode.ForeColorNum.MAGENTA;
      int bgColor = AnsiCode.BackColorNum.GREEN;
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = new VirtualConsole(ele);
      vc.submit(
            AnsiCode.CSI + color + AnsiCode.SGR + 
            AnsiCode.CSI + bgColor + AnsiCode.SGR + "Hello");
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
      VirtualConsole vc = new VirtualConsole(ele);
      vc.submit(
            AnsiCode.CSI + color + AnsiCode.SGR + 
            AnsiCode.CSI + bgColor + AnsiCode.SGR + "Colorful " +
            AnsiCode.CSI + AnsiCode.RESET + AnsiCode.SGR + "Bland");
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
      VirtualConsole vc = new VirtualConsole(ele);
      vc.submit(AnsiCode.CSI + color + AnsiCode.SGR + "Hello");
      String expected ="<span class=\"" + 
            AnsiCode.clazzForColor(color) + 
            "\">Hello</span>"; 
      Assert.assertEquals(expected, ele.getInnerHTML());
   }

   public void testMaxAnsiIntenseForegroundColor()
   {
      int color = AnsiCode.FOREGROUND_INTENSE_MAX;
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = new VirtualConsole(ele);
      vc.submit(AnsiCode.CSI + color + AnsiCode.SGR + "Hello World");
      String expected ="<span class=\"" + 
            AnsiCode.clazzForColor(color) + "\">Hello World</span>"; 
      Assert.assertEquals(expected, ele.getInnerHTML());
   }

   public void testMinAnsiIntenseBgColor()
   {
      int color = AnsiCode.BACKGROUND_INTENSE_MIN;
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = new VirtualConsole(ele);
      vc.submit(AnsiCode.CSI + color + AnsiCode.SGR + "pretty");
      String expected ="<span class=\"" + 
            AnsiCode.clazzForBgColor(color) + "\">pretty</span>"; 
      Assert.assertEquals(expected, ele.getInnerHTML());
   }

   public void testMaxAnsiIntenseBgColor()
   {
      int color = AnsiCode.BACKGROUND_INTENSE_MAX;
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = new VirtualConsole(ele);
      vc.submit(AnsiCode.CSI + color + AnsiCode.SGR + "colors");
      String expected ="<span class=\"" + 
            AnsiCode.clazzForBgColor(color) + "\">colors</span>"; 
      Assert.assertEquals(expected, ele.getInnerHTML());
   }
   
   public void testResetAnsiIntenseForegroundColor()
   {
      int color = AnsiCode.FOREGROUND_INTENSE_MIN + 1;
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = new VirtualConsole(ele);
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
      VirtualConsole vc = new VirtualConsole(ele);
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
      VirtualConsole vc = new VirtualConsole(ele);
      vc.submit(AnsiCode.CSI + AnsiCode.INVERSE + AnsiCode.SGR + "Sunny Days");
      String expected = "<span class=\"xtermInvertColor xtermInvertBgColor\"" +
            ">Sunny Days</span>";
      Assert.assertEquals(expected, ele.getInnerHTML());
   }

   public void testAnsiInvertRevertDefaultColors()
   {
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = new VirtualConsole(ele);
      vc.submit(
            AnsiCode.CSI + AnsiCode.INVERSE + AnsiCode.SGR + "Sunny " +
            AnsiCode.CSI + AnsiCode.INVERSE_OFF + AnsiCode.SGR + "Days");
      String expected = "<span class=\"xtermInvertColor xtermInvertBgColor\"" +
            ">Sunny </span><span>Days</span>"; 
      Assert.assertEquals(expected, ele.getInnerHTML());
   }
   
   public void testAnsiInvertCustomBgColor()
   {
      int bgColor = AnsiCode.BackColorNum.GREEN;
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = new VirtualConsole(ele);
      vc.submit(
            AnsiCode.CSI + bgColor + AnsiCode.SGR +
            AnsiCode.CSI + AnsiCode.INVERSE + AnsiCode.SGR + "Sunny Days");
      String expected = "<span class=\"xtermColor2 xtermInvertBgColor\"" +
            ">Sunny Days</span>";
      Assert.assertEquals(expected, ele.getInnerHTML());
   }
   
   public void testAnsiInvertCustomFgColor()
   {
      int fgColor = AnsiCode.ForeColorNum.BLUE;
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = new VirtualConsole(ele);
      vc.submit(
            AnsiCode.CSI + fgColor + AnsiCode.SGR +
            AnsiCode.CSI + AnsiCode.INVERSE + AnsiCode.SGR + "Sunny Days");
      String expected = "<span class=\"xtermInvertColor xtermBgColor4\"" +
            ">Sunny Days</span>";
      Assert.assertEquals(expected, ele.getInnerHTML());
   }

   public void testAnsiForeground256Color()
   {
      int color = 120;
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = new VirtualConsole(ele);
      vc.submit(AnsiCode.CSI + AnsiCode.FOREGROUND_EXT + ";" + 
            AnsiCode.EXT_BY_INDEX + ";" + color + AnsiCode.SGR + "Hello World");
      String expected ="<span class=\"xtermColor120\">Hello World</span>";
      Assert.assertEquals(expected, ele.getInnerHTML());
   }

   public void testAnsiBackground256Color()
   {
      int color = 213;
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = new VirtualConsole(ele);
      vc.submit(AnsiCode.CSI + AnsiCode.BACKGROUND_EXT + ";" + 
            AnsiCode.EXT_BY_INDEX + ";" + color + AnsiCode.SGR + "Hello World");
      String expected ="<span class=\"xtermBgColor213\">Hello World</span>";
      Assert.assertEquals(expected, ele.getInnerHTML());
   }

   public void testAnsi256Color()
   {
      int color = 65;
      int bgColor = 252;
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = new VirtualConsole(ele);
      vc.submit(AnsiCode.CSI + AnsiCode.FOREGROUND_EXT + ";" + 
            AnsiCode.EXT_BY_INDEX + ";" + color + AnsiCode.SGR +
            AnsiCode.CSI + AnsiCode.BACKGROUND_EXT + ";" + AnsiCode.EXT_BY_INDEX + ";" +
            bgColor + AnsiCode.SGR + "Hello World");
      String expected ="<span class=\"xtermColor65 xtermBgColor252\">Hello World</span>";
      Assert.assertEquals(expected, ele.getInnerHTML());
   }

   public void testAnsiInvert256Color()
   {
      int color = 61;
      int bgColor = 129;
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = new VirtualConsole(ele);
      vc.submit(AnsiCode.CSI + AnsiCode.FOREGROUND_EXT + ";" + 
            AnsiCode.EXT_BY_INDEX + ";" + color + AnsiCode.SGR +
            AnsiCode.CSI + AnsiCode.BACKGROUND_EXT + ";" + AnsiCode.EXT_BY_INDEX + ";" +
            bgColor + AnsiCode.SGR + 
            AnsiCode.CSI + AnsiCode.INVERSE + AnsiCode.SGR + "Hello World");
      String expected ="<span class=\"xtermColor129 xtermBgColor61\">Hello World</span>";
      Assert.assertEquals(expected, ele.getInnerHTML());
   }

   public void testAnsiInvert256FgColor()
   {
      int color = 77;
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = new VirtualConsole(ele);
      vc.submit(AnsiCode.CSI + AnsiCode.FOREGROUND_EXT + ";" + 
            AnsiCode.EXT_BY_INDEX + ";" + color + AnsiCode.SGR +
            AnsiCode.CSI + AnsiCode.INVERSE + AnsiCode.SGR + "Hello World");
      String expected ="<span class=\"xtermInvertColor xtermBgColor77\">Hello World</span>";
      Assert.assertEquals(expected, ele.getInnerHTML());
   }

   public void testAnsiInvert256BgColor()
   {
      int bgColor = 234;
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = new VirtualConsole(ele);
      vc.submit(AnsiCode.CSI + AnsiCode.BACKGROUND_EXT + ";" + 
            AnsiCode.EXT_BY_INDEX + ";" + bgColor + AnsiCode.SGR +
            AnsiCode.CSI + AnsiCode.INVERSE + AnsiCode.SGR + "Hello World");
      String expected ="<span class=\"xtermColor234 xtermInvertBgColor\">Hello World</span>";
      Assert.assertEquals(expected, ele.getInnerHTML());
   }

   public void testAnsiComplexSequence1()
   {
      int fgColor = AnsiCode.ForeColorNum.GREEN;
      int bgColor = 230;
      PreElement ele = Document.get().createPreElement();
      VirtualConsole vc = new VirtualConsole(ele);
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
      VirtualConsole vc = new VirtualConsole(ele);
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
      VirtualConsole vc = new VirtualConsole(ele);
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
      VirtualConsole vc = new VirtualConsole(ele);
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
      VirtualConsole vc = new VirtualConsole(ele);
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
      VirtualConsole vc = new VirtualConsole(ele);
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
      VirtualConsole vc = new VirtualConsole(ele);
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
      VirtualConsole vc = new VirtualConsole(ele);
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
      VirtualConsole vc = new VirtualConsole(ele);
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
      VirtualConsole vc = new VirtualConsole(ele);
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
      VirtualConsole vc = new VirtualConsole(ele, VirtualConsole.ANSI_COLOR_STRIP);
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
      VirtualConsole vc = new VirtualConsole(ele, VirtualConsole.ANSI_COLOR_OFF);
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
      VirtualConsole vc = new VirtualConsole(ele, VirtualConsole.ANSI_COLOR_STRIP);
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
      VirtualConsole vc = new VirtualConsole(ele, VirtualConsole.ANSI_COLOR_OFF);
      vc.submit(AnsiCode.CSI + color, "myStyle");
      vc.submit(AnsiCode.SGR + "Hello", "myStyle");
      String expected ="<span class=\"myStyle\"><ESC>[35mHello</span>";
      Assert.assertEquals(expected, AnsiCode.prettyPrint(ele.getInnerHTML()));
   }
}
