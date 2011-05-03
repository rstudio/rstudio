/*
 * ResizeGripper.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.client.widget;


import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.RootPanel;


public class ResizeGripper extends Composite
{
   public interface Observer
   {
      void onResizing(int xDelta, int yDelta);
      void onResizingCompleted();
   }
   
   public ResizeGripper(Observer observer)
   {   
      observer_ = observer;
      gripperImageResource_ = RESOURCES.resizeGripper();
      Image image = new Image(gripperImageResource_);
      initWidget(image);
      setStylePrimaryName(RESOURCES.styles().resizeGripper());
      
      DOM.sinkEvents(getElement(), 
                     Event.ONMOUSEDOWN | 
                     Event.ONMOUSEMOVE | 
                     Event.ONMOUSEUP |
                     Event.ONLOSECAPTURE);
      
      // glass element for capturing mouse events over iframes (based on 
      // implementation of GWT SplitPanel)
      if (glassElem_ == null) 
      {
         glassElem_ = DOM.createDiv();
         glassElem_.getStyle().setProperty("position", "absolute");
         glassElem_.getStyle().setProperty("top", "0px");
         glassElem_.getStyle().setProperty("left", "0px");
         glassElem_.getStyle().setProperty("margin", "0px");
         glassElem_.getStyle().setProperty("padding", "0px");
         glassElem_.getStyle().setProperty("border", "0px");
   
         // We need to set the background color or mouse events will go right
         // through the glassElem.
         glassElem_.getStyle().setProperty("background", "white");
         glassElem_.getStyle().setProperty("opacity", "0.0");
         glassElem_.getStyle().setProperty("filter", "alpha(opacity=0)");
      }
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
            lastX_ = DOM.eventGetClientX(event);
            lastY_ = DOM.eventGetClientY(event);
            DOM.setCapture(getElement());
            DOM.eventPreventDefault(event);
            break;
         }
        
         case Event.ONMOUSEMOVE: 
         {
            if (isResizing())
            {      
               int x = DOM.eventGetClientX(event);
               int y = DOM.eventGetClientY(event);
               
               int xDelta = x - lastX_;
               int yDelta = y - lastY_;
            
               lastX_ = DOM.eventGetClientX(event);
               lastY_ = DOM.eventGetClientY(event);
               
               observer_.onResizing(xDelta, yDelta);
               
               DOM.eventPreventDefault(event);
            }
            break;
         }
        
         case Event.ONMOUSEUP:
         {
            if (isResizing())
            {
               stopResizing();
               DOM.releaseCapture(getElement());
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
      
      super.onBrowserEvent(event);
   }
   
   private boolean isResizing()
   {
      return sizing_;
   }
   
   private void startResizing()
   {
      sizing_ = true;
      
      // Resize glassElem to take up the entire scrollable window area
      int height = RootPanel.getBodyElement().getScrollHeight() - 1;
      int width = RootPanel.getBodyElement().getScrollWidth() - 1;
      glassElem_.getStyle().setProperty("height", height + "px");
      glassElem_.getStyle().setProperty("width", width + "px");
      RootPanel.getBodyElement().appendChild(glassElem_);
   }
   
   private void stopResizing()
   {
      sizing_ = false;
      lastX_ = 0;
      lastY_ = 0;
      
      RootPanel.getBodyElement().removeChild(glassElem_);
      
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

      ImageResource resizeGripper();
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
   
   private static Element glassElem_ = null;
   

}
