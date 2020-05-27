/*
 * HTMLAttributesParser.java
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
package org.rstudio.core.client.html;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;

import com.google.gwt.user.client.Command;

public class HTMLAttributesParser
{
   public static class Attributes
   {
      public Attributes(String identifier,
                        List<String> classes,
                        Map<String, String> attributes)
      {
         identifier_ = identifier;
         classes_ = classes;
         attributes_ = attributes;
      }
      
      public String getIdentifier()
      {
         return identifier_;
      }
      
      public List<String> getClasses()
      {
         return classes_;
      }
      
      public Map<String, String> getAttributes()
      {
         return attributes_;
      }
      
      private String identifier_;
      private List<String> classes_;
      private Map<String, String> attributes_;
   }
   
   private static class Parser
   {
      public Parser(String attributes)
      {
         attributes_ = StringUtil.notNull(attributes).trim();
         map_ = new HashMap<String, String>();
         identifier_ = "";
         classes_ = new ArrayList<String>();
         
         index_ = 0;
         n_ = attributes.length();
         currentKey_ = "";
         currentValue_ = "";
      }
      
      public boolean consumeIdentifier()
      {
         char ch = attributes_.charAt(index_);
         if (ch != '#')
            return false;
         
         int index = index_;
         return consumeUntilRegex("(?:\\s|$)", new Command()
         {
            @Override
            public void execute()
            {
               identifier_ = attributes_.substring(index + 1, index_);
            }
         });
      }
      
      public boolean consumeClass()
      {
         char ch = attributes_.charAt(index_);
         if (ch != '.')
            return false;
         
         int index = index_;
         return consumeUntilRegex("(?:\\s|$)", new Command()
         {
            @Override
            public void execute()
            {
               classes_.add(attributes_.substring(index + 1, index_));
            }
         });
      }
      
      public boolean consumeAttribute()
      {
         if (!consumeKey())
            return false;
         
         if (!consumeEquals())
            return false;
         
         if (!consumeValue())
            return false;
         
         return true;
      }
      
      public boolean finished()
      {
         return index_ >= n_;
      }
      
      private boolean consumeKey()
      {
         final int index = index_;
         return consumeUntilRegex("[=]", new Command()
         {
            @Override
            public void execute()
            {
               currentKey_ = attributes_.substring(index, index_);
               map_.put(currentKey_, null);
            }
         });
      }
      
      private boolean consumeWhitespace()
      {
         return consumeUntilRegex("(?:\\S|$)");
      }
      
      private boolean consumeEquals()
      {
         char ch = attributes_.charAt(index_);
         if (ch != '=')
            return false;
         
         index_++;
         return true;
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
                  if (isValidKey(currentKey_))
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
               if (isValidKey(currentKey_))
                  map_.put(currentKey_, currentValue_);
            }
         });
      }
      
      public Attributes getAttributes()
      {
         return new Attributes(identifier_, classes_, map_);
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
      
      private static boolean isValidKey(String key)
      {
         return RE_KEY.test(key);
      }
      
      private final String attributes_;
      private final Map<String, String> map_;
      private String identifier_;
      private List<String> classes_;
      
      private int index_;
      private int n_;
      private String currentKey_;
      private String currentValue_;
      
      private static final Pattern RE_KEY = Pattern.create("^[a-zA-Z][a-zA-Z0-9_.:-]*$", "");
   }
   
   public static Attributes parseAttributes(String attributes)
   {
      Parser parser = new Parser(attributes);
      while (!parser.finished())
      {
         parser.consumeWhitespace();
         
         if (parser.consumeIdentifier())
            continue;
         
         if (parser.consumeClass())
            continue;
         
         if (parser.consumeAttribute())
            continue;
         
         break;
      }
      
      return parser.getAttributes();
   }
}
