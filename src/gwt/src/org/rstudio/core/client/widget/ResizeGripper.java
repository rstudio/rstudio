package org.rstudio.core.client.widget;


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
   public ResizeGripper()
   {    
      gripperImageResource_ = RESOURCES.resizeGripper();
      Image image = new Image(gripperImageResource_);
      initWidget(image);
      
      
      DOM.sinkEvents(getElement(), 
                     Event.ONMOUSEOVER |
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
      
      if (Event.ONMOUSEOVER == eventType) 
      {
      
      }
      else if (Event.ONMOUSEDOWN == eventType)
      {
         
      }
      else if (Event.ONMOUSEMOVE == eventType)
      {
         
      }
      else if (Event.ONMOUSEUP == eventType)
      {
         
      }
   }
   
   
   interface Styles extends CssResource
   {
     
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

}
