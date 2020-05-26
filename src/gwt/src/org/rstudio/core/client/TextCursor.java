/*
 * TextCursor.java
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

import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;

public class TextCursor
{
   public TextCursor(String data, int index)
   {
      data_ = data;
      index_ = index;
      n_ = data_.length();
   }
   
   public TextCursor(String data)
   {
      this(data, 0);
   }
   
   public TextCursor clone()
   {
      return new TextCursor(data_, index_);
   }
   
   public boolean moveToNextCharacter()
   {
      if (index_ == n_)
         return false;
      
      index_++;
      return true;
   }
   
   public boolean moveToPreviousCharacter()
   {
      if (index_ == 0)
         return false;
      
      index_--;
      return true;
   }
   
   public boolean contentEquals(char ch)
   {
      if (index_ == n_)
         return false;
      return data_.charAt(index_) == ch;
   }
   
   public boolean fwdToMatchingCharacter()
   {
      if (index_ == n_)
         return false;
      
      char lhs = data_.charAt(index_);
      char rhs = complement(lhs);
      
      boolean checkEscapes =
            lhs == '\'' ||
            lhs == '"' ||
            lhs == '`';
            
      TextCursor cursor = clone();
      int braceCount = 0;
      while (cursor.moveToNextCharacter())
      {
         if (checkEscapes && cursor.contentEquals('\\'))
         {
            if (!cursor.moveToNextCharacter())
               break;
            
            continue;
         }
         
         if (cursor.contentEquals(rhs))
         {
            if (braceCount == 0)
            {
               index_ = cursor.getIndex();
               return true;
            }
            --braceCount;
         }
         else if (cursor.contentEquals(lhs))
            ++braceCount;
      }
      
      return false;
   }
   
   public boolean bwdToMatchingCharacter()
   {
      if (index_ == n_)
         return false;
      
      char lhs = data_.charAt(index_);
      char rhs = complement(lhs);
      
      TextCursor cursor = clone();
      int braceCount = 0;
      while (cursor.moveToPreviousCharacter())
      {
         if (cursor.contentEquals(rhs))
         {
            if (braceCount == 0)
            {
               index_ = cursor.getIndex();
               return true;
            }
            --braceCount;
         }
         else if (cursor.contentEquals(lhs))
            ++braceCount;
      }
      
      return false;
      
   }
   
   public boolean fwdToCharacter(char ch, boolean skipMatchingBraces)
   {
      if (skipMatchingBraces)
         return fwdToCharacter__skip(ch);
      else
         return fwdToCharacter__noskip(ch);
   }
   
   private boolean fwdToCharacter__skip(char ch)
   {
      TextCursor cursor = clone();
      do
      {
         if (cursor.contentEquals(ch))
         {
            index_ = cursor.getIndex();
            return true;
         }
         
         if (cursor.isLeftBracket())
            if (cursor.fwdToMatchingCharacter())
               continue;
         
      } while (cursor.moveToNextCharacter());
      return false;
   }
   
   private boolean fwdToCharacter__noskip(char ch)
   {
      int idx = data_.indexOf(ch, index_);
      if (idx == -1) return false;
      index_ = idx;
      return true;
   }
   
   public int findNext(char ch)
   {
      return data_.indexOf(ch, index_ + 1);
   }
   
   public char peek()
   {
      return peek(0);
   }
   
   public char peek(int offset)
   {
      int index = index_ + offset;
      if (index < 0 || index >= n_)
         return '\0';
      return data_.charAt(index);
   }
   
   public boolean consume(char expected)
   {
      if (index_ == n_)
         return false;
      
      boolean matches = data_.charAt(index_) == expected;
      index_ += matches ? 1 : 0;
      return matches;
   }
   
   public boolean consumeUntil(char expected)
   {
      int index = data_.indexOf(expected, index_);
      if (index != -1)
      {
         index_ = index;
         return true;
      }
      return false;
   }
   
   public boolean consumeUntil(String expected)
   {
      int index = data_.indexOf(expected, index_);
      if (index != -1)
      {
         index_ = index;
         return true;
      }
      return false;
   }
   
   public boolean consumeUntilRegex(String regex)
   {
      Pattern pattern = Pattern.create(regex);
      Match match = pattern.match(data_, index_);
      if (match == null)
         return false;
      
      index_ = match.getIndex();
      return true;
   }
   
   public String getData()
   {
      return data_;
   }
   
   public void setIndex(int index) { index_ = index; }
   public int getIndex() { return index_; }
   
   public boolean isLeftBracket()
   {
      if (index_ == n_)
         return false;
      char ch = data_.charAt(index_);
      return ch == '{' || ch == '[' || ch == '(';
   }
   
   public boolean isRightBracket()
   {
      if (index_ == n_)
         return false;
      char ch = data_.charAt(index_);
      return ch == '}' || ch == ']' || ch == ')';
   }
   
   public boolean isSingleQuote()
   {
      if (index_ == n_)
         return false;
      return data_.charAt(index_) == '\'';
   }
   
   public boolean isDoubleQuote()
   {
      if (index_ == n_)
         return false;
      return data_.charAt(index_) == '"';
   }
   
   private char complement(char ch)
   {
      switch (ch)
      {
         case '(': return ')';
         case '[': return ']';
         case '{': return '}';
         
         case ')': return '(';
         case ']': return '[';
         case '}': return '{';
      }
      return ch;
   }
   
   private final String data_;
   private int index_;
   private int n_;
   
}
