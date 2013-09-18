/*
 * KeyboardShortcut.java
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
package org.rstudio.core.client.command;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import org.rstudio.core.client.BrowseCap;

public class KeyboardShortcut
{
   public KeyboardShortcut(int keycode)
   {
      this(keycode, "");
   }

   public KeyboardShortcut(int keycode, String groupName)
   {
      this(KeyboardShortcut.NONE, keycode, groupName, "");
   }
   
   public KeyboardShortcut(int modifiers, int keycode)
   {
      this(modifiers, keycode, "", "");
   }

   public KeyboardShortcut(int modifiers, int keycode, 
                           String groupName, String title)
   {
      modifiers_ = modifiers;
      keycode_ = keycode;
      groupName_ = groupName;
      order_ = ORDER++;
      title_ = title;
   }

   @Override
   public boolean equals(Object o)
   {
      if (o == null) return false;

      KeyboardShortcut that = (KeyboardShortcut) o;

      return keycode_ == that.keycode_
            && modifiers_ == that.modifiers_;
   }

   @Override
   public int hashCode()
   {
      int result = modifiers_;
      result = (result << 8) + keycode_;
      return result;
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

   private String getKeyName(boolean pretty)
   {
      boolean macStyle = BrowseCap.hasMetaKey() && pretty;

      if (keycode_ == KeyCodes.KEY_ENTER)
         return macStyle ? "&#8617;" : "Enter";
      else if (keycode_ == KeyCodes.KEY_LEFT)
         return macStyle ? "&#8592;" : "Left";
      else if (keycode_ == KeyCodes.KEY_RIGHT)
         return macStyle ? "&#8594;" : "Right";
      else if (keycode_ == KeyCodes.KEY_UP)
         return macStyle ? "&#8593;" : "Up";
      else if (keycode_ == KeyCodes.KEY_DOWN)
         return macStyle ? "&#8595;" : "Down";
      else if (keycode_ == KeyCodes.KEY_TAB)
         return macStyle ? "&#8677;" : "Tab";
      else if (keycode_ == KeyCodes.KEY_PAGEUP)
         return pretty ? "PgUp" : "PageUp";
      else if (keycode_ == KeyCodes.KEY_PAGEDOWN)
         return pretty ? "PgDn" : "PageDown";
      else if (keycode_ == 191)
         return "/";
      else if (keycode_ == 192)
         return "`";
      else if (keycode_ == 190)
         return ".";
      else if (keycode_ == 187)
         return "=";
      else if (keycode_ == 188)
         return "<";
      else if (keycode_ >= 112 && keycode_ <= 123)
         return "F" + (keycode_ - 111);
      else if (keycode_ == 8)
         return macStyle ? "&#9003;" : "Backspace";


      return Character.toUpperCase((char)keycode_) + "";
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

   private final int modifiers_;
   private final int keycode_;
   private String groupName_;
   private int order_ = 0;
   private String title_ = "";

   private static int ORDER = 0;
   
   public static final int NONE = 0;
   public static final int ALT = 1;
   public static final int CTRL = 2;
   public static final int META = 4;
   public static final int SHIFT = 8;
}
