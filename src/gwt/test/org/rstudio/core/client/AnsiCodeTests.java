/*
 * AnsiCodeTests.java
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

import org.rstudio.core.client.AnsiCode;
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
}