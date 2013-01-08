/*
 * RLiteralConversionStrategy.java
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
package org.rstudio.studio.client.workbench.views.workspace.table;

import org.rstudio.studio.client.common.r.RStringToken;
import org.rstudio.studio.client.common.r.RToken;
import org.rstudio.studio.client.common.r.RTokenizer;

import java.util.HashSet;

public class RLiteralConversionStrategy implements ScalarConversionStrategy<String>
{
   public boolean allowEnter()
   {
      return false ;
   }

   public String convertToDisplayString(String value)
   {
      return value ;
   }

   public String convertToEditString(String value)
   {
      return value ;
   }

   public String convertToValue(String text)
   {
      text = text.trim() ;
      
      RTokenizer tokenizer = new RTokenizer(text) ;
      RToken t ;
      if (null != (t = tokenizer.nextToken()))
      {
         switch (t.getTokenType())
         {
         case RToken.NUMBER:
            break ;
         case RToken.STRING:
            if (!((RStringToken)t).isWellFormed())
               throw new IllegalArgumentException("Invalid string expression") ;
            break ;
         case RToken.ID:
            if (keywords.contains(t.getContent()))
               break ;
            throw new IllegalArgumentException("Illegal expression") ;
         default:
            throw new IllegalArgumentException(
               "Invalid expression--only numeric and string literals allowed") ;
         }
      }

      if (t == null)
         return null ;

      RToken t2 = tokenizer.nextToken();
      if (t2 != null)
      {
         if (t.getTokenType() == RToken.NUMBER || !t.getContent().endsWith("i"))
         {
            if (t2.getTokenType() == RToken.OPER
                  && (t2.getContent().equals("+") || t2.getContent().equals("-")))
            {
               RToken t3 = tokenizer.nextToken();
               if (t3 != null && t3.getTokenType() == RToken.NUMBER
                     && t3.getContent().endsWith("i"))
               {
                  if (tokenizer.nextToken() == null)
                  {
                     // OK, it's an imaginary number of some type
                     return text;
                  }
               }
            }
         }

         throw new IllegalArgumentException(
               "Invalid expression--only numeric and string literals allowed") ;
      }
      
      return text;
   }
   
   private static HashSet<String> keywords = new HashSet<String>() ;
   static {
      keywords.add("T") ;
      keywords.add("F") ;
      keywords.add("TRUE") ;
      keywords.add("FALSE") ;
      keywords.add("NULL") ;
      keywords.add("Inf") ;
      keywords.add("NaN") ;
      keywords.add("NA") ;
      keywords.add("NA_integer_") ;
      keywords.add("NA_real_") ;
      keywords.add("NA_complex_") ;
      keywords.add("NA_character_") ;
   }
}
