/*
 * StringUtil.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.Window;

import org.rstudio.core.client.dom.DomMetrics;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.core.client.regex.Pattern.ReplaceOperation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class StringUtil
{
   public static String padRight(String value, int minWidth)
   {
      if (value.length() >= minWidth)
         return value;

      StringBuilder out = new StringBuilder();
      for (int i = minWidth - value.length(); i > 0; i--)
         out.append(' ');
      out.append(value);
      return out.toString();
   }

   public static int parseInt(String value, int defaultValue)
   {
      try
      {
         return Integer.parseInt(value);
      }
      catch (NumberFormatException nfe)
      {
         return defaultValue;
      }
   }

   public static String formatDate(Date date)
   {
      if (date == null)
         return "";

      return DATE_FORMAT.format(date);
   }

   public static String formatFileSize(long size)
   {
      return formatFileSize(new Long(size).intValue());
   }
   
   // return a friendly (not precise) elapsed time
   public static String formatElapsedTime(int seconds)
   {
      if (seconds < 60)
         return seconds + " second" + (seconds == 1 ? "" : "s");
      else if (seconds < 3600)
         return (seconds / 60) + " minute" + ((seconds / 60) == 1 ? "" : "s");
      else 
         return (seconds / 3600) + " hour" + ((seconds / 3600) == 1 ? "" : "s");
   }

   // Return current time as a timestamp (yyyy/m/d hh:mm:ss)
   public static native String getTimestamp() /*-{
      var now = new Date();
      var date = [ now.getFullYear(), now.getMonth() + 1, now.getDate() ];
      var time = [ now.getHours(), now.getMinutes(), now.getSeconds() ];
      for (var i = 1; i < 3; i++ ) {
         if (time[i] < 10) {
            time[i] = "0" + time[i];
         }
      }
      return date.join("/") + " " + time.join(":");
   }-*/;
    
   // Given a raw size, convert it to a human-readable value 
   // (e.g. 11580 -> "11.3 KB"). Note that this routine must generally avoid
   // implicit casts and use only ints; GWT's JavaScript compiler will truncate
   // values it believes to be ints to Int32 max/min (+/- 2 billion) during
   // type checking, and this function deals in file and object sizes larger
   // than that.
   public static String formatFileSize(int size)
   {
      int i = 0, divisor = 1;

      for (; nativeDivide(size, divisor) > 1024 && i < LABELS.length; i++)
      {
         divisor *= 1024;
      }

      return FORMAT.format((double)size / divisor) + " " + LABELS[i];
   }
   
   // Peform an integer division and return the result. GWT's division operator
   // truncates the result to Int32 range. 
   public static native int nativeDivide(int num, int denom) 
   /*-{
      return num / denom;
   }-*/;
   
   public static String prettyFormatNumber(double number)
   {
      return PRETTY_NUMBER_FORMAT.format(number);
   }

   public static String formatGeneralNumber(long number)
   {
      String val = number + "";
      if (val.length() < 5 || (number < 0 && val.length() < 6))
         return val;
      return NumberFormat.getFormat("0,000").format(number);
   }
   
   public static String formatPercent(double number)
   {
      return NumberFormat.getPercentFormat().format(number);
   }
   
   public static Size characterExtent(String text)
   {
      // split into lines and find the maximum line width
      String[] lines = text.split("\n");
      int maxWidth = 0;
      for (int i=0; i<lines.length; i++)
      {
         int width = lines[i].length();
         if (width > maxWidth)
            maxWidth = width;
      }
      
      return new Size(maxWidth, lines.length);
   }

   public static String chomp(String string)
   {
      if (string.endsWith("\n"))
         return string.substring(0, string.length()-1);
      return string;
   }

   public static boolean isNullOrEmpty(String val)
   {
      return val == null || val.length() == 0;
   }

   // WARNING: I'm pretty sure this will fail for UTF-8
   public static String textToRLiteral(String value)
   {
      StringBuffer sb = new StringBuffer();
      sb.append('"');

      for (char c : value.toCharArray())
      {
         switch (c)
         {
            case '"':
               sb.append("\\\"");
               break;
            case '\n':
               sb.append("\\n");
               break;
            case '\r':
               sb.append("\\r");
               break;
            case '\t':
               sb.append("\\t");
               break;
            case '\b':
               sb.append("\\b");
               break;
            case '\f':
               sb.append("\\f");
               break;
            case '\\':
               sb.append("\\\\");
               break;
            default:
               if (c < 32 || c > 126)
                  sb.append("\\x").append(toHex(c));
               else
                  sb.append(c);
               break;
         }
      }

      sb.append('"');
      
      return sb.toString();
   }

   private static String toHex(char c)
   {
      String table = "0123456789ABCDEF";
      return table.charAt((c >> 8) & 0xF) + "" + table.charAt(c & 0xF);
   }

   public static String toRSymbolName(String name)
   {
      if (!RegexUtil.isSyntacticRIdentifier(name) || isRKeyword(name))
         return "`" + name + "`";
      else
         return name;
   }

   private static boolean isRKeyword(String identifier)
   {
      String ALL_KEYWORDS = "|NULL|NA|TRUE|FALSE|T|F|Inf|NaN|NA_integer_|NA_real_|NA_character_|NA_complex_|function|while|repeat|for|if|in|else|next|break|...|";

      if (identifier.length() > 20 || identifier.contains("|"))
         return false;

      return ALL_KEYWORDS.indexOf("|" + identifier + "|") >= 0;
   }

   public static String notNull(String s)
   {
      return s == null ? "" : s;
   }

   public static String indent(String str, String indent)
   {
      if (isNullOrEmpty(str))
         return str;

      return indent + str.replaceAll("\n", "\n" + indent);
   }
   
   public static String join(String delimiter, String... strings)
   {
      return join(strings, delimiter);
   }
   
   public static String join(String[] collection, String delim)
   {
      return join(Arrays.asList(collection), delim);
   }
   
   public static String join(Collection<?> collection,
                             String delim)
   {
      String currDelim = "";
      StringBuilder output = new StringBuilder();
      for (Object el : collection)
      {
         output.append(currDelim).append(el == null ? "" : el.toString());
         currDelim = delim;
      }
      return output.toString();
   }

   public static String firstNotNullOrEmpty(String[] strings)
   {
      for (String s : strings)
         if (!isNullOrEmpty(s))
            return s;
      return null;
   }
   
   public static String shortPathName(FileSystemItem item, int maxWidth)
   {
      return shortPathName(item, "gwt-Label", maxWidth);
   }
   
   public static String shortPathName(FileSystemItem item,
                                      String styleName,
                                      int maxWidth)
   {
      // measure HTML and truncate if necessary
      String path = item.getPath();
      Size textSize = DomMetrics.measureHTML(path, styleName);
      if (textSize.width >= maxWidth)
      {
         // shortened directory nam
         if (item.getParentPath() != null &&
               item.getParentPath().getParentPath() != null)
         {
            path = ".../" + 
            item.getParentPath().getName() + "/" +
            item.getName(); 
         }
      }
      return path;
   }

   public static Iterable<String> getLineIterator(final String text)
   {
      return new Iterable<String>()
      {
         @Override
         public Iterator<String> iterator()
         {
            return new Iterator<String>()
            {
               private int pos = 0;
               private Pattern newline = Pattern.create("\\r?\\n");

               @Override
               public boolean hasNext()
               {
                  return pos < text.length();
               }

               @Override
               public String next()
               {
                  if (pos >= text.length())
                     return null;

                  Match match = newline.match(text, pos);
                  String result;
                  if (match == null)
                  {
                     result = text.substring(pos);
                     pos = text.length();
                  }
                  else
                  {
                     result = text.substring(pos, match.getIndex());
                     pos = match.getIndex() + match.getValue().length();
                  }
                  return result;
               }

               @Override
               public void remove()
               {
                  throw new UnsupportedOperationException();
               }
            };
         }
      };
   }

   /**
    * Removes empty or whitespace-only lines from the beginning and end of the
    * string.
    */
   public static String trimBlankLines(String data)
   {
      data = Pattern.create("^[\\r\\n\\t ]*\\n", "g").replaceAll(data, "");
      data = Pattern.create("\\r?\\n[\\r\\n\\t ]*$", "g").replaceAll(data, "");
      return data;
   }

   public static String trimLeft(String str)
   {
      return str.replaceFirst("^\\s+", "");
   }

   public static String trimRight(String str)
   {
      return str.replaceFirst("\\s+$", "");
   }

   /**
    * Returns the zero or more characters that prefix all of the lines (but see
    * allowPhantomWhitespace).
    * @param lines The lines from which to find a common prefix.
    * @param allowPhantomWhitespace See comment in function body
    * @return
    */
   public static String getCommonPrefix(String[] lines,
                                        boolean allowPhantomWhitespace,
                                        boolean skipWhitespaceOnlyLines)
   {
      if (lines.length == 0)
         return "";

      /**
       * allowPhantomWhitespace demands some explanation. Assuming these lines:
       *
       * {
       *    "#",
       *    "#  hello",
       *    "#",
       *    "#  goodbye",
       *    "#    hello again"
       * }
       *
       * The result with allowPhantomWhitespace = false would be "#", but with
       * allowPhantomWhiteSpace = true it would be "#  ". Basically phantom
       * whitespace refers to spots at the end of a line where additional
       * whitespace would lead to a longer overall prefix but would not change
       * the visible appearance of the document.
       */


      String prefix = notNull(lines[0]);

      // Usually the prefix gradually gets shorter and shorter.
      // whitespaceExpansionAllowed means that the prefix might get longer,
      // because the prefix as it stands is eligible for phantom whitespace
      // insertion. This is true iff the prefix is the same length as, or longer
      // than, all of the lines we have processed.
      boolean whitespaceExpansionAllowed = allowPhantomWhitespace;

      for (int i = 1; i < lines.length && prefix.length() > 0; i++)
      {
         String line = notNull(lines[i]);
         if (line.trim().isEmpty() && skipWhitespaceOnlyLines)
            continue;
         
         int len = whitespaceExpansionAllowed ? Math.max(prefix.length(), line.length()) :
                   allowPhantomWhitespace ? prefix.length() :
                   Math.min(prefix.length(), line.length());
         int j;
         for (j = 0; j < len; j++)
         {
            if (j >= prefix.length())
            {
               assert whitespaceExpansionAllowed;
               if (!isWhitespace(line.charAt(j)))
                  break;
               continue;
            }

            if (j >= line.length())
            {
               assert allowPhantomWhitespace;
               if (!isWhitespace(prefix.charAt(j)))
                  break;
               continue;
            }

            if (prefix.charAt(j) != line.charAt(j))
            {
               break;
            }
         }

         prefix = j <= prefix.length() ? prefix.substring(0, j)
                                       : line.substring(0, j);

         whitespaceExpansionAllowed =
               whitespaceExpansionAllowed && (prefix.length() >= line.length());
      }

      return prefix;
   }

   private static boolean isWhitespace(char c)
   {
      switch (c)
      {
         case ' ':
         case '\t':
         case '\u00A0':
         case '\r':
         case '\n':
            return true;
         default:
            return false;
      }
   }

   public static String pathToTitle(String path)
   {
      String val = FileSystemItem.createFile(path).getStem();
      val = Pattern.create("\\b[a-z]").replaceAll(val, new ReplaceOperation()
      {
         @Override
         public String replace(Match m)
         {
            return m.getValue().toUpperCase();
         }
      });
      val = Pattern.create("[-_]").replaceAll(val, " ");
      return val;
   }
   
   public static String joinStrings(List<String> strings, String separator)
   {
      String result = "";
      // GWT's exposed Strings.join often makes the compiler barf; do this 
      // manually 
      for (int i = 0; i < strings.size(); i++)
      {
         result += strings.get(i);
         if (i < strings.size() - 1)
            result += separator;
      }
      return result;
   }
   
   // Given an input URL which may be relative, return an absolute URL. Has
   // no effect on URLs which are already absolute.
   public static String makeAbsoluteUrl(String inputUrl)
   {
      String url = inputUrl;
      if (!(url.startsWith("http://") || url.startsWith("https://")))
      {
         String thisUrl = Window.Location.getProtocol() + "//" +
                          Window.Location.getHost() + "/";
         if (Window.Location.getPath().length() > 0 &&
             !Window.Location.getPath().equals("/"))
            thisUrl += Window.Location.getPath();
         if (!thisUrl.endsWith("/"))
            thisUrl += "/";
         if (url.startsWith("/"))
            url = url.substring(1);
         url = thisUrl + url;
      }
      return url;
      
   }
   
   public static String ensureSurroundedWith(String string, char chr)
   {
      if (isNullOrEmpty(string))
         return "" + chr + chr;
      String result = string;
      if (result.charAt(0) != chr)
         result = chr + result;
      if (result.charAt(result.length() - 1) != chr)
         result += chr;
      return result;
   }
   
   public static String capitalize(String input)
   {
      if (input == null || input.length() < 1)
         return input;
      return input.substring(0, 1).toUpperCase() + input.substring(1); 
   }
   
   public static final native String capitalizeAllWords(String input)
   /*-{
      return input.replace(
         /(?:^|\s)\S/g,
         function(x) { return x.toUpperCase(); }
      );
   }-*/;
   
   public static int countMatches(String line, char chr)
   {
      return line.length() - line.replace(String.valueOf(chr), "").length();
   }
   
   public static String stripRComment(String string)
   {
      boolean inSingleQuotes = false;
      boolean inDoubleQuotes = false;
      boolean inQuotes = false;
      
      char currentChar = '\0';
      char previousChar = '\0';
      
      int commentIndex = string.length();
      
      for (int i = 0; i < string.length(); i++)
      {
         currentChar = string.charAt(i);
         inQuotes = inSingleQuotes || inDoubleQuotes;
         
         if (i > 0)
         {
            previousChar = string.charAt(i - 1);
         }
         
         if (currentChar == '#' && !inQuotes)
         {
            commentIndex = i;
            break;
         }
         
         if (currentChar == '\'' && !inQuotes)
         {
            inSingleQuotes = true;
            continue;
         }
         
         if (currentChar == '\'' && previousChar != '\\' && inSingleQuotes)
         {
            inSingleQuotes = false;
            continue;
         }
         
         if (currentChar == '"' && !inQuotes)
         {
            inDoubleQuotes = true;
            continue;
         }
         
         if (currentChar == '"' && previousChar != '\\' && inDoubleQuotes)
         {
            inDoubleQuotes = false;
            continue;
         }
      }
      return string.substring(0, commentIndex);
   }
   
   public static String stripBalancedQuotes(String string)
   {
      if (string == null)
         return null;
      
      if (string == "")
         return "";
      
      boolean inSingleQuotes = false;
      boolean inDoubleQuotes = false;
      boolean inQuotes = false;

      int stringStart = 0;

      char currentChar = '\0';
      char previousChar = '\0';

      StringBuilder result = new StringBuilder();

      for (int i = 0; i < string.length(); i++)
      {
         currentChar = string.charAt(i);
         inQuotes = inSingleQuotes || inDoubleQuotes;

         if (i > 0)
         {
            previousChar = string.charAt(i - 1);
         }

         if (currentChar == '\'' && !inQuotes)
         {
            inSingleQuotes = true;
            result.append(string.substring(stringStart, i));
            continue;
         }

         if (currentChar == '\'' && previousChar != '\\' && inSingleQuotes)
         {
            inSingleQuotes = false;
            stringStart = i + 1;
            continue;
         }

         if (currentChar == '"' && !inQuotes)
         {
            inDoubleQuotes = true;
            result.append(string.substring(stringStart, i));
            continue;
         }

         if (currentChar == '"' && previousChar != '\\' && inDoubleQuotes)
         {
            inDoubleQuotes = false;
            stringStart = i + 1;
            continue;
         }
      }
      result.append(string.substring(stringStart, string.length()));
      return result.toString();
   }
   
   public static String maskStrings(String string)
   {
      return maskStrings(string, 'x');
   }
   
   public static String maskStrings(String string,
                                    char ch)
   {
      if (string == null)
         return null;
      
      if (string.length() == 0)
         return "";
      
      boolean inSingleQuotes = false;
      boolean inDoubleQuotes = false;
      boolean inQuotes = false;

      char currentChar = '\0';
      char previousChar = '\0';

      StringBuilder result = new StringBuilder();

      for (int i = 0; i < string.length(); i++)
      {
         currentChar = string.charAt(i);
         inQuotes = inSingleQuotes || inDoubleQuotes;
         
         if (i > 0)
         {
            previousChar = string.charAt(i - 1);
         }

         if (currentChar == '\'' && !inQuotes)
         {
            inSingleQuotes = true;
            result.append(currentChar);
            continue;
         }
         else if (currentChar == '\'' && previousChar != '\\' && inSingleQuotes)
         {
            inSingleQuotes = false;
            result.append(currentChar);
            continue;
         }
         else if (currentChar == '"' && !inQuotes)
         {
            inDoubleQuotes = true;
            result.append(currentChar);
            continue;
         }
         else if (currentChar == '"' && previousChar != '\\' && inDoubleQuotes)
         {
            inDoubleQuotes = false;
            result.append(currentChar);
            continue;
         }
         
         if (inSingleQuotes || inDoubleQuotes)
            result.append(ch);
         else
            result.append(currentChar);
         
      }
      
      return result.toString();
   }
   
   
   public static boolean isEndOfLineInRStringState(String string)
   {
      if (string == null)
         return false;
      
      if (string == "")
         return false;
      
      boolean inSingleQuotes = false;
      boolean inDoubleQuotes = false;
      boolean inQuotes = false;

      char currentChar = '\0';
      char previousChar = '\0';

      for (int i = 0; i < string.length(); i++)
      {
         currentChar = string.charAt(i);
         inQuotes = inSingleQuotes || inDoubleQuotes;

         if (i > 0)
         {
            previousChar = string.charAt(i - 1);
         }
         
         if (currentChar == '#' && !inQuotes)
         {
            return false;
         }

         if (currentChar == '\'' && !inQuotes)
         {
            inSingleQuotes = true;
            continue;
         }

         if (currentChar == '\'' && previousChar != '\\' && inSingleQuotes)
         {
            inSingleQuotes = false;
            continue;
         }

         if (currentChar == '"' && !inQuotes)
         {
            inDoubleQuotes = true;
            continue;
         }

         if (currentChar == '"' && previousChar != '\\' && inDoubleQuotes)
         {
            inDoubleQuotes = false;
            continue;
         }
      }
      
      return inSingleQuotes || inDoubleQuotes;
   }
   
   public static boolean isSubsequence(String self,
         String other,
         boolean caseInsensitive)
   {
      return caseInsensitive ?
            isSubsequence(self.toLowerCase(), other.toLowerCase()) :
            isSubsequence(self, other)
      ;
   }
   
   public static boolean isSubsequence(String self,
         String other)
   {

      final int self_n = self.length();
      final int other_n = other.length();

      if (other_n > self_n)
         return false;

      int self_idx = 0;
      int other_idx = 0;
         
      while (self_idx < self_n)
      {
         char selfChar = self.charAt(self_idx);
         char otherChar = other.charAt(other_idx);
         
         if (otherChar == selfChar)
         {
            ++other_idx;
            if (other_idx == other_n)
            {
               return true;
            }
         }
         ++self_idx;
      }
      return false;
   }
   
   public static List<Integer> subsequenceIndices(String sequence, String query)
   {
      List<Integer> result = new ArrayList<Integer>();
      int querySize = query.length();
      
      int prevMatchIndex = -1;
      for (int i = 0; i < querySize; i++)
      {
         int index = sequence.indexOf(query.charAt(i), prevMatchIndex + 1);
         if (index == -1)
            continue;
         
         result.add(index);
         prevMatchIndex = index;
      }
      
      return result;
   }
   
   public static String getExtension(String string, int dots)
   {
      assert dots > 0;
      int lastDotIndex = -1;
      
      if (dots == 1)
      {
         lastDotIndex = string.lastIndexOf('.');
      }
      else
      {
         String reversed = new StringBuilder(string).reverse().toString();
         for (int i = 0; i < dots; i++)
         {
            lastDotIndex = reversed.indexOf('.', lastDotIndex);
         }
         lastDotIndex = string.length() - lastDotIndex;
      }
      
      return lastDotIndex == -1 || lastDotIndex == string.length() - 1 ?
            "" :
            string.substring(lastDotIndex + 1, string.length());
   }
   
   public static String getExtension(String string)
   {
      return getExtension(string, 1);
   }
   
   public static String getToken(String string,
                                 int pos,
                                 String tokenRegex,
                                 boolean expandForward,
                                 boolean backOverWhitespace)
   {
      if (backOverWhitespace)
         while (pos > 0 && string.substring(pos - 1, pos).matches("\\s"))
            --pos;
      
      int startPos = Math.max(0, pos - 1);
      int endPos = Math.min(pos, string.length());
      
      while (startPos >= 0 &&
            string.substring(startPos, startPos + 1).matches(tokenRegex))
         --startPos;
      
      if (expandForward)
         while (endPos < string.length() &&
               string.substring(endPos, endPos + 1).matches(tokenRegex))
            ++endPos;
      
      if (startPos >= endPos)
         return "";
      
      return string.substring(startPos + 1, endPos);
   }
   
   public static String repeat(String string, int times)
   {
      StringBuilder builder = new StringBuilder();
      for (int i = 0; i < times; i++)
         builder.append(string);
      return builder.toString();
   }
   
   public static ArrayList<Integer> indicesOf(String string, char ch)
   {
      ArrayList<Integer> indices = new ArrayList<Integer>();
      
      int matchIndex = string.indexOf(ch);
      while (matchIndex != -1)
      {
         indices.add(matchIndex);
         matchIndex = string.indexOf(ch, matchIndex + 1);
      }
      return indices;
   }
   
   @SuppressWarnings("deprecation") // GWT emulation only provides isSpace
   public static boolean isWhitespace(String string)
   {
      for (int i = 0; i < string.length(); i++)
         if (!Character.isSpace(string.charAt(i)))
            return false;
      return true;
   }
   
   private static final String[] LABELS = {
         "B",
         "KB",
         "MB",
         "GB",
         "TB"
   };
   
   public static boolean isComplementOf(String self, String other)
   {
      return COMPLEMENTS.get(self).equals(other);
   }
   
   private static final HashMap<String, String> makeComplementsMap()
   {
      HashMap<String, String> map = new HashMap<String, String>();
      
      map.put("[", "]");
      map.put("]", "[");
      
      map.put("<", ">");
      map.put(">", "<");
      
      map.put("{", "}");
      map.put("}", "{");
      
      map.put("(", ")");
      map.put(")", "(");
      return map;
   }

   public static String collapse(Map<String, String> map,
                                 String keyValueSeparator,
                                 String fieldSeparator)
   {
      StringBuilder builder = new StringBuilder();
      int count = 0;
      for (Map.Entry<String, String> cursor : map.entrySet())
      {
         if (count != 0) builder.append(fieldSeparator);
         builder.append(cursor.getKey());
         builder.append(keyValueSeparator);
         builder.append(cursor.getValue());
         count++;
      }
      return builder.toString();
   }
   
   public static String prettyCamel(String string)
   {
      if (isNullOrEmpty(string))
         return string;
      
      if (string.equals(string.toUpperCase()))
         return string;
      
      String result = string.replaceAll("\\s*([A-Z])", " $1");
      return result.substring(0, 1).toUpperCase() +
             result.substring(1);
   }
   
   public static native final String escapeRegex(String regexString) /*-{
      var utils = $wnd.require("mode/utils");
      return utils.escapeRegExp(regexString);
   }-*/;
   
   public static final String getIndent(String line)
   {
      return RE_INDENT.match(line, 0).getGroup(0);
   }
   
   public static final String truncate(String string, int targetLength, String suffix)
   {
      if (string.length() <= targetLength)
         return string;
      
      int truncatedSize = targetLength - suffix.length();
      if (truncatedSize < 0)
         return string;
      
      return string.substring(0, truncatedSize) + suffix;
   }
   
   public static boolean isOneOf(String string, String... candidates)
   {
      for (String candidate : candidates)
         if (candidate.equals(string))
            return true;
      return false;
   }
   
   public static boolean isOneOf(char ch, char... candidates)
   {
      for (char candidate : candidates)
         if (ch == candidate)
            return true;
      return false;
   }

   public static final String makeRandomId(int length) 
   {
      String alphanum = "0123456789abcdefghijklmnopqrstuvwxyz";
      String id = "";
      for (int i = 0; i < length; i++)
      {
         id += alphanum.charAt((int)(Math.random() * alphanum.length()));
      }
      return id;
   }
   
   public static String ensureQuoted(String string)
   {
      String[] quotes = new String[] { "\"", "'", "`" };
      for (String quote : quotes)
         if (string.startsWith(quote) && string.endsWith(quote))
            return string;
      
      return "\"" + string.replaceAll("\"", "\\\\\"") + "\"";
   }
   
   public static String stringValue(String string)
   {
      String[] quotes = new String[] { "\"", "'", "`" };
      for (String quote : quotes)
      {
         if (string.startsWith(quote) && string.endsWith(quote))
         {
            String substring = string.substring(1, string.length() - 1);
            return substring.replaceAll("\\\\" + quote, quote);
         }
      }
      
      return string;
   }
   
   public static final native String encodeURI(String string) /*-{
      return $wnd.encodeURI(string);
   }-*/;
   
   public static final native String encodeURIComponent(String string) /*-{
      return $wnd.encodeURIComponent(string);
   }-*/;
   
   
   public static final native String normalizeNewLines(String string) /*-{
      return string.replace(/\r\n|\n\r|\r/g, "\n");
   }-*/;
   
   public static final native JsArrayString split(String string, String delimiter) /*-{
      return string.split(delimiter);
   }-*/;
   
   public static final HashMap<String, String> COMPLEMENTS =
         makeComplementsMap();
   
   /**
    * Computes a 32-bit CRC checksum from an arbitrary string.
    * 
    * @param str The string on which to compute the checksum
    * @return The checksum value, as a hexadecimal string
    */
   public static final native String crc32(String str)/*-{
      // based on: https://stackoverflow.com/questions/18638900/javascript-crc32
      var genCrc32Table = function() 
      {
         var c, crcTable = [];
         for (var n = 0; n < 256; n++) 
         {
            c = n;
            for (var k = 0; k < 8; k++)
            {
                c = ((c&1) ? (0xEDB88320 ^ (c >>> 1)) : (c >>> 1));
            }
            crcTable[n] = c;
         }
         return crcTable;
      }

      var crcTable = $wnd.rs_crc32Table || ($wnd.rs_crc32Table = genCrc32Table());
      var crc = 0 ^ (-1);

      for (var i = 0; i < str.length; i++ ) 
      {
         crc = (crc >>> 8) ^ crcTable[(crc ^ str.charCodeAt(i)) & 0xFF];
      }

      return ((crc ^ (-1)) >>> 0).toString(16);
   }-*/;
   
   private static final NumberFormat FORMAT = NumberFormat.getFormat("0.#");
   private static final NumberFormat PRETTY_NUMBER_FORMAT = NumberFormat.getFormat("#,##0.#####");
   private static final DateTimeFormat DATE_FORMAT
                          = DateTimeFormat.getFormat("MMM d, yyyy, h:mm a");
   private static final Pattern RE_INDENT = Pattern.create("^\\s*", "");

}
