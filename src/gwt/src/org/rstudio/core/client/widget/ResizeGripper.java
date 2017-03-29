/*
 * ResizeGripper.java
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
package org.rstudio.core.client.widget;


import org.rstudio.core.client.resources.ImageResource2x;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Image;

public class ResizeGripper extends Composite
{
   public interface Observer
   {
      void onResizingStarted();
      void onResizing(int xDelta, int yDelta);
      void onResizingCompleted();
   }
   
   public ResizeGripper(Observer observer)
   {   
      observer_ = observer;
      gripperImageResource_ = new ImageResource2x(RESOURCES.resizeGripper2x());
      Image image = new Image(gripperImageResource_);
      initWidget(image);
      setStylePrimaryName(RESOURCES.styles().resizeGripper());
      
      DOM.sinkEvents(getElement(), 
                     Event.ONMOUSEDOWN | 
                     Event.ONMOUSEMOVE | 
                     Event.ONMOUSEUP |
                     Event.ONLOSECAPTURE);
   }
   
   public int getImageWidth()
   {
      return gripperImageResource_.getWidth();
   }
   
   public int getImageHeight()
   {
      return gripperImageResource_.getHeight();
   }
 
   @Override
   public void onBrowserEvent(Event event) 
   {  
      switch(DOM.eventGetType(event))
      {
         case Event.ONMOUSEDOWN: 
         {
            startResizing();
            
            lastX_ = event.getClientX();
            lastY_ = event.getClientY();
            
            DOM.setCapture(getElement());
            
            observer_.onResizingStarted();
            
            event.preventDefault();
            event.stopPropagation();
            break;
         }
        
         case Event.ONMOUSEMOVE: 
         {
            if (isResizing())
            {      
               int x = event.getClientX();
               int y = event.getClientY();
               
               int xDelta = x - lastX_;
               int yDelta = y - lastY_;
            
               lastX_ = event.getClientX();
               lastY_ = event.getClientY();
               
               observer_.onResizing(xDelta, yDelta);
               
               event.preventDefault();
               event.stopPropagation();
            }
            break;
         }
        
         case Event.ONMOUSEUP:
         {
            if (isResizing())
            {
               stopResizing();
               DOM.releaseCapture(getElement());
               event.preventDefault();
               event.stopPropagation();
            }
            break;   
         }
         
         case Event.ONLOSECAPTURE: // IE-only
         {
            if (isResizing())
               stopResizing();
            break;
         }
      }
   }
   
   private boolean isResizing()
   {
      return sizing_;
   }
   
   private void startResizing()
   {
      sizing_ = true;
   }
   
   private void stopResizing()
   {
      sizing_ = false;
      lastX_ = 0;
      lastY_ = 0;
      observer_.onResizingCompleted();
   }
   
   interface Styles extends CssResource
   {
      String resizeGripper();
   }
   
   interface Resources extends ClientBundle
   {
      @Source("ResizeGripper.css")
      Styles styles();

      @Source("resizeGripper_2x.png")
      ImageResource resizeGripper2x();
   }
   
   static Resources RESOURCES = (Resources)GWT.create(Resources.class);
   public static void ensureStylesInjected() 
   {
      RESOURCES.styles().ensureInjected();
   }
   
   private final ImageResource gripperImageResource_;
   private final Observer observer_;
   
   private boolean sizing_ = false;
   private int lastX_ = 0;
   private int lastY_ = 0;
}
