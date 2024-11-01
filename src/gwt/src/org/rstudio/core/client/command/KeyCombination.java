/*
 * KeyCombination.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.client.command;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.CoreClientConstants;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.dom.EventProperty;
import org.rstudio.studio.client.application.Desktop;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.KeyCodes;

public class KeyCombination
{
   public KeyCombination(NativeEvent event)
   {
      String key = EventProperty.key(event);
      int keyCode = event.getKeyCode();
      int modifiers = KeyboardShortcut.getModifierValue(event);

      key_ = key;
      keyCode_ = normalizeKeyCode(keyCode, key_);
      modifiers_ = modifiers;
   }

   public KeyCombination(String key,
                         int keyCode,
                         int modifiers)
   {
      key_ = key;
      keyCode_ = normalizeKeyCode(keyCode, key);
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
         return ((modifiers_ & KeyboardShortcut.CTRL) == KeyboardShortcut.CTRL ? keyComboString(KeyboardShortcut.CTRL) : "")
               + ((modifiers_ & KeyboardShortcut.ALT) == KeyboardShortcut.ALT ? keyComboString(KeyboardShortcut.ALT) : "")
               + ((modifiers_ & KeyboardShortcut.SHIFT) == KeyboardShortcut.SHIFT ? keyComboString(KeyboardShortcut.SHIFT) : "")
               + ((modifiers_ & KeyboardShortcut.META) == KeyboardShortcut.META ? keyComboString(KeyboardShortcut.META) : "")
               + getKeyName(pretty);
      }
   }

   // Desktop IDE requires unlocalized modifier key names for menu shortcuts: https://github.com/rstudio/rstudio/issues/11073
   private String keyComboString(int key)
   {
      switch (key)
      {
         case KeyboardShortcut.CTRL:
            return Desktop.hasDesktopFrame() ? "Ctrl+" : constants_.keyComboCtrl(); //$NON-NLS-1$
         case KeyboardShortcut.ALT:
            return Desktop.hasDesktopFrame() ? "Alt+" : constants_.keyComboAlt(); //$NON-NLS-1$
         case KeyboardShortcut.SHIFT:
            return Desktop.hasDesktopFrame() ? "Shift+" : constants_.keyComboShift(); //$NON-NLS-1$
         case KeyboardShortcut.META:
            return Desktop.hasDesktopFrame() ? "Cmd+" : constants_.keyComboCmd(); //$NON-NLS-1$
         default:
            return "";
      }
   }

   public String getKeyName(boolean pretty)
   {
      boolean macStyle = BrowseCap.hasMetaKey() && pretty;

      // Desktop IDE requires unlocalized modifier key names for menu shortcuts: https://github.com/rstudio/rstudio/issues/11073
      if (keyCode_ == KeyCodes.KEY_ENTER)
         return macStyle ? "&#8617;" : Desktop.hasDesktopFrame() ? "Enter" : constants_.keyNameEnter(); //$NON-NLS-1$
      else if (keyCode_ == KeyCodes.KEY_LEFT)
         return macStyle ? "&#8592;" : Desktop.hasDesktopFrame() ? "Left" : constants_.keyNameLeft(); //$NON-NLS-1$
      else if (keyCode_ == KeyCodes.KEY_RIGHT)
         return macStyle ? "&#8594;" : Desktop.hasDesktopFrame() ? "Right" : constants_.keyNameRight(); //$NON-NLS-1$
      else if (keyCode_ == KeyCodes.KEY_UP)
         return macStyle ? "&#8593;" : Desktop.hasDesktopFrame() ? "Up" : constants_.keyNameUp(); //$NON-NLS-1$
      else if (keyCode_ == KeyCodes.KEY_DOWN)
         return macStyle ? "&#8595;" : Desktop.hasDesktopFrame() ? "Down" : constants_.keyNameDown(); //$NON-NLS-1$
      else if (keyCode_ == KeyCodes.KEY_TAB)
         return macStyle ? "&#8677;" : Desktop.hasDesktopFrame() ? "Tab" : constants_.keyNameTab(); //$NON-NLS-1$
      else if (keyCode_ == KeyCodes.KEY_PAGEUP)
         return pretty ? "PgUp" : Desktop.hasDesktopFrame() ? "PageUp" : constants_.keyNamePageUp(); //$NON-NLS-1$
      else if (keyCode_ == KeyCodes.KEY_PAGEDOWN)
         return pretty ? "PgDn" : Desktop.hasDesktopFrame() ? "PageDown" : constants_.keyNamePageDown(); //$NON-NLS-1$
      else if (keyCode_ == 8)
         return macStyle ? "&#9003;" : Desktop.hasDesktopFrame() ? "Backspace" : constants_.keyNameBackspace(); //$NON-NLS-1$
      else if (keyCode_ == KeyCodes.KEY_SPACE)
         // Mac spacebar shortcut character looks too much like a 'b'
         // return macStyle? "&#9250" : "Space";
         return Desktop.hasDesktopFrame() ? "Space" : constants_.keyNameSpace(); //$NON-NLS-1$

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

   private static int normalizeMinusKeyCode(int keyCode)
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

   private static int normalizeKeyCode(int keyCode, String key)
   {
      keyCode = normalizeMinusKeyCode(keyCode);

      // We shouldn't be using keycodes: they assume the US English keyboard layout.
      // Switching over entirely is a big project, so for now we'll map some specific 
      // keys back to their US keyboard keycodes.

      // Alt+- is default for "insert assignment operator" so map the "-" back to US keyboard.
      //
      // Historical curiousity: Qt did some of this for us under the covers, thus the
      // insert assignment operator worked on Qt desktop but not on Server or Electron.
      //
      // https://github.com/rstudio/rstudio/issues/12457
      if (StringUtil.equals(key, "-"))
      {
         keyCode = 189;
      }

      return keyCode;
   }

   private final String key_;
   private final int keyCode_;
   private final int modifiers_;

   private static final CoreClientConstants constants_ = GWT.create(CoreClientConstants.class);
}
