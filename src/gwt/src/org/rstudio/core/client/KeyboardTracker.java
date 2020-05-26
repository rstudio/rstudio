/*
 * KeyboardTracker.java
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
package org.rstudio.core.client;

import org.rstudio.core.client.command.KeyboardShortcut;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;
import com.google.inject.Singleton;

@Singleton
public class KeyboardTracker
{
   public KeyboardTracker()
   {
      Event.addNativePreviewHandler(new NativePreviewHandler()
      {
         @Override
         public void onPreviewNativeEvent(NativePreviewEvent preview)
         {
            int type = preview.getTypeInt();
            if ((type & Event.KEYEVENTS) == 0)
               return;
            
            NativeEvent event = preview.getNativeEvent();
            modifier_ = KeyboardShortcut.getModifierValue(event);
         }
            
      });
   }
   
   public boolean isShiftKeyDown()
   {
      return (modifier_ & KeyboardShortcut.SHIFT) != 0;
   }
   
   private int modifier_ = 0;
   
}
