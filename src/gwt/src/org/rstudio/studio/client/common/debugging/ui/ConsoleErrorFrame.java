package org.rstudio.studio.client.common.debugging.ui;

import org.rstudio.studio.client.common.debugging.model.ErrorFrame;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

public class ConsoleErrorFrame extends Composite
{

   private static ConsoleErrorFrameUiBinder uiBinder = GWT
         .create(ConsoleErrorFrameUiBinder.class);

   interface ConsoleErrorFrameUiBinder extends
         UiBinder<Widget, ConsoleErrorFrame>
   {
   }

   public ConsoleErrorFrame(ErrorFrame frame)
   {
      initWidget(uiBinder.createAndBindUi(this));
      
      frame_ = frame;
      
      boolean hasSource = !frame.getFileName().isEmpty();
      functionName.setText(frame.getFunctionName() + (hasSource ? " at " : ""));
      if (hasSource)
      {
         sourceLink.setText(frame.getFileName());
         sourceLink.setHref("#");
         sourceLink.addClickHandler(new ClickHandler()
         {            
            @Override
            public void onClick(ClickEvent event)
            {
               navigateToSource();
            }
         });
      }
   }
   
   private void navigateToSource()
   {
      // TODO: Perform source navigation 
      if (frame_ != null)
      {
         
      }
   }

   @UiField
   Label functionName;
   @UiField
   Anchor sourceLink;
   
   private ErrorFrame frame_ = null;
}
