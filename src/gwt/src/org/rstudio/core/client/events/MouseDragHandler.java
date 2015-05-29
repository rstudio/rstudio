/*
 * NativeKeyDownHandler.java
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
package org.rstudio.core.client.events;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.HasNativeEvent;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;
import com.google.gwt.user.client.ui.Widget;

// A pseudo-handler that implements the boilerplate behind making drag
// events work.
public abstract class MouseDragHandler
{
   public static class MouseCoordinates
   {
      public MouseCoordinates(int mouseX, int mouseY)
      {
         mouseX_ = mouseX;
         mouseY_ = mouseY;
      }
      
      public static MouseCoordinates fromEvent(HasNativeEvent event)
      {
         return fromEvent(event.getNativeEvent());
      }
      
      public static MouseCoordinates fromEvent(NativeEvent event)
      {
         return new MouseCoordinates(event.getClientX(), event.getClientY());
      }
      
      public int getMouseX() { return mouseX_; }
      public int getMouseY() { return mouseY_; }
      
      private final int mouseX_;
      private final int mouseY_;
   }
   
   public static class MouseDragEvent implements HasNativeEvent
   {
      public MouseDragEvent(NativeEvent event,
                            MouseCoordinates initalCoordinates,
                            MouseCoordinates lastCoordinates)
      {
         event_ = event;
         initialCoordinates_ = initalCoordinates;
         lastCoordinates_ = lastCoordinates;
      }
      
      public MouseCoordinates getMouseDelta()
      {
         return new MouseCoordinates(
               lastCoordinates_.getMouseX() - event_.getClientX(),
               lastCoordinates_.getMouseY() - event_.getClientY());
      }
      
      public MouseCoordinates getInitialCoordinates()
      {
         return initialCoordinates_;
      }
      
      public MouseCoordinates getLastCoordinates()
      {
         return lastCoordinates_;
      }
      
      public MouseCoordinates getCoordinates()
      {
         return MouseCoordinates.fromEvent(event_);
      }
      
      public NativeEvent getNativeEvent()
      {
         return event_;
      }
      
      private final NativeEvent event_;
      private final MouseCoordinates initialCoordinates_;
      private final MouseCoordinates lastCoordinates_;
   }
   
   // Must be overriden
   public abstract void onDrag(MouseDragEvent event);
   
   // Optional to override
   public void beginDrag(MouseDownEvent event) {}
   public void endDrag() {}
   
   // Privates ----
   
   private void beginDragImpl(MouseDownEvent event)
   {
      didDrag_ = false;
      
      initialCoordinates_ = lastCoordinates_ =
            MouseCoordinates.fromEvent(event);
      
      if (dragListener_ != null)
         dragListener_.removeHandler();
      
      if (clickSuppressor_ != null)
         clickSuppressor_.removeHandler();
      
      dragListener_ = Event.addNativePreviewHandler(new NativePreviewHandler()
      {
         @Override
         public void onPreviewNativeEvent(NativePreviewEvent npe)
         {
            int type = npe.getTypeInt();
            if (type == Event.ONMOUSEMOVE)
            {
               didDrag_ = true;
               MouseDragEvent event = new MouseDragEvent(
                     npe.getNativeEvent(),
                     getInitialMouseCoordinates(),
                     getLastMouseCoordinates());
               onDragImpl(event);
            }
            else if (type == Event.ONMOUSEUP)
            {
               if (didDrag_)
                  addClickSuppressor();
               endDragImpl();
            }
         }
      });
      
      beginDrag(event);
   }
   
   private void addClickSuppressor()
   {
      clickSuppressor_ = Event.addNativePreviewHandler(new NativePreviewHandler()
      {
         @Override
         public void onPreviewNativeEvent(NativePreviewEvent event)
         {
            if (event.getTypeInt() == Event.ONCLICK)
            {
               event.cancel();
               clickSuppressor_.removeHandler();
               clickSuppressor_ = null;
            }
         }
      });
   }
   
   private void onDragImpl(MouseDragEvent event)
   {
      onDrag(event);
      lastCoordinates_ = MouseCoordinates.fromEvent(event);
   }
   
   private void endDragImpl()
   {
      if (dragListener_ != null)
      {
         dragListener_.removeHandler();
         dragListener_ = null;
      }
      
      endDrag();
   }
   
   public static void addHandler(final Widget widget,
                                 final MouseDragHandler handler)
   {
      widget.addDomHandler(new MouseDownHandler()
      {
         @Override
         public void onMouseDown(MouseDownEvent event)
         {
            if (event.getNativeButton() == NativeEvent.BUTTON_LEFT)
               handler.beginDragImpl(event);
         }
      }, MouseDownEvent.getType());
   }
   
   private MouseCoordinates getInitialMouseCoordinates()
   {
      return initialCoordinates_;
   }
   
   private MouseCoordinates getLastMouseCoordinates()
   {
      return lastCoordinates_;
   }
   
   private boolean didDrag_ = false;
   
   private HandlerRegistration dragListener_;
   private HandlerRegistration clickSuppressor_;
   
   private MouseCoordinates initialCoordinates_;
   private MouseCoordinates lastCoordinates_;
   
}
