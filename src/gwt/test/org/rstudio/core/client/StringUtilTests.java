/*
 * StringUtilTests.java
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

import java.util.Date;
import java.util.List;
import java.util.ArrayList;

import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.junit.client.GWTTestCase;

public class StringUtilTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }
   
   // -- StringUtil.padRight tests --------------------------------------------

   public void testPadRightNullInput()
   {
      String orig = null;
      int minWidth = 5;
      try
      {
         StringUtil.padRight(orig, minWidth);
         fail("Expected exception to be thrown");
      } 
      catch (NullPointerException ex) { }
      catch (JavaScriptException ex) { }
   }

   public void testPadRightEmptyInput()
   {
      String empty = "";
      String twentyFiveSpaces = "                         ";
      int minWidth = 25;
      String result = StringUtil.padRight(empty, minWidth);
      assertTrue(result.length() == minWidth);
      assertEquals(result, twentyFiveSpaces);
   }

   public void testPadRightSpaces()
   {
      String twoSpaces = "  ";
      String fourSpaces = "    ";
      int minWidth = 4;
      String result = StringUtil.padRight(twoSpaces,  minWidth);
      assertTrue(result.length() == minWidth);
      assertEquals(result, fourSpaces);
   }

   public void testPadRightAlreadyLonger()
   {
      String original = "12345";
      int minWidth = 4;
      assertTrue(original.length() > minWidth);
      String result = StringUtil.padRight(original, minWidth);
      assertTrue(result.length() == original.length());
   }

   // -- StringUtil.parseInt tests --------------------------------------------

   public void testParseIntNullInput()
   {
      String input = null;
      int def = 999;
      int result = StringUtil.parseInt(input, def);
      assertEquals(def, result);
   }

   public void testParseIntNonNumericInput()
   {
      String notAnInt = "hello";
      int def = 125;
      int result = StringUtil.parseInt(notAnInt, def);
      assertEquals(def, result);
   }

   public void testParseIntValidPositive()
   {
      String anInt = "5433234";
      int expected =  5433234;
      int def = 0;
      assertFalse(def == expected);
      int result = StringUtil.parseInt(anInt, def);
      assertEquals(expected, result);
   }

   public void testParseIntOutOfRange()
   {
      String aHugeNumber = "999999999999999999";
      int expected = 256;
      int def = expected;
      int result = StringUtil.parseInt(aHugeNumber, def);
      assertEquals(expected, result);
   }

   public void testParseIntNegative()
   {
      String anInt = "-1";
      int expected =  -1;
      int def = 1;
      assertFalse(def == expected);
      int result = StringUtil.parseInt(anInt, def);
      assertEquals(expected, result);
   }

   // -- StringUtil.formatDate tests  -----------------------------------------

   public void testFormatDateNullInput()
   {
      Date input = null;
      String expected = "";
      String result = StringUtil.formatDate(input);
      assertEquals(expected, result);
   }

   public void testFormatDate()
   {
      String result = StringUtil.formatDate(new Date());
   
      // just check that it's got minimum valid length; don't want to 
      // mess with timezone awareness to do exact check
      // MMM d, yyyy, h:mm AM
      assertTrue(result.length() >= 20);
   }
   
   public void testNewlineCount()
   {
      String input = "hello\nworld\n";
      assertEquals(2, StringUtil.newlineCount(input));
      
      input = "nothing here";
      assertEquals(0, StringUtil.newlineCount(input));
      
      input = "\n\n\nzoom\n";
      assertEquals(4, StringUtil.newlineCount(input));
      
      input = "";
      assertEquals(0, StringUtil.newlineCount(input));
   }

   public void testGetAuthorityFromUrl()
   {
      String url = "http://rstudio.com/products";
      assertEquals("rstudio.com", StringUtil.getAuthorityFromUrl(url));
      
      url = "https://google.com";
      assertEquals("google.com", StringUtil.getAuthorityFromUrl(url));
      
      url = "8.8.8.8:443/notfound.html";
      assertEquals("8.8.8.8:443", StringUtil.getAuthorityFromUrl(url));
   }
   
   public void testGetHostFromUrl()
   {
      String url = "8.8.8.8:443/notfound.html";
      assertEquals("8.8.8.8", StringUtil.getHostFromUrl(url));
      
      url = "https://localhost:443/";
      assertEquals("localhost", StringUtil.getHostFromUrl(url));

      url = "https://bakersfield/";
      assertEquals("bakersfield", StringUtil.getHostFromUrl(url));
   }
   
   public void testStringEquals()
   {
      String one_1 = "one";
      String two_1 = "two";
      String one_2 = "one";
      String two_2 = "two";
      String null_1 = null;
      String null_2 = null;
      
      assertTrue(StringUtil.equals(one_1, one_2));
      assertTrue(StringUtil.equals(one_2, one_1)); 
      assertTrue(StringUtil.equals(two_1, two_2));
      assertTrue(StringUtil.equals(two_2, two_1));
      
      assertTrue(StringUtil.equals(null_1, null_2));
      assertTrue(StringUtil.equals(null_2, null_1));
      
      assertTrue(StringUtil.equals(one_1, one_1));
      assertTrue(StringUtil.equals(one_2, one_2));
      assertTrue(StringUtil.equals(two_1, two_1));
      assertTrue(StringUtil.equals(two_2, two_2));
      
      assertTrue(StringUtil.equals(null_1, null_1));
      assertTrue(StringUtil.equals(null_2, null_2));
      
      assertFalse(StringUtil.equals(one_1, two_1));
      assertFalse(StringUtil.equals(two_1, one_1));
      assertFalse(StringUtil.equals(one_2, two_2));
      assertFalse(StringUtil.equals(two_2, one_2));
      
      assertFalse(StringUtil.equals(one_1, null_1));
      assertFalse(StringUtil.equals(two_1, null_1));
      assertFalse(StringUtil.equals(one_1, null_1));
      assertFalse(StringUtil.equals(two_1, null_1));
      
      assertFalse(StringUtil.equals(one_2, null_1));
      assertFalse(StringUtil.equals(two_2, null_1));
      assertFalse(StringUtil.equals(one_2, null_1));
      assertFalse(StringUtil.equals(two_2, null_1));
      
      assertFalse(StringUtil.equals(null_2, one_1));
      assertFalse(StringUtil.equals(null_2, two_1));
      assertFalse(StringUtil.equals(null_2, one_2));
      assertFalse(StringUtil.equals(null_2, two_2));
   } 
   
   public void testConciseElapsedTime()
   {
      String seconds = StringUtil.conciseElaspedTime(35);
      assertEquals("0:35", seconds);

      String minutes = StringUtil.conciseElaspedTime(66);
      assertEquals("1:06", minutes);

      String hours = StringUtil.conciseElaspedTime(3606);
      assertEquals("1:00:06", hours);
      
      String days = StringUtil.conciseElaspedTime(180061);
      assertEquals("2:02:01:01", days);
   }
  
   public void testGetCssIdentifier()
   {
      List<Pair<String, String>> testList = new ArrayList<>();
      testList.add(new Pair<>("4abc_bad",        "_abc_bad"));
      testList.add(new Pair<>("--verybad",       "_-verybad"));
      testList.add(new Pair<>("-2badagain?",     "_2badagain_"));
      testList.add(new Pair<>("great342_-↲",     "great342_-↲"));
      testList.add(new Pair<>("-_perfectlyfine", "-_perfectlyfine"));

      for (Pair<String, String> td  : testList)
      {
         String result = StringUtil.getCssIdentifier(td.first);
         assertTrue(StringUtil.equals(td.second, result));
      }
   }

   public void testEscapeBashPathNoSpecialChars()
   {
      String input = "NothingSpecialHere.129,._+@%/-";
      String expected = input;
      String result = StringUtil.escapeBashPath(input, true);
      assertTrue(StringUtil.equals(result, expected));
   }
   
   public void testEscapeBashPathNoSpecialCharsNoTilde()
   {
      String input = "NothingSpecialHere.129,._+@%/-";
      String expected = input;
      String result = StringUtil.escapeBashPath(input, true);
      assertTrue(StringUtil.equals(result, expected));
   }
   
   public void testEscapeBashPathSpacesAndEmbeddedTilde()
   {
      String input = " ~/Something Special Here. 129, ._ +@%/- ";
      String expected = "\\ \\~/Something\\ Special\\ Here.\\ 129,\\ ._\\ +@%/-\\ ";
      String result = StringUtil.escapeBashPath(input, true);
      assertTrue(StringUtil.equals(result, expected));
   }
   
   public void testEscapeBashPathSpecialAndUnescapedTilde()
   {
      String input = "~/Something Special Here! 129, ._ +@%/->";
      String expected = "~/Something\\ Special\\ Here\\!\\ 129,\\ ._\\ +@%/-\\>";
      String result = StringUtil.escapeBashPath(input, false);
      assertTrue(StringUtil.equals(result, expected));
   }
   
   public void testEscapeBashPathSpecialAndEscapedTilde()
   {
      String input = "~/Something Special Here! 129, ._ +@%/->";
      String expected = "\\~/Something\\ Special\\ Here\\!\\ 129,\\ ._\\ +@%/-\\>";
      String result = StringUtil.escapeBashPath(input, true);
      assertTrue(StringUtil.equals(result, expected));
   }
   
   public void testIsCharAt()
   {
      assertTrue(StringUtil.isCharAt("abcd", 'a', 0));
      assertTrue(StringUtil.isCharAt("abcd", 'b', 1));
      assertTrue(StringUtil.isCharAt("abcd", 'c', 2));
      assertTrue(StringUtil.isCharAt("abcd", 'd', 3));
   }
   
   public void testIsCharAtOOB()
   {
      assertFalse(StringUtil.isCharAt("abcd", 'a', -1));
      assertFalse(StringUtil.isCharAt(null, 'a', 0));
      assertFalse(StringUtil.isCharAt("012345", '6', 6));
   }
   
   public void testSafeCharAt()
   {
      String str = "";
      
      assertEquals(StringUtil.charAt(str, 0), '\0');
      assertEquals(StringUtil.charAt(str, -1), '\0');
      assertEquals(StringUtil.charAt(str, 100), '\0');
      
      str = "abcd";

      assertEquals(StringUtil.charAt(str, 0), 'a');
      assertEquals(StringUtil.charAt(str, 1), 'b');
      assertEquals(StringUtil.charAt(str, 2), 'c');
      assertEquals(StringUtil.charAt(str, 3), 'd');
   }
}
