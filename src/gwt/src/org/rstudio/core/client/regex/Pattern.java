/*
 * Pattern.java
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
package org.rstudio.core.client.regex;

import com.google.gwt.core.client.JavaScriptObject;

public class Pattern extends JavaScriptObject
{
   public interface ReplaceOperation
   {
      String replace(Match m);
   }

   protected Pattern() {}
   
   public static native Pattern create(String pattern) /*-{
      return new RegExp(pattern, 'gm');
   }-*/;
   
   public static native Pattern create(String pattern, String flags) /*-{
      return new RegExp(pattern, flags);
   }-*/;
   
   public final native int search(String string) /*-{
      return string.search(this);
   }-*/;

   public final native Match match(String input, int index) /*-{
      this.lastIndex = index;
      var result = this.exec(input);
      if (result == null)
         return null;
      return {
         value: result[0],
         index: result.index,
         input: input,
         next: this.lastIndex,
         pattern: this,
         match: result
      };
   }-*/;
   
   public final native boolean test(String input) /*-{
      return this.test(input);
   }-*/;

   public static String escape(String str)
   {
      // Replace every character with its \\uXXXX equivalent

      StringBuilder output = new StringBuilder();
      for (int i = 0; i < str.length(); i++)
      {
         char c = str.charAt(i);
         String hexStr = Integer.toHexString(c);
         output.append("\\u");
         for (int j = 4 - hexStr.length(); j > 0; j--)
            output.append('0');
         output.append(hexStr);
      }
      return output.toString();
   }

   public native final String replaceAll(String str, String substr) /*-{
      return str.replace(this, substr);
   }-*/;

   public final String replaceAll(String str, ReplaceOperation op)
   {
      StringBuilder result = new StringBuilder();
      int tail = 0; // Index of last character copied/replaced from source str
      Match match = match(str, 0);
      while (match != null)
      {
         if (tail < match.getIndex())
            result.append(str, tail, match.getIndex());

         result.append(op.replace(match));

         tail = match.getIndex() + match.getValue().length();

         match = match.nextMatch();
      }

      if (tail < str.length())
         result.append(str, tail, str.length());

      return result.toString();
   }

   public static native String replace(String find,
                                       String repl,
                                       boolean caseSensitive) /*-{
      return find.replace(
            new RegExp(find, caseSensitive ? "g" : "ig"),
            repl);
   }-*/;
}
