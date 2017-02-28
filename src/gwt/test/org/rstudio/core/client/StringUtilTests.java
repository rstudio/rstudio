/*
 * StringUtilTests.java
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

import java.util.Date;

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
    	  fail("Expected NullPointerException to be thrown");
      } catch (NullPointerException ex) { }
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

   // -----------------------------------------------------------------------

   // TODO: Tests for remaining public StringUtil methods
   //
   // public static String formatFileSize(long size)

   // -----------------------------------------------------------------------

   // public static String formatElapsedTime(int seconds)

   // -----------------------------------------------------------------------

   // public static String formatFileSize(int size)

   // -----------------------------------------------------------------------

   // public static native int nativeDivide(int num, int denom) 

   // -----------------------------------------------------------------------

   // public static String prettyFormatNumber(double number)

   // -----------------------------------------------------------------------

   // public static String formatGeneralNumber(long number)

   // -----------------------------------------------------------------------

   // public static String formatPercent(double number)

   // -----------------------------------------------------------------------

   // public static Size characterExtent(String text)

   // -----------------------------------------------------------------------

   // public static String chomp(String string)

   // -----------------------------------------------------------------------

   // public static boolean isNullOrEmpty(String val)

   // -----------------------------------------------------------------------

   // public static String textToRLiteral(String value)

   // -----------------------------------------------------------------------

   // public static String toRSymbolName(String name)

   // -----------------------------------------------------------------------

   // public static String notNull(String s)

   // -----------------------------------------------------------------------

   // public static String indent(String str, String indent)

   // -----------------------------------------------------------------------

   // public static String join(String delimiter, String... strings)

   // -----------------------------------------------------------------------

   // public static String join(String[] collection, String delim)

   // -----------------------------------------------------------------------

   // public static String join(Collection<?> collection, String delim)

   // -----------------------------------------------------------------------

   // public static String firstNotNullOrEmpty(String[] strings)

   // -----------------------------------------------------------------------

   // public static String shortPathName(FileSystemItem item, int maxWidth)

   // -----------------------------------------------------------------------

   // public static String shortPathName(FileSystemItem item, String styleName, int maxWidth)

   // -----------------------------------------------------------------------

   // public static Iterable<String> getLineIterator(final String text)

   // -----------------------------------------------------------------------

   // public static String trimBlankLines(String data)

   // -----------------------------------------------------------------------

   // public static String trimLeft(String str)

   // -----------------------------------------------------------------------

   // public static String trimRight(String str)

   // -----------------------------------------------------------------------

   // public static String getCommonPrefix(String[] lines, boolean allowPhantomWhitespace, boolean skipWhitespaceOnlyLines)

   // -----------------------------------------------------------------------

   // public static String pathToTitle(String path)

   // -----------------------------------------------------------------------

   // public static String joinStrings(List<String> strings, String separator)

   // -----------------------------------------------------------------------

   // public static String makeAbsoluteUrl(String inputUrl)

   // -----------------------------------------------------------------------

   // public static String ensureSurroundedWith(String string, char chr)

   // -----------------------------------------------------------------------

   // public static String capitalize(String input)

   // -----------------------------------------------------------------------

   // public static final native String capitalizeAllWords(String input)

   // -----------------------------------------------------------------------

   // public static int countMatches(String line, char chr)

   // -----------------------------------------------------------------------

   // public static String stripRComment(String string)

   // -----------------------------------------------------------------------

   // public static String stripBalancedQuotes(String string)

   // -----------------------------------------------------------------------

   // public static String maskStrings(String string)

   // -----------------------------------------------------------------------

   // public static String maskStrings(String string, char ch)

   // -----------------------------------------------------------------------

   // public static boolean isEndOfLineInRStringState(String string)

   // -----------------------------------------------------------------------

   // public static boolean isSubsequence(String self, String other, boolean caseInsensitive)

   // -----------------------------------------------------------------------

   // public static boolean isSubsequence(String self, String other)

   // -----------------------------------------------------------------------

   // public static List<Integer> subsequenceIndices(String sequence, String query)

   // -----------------------------------------------------------------------

   // public static String getExtension(String string, int dots)

   // -----------------------------------------------------------------------

   // public static String getExtension(String string)

   // -----------------------------------------------------------------------

   // public static String getToken(String string, int pos, String tokenRegex, boolean expandForward, boolean backOverWhitespace)

   // -----------------------------------------------------------------------

   // public static String repeat(String string, int times)

   // -----------------------------------------------------------------------

   // public static ArrayList<Integer> indicesOf(String string, char ch)

   // -----------------------------------------------------------------------

   // public static boolean isWhitespace(String string)

   // -----------------------------------------------------------------------

   // public static boolean isComplementOf(String self, String other)

   // -----------------------------------------------------------------------

   // public static String collapse(Map<String, String> map, String keyValueSeparator, String fieldSeparator)

   // -----------------------------------------------------------------------

   // public static String prettyCamel(String string)

   // -----------------------------------------------------------------------

   // public static native final String escapeRegex(String regexString) 

   // -----------------------------------------------------------------------

   // public static final String getIndent(String line)

   // -----------------------------------------------------------------------

   // public static final String truncate(String string, int targetLength, String suffix)

   // -----------------------------------------------------------------------

   // public static boolean isOneOf(String string, String... candidates)

   // -----------------------------------------------------------------------

   // public static boolean isOneOf(char ch, char... candidates)

   // -----------------------------------------------------------------------

   // public static final String makeRandomId(int length) 

   // -----------------------------------------------------------------------

   // public static String ensureQuoted(String string)

   // -----------------------------------------------------------------------

   // public static String stringValue(String string)

   // -----------------------------------------------------------------------

   // public static final native String encodeURI(String string) 

   // -----------------------------------------------------------------------

   // public static final native String encodeURIComponent(String string) 

   // -----------------------------------------------------------------------

   // public static final native String normalizeNewLines(String string) 

   // -----------------------------------------------------------------------

   // public static final native JsArrayString split(String string, String delimiter)

   // -----------------------------------------------------------------------

   // public static final HashMap<String, String> COMPLEMENTS

   // -----------------------------------------------------------------------

   // public static final native String crc32(String str)

   // -----------------------------------------------------------------------

}
