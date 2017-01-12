/*
 * KeyboardHelper.java
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

import org.rstudio.core.client.BrowseCap;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.KeyCodes;

public class KeyboardHelper
{
   public static boolean isHyphen(NativeEvent event)
   {
      if (KeyboardShortcut.getModifierValue(event) != KeyboardShortcut.NONE)
         return false;
      
      return isHyphenKeycode(event.getKeyCode());
   }
   
   public static boolean isHyphenKeycode(NativeEvent event)
   {
      return isHyphenKeycode(event.getKeyCode());
   }
   
   public static boolean isHyphenKeycode(int keyCode)
   {
      if (BrowseCap.isFirefox())
         return keyCode == 173;
      else
         return keyCode == 189;
   }
   
   public static boolean isUnderscore(NativeEvent event)
   {
      return event.getShiftKey() && isHyphenKeycode(event.getKeyCode());
   }
   
   public static boolean isPeriod(NativeEvent event)
   {
      return !event.getShiftKey() && isPeriodKeycode(event.getKeyCode());
   }
   
   public static boolean isPeriodKeycode(int keyCode)
   {
      return keyCode == 190;
   }
   
   public static boolean isModifierKey(int keyCode)
   {
      switch (keyCode)
      {
      case KeyCodes.KEY_CTRL:
      case KeyCodes.KEY_ALT:
      case KeyCodes.KEY_SHIFT:
      case KeyCodes.KEY_WIN_IME:
      case KeyCodes.KEY_WIN_KEY:
      case KeyCodes.KEY_WIN_KEY_FF_LINUX:
      case KeyCodes.KEY_WIN_KEY_RIGHT:
      case KeyCodes.KEY_WIN_KEY_LEFT_META:
         return true;
      case KeyCodes.KEY_CONTEXT_MENU: // right meta on macOS
         return !BrowseCap.isWindows();
      default:
         return false;
      }
   }
   
   public static String keyNameFromKeyCode(int keyCode)
   {
      return keyNameFromKeyCode(keyCode, KEY_CODE_TO_KEY_NAME_MAP);
   }
   
   private static final native String keyNameFromKeyCode(int keyCode, JavaScriptObject map)
   /*-{
      return map[keyCode] || String.fromCharCode(keyCode);
   }-*/;
   
   public static int keyCodeFromKeyName(String keyName)
   {
      return keyCodeFromKeyName(keyName, KEY_NAME_TO_KEY_CODE_MAP);
   }
   
   private static final native int keyCodeFromKeyName(String keyName, JavaScriptObject map)
   /*-{
      return map[keyName.toLowerCase()] || -1;
   }-*/;
   
   private static final native JavaScriptObject makeKeyCodeToKeyNameMap()
   /*-{
      // Map array indices as key codes to corresponding name.
      var map = new Array(256);
      
      map[8] = "Backspace";
      map[9] = "Tab";
      map[12] = "NumLock";
      map[13] = "Enter";
      map[16] = "Shift";
      map[17] = "Ctrl";
      map[18] = "Alt";
      map[19] = "Pause";
      map[20] = "CapsLock";
      map[27] = "Escape";
      map[32] = "Space";
      map[33] = "PageUp";
      map[34] = "PageDown";
      map[35] = "End";
      map[36] = "Home";
      map[37] = "Left";
      map[38] = "Up";
      map[39] = "Right";
      map[40] = "Down";
      map[45] = "Insert";
      map[46] = "Delete";
      
      // Add in numbers 0-9
      for (var i = 48; i <= 57; i++)
         map[i] = "" + (i - 48);
         
      // Add in letters
      for (var i = 65; i <= 90; i++)
         map[i] = String.fromCharCode(i);
         
      // TODO: Treat these keys differently?
      map[91] = "Meta";
      map[92] = "Meta";
      map[93] = "Meta";
      
      for (var i = 96; i <= 105; i++)
         map[i] = "NumPad" + (i - 96);
      
      map[106] = "*";
      map[107] = "+";
      map[109] = "-";
      
     // NOTE: This is actually 'decimal point' which is
     // distinct as a keycode from '.', but probably easier
     // to just treat them the same.
      map[110] = "." 
      
      map[111] = "/";
      
      // Function keys
      for (var i = 112; i <= 123; i++)
         map[i] = "F" + (i - 111);
      
      map[144] = "NumLock";
      map[145] = "ScrollLock";
      map[186] = ";";
      map[187] = "=";
      map[188] = ",";
      map[189] = "-";
      map[190] = ".";
      map[191] = "/";
      map[192] = "`";
      map[219] = "[";
      map[220] = "\\";
      map[221] = "]";
      map[222] = "'";
      
      return map; 
   }-*/;
   
   private static final native JavaScriptObject makeKeyNameToKeyCodeMap(JavaScriptObject map)
   /*-{
      
      var result = {};
      for (var key in map) {
         var value = map[key];
         result[value.toLowerCase()] = parseInt(key);
      }
      return result;
      
   }-*/;
   
   private static final JavaScriptObject KEY_CODE_TO_KEY_NAME_MAP =
         makeKeyCodeToKeyNameMap();
   
   private static final JavaScriptObject KEY_NAME_TO_KEY_CODE_MAP =
         makeKeyNameToKeyCodeMap(KEY_CODE_TO_KEY_NAME_MAP);
   
   public static final int KEY_HYPHEN = BrowseCap.isFirefox() ? 173 : 189;
}
