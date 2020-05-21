/*
 * TexMagicComment.java
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
package org.rstudio.core.client.tex;

import java.util.ArrayList;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;

public class TexMagicComment
{
   public static ArrayList<TexMagicComment> parseComments(String code)
   {
      ArrayList<TexMagicComment> comments = new ArrayList<TexMagicComment>();
      
      Iterable<String> lines = StringUtil.getLineIterator(code);
      
      for (String line : lines)
      {
         line = line.trim();
         if (line.length() == 0)
         {
            continue;
         }
         else if (line.startsWith("%"))
         {
            
            Match match = magicCommentPattern_.match(line, 0);
            if (match != null)
            {
               if (match.hasGroup(1) && match.hasGroup(2) && match.hasGroup(3))
               {
                  comments.add(new TexMagicComment(match.getGroup(1),
                                                   match.getGroup(2),
                                                   match.getGroup(3)));
               
               }
            }
            
         }
         else
         {
            break;
         }
      }
    
      return comments;
   }
   

   public TexMagicComment(String scope, String variable, String value)
   {
      scope_ = scope;
      variable_ = variable;
      value_ = value;
   }
   
   public String getScope() { return scope_; }
   public String getVariable() { return variable_; }
   public String getValue() { return value_; }
   
  
   private final String scope_;
   private final String variable_;
   private final String value_;
   
   private static final Pattern magicCommentPattern_ = 
         Pattern.create("%{1,2}\\s*!(\\w+)\\s+(\\w+)\\s*=\\s*(.*)$");
}
