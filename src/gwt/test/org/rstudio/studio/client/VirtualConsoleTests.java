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

import org.rstudio.core.client.VirtualConsole;
import org.rstudio.studio.client.workbench.views.terminal.AnsiCode;

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
}
