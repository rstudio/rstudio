package org.rstudio.studio.client.workbench.views.plots.ui;

import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.core.client.widget.ThemedPopupPanel;
import org.rstudio.studio.client.workbench.views.plots.Plots.ManipulatorChangedHandler;
import org.rstudio.studio.client.workbench.views.plots.model.Manipulator;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

public class ManipulatorPopupPanel extends ThemedPopupPanel 
                             
{

   public ManipulatorPopupPanel(Manipulator manipulator,
                                final ManipulatorChangedHandler changedHandler)
   {
      super(true, false);
      
      manipulator_ = manipulator;
      changedHandler_ = changedHandler;
      
      VerticalPanel mainPanel = new VerticalPanel();
      
      final TextBox inputBox = new TextBox();
      mainPanel.add(inputBox);
      
      mainPanel.add(new ThemedButton("Change", new ClickHandler() {

      
         public void onClick(ClickEvent event)
         {
            int value = Integer.parseInt(inputBox.getText());
           
            JSONObject jsObject = new JSONObject();
            jsObject.put("x", new JSONNumber(value));
 
            changedHandler_.onManipulatorChanged(jsObject);       
         }
         
      }));
      
      
      setWidget(mainPanel);
   }
   
   
   @SuppressWarnings("unused")
   private final Manipulator manipulator_;
   private final ManipulatorChangedHandler changedHandler_;

}
