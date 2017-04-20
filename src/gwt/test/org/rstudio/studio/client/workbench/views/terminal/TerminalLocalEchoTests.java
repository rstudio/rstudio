/*
 * TerminalLocalEchoTests.java
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
package org.rstudio.studio.client.workbench.views.terminal;

import org.rstudio.core.client.StringSink;

import com.google.gwt.junit.client.GWTTestCase;
import junit.framework.Assert;

public class TerminalLocalEchoTests extends GWTTestCase
{
   static class OutputCatcher implements StringSink
   {

      @Override
      public void write(String str)
      {
         output_ = output_ + str;
      }
      
      public String getOutput()
      {
         return output_;
      }
      
      public boolean isEmpty()
      {
         return output_.length() == 0;
      }
      
      public void clear()
      {
         output_ = "";
      }

      private String output_ = "";
   }
   
   private void echoString(TerminalLocalEcho echo, String output)
   {
      for (char c : output.toCharArray()) 
      {
         echo.echo(String.valueOf(c));
      }
   }
   
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   public void testFixtureAssumptions()
   {
      OutputCatcher output = new OutputCatcher();
      TerminalLocalEcho echo = new TerminalLocalEcho(output);
      
      Assert.assertTrue(output.isEmpty());
      output.write("abc");
      Assert.assertEquals("abc", output.getOutput());
      output.clear();
      Assert.assertTrue(output.isEmpty());
      Assert.assertTrue(echo.isEmpty());
   }      
      
   public void testSimpleTextEcho()
   {
      OutputCatcher output = new OutputCatcher();
      TerminalLocalEcho echo = new TerminalLocalEcho(output);
      
      String expected = "Hello World";
      
      echoString(echo, expected);
      Assert.assertFalse(echo.isEmpty());
      Assert.assertEquals(expected, output.getOutput());
      output.clear();
      
      echo.write(expected);
      Assert.assertTrue(echo.isEmpty());
      Assert.assertTrue(output.isEmpty());
   }

   public void testSimpleSuperSetTextEcho()
   {
      OutputCatcher output = new OutputCatcher();
      TerminalLocalEcho echo = new TerminalLocalEcho(output);
      
      String written = "Hello World";
      String received = "Hello World My Old Friend";
      String expected = " My Old Friend";
      
      echoString(echo, written);
      Assert.assertFalse(echo.isEmpty());
      Assert.assertEquals(written, output.getOutput());
      output.clear();
      
      echo.write(received);
      Assert.assertTrue(echo.isEmpty());
      Assert.assertEquals(expected, output.getOutput());
   }

   public void testSimplePartialMatch()
   {
      OutputCatcher output = new OutputCatcher();
      TerminalLocalEcho echo = new TerminalLocalEcho(output);
      
      String written = "Hello World";
      String received = "Hello Planet";
      String expected = received;
      
      echoString(echo, written);
      Assert.assertFalse(echo.isEmpty());
      Assert.assertEquals(written, output.getOutput());
      output.clear();
      
      echo.write(received);
      Assert.assertTrue(echo.isEmpty());
      Assert.assertEquals(expected, output.getOutput());
   }

   public void testInterruptedPartialMatch()
   {
      OutputCatcher output = new OutputCatcher();
      TerminalLocalEcho echo = new TerminalLocalEcho(output);
      
      String written = "Hello World";
      String received = "Hello \7Planet";
      String expected = "\7Planet";
      
      echoString(echo, written);
      Assert.assertFalse(echo.isEmpty());
      Assert.assertEquals(written, output.getOutput());
      output.clear();
      
      echo.write(received);
      Assert.assertTrue(echo.isEmpty());
      Assert.assertEquals(expected, output.getOutput());
   }


   public void testSimpleUnmatchedText()
   {
      OutputCatcher output = new OutputCatcher();
      TerminalLocalEcho echo = new TerminalLocalEcho(output);
      
      String written = "Hello World";
      String read = "This differs";
      
      echoString(echo, written);
      output.clear();
      
      echo.write(read);
      Assert.assertTrue(echo.isEmpty());
      Assert.assertEquals(read, output.getOutput());
   }

   public void testCarriageReturnChar()
   {
      OutputCatcher output = new OutputCatcher();
      TerminalLocalEcho echo = new TerminalLocalEcho(output);
      
      String written = "Hello World";
      String read = "Hello\r World";
      String expected = "\r";
      
      echoString(echo, written);
      output.clear();
      
      echo.write(read);
      Assert.assertTrue(echo.isEmpty());
      Assert.assertEquals(expected, output.getOutput());
   }

   public void testNewLineChar()
   {
      OutputCatcher output = new OutputCatcher();
      TerminalLocalEcho echo = new TerminalLocalEcho(output);
      
      String written = "Hello World";
      String read = "Hello World\n";
      String expected = "\n";
      
      echoString(echo, written);
      output.clear();
      
      echo.write(read);
      Assert.assertTrue(echo.isEmpty());
      Assert.assertEquals(expected, output.getOutput());
   }

   public void testCRLFChar()
   {
      OutputCatcher output = new OutputCatcher();
      TerminalLocalEcho echo = new TerminalLocalEcho(output);
      
      String written = "Hello World";
      String read = "\r\nHello World\r\n";
      String expected = "\r\n\r\n";
      
      echoString(echo, written);
      output.clear();
      
      echo.write(read);
      Assert.assertTrue(echo.isEmpty());
      Assert.assertEquals(expected, output.getOutput());
   }

   public void testBackspaceChar()
   {
      OutputCatcher output = new OutputCatcher();
      TerminalLocalEcho echo = new TerminalLocalEcho(output);
      
      String written = "Hell\bo World";
      String read = "Hell\bo World";
      String expected = "\b";
      
      echoString(echo, written);
      output.clear();
      
      echo.write(read);
      Assert.assertTrue(echo.isEmpty());
      Assert.assertEquals(expected, output.getOutput());
   }

   public void testDELChar()
   {
      OutputCatcher output = new OutputCatcher();
      TerminalLocalEcho echo = new TerminalLocalEcho(output);
      
      String written = "Hello W\177orld";
      String read = "Hello W\177orld";
      String expected = "\177";
      
      echoString(echo, written);
      output.clear();
      
      echo.write(read);
      Assert.assertTrue(echo.isEmpty());
      Assert.assertEquals(expected, output.getOutput());
   }

   public void testBELChar()
   {
      OutputCatcher output = new OutputCatcher();
      TerminalLocalEcho echo = new TerminalLocalEcho(output);
      
      String written = "The Quick Brown Ferret";
      String read = "The Quick\7 Brown \7Ferret";
      String expected = "\7\7";
      
      echoString(echo, written);
      output.clear();
      
      echo.write(read);
      Assert.assertTrue(echo.isEmpty());
      Assert.assertEquals(expected, output.getOutput());
   }

   public void testAllCtrlChar()
   {
      OutputCatcher output = new OutputCatcher();
      TerminalLocalEcho echo = new TerminalLocalEcho(output);
      
      String written = "The Quick Brown\r\n Ferret";
      String read = "The Q\bui\177ck\7 Brown\r\n \7Ferret";
      String expected = "\b\177\7\r\n\7";
      
      echoString(echo, written);
      output.clear();
      
      echo.write(read);
      Assert.assertTrue(echo.isEmpty());
      Assert.assertEquals(expected, output.getOutput());
   }

   public void testAnsiColorSequence()
   {
      OutputCatcher output = new OutputCatcher();
      TerminalLocalEcho echo = new TerminalLocalEcho(output);
      
      String written = "The Quick Green Ferret";
      String read = "The Quick \033[32mGreen\033[39m Ferret";
      String expected = "\033[32m\033[39m";
      
      echoString(echo, written);
      output.clear();
      
      echo.write(read);
      Assert.assertTrue(echo.isEmpty());
      Assert.assertEquals(expected, output.getOutput());
   }

   public void testAnsiClearEOLSequence()
   {
      OutputCatcher output = new OutputCatcher();
      TerminalLocalEcho echo = new TerminalLocalEcho(output);
      
      String written = "The Happy Ferret";
      String read = "The Ha\033[Kppy Ferret";
      String expected = "\033[K";
      
      echoString(echo, written);
      output.clear();
      
      echo.write(read);
      Assert.assertTrue(echo.isEmpty());
      Assert.assertEquals(expected, output.getOutput());
   }

   public void testEchoPause()
   {
      OutputCatcher output = new OutputCatcher();
      TerminalLocalEcho echo = new TerminalLocalEcho(output);
      
      String password = "MySecretPassword!";
      
      Assert.assertFalse(echo.paused());
      echo.pause(50);
      
      echoString(echo, password);
      Assert.assertTrue(echo.paused());
      Assert.assertTrue(echo.isEmpty());
      Assert.assertTrue(output.isEmpty());
   }

}