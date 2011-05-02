package org.rstudio.core.client.widget;


import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;




public class ResizeGripper extends Composite
{
   public ResizeGripper(Widget targetWidget)
   {    
      targetWidget_ = targetWidget;
      gripperImageResource_ = RESOURCES.resizeGripper();
      Image image = new Image(gripperImageResource_);
      initWidget(image);
      setStylePrimaryName(RESOURCES.styles().resizeGripper());
      
      
      DOM.sinkEvents(getElement(), 
                     Event.ONMOUSEDOWN | 
                     Event.ONMOUSEMOVE | 
                     Event.ONMOUSEUP);
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
      final int eventType = DOM.eventGetType(event);
      
      if (Event.ONMOUSEDOWN == eventType)
      {
         sizing_ = true;
         lastX_ = DOM.eventGetClientX(event);
         lastY_ = DOM.eventGetClientY(event);
         DOM.setCapture(getElement());
      }
      else if (Event.ONMOUSEMOVE == eventType)
      {
         if (sizing_)
         {     
            DOM.setCapture(getElement());
            
            int x = DOM.eventGetClientX(event);
            int y = DOM.eventGetClientY(event);
            int xOffset = x - lastX_;
            int yOffset = y - lastY_;
            
            targetWidget_.setSize(
              targetWidget_.getOffsetWidth() + xOffset + "px",
              targetWidget_.getOffsetHeight() + yOffset + "px");
            
            lastX_ = DOM.eventGetClientX(event);
            lastY_ = DOM.eventGetClientY(event);
            
            event.stopPropagation();
         }
      }
      else if (Event.ONMOUSEUP == eventType)
      {
         sizing_ = false;
         lastX_ = 0;
         lastY_ = 0;
         DOM.releaseCapture(getElement());
         
      }
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
   
   private final Widget targetWidget_;
   private final ImageResource gripperImageResource_;
   
   private boolean sizing_ = false;
   private int lastX_ = 0;
   private int lastY_ = 0;
   

}
