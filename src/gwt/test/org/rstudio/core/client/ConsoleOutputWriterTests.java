/*
 * ConsoleOutputWriterTests.java
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

import java.util.List;

import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.dom.client.Text;
import com.google.gwt.junit.client.GWTTestCase;

import junit.framework.Assert;

public class ConsoleOutputWriterTests extends GWTTestCase
{
   private final String nullClazz = null;
   private final boolean notError = false;
   private final boolean isError = true;
   private final boolean checkLineCount = false;
   private final boolean ignoreLineCount = true;
   private final String myClass = "myClass";
   private final String myErrorClass = "myErrorClass";

   private final String newlineErrorSpan = "<span class=\"myErrorClass\">\n</span>";

   private String numberedLine(int i)
   {
      return i + "\n";
   }

   private String getInnerHTML(ConsoleOutputWriter output)
   {
      SpanElement outerSpan = SpanElement.as(output.getElement().getFirstChildElement());
      return outerSpan.getInnerHTML();
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

      public final int truncateLines_ = 1000;
      public final String ansiMode_ = UserPrefs.ANSI_CONSOLE_MODE_ON;
      public final boolean screenReaderEnabled_ = false;
   }

   private static class VCFactory implements VirtualConsoleFactory
   {
      @Override
      public VirtualConsole create(Element elem)
      {
         return new VirtualConsole(elem, new FakePrefs());
      }
   }

   private ConsoleOutputWriter getCOW()
   {
      return new ConsoleOutputWriter(new VCFactory(), null);
   }

   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   public void testCreation()
   {
      ConsoleOutputWriter output = getCOW();
      Assert.assertNotNull(output);
      Assert.assertNotNull(output.getWidget());
   }

   public void testSetGetMaxLines()
   {
      ConsoleOutputWriter output = getCOW();
      Assert.assertEquals(-1,  output.getMaxOutputLines());
      output.setMaxOutputLines(1000);
      Assert.assertEquals(1000, output.getMaxOutputLines());
   }

   public void testSimpleLineCount()
   {
      ConsoleOutputWriter output = getCOW();
      Assert.assertEquals(0, output.getCurrentLines());
      Assert.assertTrue(output.outputToConsole("Hello World",
            nullClazz, notError, checkLineCount, false));
      Assert.assertEquals(0,  output.getCurrentLines());
      Assert.assertTrue(output.outputToConsole(" more on same line",
            nullClazz, notError, checkLineCount, false));
      Assert.assertEquals(0,  output.getCurrentLines());
      Assert.assertTrue(output.outputToConsole("next line starts now\n",
            nullClazz, notError, checkLineCount, false));
      Assert.assertEquals(1,  output.getCurrentLines());
      Assert.assertEquals(1, DomUtils.countLines(output.getElement(), true));
   }

   public void testTrimming()
   {
      // this test is rather bulky; it tests that lines get trimmed, but also
      // some basic cross-checking of the output structure, behavior of
      // trimOutput, and lack of a class attribute when no clazz was specified
      ConsoleOutputWriter output = getCOW();
      final int maxLines = 25;
      output.setMaxOutputLines(maxLines);

      // write just below the maximum
      for (int i = 0; i < maxLines; i++)
      {
         Assert.assertTrue(output.outputToConsole(numberedLine(i),
               nullClazz, notError, checkLineCount, false));
         Assert.assertEquals(i + 1, output.getCurrentLines());
      }

      // trim should be a no-op when below the limit
      Assert.assertEquals(maxLines, output.getCurrentLines());
      Assert.assertFalse(output.trimExcess());
      Assert.assertEquals(maxLines, output.getCurrentLines());

      // go over the limit
      Assert.assertFalse(output.outputToConsole(numberedLine(maxLines),
            nullClazz, notError, checkLineCount, false));
      Assert.assertEquals(maxLines, output.getCurrentLines());

      // go over the limit again
      Assert.assertFalse(output.outputToConsole(numberedLine(maxLines + 1),
            nullClazz, notError, checkLineCount, false));
      Assert.assertEquals(maxLines, output.getCurrentLines());

      // verify DOM matches expectations; first two output lines (0 and 1)
      // should have been removed; since clazz has been the same, all lines
      // should be in a single child <span> under the initial <span>
      Element parent = output.getElement();
      Assert.assertEquals(1, parent.getChildCount());
      SpanElement outerSpan = SpanElement.as(parent.getFirstChildElement());
      Assert.assertEquals(1, outerSpan.getChildCount());
      SpanElement span = SpanElement.as(outerSpan.getFirstChildElement());
      Assert.assertEquals(1, span.getChildCount());
      Text text = Text.as(span.getChild(0));

      StringBuilder expected = new StringBuilder();
      for (int i = 2; i <= maxLines + 1; i++)
      {
         expected.append(numberedLine(i));
      }
      Assert.assertEquals(expected.toString(), text.getData());
      Assert.assertEquals(span.getClassName(), "");

      String expectedInnerHtml = "<span>" + expected.toString() + "</span>";
      Assert.assertEquals(expectedInnerHtml, getInnerHTML(output));
      Assert.assertEquals(maxLines, DomUtils.countLines(output.getElement(), true));
   }

   public void testBulkAddThenTrim()
   {
      // bulk adding lets you add more lines than the maximum, but defer
      // trimming until the end
      ConsoleOutputWriter output = getCOW();
      final int maxLines = 50;
      output.setMaxOutputLines(maxLines);

      for (int i = 0; i < maxLines + 10; i++)
      {
         Assert.assertTrue(output.outputToConsole(numberedLine(i),
               myClass, notError, ignoreLineCount, false));
         Assert.assertEquals(i + 1, output.getCurrentLines());
      }

      Assert.assertEquals(maxLines + 10, output.getCurrentLines());
      Assert.assertTrue(output.trimExcess());
      Assert.assertEquals(maxLines, output.getCurrentLines());
      Assert.assertEquals(maxLines, DomUtils.countLines(output.getElement(), true));

      StringBuilder expected = new StringBuilder();
      expected.append("<span class=\"" + myClass + "\">");
      for (int i = 10; i < maxLines + 10; i++)
      {
         expected.append(numberedLine(i));
      }
      expected.append("</span>");

      Assert.assertEquals(expected.toString(), getInnerHTML(output));
   }

   public void testWriteSimpleError()
   {
      ConsoleOutputWriter output = getCOW();
      String errorMsg = "Oh no, an error!!";

      Assert.assertTrue(output.outputToConsole(errorMsg,
               myErrorClass, isError, ignoreLineCount, false));

      Assert.assertEquals(0,  output.getCurrentLines());

      String expected = "<span class=\"myErrorClass\">" + errorMsg + "</span>";
      Assert.assertEquals(expected, getInnerHTML(output));
   }

   public void testWriteSimpleErrorWithNewline()
   {
      ConsoleOutputWriter output = getCOW();
      String errorMsg = "Oh no, an error!!\n";

      Assert.assertTrue(output.outputToConsole(errorMsg,
               myErrorClass, isError, ignoreLineCount, false));

      Assert.assertEquals(1, output.getCurrentLines());
      String expected = "<span class=\"myErrorClass\">" + errorMsg + "</span>";
      Assert.assertEquals(expected, getInnerHTML(output));
   }

   ////////////////////////////////////////////////////////////////////////////
   // Below here are a bunch of tests I had written in R and was checking by
   // eyeball directly in RStudio. https://github.com/gtritchie/console_tests
   // These test a variety of issues that came up in the product during
   // development, with a mixture of Ansi colors, regular output, error output,
   // and so on.
   ////////////////////////////////////////////////////////////////////////////

   public void test1()
   {
      // output multiple errors with same style, ensure each goes into its own
      // span and the final one is captured and available via getNewElements
      ConsoleOutputWriter output = getCOW();

      output.outputToConsole("1\n", myErrorClass, isError, ignoreLineCount, false);
      output.outputToConsole("2\n", myErrorClass, isError, ignoreLineCount, false);
      output.outputToConsole("3\n", myErrorClass, isError, ignoreLineCount, false);
      output.outputToConsole("4\n", myErrorClass, isError, ignoreLineCount, false);

      String lastError = "Error in h() : An error! Oh No!";
      output.outputToConsole(lastError, myErrorClass, isError, ignoreLineCount, false);

      String lastErrorSpan = "<span class=\"myErrorClass\">" + lastError + "</span>";
      String expected =
            "<span class=\"myErrorClass\">1\n</span>" +
            "<span class=\"myErrorClass\">2\n</span>" +
            "<span class=\"myErrorClass\">3\n</span>" +
            "<span class=\"myErrorClass\">4\n</span>" +
            lastErrorSpan;

      Assert.assertEquals(4, output.getCurrentLines());
      Assert.assertEquals(expected, getInnerHTML(output));

      List<Element> newElements = output.getNewElements();
      Assert.assertFalse(newElements.isEmpty());
      Assert.assertEquals(1, newElements.size());
      Assert.assertEquals(lastErrorSpan, newElements.get(0).getString());
   }

   public void test2()
   {
      // output multiple errors with varying styles via Ansi codes, ensure
      // each goes into its own span with correct styles, and the final one,
      // which also has multiple styles and spans three lines is
      // captured and available via getNewElements.
      ConsoleOutputWriter output = getCOW();

      output.outputToConsole("1\n", myErrorClass, isError, ignoreLineCount, false);
      output.outputToConsole("2\n", myErrorClass, isError, ignoreLineCount, false);
      output.outputToConsole("3\n", myErrorClass, isError, ignoreLineCount, false);
      output.outputToConsole("4\n", myErrorClass, isError, ignoreLineCount, false);

      String lastError1 = "Error in h2() : An error!\n";
      String lastError2 = "\033[31mOh No!\n\033[39m";
      String lastError3 = "\033[43m\033[31mWow!\033[39m\033[49m";

      output.outputToConsole(lastError1 + lastError2 + lastError3,
            myErrorClass, isError, ignoreLineCount, false);

      String lastError1Span = "<span class=\"myErrorClass\">" +
                  lastError1 + "</span>";
      String lastError2Span = "<span class=\"myErrorClass xtermColor1\">" +
                  "Oh No!" + "</span>";
      String lastError3Span = "<span class=\"myErrorClass xtermBgColor3 xtermColor1\">" +
                  "Wow!" + "</span>";

      String expected =
            "<span class=\"myErrorClass\">1\n</span>" +
            "<span class=\"myErrorClass\">2\n</span>" +
            "<span class=\"myErrorClass\">3\n</span>" +
            "<span class=\"myErrorClass\">4\n</span>" +
            lastError1Span + lastError2Span + newlineErrorSpan + lastError3Span;

      Assert.assertEquals(6, output.getCurrentLines());
      Assert.assertEquals(expected, getInnerHTML(output));

      List<Element> newElements = output.getNewElements();
      Assert.assertFalse(newElements.isEmpty());
      Assert.assertEquals(4, newElements.size());
      Assert.assertEquals(lastError1Span, newElements.get(0).getString());
      Assert.assertEquals(lastError2Span, newElements.get(1).getString());
      Assert.assertEquals(newlineErrorSpan, newElements.get(2).getString());
      Assert.assertEquals(lastError3Span, newElements.get(3).getString());
   }

   public void test3()
   {
      // output two regular strings, without a newline, and make sure
      // they turn into a single span of text
      ConsoleOutputWriter output = getCOW();

      output.outputToConsole("Hello", myClass, notError, ignoreLineCount, false);
      output.outputToConsole("World", myClass, notError, ignoreLineCount, false);

      Assert.assertEquals(0, output.getCurrentLines());

      Assert.assertEquals("<span class=\"myClass\">HelloWorld</span>",
            getInnerHTML(output));
   }

   public void test4()
   {
      // output 4 regular strings followed by newlines; should end up in a
      // single span
      ConsoleOutputWriter output = getCOW();

      output.outputToConsole("One\n", myClass, notError, ignoreLineCount, false);
      output.outputToConsole("Two\n", myClass, notError, ignoreLineCount, false);
      output.outputToConsole("Three\n", myClass, notError, ignoreLineCount, false);
      output.outputToConsole("Four\n", myClass, notError, ignoreLineCount, false);

      Assert.assertEquals(4, output.getCurrentLines());

      Assert.assertEquals("<span class=\"myClass\">One\nTwo\nThree\nFour\n</span>",
            getInnerHTML(output));

   }

   public void test5()
   {
      // output two error strings, then one regular string, then another error
      // and make sure DOM ends up as expected
      ConsoleOutputWriter output = getCOW();

      output.outputToConsole("1\n", myErrorClass, isError, ignoreLineCount, false);
      output.outputToConsole("2\n", myErrorClass, isError, ignoreLineCount, false);
      output.outputToConsole("Hello ", myClass, notError, ignoreLineCount, false);
      output.outputToConsole("world\n", myClass, notError, ignoreLineCount, false);
      output.outputToConsole("3\n", myErrorClass, isError, ignoreLineCount, false);

      Assert.assertEquals(4, output.getCurrentLines());

      Assert.assertEquals("<span class=\"myErrorClass\">1\n</span>" +
            "<span class=\"myErrorClass\">2\n</span>" +
            "<span class=\"myClass\">Hello world\n</span>" +
            "<span class=\"myErrorClass\">3\n</span>",
            getInnerHTML(output));

      List<Element> newElements = output.getNewElements();
      Assert.assertEquals(1, newElements.size());
      Assert.assertEquals("<span class=\"myErrorClass\">3\n</span>",
            newElements.get(0).getString());
   }

   public void test6()
   {
      // output 4 error messages without ansi codes; each should end up in its
      // own span, and the final should be captured
      ConsoleOutputWriter output = getCOW();

      output.outputToConsole("1\n", myErrorClass, isError, ignoreLineCount, false);
      output.outputToConsole("2\n", myErrorClass, isError, ignoreLineCount, false);
      output.outputToConsole("3\n", myErrorClass, isError, ignoreLineCount, false);
      output.outputToConsole("4", myErrorClass, isError, ignoreLineCount, false);

      Assert.assertEquals(3, output.getCurrentLines());

      Assert.assertEquals(
            "<span class=\"myErrorClass\">1\n</span>" +
            "<span class=\"myErrorClass\">2\n</span>" +
            "<span class=\"myErrorClass\">3\n</span>" +
            "<span class=\"myErrorClass\">4</span>",
            getInnerHTML(output));

      List<Element> newElements = output.getNewElements();
      Assert.assertEquals(1, newElements.size());
      Assert.assertEquals("<span class=\"myErrorClass\">4</span>",
            newElements.get(0).getString());
   }

   public void test7()
   {
      // output a single error message with ansi code in it
      ConsoleOutputWriter output = getCOW();

      output.outputToConsole("Error in test7a() : \033[32mHi\033[39m", myErrorClass,
            isError, ignoreLineCount, false);

      Assert.assertEquals(0, output.getCurrentLines());

      Assert.assertEquals(
            "<span class=\"myErrorClass\">Error in test7a() : </span>" +
            "<span class=\"myErrorClass xtermColor2\">Hi</span>",
            getInnerHTML(output));

      List<Element> newElements = output.getNewElements();
      Assert.assertEquals(2, newElements.size());
      Assert.assertEquals("<span class=\"myErrorClass\">Error in test7a() : </span>",
            newElements.get(0).getString());
      Assert.assertEquals("<span class=\"myErrorClass xtermColor2\">Hi</span>",
            newElements.get(1).getString());
   }

   public void test8()
   {
      // Write four error lines, then a complex multi-line error message with
      // ansi codes.
      ConsoleOutputWriter output = getCOW();

      output.outputToConsole("1\n", myErrorClass, isError, ignoreLineCount, false);
      output.outputToConsole("2\n", myErrorClass, isError, ignoreLineCount, false);
      output.outputToConsole("3\n", myErrorClass, isError, ignoreLineCount, false);
      output.outputToConsole("4\n", myErrorClass, isError, ignoreLineCount, false);
      output.outputToConsole(
            "Error in test8b() : An error!\n" +
            "\033[31mOh No!\n\033[39m" +
            "\033[43m\033[31mA multiline error with colors.\n\033[39m\033[49m" +
            "\033[7mAnd some inverse text.\n\033[27m" +
            "\033[1m\033[3mThe Horror!!\033[23m\033[22m",
            myErrorClass, isError, ignoreLineCount, false);

      Assert.assertEquals(8, output.getCurrentLines());

      Assert.assertEquals(
            "<span class=\"myErrorClass\">1\n</span>" +
            "<span class=\"myErrorClass\">2\n</span>" +
            "<span class=\"myErrorClass\">3\n</span>" +
            "<span class=\"myErrorClass\">4\n</span>" +
            "<span class=\"myErrorClass\">Error in test8b() : An error!\n</span>" +
            "<span class=\"myErrorClass xtermColor1\">Oh No!</span>" +
            newlineErrorSpan +
            "<span class=\"myErrorClass xtermBgColor3 xtermColor1\">A multiline error with colors.</span>" +
            newlineErrorSpan +
            "<span class=\"myErrorClass xtermInvertColor xtermInvertBgColor\">And some inverse text.</span>" +
            newlineErrorSpan +
            "<span class=\"myErrorClass xtermBold xtermItalic\">The Horror!!</span>",
            getInnerHTML(output));

      List<Element> newElements = output.getNewElements();
      Assert.assertEquals(8, newElements.size());

      Assert.assertEquals("<span class=\"myErrorClass\">Error in test8b() : An error!\n</span>",
            newElements.get(0).getString());
      Assert.assertEquals("<span class=\"myErrorClass xtermColor1\">Oh No!</span>",
            newElements.get(1).getString());
      Assert.assertEquals(newlineErrorSpan, newElements.get(2).getString());
      Assert.assertEquals("<span class=\"myErrorClass xtermBgColor3 xtermColor1\">A multiline error with colors.</span>" ,
            newElements.get(3).getString());
      Assert.assertEquals(newlineErrorSpan, newElements.get(4).getString());
      Assert.assertEquals("<span class=\"myErrorClass xtermInvertColor xtermInvertBgColor\">And some inverse text.</span>",
            newElements.get(5).getString());
      Assert.assertEquals(newlineErrorSpan, newElements.get(6).getString());
      Assert.assertEquals("<span class=\"myErrorClass xtermBold xtermItalic\">The Horror!!</span>",
            newElements.get(7).getString());
   }

   public void test9()
   {
      // write a single-line error followed by a multi-line error, no ansi codes
      ConsoleOutputWriter output = getCOW();

      output.outputToConsole("Hello\n", myErrorClass, isError, ignoreLineCount, false);
      output.outputToConsole("A\nB\nC", myErrorClass, isError, ignoreLineCount, false);

      Assert.assertEquals(3, output.getCurrentLines());

      Assert.assertEquals(
            "<span class=\"myErrorClass\">Hello\n</span>" +
            "<span class=\"myErrorClass\">A\nB\nC</span>",
            getInnerHTML(output));

      List<Element> newElements = output.getNewElements();
      Assert.assertEquals(1, newElements.size());
      Assert.assertEquals("<span class=\"myErrorClass\">A\nB\nC</span>",
            newElements.get(0).getString());
   }

   public void test10()
   {
      // inline editing via \r without ansi codes
      ConsoleOutputWriter output = getCOW();
      output.outputToConsole("\rfoobar", myClass, notError, ignoreLineCount, false);
      output.outputToConsole("\rX foobar\n", myClass, notError, ignoreLineCount, false);

      Assert.assertEquals(1, output.getCurrentLines());
      Assert.assertEquals(
            "<span class=\"myClass\">X foobar\n</span>",
            getInnerHTML(output));
   }

   public void test11()
   {
      // inline editing via \r with ansi codes
      ConsoleOutputWriter output = getCOW();
      output.outputToConsole("\rfoobar", myClass, notError, ignoreLineCount, false);
      output.outputToConsole("\r\033[32mX\033[39m \033[31mfoobar\033[39m\n",
            myClass, notError, ignoreLineCount, false);

      Assert.assertEquals(1, output.getCurrentLines());
      Assert.assertEquals(
            "<span class=\"myClass xtermColor2\">X</span>" +
            "<span class=\"myClass\"> </span>" +
            "<span class=\"myClass xtermColor1\">foobar</span>" +
            "<span class=\"myClass\">\n</span>",
            getInnerHTML(output));
   }

   public void test12()
   {
      // inline editing via \r with ansi codes; multiple output lines, don't
      // overwrite entire original output
      ConsoleOutputWriter output = getCOW();
      output.outputToConsole("Hello\nWorld", myClass, notError, ignoreLineCount, false);
      output.outputToConsole("\r\033[32mX\033[39m \033[31mY\033[39m\n",
            myClass, notError, ignoreLineCount, false);

      Assert.assertEquals(2, output.getCurrentLines());
      Assert.assertEquals(
            "<span class=\"myClass\">Hello\n</span>" +
            "<span class=\"myClass xtermColor2\">X</span>" +
            "<span class=\"myClass\"> </span>" +
            "<span class=\"myClass xtermColor1\">Y</span>" +
            "<span class=\"myClass\">ld\n</span>",
            getInnerHTML(output));
   }

   public void test13()
   {
      // inline editing via \r with ansi codes; multiple output lines,
      // overwrite entire original output
      ConsoleOutputWriter output = getCOW();
      output.outputToConsole("Hello\nWorld", myClass, notError, ignoreLineCount, false);
      output.outputToConsole("\r\033[32m123\033[39m\033[31m45\033[39m\n",
            myClass, notError, ignoreLineCount, false);

      Assert.assertEquals(2, output.getCurrentLines());
      Assert.assertEquals(
            "<span class=\"myClass\">Hello\n</span>" +
            "<span class=\"myClass xtermColor2\">123</span>" +
            "<span class=\"myClass\"></span>" +
            "<span class=\"myClass xtermColor1\">45</span>" +
            "<span class=\"myClass\">\n</span>",
            getInnerHTML(output));
   }

   public void test14()
   {
      // output mixture of normal and error output, make sure DOM is correct
      ConsoleOutputWriter output = getCOW();

      output.outputToConsole("Beginning\n", myClass, notError, ignoreLineCount, false);
      output.outputToConsole("1\n", myErrorClass, isError, ignoreLineCount, false);
      output.outputToConsole("2\n", myErrorClass, isError, ignoreLineCount, false);
      output.outputToConsole("Hello ", myClass, notError, ignoreLineCount, false);
      output.outputToConsole("world\n", myClass, notError, ignoreLineCount, false);
      output.outputToConsole("3\n", myErrorClass, isError, ignoreLineCount, false);
      output.outputToConsole("END", myClass, notError, ignoreLineCount, false);

      Assert.assertEquals(5, output.getCurrentLines());

      Assert.assertEquals("<span class=\"myClass\">Beginning\n</span>" +
            "<span class=\"myErrorClass\">1\n</span>" +
            "<span class=\"myErrorClass\">2\n</span>" +
            "<span class=\"myClass\">Hello world\n</span>" +
            "<span class=\"myErrorClass\">3\n</span>" +
            "<span class=\"myClass\">END</span>",
            getInnerHTML(output));

   }

   public void test15()
   {
      // write multiple error lines followed by a multi-line error, no ansi codes
      ConsoleOutputWriter output = getCOW();

      output.outputToConsole("1\n", myErrorClass, isError, ignoreLineCount, false);
      output.outputToConsole("2\n", myErrorClass, isError, ignoreLineCount, false);
      output.outputToConsole(
            "Error in h15() : A multiline error without colors!\nOh No!",
            myErrorClass, isError, ignoreLineCount, false);

      Assert.assertEquals(3, output.getCurrentLines());

      Assert.assertEquals(
            "<span class=\"myErrorClass\">1\n</span>" +
            "<span class=\"myErrorClass\">2\n</span>" +
            "<span class=\"myErrorClass\">Error in h15() : A multiline error " +
               "without colors!\nOh No!</span>",
            getInnerHTML(output));

      List<Element> newElements = output.getNewElements();
      Assert.assertEquals(1, newElements.size());
      Assert.assertEquals("<span class=\"myErrorClass\">Error in h15() : A " +
         "multiline error without colors!\nOh No!</span>",
            newElements.get(0).getString());
   }

   public void test16()
   {
      // write several lines of regular output
      ConsoleOutputWriter output = getCOW();

      output.outputToConsole("one\n", myClass, notError, ignoreLineCount, false);
      output.outputToConsole("two\n", myClass, notError, ignoreLineCount, false);
      output.outputToConsole("three", myClass, notError, ignoreLineCount, false);

      Assert.assertEquals(2, output.getCurrentLines());

      Assert.assertEquals(
            "<span class=\"myClass\">one\ntwo\nthree</span>",
            getInnerHTML(output));
   }

   public void test17()
   {
      // Test moved to desktop automation; evidently there are some differences
      // in behavior for the regular expression used by typical browsers and
      // the JavaScript emulation in our unit tests, and so the old test here
      // was incorrectly failing.
   }

   public void test18()
   {
      // https://github.com/rstudio/rstudio/issues/7278
      ConsoleOutputWriter output = getCOW();
      output.outputToConsole("B\033[31mx\033[39mA", myClass, notError, ignoreLineCount, false);
      output.outputToConsole("\r   ", myClass, notError, ignoreLineCount, false);
      output.outputToConsole("\rMessage\n", myClass, notError, ignoreLineCount, false);
      Assert.assertEquals(1, output.getCurrentLines());
      Assert.assertEquals("<span class=\"myClass\">Message\n</span>", getInnerHTML(output));
   }

   // ---- incremental line counting ----

   private void assertLinesMatchDom(ConsoleOutputWriter output)
   {
      Assert.assertEquals(DomUtils.countLines(output.getElement(), true), output.getCurrentLines());
   }

   public void testLineCountWithInterleavedErrorAndOutput()
   {
      // each switch between stdout and stderr is its own chunk; the count
      // must stay exact without re-counting the scrollback every time
      ConsoleOutputWriter output = getCOW();
      for (int i = 0; i < 50; i++)
      {
         output.outputToConsole("message " + i + "\n", myErrorClass, isError, ignoreLineCount, false);
         output.outputToConsole("output " + i + "\n", nullClazz, notError, ignoreLineCount, false);
      }

      Assert.assertEquals(100, output.getCurrentLines());
      assertLinesMatchDom(output);
   }

   public void testLineCountWithConditionGroups()
   {
      // R wraps conditions in group escapes; the group's surrounding newlines
      // are trimmed from the DOM when the next output starts
      ConsoleOutputWriter output = getCOW();
      for (int i = 0; i < 20; i++)
      {
         output.outputToConsole("\033G3;\nmessage " + i + "\n\033g", myErrorClass, isError, ignoreLineCount, false);
         output.outputToConsole("output " + i + "\n", nullClazz, notError, ignoreLineCount, false);
      }
      output.normalizePreviousOutput();

      assertLinesMatchDom(output);
   }

   public void testTrimmingStaysExactAcrossConditionGroups()
   {
      // an over-count would trim real lines each time, shrinking the visible
      // scrollback a little further on every group
      ConsoleOutputWriter output = getCOW();
      int maxLines = 10;
      output.setMaxOutputLines(maxLines);
      for (int i = 0; i < 100; i++)
      {
         output.outputToConsole("\033G3;\nmessage " + i + "\n\033g", myErrorClass, isError, checkLineCount, false);
         output.outputToConsole("output " + i + "\n", nullClazz, notError, checkLineCount, false);
      }

      Assert.assertEquals(maxLines, output.getCurrentLines());
      Assert.assertEquals(maxLines, DomUtils.countLines(output.getElement(), true));
   }

   public void testLineCountWithCarriageReturnProgress()
   {
      ConsoleOutputWriter output = getCOW();
      for (int i = 0; i < 5; i++)
         output.outputToConsole("line " + i + "\n", nullClazz, notError, ignoreLineCount, false);
      for (int i = 0; i < 50; i++)
         output.outputToConsole("\rprogress " + i, nullClazz, notError, ignoreLineCount, false);
      output.outputToConsole("\n", nullClazz, notError, ignoreLineCount, false);

      Assert.assertEquals(6, output.getCurrentLines());
      assertLinesMatchDom(output);
      Assert.assertTrue(output.getElement().getInnerText().endsWith("line 4\nprogress 49\n"));
   }

   public void testLineCountAfterTrimmingThenOverwriting()
   {
      // the scrollback limit trims the front of the trailing span behind the
      // virtual console's back; edits at the end must still land correctly
      ConsoleOutputWriter output = getCOW();
      output.setMaxOutputLines(3);
      for (int i = 0; i < 6; i++)
         output.outputToConsole("line " + i + "\n", nullClazz, notError, checkLineCount, false);
      output.outputToConsole("progress 1", nullClazz, notError, checkLineCount, false);
      output.outputToConsole("\rprogress 2\n", nullClazz, notError, checkLineCount, false);

      Assert.assertEquals("line 4\nline 5\nprogress 2\n", output.getElement().getInnerText());
      Assert.assertEquals(3, output.getCurrentLines());
      assertLinesMatchDom(output);
   }

   public void testBulkOutputIsWrittenInFewTextNodes()
   {
      // 10k lines in one chunk used to mean 20k rewrites of the whole span
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < 10000; i++)
         sb.append("line ").append(i).append("\n");

      ConsoleOutputWriter output = getCOW();
      output.outputToConsole(sb.toString(), nullClazz, notError, ignoreLineCount, false);

      // the first segment is written when the span is created; everything
      // after it arrives in a single flush
      Element span = output.getElement().getFirstChildElement().getFirstChildElement();
      Assert.assertEquals(sb.toString(), span.getInnerText());
      Assert.assertTrue(span.getChildCount() <= 2);
      Assert.assertEquals(10000, output.getCurrentLines());
   }

   public void testFormFeedResetsLineCount()
   {
      ConsoleOutputWriter output = getCOW();
      output.outputToConsole("one\ntwo\n", nullClazz, notError, ignoreLineCount, false);
      output.outputToConsole("\fthree\n", nullClazz, notError, ignoreLineCount, false);
      output.outputToConsole("four\n", nullClazz, notError, ignoreLineCount, false);

      Assert.assertEquals(2, output.getCurrentLines());
      assertLinesMatchDom(output);
   }

   public void testPromptTimeEditsAreCounted()
   {
      // at each prompt the shell trims the last group's newlines and makes
      // sure the next output starts on a fresh line; both change the DOM
      // without going through outputToConsole
      ConsoleOutputWriter output = getCOW();
      output.outputToConsole("\033G3;\nmessage\n\n\033g", myErrorClass, isError, ignoreLineCount, false);
      output.normalizePreviousOutput();
      assertLinesMatchDom(output);

      output.ensureStartingOnNewLine();
      assertLinesMatchDom(output);

      output.outputToConsole("no newline", nullClazz, notError, ignoreLineCount, false);
      output.normalizePreviousOutput();
      output.ensureStartingOnNewLine();
      assertLinesMatchDom(output);
      Assert.assertTrue(output.getElement().getInnerText().endsWith("no newline\n"));
   }

   public void testTrimmingStaysExactAcrossPrompts()
   {
      // the uncounted newline added at each prompt would otherwise let the
      // scrollback grow past its limit, one line per command
      ConsoleOutputWriter output = getCOW();
      int maxLines = 5;
      output.setMaxOutputLines(maxLines);
      for (int i = 0; i < 50; i++)
      {
         output.outputToConsole("command " + i + " output", nullClazz, notError, checkLineCount, false);
         output.normalizePreviousOutput();
         output.ensureStartingOnNewLine();
         output.outputToConsole("> ", nullClazz, notError, checkLineCount, false);
      }

      Assert.assertEquals(maxLines, output.getCurrentLines());
      Assert.assertEquals(maxLines, DomUtils.countLines(output.getElement(), true));
   }

   public void testDiscardedGroupDoesNotTrimVisibleOutput()
   {
      ConsoleOutputWriter output = getCOW();
      output.setMaxOutputLines(2);
      output.outputToConsole("\033G3;one\ntwo\n\033g", nullClazz, notError, checkLineCount, false);
      output.outputToConsole("\033[2A\033G2;\nwarn\n\033g", nullClazz, notError, checkLineCount, false);
      assertLinesMatchDom(output);
      output.outputToConsole("next\nlast\n", nullClazz, notError, checkLineCount, false);

      Assert.assertEquals("warnnext\nlast\n", output.getElement().getInnerText());
      Assert.assertEquals(2, output.getCurrentLines());
      assertLinesMatchDom(output);
   }

   public void testStyledOverwriteOfTrimmedRangeAfterGroupTruncation()
   {
      ConsoleOutputWriter output = getCOW();
      output.setMaxOutputLines(3);
      String[] chunks = {
         "\033[31my\n\n",
         "\n\033[F",
         "\033G3;\n\n\033[F",
         "\033[0m\033[A\033[F",
         "\033[Ar",
      };
      for (String chunk : chunks)
      {
         output.outputToConsole(chunk, myClass, notError, checkLineCount, false);
         assertLinesMatchDom(output);
      }

      // The red span has left the scrollback. Rewriting it in another style
      // must neither insert beside a detached node nor revive its contents.
      Assert.assertEquals("\n\n\n", output.getElement().getInnerText());
      output.outputToConsole("\033g\033[99B\033[99Gdone\n", myClass, notError, checkLineCount, false);
      Assert.assertEquals("\n\ndone\n", output.getElement().getInnerText());
      assertLinesMatchDom(output);
   }

   public void testEditsWithinDiscardedRangesStayOffscreen()
   {
      ConsoleOutputWriter output = getCOW();
      output.setMaxOutputLines(2);
      output.outputToConsole(
            "\033[31mabc\033[34mdef\033[0m\nkeep1\nkeep2\n",
            myClass, notError, checkLineCount, false);

      String[] edits = {
         "\033[99F\033[32mX", // prefix of a discarded range
         "\033[2GY",         // inside a discarded range
         "\033[1GZZZZZZ",    // across both discarded ranges
      };
      for (String edit : edits)
      {
         output.outputToConsole(edit, myClass, notError, checkLineCount, false);
         Assert.assertEquals("keep1\nkeep2\n", output.getElement().getInnerText());
         assertLinesMatchDom(output);
      }
   }
}
