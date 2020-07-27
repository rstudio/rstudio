/*
 * KeyCombination.java
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

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.Version;
import org.rstudio.core.client.dom.EventProperty;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.KeyCodes;

public class KeyCombination
{
   public KeyCombination(NativeEvent event)
   {
      String key = EventProperty.key(event);
      int keyCode = event.getKeyCode();
      int modifiers = KeyboardShortcut.getModifierValue(event);

      // Unfortunately, the 'key' event property is corrupt with
      // certain versions of Qt. We need to check that we've received
      // a valid 'key' entry; if it's not valid, then we infer the correct
      // key based on the keycode. Note that this inference may be incorrect
      // for alternate keyboard layouts.
      //
      // https://github.com/rstudio/rstudio/issues/6129
      // https://bugreports.qt.io/browse/QTBUG-81783
      if (requiresQtWebEngineWorkaround())
      {
         key = KeyboardHelper.keyNameFromKeyCode(keyCode);
      }

      key_ = key;
      keyCode_ = normalizeKeyCode(keyCode);
      modifiers_ = modifiers;

   }

   public KeyCombination(String key,
                         int keyCode,
                         int modifiers)
   {
      key_ = key;
      keyCode_ = normalizeKeyCode(keyCode);
      modifiers_ = modifiers;
   }

   public String key()
   {
      return key_;
   }

   public int getKeyCode()
   {
      return keyCode_;
   }

   public int getModifier()
   {
      return modifiers_;
   }

   public boolean isCtrlPressed()
   {
      return (modifiers_ & KeyboardShortcut.CTRL) == KeyboardShortcut.CTRL;
   }

   public boolean isAltPressed()
   {
      return (modifiers_ & KeyboardShortcut.ALT) == KeyboardShortcut.ALT;
   }

   public boolean isShiftPressed()
   {
      return (modifiers_ & KeyboardShortcut.SHIFT) == KeyboardShortcut.SHIFT;
   }

   public boolean isMetaPressed()
   {
      return (modifiers_ & KeyboardShortcut.META) == KeyboardShortcut.META;
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
         return ((modifiers_ & KeyboardShortcut.CTRL) == KeyboardShortcut.CTRL ? "&#8963;" : "")
               + ((modifiers_ & KeyboardShortcut.ALT) == KeyboardShortcut.ALT ? "&#8997;" : "")
               + ((modifiers_ & KeyboardShortcut.SHIFT) == KeyboardShortcut.SHIFT ? "&#8679;" : "")
               + ((modifiers_ & KeyboardShortcut.META) == KeyboardShortcut.META ? "&#8984;" : "")
               + getKeyName(true);
      }
      else
      {
         return ((modifiers_ & KeyboardShortcut.CTRL) == KeyboardShortcut.CTRL ? "Ctrl+" : "")
               + ((modifiers_ & KeyboardShortcut.ALT) == KeyboardShortcut.ALT ? "Alt+" : "")
               + ((modifiers_ & KeyboardShortcut.SHIFT) == KeyboardShortcut.SHIFT ? "Shift+" : "")
               + ((modifiers_ & KeyboardShortcut.META) == KeyboardShortcut.META ? "Cmd+" : "")
               + getKeyName(pretty);
      }
   }

   public String getKeyName(boolean pretty)
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
      else if (keyCode_ == 8)
         return macStyle ? "&#9003;" : "Backspace";
      else if (keyCode_ == KeyCodes.KEY_SPACE)
         return macStyle? "&#9250" : "Space";

      if (key_ != null)
         return key_;

      return KeyboardHelper.keyNameFromKeyCode(keyCode_);
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

   private static boolean requiresQtWebEngineWorkaround()
   {
      if (REQUIRES_QT_WEBENGINE_WORKAROUND == null)
      {
         REQUIRES_QT_WEBENGINE_WORKAROUND = requiresQtWebEngineWorkaroundImpl();
      }

      return REQUIRES_QT_WEBENGINE_WORKAROUND;
   }

   private static boolean requiresQtWebEngineWorkaroundImpl()
   {
      if (!BrowseCap.isQtWebEngine())
         return false;

      String version = BrowseCap.qtWebEngineVersion();
      return Version.compare(version, "5.15.0") < 0;
   }

   private static int normalizeKeyCode(int keyCode)
   {
      switch (keyCode)
      {

      case 109: // NumPad minus
      case 173: // Firefox hyphen
         return 189;

      default:
         return keyCode;

      }
   }

   private final String key_;
   private final int keyCode_;
   private final int modifiers_;

   private static Boolean REQUIRES_QT_WEBENGINE_WORKAROUND = null;
}
