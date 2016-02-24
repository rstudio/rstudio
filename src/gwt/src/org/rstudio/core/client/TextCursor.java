/*
 * TextCursor.java
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
      return data_.charAt(index_) == ch;
   }
   
   public boolean fwdToMatchingCharacter()
   {
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
      int idx = data_.indexOf(ch, index_ + 1);
      if (idx == -1) return false;
      index_ = idx;
      return true;
   }
   
   public int findNext(char ch)
   {
      return data_.indexOf(ch, index_ + 1);
   }
   
   public String getData() { return data_; }
   public int getIndex() { return index_; }
   
   public boolean isLeftBracket()
   {
      char ch = data_.charAt(index_);
      return ch == '{' || ch == '[' || ch == '(';
   }
   
   public boolean isRightBracket()
   {
      char ch = data_.charAt(index_);
      return ch == '}' || ch == ']' || ch == ')';
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
