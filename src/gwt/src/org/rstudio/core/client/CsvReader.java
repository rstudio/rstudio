/*
 * CsvReader.java
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

import java.util.ArrayList;
import java.util.Iterator;

public class CsvReader implements Iterable<String[]>
{
   public CsvReader(String data)
   {
      data_ = data;
   }

   public Iterator<String[]> iterator()
   {
      return new Iterator<String[]>()
      {
         private int pos = 0;

         public boolean hasNext()
         {
            return pos < data_.length();
         }

         public String[] next()
         {
            ArrayList<String> list = new ArrayList<String>();
            StringBuilder chunk = new StringBuilder();

            final int START = 0;
            final int IN_UNQUOTED = 1;
            final int IN_QUOTE = 2;
            final int QUOTE_ENDED = 3;

            int state = START;
            for ( ; pos < data_.length(); pos++)
            {
               char c = data_.charAt(pos);

               if (c == '\n' && state != IN_QUOTE)
               {
                  pos++;
                  break;
               }
               if (c == ',' && state != IN_QUOTE)
               {
                  if (state != QUOTE_ENDED)
                     list.add(chunk.toString());
                  chunk = new StringBuilder();
                  state = START;
                  continue;
               }
               if (c == '"' && state == START)
               {
                  state = IN_QUOTE;
                  continue;
               }
               if (c == '"' && state == IN_QUOTE)
               {
                  int lookahead = (pos < data_.length() - 1)
                                  ? data_.charAt(pos+1)
                                  : -1;
                  if (lookahead == '"')
                  {
                     chunk.append((char)lookahead);
                     pos++;
                     continue;
                  }
                  list.add(chunk.toString());
                  chunk = new StringBuilder();
                  state = QUOTE_ENDED;
                  continue;
               }

               if (state == START)
                  state = IN_UNQUOTED;
               chunk.append(c);
            }

            if (state != QUOTE_ENDED)
            {
               list.add(chunk.toString());
            }

            return list.toArray(new String[0]);
         }

         public void remove()
         {
            throw new UnsupportedOperationException();
         }
      };
   }

   private final String data_;
}
