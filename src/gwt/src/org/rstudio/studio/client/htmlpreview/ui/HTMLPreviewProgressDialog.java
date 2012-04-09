package org.rstudio.studio.client.htmlpreview.ui;


import org.rstudio.core.client.widget.ProgressDialog;
import org.rstudio.studio.client.common.OutputBuffer;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;


public class HTMLPreviewProgressDialog extends ProgressDialog
                                       implements HasClickHandlers

{
   public HTMLPreviewProgressDialog(String caption)
   {
      super(caption);        
   }
   
   @Override
   public HandlerRegistration addClickHandler(ClickHandler handler)
   {
      return stopButton().addClickHandler(handler);
   }  
   
   public void setCaption(String caption)
   {
      setLabel(caption);
   }
  
   public void showOutput(String output)
   {
      if (!isShowing())
         showModal();
      
      output_.append(output);  
   }
   
   public void stopProgress()
   {
      hideProgress();
      stopButton().setText("Close");
   }
   
   public void dismiss()
   {
      closeDialog();
   }

   @Override
   protected Widget createDisplayWidget()
   {
      SimplePanel panel = new SimplePanel();
      int maxHeight = Window.getClientHeight() - 150;
      int height = Math.min(500, maxHeight);
      panel.getElement().getStyle().setHeight(height, Unit.PX);
           
      output_ = new OutputBuffer();
      panel.setWidget(output_);
      return panel;
   } 

   private OutputBuffer output_;  
}
