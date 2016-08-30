/*
 * MouseTracker.java
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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
            if ((preview.getTypeInt() & Event.MOUSEEVENTS) == 0)
               return;
            
            NativeEvent event = preview.getNativeEvent();
            lastMouseX_ = event.getClientX();
            lastMouseY_ = event.getClientY();
         }
      });
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
}
