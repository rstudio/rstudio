/*
 * ChunkHeaderParser.java
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
package org.rstudio.studio.client.workbench.views.vcs.common.diff;

import java.util.ArrayList;

class ChunkHeaderParser
{
   public ChunkHeaderParser(String s)
   {
      s_ = s;
      p_ = 0;
   }

   public ChunkHeaderInfo parse()
   {
      // Example: @@ -6,4 +10,5 @@
      // Example: @@ -6 +10,1 @@
      // Example: @@@ -6,4 -6,3 +10,5 @@@
      int atCount = 0;
      while (matchChar('@'))
         atCount++;

      if (atCount < 2)
         return null;

      // match atCount many ranges
      ArrayList<Range> ranges = new ArrayList<Range>(atCount);
      for (int i = 0; i < atCount; i++)
      {
         matchWhitespace();

         char prefix = (i < atCount-1) ? '-' : '+';
         Integer start, count;

         if (!matchChar(prefix)
             || null == (start = matchNumber()))
         {
            return null;
         }


         if (matchChar(','))
         {
            if (null == (count = matchNumber()))
               return null;
         }
         else
         {
            count = 1;
         }

         ranges.add(new Range(start, count));
      }

      matchWhitespace();

      for (int i = 0; i < atCount; i++)
      {
         if (!matchChar('@'))
            return null;
      }

      String extraInfo = s_.substring(p_);

      return new ChunkHeaderInfo(ranges.toArray(new Range[ranges.size()]),
                                 extraInfo);
   }

   Integer matchNumber()
   {
      StringBuilder num = new StringBuilder();
      while (true)
      {
         int i = peek();
         if (i >= '0' && i <= '9')
         {
            num.append((char)i);
            p_++;
         }
         else
         {
            break;
         }
      }

      if (num.length() == 0)
         return null;

      return Integer.parseInt(num.toString());
   }

   boolean matchChar(char c)
   {
      if (peek() == c)
      {
         p_++;
         return true;
      }

      return false;
   }

   boolean matchWhitespace()
   {
      boolean sawWhitespace = false;
      while (true)
      {
         switch (peek())
         {
            case ' ':
            case '\t':
               p_++;
               sawWhitespace = true;
               break;
            default:
               return sawWhitespace;
         }
      }
   }

   int peek()
   {
      if (p_ >= s_.length())
         return -1;
      return s_.charAt(p_);
   }

   final String s_;
   int p_;
}
