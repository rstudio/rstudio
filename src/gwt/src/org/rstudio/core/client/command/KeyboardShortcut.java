/*
 * KeyboardShortcut.java
 *
 * Copyright (C) 2009-15 by RStudio, Inc.
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

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.KeyCodes;

import org.rstudio.core.client.BrowseCap;
import java.util.ArrayList;
import java.util.List;

public class KeyboardShortcut
{
   public static class KeyCombination
   {
      public KeyCombination(NativeEvent event)
      {
         keyCode_ = event.getKeyCode();
         modifiers_ = getModifierValue(event);
      }
      
      public KeyCombination(int keyCode, int modifiers)
      {
         keyCode_ = keyCode;
         modifiers_ = modifiers;
      }
      
      public int getKeyCode()
      {
         return keyCode_;
      }
      
      public int getModifier()
      {
         return modifiers_;
      }
      
      @Override
      public String toString()
      {
         return toString(false);
      }
      
      public String toString(boolean pretty)
      {
         if (BrowseCap.hasMetaKey() && pretty)
         {
            return ((modifiers_ & CTRL) == CTRL ? "&#8963;" : "")
                  + ((modifiers_ & SHIFT) == SHIFT ? "&#8679;" : "")
                  + ((modifiers_ & ALT) == ALT ? "&#8997;" : "")
                  + ((modifiers_ & META) == META ? "&#8984;" : "")
                  + getKeyName(true);
         }
         else
         {
            return ((modifiers_ & CTRL) == CTRL ? "Ctrl+" : "")
                  + ((modifiers_ & SHIFT) == SHIFT ? "Shift+" : "")
                  + ((modifiers_ & ALT) == ALT ? "Alt+" : "")
                  + ((modifiers_ & META) == META ? "Meta+" : "")
                  + getKeyName(pretty);
         }
      }
      
      private String getKeyName(boolean pretty)
      {
         boolean macStyle = BrowseCap.hasMetaKey() && pretty;

         if (keyCode_ == KeyCodes.KEY_ENTER)
            return macStyle ? "&#8617;" : "Enter";
         else if (keyCode_ == KeyCodes.KEY_LEFT)
            return macStyle ? "&#8592;" : "Left";
         else if (keyCode_ == KeyCodes.KEY_RIGHT)
            return macStyle ? "&#8594;" : "Right";
         else if (keyCode_ == KeyCodes.KEY_UP)
            return macStyle ? "&#8593;" : "Up";
         else if (keyCode_ == KeyCodes.KEY_DOWN)
            return macStyle ? "&#8595;" : "Down";
         else if (keyCode_ == KeyCodes.KEY_TAB)
            return macStyle ? "&#8677;" : "Tab";
         else if (keyCode_ == KeyCodes.KEY_PAGEUP)
            return pretty ? "PgUp" : "PageUp";
         else if (keyCode_ == KeyCodes.KEY_PAGEDOWN)
            return pretty ? "PgDn" : "PageDown";
         else if (keyCode_ == 191)
            return "/";
         else if (keyCode_ == 192)
            return "`";
         else if (keyCode_ == 190)
            return ".";
         else if (keyCode_ == 187)
            return "=";
         else if (keyCode_ == 188)
            return "<";
         else if (KeyboardHelper.isHyphenKeycode(keyCode_))
            return "-";
         else if (keyCode_ >= 112 && keyCode_ <= 123)
            return "F" + (keyCode_ - 111);
         else if (keyCode_ == 8)
            return macStyle ? "&#9003;" : "Backspace";

         return Character.toUpperCase((char) keyCode_) + "";
      }
      
      @Override
      public int hashCode()
      {
         int result = modifiers_;
         result = (result << 8) + keyCode_;
         return result;
      }
      
      @Override
      public boolean equals(Object object)
      {
         if (object == null || !(object instanceof KeyCombination))
            return false;
         
         KeyCombination other = (KeyCombination) object;
         return keyCode_ == other.keyCode_ &&
                modifiers_ == other.modifiers_;
      }
      
      private final int keyCode_;
      private final int modifiers_;
   }
   
   public static class KeySequence
   {
      public KeySequence()
      {
         keyPresses_ = new ArrayList<KeyCombination>();
      }
      
      public KeySequence(int keyCode, int modifiers)
      {
         this();
         keyPresses_.add(new KeyCombination(keyCode, modifiers));
      }
      
      public void clear()
      {
         keyPresses_.clear();
      }
      
      public void add(NativeEvent event)
      {
         keyPresses_.add(new KeyCombination(
               event.getKeyCode(),
               getModifierValue(event)));
      }
      
      @Override
      public String toString()
      {
         return toString(false);
      }
      
      public String toString(boolean pretty)
      {
         if (keyPresses_.size() == 0)
            return "";
         
         StringBuilder builder = new StringBuilder();
         builder.append(keyPresses_.get(0).toString(pretty));
         for (int i = 1; i < keyPresses_.size(); i++)
         {
            builder.append(", ");
            builder.append(keyPresses_.get(i).toString(pretty));
         }
         return builder.toString();
      }
      
      @Override
      public int hashCode()
      {
         int code = 1;
         for (int i = 0; i < keyPresses_.size(); i++)
            code += (1 << (10 + i)) + keyPresses_.get(i).hashCode();
         return code;
      }
      
      @Override
      public boolean equals(Object object)
      {
         if (object == null || !(object instanceof KeySequence))
            return false;
         
         KeySequence other = (KeySequence) object;
         for (int i = 0; i < keyPresses_.size(); i++)
            if (!keyPresses_.get(i).equals(other.keyPresses_.get(i)))
               return false;
         
         return true;
      }
      
      private final List<KeyCombination> keyPresses_;
   }
   
   public KeyboardShortcut(int keyCode)
   {
      this(keyCode, "");
   }

   public KeyboardShortcut(int keyCode, String groupName)
   {
      this(KeyboardShortcut.NONE, keyCode, groupName, "", "");
   }
   
   public KeyboardShortcut(int modifiers, int keyCode)
   {
      this(modifiers, keyCode, "", "", "");
   }
   
   public KeyboardShortcut(int modifiers, int keyCode, 
                           String groupName, String title, String disableModes)
   {
      this(new KeySequence(keyCode, modifiers), groupName, title, disableModes);
   }
   
   public KeyboardShortcut(KeySequence keySequence)
   {
      this(keySequence, "", "", "");
   }
   
   public KeyboardShortcut(KeySequence keySequence,
                           String groupName, String title, String disableModes)
   {
      keySequence_ = keySequence;
      groupName_ = groupName;
      order_ = ORDER++;
      title_ = title;
      if (disableModes.length() > 0)
      {
         String[] disableModeList = disableModes.split(",");
         for (String disableMode: disableModeList)
         {
            if (disableMode.equals("vim"))
            {
               disableModes_ = disableModes_ | MODE_VIM;
            }
         }
      }
   }
   
   public KeySequence getKeySequence()
   {
      return keySequence_;
   }
   
   @Override
   public boolean equals(Object object)
   {
      if (object == null || !(object instanceof KeyboardShortcut))
         return false;
      
      KeyboardShortcut other = (KeyboardShortcut) object;
      return keySequence_.equals(other.keySequence_);
   }

   @Override
   public int hashCode()
   {
      return keySequence_.hashCode();
   }
   
   @Override
   public String toString()
   {
      return keySequence_.toString(false);
   }
   
   public String toString(boolean pretty)
   {
      return keySequence_.toString(pretty);
   }

   public String getGroupName()
   {
      return groupName_;
   }
   
   public int getOrder()
   {
      return order_;
   }
   
   public String getTitle()
   {
      return title_;
   }

   public boolean isModeDisabled(int mode)
   {
      return (mode & disableModes_) > 0;
   }
   
   public boolean isModalShortcut()
   {
      return disableModes_ != MODE_NONE;
   }

   public static int getModifierValue(NativeEvent e)
   {
      int modifiers = 0;
      if (e.getAltKey())
         modifiers += ALT;
      if (e.getCtrlKey())
         modifiers += CTRL;
      if (e.getMetaKey())
         modifiers += META;
      if (e.getShiftKey())
         modifiers += SHIFT;
      return modifiers;
   }
   
   private final KeySequence keySequence_;
   
   private String groupName_;
   private int order_ = 0;
   private String title_ = "";
   private int disableModes_ = MODE_NONE;

   private static int ORDER = 0;
   
   public static final int NONE = 0;
   public static final int ALT = 1;
   public static final int CTRL = 2;
   public static final int META = 4;
   public static final int SHIFT = 8;
   
   public static final int MODE_NONE = 0;
   public static final int MODE_VIM = 1;
   public static final int MODE_EMACS = 2;
}
