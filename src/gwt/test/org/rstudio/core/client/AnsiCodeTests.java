/*
 * AnsiCodeTests.java
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

import com.google.gwt.junit.client.GWTTestCase;
import junit.framework.Assert;

public class AnsiCodeTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   public void testConsoleControlCharacterParse()
   {
      boolean foundLinefeed = false;
      boolean foundFormfeed = false;
      boolean foundBackspace = false;
      boolean foundCarriageReturn = false;
      boolean foundFirstESCMarker = false;
      boolean foundSecondESCMarker = false;

      Pattern ConsoleCtrlPattern = Pattern.create(AnsiCode.CONTROL_REGEX);
      
      String data = "A\nBC\fD\7EF\bG\rH\u001bZoom\u009bNoMore]";
      Match match = ConsoleCtrlPattern.match(data, 0);
      Assert.assertNotNull(match);
      while (match != null)
      {
         if (match.getValue().compareTo("\n") == 0)
         {
            Assert.assertFalse(foundLinefeed);
            Assert.assertEquals(1, match.getIndex());
            foundLinefeed = true;
         }
         else if (match.getValue().compareTo("\f") == 0)
         {
            Assert.assertFalse(foundFormfeed);
            Assert.assertEquals(4, match.getIndex());
            foundFormfeed = true;
         }
         else if (match.getValue().compareTo("\b") == 0)
         {
            Assert.assertFalse(foundBackspace);
            Assert.assertEquals(9, match.getIndex());
            foundBackspace = true;
         }
         else if (match.getValue().compareTo("\r") == 0)
         {
            Assert.assertFalse(foundCarriageReturn);
            Assert.assertEquals(11, match.getIndex());
            foundCarriageReturn = true;
         }
         else if (match.getValue().compareTo("\u001b") == 0)
         {
            Assert.assertFalse(foundFirstESCMarker);
            Assert.assertEquals(13, match.getIndex());
            foundFirstESCMarker = true;
         }
         else if (match.getValue().compareTo("\u009b") == 0)
         {
            Assert.assertFalse(foundSecondESCMarker);
            Assert.assertEquals(18, match.getIndex());
            foundSecondESCMarker = true;
         }
         match = match.nextMatch();
      }
      Assert.assertTrue(foundLinefeed);
      Assert.assertTrue(foundFormfeed);
      Assert.assertTrue(foundBackspace);
      Assert.assertTrue(foundCarriageReturn);
   }
   
   public void testBoldItalicOnOff()
   {
      AnsiCode ansi = new AnsiCode();

      String boldOn = "\033[1m";
      AnsiCode.AnsiClazzes newClazz = ansi.processCode(boldOn);
      Assert.assertEquals("xtermBold", newClazz.inlineClazzes);
      Assert.assertNull(newClazz.blockClazzes);
      
      String italicOn = "\033[3m";
      newClazz = ansi.processCode(italicOn);
      Assert.assertEquals("xtermBold xtermItalic", newClazz.inlineClazzes);
      Assert.assertNull(newClazz.blockClazzes);
      
      String boldOff = "\033[22m";
      newClazz = ansi.processCode(boldOff);
      Assert.assertEquals("xtermItalic", newClazz.inlineClazzes);
      Assert.assertNull(newClazz.blockClazzes);

      String italicOff = "\033[23m";
      newClazz = ansi.processCode(italicOff);
      Assert.assertNull(newClazz.inlineClazzes);
      Assert.assertNull(newClazz.blockClazzes);
   }

   public void testSimpleColors()
   {
      AnsiCode ansi = new AnsiCode();

      String redFg = "\033[31m";
      AnsiCode.AnsiClazzes newClazz = ansi.processCode(redFg);
      Assert.assertEquals("xtermColor1", newClazz.inlineClazzes);
      Assert.assertNull(newClazz.blockClazzes);
      
      String cyanFg = "\033[36m";
      newClazz = ansi.processCode(cyanFg);
      Assert.assertEquals("xtermColor6", newClazz.inlineClazzes);
      Assert.assertNull(newClazz.blockClazzes);

      String blueBg = "\033[44m";
      newClazz = ansi.processCode(blueBg);
      Assert.assertEquals("xtermColor6 xtermBgColor4", newClazz.inlineClazzes);
      Assert.assertNull(newClazz.blockClazzes);

      String resetFg = "\033[39m";
      newClazz = ansi.processCode(resetFg);
      Assert.assertEquals("xtermBgColor4", newClazz.inlineClazzes);
      Assert.assertNull(newClazz.blockClazzes);

      String resetBg = "\033[49m";
      newClazz = ansi.processCode(resetBg);
      Assert.assertNull(newClazz.inlineClazzes);
      Assert.assertNull(newClazz.blockClazzes);
   }

   public void testFontNineOnOff()
   {
      AnsiCode ansi = new AnsiCode();

      String fontNine = "\033[19m";
      AnsiCode.AnsiClazzes newClazz = ansi.processCode(fontNine);
      Assert.assertEquals("xtermFont9", newClazz.blockClazzes);
      Assert.assertNull(newClazz.inlineClazzes);

      String defaultFont = "\033[10m";
      newClazz = ansi.processCode(defaultFont);
      Assert.assertNull(newClazz.blockClazzes);
      Assert.assertNull(newClazz.inlineClazzes);
   } 

   public void testFontNineWithColors()
   {
      AnsiCode ansi = new AnsiCode();

      String redFg = "\033[31m";
      AnsiCode.AnsiClazzes newClazz = ansi.processCode(redFg);
 
      String fontNine = "\033[19m";
      newClazz = ansi.processCode(fontNine);
      Assert.assertEquals("xtermFont9", newClazz.blockClazzes);
      Assert.assertEquals("xtermColor1", newClazz.inlineClazzes);

      String defaultFont = "\033[10m";
      newClazz = ansi.processCode(defaultFont);
      Assert.assertNull(newClazz.blockClazzes);
      Assert.assertEquals("xtermColor1", newClazz.inlineClazzes);
      
      String resetAll = "\033[0m";
      newClazz = ansi.processCode(resetAll);
      Assert.assertNull(newClazz.blockClazzes);
      Assert.assertNull(newClazz.inlineClazzes);
    } 

   public void testEmptyParameterResets()
   {
      AnsiCode ansi = new AnsiCode();

      ansi.processCode("\033[1;31m");
      Assert.assertNull(ansi.processCode("\033[m").inlineClazzes);

      ansi.processCode("\033[31m");
      Assert.assertEquals("xtermBold", ansi.processCode("\033[;1m").inlineClazzes);

      ansi.processCode("\033[31m");
      Assert.assertNull(ansi.processCode("\033[1;m").inlineClazzes);
   }

   public void testUnknownExtendedColorFormat()
   {
      AnsiCode ansi = new AnsiCode();
      ansi.processCode("\033[31m");
      AnsiCode.AnsiClazzes clazzes = ansi.processCode("\033[38;3m");
      Assert.assertNotNull(clazzes);
      Assert.assertNull(clazzes.inlineClazzes);

      // the reset also ends inverse mode, so the next color is a foreground
      ansi.processCode("\033[7m");
      ansi.processCode("\033[38;3m");
      Assert.assertEquals("xtermColor1", ansi.processCode("\033[31m").inlineClazzes);
   }

   public void testSubParametersSkipped()
   {
      AnsiCode ansi = new AnsiCode();
      String expected = ansi.processCode("\033[1;31m").inlineClazzes;

      ansi = new AnsiCode();
      Assert.assertEquals(expected, ansi.processCode("\033[1;31;4:3m").inlineClazzes);

      ansi = new AnsiCode();
      Assert.assertNull(ansi.processCode("\033[38:2::255:0:0m").inlineClazzes);
   }

   public void testStripHyperlinks()
   {
      String belLink = "\033]8;;https://example.com\007link\033]8;;\007";
      Assert.assertEquals("link", AnsiCode.strip(belLink));

      String stLink = "\033]8;;https://example.com\033\\link\033]8;;\033\\";
      Assert.assertEquals("link", AnsiCode.strip(stLink));
   }

   public void testStripMatchesConsoleParsing()
   {
      // CSI sequences end at their final byte, which may be '['
      Assert.assertEquals("Ab", AnsiCode.strip("\033[[Ab"));
      Assert.assertEquals("ab", AnsiCode.strip("a\033[4:3mb"));
      Assert.assertEquals("ab", AnsiCode.strip("a\033[2 qb"));
      Assert.assertEquals("ab", AnsiCode.strip("a\u009b31mb"));

      // other escape sequences, BEL, and other string sequences
      Assert.assertEquals("ab", AnsiCode.strip("a\033(Bb"));
      Assert.assertEquals("ab", AnsiCode.strip("a\007b"));
      Assert.assertEquals("ab", AnsiCode.strip("a\033P1$rq\033\\b"));
      Assert.assertEquals("ab", AnsiCode.strip("a\033]0;title\007b"));

      // a string ended by another escape sequence, and a malformed one
      Assert.assertEquals("ab", AnsiCode.strip("a\033]0;title\033[31mb"));
      Assert.assertEquals("0;title\nnext", AnsiCode.strip("\033]0;title\nnext"));
      Assert.assertEquals("a\nb", AnsiCode.strip("a\033\nb"));
   }
}
