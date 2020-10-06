/*
 * KeySequence.java
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
package org.rstudio.core.client.command;

import java.util.ArrayList;
import java.util.List;

import org.rstudio.core.client.StringUtil;

import com.google.gwt.dom.client.NativeEvent;

public class KeySequence
{
   public KeySequence()
   {
      keyCombinations_ = new ArrayList<KeyCombination>();
   }

   public KeySequence(List<KeyCombination> keyList)
   {
      keyCombinations_ = new ArrayList<KeyCombination>(keyList);
   }

   public static KeySequence fromShortcutString(String shortcut)
   {
      KeySequence sequence = new KeySequence();
      if (StringUtil.isNullOrEmpty(shortcut))
         return sequence;

      String[] splat = shortcut.split("\\s+");
      for (int i = 0; i < splat.length; i++)
      {
         String sc = splat[i].toLowerCase();

         int modifiers = KeyboardShortcut.NONE;
         if (sc.indexOf("ctrl") != -1)
            modifiers |= KeyboardShortcut.CTRL;
         if (sc.indexOf("alt") != -1 || sc.indexOf("option") != -1)
            modifiers |= KeyboardShortcut.ALT;
         if (sc.indexOf("shift") != -1)
            modifiers |= KeyboardShortcut.SHIFT;
         if (sc.indexOf("meta") != -1 || sc.indexOf("cmd") != -1 || sc.indexOf("command") != -1)
            modifiers |= KeyboardShortcut.META;

         KeyCombination keyCombination;

         if (sc.endsWith("-"))
         {
            keyCombination = new KeyCombination("-", KeyboardHelper.KEY_HYPHEN, modifiers);
         }
         else if (sc.endsWith("+"))
         {
            keyCombination = new KeyCombination("+", 187, modifiers);
         }
         else
         {
            String[] keySplit = sc.split("[-+]");
            String key = StringUtil.capitalize(keySplit[keySplit.length - 1]);
            int keyCode = KeyboardHelper.keyCodeFromKeyName(key);
            keyCombination = new KeyCombination(key, keyCode, modifiers);
         }

         sequence.add(keyCombination);
      }

      return sequence;
   }

   public void set(KeySequence others)
   {
      keyCombinations_.clear();
      keyCombinations_.addAll(others.keyCombinations_);
   }

   public void clear()
   {
      keyCombinations_.clear();
   }

   public KeySequence clone()
   {
      KeySequence clone = new KeySequence();
      clone.keyCombinations_.addAll(keyCombinations_);
      return clone;
   }

   public boolean isEmpty()
   {
      return keyCombinations_.isEmpty();
   }

   public int size()
   {
      return keyCombinations_.size();
   }

   public KeyCombination get(int index)
   {
      return keyCombinations_.get(index);
   }

   public void pop()
   {
      if (keyCombinations_ != null && keyCombinations_.size() > 0)
         keyCombinations_.remove(keyCombinations_.size() - 1);
   }

   public void add(NativeEvent event)
   {
      keyCombinations_.add(new KeyCombination(event));
   }

   public void add(KeyCombination combination)
   {
      keyCombinations_.add(combination);
   }

   public boolean startsWith(KeySequence other, boolean strict)
   {
      if (other.keyCombinations_.size() > keyCombinations_.size())
         return false;

      if (strict && other.keyCombinations_.size() == keyCombinations_.size())
         return false;

      for (int i = 0; i < other.keyCombinations_.size(); i++)
         if (keyCombinations_.get(i) != other.keyCombinations_.get(i))
            return false;

      return true;
   }

   @Override
   public String toString()
   {
      return toString(false);
   }

   public String toString(boolean pretty)
   {
      if (keyCombinations_.isEmpty())
         return "";

      StringBuilder builder = new StringBuilder();
      builder.append(keyCombinations_.get(0).toString(pretty));
      for (int i = 1; i < keyCombinations_.size(); i++)
      {
         builder.append(" ");
         builder.append(keyCombinations_.get(i).toString(pretty));
      }
      return builder.toString();
   }

   @Override
   public int hashCode()
   {
      int code = 1;
      for (int i = 0; i < keyCombinations_.size(); i++)
         code += (1 << (10 + i)) + keyCombinations_.get(i).hashCode();
      return code;
   }

   @Override
   public boolean equals(Object object)
   {
      if (object == null || !(object instanceof KeySequence))
         return false;

      KeySequence other = (KeySequence) object;
      if (keyCombinations_ == null || other.keyCombinations_ == null)
         return false;

      if (keyCombinations_.size() != other.keyCombinations_.size())
         return false;

      for (int i = 0; i < keyCombinations_.size(); i++)
         if (keyCombinations_.get(i) != other.keyCombinations_.get(i))
            return false;

      return true;
   }

   public List<KeyCombination> getData()
   {
      return keyCombinations_;
   }

   private final List<KeyCombination> keyCombinations_;
}


