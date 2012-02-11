/*
 * StringUtil.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.client;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.NumberFormat;
import org.rstudio.core.client.dom.DomMetrics;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

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
      if (size < 1024)
      {
         if (size == 1)
            return size + " byte";
         else
            return size + " bytes";
      }

      int i = Arrays.binarySearch(SIZES, size);

      long divisor;
      String label;

      if (i >= 0)
      {
         divisor = SIZES[i];
         label = LABELS[i];
      }
      else
      {
         i = ~i;
         i--;
         divisor = SIZES[i];
         label = LABELS[i];
      }

      return FORMAT.format((double)size / divisor) + " " + label;
   }
   
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
      if (!name.matches("^[a-zA-Z_.][a-zA-Z0-9_.]*$")
          || name.matches("^.[0-9].*$")
          || isRKeyword(name))
      {
         return "`" + name + "`";
      }
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
                                        boolean allowPhantomWhitespace)
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

   private static final long[] SIZES = {
         1024L, // kilobyte
         1024L * 1024L, // megabyte
         1024L * 1024L * 1024L, // gigabyte
         1024L * 1024L * 1024L * 1024L, // terabyte
   };
   private static final String[] LABELS = {
         "KB",
         "MB",
         "GB",
         "TB"
   };
   private static final NumberFormat FORMAT = NumberFormat.getFormat("0.#");
   private static final NumberFormat PRETTY_NUMBER_FORMAT = NumberFormat.getFormat("#,##0.#####");
   private static final DateTimeFormat DATE_FORMAT
                          = DateTimeFormat.getFormat("MMM d, yyyy, h:mm a");

}
