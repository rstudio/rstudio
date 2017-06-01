/*
 * ConsoleOutputWriterTests.java
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
package org.rstudio.core.client;

import java.util.List;

import org.rstudio.core.client.ConsoleOutputWriter;
import org.rstudio.core.client.dom.DomUtils;

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
      return String.valueOf(i) + "\n";
   }
   
   private String getInnerHTML(ConsoleOutputWriter output)
   {
      SpanElement outerSpan = SpanElement.as(output.getElement().getFirstChildElement());
      return outerSpan.getInnerHTML();
   }

   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   public void testCreation()
   {
      ConsoleOutputWriter output = new ConsoleOutputWriter();
      Assert.assertNotNull(output);
      Assert.assertNotNull(output.getWidget());
   }

   public void testSetGetMaxLines()
   {
      ConsoleOutputWriter output = new ConsoleOutputWriter();
      Assert.assertEquals(-1,  output.getMaxOutputLines());
      output.setMaxOutputLines(1000);
      Assert.assertEquals(1000, output.getMaxOutputLines());
   }
   
   public void testSimpleLineCount()
   {
      ConsoleOutputWriter output = new ConsoleOutputWriter();
      Assert.assertEquals(0, output.getCurrentLines());
      Assert.assertTrue(output.outputToConsole("Hello World", 
            nullClazz, notError, checkLineCount));
      Assert.assertEquals(0,  output.getCurrentLines());
      Assert.assertTrue(output.outputToConsole(" more on same line", 
            nullClazz, notError, checkLineCount));
      Assert.assertEquals(0,  output.getCurrentLines());
      Assert.assertTrue(output.outputToConsole("next line starts now\n", 
            nullClazz, notError, checkLineCount));
      Assert.assertEquals(1,  output.getCurrentLines());
      Assert.assertEquals(1, DomUtils.countLines(output.getElement(), true));
   }
   
   public void testTrimming()
   {
      // this test is rather bulky; it tests that lines get trimmed, but also
      // some basic cross-checking of the output structure, behavior of
      // trimOutput, and lack of a class attribute when no clazz was specified
      ConsoleOutputWriter output = new ConsoleOutputWriter();
      final int maxLines = 25;
      output.setMaxOutputLines(maxLines);

      // write just below the maximum
      for (int i = 0; i < maxLines; i++)
      {
         Assert.assertTrue(output.outputToConsole(numberedLine(i), 
               nullClazz, notError, checkLineCount));
         Assert.assertEquals(i + 1, output.getCurrentLines());
      }
      
      // trim should be a no-op when below the limit
      Assert.assertEquals(maxLines, output.getCurrentLines());
      Assert.assertFalse(output.trimExcess());
      Assert.assertEquals(maxLines, output.getCurrentLines());

      // go over the limit
      Assert.assertFalse(output.outputToConsole(numberedLine(maxLines), 
            nullClazz, notError, checkLineCount));
      Assert.assertEquals(maxLines, output.getCurrentLines());
      
      // go over the limit again
      Assert.assertFalse(output.outputToConsole(numberedLine(maxLines + 1), 
            nullClazz, notError, checkLineCount));
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
      ConsoleOutputWriter output = new ConsoleOutputWriter();
      final int maxLines = 50;
      output.setMaxOutputLines(maxLines);

      for (int i = 0; i < maxLines + 10; i++)
      {
         Assert.assertTrue(output.outputToConsole(numberedLine(i), 
               myClass, notError, ignoreLineCount));
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
      ConsoleOutputWriter output = new ConsoleOutputWriter();
      String errorMsg = "Oh no, an error!!";

      Assert.assertTrue(output.outputToConsole(errorMsg,
               myErrorClass, isError, ignoreLineCount));
      
      Assert.assertEquals(0,  output.getCurrentLines());

      String expected = "<span class=\"myErrorClass\">" + errorMsg + "</span>";
      Assert.assertEquals(expected, getInnerHTML(output));
   }

   public void testWriteSimpleErrorWithNewline()
   {
      ConsoleOutputWriter output = new ConsoleOutputWriter();
      String errorMsg = "Oh no, an error!!\n";

      Assert.assertTrue(output.outputToConsole(errorMsg,
               myErrorClass, isError, ignoreLineCount));
      
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
      ConsoleOutputWriter output = new ConsoleOutputWriter();
      
      output.outputToConsole("1\n", myErrorClass, isError, ignoreLineCount);
      output.outputToConsole("2\n", myErrorClass, isError, ignoreLineCount);
      output.outputToConsole("3\n", myErrorClass, isError, ignoreLineCount);
      output.outputToConsole("4\n", myErrorClass, isError, ignoreLineCount);
      
      String lastError = "Error in h() : An error! Oh No!"; 
      output.outputToConsole(lastError, myErrorClass, isError, ignoreLineCount);
      
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
      ConsoleOutputWriter output = new ConsoleOutputWriter();
      
      output.outputToConsole("1\n", myErrorClass, isError, ignoreLineCount);
      output.outputToConsole("2\n", myErrorClass, isError, ignoreLineCount);
      output.outputToConsole("3\n", myErrorClass, isError, ignoreLineCount);
      output.outputToConsole("4\n", myErrorClass, isError, ignoreLineCount);
      
      String lastError1 = "Error in h2() : An error!\n";
      String lastError2 = "\033[31mOh No!\n\033[39m";
      String lastError3 = "\033[43m\033[31mWow!\033[39m\033[49m";
      
      output.outputToConsole(lastError1 + lastError2 + lastError3, 
            myErrorClass, isError, ignoreLineCount);
      
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
      ConsoleOutputWriter output = new ConsoleOutputWriter();
      
      output.outputToConsole("Hello", myClass, notError, ignoreLineCount);
      output.outputToConsole("World", myClass, notError, ignoreLineCount);
      
      Assert.assertEquals(0, output.getCurrentLines());
      
      Assert.assertEquals("<span class=\"myClass\">HelloWorld</span>", 
            getInnerHTML(output));
   }

   public void test4()
   {
      // output 4 regular strings followed by newlines; should end up in a
      // single span
      ConsoleOutputWriter output = new ConsoleOutputWriter();
      
      output.outputToConsole("One\n", myClass, notError, ignoreLineCount);
      output.outputToConsole("Two\n", myClass, notError, ignoreLineCount);
      output.outputToConsole("Three\n", myClass, notError, ignoreLineCount);
      output.outputToConsole("Four\n", myClass, notError, ignoreLineCount);
      
      Assert.assertEquals(4, output.getCurrentLines());
      
      Assert.assertEquals("<span class=\"myClass\">One\nTwo\nThree\nFour\n</span>", 
            getInnerHTML(output));

   }

   public void test5()
   {
      // output two error strings, then one regular string, then another error
      // and make sure DOM ends up as expected
      ConsoleOutputWriter output = new ConsoleOutputWriter();
      
      output.outputToConsole("1\n", myErrorClass, isError, ignoreLineCount);
      output.outputToConsole("2\n", myErrorClass, isError, ignoreLineCount);
      output.outputToConsole("Hello ", myClass, notError, ignoreLineCount);
      output.outputToConsole("world\n", myClass, notError, ignoreLineCount);
      output.outputToConsole("3\n", myErrorClass, isError, ignoreLineCount);
      
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
      ConsoleOutputWriter output = new ConsoleOutputWriter();
      
      output.outputToConsole("1\n", myErrorClass, isError, ignoreLineCount);
      output.outputToConsole("2\n", myErrorClass, isError, ignoreLineCount);
      output.outputToConsole("3\n", myErrorClass, isError, ignoreLineCount);
      output.outputToConsole("4", myErrorClass, isError, ignoreLineCount);
      
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
      ConsoleOutputWriter output = new ConsoleOutputWriter();
      
      output.outputToConsole("Error in test7a() : \033[32mHi\033[39m", myErrorClass, 
            isError, ignoreLineCount);
      
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
      ConsoleOutputWriter output = new ConsoleOutputWriter();

      output.outputToConsole("1\n", myErrorClass, isError, ignoreLineCount);
      output.outputToConsole("2\n", myErrorClass, isError, ignoreLineCount);
      output.outputToConsole("3\n", myErrorClass, isError, ignoreLineCount);
      output.outputToConsole("4\n", myErrorClass, isError, ignoreLineCount);
      output.outputToConsole(
            "Error in test8b() : An error!\n" +
            "\033[31mOh No!\n\033[39m" +
            "\033[43m\033[31mA multiline error with colors.\n\033[39m\033[49m" +
            "\033[7mAnd some inverse text.\n\033[27m" +
            "\033[1m\033[3mThe Horror!!\033[23m\033[22m",
            myErrorClass, isError, ignoreLineCount);
      
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
      ConsoleOutputWriter output = new ConsoleOutputWriter();

      output.outputToConsole("Hello\n", myErrorClass, isError, ignoreLineCount);
      output.outputToConsole("A\nB\nC", myErrorClass, isError, ignoreLineCount);

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
      ConsoleOutputWriter output = new ConsoleOutputWriter();
      output.outputToConsole("\rfoobar", myClass, notError, ignoreLineCount);
      output.outputToConsole("\rX foobar\n", myClass, notError, ignoreLineCount);
      
      Assert.assertEquals(1, output.getCurrentLines());
      Assert.assertEquals(
            "<span class=\"myClass\">X foobar\n</span>",
            getInnerHTML(output));
   }
   
   public void test11()
   {
      // inline editing via \r with ansi codes
      ConsoleOutputWriter output = new ConsoleOutputWriter();
      output.outputToConsole("\rfoobar", myClass, notError, ignoreLineCount);
      output.outputToConsole("\r\033[32mX\033[39m \033[31mfoobar\033[39m\n", 
            myClass, notError, ignoreLineCount);
      
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
      ConsoleOutputWriter output = new ConsoleOutputWriter();
      output.outputToConsole("Hello\nWorld", myClass, notError, ignoreLineCount);
      output.outputToConsole("\r\033[32mX\033[39m \033[31mY\033[39m\n", 
            myClass, notError, ignoreLineCount);
      
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
      ConsoleOutputWriter output = new ConsoleOutputWriter();
      output.outputToConsole("Hello\nWorld", myClass, notError, ignoreLineCount);
      output.outputToConsole("\r\033[32m123\033[39m\033[31m45\033[39m\n", 
            myClass, notError, ignoreLineCount);
      
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
      ConsoleOutputWriter output = new ConsoleOutputWriter();
      
      output.outputToConsole("Beginning\n", myClass, notError, ignoreLineCount);
      output.outputToConsole("1\n", myErrorClass, isError, ignoreLineCount);
      output.outputToConsole("2\n", myErrorClass, isError, ignoreLineCount);
      output.outputToConsole("Hello ", myClass, notError, ignoreLineCount);
      output.outputToConsole("world\n", myClass, notError, ignoreLineCount);
      output.outputToConsole("3\n", myErrorClass, isError, ignoreLineCount);
      output.outputToConsole("END", myClass, notError, ignoreLineCount);
      
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
      ConsoleOutputWriter output = new ConsoleOutputWriter();

      output.outputToConsole("1\n", myErrorClass, isError, ignoreLineCount);
      output.outputToConsole("2\n", myErrorClass, isError, ignoreLineCount);
      output.outputToConsole(
            "Error in h15() : A multiline error without colors!\nOh No!", 
            myErrorClass, isError, ignoreLineCount);

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
      ConsoleOutputWriter output = new ConsoleOutputWriter();
      
      output.outputToConsole("one\n", myClass, notError, ignoreLineCount);
      output.outputToConsole("two\n", myClass, notError, ignoreLineCount);
      output.outputToConsole("three", myClass, notError, ignoreLineCount);

      Assert.assertEquals(2, output.getCurrentLines());

      Assert.assertEquals(
            "<span class=\"myClass\">one\ntwo\nthree</span>",
            getInnerHTML(output));
   } 
 }
