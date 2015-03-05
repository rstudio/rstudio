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

import com.google.gwt.dom.client.NativeEvent;

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

}
