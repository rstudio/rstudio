/*
 * HTMLAttributesParser.java
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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
package org.rstudio.core.client.html;

import java.util.HashMap;
import java.util.Map;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;

import com.google.gwt.user.client.Command;

public class HTMLAttributesParser
{
   private static class Parser
   {
      public Parser(String attributes)
      {
         attributes_ = StringUtil.notNull(attributes).trim();
         map_ = new HashMap<String, String>();
         
         index_ = 0;
         currentKey_ = "";
         currentValue_ = "";
      }
      
      public boolean consumeKey()
      {
         final int index = index_;
         return consumeUntilRegex("[\\s=]", new Command()
         {
            @Override
            public void execute()
            {
               currentKey_ = attributes_.substring(index, index_);
               map_.put(currentKey_, null);
            }
         });
      }
      
      public boolean consumeWhitespace()
      {
         return consumeUntilRegex("\\S");
      }
      
      public boolean consumeEquals()
      {
         return consumeUntilRegex("[^\\s=]");
      }
      
      public boolean consumeValue()
      {
         final int index = index_;
         char ch = attributes_.charAt(index_);
         if (ch == '\'' || ch == '"')
         {
            return consumeQuotedValue(ch, new Command()
            {
               @Override
               public void execute()
               {
                  currentValue_ = attributes_.substring(index + 1, index_ - 1);
                  map_.put(currentKey_, currentValue_);
               }
            });
         }
         
         return consumeUntilRegex("(?:\\s|$)", new Command()
         {
            @Override
            public void execute()
            {
               currentValue_ = attributes_.substring(index, index_);
               map_.put(currentKey_, currentValue_);
            }
         });
      }
      
      public Map<String, String> parsedAttributes()
      {
         return map_;
      }
      
      private boolean consumeUntilRegex(String regex)
      {
         return consumeUntilRegex(regex, null);
      }
      
      private boolean consumeUntilRegex(String regex, Command command)
      {
         Pattern pattern = Pattern.create(regex);
         Match match = pattern.match(attributes_, index_);
         if (match == null)
            return false;
         
         index_ = match.getIndex();
         if (command != null)
            command.execute();
         
         return true;
      }
      
      private boolean consumeQuotedValue(char ch, Command command)
      {
         int index = attributes_.indexOf(ch, index_ + 1);
         if (index == -1)
            return false;
         
         while (attributes_.charAt(index - 1) == ch)
         {
            index = attributes_.indexOf(ch, index + 1);
            if (index == -1)
               return false;
         }
         
         index_ = index + 1;
         if (command != null)
            command.execute();
            
         return true;
      }
      
      private final String attributes_;
      private final Map<String, String> map_;
      
      private int index_;
      private String currentKey_;
      private String currentValue_;
   }
   
   public static Map<String, String> parseAttributes(String attributes)
   {
      Parser parser = new Parser(attributes);
      while (true)
      {
         parser.consumeWhitespace();
         
         if (!parser.consumeKey())
            break;
         
         parser.consumeWhitespace();
         
         if (!parser.consumeEquals())
            break;
         
         parser.consumeWhitespace();
         
         if (!parser.consumeValue())
            break;
      }
      
      return parser.parsedAttributes();
   }
}
