/*
 * MouseTracker.java
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;
import com.google.inject.Singleton;

@Singleton
public class MouseTracker
{
   public MouseTracker()
   {
      Event.addNativePreviewHandler(new NativePreviewHandler()
      {
         @Override
         public void onPreviewNativeEvent(NativePreviewEvent preview)
         {
            int type = preview.getTypeInt();
            if ((type & Event.MOUSEEVENTS) == 0)
            {
               lastEventType_ = type;
               return;
            }
            
            NativeEvent event = preview.getNativeEvent();
            
            // Safari triggers certain mouse events when modifier keys are
            // pressed. Suppress those as they can cause unexpected behavior
            // when e.g. typing in a textbox while the mouse happens to lie
            // over a popup panel (e.g. C++ completions, Find in Files)
            if (isSafariModifierKeyCausingMouseEvent(event, type))
            {
               event.stopPropagation();
               event.preventDefault();
            }
            
            lastEventType_ = type;
            lastMouseX_ = event.getClientX();
            lastMouseY_ = event.getClientY();
         }
      });
   }
   
   private boolean isSafariModifierKeyCausingMouseEvent(NativeEvent event,
                                                        int type)
   {
      if (!IS_SAFARI)
         return false;
      
      // If the last event was a key event, and we just got a mouse event,
      // suppress it. (The intention here is to suppress a 'mouseout' or
      // 'mouseover' event synthesized in respond to a modifier keypress.
      //
      // Note that these events can be emitted in response to a keyup as
      // well; in such a case the associated modifier key will no longer
      // be held down and so we just globally suppress 'mouseover' and
      // 'mouseout' following a keyevent.
      if ((lastEventType_ & Event.KEYEVENTS) != 0 &&
          (type == Event.ONMOUSEOVER || type == Event.ONMOUSEOUT))
      {
         suppressing_ = true;
         return true;
      }
      
      // Stop suppressing once we get a new non-mouseover/mouseout event.
      else if (type != Event.ONMOUSEOVER && type != Event.ONMOUSEOUT)
      {
         suppressing_ = false;
         return false;
      }
      
      // For other intermediate events, return status of suppressing flag.
      return suppressing_;
   }
   
   public int getLastMouseX()
   {
      return lastMouseX_;
   }
   
   public int getLastMouseY()
   {
      return lastMouseY_;
   }
   
   private int lastMouseX_;
   private int lastMouseY_;
   
   private int lastEventType_;
   private boolean suppressing_;
   
   private static final boolean IS_SAFARI =
         BrowseCap.isSafari() || BrowseCap.isMacintoshDesktop();
}
