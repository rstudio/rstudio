/*
 * SpellingSandboxDialog.java
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
package org.rstudio.studio.client.common.spelling.view;

import com.google.gwt.user.client.ui.TextArea;

public class SpellingSandboxDebugText extends TextArea
                                      implements SpellCheckerTarget
{
   
   public SpellingSandboxDebugText()
   {
      setCharacterWidth(80);
      setVisibleLines(25);
   }

   public void changeWord(String fromWord,String toWord)
   {
      if (wordIdx_ > 0 && words_[wordIdx_-1].compareTo(fromWord) == 0)
         words_[wordIdx_-1] = toWord;
   }
   
   public void changeAllWords(String fromWord,String toWord)
   {
      for (int i = 0; i < words_.length; i++)
      {
         if (words_[i].compareTo(fromWord) == 0)
         {
            words_[i] = toWord;
         }
      }
   }
   
   public String getNextWord()
   {
      if (wordIdx_ < words_.length)
      {
         return words_[wordIdx_++];
      } else 
      {
         return "";
      }
   }
   public boolean isEmpty()
   {
      return getText().trim().isEmpty();
   }
   
   public void startSpellChecking()
   {
      words_ = getText().split("\\w");
      whiteSpace_ = getText().split("[^\\w]");
      wordIdx_ = 0;
   }
   
   public void spellCheckComplete()
   {
      // Replace textarea with current words and whitespace
      String[] ary1, ary2;
      if (getText().matches("\\w.*"))
      {
         ary1 = whiteSpace_;
         ary2 = words_;
      } else 
      {
         ary1 = words_;
         ary2 = whiteSpace_;
      }
      int len = (ary1.length <= ary2.length)? ary1.length : ary2.length;
      
      String replace = "";
      for (int i = 0; i < len; i++){
         replace.concat(ary1[i]);
         replace.concat(ary2[i]);
      }
      if (ary1.length <= ary2.length)
      {
         for (int i = ary1.length; i < ary2.length; i++)
            replace.concat(ary2[i]);
      } else 
      {
         for (int i = ary2.length; i < ary1.length; i++)
            replace.concat(ary1[i]);
         
      }
      setText(replace);
   }
   
   int wordIdx_;
   private String[] words_;
   private String[] whiteSpace_;
}

