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

import org.rstudio.core.client.Debug;
import com.google.gwt.user.client.ui.TextArea;

public class SpellingSandboxDebugText extends TextArea
                                      implements SpellCheckerTarget
{
   
   public SpellingSandboxDebugText()
   {
      setCharacterWidth(80);
      setVisibleLines(20);
   }

   public void changeWord(String fromWord,String toWord)
   {
      if (wordIdx_ > 0 && words_[wordIdx_-1].compareTo(fromWord) == 0){
         words_[wordIdx_-1] = toWord;
         isDirty_ = true;
      }
   }
   
   public void changeAllWords(String fromWord,String toWord)
   {
      for (int i = 0; i < words_.length; i++)
      {
         if (words_[i].compareTo(fromWord) == 0)
         {
            words_[i] = toWord;
            isDirty_ = true;
         }
      }
   }
   
   public String getNextWord()
   {
      if (wordIdx_ < words_.length)
      {
         Debug.log(new String("getNextWord("+words_[wordIdx_] + ")\n"));
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
      words_ = getText().split("[ \n\t]+");
      Debug.log("words_[0] is: " + words_[0]);
      whiteSpace_ = getText().split("[^ \n\t]+");
      wordIdx_ = 0;
      isDirty_ = false;
   }
   
   public void spellCheckComplete()
   {
      if (!isDirty_)
         return;
      
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
      
      StringBuilder replace = new StringBuilder();
      for (int i = 0; i < len; i++){
         Debug.log("ary1["+String.valueOf(i)+"] is" + ary1[i] + "\n");
         Debug.log("ary2["+String.valueOf(i)+"] is" + ary2[i] + "\n");
         replace.append(ary1[i]);
         replace.append(ary2[i]);
         Debug.log("replace is" + replace + "\n");
      }
      if (ary1.length <= ary2.length)
      {
         for (int i = ary1.length; i < ary2.length; i++)
            replace.append(ary2[i]);
      } else 
      {
         for (int i = ary2.length; i < ary1.length; i++)
            replace.append(ary1[i]);
         
      }
      setText(replace.toString());
   }
   
   boolean isDirty_;
   int wordIdx_;
   private String[] words_;
   private String[] whiteSpace_;
}

