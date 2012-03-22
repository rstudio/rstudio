/*
 * SpellingSandboxDebugText.java
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

import java.util.ArrayList;

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
      if (wordIdx_ > 0 && words_.get(wordIdx_-1).compareTo(fromWord) == 0){
         words_.set(wordIdx_-1,toWord);
         isDirty_ = true;
      }
   }
   
   public void changeAllWords(String fromWord,String toWord)
   {
      for (int i = 0; i < words_.size(); i++)
      {
         if (words_.get(i).compareTo(fromWord) == 0)
         {
            words_.set(i, toWord);
            isDirty_ = true;
         }
      }
   }
   
   public String getNextWord()
   {
      if (wordIdx_ < words_.size())
      {
         return words_.get(wordIdx_++);
      } else 
      {
         return "";
      }
   }
   public boolean isEmpty()
   {
      return getText().trim().isEmpty();
   }
   
   public ArrayList<String> stringArrayToList(String[] ary)
   {
      ArrayList<String> lst = new ArrayList<String>();
      if (ary.length > 0)
      {
         if (!ary[0].isEmpty())
            lst.add(ary[0]);
         for (int i = 1; i < ary.length; i++)
            lst.add(ary[i]);
      }
      return lst;
   }
   
   public void logArrayList(String msg, ArrayList<String> ary)
   {
      Debug.log(msg+"\n");
      for (int i = 0; i < ary.size(); i++)
      {
         Debug.log("<"+ary.get(i)+">\n");
      }
   }
   public void startSpellChecking()
   {
      Debug.log("text: <"+getText()+">\n");
      words_ = stringArrayToList(getText().split("[^"+wordPattern_+"]+"));
      logArrayList("words_:",words_);
      nonWords_ = stringArrayToList(getText().split("["+wordPattern_+"]+"));
      logArrayList("nonWords_:",nonWords_);
      wordIdx_ = 0;
      isDirty_ = false;
   }
   
   public void spellCheckComplete()
   {
      if (!isDirty_)
         return;
      
      // Replace textarea with current words and nonWords
      if (nonWords_.size() > 0)
      {
         ArrayList<String> ary1, ary2;
         if (String.valueOf(getText().charAt(0)).matches("["+wordPattern_+"]"))
         {
            Debug.log("Printing words first\n");
            ary1 = words_;
            ary2 = nonWords_;
         } else 
         {
            ary1 = nonWords_;
            ary2 = words_;
         }
         int len = (ary1.size() <= ary2.size())? ary1.size() : ary2.size();
         
         StringBuilder replace = new StringBuilder();
         for (int i = 0; i < len; i++){
            replace.append(ary1.get(i));
            replace.append(ary2.get(i));
         }
         if (ary1.size() <= ary2.size())
         {
            for (int i = ary1.size(); i < ary2.size(); i++)
               replace.append(ary2.get(i));
         } else 
         {
            for (int i = ary2.size(); i < ary1.size(); i++)
               replace.append(ary1.get(i));
            
         }
         setText(replace.toString());
      } else {
         setText(words_.get(0));
      }
   }
   
   private boolean isDirty_;
   private int wordIdx_;
   private String wordPattern_ = "\\w'";
   private ArrayList<String> words_;
   private ArrayList<String> nonWords_;
}

