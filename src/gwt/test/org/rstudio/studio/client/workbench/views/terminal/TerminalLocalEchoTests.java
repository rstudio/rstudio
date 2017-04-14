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

}