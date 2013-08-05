package org.rstudio.studio.client.common.debugging.ui;

import org.rstudio.studio.client.common.debugging.model.UnhandledError;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;

public class ConsoleError extends Composite
{

   private static ConsoleErrorUiBinder uiBinder = GWT
         .create(ConsoleErrorUiBinder.class);

   interface ConsoleErrorUiBinder extends UiBinder<Widget, ConsoleError>
   {
   }

   public ConsoleError(UnhandledError err)
   {
      initWidget(uiBinder.createAndBindUi(this));
      
      showTraceback.addClickHandler(new ClickHandler()
      {         
         @Override
         public void onClick(ClickEvent event)
         {
            showingTraceback_ = !showingTraceback_;
            showTraceback.setText(showingTraceback_ ? 
                  "Show Traceback" : "Hide Traceback");
            framePanel.setVisible(showingTraceback_);
         }
      });
      
      for (int i = 0; i < err.getErrorFrames().length(); i++)
      {
         ConsoleErrorFrame frame = new ConsoleErrorFrame(
               err.getErrorFrames().get(i));
         framePanel.add(frame);
      }
   }

   @UiField
   Button showTraceback;
   @UiField
   HTMLPanel framePanel;
   
   private boolean showingTraceback_ = false;
}
