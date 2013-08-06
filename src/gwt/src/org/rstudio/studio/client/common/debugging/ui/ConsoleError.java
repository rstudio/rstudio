package org.rstudio.studio.client.common.debugging.ui;

import org.rstudio.studio.client.common.debugging.model.ErrorFrame;
import org.rstudio.studio.client.common.debugging.model.UnhandledError;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

public class ConsoleError extends Composite
{

   private static ConsoleErrorUiBinder uiBinder = GWT
         .create(ConsoleErrorUiBinder.class);

   interface ConsoleErrorUiBinder extends UiBinder<Widget, ConsoleError>
   {
   }
   
   public interface Observer
   {
      void onErrorBoxResize();
      void showSourceForFrame(ErrorFrame frame);
   }

   public ConsoleError(UnhandledError err, String errorClass, Observer observer)
   {
      observer_ = observer;
      
      initWidget(uiBinder.createAndBindUi(this));
   
      errorMessage.setText(err.getErrorMessage().trim());
      errorMessage.addStyleName(errorClass);
      
      ClickHandler showHideTraceback = new ClickHandler()
      {         
         @Override
         public void onClick(ClickEvent event)
         {
            showingTraceback_ = !showingTraceback_;
            showTracebackText.setText(showingTraceback_ ? 
                  "Hide Traceback" : "Show Traceback");
            framePanel.setVisible(showingTraceback_);
            observer_.onErrorBoxResize();
         }
      };
      
      showTracebackText.addClickHandler(showHideTraceback);
      showTracebackImage.addClickHandler(showHideTraceback);
      
      for (int i = err.getErrorFrames().length() - 1; i >= 0; i--)
      {
         ConsoleErrorFrame frame = new ConsoleErrorFrame(
               err.getErrorFrames().get(i), observer_);
         framePanel.add(frame);
      }
   }

   @UiField
   Anchor showTracebackText;
   @UiField
   Image showTracebackImage;
   @UiField
   HTMLPanel framePanel;
   @UiField
   Label errorMessage;
   
   private Observer observer_;
   private boolean showingTraceback_ = false;
}
