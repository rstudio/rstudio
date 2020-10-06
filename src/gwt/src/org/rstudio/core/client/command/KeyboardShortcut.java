/*
 * KeyboardShortcut.java
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

import com.google.gwt.dom.client.NativeEvent;

public class KeyboardShortcut
{
   public KeyboardShortcut(KeySequence keySequence,
                           String groupName,
                           String title,
                           String disableModes)
   {
      keySequence_ = keySequence;
      groupName_ = groupName;
      order_ = ORDER++;
      title_ = title;
      disableModes_ = ShortcutManager.parseDisableModes(disableModes);
   }

   public KeyboardShortcut(KeySequence keySequence)
   {
      this(keySequence, "", "", "");
   }

   public KeyboardShortcut(String key, int keyCode, int modifiers)
   {
      this(keySequence(key, keyCode, modifiers));
   }


   public int getDisableModes()
   {
      return disableModes_;
   }

   public KeySequence getKeySequence()
   {
      return keySequence_;
   }

   public boolean startsWith(KeyboardShortcut other, boolean strict)
   {
      return getKeySequence().startsWith(other.getKeySequence(), strict);
   }

   public boolean startsWith(KeySequence sequence, boolean strict)
   {
      return getKeySequence().startsWith(sequence, strict);
   }

   @Override
   public boolean equals(Object object)
   {
      if (object == null || !(object instanceof KeyboardShortcut))
         return false;

      KeyboardShortcut other = (KeyboardShortcut) object;
      return keySequence_ == other.keySequence_;
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

   private static KeySequence keySequence(String key,
                                          int keyCode,
                                          int modifiers)
   {
      KeySequence sequence = new KeySequence();
      sequence.add(new KeyCombination(key, keyCode, modifiers));
      return sequence;
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
   public static final int MODE_DEFAULT = 1;
   public static final int MODE_VIM = 2;
   public static final int MODE_EMACS = 4;
   public static final int MODE_SUBLIME = 8;
}
